package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.item.TooltipContextAccess;

public final class ClientTooltipBinding {

    private ClientTooltipBinding() {
    }

    public static void register() {
        TooltipContextAccess.bind(
                () -> Screen.hasShiftDown() || CombatStylesConfig.get().alwaysExpandTooltips(),
                () -> Minecraft.getInstance().player);
        TooltipContextAccess.bindDetailed(Screen::hasControlDown);
    }
}
