package net.pixeldreamstudios.combat_styles.network.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import net.pixeldreamstudios.combat_styles.network.ApplyPosePayload;
import net.pixeldreamstudios.combat_styles.network.ApplySequencePayload;
import net.pixeldreamstudios.combat_styles.network.SyncCooldownsPayload;
import net.pixeldreamstudios.combat_styles.network.ExtractStylePayload;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.network.SyncConfigPayload;
import net.pixeldreamstudios.combat_styles.network.SyncSpellLocksPayload;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.network.SwingHastePayload;
import net.pixeldreamstudios.combat_styles.network.HitboxClaimPayload;
import net.pixeldreamstudios.combat_styles.network.SwungAttackPayload;
import net.pixeldreamstudios.combat_styles.network.AlignComboPayload;
import net.pixeldreamstudios.combat_styles.network.ApplyStackEquipmentPayload;
import net.pixeldreamstudios.combat_styles.network.HoldComboPayload;
import net.pixeldreamstudios.combat_styles.network.SyncDefaultStylesPayload;
import net.pixeldreamstudios.combat_styles.network.SyncAffixesPayload;
import net.pixeldreamstudios.combat_styles.network.SyncAttackOverridesPayload;
import net.pixeldreamstudios.combat_styles.network.SyncEquipmentPayload;
import net.pixeldreamstudios.combat_styles.network.SyncLearnedPayload;

public final class NetworkManagerImpl {

    private NetworkManagerImpl() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ApplySequencePayload.TYPE, ApplySequencePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ApplyPosePayload.TYPE, ApplyPosePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ExtractStylePayload.TYPE, ExtractStylePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SwungAttackPayload.TYPE, SwungAttackPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SwungAttackPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        NetworkManager.handleSwungAttack(context.player(), payload)));
        PayloadTypeRegistry.playC2S().register(HitboxClaimPayload.TYPE,
                HitboxClaimPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HitboxClaimPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        NetworkManager.handleHitboxClaims(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ExtractStylePayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        NetworkManager.handleExtractStyle(context.player(), payload)));
        PayloadTypeRegistry.playS2C().register(SyncLearnedPayload.TYPE, SyncLearnedPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncDefaultStylesPayload.TYPE,
                SyncDefaultStylesPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncEquipmentPayload.TYPE,
                SyncEquipmentPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncAffixesPayload.TYPE,
                SyncAffixesPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncAttackOverridesPayload.TYPE,
                SyncAttackOverridesPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncCooldownsPayload.TYPE,
                SyncCooldownsPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(HoldComboPayload.TYPE,
                HoldComboPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ApplyStackEquipmentPayload.TYPE,
                ApplyStackEquipmentPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ApplyStackEquipmentPayload.TYPE,
                (payload, context) -> context.server().execute(() ->
                        NetworkManager.handleApplyStackEquipment(context.player(), payload)));
        PayloadTypeRegistry.playS2C().register(AlignComboPayload.TYPE,
                AlignComboPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SwingHastePayload.TYPE,
                SwingHastePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ApplySequencePayload.TYPE, (payload, context) ->
                context.server().execute(() -> NetworkManager.handleApplySequence(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ApplyPosePayload.TYPE, (payload, context) ->
                context.server().execute(() -> NetworkManager.handleApplyPose(context.player(), payload)));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> {
                    NetworkManager.syncLearned(handler.getPlayer());
                    NetworkManager.syncDefaultStyles(handler.getPlayer());
                }));
        PayloadTypeRegistry.playC2S().register(EditConfigPayload.TYPE, EditConfigPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(EditConfigPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        NetworkManager.handleEditConfig(context.player(), payload)));
        PayloadTypeRegistry.playS2C().register(SyncConfigPayload.TYPE, SyncConfigPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncSpellLocksPayload.TYPE,
                SyncSpellLocksPayload.STREAM_CODEC);
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            if (!joined) {
                NetworkManager.syncDefaultStyles(player);
            }
        });
    }

    public static void sendLearnedToClient(ServerPlayer player, SyncLearnedPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendDefaultStylesToClient(ServerPlayer player, SyncDefaultStylesPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendEquipmentToClient(ServerPlayer player, SyncEquipmentPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendAffixesToClient(ServerPlayer player, SyncAffixesPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendAttackOverridesToClient(ServerPlayer player,
                                                   SyncAttackOverridesPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendCooldownsToClient(ServerPlayer player, SyncCooldownsPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendConfigToClient(ServerPlayer player, SyncConfigPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendSpellLocksToClient(ServerPlayer player, SyncSpellLocksPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendEditConfig(EditConfigPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    @Environment(EnvType.CLIENT)
    public static void sendApplySequence(ApplySequencePayload payload) {
        ClientPlayNetworking.send(payload);
    }

    @Environment(EnvType.CLIENT)
    public static void sendApplyPose(ApplyPosePayload payload) {
        ClientPlayNetworking.send(payload);
    }

    @Environment(EnvType.CLIENT)
    public static void sendExtractStyle(ExtractStylePayload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static void sendHoldCombo(ServerPlayer player, HoldComboPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendApplyStackEquipment(ApplyStackEquipmentPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static void sendAlignCombo(ServerPlayer player, AlignComboPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendSwingHaste(ServerPlayer player, SwingHastePayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void sendSwungAttack(SwungAttackPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static void sendHitboxClaims(HitboxClaimPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
