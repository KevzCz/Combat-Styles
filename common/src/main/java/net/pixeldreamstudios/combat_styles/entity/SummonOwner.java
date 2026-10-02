package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class SummonOwner {

    public static final String SUMMON_TAG = "combat_styles_summon";

    private SummonOwner() {
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
    }

    public static UUID inheritedOwner(Entity spawned) {
        if (spawned instanceof TraceableEntity traceable) {
            UUID linked = ownerFromSummoner(traceable.getOwner());
            if (linked != null) {
                return linked;
            }
        }
        if (spawned instanceof OwnableEntity ownable) {
            UUID tamed = ownable.getOwnerUUID();
            if (tamed != null) {
                return tamed;
            }
        }
        return null;
    }

    private static UUID ownerFromSummoner(Entity summoner) {
        if (summoner instanceof SummonData data) {
            return data.combat_styles$summonOwner();
        }
        return summoner instanceof Player player ? player.getUUID() : null;
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

}
