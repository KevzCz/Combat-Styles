package net.pixeldreamstudios.combat_styles.api;

import net.minecraft.world.entity.player.Player;

public interface SwingTimingInfo {

    float NEUTRAL = 1.0F;

    float MIN_MULTIPLIER = 0.05F;

    float MAX_MULTIPLIER = 4.0F;

    float MIN_SWING_TICKS = 2.0F;

    float cooldownMultiplier(Player player);

    float thisSwingMultiplier(Player player);

    float pendingMultiplier(Player player);

    int pendingSwings(Player player);

    String pendingHitbox(Player player);

    boolean hasPending(Player player);

    long swingIndex(Player player);

    boolean dualWielding(Player player);

    boolean swungWithOffHand(Player player, int comboCount);

    int sequenceSlot(Player player, int comboCount, int sequenceSize);

    void scaleThisSwing(Player player, float multiplier);

    void hastenNextSwings(Player player, float multiplier, int swings, String hitbox);

    void clear(Player player);
}
