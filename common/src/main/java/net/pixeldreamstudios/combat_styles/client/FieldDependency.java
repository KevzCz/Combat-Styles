package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.equipment.CastGate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum FieldDependency {
    TARGET("target", "◆", 0xFFD9A959),
    PREFERS_TARGET("prefers_target", "◇", 0xFFB0A070),
    ABILITY("ability", "◆", 0xFF8FB3D9),
    SUMMON("summon", "◆", 0xFF9FD98F),
    CHARGE("charge", "◆", 0xFFD98F8F),
    WINDOW("window", "◆", 0xFFC79FD9),
    ANIMATION("animation", "◆", 0xFF9FD9C7),
    HIT("hit", "◆", 0xFFD9B38F);

    private static final String LABEL_PREFIX = "dependency.combat_styles.";
    private static final String PROVIDES_PREFIX = "provides.combat_styles.";
    private static final String PROVIDES_MARKER = "▲";

    private static final Map<String, Set<FieldDependency>> BY_KEY = new LinkedHashMap<>();
    private static final Map<String, Set<FieldDependency>> PROVIDED_BY = new LinkedHashMap<>();
    private static final Map<String, Set<String>> BLOCKED_UNDER = new LinkedHashMap<>();
    private static final Map<String, Map<FieldDependency, String>> SCOPED = new LinkedHashMap<>();

    static {
        bind(TARGET,
                "target_health_above", "target_health_below", "behind_target",
                "target_above", "target_below",
                "of_target_max_health", "of_target_missing_health",
                "disarm", "freeze_ticks", "cleave",
                "on_connect_ratio", "on_connect_knockback");
        bind(PREFERS_TARGET,
                "toward_target", "inherit_target", "aim_at_target",
                "from_target", "anchor", "hits_attacker");
        bind(ABILITY, "ability", "abilities", "swaps", "finale");
        bind(SUMMON,
                "max_diving", "damage_share", "anchored", "max_active",
                "replaced_summoned_entities", "replaces_summons",
                "become_entity", "exempt", "on_summon_hit");
        bind(CHARGE, "set");
        bind(WINDOW, "uses", "duration", "refresh_on_use");
        blockUnder("duration", "shield", "status_effects", "self_effects", "attributes");
        provide(WINDOW, "on_hurt", "on_kill", "on_miss", "on_summon_hit",
                "on_shoot", "on_shield_block", "on_use", "on_projectile_hit", CastGate.KEY);
        provide(ANIMATION, "on_use");
        bind(ANIMATION, "channel_animation", "release_animation");
        provide(TARGET, "on_kill", "on_summon_hit", "on_player_hit", "on_projectile_hit",
                "on_hurt", "on_shield_block", CastGate.KEY);
        provide(SUMMON, "on_summon_hit");
        bindUnder(HIT, EffectSchema.ROOT_TRIGGER,
                "damage_above", "damage_below", "damage_type", "target");
        blockUnder("damage_above", "on_hurt", "on_projectile_hit");
        blockUnder("damage_below", "on_hurt", "on_projectile_hit");
        blockUnder("damage_type", "on_projectile_hit");
        blockUnder("target", "on_projectile_hit");
        provide(HIT, "on_player_hit", "on_projectile_hit");
    }

    private final String id;
    private final String marker;
    private final int color;

    FieldDependency(String id, String marker, int color) {
        this.id = id;
        this.marker = marker;
        this.color = color;
    }

    private static void bind(FieldDependency dependency, String... keys) {
        for (String key : keys) {
            BY_KEY.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(dependency);
        }
    }

    private static void bindUnder(FieldDependency dependency, String scope, String... keys) {
        for (String key : keys) {
            SCOPED.computeIfAbsent(key, ignored -> new LinkedHashMap<>()).put(dependency, scope);
        }
    }

    private static void blockUnder(String key, String... segments) {
        BLOCKED_UNDER.computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                .addAll(List.of(segments));
    }

    private static void provide(FieldDependency dependency, String... keys) {
        for (String key : keys) {
            PROVIDED_BY.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(dependency);
        }
    }

    public record Marker(String glyph, int color) {
    }

    public static List<Marker> markers(String path, String key) {
        List<Marker> found = new ArrayList<>();
        for (FieldDependency dependency : of(path, key)) {
            found.add(new Marker(dependency.marker(), dependency.color()));
        }
        for (FieldDependency provided : providedBy(key)) {
            found.add(new Marker(provided.providesMarker(), provided.color()));
        }
        return found;
    }

    public static List<Component> lines(String path, String key) {
        List<Component> found = new ArrayList<>();
        for (FieldDependency dependency : of(path, key)) {
            found.add(Component.literal(dependency.marker() + " ")
                    .withColor(dependency.color())
                    .append(dependency.label().copy().withStyle(ChatFormatting.GRAY)));
        }
        for (FieldDependency provided : providedBy(key)) {
            found.add(Component.literal(provided.providesMarker() + " ")
                    .withColor(provided.color())
                    .append(provided.providesLabel(key).copy().withStyle(ChatFormatting.GRAY)));
        }
        return found;
    }

    public String providesMarker() {
        return PROVIDES_MARKER;
    }

    public Component providesLabel() {
        return Component.translatable(PROVIDES_PREFIX + id);
    }

    public Component providesLabel(String key) {
        if (key == null || key.isBlank()) {
            return providesLabel();
        }
        return Component.translatableWithFallback(PROVIDES_PREFIX + id + "." + key,
                providesLabel().getString());
    }

    public static List<FieldDependency> providedBy(String key) {
        if (key == null) {
            return List.of();
        }
        Set<FieldDependency> found = PROVIDED_BY.get(key);
        return found == null ? List.of() : List.copyOf(found);
    }

    public String id() {
        return id;
    }

    public String marker() {
        return marker;
    }

    public int color() {
        return color;
    }

    public Component label() {
        return Component.translatable(LABEL_PREFIX + id);
    }

    public static List<FieldDependency> of(String key) {
        if (key == null) {
            return List.of();
        }
        Set<FieldDependency> found = BY_KEY.get(key);
        return found == null ? List.of() : List.copyOf(found);
    }

    public static List<FieldDependency> of(String path, String key) {
        if (key == null) {
            return List.of();
        }
        Set<String> blocked = BLOCKED_UNDER.get(key);
        if (blocked != null && path != null) {
            for (String segment : blocked) {
                if (path.contains(segment)) {
                    return List.of();
                }
            }
        }
        Map<FieldDependency, String> scoped = SCOPED.get(key);
        if (scoped == null || path == null) {
            return of(key);
        }
        Set<FieldDependency> found = new LinkedHashSet<>(of(key));
        for (Map.Entry<FieldDependency, String> entry : scoped.entrySet()) {
            if (path.startsWith(entry.getValue())) {
                found.add(entry.getKey());
            }
        }
        return List.copyOf(found);
    }

    public static boolean any(String key) {
        return !of(key).isEmpty();
    }
}
