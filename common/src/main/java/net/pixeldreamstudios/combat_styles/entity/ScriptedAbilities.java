package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.config.ConfigPaths;
import net.pixeldreamstudios.combat_styles.config.DefinitionLayer;
import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ScriptedAbilities {

    private static final String LABEL = "summon ability";
    private static final List<String> TUNABLE = List.of(AbilityTuning.KEY_DAMAGE, AbilityTuning.KEY_TIMING,
            AbilityTuning.KEY_SHAPE, AbilityTuning.KEY_FX, AbilityTuning.KEY_STEPS);

    public static final DefinitionLayer<ScriptedAbility> DEFINITIONS = new DefinitionLayer<>(LABEL,
            ConfigPaths.SUMMON_ABILITIES, ScriptedAbility.CODEC, ScriptedAbilities::rebuild);

    private static final Map<String, ScriptedAbility> ACTIVE = new LinkedHashMap<>();
    private static final Set<String> REGISTERED = new LinkedHashSet<>();

    private ScriptedAbilities() {
    }

    public static synchronized void rebuild() {
        apply(DEFINITIONS.authoritative());
    }

    public static synchronized void rebuildFromSync() {
        apply(DEFINITIONS.view());
    }

    private static void apply(Map<String, ScriptedAbility> definitions) {
        for (String key : REGISTERED) {
            AbilityRegistry.unregister(key);
            AbilityConfig.forgetData(key);
        }
        REGISTERED.clear();
        ACTIVE.clear();
        for (Map.Entry<String, ScriptedAbility> named : definitions.entrySet()) {
            String key = AbilityConfig.key(named.getKey());
            ScriptedAbility ability = named.getValue();
            if (AbilityRegistry.has(key)) {
                CombatStyles.LOGGER.warn("Summon ability {} is already registered in code; "
                        + "the data version is ignored", named.getKey());
                continue;
            }
            AbilityRegistry.register(key, ability.passive()
                    ? new ScriptedPassive(ability)
                    : new ScriptedActive(ability));
            AbilityConfig.defineData(key, ability.defaults());
            REGISTERED.add(key);
            ACTIVE.put(key, ability);
        }
    }

    private record ScriptedActive(ScriptedAbility ability) implements ActiveAbility {

        @Override
        public void run(ServerLevel level, AbilityHost host, int elapsed, float damage,
                        AbilityConfig.Entry tuned) {
            ability.run(level, host, elapsed, damage, tuned);
        }

        @Override
        public List<String> usedBy() {
            return ability.usedBy();
        }

        @Override
        public List<String> tunable() {
            return TUNABLE;
        }

        @Override
        public boolean scripted() {
            return true;
        }
    }

    private record ScriptedPassive(ScriptedAbility ability) implements PassiveAbility {

        @Override
        public void run(ServerLevel level, AbilityHost host, int elapsed, float damage,
                        AbilityConfig.Entry tuned) {
            ability.run(level, host, elapsed, damage, tuned);
        }

        @Override
        public PassiveTrigger on() {
            return ability.on();
        }

        @Override
        public float chance() {
            return ability.chance();
        }

        @Override
        public List<String> usedBy() {
            return ability.usedBy();
        }

        @Override
        public List<String> tunable() {
            return TUNABLE;
        }

        @Override
        public boolean scripted() {
            return true;
        }
    }

    public static ScriptedAbility get(String ability) {
        return ACTIVE.get(AbilityConfig.key(ability));
    }

    public static boolean scripted(String ability) {
        return ACTIVE.containsKey(AbilityConfig.key(ability));
    }

    public static Vec3 focusFor(String ability, AbilityHost host, AbilityConfig.Entry tuned) {
        ScriptedAbility scripted = get(ability);
        return scripted == null ? null : scripted.focusPoint(host, tuned);
    }
}
