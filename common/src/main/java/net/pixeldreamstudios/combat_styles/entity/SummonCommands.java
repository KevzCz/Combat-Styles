package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

public final class SummonCommands {

    private static final double FOCUS_RANGE = 48.0D;
    private static final double RECALL_RADIUS = 2.0D;
    private static final double FULL_TURN = Math.PI * 2.0D;

    private SummonCommands() {
    }

    public static int recall(ServerPlayer player) {
        int recalled = 0;
        List<Entity> summons = SummonRoster.all(player);
        for (Entity summon : summons) {
            if (summon instanceof ConfigurableSummon) {
                continue;
            }
            double angle = FULL_TURN * recalled / Math.max(1, summons.size());
            summon.teleportTo(player.getX() + Math.cos(angle) * RECALL_RADIUS, player.getY(),
                    player.getZ() + Math.sin(angle) * RECALL_RADIUS);
            if (summon instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
            recalled++;
        }
        return recalled;
    }

    public static int focus(ServerPlayer player, Entity target) {
        if (!(target instanceof LivingEntity living) || !living.isAlive()
                || living.distanceToSqr(player) > FOCUS_RANGE * FOCUS_RANGE) {
            return 0;
        }
        int sent = 0;
        for (Entity summon : SummonRoster.all(player)) {
            if (!SummonAi.huntable(summon, living)) {
                continue;
            }
            if (summon instanceof ConfigurableSummon configurable) {
                configurable.retarget(living);
            } else if (summon instanceof Mob mob) {
                mob.setTarget(living);
            } else {
                continue;
            }
            sent++;
        }
        return sent;
    }
}
