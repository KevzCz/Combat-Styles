package net.pixeldreamstudios.combat_styles.api;

import net.pixeldreamstudios.combat_styles.affix.CombatEffectDealer;
import net.pixeldreamstudios.combat_styles.equipment.SequenceSlots;
import net.minecraft.world.entity.player.Player;

import net.bettercombat.logic.PlayerAttackHelper;
import net.pixeldreamstudios.combat_styles.equipment.SwingHaste;

public final class SwingTiming implements SwingTimingInfo {

    public static final SwingTiming INSTANCE = new SwingTiming();

    private SwingTiming() {
    }

    @Override
    public float cooldownMultiplier(Player player) {
        return SwingHaste.clamped(player);
    }

    @Override
    public float thisSwingMultiplier(Player player) {
        return SwingHaste.selfMultiplier(player);
    }

    @Override
    public float pendingMultiplier(Player player) {
        return SwingHaste.ratio(player);
    }

    @Override
    public int pendingSwings(Player player) {
        return SwingHaste.pendingSwings(player);
    }

    @Override
    public String pendingHitbox(Player player) {
        return SwingHaste.pendingHitbox(player);
    }

    @Override
    public boolean hasPending(Player player) {
        return SwingHaste.armed(player);
    }

    @Override
    public long swingIndex(Player player) {
        return SwingHaste.swingIndex(player);
    }

    @Override
    public boolean dualWielding(Player player) {
        return player != null && PlayerAttackHelper.isDualWielding(player);
    }

    @Override
    public boolean swungWithOffHand(Player player, int comboCount) {
        return SequenceSlots.swungWithOffHand(player, comboCount);
    }

    @Override
    public int sequenceSlot(Player player, int comboCount, int sequenceSize) {
        return SequenceSlots.liveSlot(player,
                CombatEffectDealer.swingingWeapon(player, comboCount), comboCount, sequenceSize);
    }

    @Override
    public void scaleThisSwing(Player player, float multiplier) {
        SwingHaste.grantNow(player, multiplier);
    }

    @Override
    public void hastenNextSwings(Player player, float multiplier, int swings, String hitbox) {
        SwingHaste.grant(player, multiplier, swings, hitbox);
    }

    @Override
    public void clear(Player player) {
        if (player != null) {
            SwingHaste.forget(player.getUUID());
        }
    }
}
