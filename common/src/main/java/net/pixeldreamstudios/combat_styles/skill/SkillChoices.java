package net.pixeldreamstudios.combat_styles.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillChoices {

    public static final String NBT_KEY = "CombatStylesSkillChoices";
    private static final String KEY_OPTION = "option";
    private static final String KEY_SWAPPED_AT = "swapped_at";
    private static final int OUT_OF_COMBAT_TICKS = 100;

    public record Spec(List<String> options, String defaultOption, int swapCooldownTicks, boolean outOfCombat) {

        public Spec {
            options = List.copyOf(options);
            swapCooldownTicks = Math.max(0, swapCooldownTicks);
        }
    }

    public enum Result {
        CHANGED, UNCHANGED, UNKNOWN_CHOICE, UNKNOWN_OPTION, ON_COOLDOWN, IN_COMBAT
    }

    public static final class Data {

        private final Map<String, String> options = new LinkedHashMap<>();
        private final Map<String, Long> swappedAt = new LinkedHashMap<>();

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            options.forEach((choice, option) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString(KEY_OPTION, option);
                entry.putLong(KEY_SWAPPED_AT, swappedAt.getOrDefault(choice, 0L));
                tag.put(choice, entry);
            });
            return tag;
        }

        public void load(CompoundTag tag) {
            options.clear();
            swappedAt.clear();
            for (String choice : tag.getAllKeys()) {
                CompoundTag entry = tag.getCompound(choice);
                options.put(choice, entry.getString(KEY_OPTION));
                swappedAt.put(choice, entry.getLong(KEY_SWAPPED_AT));
            }
        }

        public void copyFrom(Data other) {
            load(other.save());
        }
    }

    public interface Holder {
        Data combat_styles$skillChoices();
    }

    private static final Map<UUID, Map<String, Map<String, Spec>>> SPECS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Runnable>> LISTENERS = new ConcurrentHashMap<>();

    private SkillChoices() {
    }

    public static void define(ServerPlayer player, String choice, String source, Spec spec) {
        Map<String, Map<String, Spec>> specs = SPECS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
        Map<String, Spec> sources = specs.computeIfAbsent(choice, id -> new ConcurrentHashMap<>());
        if (spec == null) {
            sources.remove(source);
            if (sources.isEmpty()) {
                specs.remove(choice);
            }
        } else {
            sources.put(source, spec);
        }
    }

    public static void listen(ServerPlayer player, String key, Runnable listener) {
        Map<String, Runnable> listeners = LISTENERS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
        if (listener == null) {
            listeners.remove(key);
        } else {
            listeners.put(key, listener);
        }
    }

    public static Spec specOf(Player player, String choice) {
        Map<String, Map<String, Spec>> specs = player == null ? null : SPECS.get(player.getUUID());
        Map<String, Spec> sources = specs == null ? null : specs.get(choice);
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        Spec merged = null;
        for (Spec spec : sources.values()) {
            merged = merged == null || spec.swapCooldownTicks() < merged.swapCooldownTicks() ? spec : merged;
        }
        return merged;
    }

    public static List<String> choicesOf(Player player) {
        Map<String, Map<String, Spec>> specs = player == null ? null : SPECS.get(player.getUUID());
        return specs == null ? List.of() : new ArrayList<>(specs.keySet());
    }

    public static String selected(Player player, String choice) {
        Spec spec = specOf(player, choice);
        if (spec == null) {
            return null;
        }
        String stored = dataOf(player) == null ? null : dataOf(player).options.get(choice);
        return stored != null && spec.options().contains(stored) ? stored : spec.defaultOption();
    }

    public static long cooldownLeft(ServerPlayer player, String choice) {
        Spec spec = specOf(player, choice);
        Data data = dataOf(player);
        if (spec == null || data == null || !data.swappedAt.containsKey(choice)) {
            return 0L;
        }
        long ready = data.swappedAt.get(choice) + spec.swapCooldownTicks();
        return Math.max(0L, ready - player.level().getGameTime());
    }

    public static Result select(ServerPlayer player, String choice, String option) {
        Spec spec = specOf(player, choice);
        if (spec == null) {
            return Result.UNKNOWN_CHOICE;
        }
        if (!spec.options().contains(option)) {
            return Result.UNKNOWN_OPTION;
        }
        if (option.equals(selected(player, choice))) {
            return Result.UNCHANGED;
        }
        if (cooldownLeft(player, choice) > 0L) {
            return Result.ON_COOLDOWN;
        }
        if (spec.outOfCombat() && inCombat(player)) {
            return Result.IN_COMBAT;
        }
        Data data = dataOf(player);
        if (data == null) {
            return Result.UNKNOWN_CHOICE;
        }
        data.options.put(choice, option);
        data.swappedAt.put(choice, player.level().getGameTime());
        Map<String, Runnable> listeners = LISTENERS.get(player.getUUID());
        if (listeners != null) {
            for (Runnable listener : new ArrayList<>(listeners.values())) {
                listener.run();
            }
        }
        return Result.CHANGED;
    }

    public static void copy(Player from, Player to) {
        Data source = dataOf(from);
        Data target = dataOf(to);
        if (source != null && target != null) {
            target.copyFrom(source);
        }
    }

    public static void forget(UUID playerId) {
        SPECS.remove(playerId);
        LISTENERS.remove(playerId);
    }

    private static boolean inCombat(ServerPlayer player) {
        int now = player.tickCount;
        return now - player.getLastHurtByMobTimestamp() < OUT_OF_COMBAT_TICKS
                || now - player.getLastHurtMobTimestamp() < OUT_OF_COMBAT_TICKS;
    }

    private static Data dataOf(Player player) {
        return player instanceof Holder holder ? holder.combat_styles$skillChoices() : null;
    }
}
