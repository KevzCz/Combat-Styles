package net.pixeldreamstudios.combat_styles.attribute;

import net.minecraft.world.entity.player.Player;

public final class MeleeKnockback {

    private static final ThreadLocal<Double> SCALE = new ThreadLocal<>();

    private MeleeKnockback() {
    }

    public static void begin(Player attacker) {
        if (attacker == null || attacker.level().isClientSide()) {
            return;
        }
        double scale = BudgetAttributes.valueOf(attacker, ModAttributes.MELEE_KNOCKBACK_ID,
                ModAttributes.MELEE_KNOCKBACK_DEFAULT);
        if (scale != ModAttributes.MELEE_KNOCKBACK_DEFAULT) {
            SCALE.set(scale);
        }
    }

    public static void end() {
        SCALE.remove();
    }

    public static double scaled(double strength) {
        Double scale = SCALE.get();
        return scale == null ? strength : strength * scale;
    }
}
