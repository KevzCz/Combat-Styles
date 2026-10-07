package net.pixeldreamstudios.combat_styles.entity;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.config.OptionField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AbilityRegistry {

    public static final String OPTION_SELECTOR = "ability";

    private static final Map<String, SummonAbility> ABILITIES = new LinkedHashMap<>();

    private AbilityRegistry() {
    }

    public static void register(String ability, SummonAbility behaviour) {
        if (ability == null || behaviour == null) {
            return;
        }
        SummonAbility previous = ABILITIES.put(AbilityConfig.key(ability), behaviour);
        if (previous != null && previous != behaviour) {
            CombatStyles.LOGGER.warn("Ability {} was registered twice; the later one wins",
                    AbilityConfig.key(ability));
        }
    }

    public static Set<String> entitiesFor(String ability) {
        SummonAbility found = get(ability);
        return found == null ? Set.of() : Set.copyOf(found.usedBy());
    }

    public static boolean usableBy(String ability, String entityId) {
        Set<String> bound = entitiesFor(ability);
        return bound.isEmpty() || entityId == null || entityId.isBlank() || bound.contains(entityId);
    }

    public static List<String> usableBy(String entityId) {
        List<String> usable = new ArrayList<>();
        for (String ability : ABILITIES.keySet()) {
            if (usableBy(ability, entityId)) {
                usable.add(ability);
            }
        }
        return usable;
    }

    public static SummonAbility get(String ability) {
        return ABILITIES.get(AbilityConfig.key(ability));
    }

    public static boolean has(String ability) {
        return get(ability) != null;
    }

    public static boolean passive(String ability) {
        return get(ability) instanceof PassiveAbility;
    }

    public static void unregister(String ability) {
        ABILITIES.remove(AbilityConfig.key(ability));
    }

    public static void clear() {
        ABILITIES.clear();
    }

    public static List<String> registered() {
        return List.copyOf(ABILITIES.keySet());
    }

    public static List<OptionField> optionsFor(String ability) {
        SummonAbility found = get(ability);
        return found == null ? List.of() : found.options();
    }

    public static List<String> tunableFor(String ability) {
        SummonAbility found = get(ability);
        return found == null ? List.of() : found.tunable();
    }
}
