package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class ScreenDraw {

    private static final int SHADOW_OFFSET = 2;
    private static final int SHADOW_COLOR = 0x40000000;
    private static final int THUMB_MIN = 12;
    private static final int FIELD_PAD = 3;
    private static final String ELLIPSIS = "…";
    private static final int TOOLTIP_MAX_WIDTH = 240;
    private static final int TOOLTIP_MIN_WIDTH = 120;
    private static final int TOOLTIP_EDGE = 16;

    private ScreenDraw() {
    }

    public static void tooltip(GuiGraphics graphics, Font font, List<Component> lines,
                               int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        int width = Math.max(TOOLTIP_MIN_WIDTH,
                Math.min(TOOLTIP_MAX_WIDTH, graphics.guiWidth() / 2 - TOOLTIP_EDGE));
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) {
            wrapped.addAll(font.split(line, width));
        }
        graphics.renderTooltip(font, wrapped, mouseX, mouseY);
    }

    public static void tooltip(GuiGraphics graphics, Font font, Component line,
                               int mouseX, int mouseY) {
        tooltip(graphics, font, List.of(line), mouseX, mouseY);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + SHADOW_OFFSET, y + SHADOW_OFFSET,
                x + width + SHADOW_OFFSET, y + height + SHADOW_OFFSET, SHADOW_COLOR);
        graphics.fill(x, y, x + width, y + height, ScreenTheme.PANEL);
        graphics.renderOutline(x, y, width, height, ScreenTheme.BORDER);
    }

    public static void sunken(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, ScreenTheme.SUNK);
        graphics.renderOutline(x, y, width, height, ScreenTheme.BORDER_SOFT);
    }

    public static void sectionHeader(GuiGraphics graphics, Font font, Component label,
                                     int x, int y, int width) {
        graphics.drawString(font, label, x, y, ScreenTheme.HEADING, false);
        int lineX = x + font.width(label) + 6;
        if (lineX < x + width) {
            graphics.fill(lineX, y + 4, x + width, y + 5, ScreenTheme.BORDER_SOFT);
        }
    }

    public static boolean onScrollbar(double mouseX, double mouseY, int x, int y,
                                      int height, int visible, int total) {
        return total > visible
                && isWithin(mouseX, mouseY, x, y, ScreenTheme.SCROLLBAR_WIDTH, height);
    }

    public static int scrollFromDrag(double mouseY, int y, int height,
                                     int visible, int total) {
        if (total <= visible || height <= 0) {
            return 0;
        }
        double ratio = (mouseY - y) / height;
        return Math.clamp((int) Math.round(ratio * (total - visible)), 0, total - visible);
    }

    public static void scrollbar(GuiGraphics graphics, int x, int y, int height,
                                 int visible, int total, int scroll) {
        if (total <= visible) {
            return;
        }
        graphics.fill(x, y, x + ScreenTheme.SCROLLBAR_WIDTH, y + height, ScreenTheme.TRACK);
        int thumb = Math.max(THUMB_MIN, height * visible / total);
        int travel = height - thumb;
        int offset = travel * scroll / Math.max(1, total - visible);
        graphics.fill(x, y + offset, x + ScreenTheme.SCROLLBAR_WIDTH,
                y + offset + thumb, ScreenTheme.ACCENT);
    }

    public static void rowBackground(GuiGraphics graphics, int x, int y, int width,
                                     int height, int index, boolean hovered, boolean selected) {
        if (selected) {
            graphics.fill(x, y, x + width, y + height, ScreenTheme.PRESSED);
        } else if (hovered) {
            graphics.fill(x, y, x + width, y + height, ScreenTheme.HOVER);
        } else if ((index & 1) == 1) {
            graphics.fill(x, y, x + width, y + height, ScreenTheme.ROW_ALT);
        }
    }

    public static void hoverField(GuiGraphics graphics, int textLeft, int y, int textWidth,
                                  int height, int mouseX, int mouseY) {
        int boxX = textLeft - FIELD_PAD;
        int boxWidth = textWidth + FIELD_PAD * 2;
        if (isWithin(mouseX, mouseY, boxX, y, boxWidth, height)) {
            fieldOutline(graphics, boxX, y, boxWidth, height, false);
        }
    }

    public static void fieldHighlight(GuiGraphics graphics, int x, int y, int width, int height,
                                      boolean focused) {
        outlineWith(graphics, x, y, width, height,
                focused ? ScreenTheme.ACCENT : ScreenTheme.FIELD_HOVER);
    }

    public static void fieldOutline(GuiGraphics graphics, int x, int y, int width, int height,
                                    boolean focused) {
        outlineWith(graphics, x, y, width, height,
                focused ? ScreenTheme.ACCENT : ScreenTheme.BORDER);
    }

    private static void outlineWith(GuiGraphics graphics, int x, int y, int width, int height,
                                    int colour) {
        graphics.fill(x, y, x + width, y + 1, colour);
        graphics.fill(x, y + height - 1, x + width, y + height, colour);
        graphics.fill(x, y, x + 1, y + height, colour);
        graphics.fill(x + width - 1, y, x + width, y + height, colour);
    }

    public static void accentBar(GuiGraphics graphics, int x, int y, int height, int colour) {
        graphics.fill(x, y, x + 2, y + height, colour);
    }

    public static String trim(Font font, String text, int maxWidth) {
        if (maxWidth <= 0 || text == null) {
            return "";
        }
        return font.width(text) <= maxWidth
                ? text
                : font.plainSubstrByWidth(text, maxWidth - font.width(ELLIPSIS)) + ELLIPSIS;
    }

    public static boolean isWithin(double mouseX, double mouseY,
                                   int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
