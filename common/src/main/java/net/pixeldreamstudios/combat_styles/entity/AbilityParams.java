package net.pixeldreamstudios.combat_styles.entity;

import net.pixeldreamstudios.combat_styles.config.AbilityConfig;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AbilityParams {

    public static final String RAW = "raw";
    public static final String PERCENT = "percent";
    public static final String SECONDS = "seconds";
    public static final String BLOCKS = "blocks";

    private static final Map<String, String> DISPLAY = new LinkedHashMap<>();
    private static final int TICKS_PER_SECOND = 20;

    static {
        display("chance", PERCENT);
        display("lifesteal", PERCENT);
        display("applies_chance", PERCENT);
        display("per_point", PERCENT);
        display("weapon_damage_ratio", PERCENT);
        display("duration_ticks", SECONDS);
        display("windup_ticks", SECONDS);
        display("cooldown_ticks", SECONDS);
        display("radius", BLOCKS);
        display("range", BLOCKS);
        display("rise", BLOCKS);
        display("lift", BLOCKS);
    }

    private AbilityParams() {
    }

    public static void display(String option, String kind) {
        if (option == null || kind == null) {
            return;
        }
        DISPLAY.put(option.toLowerCase(Locale.ROOT), kind);
    }

    public static String displayOf(String option) {
        return DISPLAY.getOrDefault(option == null ? "" : option.toLowerCase(Locale.ROOT), RAW);
    }

    public static String format(String option, double value) {
        return switch (displayOf(option)) {
            case PERCENT -> Math.round(value * 100.0D) + "%";
            case SECONDS -> trim(value / TICKS_PER_SECOND) + "s";
            case BLOCKS -> trim(value) + " blocks";
            default -> trim(value);
        };
    }

    private static String trim(double value) {
        return value == Math.floor(value)
                ? String.valueOf((long) value)
                : String.valueOf(Math.round(value * 100.0D) / 100.0D);
    }

    public static Map<String, Double> of(AbilityConfig.Entry entry) {
        Map<String, Double> params = new LinkedHashMap<>();
        if (entry == null) {
            return params;
        }
        params.put("base_damage", (double) entry.damage.baseDamage);
        params.put("weapon_damage_ratio", (double) entry.damage.weaponDamageRatio);
        params.put("per_point", entry.damage.perPoint);
        params.put("hits", (double) entry.hits());
        params.put("lifesteal", (double) entry.damage.lifesteal);
        params.put("applies_stacks", (double) entry.damage.appliesStacks);
        params.put("applies_chance", (double) entry.damage.appliesChance);
        params.put("duration_ticks", (double) entry.duration());
        params.put("windup_ticks", (double) entry.windup());
        params.put("radius", entry.shape.radius);
        params.put("range", entry.shape.range);
        params.put("rise", entry.shape.rise);
        params.put("pull", entry.shape.pull);
        params.put("lift", entry.shape.lift);
        params.put("max_targets", (double) entry.targets());
        params.putAll(entry.options());
        return Map.copyOf(params);
    }
}
