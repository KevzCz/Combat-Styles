package net.pixeldreamstudios.combat_styles.client;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.Minecraft;

public final class KeybindHandler {

    public static final String CATEGORY = "key.categories.combat_styles";
    public static final String OPEN_CUSTOMIZER = "key.combat_styles.open_customizer";
    public static final String OPEN_EQUIPMENT = "key.combat_styles.open_equipment";

    private KeybindHandler() {
    }

    @ExpectPlatform
    public static void register() {
        throw new AssertionError();
    }

    public static void openEquipment(Minecraft client) {
        if (client.player == null || client.screen != null
                || !client.player.getAbilities().instabuild) {
            return;
        }
        client.setScreen(new EquipmentEditorScreen(null));
    }

    public static void openCustomizer(Minecraft client) {
        if (client.player == null || client.screen != null) {
            return;
        }
        client.setScreen(new CustomizerScreen(new CustomizerState(client.player)));
    }
}
