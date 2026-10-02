package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.network.chat.Component;

public final class SchemaError {

    private static final String KEY_MISSING = "screen.combat_styles.error_missing";
    private static final String KEY_MISSING_WHY = "screen.combat_styles.error_missing_why";
    private static final String KEY_DROPPED = "screen.combat_styles.error_dropped";
    private static final String KEY_DROPPED_WHY = "screen.combat_styles.error_dropped_why";
    private static final String KEY_TYPE = "screen.combat_styles.error_type";
    private static final String KEY_RANGE = "screen.combat_styles.error_range";

    private static final String NO_KEY = "No key ";
    private static final String NOT_A = "Not a ";
    private static final String VALUE = "Value ";
    private static final String OUTSIDE = " outside of range [";
    private static final String RANGE_SPLIT = ":";
    private static final String RANGE_END = "]";
    private static final String MAP_NOISE = " in MapLike";
    private static final String VALUE_SPLIT = ": ";

    private SchemaError() {
    }

    public static Component causeOf(String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        String field = missingField(raw);
        if (field != null) {
            return Component.translatable(KEY_MISSING, field);
        }
        String[] range = rangeOf(raw);
        if (range != null) {
            return Component.translatable(KEY_RANGE, range[0], range[1], range[2]);
        }
        int typeAt = raw.indexOf(NOT_A);
        if (typeAt >= 0) {
            String rest = raw.substring(typeAt + NOT_A.length());
            int split = rest.indexOf(VALUE_SPLIT);
            String expected = split < 0 ? rest : rest.substring(0, split);
            String got = split < 0 ? "" : rest.substring(split + VALUE_SPLIT.length());
            return Component.translatable(KEY_TYPE, expected, got);
        }
        return Component.literal(trimNoise(raw));
    }

    public static Component whyOf(String raw) {
        return raw != null && missingField(raw) != null
                ? Component.translatable(KEY_MISSING_WHY)
                : null;
    }

    public static Component droppedCause(String fields) {
        return Component.translatable(KEY_DROPPED, fields);
    }

    public static Component droppedWhy() {
        return Component.translatable(KEY_DROPPED_WHY);
    }

    private static String missingField(String raw) {
        int at = raw.indexOf(NO_KEY);
        if (at < 0) {
            return null;
        }
        String rest = raw.substring(at + NO_KEY.length());
        int end = rest.indexOf(' ');
        return end < 0 ? rest : rest.substring(0, end);
    }

    private static String[] rangeOf(String raw) {
        int valueAt = raw.indexOf(VALUE);
        int outsideAt = raw.indexOf(OUTSIDE);
        if (valueAt < 0 || outsideAt < valueAt) {
            return null;
        }
        String value = raw.substring(valueAt + VALUE.length(), outsideAt);
        String bounds = raw.substring(outsideAt + OUTSIDE.length());
        int end = bounds.indexOf(RANGE_END);
        int split = bounds.indexOf(RANGE_SPLIT);
        if (end < 0 || split < 0 || split > end) {
            return null;
        }
        return new String[]{value, bounds.substring(0, split), bounds.substring(split + 1, end)};
    }

    private static String trimNoise(String raw) {
        int at = raw.indexOf(MAP_NOISE);
        return at < 0 ? raw : raw.substring(0, at);
    }
}
