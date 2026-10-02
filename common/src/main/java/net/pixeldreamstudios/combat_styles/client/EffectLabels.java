package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.StyleText;

public final class EffectLabels {

    private static final String FIELD_PREFIX = "field.combat_styles.";
    private static final String HINT_PREFIX = "hint.combat_styles.";
    private static final String ROOT_MARK = "@";
    private static final String SCOPE_JOIN = ".";

    private EffectLabels() {
    }

    public static Component labelFor(String key) {
        if (key == null || key.isBlank()) {
            return Component.empty();
        }
        if (Character.isDigit(key.charAt(0))) {
            return Component.literal(key);
        }
        if (key.indexOf(':') >= 0) {
            return StyleText.choiceName(key);
        }
        return Component.translatableWithFallback(FIELD_PREFIX + key,
                StyleText.prettify(key));
    }

    public static Component labelFor(String path, String key) {
        String scoped = scopedKey(FIELD_PREFIX, path, key);
        return scoped == null ? labelFor(key) : Component.translatable(scoped);
    }

    public static Component hintFor(String path, String key) {
        String scoped = scopedKey(HINT_PREFIX, path, key);
        return scoped == null ? hintFor(key) : Component.translatable(scoped);
    }

    public static Component hintFor(String key) {
        if (key == null || key.isBlank() || Character.isDigit(key.charAt(0))) {
            return null;
        }
        String translation = HINT_PREFIX + key;
        Component hint = Component.translatable(translation);
        return hint.getString().equals(translation) ? null : hint;
    }

    private static String scopedKey(String prefix, String path, String key) {
        if (path == null || key == null || key.isBlank() || Character.isDigit(key.charAt(0))) {
            return null;
        }
        String[] segments = path.split("/");
        for (int index = segments.length - 2; index >= 0; index--) {
            String scope = segments[index];
            if (scope.isEmpty() || Character.isDigit(scope.charAt(0))) {
                continue;
            }
            String candidate = prefix + scope.replace(ROOT_MARK, "") + SCOPE_JOIN + key;
            if (Language.getInstance().has(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
