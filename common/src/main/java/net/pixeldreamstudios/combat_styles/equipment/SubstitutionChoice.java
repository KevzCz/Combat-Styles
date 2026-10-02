package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SubstitutionChoice {

    private static final String SEPARATOR = "|";

    private static final Map<String, String> CHOSEN = new ConcurrentHashMap<>();
    private static final Map<String, String> MIRRORED = new ConcurrentHashMap<>();

    private SubstitutionChoice() {
    }

    private static String keyOf(Player player, String itemId) {
        return player.getUUID() + SEPARATOR + itemId;
    }

    public static void record(Player player, String itemId, String effectKey) {
        if (player == null || itemId == null || player.level().isClientSide()) {
            return;
        }
        String key = keyOf(player, itemId);
        if (effectKey == null) {
            CHOSEN.remove(key);
            return;
        }
        CHOSEN.put(key, effectKey);
    }

    public static String of(Player player, String itemId) {
        if (player == null || itemId == null) {
            return null;
        }
        return player.level().isClientSide()
                ? MIRRORED.get(itemId)
                : CHOSEN.get(keyOf(player, itemId));
    }

    public static void accept(Map<String, String> choices) {
        MIRRORED.keySet().retainAll(choices.keySet());
        MIRRORED.putAll(choices);
    }

    public static Map<String, String> snapshot(Player player) {
        if (player == null) {
            return Map.of();
        }
        String prefix = player.getUUID() + SEPARATOR;
        Map<String, String> owned = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : CHOSEN.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                owned.put(entry.getKey().substring(prefix.length()), entry.getValue());
            }
        }
        return owned;
    }

    public static void forget(UUID playerId) {
        String prefix = playerId + SEPARATOR;
        CHOSEN.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public static void clear() {
        MIRRORED.clear();
    }
}
