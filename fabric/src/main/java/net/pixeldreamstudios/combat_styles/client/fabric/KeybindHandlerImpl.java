package net.pixeldreamstudios.combat_styles.client.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import net.pixeldreamstudios.combat_styles.client.KeybindHandler;

public final class KeybindHandlerImpl {

    private KeybindHandlerImpl() {
    }

    public static void register() {
        KeyMapping openCustomizer = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KeybindHandler.OPEN_CUSTOMIZER,
                GLFW.GLFW_KEY_K,
                KeybindHandler.CATEGORY));

        KeyMapping openEquipment = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KeybindHandler.OPEN_EQUIPMENT,
                GLFW.GLFW_KEY_J,
                KeybindHandler.CATEGORY));

        KeyMapping recallSummons = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KeybindHandler.SUMMON_RECALL,
                InputConstants.UNKNOWN.getValue(),
                KeybindHandler.CATEGORY));

        KeyMapping focusSummons = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                KeybindHandler.SUMMON_FOCUS,
                InputConstants.UNKNOWN.getValue(),
                KeybindHandler.CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
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
