package net.pixeldreamstudios.combat_styles.client;

import java.util.List;

public record HudLayout(HudDensity density, boolean vertical, boolean flipped, int cellWidth,
                        int cellHeight, int laneWidth, int width, int height) {

    public static final int SLOT = 18;
    public static final int GAP = 2;
    public static final int PAD = 3;
    public static final int LANE = 3;
    public static final int LANE_GAP = 1;
    public static final int LANE_INSET = 3;
    public static final int TICK = 1;
    public static final int TIMER_GAP = 2;
    public static final int TIMER_WIDTH = 12;

    public static HudLayout of(List<HudRow> rows, HudDensity density, boolean vertical,
                               boolean flipped, int timerWidth) {
        int lanes = 0;
        for (HudRow row : rows) {
            lanes = Math.max(lanes, row.lanes().size());
        }
        int laneWidth = laneWidthFor(density, lanes);
        int timer = density == HudDensity.EXPANDED && timerWidth > 0
                ? TIMER_GAP + timerWidth
                : 0;
        int cellWidth = SLOT + laneWidth + timer;
        int count = rows.size();
        int width = PAD * 2 + (vertical
                ? cellWidth
                : count * cellWidth + Math.max(0, count - 1) * GAP);
        int height = PAD * 2 + (vertical
                ? count * SLOT + Math.max(0, count - 1) * GAP
                : SLOT);
        return new HudLayout(density, vertical, flipped, cellWidth, SLOT, laneWidth, width, height);
    }

    private static int laneWidthFor(HudDensity density, int lanes) {
        if (lanes <= 0) {
            return 0;
        }
        if (density == HudDensity.MINIMAL) {
            return TICK + LANE_INSET;
        }
        return LANE_INSET + lanes * LANE + Math.max(0, lanes - 1) * LANE_GAP;
    }

    public int itemX(int cellX) {
        return flipped ? cellX + cellWidth - SLOT : cellX;
    }

    public int laneX(int cellX, int index) {
        int offset = LANE_INSET + index * (LANE + LANE_GAP);
        return flipped ? itemX(cellX) - offset - LANE : itemX(cellX) + SLOT + offset;
    }

    public int tickX(int cellX) {
        return flipped ? itemX(cellX) + SLOT + LANE_INSET : itemX(cellX) - TICK - LANE_INSET;
    }

    public int timerX(int cellX, int textWidth) {
        return flipped
                ? itemX(cellX) - laneWidth - TIMER_GAP - textWidth
                : itemX(cellX) + SLOT + laneWidth + TIMER_GAP;
    }

    public int advance() {
        return vertical ? cellHeight + GAP : cellWidth + GAP;
    }
}
