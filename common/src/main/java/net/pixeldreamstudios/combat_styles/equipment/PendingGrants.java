package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingGrants {

    private static final int EXPIRY_TICKS = 200;

    private record Pending(int swings, long touchedAt) {
    }

    private static final Map<UUID, Map<String, Pending>> GRANTS = new ConcurrentHashMap<>();

    private PendingGrants() {
    }

    public static void forget(UUID playerId) {
        GRANTS.remove(playerId);
    }

    public static void queue(Player player, String affixId, int swings) {
        if (player == null || affixId == null || swings <= 0) {
            return;
        }
        GRANTS.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>())
                .put(affixId, new Pending(swings, player.level().getGameTime()));
    }

    public static List<String> peek(Player player) {
        if (player == null) {
            return List.of();
        }
        Map<String, Pending> pending = GRANTS.get(player.getUUID());
        if (pending == null || pending.isEmpty()) {
            return List.of();
        }
        long now = player.level().getGameTime();
        pending.entrySet().removeIf(entry -> now - entry.getValue().touchedAt() > EXPIRY_TICKS);
        return pending.isEmpty() ? List.of() : List.copyOf(pending.keySet());
    }

    public static void consume(Player player) {
        if (player == null) {
            return;
        }
        Map<String, Pending> pending = GRANTS.get(player.getUUID());
        if (pending == null || pending.isEmpty()) {
            return;
        }
        List<String> spent = new ArrayList<>();
        for (Map.Entry<String, Pending> entry : pending.entrySet()) {
            int left = entry.getValue().swings() - 1;
            if (left <= 0) {
                spent.add(entry.getKey());
            } else {
                entry.setValue(new Pending(left, entry.getValue().touchedAt()));
            }
        }
        spent.forEach(pending::remove);
        if (pending.isEmpty()) {
            GRANTS.remove(player.getUUID());
        }
    }
}
