package net.pixeldreamstudios.combat_styles.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OptionSources {

    public static final String KEY_OPTIONS = "options";
    public static final String SCOPE_SELF = "";
    private static final String PATH_SEPARATOR = "/";

    private static final Map<String, ChoiceSource> SOURCES = new LinkedHashMap<>();

    private OptionSources() {
    }

    public static void register(ChoiceSource source) {
        if (source != null && source.selector() != null) {
            SOURCES.put(source.selector(), source);
        }
    }

    public static ChoiceSource get(String selector) {
        return selector == null ? null : SOURCES.get(selector);
    }

    public static List<String> selectors() {
        return List.copyOf(SOURCES.keySet());
    }

    public static List<String> values(String selector) {
        ChoiceSource source = get(selector);
        List<String> values = source == null ? null : source.values();
        return values == null ? List.of() : values;
    }

    public static List<OptionField> fieldsFor(String selector, String value) {
        ChoiceSource source = get(selector);
        if (source == null || value == null || value.isBlank()) {
            return List.of();
        }
        List<OptionField> found = source.options(value);
        return found == null ? List.of() : found;
    }

    public static JsonObject prefilled(String selector, String value) {
        JsonObject filled = new JsonObject();
        for (OptionField field : fieldsFor(selector, value)) {
            filled.add(field.key(), field.sample().deepCopy());
        }
        ChoiceSource source = get(selector);
        JsonObject configured = source == null || value == null ? null : source.optionDefaults(value);
        if (configured != null) {
            for (Map.Entry<String, JsonElement> entry : configured.entrySet()) {
                if (filled.has(entry.getKey())) {
                    filled.add(entry.getKey(), entry.getValue().deepCopy());
                }
            }
        }
        return filled;
    }

    public static String mountOf(String key) {
        if (key == null) {
            return null;
        }
        for (ChoiceSource source : SOURCES.values()) {
            for (String value : values(source.selector())) {
                for (OptionField field : fieldsFor(source.selector(), value)) {
                    if (key.equals(field.key()) && field.mount() != null) {
                        return field.mount();
                    }
                }
            }
        }
        return null;
    }

    public static JsonElement template(String selector, String value, String scope, String key) {
        ChoiceSource source = get(selector);
        if (source == null || value == null || value.isBlank() || key == null) {
            return null;
        }
        JsonElement made = source.template(value, scope == null ? SCOPE_SELF : scope, key);
        return made == null ? null : made.deepCopy();
    }

    public static List<String> allowed(String selector, String value, String scope) {
        ChoiceSource source = get(selector);
        if (source == null || value == null || value.isBlank()) {
            return null;
        }
        return source.allowed(value, scope == null ? SCOPE_SELF : scope);
    }

    public static boolean permitted(List<String> allowed, String path) {
        if (allowed == null) {
            return true;
        }
        String node = path.startsWith(PATH_SEPARATOR) ? path.substring(1) : path;
        for (String entry : allowed) {
            if (node.equals(entry) || node.startsWith(entry + PATH_SEPARATOR)
                    || entry.startsWith(node + PATH_SEPARATOR)) {
                return true;
            }
        }
        return false;
    }

    public static JsonObject pruned(JsonObject source, List<String> allowed) {
        return pruned(source, allowed, "");
    }

    private static JsonObject pruned(JsonObject source, List<String> allowed, String prefix) {
        JsonObject kept = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + PATH_SEPARATOR + entry.getKey();
            if (!permitted(allowed, path)) {
                continue;
            }
            JsonElement value = entry.getValue();
            kept.add(entry.getKey(), value.isJsonObject()
                    ? pruned(value.getAsJsonObject(), allowed, path)
                    : value.deepCopy());
        }
        return kept;
    }
}
