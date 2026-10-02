package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerTags {

    private static final String SEPARATOR = ",";

    private static final Map<UUID, Set<String>> SENT = new ConcurrentHashMap<>();
    private static volatile Set<String> mirrored = Set.of();

    private PlayerTags() {
    }

    public static Set<String> of(Player player) {
        if (player == null) {
            return Set.of();
        }
        return player.level().isClientSide() ? mirrored : player.getTags();
    }

    public static boolean consumeChanged(Player player) {
        if (player == null) {
            return false;
        }
        Set<String> now = Set.copyOf(player.getTags());
        Set<String> sent = SENT.getOrDefault(player.getUUID(), Set.of());
        if (now.equals(sent)) {
            return false;
        }
        SENT.put(player.getUUID(), now);
        return true;
    }

    public static String markFor(Player player) {
        return ChargeMirror.TAGS_MARK + String.join(SEPARATOR, player.getTags());
    }

    public static void acceptMark(String mark) {
        String body = mark.substring(ChargeMirror.TAGS_MARK.length());
        mirrored = body.isEmpty() ? Set.of() : Set.of(body.split(SEPARATOR));
    }

    public static void clearMirror() {
        mirrored = Set.of();
    }

    public static void forget(UUID playerId) {
        SENT.remove(playerId);
    }
}
