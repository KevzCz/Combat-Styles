package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;

public final class Scrollbar {

    public static final int WIDTH = 4;
    public static final int GRAB_MARGIN = 4;
    private static final int MIN_THUMB = 14;

    private final int sunkColor;
    private final int thumbColor;

    private int x;
    private int top;
    private int bottom;
    private int thumbY;
    private int thumbHeight;
    private int maxScroll;
    private boolean visible;
    private boolean dragging;
    private int grabOffset;

    public Scrollbar(int sunkColor, int thumbColor) {
        this.sunkColor = sunkColor;
        this.thumbColor = thumbColor;
    }

    public void render(GuiGraphics graphics, int x, int top, int bottom,
                       int scroll, int maxScroll, int totalRows, int visibleRows) {
        this.x = x;
        this.top = top;
        this.bottom = bottom;
        this.maxScroll = maxScroll;
        this.visible = maxScroll > 0;
        if (!visible) {
            dragging = false;
            return;
        }

        int trackHeight = bottom - top;
        graphics.fill(x, top, x + WIDTH, bottom, sunkColor);
        thumbHeight = Math.max(MIN_THUMB, trackHeight * visibleRows / Math.max(1, totalRows));
        thumbHeight = Math.min(thumbHeight, trackHeight);
        thumbY = top + (trackHeight - thumbHeight) * scroll / maxScroll;
        graphics.fill(x, thumbY, x + WIDTH, thumbY + thumbHeight, thumbColor);
    }

    public boolean isOverTrack(double mouseX, double mouseY) {
        return visible && mouseX >= x - GRAB_MARGIN && mouseX < x + WIDTH + GRAB_MARGIN
                && mouseY >= top && mouseY < bottom;
    }

    public int beginDrag(double mouseX, double mouseY, int scroll) {
        if (!isOverTrack(mouseX, mouseY)) {
            return scroll;
        }
        dragging = true;
        if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
            grabOffset = (int) (mouseY - thumbY);
            return scroll;
        }
        grabOffset = thumbHeight / 2;
        return scrollFor(mouseY);
    }

    public int drag(double mouseY, int scroll) {
        return dragging ? scrollFor(mouseY) : scroll;
    }

    public void endDrag() {
        dragging = false;
    }

    public boolean isDragging() {
        return dragging;
    }

    private int scrollFor(double mouseY) {
        int trackHeight = bottom - top;
        int span = trackHeight - thumbHeight;
        if (span <= 0) {
            return 0;
        }
        double target = mouseY - grabOffset - top;
        int scroll = (int) Math.round(target * maxScroll / span);
        return Math.clamp(scroll, 0, maxScroll);
    }
}
