package net.pixeldreamstudios.combat_styles.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.gui.screens.Screen;

import net.pixeldreamstudios.combat_styles.client.ClientTooltipBinding;
import net.pixeldreamstudios.combat_styles.client.SwungAttackReporter;
import net.pixeldreamstudios.combat_styles.equipment.SwingHaste;
import net.pixeldreamstudios.combat_styles.network.AlignComboPayload;
import net.pixeldreamstudios.combat_styles.network.SummonSyncPayload;
import net.pixeldreamstudios.combat_styles.client.ClientSummons;
import net.pixeldreamstudios.combat_styles.network.HoldComboPayload;
import net.pixeldreamstudios.combat_styles.network.SwingHastePayload;
import net.pixeldreamstudios.combat_styles.equipment.ComboHold;
import net.pixeldreamstudios.combat_styles.client.ClientDisconnect;
import net.pixeldreamstudios.combat_styles.client.ClientConfigSync;
import net.pixeldreamstudios.combat_styles.client.ClientStyleData;
import net.pixeldreamstudios.combat_styles.client.KeybindHandler;
import net.pixeldreamstudios.combat_styles.client.SlashWaveRenderer;
import net.pixeldreamstudios.combat_styles.client.CooldownHud;
import net.pixeldreamstudios.combat_styles.client.ClientActivationTick;
import net.pixeldreamstudios.combat_styles.client.CooldownMirror;
import net.pixeldreamstudios.combat_styles.client.tooltip.ArcTooltipComponent;
import net.pixeldreamstudios.combat_styles.client.tooltip.ArcTooltipData;
import net.pixeldreamstudios.combat_styles.entity.ModEntities;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentProviders;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.network.SyncDefaultStylesPayload;
import net.pixeldreamstudios.combat_styles.network.SyncAffixesPayload;
import net.pixeldreamstudios.combat_styles.network.SyncLootPayload;
import net.pixeldreamstudios.combat_styles.network.SyncAttackOverridesPayload;
import net.pixeldreamstudios.combat_styles.network.SyncEquipmentPayload;
import net.pixeldreamstudios.combat_styles.network.SyncCooldownsPayload;
import net.pixeldreamstudios.combat_styles.network.SyncConfigPayload;
import net.pixeldreamstudios.combat_styles.network.SyncSpellLocksPayload;
import net.pixeldreamstudios.combat_styles.equipment.SpellLockState;
import net.pixeldreamstudios.combat_styles.network.SyncLearnedPayload;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.ClientTooltipState;

public class CombatStylesFabricClient implements ClientModInitializer {

    private static void acceptDefaults(SyncDefaultStylesPayload payload) {
        payload.applyToClient();
    }

    @Override
    public void onInitializeClient() {
        KeybindHandler.register();
        ClientTooltipBinding.register();
        SwungAttackReporter.register();
        ClientPlayNetworking.registerGlobalReceiver(HoldComboPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    ComboHold.queue(context.player(), payload.slots());
                    ComboHold.apply(context.player());
                }));

        ClientPlayNetworking.registerGlobalReceiver(SummonSyncPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        ClientSummons.accept(payload)));

        ClientPlayNetworking.registerGlobalReceiver(AlignComboPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        ComboHold.align(context.player(), payload.combo())));

        ClientPlayNetworking.registerGlobalReceiver(SwingHastePayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        SwingHaste.accept(context.player(), payload.ratio(),
                                payload.swings(), payload.hitboxOrNull())));

        ClientPlayNetworking.registerGlobalReceiver(SyncLearnedPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientStyleData.accept(payload.copies(), payload.variants())));

        ClientPlayNetworking.registerGlobalReceiver(SyncDefaultStylesPayload.TYPE, (payload, context) ->
                context.client().execute(() -> acceptDefaults(payload)));

        ClientPlayNetworking.registerGlobalReceiver(SyncEquipmentPayload.TYPE, (payload, context) ->
                context.client().execute(() -> EquipmentRegistry.acceptSync(payload.definitions(),
                        payload.fromData(), payload.overridden())));

        ClientPlayNetworking.registerGlobalReceiver(SyncAffixesPayload.TYPE, (payload, context) ->
                context.client().execute(() -> payload.applyToClient()));

        ClientPlayNetworking.registerGlobalReceiver(SyncLootPayload.TYPE, (payload, context) ->
                context.client().execute(() -> payload.applyToClient()));

        ClientPlayNetworking.registerGlobalReceiver(SyncAttackOverridesPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        AttackOverrides.acceptSync(payload.overrides())));

        ClientPlayNetworking.registerGlobalReceiver(SyncSpellLocksPayload.TYPE, (payload, context) ->
                context.client().execute(() -> SpellLockState.accept(payload.toState())));

        ClientPlayNetworking.registerGlobalReceiver(SyncConfigPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientConfigSync.accept(payload.json())));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientDisconnect.clearAll();
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncCooldownsPayload.TYPE,
                (payload, context) -> context.client().execute(() -> CooldownMirror.accept(
                        payload.remaining(), payload.total(), payload.armed(), payload.proc())));

        TooltipComponentCallback.EVENT.register(data -> data instanceof ArcTooltipData arc
                ? new ArcTooltipComponent(arc.shape(), arc.angle(), arc.reach(), arc.baseReach())
                : null);

        EntityRendererRegistry.register(ModEntities.SLASH_WAVE.get(), SlashWaveRenderer::new);

        HudRenderCallback.EVENT.register((graphics, tickDelta) ->
                CooldownHud.render(graphics,
                        graphics.guiWidth(), graphics.guiHeight()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EquipmentProviders.advance();
            CooldownMirror.tick();
            ClientActivationTick.tick();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client ->
                ClientTooltipState.setExpanded(Screen.hasShiftDown()));
    }
}
