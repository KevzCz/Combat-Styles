package net.pixeldreamstudios.combat_styles.client.neoforge;

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

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post tick) -> {
            while (openCustomizer.consumeClick()) {
                KeybindHandler.openCustomizer(Minecraft.getInstance());
            }
            while (openEquipment.consumeClick()) {
                KeybindHandler.openEquipment(Minecraft.getInstance());
            }
        });
    }
}
