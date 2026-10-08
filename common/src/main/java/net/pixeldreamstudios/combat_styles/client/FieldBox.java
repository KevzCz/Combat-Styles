package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class FieldBox extends EditBox {

    private final Font font;
    private Component fullHint;

    public FieldBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.font = font;
    }

    @Override
    public void setHint(Component hint) {
        fullHint = hint;
        super.setHint(fitted(hint));
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        if (fullHint != null) {
            super.setHint(fitted(fullHint));
        }
    }

    private Component fitted(Component hint) {
        String text = hint.getString();
        String trimmed = ScreenDraw.trim(font, text, getInnerWidth());
        return trimmed.equals(text) ? hint : Component.literal(trimmed).withStyle(hint.getStyle());
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
