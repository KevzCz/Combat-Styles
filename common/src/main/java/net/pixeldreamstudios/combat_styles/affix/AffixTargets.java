package net.pixeldreamstudios.combat_styles.affix;

import net.bettercombat.logic.TargetHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class AffixTargets {

    private AffixTargets() {
    }

    public static boolean canHitFrom(LivingEntity owner, Entity target) {
        if (owner == null || target == null || target == owner) {
            return false;
        }
        if (owner instanceof Player player) {
            return canHit(player, target);
        }
        if (!(target instanceof LivingEntity living)) {
            return false;
        }
        return !owner.isAlliedTo(living);
    }

    public static boolean canHit(Player player, Entity target) {
        if (player == null || target == null || target == player) {
            return false;
        }
        return TargetHelper.isHitAllowed(false, TargetHelper.getRelation(player, target));
    }
}
