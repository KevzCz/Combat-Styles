package net.pixeldreamstudios.combat_styles.client.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.client.KeybindHandler;

@EventBusSubscriber(modid = CombatStyles.MOD_ID, value = Dist.CLIENT)
public final class KeybindHandlerImpl {

    private static KeyMapping openCustomizer;
    private static KeyMapping openEquipment;
    private static KeyMapping recallSummons;
    private static KeyMapping focusSummons;

    private KeybindHandlerImpl() {
    }

    public static void register() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        openCustomizer = new KeyMapping(
                KeybindHandler.OPEN_CUSTOMIZER,
                GLFW.GLFW_KEY_K,
                KeybindHandler.CATEGORY);
        event.register(openCustomizer);

        openEquipment = new KeyMapping(
                KeybindHandler.OPEN_EQUIPMENT,
                GLFW.GLFW_KEY_J,
                KeybindHandler.CATEGORY);
        event.register(openEquipment);

        recallSummons = new KeyMapping(
                KeybindHandler.SUMMON_RECALL,
                InputConstants.UNKNOWN.getValue(),
                KeybindHandler.CATEGORY);
        event.register(recallSummons);

        focusSummons = new KeyMapping(
                KeybindHandler.SUMMON_FOCUS,
                InputConstants.UNKNOWN.getValue(),
                KeybindHandler.CATEGORY);
        event.register(focusSummons);

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post tick) -> {
            Minecraft client = Minecraft.getInstance();
            while (openCustomizer.consumeClick()) {
                KeybindHandler.openCustomizer(client);
            }
            while (openEquipment.consumeClick()) {
                KeybindHandler.openEquipment(client);
            }
            while (recallSummons.consumeClick()) {
                KeybindHandler.recallSummons(client);
            }
            while (focusSummons.consumeClick()) {
                KeybindHandler.focusSummons(client);
            }
        });
    }
}
