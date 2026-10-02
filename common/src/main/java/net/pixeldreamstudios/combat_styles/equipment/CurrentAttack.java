package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.BetterCombatMod;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.ComboState;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.style.StyleEntry;

public record CurrentAttack(int slot, int size, String hitbox, StyleEntry entry) {

    public static CurrentAttack of(Player player) {
        if (player == null || player.level().isClientSide()) {
            return null;
        }
        AttackHand hand = CombatState.swungHand(player);
        if (hand == null || CombatState.ticksSinceSwungHand(player) > comboResetTicks(player)) {
            return null;
        }
        ComboState combo = hand.combo();
        int size = combo == null ? 0 : combo.total();
        int slot = size > 0 ? combo.current() - 1 : -1;
        String hitbox = hand.attack() == null || hand.attack().hitbox() == null
                ? null
                : hand.attack().hitbox().name();
        return new CurrentAttack(slot, size, hitbox, CombatState.swungEntry(player));
    }

    private static long comboResetTicks(Player player) {
        return Math.round(PlayerAttackHelper.getAttackCooldownTicksCapped(player)
                * BetterCombatMod.config.combo_reset_rate);
    }
}
