package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

public final class AttackConditions {

    public static final boolean SHIELD_CHECK_ALWAYS_PASSES = true;

    private AttackConditions() {
    }

    public static List<String> names() {
        return Arrays.stream(WeaponAttributes.Condition.values()).map(Enum::name).toList();
    }

    public static boolean passes(WeaponAttributes.Condition[] conditions, Player player,
                                 boolean offHand) {
        if (conditions == null || conditions.length == 0) {
            return true;
        }
        for (WeaponAttributes.Condition condition : conditions) {
            if (!passes(condition, player, offHand)) {
                return false;
            }
        }
        return true;
    }

    public static boolean passes(WeaponAttributes.Condition condition, Player player,
                                 boolean offHand) {
        if (condition == null) {
            return true;
        }
        return switch (condition) {
            case NOT_DUAL_WIELDING -> !PlayerAttackHelper.isDualWielding(player);
            case DUAL_WIELDING_ANY -> PlayerAttackHelper.isDualWielding(player);
            case DUAL_WIELDING_SAME -> PlayerAttackHelper.isDualWielding(player)
                    && player.getMainHandItem().getItem() == player.getOffhandItem().getItem();
            case DUAL_WIELDING_SAME_CATEGORY -> PlayerAttackHelper.isDualWielding(player)
                    && sameCategory(player.getMainHandItem(), player.getOffhandItem());
            case NO_OFFHAND_ITEM -> player.getOffhandItem().isEmpty();
            case OFF_HAND_SHIELD -> SHIELD_CHECK_ALWAYS_PASSES;
            case MAIN_HAND_ONLY -> !offHand;
            case OFF_HAND_ONLY -> offHand;
            case MOUNTED -> player.getVehicle() != null;
            case NOT_MOUNTED -> player.getVehicle() == null;
        };
    }

    private static boolean sameCategory(ItemStack mainHand, ItemStack offHand) {
        WeaponAttributes main = WeaponRegistry.getAttributes(mainHand);
        WeaponAttributes off = WeaponRegistry.getAttributes(offHand);
        if (main == null || off == null) {
            return false;
        }
        String mainCategory = main.category();
        String offCategory = off.category();
        if (mainCategory == null || mainCategory.isEmpty()
                || offCategory == null || offCategory.isEmpty()) {
            return false;
        }
        return mainCategory.equals(offCategory);
    }
}
