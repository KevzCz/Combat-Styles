package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.affix.CombatEffectDealer;
import net.pixeldreamstudios.combat_styles.style.AttackDrag;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackBehaviour;
import net.pixeldreamstudios.combat_styles.style.HitboxOffset;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SwingDrag {

    private static final double MAX_TRAVEL = 32.0D;
    private static final int SWEEP_BASE = 1 << 16;
    private static final int EXTRA_SHIFT = 17;
    private static final int EXTRA_MASK = 0x7F;
    public static final int NO_EXTRA = -1;

    private static final Set<UUID> SWEEPING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> SWEPT = ConcurrentHashMap.newKeySet();

    public static void beginSweep(Player player) {
        if (player != null) {
            SWEEPING.add(player.getUUID());
        }
    }

    public static void endSweep(Player player) {
        if (player != null) {
            if (SWEEPING.remove(player.getUUID())) {
                SWEPT.add(player.getUUID());
            }
        }
    }

    public static boolean claimSwept(Player player) {
        return player != null && SWEPT.remove(player.getUUID());
    }

    public static boolean sweeping(Player player) {
        return player != null && SWEEPING.contains(player.getUUID());
    }

    private static final ThreadLocal<Boolean> SWEEP_PACKET =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static void markSweepPacket() {
        SWEEP_PACKET.set(Boolean.TRUE);
    }

    public static boolean claimSweepPacket() {
        boolean was = Boolean.TRUE.equals(SWEEP_PACKET.get());
        SWEEP_PACKET.set(Boolean.FALSE);
        return was;
    }

    public static void forgetSweep(UUID playerId) {
        SWEEPING.remove(playerId);
        SWEPT.remove(playerId);
    }

    private static final ThreadLocal<Integer> EXTRA_PACKET =
            ThreadLocal.withInitial(() -> NO_EXTRA);

    public static void markExtra(int extraIndex) {
        EXTRA_PACKET.set(extraIndex);
    }

    public static int claimExtra() {
        int was = EXTRA_PACKET.get();
        EXTRA_PACKET.set(NO_EXTRA);
        return was;
    }

    public static int encodeSweep(int comboCount) {
        return SWEEP_BASE + comboCount;
    }

    public static int encodeExtra(int comboCount, int extraIndex) {
        int encoded = SWEEP_BASE + (Math.max(0, comboCount) & (SWEEP_BASE - 1));
        return encoded | ((Math.min(Math.max(extraIndex, 0), EXTRA_MASK - 1) + 1)
                << EXTRA_SHIFT);
    }

    public static boolean isSweep(int comboCount) {
        return comboCount >= SWEEP_BASE && decodeExtra(comboCount) < 0;
    }

    public static boolean isExtra(int comboCount) {
        return decodeExtra(comboCount) >= 0;
    }

    public static int decodeSweep(int comboCount) {
        if (comboCount < SWEEP_BASE) {
            return comboCount;
        }
        return isExtra(comboCount)
                ? (comboCount - SWEEP_BASE) & (SWEEP_BASE - 1)
                : comboCount - SWEEP_BASE;
    }

    public static int decodeExtra(int comboCount) {
        if (comboCount < SWEEP_BASE) {
            return NO_EXTRA;
        }
        int packed = (comboCount >> EXTRA_SHIFT) & EXTRA_MASK;
        return packed == 0 ? NO_EXTRA : packed - 1;
    }

    private static final ThreadLocal<Double> ALLOWANCE =
            ThreadLocal.withInitial(() -> 0.0D);

    private SwingDrag() {
    }

    public static AttackDrag dragFor(Player player, int comboCount) {
        if (player == null) {
            return AttackDrag.NONE;
        }
        ItemStack weapon = CombatEffectDealer.swingingWeapon(player, comboCount);
        if (weapon.isEmpty()) {
            return AttackDrag.NONE;
        }
        return SwingDash.dragFor(player, weapon, comboCount);
    }

    public static void begin(Player player, int comboCount) {
        AttackDrag drag = dragFor(player, comboCount);
        if (!drag.enabled()) {
            ALLOWANCE.set(offsetReach(player, comboCount));
            return;
        }
        ALLOWANCE.set(offsetReach(player, comboCount) + travelDuring(player, drag));
    }

    private static double offsetReach(Player player, int comboCount) {
        ItemStack weapon = CombatEffectDealer.swingingWeapon(player, comboCount);
        if (weapon.isEmpty()) {
            return 0.0D;
        }
        AttackOverride override = CombatEffectDealer.shapedOverrideFor(player, comboCount);
        if (override == null) {
            return 0.0D;
        }
        AttackBehaviour behaviour = override.behaviourOrDefault();
        HitboxOffset offset = behaviour.offsetOrNone();
        double cursor = behaviour.placementOrNone().followsCursor()
                ? behaviour.placementOrNone().clampedRange()
                : 0.0D;
        double boxes = behaviour.boxesOrNone().reach();
        if (offset.isEmpty() && cursor <= 0.0D && boxes <= 0.0D) {
            return 0.0D;
        }
        return cursor + boxes + Math.sqrt(offset.forward() * offset.forward()
                + offset.up() * offset.up()
                + offset.right() * offset.right());
    }

    private static double travelDuring(Player player, AttackDrag drag) {
        Vec3 velocity = player.getDeltaMovement();
        double perTick = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        return Math.min(perTick * drag.ticks(), MAX_TRAVEL);
    }

    public static void end() {
        ALLOWANCE.remove();
    }

    public static double allowance() {
        return ALLOWANCE.get();
    }

    public static boolean active() {
        return ALLOWANCE.get() > 0.0D;
    }

    public static double validatedDistance(double distance) {
        double bonus = ALLOWANCE.get();
        if (bonus <= 0.0D) {
            return distance;
        }
        return Math.max(0.0D, distance - bonus * bonus);
    }
}
