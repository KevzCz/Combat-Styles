package net.pixeldreamstudios.combat_styles.compat.puffish;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.pixeldreamstudios.combat_styles.skill.SkillChoices;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

final class ChoiceReward extends PlayerReward {

    private static final String KEY_CHOICE = "choice";
    private static final String KEY_OPTIONS = "options";
    private static final String KEY_DEFAULT = "default";
    private static final String KEY_SWAP_COOLDOWN = "swap_cooldown";
    private static final String KEY_OUT_OF_COMBAT = "out_of_combat";

    private final String choice;
    private final Map<String, NestedRewards> options = new LinkedHashMap<>();
    private final SkillChoices.Spec spec;

    ChoiceReward(JsonElement data, RewardConfigContext context) {
        JsonObject object = RewardData.object(data);
        if (!object.has(KEY_CHOICE) || !object.has(KEY_OPTIONS) || !object.get(KEY_OPTIONS).isJsonObject()) {
            throw new IllegalArgumentException("Expected \"" + KEY_CHOICE + "\" and an \"" + KEY_OPTIONS + "\" object");
        }
        this.choice = object.get(KEY_CHOICE).getAsString();
        JsonElement optionData = RewardData.child(data, KEY_OPTIONS);
        for (String option : object.getAsJsonObject(KEY_OPTIONS).keySet()) {
            options.put(option, NestedRewards.parse(RewardData.child(optionData, option), context));
        }
        if (options.isEmpty()) {
            throw new IllegalArgumentException("A choice needs at least one option");
        }
        String fallback = object.has(KEY_DEFAULT) ? object.get(KEY_DEFAULT).getAsString() : options.keySet().iterator().next();
        if (!options.containsKey(fallback)) {
            throw new IllegalArgumentException("Default option " + fallback + " is not one of the options");
        }
        this.spec = new SkillChoices.Spec(new ArrayList<>(options.keySet()), fallback,
                object.has(KEY_SWAP_COOLDOWN) ? object.get(KEY_SWAP_COOLDOWN).getAsInt() : 0,
                object.has(KEY_OUT_OF_COMBAT) && object.get(KEY_OUT_OF_COMBAT).getAsBoolean());
    }

    @Override
    protected void apply(ServerPlayer player, int count) {
        if (count > 0) {
            SkillChoices.define(player, choice, key, spec);
            SkillChoices.listen(player, key, () -> refresh(player));
        } else {
            SkillChoices.define(player, choice, key, null);
            SkillChoices.listen(player, key, null);
        }
        refresh(player, count);
    }

    private void refresh(ServerPlayer player) {
        refresh(player, countOf(player));
    }

    private void refresh(ServerPlayer player, int count) {
        String selected = count > 0 ? SkillChoices.selected(player, choice) : null;
        for (Map.Entry<String, NestedRewards> option : options.entrySet()) {
            option.getValue().update(player, option.getKey().equals(selected) ? count : 0);
        }
    }

    @Override
    protected void disposed(RewardDisposeContext context) {
        for (NestedRewards rewards : options.values()) {
            rewards.dispose(context);
        }
    }
}
