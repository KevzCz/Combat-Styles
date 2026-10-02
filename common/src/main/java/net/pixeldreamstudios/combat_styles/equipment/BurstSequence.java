package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.style.StyleEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BurstSequence {

    private record Active(List<StyleEntry> entries, int played, long expiresAt, int resumeAt) {
    }

    private static final Map<UUID, Active> SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, Active> CLIENT = new ConcurrentHashMap<>();

    private BurstSequence() {
    }

    private static Map<UUID, Active> side(Player player) {
        return player.level().isClientSide() ? CLIENT : SERVER;
    }

    public static void forget(UUID playerId) {
        SERVER.remove(playerId);
        CLIENT.remove(playerId);
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    public static void begin(Player player, List<StyleEntry> entries, int lingerTicks,
                             int resumeAt) {
        if (player == null || entries == null || entries.isEmpty()) {
            return;
        }
        long expiry = player.level().getGameTime()
                + (lingerTicks <= 0 ? StyleSubstitution.DEFAULT_LINGER_TICKS : lingerTicks);
        side(player).put(player.getUUID(),
                new Active(List.copyOf(entries), 0, expiry, resumeAt));
    }

    public static boolean active(Player player) {
        return current(player) != null;
    }

    private static Active current(Player player) {
        if (player == null) {
            return null;
        }
        Map<UUID, Active> side = side(player);
        Active active = side.get(player.getUUID());
        if (active == null) {
            return null;
        }
        if (player.level().getGameTime() > active.expiresAt()) {
            side.remove(player.getUUID());
            return null;
        }
        return active;
    }

    public static StyleEntry peek(Player player) {
        Active active = current(player);
        if (active == null || active.played() >= active.entries().size()) {
            return null;
        }
        return active.entries().get(active.played());
    }

    public static List<StyleEntry> remaining(Player player) {
        Active active = current(player);
        if (active == null) {
            return List.of();
        }
        return new ArrayList<>(active.entries().subList(
                Math.min(active.played(), active.entries().size()), active.entries().size()));
    }

    public static int resumeAt(Player player) {
        Active active = current(player);
        return active == null ? 0 : active.resumeAt();
    }

    public static void advance(Player player) {
        Active active = current(player);
        if (active == null) {
            return;
        }
        int played = active.played() + 1;
        if (played >= active.entries().size()) {
            side(player).remove(player.getUUID());
            return;
        }
        side(player).put(player.getUUID(), new Active(active.entries(), played,
                active.expiresAt(), active.resumeAt()));
    }
}
