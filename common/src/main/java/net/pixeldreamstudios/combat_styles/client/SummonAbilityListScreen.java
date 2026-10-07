package net.pixeldreamstudios.combat_styles.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.entity.AbilityChoices;
import net.pixeldreamstudios.combat_styles.entity.AbilityKind;
import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;
import net.pixeldreamstudios.combat_styles.entity.AbilityRegistry;
import net.pixeldreamstudios.combat_styles.entity.ScriptedAbilities;
import net.pixeldreamstudios.combat_styles.entity.ScriptedAbility;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SummonAbilityListScreen extends DefinitionListScreen<ScriptedAbility> {

    private static final String KEY_TITLE = "screen.combat_styles.summon_ability_list";
    private static final String KEY_SUMMARY = "screen.combat_styles.summon_ability_summary";
    private static final String KEY_PASSIVE_SUMMARY = "screen.combat_styles.summon_ability_passive_summary";
    private static final String KEY_BUILT_IN = "screen.combat_styles.summon_ability_built_in";
    private static final String KEY_KIND_PREFIX = "screen.combat_styles.ability_kind.";
    private static final String KEY_TRIGGER_PREFIX = "tooltip.combat_styles.summon_passive_trigger.";
    private static final String KEY_EDITOR = "screen.combat_styles.summon_ability_editor";
    private static final String KEY_TUNING_EDITOR = "screen.combat_styles.summon_ability_tuning";
    private static final String KEY_DAMAGE_SPEC = "damage_spec";
    private static final String ACTION_SEPARATOR = ", ";
    private static final double PERCENT = 100.0D;

    public SummonAbilityListScreen(Screen parent, boolean creative) {
        super(Component.translatable(KEY_TITLE), parent, creative, ScriptedAbilities.DEFINITIONS,
                EffectSchema.ROOT_SUMMON_ABILITY, EditConfigPayload.Kind.SUMMON_ABILITY,
                EditConfigPayload.Kind.SUMMON_ABILITY_CLEAR, ScriptedAbility.SAMPLE,
                Component.translatable(KEY_EDITOR));
    }

    @Override
    protected List<Named<ScriptedAbility>> entries() {
        List<Named<ScriptedAbility>> all = new ArrayList<>(super.entries());
        for (String id : AbilityRegistry.registered()) {
            if (!ScriptedAbilities.scripted(id)) {
                all.add(new Named<>(id, null));
            }
        }
        return all;
    }

    @Override
    protected Source sourceOf(Named<ScriptedAbility> entry) {
        return entry.value() == null ? Source.BUILT_IN : super.sourceOf(entry);
    }

    @Override
    protected void openEditor(Named<ScriptedAbility> existing) {
        if (existing != null && existing.value() == null) {
            editBuiltIn(existing.id());
            return;
        }
        super.openEditor(existing);
    }

    private void editBuiltIn(String id) {
        JsonObject full = AbilityConfig.entryJson(id);
        List<String> allowed = AbilityRegistry.tunableFor(id);
        JsonObject shown = AbilityChoices.tuningOf(id);
        AbilityTuning current = AbilityTuning.CODEC
                .parse(JsonOps.INSTANCE, shown)
                .result()
                .orElse(AbilityTuning.NONE);
        minecraft.setScreen(new EffectTreeScreen(this, Component.translatable(KEY_TUNING_EDITOR),
                Component.literal(id), AbilityTuning.CODEC, current,
                applied -> applied.ifPresent(value -> saveBuiltIn(id, full, value)))
                .withSchema(EffectSchema.ROOT_ABILITY_TUNING)
                .withAllowed(allowed)
                .withOptionSource(AbilityRegistry.OPTION_SELECTOR, id, AbilityChoices.SCOPE_TUNING));
    }

    private static void merge(JsonObject into, JsonObject edits) {
        for (Map.Entry<String, JsonElement> entry : edits.entrySet()) {
            JsonElement existing = into.get(entry.getKey());
            if (existing != null && existing.isJsonObject() && entry.getValue().isJsonObject()
                    && !KEY_DAMAGE_SPEC.equals(entry.getKey())) {
                merge(existing.getAsJsonObject(), entry.getValue().getAsJsonObject());
            } else {
                into.add(entry.getKey(), entry.getValue().deepCopy());
            }
        }
    }

    private void saveBuiltIn(String id, JsonObject full, AbilityTuning value) {
        if (!ConfigEdits.permitted()) {
            showError(ConfigEdits.operatorOnly());
            return;
        }
        JsonElement edited = AbilityTuning.CODEC.encodeStart(JsonOps.INSTANCE, value)
                .result().orElse(null);
        if (edited == null || !edited.isJsonObject()) {
            return;
        }
        JsonObject merged = full.deepCopy();
        merge(merged, edited.getAsJsonObject());
        if (ConfigEdits.connected()) {
            ConfigEdits.send(new EditConfigPayload(EditConfigPayload.Kind.ABILITY, id, merged.toString()));
        } else {
            AbilityConfig.applyEntry(id, merged);
        }
        refresh();
    }

    @Override
    protected boolean remove(Named<ScriptedAbility> entry) {
        return entry.value() != null && super.remove(entry);
    }

    @Override
    protected String describe(ScriptedAbility value) {
        if (value == null) {
            return Component.translatable(KEY_BUILT_IN,
                    Component.translatable(KEY_KIND_PREFIX + AbilityKind.ACTIVE.getSerializedName()))
                    .getString();
        }
        Set<String> actions = new LinkedHashSet<>();
        for (ScriptedAbility.Step step : value.steps()) {
            actions.add(StyleText.prettify(step.action().type().getSerializedName()));
        }
        String steps = String.join(ACTION_SEPARATOR, new ArrayList<>(actions));
        Component kind = Component.translatable(KEY_KIND_PREFIX + value.kind().getSerializedName());
        if (value.passive()) {
            return Component.translatable(KEY_PASSIVE_SUMMARY, kind,
                    Component.translatable(KEY_TRIGGER_PREFIX + value.on().getSerializedName()),
                    StyleText.formatCompact(value.chance() * PERCENT), value.steps().size(), steps)
                    .getString();
        }
        return Component.translatable(KEY_SUMMARY, kind, value.steps().size(), steps).getString();
    }

    @Override
    protected int tint(ScriptedAbility value) {
        return value == null ? ScreenTheme.MUTED : ScreenTheme.ACCENT;
    }
}
