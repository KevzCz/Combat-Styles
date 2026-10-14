package net.pixeldreamstudios.combat_styles.compat.puffish;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.pixeldreamstudios.combat_styles.skill.SkillConditions;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ConditionalReward extends PlayerReward {

    private static final String KEY_CONDITION = "condition";
    private static final String KEY_REWARDS = "rewards";

    private final SkillConditions.Condition condition;
    private final NestedRewards rewards;
    private final Map<UUID, Integer> applied = new ConcurrentHashMap<>();

    ConditionalReward(JsonElement data, RewardConfigContext context) {
        this.condition = SkillConditions.parse(RewardData.object(data).get(KEY_CONDITION));
        this.rewards = NestedRewards.parse(RewardData.child(data, KEY_REWARDS), context);
        PuffishBridge.track(this);
    }

    @Override
    protected void apply(ServerPlayer player, int count) {
        evaluate(player, count);
    }

    void evaluate(MinecraftServer server) {
        for (Map.Entry<UUID, Integer> entry : Map.copyOf(counts()).entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                evaluate(player, entry.getValue());
            }
        }
    }

    private void evaluate(ServerPlayer player, int count) {
        int effective = count > 0 && condition.test(player) ? count : 0;
        Integer previous = applied.get(player.getUUID());
        if (previous != null && previous == effective) {
            return;
        }
        if (effective == 0) {
            applied.remove(player.getUUID());
        } else {
            applied.put(player.getUUID(), effective);
        }
        rewards.update(player, effective);
    }

    @Override
    protected void forget(UUID playerId) {
        super.forget(playerId);
        applied.remove(playerId);
    }

    @Override
    protected void disposed(RewardDisposeContext context) {
        applied.clear();
        rewards.dispose(context);
        PuffishBridge.untrack(this);
    }
}
