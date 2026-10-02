package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DodgeState {

    private record Window(float chance, long expiresAt) {
    }

    private static final Map<UUID, Window> WINDOWS = new ConcurrentHashMap<>();

    private DodgeState() {
    }

    public static void forget(UUID playerId) {
        WINDOWS.remove(playerId);
    }

    public static void grant(Player player, float chance, int durationTicks) {
        if (player == null || chance <= 0.0F || durationTicks <= 0) {
            return;
        }
        WINDOWS.put(player.getUUID(), new Window(chance,
                player.level().getGameTime() + durationTicks));
    }

    public static boolean shouldDodge(Player player) {
        if (player == null) {
            return false;
        }
        Window window = WINDOWS.get(player.getUUID());
        if (window == null) {
            return false;
        }
        if (player.level().getGameTime() > window.expiresAt()) {
            WINDOWS.remove(player.getUUID());
            return false;
        }
        return player.getRandom().nextFloat() < window.chance();
    }

    public static void tick(long gameTime) {
        WINDOWS.values().removeIf(window -> gameTime > window.expiresAt());
    }
}
