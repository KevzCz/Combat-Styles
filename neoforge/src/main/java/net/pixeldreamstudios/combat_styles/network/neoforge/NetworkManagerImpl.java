package net.pixeldreamstudios.combat_styles.network.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.client.ClientConfigSync;
import net.pixeldreamstudios.combat_styles.client.ClientStyleData;
import net.pixeldreamstudios.combat_styles.client.CooldownMirror;
import net.pixeldreamstudios.combat_styles.network.ApplyPosePayload;
import net.pixeldreamstudios.combat_styles.network.ApplyWeaponCategoryPayload;
import net.pixeldreamstudios.combat_styles.network.ApplySequencePayload;
import net.pixeldreamstudios.combat_styles.network.SyncCooldownsPayload;
import net.pixeldreamstudios.combat_styles.network.ExtractStylePayload;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.network.SyncConfigPayload;
import net.pixeldreamstudios.combat_styles.network.SyncSpellLocksPayload;
import net.pixeldreamstudios.combat_styles.equipment.SpellLockState;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.equipment.ComboHold;
import net.pixeldreamstudios.combat_styles.equipment.SwingHaste;
import net.pixeldreamstudios.combat_styles.network.SwingHastePayload;
import net.pixeldreamstudios.combat_styles.network.HitboxClaimPayload;
import net.pixeldreamstudios.combat_styles.network.SwungAttackPayload;
import net.pixeldreamstudios.combat_styles.network.AlignComboPayload;
import net.pixeldreamstudios.combat_styles.network.ApplyStackEquipmentPayload;
import net.pixeldreamstudios.combat_styles.network.HoldComboPayload;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.network.SyncDefaultStylesPayload;
import net.pixeldreamstudios.combat_styles.network.SyncAffixesPayload;
import net.pixeldreamstudios.combat_styles.network.SyncLootPayload;
import net.pixeldreamstudios.combat_styles.network.SyncAttackOverridesPayload;
import net.pixeldreamstudios.combat_styles.network.SyncEquipmentPayload;
import net.pixeldreamstudios.combat_styles.network.SyncLearnedPayload;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;

@EventBusSubscriber(modid = CombatStyles.MOD_ID)
public final class NetworkManagerImpl {

    private static final String PROTOCOL_VERSION = "1";

    private NetworkManagerImpl() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                NetworkManager.syncLearned(player);
                NetworkManager.syncDefaultStyles(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
            if (event.getPlayer() == null) {
                event.getRelevantPlayers().forEach(NetworkManager::syncDefaultStyles);
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(ApplySequencePayload.TYPE, ApplySequencePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        NetworkManager.handleApplySequence(player, payload);
                    }
                }));
        registrar.playToServer(ApplyPosePayload.TYPE, ApplyPosePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        NetworkManager.handleApplyPose(player, payload);
                    }
                }));
        registrar.playToServer(ApplyWeaponCategoryPayload.TYPE, ApplyWeaponCategoryPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        NetworkManager.handleApplyWeaponCategory(player, payload);
                    }
                }));
        registrar.playToServer(ExtractStylePayload.TYPE, ExtractStylePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        NetworkManager.handleExtractStyle(player, payload);
                    }
                }));
        registrar.playToClient(HoldComboPayload.TYPE, HoldComboPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> {
                            ComboHold.queue(context.player(), payload.slots());
                            ComboHold.apply(context.player());
                        }));
        registrar.playToServer(ApplyStackEquipmentPayload.TYPE,
                ApplyStackEquipmentPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        NetworkManager.handleApplyStackEquipment(player, payload);
                    }
                }));
        registrar.playToClient(AlignComboPayload.TYPE, AlignComboPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ComboHold.align(context.player(), payload.combo())));
        registrar.playToClient(SwingHastePayload.TYPE, SwingHastePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> SwingHaste.accept(context.player(), payload.ratio(),
                                payload.swings(), payload.hitboxOrNull())));
        registrar.playToServer(SwungAttackPayload.TYPE, SwungAttackPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        NetworkManager.handleSwungAttack(player, payload);
                    }
                }));
        registrar.playToServer(HitboxClaimPayload.TYPE, HitboxClaimPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        NetworkManager.handleHitboxClaims(player, payload);
                    }
                }));
        registrar.playToClient(SyncLearnedPayload.TYPE, SyncLearnedPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientStyleData.accept(payload.copies(), payload.variants())));
        registrar.playToClient(SyncDefaultStylesPayload.TYPE, SyncDefaultStylesPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> acceptDefaults(payload)));
        registrar.playToClient(SyncEquipmentPayload.TYPE, SyncEquipmentPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        EquipmentRegistry.acceptSync(payload.definitions(),
                        payload.fromData(), payload.overridden())));
        registrar.playToClient(SyncAffixesPayload.TYPE, SyncAffixesPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        payload.applyToClient()));
        registrar.playToClient(SyncLootPayload.TYPE, SyncLootPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        payload.applyToClient()));
        registrar.playToClient(SyncAttackOverridesPayload.TYPE,
                SyncAttackOverridesPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        AttackOverrides.acceptSync(payload.overrides())));
        registrar.playToServer(EditConfigPayload.TYPE, EditConfigPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        NetworkManager.handleEditConfig(player, payload);
                    }
                }));
        registrar.playToClient(SyncSpellLocksPayload.TYPE, SyncSpellLocksPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        SpellLockState.accept(payload.toState())));
        registrar.playToClient(SyncConfigPayload.TYPE, SyncConfigPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        ClientConfigSync.accept(payload.json())));
        registrar.playToClient(SyncCooldownsPayload.TYPE, SyncCooldownsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> CooldownMirror.accept(
                        payload.remaining(), payload.total(), payload.armed(), payload.proc())));
    }

    private static void acceptDefaults(SyncDefaultStylesPayload payload) {
        payload.applyToClient();
    }

    public static void sendDefaultStylesToClient(ServerPlayer player, SyncDefaultStylesPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendEquipmentToClient(ServerPlayer player, SyncEquipmentPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendAffixesToClient(ServerPlayer player, SyncAffixesPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendCooldownsToClient(ServerPlayer player, SyncCooldownsPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendLootToClient(ServerPlayer player, SyncLootPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendAttackOverridesToClient(ServerPlayer player,
                                                   SyncAttackOverridesPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendConfigToClient(ServerPlayer player, SyncConfigPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendSpellLocksToClient(ServerPlayer player, SyncSpellLocksPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendEditConfig(EditConfigPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendApplySequence(ApplySequencePayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendApplyPose(ApplyPosePayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendApplyWeaponCategory(ApplyWeaponCategoryPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendExtractStyle(ExtractStylePayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendApplyStackEquipment(ApplyStackEquipmentPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendAlignCombo(ServerPlayer player, AlignComboPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendHoldCombo(ServerPlayer player, HoldComboPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendSwingHaste(ServerPlayer player, SwingHastePayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendSwungAttack(SwungAttackPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendHitboxClaims(HitboxClaimPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendLearnedToClient(ServerPlayer player, SyncLearnedPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
