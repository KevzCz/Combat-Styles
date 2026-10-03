package net.pixeldreamstudios.combat_styles.neoforge.client;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.pixeldreamstudios.combat_styles.client.ClientDisconnect;
import net.pixeldreamstudios.combat_styles.client.ConfigScreen;
import net.pixeldreamstudios.combat_styles.client.CooldownHud;
import net.pixeldreamstudios.combat_styles.client.ClientTooltipBinding;
import net.pixeldreamstudios.combat_styles.client.SwungAttackReporter;
import net.pixeldreamstudios.combat_styles.client.ClientActivationTick;
import net.pixeldreamstudios.combat_styles.client.CooldownMirror;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.client.SlashWaveRenderer;
import net.pixeldreamstudios.combat_styles.client.tooltip.ArcTooltipComponent;
import net.pixeldreamstudios.combat_styles.client.tooltip.ArcTooltipData;
import net.pixeldreamstudios.combat_styles.entity.ModEntities;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentProviders;
import net.pixeldreamstudios.combat_styles.style.ClientTooltipState;

@Mod(value = CombatStyles.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CombatStyles.MOD_ID, value = Dist.CLIENT)
public final class CombatStylesNeoForgeClient {

    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        CooldownHud.render(event.getGuiGraphics(),
                event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientDisconnect.clearAll();
    }

    public CombatStylesNeoForgeClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new ConfigScreen(parent));
        modBus.addListener(CombatStylesNeoForgeClient::onClientSetup);
        modBus.addListener(CombatStylesNeoForgeClient::onRegisterTooltips);
        modBus.addListener(CombatStylesNeoForgeClient::onRegisterRenderers);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientTooltipBinding::register);
        event.enqueueWork(SwungAttackReporter::register);
    }

    private static void onRegisterTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ArcTooltipData.class, data -> new ArcTooltipComponent(
                data.shape(), data.angle(), data.reach(), data.baseReach()));
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SLASH_WAVE.get(), SlashWaveRenderer::new);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientTooltipState.setExpanded(Screen.hasShiftDown());
        EquipmentProviders.advance();
        CooldownMirror.tick();
        ClientActivationTick.tick();
    }

}
