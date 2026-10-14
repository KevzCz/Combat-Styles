package net.pixeldreamstudios.combat_styles.compat.puffish;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Experience;
import net.puffish.skillsmod.api.SkillsAPI;
import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TreeRequirements {

    private static final int CHECK_INTERVAL = 20;

    private static final Set<ResourceLocation> WARNED_MISSING = ConcurrentHashMap.newKeySet();

    private TreeRequirements() {
    }

    static void tick(MinecraftServer server) {
        List<TreeRequirement> requirements = TreeRequirementLoader.requirements();
        if (requirements.isEmpty() || server.getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (TreeRequirement requirement : requirements) {
                check(server, player, requirement);
            }
        }
    }

    static void checkNow(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        for (TreeRequirement requirement : TreeRequirementLoader.requirements()) {
            check(server, player, requirement);
        }
    }

    static void setUnlocked(ServerPlayer player, ResourceLocation categoryId, boolean unlocked) {
        SkillsAPI.getCategory(categoryId).ifPresentOrElse(category -> {
            if (unlocked) {
                category.unlock(player);
            } else {
                category.lock(player);
            }
        }, () -> CombatStyles.LOGGER.warn("Unknown Puffish category {}", categoryId));
    }

    private static void check(MinecraftServer server, ServerPlayer player, TreeRequirement requirement) {
        Optional<Category> found = SkillsAPI.getCategory(requirement.category());
        if (found.isEmpty()) {
            return;
        }
        Category category = found.get();
        if (!category.isUnlocked(player)) {
            return;
        }
        if (isMet(server, player, requirement)) {
            requirement.carryExperience().ifPresent(pool -> restore(player, category, pool));
            return;
        }
        revoke(player, category, requirement);
    }

    private static boolean isMet(MinecraftServer server, ServerPlayer player, TreeRequirement requirement) {
        if (requirement.advancement().isEmpty()) {
            return true;
        }
        ResourceLocation id = requirement.advancement().get();
        AdvancementHolder advancement = server.getAdvancements().get(id);
        if (advancement == null) {
            if (WARNED_MISSING.add(id)) {
                CombatStyles.LOGGER.warn("Tree requirement for {} names unknown advancement {}; treating it as met",
                        requirement.category(), id);
            }
            return true;
        }
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void restore(ServerPlayer player, Category category, String pool) {
        int stored = TreePools.take(player, pool);
        if (stored > 0) {
            category.getExperience().ifPresent(experience -> experience.addTotal(player, stored));
        }
    }

    private static void revoke(ServerPlayer player, Category category, TreeRequirement requirement) {
        int pooled = 0;
        for (Category tree : treesOf(category, requirement)) {
            Optional<Experience> experience = tree.getExperience();
            if (experience.isPresent()) {
                pooled += experience.get().getTotal(player);
            }
            tree.resetSkills(player);
            experience.ifPresent(value -> value.setTotal(player, 0));
            tree.lock(player);
        }
        int carried = pooled;
        requirement.carryExperience().ifPresent(pool -> TreePools.add(player, pool, carried));
        requirement.removeTags().forEach(player::removeTag);
        CombatStyles.LOGGER.info("Revoked tree {} for {} (requirement no longer met, {} experience pooled)",
                requirement.category(), player.getGameProfile().getName(), carried);
    }

    private static List<Category> treesOf(Category category, TreeRequirement requirement) {
        List<Category> trees = new ArrayList<>();
        trees.add(category);
        for (ResourceLocation id : requirement.linked()) {
            SkillsAPI.getCategory(id).ifPresent(trees::add);
        }
        return trees;
    }
}
