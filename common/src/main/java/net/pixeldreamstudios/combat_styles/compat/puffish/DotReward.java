package net.pixeldreamstudios.combat_styles.compat.puffish;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.pixeldreamstudios.combat_styles.skill.SkillConditions;
import net.pixeldreamstudios.combat_styles.skill.SkillDots;

import java.util.ArrayList;
import java.util.List;

final class DotReward extends PlayerReward {

    private static final String KEY_EFFECTS = "effects";
    private static final String KEY_SCALING = "scaling";
    private static final String KEY_PER_POINT = "per_point";
    private static final String KEY_FLAT = "flat";

    private final List<SkillDots.Boost> boosts = new ArrayList<>();

    DotReward(JsonElement data, RewardConfigContext context) {
        JsonObject object = RewardData.object(data);
        List<String> effects = RewardData.strings(object, KEY_EFFECTS);
        if (effects.isEmpty()) {
            throw new IllegalArgumentException("Expected \"" + KEY_EFFECTS + "\"");
        }
        String scaling = object.has(KEY_SCALING) ? object.get(KEY_SCALING).getAsString() : null;
        float perPoint = object.has(KEY_PER_POINT) ? object.get(KEY_PER_POINT).getAsFloat() : 0.0F;
        float flat = object.has(KEY_FLAT) ? object.get(KEY_FLAT).getAsFloat() : 0.0F;
        for (String effect : effects) {
            boosts.add(new SkillDots.Boost(SkillConditions.effect(effect),
                    scaling == null ? null : SkillConditions.attribute(scaling), perPoint, flat));
        }
    }

    @Override
    protected void apply(ServerPlayer player, int count) {
        SkillDots.define(player, key, count > 0 ? boosts : null);
    }
}
