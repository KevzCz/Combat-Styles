package net.pixeldreamstudios.combat_styles.client;

import net.pixeldreamstudios.combat_styles.progression.ClientLearnedView;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StyleKeys;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ClientStyleData {

    private static final Map<String, Integer> COPIES = new LinkedHashMap<>();
    private static final Map<String, StyleEntry> VARIANTS = new LinkedHashMap<>();

    public static void acceptVariants(Map<String, StyleEntry> variants) {
        VARIANTS.clear();
        if (variants == null) {
            return;
        }
        for (Map.Entry<String, StyleEntry> entry : variants.entrySet()) {
            StyleEntry resolved = CustomStyles.resolve(entry.getValue());
            VARIANTS.put(resolved.learnedKey(), resolved);
        }
    }

    public static StyleEntry variant(String key) {
        return key == null ? null : VARIANTS.get(key);
    }

    public static Map<String, StyleEntry> variants() {
        return Map.copyOf(VARIANTS);
    }

    private ClientStyleData() {
    }

    public static void accept(Map<String, Integer> copies) {
        COPIES.clear();
        COPIES.putAll(copies);
        ClientLearnedView.bind(ClientStyleData::all);
    }

    public static boolean hasLearned(String styleId) {
        if (styleId == null) {
            return false;
        }
        if (copies(styleId) > 0 || copies(StyleKeys.CUSTOM_PREFIX + styleId) > 0) {
            return true;
        }
        for (Map.Entry<String, Integer> entry : COPIES.entrySet()) {
            if (entry.getValue() > 0
                    && StyleRegistry.sameStyle(StyleEntry.styleIdOfKey(entry.getKey()), styleId)) {
                return true;
            }
        }
        return false;
    }

    public static int copies(String styleId) {
        if (styleId == null) {
            return 0;
        }
        Integer own = COPIES.get(styleId);
        return own != null ? own : COPIES.getOrDefault(StyleRegistry.representativeOf(styleId), 0);
    }

    public static int count() {
        return COPIES.size();
    }

    public static int distinctStyles() {
        Set<String> styles = new HashSet<>();
        for (Map.Entry<String, Integer> entry : COPIES.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            String styleId = StyleEntry.styleIdOfKey(entry.getKey());
            if (styleId != null && !styleId.isBlank()) {
                styles.add(styleId);
            }
        }
        return styles.size();
    }

    public static Map<String, Integer> all() {
        return Map.copyOf(COPIES);
    }

    public static void accept(Map<String, Integer> copies, Map<String, StyleEntry> variants) {
        accept(copies);
        acceptVariants(variants);
    }

    public static void clear() {
        COPIES.clear();
        VARIANTS.clear();
    }
}
