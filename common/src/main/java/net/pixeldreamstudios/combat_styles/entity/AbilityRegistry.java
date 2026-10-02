package net.pixeldreamstudios.combat_styles.entity;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AbilityRegistry {

    private static final Map<String, AbilityBehaviour> BEHAVIOURS = new LinkedHashMap<>();
    private static final Map<String, List<String>> TUNABLES = new LinkedHashMap<>();

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
    }

    public static void clear() {
        BEHAVIOURS.clear();
        TUNABLES.clear();
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
