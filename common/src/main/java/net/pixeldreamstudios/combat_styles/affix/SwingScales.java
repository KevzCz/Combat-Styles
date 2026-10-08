package net.pixeldreamstudios.combat_styles.affix;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.equipment.SequenceSlots;
import net.pixeldreamstudios.combat_styles.equipment.SwingHaste;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class SwingScales {

    private SwingScales() {
    }

    private static float ownCooldownScaleFor(Player player, int comboCount) {
        if (player == null) {
            return SwingHaste.NEUTRAL_MULTIPLIER;
        }
        AttackOverride override = CombatEffectDealer.plannedAttackOverrideFor(player, comboCount);
        return override == null
                ? SwingHaste.NEUTRAL_MULTIPLIER
                : override.cooldownScale()
                        .map(value -> (float) value.amount())
                        .orElse(SwingHaste.NEUTRAL_MULTIPLIER);
    }

    private static float inheritedHasteFor(Player player, int comboCount) {
        if (player == null || comboCount <= 0) {
            return SwingHaste.NEUTRAL_MULTIPLIER;
        }
        AttackOverride previous = CombatEffectDealer.plannedAttackOverrideFor(player, comboCount - 1);
        return previous == null
                ? SwingHaste.NEUTRAL_MULTIPLIER
                : previous.hastensNext()
                        .map(value -> (float) value.amount())
                        .orElse(SwingHaste.NEUTRAL_MULTIPLIER);
    }

    public static boolean scalesAnimation(Player player, int comboCount) {
        if (player == null) {
            return false;
        }
        AttackOverride override = CombatEffectDealer.plannedAttackOverrideFor(player, comboCount);
        return override != null && override.scalesAnimation();
    }

    public static float upcomingTimingScaleFor(Player player, int comboCount) {
        if (player == null) {
            return SwingHaste.NEUTRAL_MULTIPLIER;
        }
        return ownCooldownScaleFor(player, comboCount)
                * inheritedHasteFor(player, comboCount);
    }

    public static float cooldownScaleFor(Player player, int comboCount) {
        if (player == null) {
            return SwingHaste.NEUTRAL_MULTIPLIER;
        }
        AttackOverride override = CombatEffectDealer.plannedAttackOverrideFor(player, comboCount);
        float own = ownCooldownScaleFor(player, comboCount);
        float inherited = inheritedHasteFor(player, comboCount);
        float scale = own;
        if (CombatStylesConfig.get().debugLogging()) {
            ItemStack weapon = CombatEffectDealer.swingingWeapon(player, comboCount);
            StyleSequence sequence = SequenceService.sequenceOf(weapon, player.getUUID());
            int slots = sequence.entries().size();
            CombatStyles.LOGGER.info(
                    "[CD-resolve] combo={} seqCombo={} slots={} slot={} override={} own={} inherited={} scale={}",
                    comboCount, SequenceSlots.sequenceComboOf(player, comboCount), slots,
                    slots <= 0 ? -1 : SequenceSlots.liveSlot(player, weapon, comboCount, slots),
                    override == null ? "none" : "found", own, inherited, scale);
        }
        return scale;
    }

    public static float comboWindowScaleFor(Player player, int comboCount) {
        if (player == null) {
            return SwingHaste.NEUTRAL_MULTIPLIER;
        }
        float own = ownCooldownScaleFor(player, comboCount);
        float next = ownCooldownScaleFor(player, comboCount + 1);
        AttackOverride override = CombatEffectDealer.plannedAttackOverrideFor(player, comboCount);
        float hastens = override == null
                ? SwingHaste.NEUTRAL_MULTIPLIER
                : override.hastensNext()
                        .map(value -> (float) value.amount())
                        .orElse(SwingHaste.NEUTRAL_MULTIPLIER);
        return Math.max(own, next * hastens);
    }
}
