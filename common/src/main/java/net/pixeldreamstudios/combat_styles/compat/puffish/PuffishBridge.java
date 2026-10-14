package net.pixeldreamstudios.combat_styles.compat.puffish;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.SkillsAPI;
import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PuffishBridge {

    private static final int EVALUATE_INTERVAL = 10;

    private static final Set<ConditionalReward> CONDITIONALS = ConcurrentHashMap.newKeySet();

    private PuffishBridge() {
    }

    public static void init() {
        SkillsAPI.registerReward(CombatStyles.id("equipment"),
                context -> RewardData.create(context, GrantRewards.Equipment::new));
        SkillsAPI.registerReward(CombatStyles.id("style"),
                context -> RewardData.create(context, GrantRewards.Style::new));
        SkillsAPI.registerReward(CombatStyles.id("budget"),
                context -> RewardData.create(context, GrantRewards.Budget::new));
        SkillsAPI.registerReward(CombatStyles.id("conditional"),
                context -> RewardData.create(context, ConditionalReward::new));
        SkillsAPI.registerReward(CombatStyles.id("choice"),
                context -> RewardData.create(context, ChoiceReward::new));
        SkillsAPI.registerReward(CombatStyles.id("counter"),
                context -> RewardData.create(context, CounterReward::new));
        SkillsAPI.registerReward(CombatStyles.id("on_event"),
                context -> RewardData.create(context, EventReward::new));
        SkillsAPI.registerReward(CombatStyles.id("dot"),
                context -> RewardData.create(context, DotReward::new));
    }

    public static void tick(MinecraftServer server) {
        TreeRequirements.tick(server);
        if (CONDITIONALS.isEmpty() || server.getTickCount() % EVALUATE_INTERVAL != 0) {
            return;
        }
        for (ConditionalReward reward : CONDITIONALS) {
            reward.evaluate(server);
        }
    }

    public static void checkTreesNow(ServerPlayer player) {
        TreeRequirements.checkNow(player);
    }

    public static void setCategoryUnlocked(ServerPlayer player, ResourceLocation categoryId, boolean unlocked) {
        TreeRequirements.setUnlocked(player, categoryId, unlocked);
    }

    public static void forget(UUID playerId) {
        for (ConditionalReward reward : CONDITIONALS) {
            reward.forget(playerId);
        }
    }

    static void track(ConditionalReward reward) {
        CONDITIONALS.add(reward);
    }

    static void untrack(ConditionalReward reward) {
        CONDITIONALS.remove(reward);
    }
}
