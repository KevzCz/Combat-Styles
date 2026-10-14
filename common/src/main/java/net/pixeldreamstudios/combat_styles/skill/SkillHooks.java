package net.pixeldreamstudios.combat_styles.skill;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.combat_styles.api.CombatStylesAPI;
import net.pixeldreamstudios.combat_styles.compat.PuffishCompat;
import net.pixeldreamstudios.combat_styles.compat.puffish.TreePools;
import net.pixeldreamstudios.combat_styles.kit.KitHooks;
import net.pixeldreamstudios.combat_styles.kit.KitState;
import net.pixeldreamstudios.combat_styles.progression.LearnedStyles;
import net.pixeldreamstudios.combat_styles.progression.StyleProgression;

import java.util.UUID;

public final class SkillHooks {

    private SkillHooks() {
    }

    public static void init() {
        CombatStylesAPI.registerBudgetModifier((holder, stack, category, budget) ->
                holder instanceof Player player ? budget + SkillGrants.budgetOf(player) : budget);
        PuffishCompat.init();
    }

    public static void tick(MinecraftServer server) {
        PuffishCompat.tick(server);
        KitHooks.tick(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SkillCounters.tick(player);
            SkillTags.tick(player);
            SkillScaling.tick(player);
            SkillEvents.tick(player);
            SkillGrants.flush(player);
        }
    }

    public static void cloned(ServerPlayer from, ServerPlayer to) {
        SkillChoices.copy(from, to);
        TreePools.copy(from, to);
        KitState.copy(from, to);
        LearnedStyles source = StyleProgression.of(from);
        LearnedStyles target = StyleProgression.of(to);
        if (source != null && target != null) {
            target.load(source.save());
        }
    }

    public static void quit(ServerPlayer player) {
        SkillTags.forget(player);
        forget(player.getUUID());
    }

    public static void forget(UUID playerId) {
        SkillGrants.forget(playerId);
        SkillEvents.forget(playerId);
        SkillCounters.forget(playerId);
        SkillScaling.forget(playerId);
        SkillChoices.forget(playerId);
        SkillDots.forget(playerId);
        PuffishCompat.forget(playerId);
    }
}
