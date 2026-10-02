package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class FieldBox extends EditBox {

    public FieldBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        if (!isVisible()) {
            return;
        }
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, getX(), getY(),
                getWidth(), getHeight());
        if (!hovered && !isFocused()) {
            return;
        }
        ScreenDraw.fieldHighlight(graphics, getX() - 1, getY() - 1,
                getWidth() + 2, getHeight() + 2, isFocused());
    }
}
