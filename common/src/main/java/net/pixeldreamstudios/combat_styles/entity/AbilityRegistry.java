package net.pixeldreamstudios.combat_styles.entity;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AbilityRegistry {

    private static final Map<String, AbilityBehaviour> BEHAVIOURS = new LinkedHashMap<>();
    private static final Map<String, List<String>> TUNABLES = new LinkedHashMap<>();
    private static final Map<String, Set<String>> ENTITIES = new LinkedHashMap<>();

    private AbilityRegistry() {
    }

    public static void register(String ability, AbilityBehaviour behaviour) {
        if (ability == null || behaviour == null) {
            return;
        }
        AbilityBehaviour previous = BEHAVIOURS.put(AbilityConfig.key(ability), behaviour);
        if (previous != null && previous != behaviour) {
            CombatStyles.LOGGER.warn("Ability {} was registered twice; the later one wins",
                    AbilityConfig.key(ability));
        }
    }

    public static void register(String ability, List<String> entities, AbilityBehaviour behaviour) {
        register(ability, behaviour);
        if (ability != null && behaviour != null && entities != null && !entities.isEmpty()) {
            ENTITIES.put(AbilityConfig.key(ability), Set.copyOf(entities));
        }
    }

    public static Set<String> entitiesFor(String ability) {
        Set<String> bound = ENTITIES.get(AbilityConfig.key(ability));
        return bound == null ? Set.of() : bound;
    }

    public static boolean usableBy(String ability, String entityId) {
        Set<String> bound = entitiesFor(ability);
        return bound.isEmpty() || entityId == null || entityId.isBlank() || bound.contains(entityId);
    }

    public static List<String> usableBy(String entityId) {
        List<String> usable = new ArrayList<>();
        for (String ability : BEHAVIOURS.keySet()) {
            if (usableBy(ability, entityId)) {
                usable.add(ability);
            }
        }
        return usable;
    }

    public static AbilityBehaviour get(String ability) {
        return BEHAVIOURS.get(AbilityConfig.key(ability));
    }

    public static boolean has(String ability) {
        return get(ability) != null;
    }

    public static void unregister(String ability) {
        String key = AbilityConfig.key(ability);
        BEHAVIOURS.remove(key);
        TUNABLES.remove(key);
        ENTITIES.remove(key);
    }

    public static void clear() {
        BEHAVIOURS.clear();
        TUNABLES.clear();
        ENTITIES.clear();
    }

    public static List<String> registered() {
        return List.copyOf(BEHAVIOURS.keySet());
    }

    public static void declareTunable(String ability, List<String> fields) {
        if (ability == null || fields == null || fields.isEmpty()) {
            return;
        }
        TUNABLES.put(AbilityConfig.key(ability), List.copyOf(fields));
    }

    public static List<String> tunableFor(String ability) {
        List<String> declared = TUNABLES.get(AbilityConfig.key(ability));
        return declared == null ? List.of() : declared;
    }

}
