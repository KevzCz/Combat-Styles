package net.pixeldreamstudios.combat_styles.client;

import net.pixeldreamstudios.combat_styles.equipment.SubstitutionMatch;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.client.BetterCombatClientEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.equipment.ChargeMirror;
import net.pixeldreamstudios.combat_styles.equipment.ClientSwing;
import net.pixeldreamstudios.combat_styles.equipment.StyleSubstitution;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.SequenceSlots;
import net.pixeldreamstudios.combat_styles.equipment.SubstitutionScope;
import net.pixeldreamstudios.combat_styles.equipment.SwingHaste;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.network.SwungAttackPayload;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.SwingHolder;
import net.pixeldreamstudios.combat_styles.style.SwingWindow;

import java.util.List;

public final class SwungAttackReporter {

    private static long lastAnimation = -1L;
    private SwungAttackReporter() {
    }

    public static void register() {
        try {
            BetterCombatClientEvents.ATTACK_START.register(SwungAttackReporter::report);
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.error("Unable to report swung attacks to the server", throwable);
        }
    }

    private static void report(LocalPlayer player, AttackHand hand) {
        if (player == null || hand == null) {
            return;
        }
        WeaponAttributes.Attack attack = hand.attack();
        if (attack == null || attack.animation() == null) {
            return;
        }
        long now = player.level().getGameTime();
        SwingWindow.beginSwing(player);
        if (CombatDebug.enabled(player)) {
            CombatDebug.fired(player, "animation played [" + AttackInput.label() + "]",
                    attack.animation() + " — " + (lastAnimation < 0 ? "first" : (now - lastAnimation)
                            + "t since the last one"));
        }
        lastAnimation = now;
        ChargeMirror.advanceSwingCount();
        ChargeMirror.recordSwungAttack(attack);
        EquipmentDefinition substituting = SubstitutionScope.activeFor(
                player, player.getMainHandItem());
        if (substituting != null && SubstitutionMatch.provides(substituting, attack)) {
            StyleSubstitution running =
                    substituting.substitutingEffect(player).substitution().orElse(null);
            int size = running == null ? 0 : running.styles().size();
            if (ChargeMirror.holdProgress(substituting.id()) + 1 >= size) {
                ChargeMirror.markSpentLocally(substituting.id());
            }
        }
        SwingHaste.consume(player,
                attack.hitbox() == null ? null : attack.hitbox().name());
        float yaw = (float) Math.toRadians(player.getYRot());
        int slot;
        int size;
        SwingHolder.push(player);
        try {
            List<StyleEntry> entries = SequenceService
                    .sequenceOf(player.getMainHandItem(), player.getUUID()).entries();
            slot = SequenceSlots.slotOf(entries, attack,
                    hand.combo() == null ? ClientSwing.NO_SLOT : hand.combo().current() - 1);
            size = entries.size();
        } finally {
            SwingHolder.pop();
        }
        ChargeMirror.recordSwungSlot(slot, size);
        NetworkManager.sendSwungAttack(new SwungAttackPayload(
                slot, size,
                attack.animation(),
                attack.hitbox() == null ? "" : attack.hitbox().name(),
                -Mth.sin(yaw), Mth.cos(yaw)));
        PendingClaims.clear();
    }

}
