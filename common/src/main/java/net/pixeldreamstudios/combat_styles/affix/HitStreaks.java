package net.pixeldreamstudios.combat_styles.affix;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HitStreaks {

    private record Entry(Set<UUID> lastTargets, Set<UUID> currentTargets,
                         Map<UUID, Integer> streaks, long lastHitTick) {
    }

    private static final Map<UUID, Entry> STREAKS = new ConcurrentHashMap<>();

    private HitStreaks() {
    }

    public static void forget(UUID playerId) {
        STREAKS.remove(playerId);
    }

    public static void expire(long gameTime) {
        long timeout = CombatStylesConfig.get().hitHistoryTimeout();
        STREAKS.values().removeIf(entry -> gameTime - entry.lastHitTick() > timeout);
    }

    public static void beginSwing(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        Entry entry = activeEntry(player);
        if (entry == null || entry.currentTargets().isEmpty()) {
            return;
        }
        Set<UUID> rolled = new LinkedHashSet<>(entry.currentTargets());
        Map<UUID, Integer> kept = new LinkedHashMap<>();
        for (UUID id : rolled) {
            Integer streak = entry.streaks().get(id);
            kept.put(id, streak == null ? 1 : streak);
        }
        STREAKS.put(player.getUUID(),
                new Entry(rolled, new LinkedHashSet<>(), kept, entry.lastHitTick()));
    }

    public static void record(Player player, LivingEntity target) {
        if (player == null || target == null || player.level().isClientSide()) {
            return;
        }
        UUID targetId = target.getUUID();
        Entry previous = activeEntry(player);
        Set<UUID> last = previous == null ? Set.of() : previous.lastTargets();
        Set<UUID> current = previous == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(previous.currentTargets());
        Map<UUID, Integer> streaks = previous == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(previous.streaks());

        if (current.add(targetId)) {
            int streak = last.contains(targetId)
                    ? streaks.getOrDefault(targetId, 0) + 1
                    : 1;
            streaks.put(targetId, streak);
        }
        streaks.keySet().retainAll(union(last, current));

        STREAKS.put(player.getUUID(), new Entry(
                previous == null ? new LinkedHashSet<>() : previous.lastTargets(),
                current, streaks, player.level().getGameTime()));
    }

    public static int streakOf(Player player) {
        Entry entry = activeEntry(player);
        if (entry == null) {
            return 0;
        }
        int best = 0;
        for (int streak : entry.streaks().values()) {
            best = Math.max(best, streak);
        }
        return best;
    }

    public static int streakOf(Player player, LivingEntity target) {
        Entry entry = activeEntry(player);
        if (entry == null || target == null) {
            return 0;
        }
        return entry.streaks().getOrDefault(target.getUUID(), 0);
    }

    public static boolean isSameTarget(Player player, LivingEntity target) {
        Entry entry = activeEntry(player);
        return entry != null && target != null
                && entry.lastTargets().contains(target.getUUID());
    }

    private static Set<UUID> union(Set<UUID> left, Set<UUID> right) {
        Set<UUID> both = new LinkedHashSet<>(left);
        both.addAll(right);
        return both;
    }

    private static Entry activeEntry(Player player) {
        Entry entry = STREAKS.get(player.getUUID());
        if (entry == null) {
            return null;
        }
        long elapsed = player.level().getGameTime() - entry.lastHitTick();
        if (elapsed > CombatStylesConfig.get().hitHistoryTimeout()) {
            STREAKS.remove(player.getUUID());
            return null;
        }
        return entry;
    }
}
