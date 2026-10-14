package net.pixeldreamstudios.combat_styles.compat.puffish;

import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.config.skill.SkillRewardConfig;
import net.puffish.skillsmod.impl.rewards.RewardUpdateContextImpl;

import java.util.ArrayList;
import java.util.List;

final class NestedRewards {

    private final List<SkillRewardConfig> rewards;

    private NestedRewards(List<SkillRewardConfig> rewards) {
        this.rewards = List.copyOf(rewards);
    }

    static NestedRewards parse(JsonElement element, ConfigContext context) {
        List<SkillRewardConfig> parsed = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        element.getAsArray()
                .ifFailure(problem -> problems.add(problem.toString()))
                .ifSuccess(array -> array.stream().forEach(entry -> SkillRewardConfig.parse(entry, context)
                        .ifSuccess(parsed::add)
                        .ifFailure(problem -> problems.add(problem.toString()))));
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", problems));
        }
        return new NestedRewards(parsed);
    }

    void update(ServerPlayer player, int count) {
        for (SkillRewardConfig reward : rewards) {
            reward.instance().update(new RewardUpdateContextImpl(player, count, false));
        }
    }

    void dispose(RewardDisposeContext context) {
        for (SkillRewardConfig reward : rewards) {
            reward.instance().dispose(context);
        }
    }
}
