package net.pixeldreamstudios.combat_styles.mixin;

import net.pixeldreamstudios.combat_styles.equipment.SequenceSlots;
import net.bettercombat.network.Packets;
import net.bettercombat.network.ServerNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.CombatEffectDealer;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.equipment.PendingExtraHit;
import net.pixeldreamstudios.combat_styles.equipment.SwingDrag;

@Mixin(ServerNetwork.class)
public class AffixSwingMixin {

    @ModifyVariable(method = "handleAttackRequest", at = @At("HEAD"), argsOnly = true,
            remap = false, require = 1)
    private static Packets.C2S_AttackRequest combat_styles$decodeSweepPacket(
            Packets.C2S_AttackRequest request) {
        if (request == null) {
            return request;
        }
        boolean sweep = SwingDrag.isSweep(request.comboCount());
        boolean extra = SwingDrag.isExtra(request.comboCount());
        if (!sweep && !extra) {
            return request;
        }
        Packets.C2S_AttackRequest decoded = new Packets.C2S_AttackRequest(
                SwingDrag.decodeSweep(request.comboCount()), request.isSneaking(),
                request.selectedSlot(), request.cursorTarget(), request.entityIds());
        if (sweep) {
            SwingDrag.markSweepPacket();
        }
        SwingDrag.markExtra(SwingDrag.decodeExtra(request.comboCount()));
        return decoded;
    }

    @Inject(method = "handleAttackRequest", at = @At("HEAD"), remap = false, require = 1)
    private static void combat_styles$onSwing(Packets.C2S_AttackRequest request,
                                              MinecraftServer server,
                                              ServerPlayer player,
                                              ServerGamePacketListenerImpl listener,
                                              CallbackInfo callback) {
        int combo = SwingDrag.decodeSweep(request.comboCount());
        boolean sweep = SwingDrag.claimSweepPacket();
        int extra = SwingDrag.claimExtra();
        if (extra >= 0) {
            PendingExtraHit.enqueue(player, extra);
            PendingExtraHit.push(extra);
            SwingDrag.begin(player, combo);
            if (CombatStylesConfig.get().debugLogging()) {
                CombatStyles.LOGGER.info("[EXTRA] packet raw={} combo={} extra={} reach={}",
                        request.comboCount(), combo, extra, SwingDrag.allowance());
            }
            CombatEffectDealer.armSweepAttributes(player, combo);
            return;
        }
        PendingExtraHit.push(SwingDrag.NO_EXTRA);
        if (sweep) {
            SwingDrag.begin(player, combo);
            SwingDrag.beginSweep(player);
            if (CombatStylesConfig.get().debugLogging()) {
                CombatStyles.LOGGER.info("[DRAG-SERVER] raw={} combo={} slot={} drag={}",
                        request.comboCount(), combo,
                        SequenceSlots.sequenceComboOf(player, combo),
                        SwingDrag.dragFor(player, combo).enabled());
            }
            CombatEffectDealer.armSweepAttributes(player, combo);
            return;
        }
        SwingDrag.endSweep(player);
        CombatEffectDealer.captureSwungAttack(player, combo);
        SwingDrag.begin(player, combo);
        CombatEffectDealer.latchSwingCharges(player, combo);
        CombatEffectDealer.armSwing(player, combo);
        server.execute(() -> CombatEffectDealer.onPlayerSwing(player, combo));
    }

}
