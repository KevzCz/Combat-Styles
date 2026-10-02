package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.function.Supplier;

public final class SubstitutionScope {
    private static final ThreadLocal<Integer> SUPPRESSED = ThreadLocal.withInitial(() -> 0);

    private SubstitutionScope() {
    }

    public static boolean suppressed() {
        return SUPPRESSED.get() > 0;
    }

    public static <T> T withoutSubstitution(Supplier<T> action) {
        SUPPRESSED.set(SUPPRESSED.get() + 1);
        try {
            return action.get();
        } finally {
            int depth = SUPPRESSED.get() - 1;
            if (depth <= 0) {
                SUPPRESSED.remove();
            } else {
                SUPPRESSED.set(depth);
            }
        }
    }

    public static EquipmentDefinition candidateFor(Player player, ItemStack weapon) {
        return suppressed() ? null : EquipmentRuntime.substituting(player, weapon);
    }

    public static EquipmentDefinition activeFor(Player player, ItemStack weapon) {
        if (suppressed()) {
            return null;
        }
        EquipmentDefinition definition = EquipmentRuntime.substituting(player, weapon);
        if (definition == null || player == null || !player.level().isClientSide()) {
            return definition;
        }
        return ClientPass.running(definition.id()) ? definition : null;
    }

    public static void forget(UUID playerId) {
    }

    public static AttackHand queuedAttack(Player player, int comboCount) {
        if (player == null) {
            return null;
        }
        return withoutSubstitution(
                () -> PlayerAttackHelper.getCurrentAttack(player, comboCount));
    }

    public static boolean sameAttack(WeaponAttributes.Attack a, WeaponAttributes.Attack b) {
        if (a == null || b == null) {
            return false;
        }
        if (a == b) {
            return true;
        }
        return a.animation() != null
                && a.animation().equals(b.animation())
                && a.hitbox() == b.hitbox();
    }
}
