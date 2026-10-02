package net.pixeldreamstudios.combat_styles.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import net.pixeldreamstudios.combat_styles.equipment.AffixPlateData;

import org.joml.Matrix4f;

import java.util.List;

public class AffixPlateComponent implements ClientTooltipComponent {

    private static final int PLATE_H = 11;
    private static final int PAD_X = 4;
    private static final int GAP = 3;
    private static final int NAME_GAP = 4;
    private static final int LINE_H = 10;
    private static final int TOP_GAP = 1;
    private static final int BOTTOM_GAP = 2;
    private static final int GLYPH_H = 8;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAAAAAA;
    private static final int SHADOW = 0x55000000;
    private static final int LIGHT_PACKED = 0xF000F0;
    private static final int PLATE_LIFT = 1;

    private final Component name;
    private final FormattedCharSequence header;
    private final List<AffixPlateData.Plate> plates;
    private final List<Component> lines;

    public AffixPlateComponent(AffixPlateData data) {
        this(data, data.name() == null ? null : data.name().getVisualOrderText());
    }

    public AffixPlateComponent(AffixPlateData data, FormattedCharSequence header) {
        this.name = data.name();
        this.header = header;
        this.plates = data.plates();
        this.lines = data.lines();
    }

    private int headerWidth(Font font) {
        return header == null ? font.width(name) : font.width(header);
    }

    private int plateWidth(Font font, AffixPlateData.Plate plate) {
        return PAD_X + font.width(plate.label()) + PAD_X;
    }

    private int headerHeight() {
        return Math.max(GLYPH_H, plates.isEmpty() ? GLYPH_H : PLATE_H);
    }

    @Override
    public int getHeight() {
        return TOP_GAP + headerHeight() + BOTTOM_GAP + lines.size() * LINE_H;
    }

    @Override
    public int getWidth(Font font) {
        int header = headerWidth(font);
        for (AffixPlateData.Plate plate : plates) {
            header += NAME_GAP + plateWidth(font, plate);
        }
        int max = header;
        for (Component line : lines) {
            max = Math.max(max, font.width(line));
        }
        return max;
    }

    @Override
    public void renderText(Font font, int x, int y, Matrix4f matrix,
                           MultiBufferSource.BufferSource buffers) {
        int headerY = y + TOP_GAP + (headerHeight() - GLYPH_H) / 2;
        if (header == null) {
            font.drawInBatch(name, x, headerY, WHITE, true, matrix, buffers,
                    Font.DisplayMode.NORMAL, 0, LIGHT_PACKED);
        } else {
            font.drawInBatch(header, x, headerY, WHITE, true, matrix, buffers,
                    Font.DisplayMode.NORMAL, 0, LIGHT_PACKED);
        }

        int plateX = x + headerWidth(font) + NAME_GAP;
        for (AffixPlateData.Plate plate : plates) {
            font.drawInBatch(plate.label(), plateX + PAD_X, headerY, WHITE, true, matrix, buffers,
                    Font.DisplayMode.NORMAL, 0, LIGHT_PACKED);
            plateX += plateWidth(font, plate) + GAP;
        }

        int lineY = y + TOP_GAP + headerHeight() + BOTTOM_GAP;
        for (Component line : lines) {
            font.drawInBatch(line, x, lineY, MUTED, false, matrix, buffers,
                    Font.DisplayMode.NORMAL, 0, LIGHT_PACKED);
            lineY += LINE_H;
        }
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        if (plates.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        int plateY = y + TOP_GAP + (headerHeight() - PLATE_H) / 2 - PLATE_LIFT;
        int plateX = x + headerWidth(font) + NAME_GAP;
        for (AffixPlateData.Plate plate : plates) {
            int width = plateWidth(font, plate);
            drawPlate(graphics, plateX, plateY, width, PLATE_H, plate.fillColor());
            plateX += width + GAP;
        }
        RenderSystem.disableBlend();
    }

    private static void drawPlate(GuiGraphics graphics, int x, int y, int w, int h, int fill) {
        int top = PlateColors.lighten(fill, 0.18F);
        int bottom = PlateColors.darken(fill, 0.12F);
        int border = PlateColors.darken(fill, 0.45F);

        graphics.fill(x + 2, y + h, x + w, y + h + 1, SHADOW);
        graphics.fill(x + w, y + 2, x + w + 1, y + h, SHADOW);

        graphics.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, top, bottom);
        graphics.fillGradient(x, y + 2, x + 1, y + h - 2, top, bottom);
        graphics.fillGradient(x + w - 1, y + 2, x + w, y + h - 2, top, bottom);

        graphics.fill(x + 1, y, x + w - 1, y + 1, border);
        graphics.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
        graphics.fill(x, y + 2, x + 1, y + h - 2, border);
        graphics.fill(x + w - 1, y + 2, x + w, y + h - 2, border);

        graphics.fill(x + 2, y + 1, x + w - 2, y + 2, PlateColors.lighten(fill, 0.35F));
    }
}
