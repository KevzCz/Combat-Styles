package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.CombatEffectDealer;
import net.pixeldreamstudios.combat_styles.affix.AffixImpacts;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.EquipmentAffix;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.custom.CustomScopes;
import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.function.Predicate;

public final class EquipmentActivation {

    private static final int OWNER_DEPTH = 4;

    private static final Set<String> FIRING = new HashSet<>();
    private static final ThreadLocal<Entity> CURRENT_SHOT = new ThreadLocal<>();
    private static final ThreadLocal<UseEvent> CURRENT_USE = new ThreadLocal<>();

    private EquipmentActivation() {
    }

    public static void shot(Player player, Entity projectile, boolean viaSummon) {
        if (projectile == null || ProjectileCopies.isCopy(projectile)) {
            return;
        }
        ItemStack weapon = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        ShotEvent event = new ShotEvent(projectile, weapon, viaSummon);
        CURRENT_SHOT.set(projectile);
        try {
            react(player, EquipmentContext.shotBy(player, event),
                    trigger -> trigger.activation().reactsToShoot(), "shot");
        } finally {
            CURRENT_SHOT.remove();
        }
    }

    public static Entity currentShot() {
        return CURRENT_SHOT.get();
    }

    public static void blocked(Player player, BlockEvent event) {
        if (event == null) {
            return;
        }
        react(player, EquipmentContext.blockedBy(player, event),
                trigger -> trigger.activation().reactsToBlock(), "shield block");
    }

    public static void used(Player player, UseEvent event) {
        if (event == null) {
            return;
        }
        CURRENT_USE.set(event);
        try {
            react(player, EquipmentContext.usedBy(player, event),
                    trigger -> trigger.activation().usesItem(), "use");
        } finally {
            CURRENT_USE.remove();
        }
    }

    public static UseEvent currentUse() {
        return CURRENT_USE.get();
    }

    public static void cast(Player player, SpellCastEvent event) {
        if (event == null) {
            return;
        }
        react(player, EquipmentContext.castBy(player, event),
                trigger -> trigger.activation().reactsToCast(), "spell cast");
    }

    public static void summoned(Player player, SummonEvent event) {
        if (event == null) {
            return;
        }
        react(player, EquipmentContext.summonedBy(player, event),
                trigger -> trigger.activation().summon().hears(event.kind()),
                "summon " + event.kind().name().toLowerCase(Locale.ROOT));
    }

    public static void projectileHit(Player player, ProjectileHitEvent event) {
        if (event == null) {
            return;
        }
        react(player, EquipmentContext.projectileHitBy(player, event),
                trigger -> trigger.activation().reactsToProjectileHit(), "projectile hit");
    }

    public static Player ownerOf(Entity projectile) {
        if (!(projectile instanceof Projectile shot)) {
            return null;
        }
        Entity owner = shot.getOwner();
        for (int depth = 0; owner != null && depth < OWNER_DEPTH; depth++) {
            if (owner instanceof Player player) {
                return player;
            }
            owner = owner instanceof OwnableEntity owned ? owned.getOwner() : null;
        }
        return null;
    }

    private static void react(Player player, EquipmentContext context,
                              Predicate<EquipmentTrigger> wanted, String label) {
        if (player == null || player.level().isClientSide() || FIRING.contains(label)) {
            return;
        }
        if (EquipmentRegistry.isEmpty() || EquipmentProviders.isEmpty()) {
            return;
        }
        FIRING.add(label);
        try {
            EventKind kind = EventKind.activationOf(context);
            List<EquipmentRuntime.Match> heard = new ArrayList<>();
            for (EquipmentRuntime.Match match : EquipmentRuntime.matching(player, context)) {
                if (match.hears(wanted) || match.effect().events().has(kind)) {
                    heard.add(match);
                }
            }
            for (EquipmentRuntime.Match match : EquipmentRuntime.pickGroups(player, context, heard)) {
                List<AffixEffects.OnHit> payloads = match.effect().events().get(kind);
                boolean arming = EffectWindows.arms(match)
                        && match.trigger().eventFiring(context);
                boolean hooked = Customs.has(match.entry()) || Customs.has(match.effect());
                if (!arming && payloads.isEmpty()
                        && match.entry().selfEffects().isEmpty() && !hooked) {
                    continue;
                }
                try {
                    if (arming) {
                        if (EffectWindows.arm(player, match)) {
                            if (CombatDebug.enabled(player)) {
                                CombatDebug.fired(player, match.id(), "armed on " + label);
                            }
                            sync(player, match);
                        }
                        if (payloads.isEmpty() && !hooked) {
                            continue;
                        }
                    }
                    if (!EffectWindows.claim(player, match)) {
                        continue;
                    }
                    StyleAffix affix = EquipmentAffix.of(match);
                    if (CombatDebug.enabled(player)) {
                        CombatDebug.fired(player, match.id(), "procced on " + label);
                    }
                    sync(player, match);
                    LivingEntity target = context.target();
                    ItemStack owner = match.stackOf(player);
                    float damage = context.reactionDamage()
                            * EffectStacking.multiplier(player, match.entry(), owner);
                    Runnable fire = () -> {
                        Customs.apply(CustomScopes.TRIGGERED_EFFECT, match.entry(), player, player, target);
                        Customs.apply(CustomScopes.EQUIPMENT_EFFECT, match.effect(), player, player, target);
                        AffixImpacts.applySelfEffects(player, match.entry().selfEffects());
                        for (AffixEffects.OnHit payload : payloads) {
                            CombatEffectDealer.applyHurtReaction(player, target, affix,
                                    payload, damage);
                        }
                    };
                    EffectStacking.fire(player, match.entry(), owner, match.id(), fire);
                } catch (Throwable throwable) {
                    CombatStyles.LOGGER.error("Failed to apply {} reaction {}",
                            label, match.id(), throwable);
                }
            }
        } finally {
            FIRING.remove(label);
        }
    }

    private static void sync(Player player, EquipmentRuntime.Match match) {
        if (player instanceof ServerPlayer served) {
            NetworkManager.syncCooldowns(served, match.itemId());
        }
    }
}
