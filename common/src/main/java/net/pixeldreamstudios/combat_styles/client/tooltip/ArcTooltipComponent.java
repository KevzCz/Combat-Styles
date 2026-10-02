package net.pixeldreamstudios.combat_styles.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import net.pixeldreamstudios.combat_styles.api.AttackShape;

public class ArcTooltipComponent implements ClientTooltipComponent {

    private static final int CELL = 3;
    private static final int GRID_WIDTH = 17;
    private static final int HALF_WIDTH = GRID_WIDTH / 2;
    private static final int FLAT_HEIGHT = 10;
    private static final int ROUND_HEIGHT = 17;
    private static final int PADDING = 2;
    private static final double FULL_CIRCLE = 330.0D;

    private static final int COLOR_GROUND = 0xFF0F0D18;
    private static final int COLOR_HORIZONTAL = 0xFF8FD7E8;
    private static final int COLOR_VERTICAL = 0xFFC08AE8;
    private static final int COLOR_REDUCED = 0xFFE88A8A;
    private static final int COLOR_LOST = 0xFF332C46;
    private static final int COLOR_ORIGIN = 0xFFFFD98A;

    private final AttackShape shape;
    private final double angle;
    private final double reach;
    private final double baseReach;

    public ArcTooltipComponent(AttackShape shape, double angle, double reach, double baseReach) {
        this.shape = shape == null ? AttackShape.HORIZONTAL_SWEEP : shape;
        this.angle = angle;
        this.reach = Math.max(0.0D, reach);
        this.baseReach = Math.max(this.reach, baseReach);
    }

    private boolean isRound() {
        return shape != AttackShape.VERTICAL_SWEEP && Math.abs(angle) >= FULL_CIRCLE;
    }

    private int rows() {
        return isRound() ? ROUND_HEIGHT : FLAT_HEIGHT;
    }

    private int originRow() {
        return isRound() ? ROUND_HEIGHT / 2 : FLAT_HEIGHT - 1;
    }

    private double radiusFor(double multiplier) {
        return (isRound() ? ROUND_HEIGHT / 2.0D : FLAT_HEIGHT - 1.0D) * multiplier;
    }

    private boolean covers(int dx, int dy, double radius) {
        if (dx == 0 && dy == 0) {
            return false;
        }
        double distance = Math.sqrt((double) dx * dx + (double) dy * dy);
        if (distance > radius) {
            return false;
        }
        if (shape == AttackShape.THRUST) {
            return Math.abs(dx) <= 1 && dy > 0;
        }
        double half = Math.toRadians(Math.abs(angle)) / 2.0D;
        return Math.abs(Math.atan2(dx, dy)) <= half;
    }

    private int fillColor() {
        if (reach < baseReach) {
            return COLOR_REDUCED;
        }
        return shape == AttackShape.VERTICAL_SWEEP ? COLOR_VERTICAL : COLOR_HORIZONTAL;
    }

    @Override
    public int getHeight() {
        return rows() * CELL + PADDING * 2;
    }

    @Override
    public int getWidth(Font font) {
        return GRID_WIDTH * CELL + PADDING * 2;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        int left = x + PADDING;
        int top = y + PADDING;
        int rows = rows();
        int origin = originRow();
        double radius = radiusFor(reach);
        double ghostRadius = radiusFor(baseReach);
        int color = fillColor();

        graphics.fill(left, top, left + GRID_WIDTH * CELL, top + rows * CELL, COLOR_GROUND);

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < GRID_WIDTH; column++) {
                int dx = column - HALF_WIDTH;
                int dy = origin - row;
                int px = left + column * CELL;
                int py = top + row * CELL;

                if (dx == 0 && dy == 0) {
                    graphics.fill(px, py, px + CELL, py + CELL, COLOR_ORIGIN);
                    continue;
                }
                if (covers(dx, dy, radius)) {
                    graphics.fill(px, py, px + CELL, py + CELL, color);
                } else if (covers(dx, dy, ghostRadius)) {
                    graphics.fill(px, py, px + CELL, py + CELL, COLOR_LOST);
                }
            }
        }
    }
}
