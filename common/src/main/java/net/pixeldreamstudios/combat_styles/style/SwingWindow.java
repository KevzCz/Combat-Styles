package net.pixeldreamstudios.combat_styles.style;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SwingWindow {

    private record Window(long id) {
    }

    private record Key(UUID player, boolean client) {
    }

    private static final Map<Key, Window> WINDOWS = new ConcurrentHashMap<>();

    private SwingWindow() {
    }

    public static void beginSwing(Player player) {
        if (player == null) {
            return;
        }
        Key id = keyOf(player);
        Window window = WINDOWS.get(id);
        WINDOWS.put(id, new Window(window == null ? 1L : window.id() + 1L));
    }

    public static long current(Player player, long now) {
        if (player == null) {
            return 0L;
        }
        Window window = WINDOWS.get(keyOf(player));
        return window == null ? 0L : window.id();
    }

    public static void forget(UUID playerId) {
        WINDOWS.keySet().removeIf(key -> key.player().equals(playerId));
    }

    private static Key keyOf(Player player) {
        return new Key(player.getUUID(), player.level().isClientSide());
    }
}
