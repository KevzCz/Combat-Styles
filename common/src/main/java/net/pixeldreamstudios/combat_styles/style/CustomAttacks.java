package net.pixeldreamstudios.combat_styles.style;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomAttacks {

    private static final Map<String, CustomAttack> DEFINITIONS = new ConcurrentHashMap<>();
    private static final Map<String, CustomAttack> SYNCED = new ConcurrentHashMap<>();
    private static final Set<String> SYNCED_CONFIG = ConcurrentHashMap.newKeySet();

    private CustomAttacks() {
    }

    public static void accept(Map<String, CustomAttack> loaded) {
        DEFINITIONS.clear();
        DEFINITIONS.putAll(loaded);
    }

    public static void acceptSync(Map<String, CustomAttack> synced, Set<String> fromConfig) {
        SYNCED.clear();
        SYNCED.putAll(synced);
        SYNCED_CONFIG.clear();
        SYNCED_CONFIG.addAll(fromConfig);
    }

    public static void acceptLocal(CustomAttack attack) {
        if (attack != null && !attack.id().isBlank()) {
            DEFINITIONS.put(attack.id(), attack);
        }
    }

    public static void removeLocal(String id) {
        if (id != null) {
            DEFINITIONS.remove(id);
            SYNCED.remove(id);
        }
    }

    public static void clearSync() {
        SYNCED.clear();
        SYNCED_CONFIG.clear();
    }

    public static CustomAttack get(String id) {
        CustomAttack defined = id == null ? null : DEFINITIONS.get(id);
        if (defined != null) {
            return defined;
        }
        CustomAttack synced = id == null ? null : SYNCED.get(id);
        if (synced != null) {
            return synced;
        }
        return id == null ? null : CustomAttackConfig.loaded().get(id);
    }

    public static boolean isConfigOwned(String id) {
        return id != null && (CustomAttackConfig.isFromConfig(id) || SYNCED_CONFIG.contains(id));
    }

    public static boolean isDatapackOwned(String id) {
        return id != null && (DEFINITIONS.containsKey(id) || SYNCED.containsKey(id))
                && !isConfigOwned(id);
    }

    public static boolean contains(String id) {
        return get(id) != null;
    }

    public static List<CustomAttack> all() {
        Map<String, CustomAttack> merged = new LinkedHashMap<>();
        merged.putAll(CustomAttackConfig.loaded());
        merged.putAll(SYNCED);
        merged.putAll(DEFINITIONS);
        return List.copyOf(new ArrayList<>(merged.values()));
    }

    public static Map<String, CustomAttack> snapshot() {
        Map<String, CustomAttack> merged = new LinkedHashMap<>(CustomAttackConfig.loaded());
        merged.putAll(DEFINITIONS);
        return merged;
    }

    public static int size() {
        return all().size();
    }
}
