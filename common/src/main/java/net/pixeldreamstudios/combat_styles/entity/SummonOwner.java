package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class SummonOwner {

    public static final String SUMMON_TAG = "combat_styles_summon";

    private static final double NO_REINFORCEMENTS = 0.0D;
    private static final int MAX_OWNER_DEPTH = 4;

    private SummonOwner() {
    }

    public static boolean isSummon(Entity entity) {
        return entity != null && entity.getTags().contains(SUMMON_TAG);
    }

    public static void stamp(Entity summon, Player owner) {
        if (summon == null || owner == null) {
            return;
        }
        summon.addTag(SUMMON_TAG);
        if (summon instanceof SummonData data && data.combat_styles$summonOwner() == null) {
            data.combat_styles$setSummonOwner(owner);
        }
        if (summon instanceof TamableAnimal tamable) {
            tamable.tame(owner);
        }
        if (summon instanceof Mob mob) {
            settle(mob);
        }
    }

    private static void settle(Mob mob) {
        mob.setPersistenceRequired();
        SummonBrains.holdDigging(mob);
        mob.setCanPickUpLoot(false);
        AttributeInstance reinforcements = mob.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (reinforcements != null) {
            reinforcements.setBaseValue(NO_REINFORCEMENTS);
        }
        if (mob instanceof Zombie zombie) {
            zombie.setCanBreakDoors(false);
        }
        if (mob instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(true);
        }
        if (mob instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
        }
    }

    public static Entity summoningCreatorOf(Entity spawned) {
        if (!(spawned instanceof Mob)) {
            return null;
        }
        Entity creator = spawned instanceof TraceableEntity traceable ? traceable.getOwner() : null;
        if (creator == null && spawned instanceof OwnableEntity ownable
                && ownable.getOwnerUUID() != null
                && spawned.level() instanceof ServerLevel server) {
            creator = server.getEntity(ownable.getOwnerUUID());
        }
        return creator != null && creator != spawned
                && (isSummon(creator) || creator instanceof ConfigurableSummon) ? creator : null;
    }

    public static UUID ownerOf(Entity summon) {
        if (summon == null) {
            return null;
        }
        if (summon instanceof ConfigurableSummon configurable) {
            UUID owned = configurable.summonOwner();
            if (owned != null) {
                return owned;
            }
        }
        if (summon instanceof SummonData data) {
            UUID owned = data.combat_styles$summonOwner();
            if (owned != null) {
                return owned;
            }
        }
        return summon instanceof OwnableEntity ownable ? ownable.getOwnerUUID() : null;
    }

    public static Player ownerPlayer(Entity summon) {
        Level level = summon == null ? null : summon.level();
        UUID id = ownerOf(summon);
        return level == null || id == null ? null : level.getPlayerByUUID(id);
    }

    public static boolean ownedBy(Entity summon, Player player) {
        return player != null && player.getUUID().equals(ownerOf(summon));
    }

    public static UUID boundOwner(Entity entity) {
        if (entity instanceof ConfigurableSummon configurable && configurable.summonOwner() != null) {
            return configurable.summonOwner();
        }
        return entity instanceof SummonData data ? data.combat_styles$summonOwner() : null;
    }

    public static boolean ownerPair(Entity first, Entity second) {
        if (first == null || second == null) {
            return false;
        }
        UUID firstOwner = boundOwner(first);
        if (firstOwner != null && firstOwner.equals(second.getUUID())) {
            return true;
        }
        UUID secondOwner = boundOwner(second);
        return secondOwner != null && secondOwner.equals(first.getUUID());
    }

    public static Entity responsibleSummon(Entity source) {
        Entity current = source;
        for (int depth = 0; current != null && depth < MAX_OWNER_DEPTH; depth++) {
            if (isSummon(current) || current instanceof ConfigurableSummon) {
                return current;
            }
            if (!(current instanceof TraceableEntity traceable)) {
                return null;
            }
            Entity next = traceable.getOwner();
            if (next == current) {
                return null;
            }
            current = next;
        }
        return null;
    }

    public static boolean friendly(Entity source, Entity victim) {
        if (victim == null) {
            return false;
        }
        Entity summon = responsibleSummon(source);
        UUID owner = summon == null ? null : ownerOf(summon);
        if (owner == null || summon == victim) {
            return false;
        }
        return owner.equals(victim.getUUID()) || owner.equals(ownerOf(victim));
    }
}
