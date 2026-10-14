package net.pixeldreamstudios.combat_styles.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillCounters {

    public record Spec(int max, int decayTicks, boolean resetAtMax) {

        public Spec {
            max = Math.max(1, max);
            decayTicks = Math.max(0, decayTicks);
        }

        public Spec mergedWith(Spec other) {
            if (other == null) {
                return this;
            }
            return new Spec(Math.max(max, other.max), Math.max(decayTicks, other.decayTicks),
                    resetAtMax || other.resetAtMax);
        }
    }

    private record Value(int amount, long changedAt) {
    }

    private static final Map<UUID, Map<String, Map<String, Spec>>> SPECS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Value>> VALUES = new ConcurrentHashMap<>();

    private SkillCounters() {
    }

    public static void define(ServerPlayer player, String counter, String source, Spec spec) {
        Map<String, Map<String, Spec>> specs = SPECS.computeIfAbsent(player.getUUID(),
                id -> new ConcurrentHashMap<>());
        Map<String, Spec> sources = specs.computeIfAbsent(counter, id -> new ConcurrentHashMap<>());
        if (spec == null) {
            sources.remove(source);
            if (sources.isEmpty()) {
                specs.remove(counter);
                reset(player, counter);
            }
        } else {
            sources.put(source, spec);
        }
    }

    public static Spec specOf(Player player, String counter) {
        Map<String, Map<String, Spec>> specs = player == null ? null : SPECS.get(player.getUUID());
        Map<String, Spec> sources = specs == null ? null : specs.get(counter);
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        Spec merged = null;
        for (Spec spec : sources.values()) {
            merged = merged == null ? spec : merged.mergedWith(spec);
        }
        return merged;
    }

    public static int get(Player player, String counter) {
        Map<String, Value> values = player == null ? null : VALUES.get(player.getUUID());
        Value value = values == null ? null : values.get(counter);
        return value == null ? 0 : value.amount();
    }

    public static void add(ServerPlayer player, String counter, int amount) {
        Spec spec = specOf(player, counter);
        if (spec == null || amount == 0) {
            return;
        }
        Map<String, Value> values = VALUES.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
        int before = get(player, counter);
        int after = Math.max(0, Math.min(spec.max(), before + amount));
        values.put(counter, new Value(after, player.level().getGameTime()));
        if (after >= spec.max() && before < spec.max()) {
            SkillEvents.counterMax(player, counter);
            if (spec.resetAtMax()) {
                reset(player, counter);
            }
        }
    }

    public static void reset(ServerPlayer player, String counter) {
        Map<String, Value> values = VALUES.get(player.getUUID());
        if (values != null) {
            values.remove(counter);
        }
    }

    public static void tick(ServerPlayer player) {
        Map<String, Value> values = VALUES.get(player.getUUID());
        if (values == null || values.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        for (Map.Entry<String, Value> entry : Map.copyOf(values).entrySet()) {
            Spec spec = specOf(player, entry.getKey());
            if (spec == null) {
                values.remove(entry.getKey());
            } else if (spec.decayTicks() > 0 && now - entry.getValue().changedAt() >= spec.decayTicks()) {
                values.remove(entry.getKey());
            }
        }
    }

    public static void forget(UUID playerId) {
        SPECS.remove(playerId);
        VALUES.remove(playerId);
    }
}
