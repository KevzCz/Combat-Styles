package net.pixeldreamstudios.combat_styles.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.config.ChoiceSource;
import net.pixeldreamstudios.combat_styles.config.OptionField;
import net.pixeldreamstudios.combat_styles.config.OptionSources;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;
import net.pixeldreamstudios.combat_styles.equipment.AbilityUse;
import net.pixeldreamstudios.combat_styles.equipment.StepTuning;

import java.util.List;

public final class AbilityChoices implements ChoiceSource {

    public static final String SCOPE_TUNING = AbilityUse.KEY_TUNING;

    @Override
    public String selector() {
        return AbilityRegistry.OPTION_SELECTOR;
    }

    @Override
    public List<String> values() {
        return AbilityRegistry.registered();
    }

    @Override
    public List<OptionField> options(String value) {
        return AbilityRegistry.optionsFor(value);
    }

    @Override
    public JsonObject optionDefaults(String value) {
        AbilityConfig.Entry entry = AbilityConfig.get(value);
        return entry == null ? null : entry.options;
    }

    @Override
    public JsonElement template(String value, String scope, String key) {
        if (OptionSources.SCOPE_SELF.equals(scope) && SCOPE_TUNING.equals(key)) {
            return tuningOf(value);
        }
        if (SCOPE_TUNING.equals(scope)) {
            return tuningOf(value).get(key);
        }
        return null;
    }

    @Override
    public List<String> allowed(String value, String scope) {
        if (!SCOPE_TUNING.equals(scope)) {
            return null;
        }
        List<String> declared = AbilityRegistry.tunableFor(value);
        return declared.isEmpty() ? null : declared;
    }

    public static JsonObject tuningOf(String ability) {
        List<String> allowed = AbilityRegistry.tunableFor(ability);
        List<String> limit = allowed.isEmpty() ? null : allowed;
        JsonObject full = AbilityConfig.entryJson(ability);
        JsonObject shown = normalized(OptionSources.pruned(full, limit));
        if (OptionSources.permitted(limit, OptionSources.KEY_OPTIONS)) {
            JsonObject options = OptionSources.prefilled(AbilityRegistry.OPTION_SELECTOR, ability);
            JsonElement kept = shown.get(OptionSources.KEY_OPTIONS);
            if (kept != null && kept.isJsonObject()) {
                for (var entry : kept.getAsJsonObject().entrySet()) {
                    options.add(entry.getKey(), entry.getValue().deepCopy());
                }
            }
            if (!options.isEmpty()) {
                shown.add(OptionSources.KEY_OPTIONS, options);
            }
        }
        if (OptionSources.permitted(limit, AbilityTuning.KEY_STEPS)) {
            JsonArray steps = stepsOf(ability, shown);
            if (!steps.isEmpty()) {
                shown.add(AbilityTuning.KEY_STEPS, steps);
            }
        }
        return shown;
    }

    private static JsonObject normalized(JsonObject raw) {
        return AbilityTuning.CODEC.parse(JsonOps.INSTANCE, raw).result()
                .flatMap(tuning -> AbilityTuning.CODEC.encodeStart(JsonOps.INSTANCE, tuning).result())
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject)
                .orElseGet(JsonObject::new);
    }

    private static JsonArray stepsOf(String ability, JsonObject shown) {
        JsonArray steps = new JsonArray();
        ScriptedAbility scripted = ScriptedAbilities.get(ability);
        if (scripted == null) {
            return steps;
        }
        JsonElement abilitySpec = abilitySpecOf(shown);
        for (int index = 0; index < scripted.steps().size(); index++) {
            ScriptedAbility.Action action = scripted.steps().get(index).action();
            if (!action.type().actionKeys().contains(ScriptedAbility.KEY_DAMAGE_SPEC)) {
                continue;
            }
            JsonElement spec = action.damageSpec()
                    .flatMap(own -> DamageSpec.CODEC.encodeStart(JsonOps.INSTANCE, own).result())
                    .orElse(abilitySpec == null ? new JsonObject() : abilitySpec.deepCopy());
            JsonObject step = new JsonObject();
            step.addProperty(StepTuning.KEY_STEP, index);
            step.add(StepTuning.KEY_DAMAGE_SPEC, spec);
            steps.add(step);
        }
        return steps;
    }

    private static JsonElement abilitySpecOf(JsonObject shown) {
        JsonElement damage = shown.get(AbilityTuning.KEY_DAMAGE);
        return damage != null && damage.isJsonObject()
                ? damage.getAsJsonObject().get(ScriptedAbility.KEY_DAMAGE_SPEC)
                : null;
    }
}
