package net.pixeldreamstudios.combat_styles.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import net.pixeldreamstudios.combat_styles.client.ScreenDraw;

import java.util.List;

public final class TooltipPanel {

    private static final int PADDING = 4;
    private static final int LINE_GAP = 2;
    private static final int EDGE_MARGIN = 6;
    private static final int COLOR_BACKGROUND = 0xF0100010;
    private static final int COLOR_BORDER_TOP = 0x505A4A8A;
    private static final int COLOR_BORDER_BOTTOM = 0x5028213F;

    private TooltipPanel() {
    }

    public static void render(GuiGraphics graphics, Font font,
                              List<ClientTooltipComponent> components,
                              int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (components.isEmpty()) {
            return;
        }
        int width = contentWidth(font, components);
        int height = contentHeight(components);

        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + width + PADDING * 2 > screenWidth) {
            x = Math.max(EDGE_MARGIN, mouseX - width - PADDING * 2 - 12);
        }
        if (y + height + PADDING * 2 > screenHeight) {
            y = Math.max(EDGE_MARGIN, screenHeight - height - PADDING * 2 - EDGE_MARGIN);
        }
        if (y < EDGE_MARGIN) {
            y = EDGE_MARGIN;
        }

        renderAt(graphics, font, components, x, y);
    }

    public static void renderAt(GuiGraphics graphics, Font font,
                                List<ClientTooltipComponent> components, int left, int top) {
        if (components.isEmpty()) {
            return;
        }
        ScreenDraw.markTooltip();
        int width = contentWidth(font, components);
        int height = contentHeight(components);
        int right = left + width + PADDING * 2;
        int bottom = top + height + PADDING * 2;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        graphics.fill(left, top, right, bottom, COLOR_BACKGROUND);
        graphics.fill(left, top, right, top + 1, COLOR_BORDER_TOP);
        graphics.fill(left, bottom - 1, right, bottom, COLOR_BORDER_BOTTOM);
        graphics.fill(left, top, left + 1, bottom, COLOR_BORDER_TOP);
        graphics.fill(right - 1, top, right, bottom, COLOR_BORDER_BOTTOM);

        int cursor = top + PADDING;
        for (ClientTooltipComponent component : components) {
            component.renderText(font, left + PADDING, cursor, graphics.pose().last().pose(),
                    graphics.bufferSource());
            cursor += component.getHeight() + LINE_GAP;
        }

        cursor = top + PADDING;
        for (ClientTooltipComponent component : components) {
            component.renderImage(font, left + PADDING, cursor, graphics);
            cursor += component.getHeight() + LINE_GAP;
        }
        graphics.pose().popPose();
    }

    private static int contentWidth(Font font, List<ClientTooltipComponent> components) {
        int width = 0;
        for (ClientTooltipComponent component : components) {
            width = Math.max(width, component.getWidth(font));
        }
        return width;
    }

    private static int contentHeight(List<ClientTooltipComponent> components) {
        int height = 0;
        for (ClientTooltipComponent component : components) {
            height += component.getHeight() + LINE_GAP;
        }
        return height - LINE_GAP;
    }
}
