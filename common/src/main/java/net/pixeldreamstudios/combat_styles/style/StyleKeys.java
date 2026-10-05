package net.pixeldreamstudios.combat_styles.style;

import java.util.ArrayList;
import java.util.List;

public final class StyleKeys {

    public static final String KEY_SEPARATOR = "@";
    public static final String AFFIX_SEPARATOR = "+";
    public static final String NAME_SEPARATOR = "#";
    public static final String CUSTOM_PREFIX = "custom:";

    private StyleKeys() {
    }

    public static String key(String styleId, String affixId) {
        return affixId == null || affixId.isBlank() ? styleId : styleId + KEY_SEPARATOR + affixId;
    }

    public static String key(String styleId, List<String> affixIds) {
        if (affixIds == null || affixIds.isEmpty()) {
            return styleId;
        }
        return styleId + KEY_SEPARATOR + String.join(AFFIX_SEPARATOR, sorted(affixIds));
    }

    public static List<String> sorted(List<String> affixIds) {
        List<String> copy = new ArrayList<>(affixIds);
        copy.sort(String::compareTo);
        return List.copyOf(copy);
    }

    public static String styleIdOf(String key) {
        String base = withoutName(key);
        int split = base.indexOf(KEY_SEPARATOR);
        return split < 0 ? base : base.substring(0, split);
    }

    public static List<String> affixIdsOf(String key) {
        String base = withoutName(key);
        int split = base.indexOf(KEY_SEPARATOR);
        if (split < 0 || split == base.length() - 1) {
            return List.of();
        }
        return List.of(base.substring(split + 1).split("\\" + AFFIX_SEPARATOR));
    }

    private static String withoutName(String key) {
        int named = key.indexOf(NAME_SEPARATOR);
        return named >= 0 ? key.substring(0, named) : key;
    }
}
