package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

public final class SummonAi {

    private static final double FOLLOW_RANGE = 24.0D;

    private SummonAi() {
    }

    public static boolean ownedBy(Entity entity, UUID owner) {
        if (owner == null || !(entity instanceof SummonData data)) {
            return false;
        }
        return owner.equals(data.combat_styles$summonOwner());
    }

    public static boolean huntable(Entity summon, LivingEntity candidate) {
        if (candidate == null || !candidate.isAlive() || candidate == summon) {
            return false;
        }
        UUID owner = summon instanceof SummonData data
                ? data.combat_styles$summonOwner()
                : null;
        if (owner == null) {
            return true;
        }
        if (candidate.getUUID().equals(owner)) {
            return false;
        }
        if (candidate instanceof SummonData other) {
            UUID otherOwner = other.combat_styles$summonOwner();
            if (owner.equals(otherOwner)) {
                return false;
            }
        }
        return candidate.distanceToSqr(summon) <= FOLLOW_RANGE * FOLLOW_RANGE;
    }
}
