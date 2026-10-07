package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentActivation;
import net.pixeldreamstudios.combat_styles.equipment.SummonEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SummonLifecycle {

    private record Gear(long tick, Map<EquipmentSlot, ItemStack> stacks) {
    }

    private static final Map<UUID, Gear> FALLEN_GEAR = new ConcurrentHashMap<>();

    private SummonLifecycle() {
    }

    public static void freshlySpawned(Entity entity) {
        if (entity == null || entity.level().isClientSide() || SummonOwner.isSummon(entity)) {
            return;
        }
        SummonStrike.linkCast(entity);
        Entity creator = SummonOwner.summoningCreatorOf(entity);
        if (creator == null) {
            return;
        }
        Player owner = SummonOwner.ownerPlayer(creator);
        if (owner == null) {
            return;
        }
        SummonOwner.stamp(entity, owner);
        if (SpellEngineCompat.reownSpellSummon(entity, owner)) {
            return;
        }
        if (creator instanceof SummonData from && entity instanceof SummonData into) {
            into.combat_styles$setSummonDamage(from.combat_styles$summonDamage());
            into.combat_styles$setSummonSpec(from.combat_styles$summonSpec());
            into.combat_styles$applySummonGoals();
        }
    }

    public static void spawned(Player owner, Entity summon) {
        fire(owner, new SummonEvent(SummonEvent.Kind.SPAWN, summon, null));
    }

    public static void died(LivingEntity dying, DamageSource source) {
        if (dying == null || dying.level().isClientSide()) {
            return;
        }
        if (SummonOwner.isSummon(dying)) {
            Player owner = SummonOwner.ownerPlayer(dying);
            LivingEntity killer = source != null && source.getEntity() instanceof LivingEntity living
                    ? living
                    : null;
            fire(owner, new SummonEvent(SummonEvent.Kind.DEATH, dying, killer));
            return;
        }
        if (dying instanceof Mob) {
            FALLEN_GEAR.put(dying.getUUID(), new Gear(dying.level().getGameTime(),
                    SummonEquipment.snapshotOf(dying)));
        }
        Entity killer = summonKillerOf(source);
        if (killer != null) {
            fire(SummonOwner.ownerPlayer(killer), new SummonEvent(SummonEvent.Kind.KILL, killer, dying));
            AbilityHosts.react(killer, PassiveTrigger.KILL, dying);
            return;
        }
        Entity pet = hostedPetBehind(source);
        if (pet != null) {
            AbilityHosts.react(pet, PassiveTrigger.KILL, dying);
        }
    }

    public static void hurt(LivingEntity victim, DamageSource source) {
        if (victim == null || victim.level().isClientSide()) {
            return;
        }
        LivingEntity attacker = source != null && source.getEntity() instanceof LivingEntity living
                ? living
                : null;
        if (SummonOwner.isSummon(victim) || AbilityHosts.hosts(victim)) {
            AbilityHosts.react(victim, PassiveTrigger.HURT, attacker);
        }
        Entity pet = hostedPetBehind(source);
        if (pet != null && pet != victim) {
            AbilityHosts.react(pet, PassiveTrigger.HIT, victim);
        }
    }

    private static Entity hostedPetBehind(DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity direct = source.getDirectEntity();
        Entity shooter = direct instanceof Projectile projectile ? projectile.getOwner() : null;
        for (Entity candidate : new Entity[] {direct, source.getEntity(), shooter}) {
            if (candidate != null && !SummonOwner.isSummon(candidate) && AbilityHosts.hosts(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Entity summonKillerOf(DamageSource source) {
        Entity striking = SummonStrike.striking();
        if (SummonOwner.isSummon(striking)) {
            return striking;
        }
        if (source == null) {
            return null;
        }
        if (SummonOwner.isSummon(source.getDirectEntity())) {
            return source.getDirectEntity();
        }
        return SummonOwner.isSummon(source.getEntity()) ? source.getEntity() : null;
    }

    public static Map<EquipmentSlot, ItemStack> gearOf(LivingEntity victim) {
        Gear captured = FALLEN_GEAR.get(victim.getUUID());
        if (captured != null && captured.tick() == victim.level().getGameTime()) {
            return captured.stacks();
        }
        return SummonEquipment.snapshotOf(victim);
    }

    public static void tick(long gameTime) {
        FALLEN_GEAR.values().removeIf(gear -> gear.tick() < gameTime);
    }

    private static void fire(Player owner, SummonEvent event) {
        if (owner == null) {
            return;
        }
        try {
            EquipmentActivation.summoned(owner, event);
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.error("Failed to fire summon {} event", event.kind(), throwable);
        }
    }

    public static void dismiss(Entity summon) {
        if (summon == null || summon.isRemoved()) {
            return;
        }
        if (summon instanceof ConfigurableSummon configurable) {
            configurable.dismiss();
            return;
        }
        summon.discard();
    }

    public static int clearOwned(MinecraftServer server, UUID owner) {
        if (server == null || owner == null) {
            return 0;
        }
        List<Entity> owned = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (SummonOwner.isSummon(entity) && owner.equals(SummonOwner.ownerOf(entity))) {
                    owned.add(entity);
                }
            }
        }
        for (Entity entity : owned) {
            entity.discard();
        }
        return owned.size();
    }
}
