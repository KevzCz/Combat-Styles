package net.pixeldreamstudios.combat_styles.api;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SchemaRegistry {

    public record Entry(String path, String key, JsonElement sample, List<String> choices) {

        public Entry {
            choices = choices == null ? List.of() : List.copyOf(choices);
        }
    }

    public static final String SCOPE_EFFECT = "";
    public static final String SCOPE_TRIGGER = "@trigger";
    public static final String SCOPE_SUBSTITUTION = "@substitution";
    public static final String SCOPE_SUMMON_REPLACE = "@summon_replace";
    public static final String SCOPE_SUMMON_PROFILE = "@summon_profile";
    public static final String SCOPE_SUMMON_DIVE = "@summon_dive";
    public static final String SCOPE_BEHAVIOUR = "@behaviour";
    public static final String SCOPE_TUNING = "@tuning";
    public static final String SCOPE_ECHO = "@echo";

    private static final Map<String, List<Entry>> ENTRIES = new LinkedHashMap<>();
    private static final Map<String, List<String>> CHOICES = new LinkedHashMap<>();

    private SchemaRegistry() {
    }

    public static <T> void registerField(String scope, String key, Codec<T> codec, T sample) {
        registerField(scope, key, codec, sample, List.of());
    }

    public static <T> void registerField(String scope, String key, Codec<T> codec, T sample,
                                         List<String> choices) {
        if (key == null || key.isBlank() || codec == null) {
            return;
        }
        JsonElement encoded = codec.encodeStart(JsonOps.INSTANCE, sample)
                .resultOrPartial(error -> CombatStyles.LOGGER.error(
                        "Schema field {} could not be encoded: {}", key, error))
                .orElse(null);
        if (encoded == null) {
            return;
        }
        ENTRIES.computeIfAbsent(scope == null ? SCOPE_EFFECT : scope,
                ignored -> new ArrayList<>()).add(new Entry(scope, key, encoded, choices));
    }

    public static void registerChoices(String key, List<String> choices) {
        if (key == null || key.isBlank() || choices == null || choices.isEmpty()) {
            return;
        }
        CHOICES.computeIfAbsent(key, ignored -> new ArrayList<>()).addAll(choices);
    }

    public static List<Entry> fieldsFor(String scope) {
        return List.copyOf(ENTRIES.getOrDefault(scope == null ? SCOPE_EFFECT : scope, List.of()));
    }

    public static List<String> choicesFor(String key) {
        return List.copyOf(CHOICES.getOrDefault(key, List.of()));
    }

    public static boolean hasChoices(String key) {
        return CHOICES.containsKey(key);
    }
}
