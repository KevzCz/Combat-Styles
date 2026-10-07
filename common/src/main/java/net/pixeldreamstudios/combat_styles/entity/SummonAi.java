package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.affix.AffixTargets;
import net.pixeldreamstudios.combat_styles.config.SummonTuning;

import java.util.UUID;

public final class SummonAi {

    private SummonAi() {
    }

    public static boolean ownedBy(Entity entity, UUID owner) {
        if (owner == null || !(entity instanceof SummonData data)) {
            return false;
        }
        return owner.equals(data.combat_styles$summonOwner());
    }

    public static boolean friendlyTo(Entity summon, Entity candidate) {
        UUID owner = SummonOwner.ownerOf(summon);
        return owner != null && candidate != null
                && (owner.equals(candidate.getUUID()) || owner.equals(SummonOwner.ownerOf(candidate)));
    }

    public static boolean huntable(Entity summon, LivingEntity candidate) {
        if (candidate == null || !candidate.isAlive() || candidate == summon) {
            return false;
        }
        Player owner = SummonOwner.ownerPlayer(summon);
        if (owner == null || friendlyTo(summon, candidate)) {
            return false;
        }
        if (!AffixTargets.canHit(owner, candidate)) {
            return false;
        }
        double range = SummonTuning.get().huntRange;
        return candidate.distanceToSqr(summon) <= range * range;
    }
}
