package net.pixeldreamstudios.combat_styles.compat.puffish;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

abstract class PlayerReward implements Reward {

    protected final String key = UUID.randomUUID().toString();
    private final Map<UUID, Integer> counts = new ConcurrentHashMap<>();

    @Override
    public final void update(RewardUpdateContext context) {
        ServerPlayer player = context.getPlayer();
        int count = Math.max(0, context.getCount());
        Integer previous = count == 0 ? counts.remove(player.getUUID()) : counts.put(player.getUUID(), count);
        if (previous == null && count == 0) {
            return;
        }
        apply(player, count);
    }

    @Override
    public final void dispose(RewardDisposeContext context) {
        MinecraftServer server = context.getServer();
        for (UUID playerId : Map.copyOf(counts).keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                apply(player, 0);
            }
        }
        counts.clear();
        disposed(context);
    }

    protected int countOf(ServerPlayer player) {
        return counts.getOrDefault(player.getUUID(), 0);
    }

    protected Map<UUID, Integer> counts() {
        return counts;
    }

    protected void forget(UUID playerId) {
        counts.remove(playerId);
    }

    protected abstract void apply(ServerPlayer player, int count);

    protected void disposed(RewardDisposeContext context) {
    }
}
