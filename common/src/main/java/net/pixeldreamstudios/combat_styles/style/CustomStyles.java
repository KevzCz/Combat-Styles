package net.pixeldreamstudios.combat_styles.style;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomStyles {

    private static final Map<String, CustomStyle> DEFINITIONS = new ConcurrentHashMap<>();
    private static final Map<String, CustomStyle> LOCAL = new ConcurrentHashMap<>();
    private static final Map<String, CustomStyle> SYNCED = new ConcurrentHashMap<>();
    private static final Set<String> SYNCED_CONFIG = ConcurrentHashMap.newKeySet();
    private static final Set<String> SYNCED_DATA = ConcurrentHashMap.newKeySet();

    private CustomStyles() {
    }

    public static void acceptLocal(CustomStyle style) {
        if (style != null && !style.id().isBlank()) {
            LOCAL.put(style.id(), style);
        }
    }

    public static void removeLocal(String id) {
        if (id == null) {
            return;
        }
        LOCAL.remove(id);
        if (!hasDatapackVersion(id)) {
            SYNCED.remove(id);
        }
    }

    public static void accept(Map<String, CustomStyle> loaded) {
        DEFINITIONS.clear();
        if (loaded != null) {
            DEFINITIONS.putAll(loaded);
        }
    }

    public static void acceptSync(Map<String, CustomStyle> synced, Set<String> fromConfig,
                                  Set<String> fromData) {
        SYNCED.clear();
        if (synced != null) {
            SYNCED.putAll(synced);
        }
        SYNCED_CONFIG.clear();
        if (fromConfig != null) {
            SYNCED_CONFIG.addAll(fromConfig);
        }
        SYNCED_DATA.clear();
        if (fromData != null) {
            SYNCED_DATA.addAll(fromData);
        }
        LOCAL.clear();
    }

    public static void clearSync() {
        SYNCED.clear();
        SYNCED_CONFIG.clear();
        SYNCED_DATA.clear();
        LOCAL.clear();
    }

    public static Map<String, CustomStyle> authoritative() {
        Map<String, CustomStyle> merged = new LinkedHashMap<>(DEFINITIONS);
        merged.putAll(CustomStyleConfig.loaded());
        return merged;
    }

    public static Set<String> datapackIds() {
        return Set.copyOf(DEFINITIONS.keySet());
    }

    public static Map<String, CustomStyle> snapshot() {
        Map<String, CustomStyle> merged = new LinkedHashMap<>(SYNCED);
        merged.putAll(authoritative());
        merged.putAll(LOCAL);
        return merged;
    }

    public static CustomStyle get(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        CustomStyle local = LOCAL.get(id);
        if (local != null) {
            return local;
        }
        CustomStyle fromConfig = CustomStyleConfig.loaded().get(id);
        if (fromConfig != null) {
            return fromConfig;
        }
        CustomStyle defined = DEFINITIONS.get(id);
        return defined != null ? defined : SYNCED.get(id);
    }

    public static boolean isConfigOwned(String id) {
        return id != null && (CustomStyleConfig.isFromConfig(id) || SYNCED_CONFIG.contains(id)
                || LOCAL.containsKey(id));
    }

    public static boolean hasDatapackVersion(String id) {
        return id != null && (DEFINITIONS.containsKey(id) || SYNCED_DATA.contains(id));
    }

    public static boolean isOverridden(String id) {
        return isConfigOwned(id) && hasDatapackVersion(id);
    }

    public static boolean contains(String id) {
        return get(id) != null;
    }

    public static int size() {
        return snapshot().size();
    }

    public static List<CustomStyle> all() {
        return List.copyOf(snapshot().values());
    }

    public static List<String> groups() {
        return List.copyOf(new TreeMap<>(byGroup()).keySet());
    }

    public static Map<String, List<CustomStyle>> byGroup() {
        Map<String, List<CustomStyle>> grouped = new TreeMap<>();
        for (CustomStyle style : snapshot().values()) {
            grouped.computeIfAbsent(style.group().toLowerCase(Locale.ROOT),
                    key -> new ArrayList<>()).add(style);
        }
        return grouped;
    }

    public static String styleIdOf(String customStyleId) {
        CustomStyle definition = get(customStyleId);
        return definition == null ? "" : definition.entry().styleId();
    }

    public static StyleSequence resolve(StyleSequence sequence) {
        if (sequence == null || sequence.isEmpty()) {
            return sequence;
        }
        List<StyleEntry> resolved = new ArrayList<>(sequence.entries().size());
        boolean changed = false;
        for (StyleEntry entry : sequence.entries()) {
            StyleEntry merged = resolve(entry);
            changed |= merged != entry;
            resolved.add(merged);
        }
        return changed ? new StyleSequence(sequence.name(), resolved) : sequence;
    }

    public static StyleEntry resolve(StyleEntry entry) {
        if (entry == null || entry.customStyleId().isEmpty()) {
            return entry;
        }
        CustomStyle definition = get(entry.customStyleId().get());
        return definition == null ? entry : definition.mergedWith(entry);
    }

    public static String categoryOf(StyleEntry entry, StylePage page) {
        if (entry != null && entry.customStyleId().isPresent()) {
            CustomStyle definition = get(entry.customStyleId().get());
            if (definition != null && definition.category().isPresent()) {
                return definition.category().get();
            }
        }
        return page == null ? null : page.category();
    }
}
