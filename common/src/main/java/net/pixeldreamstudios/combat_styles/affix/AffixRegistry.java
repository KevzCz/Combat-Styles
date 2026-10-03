package net.pixeldreamstudios.combat_styles.affix;

import net.minecraft.util.RandomSource;

import net.pixeldreamstudios.combat_styles.style.StylePage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AffixRegistry {

    private static final Map<String, StyleAffix> AFFIXES = new ConcurrentHashMap<>();
    private static final Map<String, StyleAffix> DATAPACK = new ConcurrentHashMap<>();
    private static final Map<String, StyleAffix> CUSTOM = new ConcurrentHashMap<>();
    private static final Map<String, StyleAffix> SYNCED = new ConcurrentHashMap<>();
    private static final Set<String> SYNCED_CONFIG = ConcurrentHashMap.newKeySet();
    private static final Set<String> SYNCED_DATA = ConcurrentHashMap.newKeySet();
    private static volatile List<StyleAffix> ordered = List.of();

    private AffixRegistry() {
    }

    public static void accept(Map<String, StyleAffix> loaded) {
        DATAPACK.clear();
        DATAPACK.putAll(loaded);
        rebuild();
    }

    public static void acceptCustom(Map<String, StyleAffix> loaded) {
        CUSTOM.clear();
        CUSTOM.putAll(loaded);
        rebuild();
    }

    public static void acceptSync(Map<String, StyleAffix> synced, Set<String> fromConfig,
                                  Set<String> fromData) {
        SYNCED.clear();
        SYNCED.putAll(synced);
        SYNCED_CONFIG.clear();
        SYNCED_CONFIG.addAll(fromConfig);
        SYNCED_DATA.clear();
        SYNCED_DATA.addAll(fromData);
        rebuild();
    }

    public static void removeLocal(String affixId) {
        if (affixId != null && !hasDatapackVersion(affixId)) {
            SYNCED.remove(affixId);
            rebuild();
        }
    }

    public static Map<String, StyleAffix> authoritative() {
        Map<String, StyleAffix> merged = new LinkedHashMap<>(DATAPACK);
        merged.putAll(CUSTOM);
        return merged;
    }

    public static Set<String> customIds() {
        return Set.copyOf(CUSTOM.keySet());
    }

    public static Set<String> datapackIds() {
        return Set.copyOf(DATAPACK.keySet());
    }

    public static boolean isConfigOwned(String affixId) {
        return affixId != null && (CUSTOM.containsKey(affixId) || SYNCED_CONFIG.contains(affixId));
    }

    public static boolean hasDatapackVersion(String affixId) {
        return affixId != null && (DATAPACK.containsKey(affixId) || SYNCED_DATA.contains(affixId));
    }

    public static boolean isOverridden(String affixId) {
        return isConfigOwned(affixId) && hasDatapackVersion(affixId);
    }

    public static void acceptLocal(StyleAffix affix) {
        if (affix != null) {
            SYNCED.put(affix.serializedId(), affix);
            rebuild();
        }
    }

    public static void clearSync() {
        SYNCED.clear();
        SYNCED_CONFIG.clear();
        SYNCED_DATA.clear();
        rebuild();
    }

    public static Map<String, StyleAffix> snapshot() {
        return Map.copyOf(AFFIXES);
    }

    private static void rebuild() {
        AFFIXES.clear();
        AFFIXES.putAll(DATAPACK);
        AFFIXES.putAll(CUSTOM);
        AFFIXES.putAll(SYNCED);
        List<StyleAffix> sorted = new ArrayList<>(AFFIXES.values());
        sorted.sort((a, b) -> a.id().compareTo(b.id()));
        ordered = List.copyOf(sorted);
    }

    public static StyleAffix get(String affixId) {
        if (affixId == null || affixId.isBlank()) {
            return null;
        }
        String normalized = affixId.trim();
        StyleAffix direct = AFFIXES.get(normalized);
        if (direct != null) {
            return direct;
        }
        if (!normalized.contains(":")) {
            for (StyleAffix affix : ordered) {
                if (affix.id().getPath().equals(normalized)) {
                    return affix;
                }
            }
        }
        return null;
    }

    public static boolean contains(String affixId) {
        return get(affixId) != null;
    }

    public static List<StyleAffix> all() {
        return ordered;
    }

    public static boolean isEmpty() {
        return AFFIXES.isEmpty();
    }

    public static int size() {
        return AFFIXES.size();
    }

    public static List<StyleAffix> applicableTo(StylePage page) {
        List<StyleAffix> matching = new ArrayList<>();
        for (StyleAffix affix : ordered) {
            if (affix.canApplyTo(page)) {
                matching.add(affix);
            }
        }
        return matching;
    }

    public static StyleAffix weightedPick(StylePage page, RandomSource random) {
        List<StyleAffix> candidates = applicableTo(page);
        if (candidates.isEmpty()) {
            return null;
        }
        int total = 0;
        for (StyleAffix affix : candidates) {
            total += Math.max(0, affix.weight());
        }
        if (total <= 0) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        int roll = random.nextInt(total);
        for (StyleAffix affix : candidates) {
            roll -= Math.max(0, affix.weight());
            if (roll < 0) {
                return affix;
            }
        }
        return candidates.get(candidates.size() - 1);
    }
}
