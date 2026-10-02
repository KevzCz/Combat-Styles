package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class IFrameState {

    private static final Map<UUID, Long> WINDOWS = new ConcurrentHashMap<>();

    private IFrameState() {
    }

    public static void forget(UUID playerId) {
        WINDOWS.remove(playerId);
    }

    public static void grant(Player player, int durationTicks) {
        if (player == null || durationTicks <= 0) {
            return;
        }
        long expiresAt = player.level().getGameTime() + durationTicks;
        WINDOWS.merge(player.getUUID(), expiresAt, Math::max);
    }

    public static boolean active(Player player) {
        if (player == null) {
            return false;
        }
        Long expiresAt = WINDOWS.get(player.getUUID());
        if (expiresAt == null) {
            return false;
        }
        if (player.level().getGameTime() > expiresAt) {
            WINDOWS.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static int ticksLeft(Player player) {
        if (player == null) {
            return 0;
        }
        Long expiresAt = WINDOWS.get(player.getUUID());
        return expiresAt == null
                ? 0
                : (int) Math.max(0L, expiresAt - player.level().getGameTime());
    }

    public static void tick(long gameTime) {
        WINDOWS.values().removeIf(expiresAt -> gameTime > expiresAt);
    }
}
