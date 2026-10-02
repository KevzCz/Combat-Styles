package net.pixeldreamstudios.combat_styles.api;

import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatScheduler {

    public static final int IMMEDIATE = 0;

    private record Task(UUID owner, long dueAt, String label, Runnable action,
                        boolean requiresAlive) {
    }

    private static final Set<Task> PENDING = ConcurrentHashMap.newKeySet();

    private CombatScheduler() {
    }

    public static boolean after(Player player, int delayTicks, String label, Runnable action) {
        return after(player, delayTicks, label, action, true);
    }

    public static boolean after(Player player, int delayTicks, String label, Runnable action,
                                boolean requiresAlive) {
        if (action == null || player == null || player.level().isClientSide()) {
            return false;
        }
        if (delayTicks <= IMMEDIATE) {
            return false;
        }
        PENDING.add(new Task(player.getUUID(),
                player.level().getGameTime() + delayTicks, label, action, requiresAlive));
        return true;
    }

    public static void forget(UUID playerId) {
        PENDING.removeIf(task -> task.owner().equals(playerId));
    }

    public static void clear() {
        PENDING.clear();
    }

    public static int pending() {
        return PENDING.size();
    }

    public static void tick(long gameTime, PlayerLookup lookup) {
        if (PENDING.isEmpty()) {
            return;
        }
        PENDING.removeIf(task -> {
            if (gameTime < task.dueAt()) {
                return false;
            }
            Player player = lookup.find(task.owner());
            if (player == null || (task.requiresAlive() && !player.isAlive())) {
                return true;
            }
            try {
                task.action().run();
            } catch (Throwable throwable) {
                CombatStyles.LOGGER.error("Scheduled task {} failed", task.label(), throwable);
            }
            return true;
        });
    }

    @FunctionalInterface
    public interface PlayerLookup {
        Player find(UUID playerId);
    }
}
