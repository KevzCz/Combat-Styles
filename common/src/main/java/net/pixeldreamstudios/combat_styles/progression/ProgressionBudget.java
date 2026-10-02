package net.pixeldreamstudios.combat_styles.progression;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;

public final class ProgressionBudget {

    private ProgressionBudget() {
    }

    public static double forHolder(LivingEntity holder) {
        if (!(holder instanceof Player player)) {
            return 0.0D;
        }
        return forStyleCount(distinctStylesOf(player));
    }

    public static int distinctStylesOf(Player player) {
        boolean includeCustom = CombatStylesConfig.get().countCustomStyles();
        LearnedStyles learned = StyleProgression.of(player);
        int owned = learned == null ? 0 : learned.distinctStyleCount(includeCustom);
        if (owned > 0 || !player.level().isClientSide()) {
            return owned;
        }
        return LearnedStyles.countDistinctStyles(ClientLearnedView.copies(), includeCustom);
    }

    public static double forStyleCount(int distinct) {
        CombatStylesConfig config = CombatStylesConfig.get();
        double perStyle = config.budgetPerStyle();
        if (perStyle <= 0.0D || distinct <= 0) {
            return 0.0D;
        }

        double granted;
        if (config.budgetStyleLinear()) {
            granted = perStyle * distinct;
        } else {
            double soft = Math.max(1.0D, config.budgetStyleSoftCap());
            granted = perStyle * soft * Math.log(1.0D + distinct / soft);
        }

        double max = config.budgetStyleMax();
        return max > 0.0D ? Math.min(granted, max) : granted;
    }
}
