package net.pixeldreamstudios.combat_styles.style;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomAttacks {

    private static final Map<String, CustomAttack> DEFINITIONS = new ConcurrentHashMap<>();
    private static final Map<String, CustomAttack> LOCAL = new ConcurrentHashMap<>();
    private static final Map<String, CustomAttack> SYNCED = new ConcurrentHashMap<>();
    private static final Set<String> SYNCED_CONFIG = ConcurrentHashMap.newKeySet();
    private static final Set<String> SYNCED_DATA = ConcurrentHashMap.newKeySet();

    private CustomAttacks() {
    }

    public static void accept(Map<String, CustomAttack> loaded) {
        DEFINITIONS.clear();
        if (loaded != null) {
            DEFINITIONS.putAll(loaded);
        }
    }

    public static void acceptSync(Map<String, CustomAttack> synced, Set<String> fromConfig,
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

    public static void acceptLocal(CustomAttack attack) {
        if (attack != null && !attack.id().isBlank()) {
            LOCAL.put(attack.id(), attack);
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

    public static void clearSync() {
        SYNCED.clear();
        SYNCED_CONFIG.clear();
        SYNCED_DATA.clear();
        LOCAL.clear();
    }

    public static CustomAttack get(String id) {
        if (id == null) {
            return null;
        }
        CustomAttack local = LOCAL.get(id);
        if (local != null) {
            return local;
        }
        CustomAttack fromConfig = CustomAttackConfig.loaded().get(id);
        if (fromConfig != null) {
            return fromConfig;
        }
        CustomAttack defined = DEFINITIONS.get(id);
        return defined != null ? defined : SYNCED.get(id);
    }

    public static boolean isConfigOwned(String id) {
        return id != null && (CustomAttackConfig.isFromConfig(id) || SYNCED_CONFIG.contains(id)
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

    public static Set<String> datapackIds() {
        return Set.copyOf(DEFINITIONS.keySet());
    }

    public static Map<String, CustomAttack> authoritative() {
        Map<String, CustomAttack> merged = new LinkedHashMap<>(DEFINITIONS);
        merged.putAll(CustomAttackConfig.loaded());
        return merged;
    }

    public static List<CustomAttack> all() {
        Map<String, CustomAttack> merged = new LinkedHashMap<>(SYNCED);
        merged.putAll(authoritative());
        merged.putAll(LOCAL);
        return List.copyOf(new ArrayList<>(merged.values()));
    }

    public static Map<String, CustomAttack> snapshot() {
        return authoritative();
    }

    public static int size() {
        return all().size();
    }
}
