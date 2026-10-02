package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class PanelButton extends Button {

    private static final int FILL = 0x18FFFFFF;
    private static final int FILL_HOVER = 0x38FFFFFF;
    private static final int FILL_OFF = 0x10000000;

    protected PanelButton(int x, int y, int width, int height, Component message,
                          OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public static Builder of(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused() && active;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                active ? (hovered ? FILL_HOVER : FILL) : FILL_OFF);
        ScreenDraw.fieldOutline(graphics, getX(), getY(), getWidth(), getHeight(), hovered);

        int colour = !active ? ScreenTheme.DIM
                : hovered ? ScreenTheme.ACCENT : ScreenTheme.TEXT;
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, colour);
    }

    public static final class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x;
        private int y;
        private int width = 74;
        private int height = ScreenTheme.BUTTON_HEIGHT;
        private Component tooltip;

        private Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder tooltip(Component lines) {
            this.tooltip = lines;
            return this;
        }

        public Builder bounds(int boundsX, int boundsY, int boundsWidth, int boundsHeight) {
            this.x = boundsX;
            this.y = boundsY;
            this.width = boundsWidth;
            this.height = boundsHeight;
            return this;
        }

        public PanelButton build() {
            PanelButton button = new PanelButton(x, y, width, height, message, onPress);
            if (tooltip != null) {
                button.setTooltip(Tooltip.create(tooltip));
            }
            return button;
        }
    }
}
