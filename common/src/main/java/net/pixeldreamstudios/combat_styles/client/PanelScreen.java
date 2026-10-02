package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;

public abstract class PanelScreen extends ListScreen {

    private static final int SCROLLBAR_INSET = 2;

    private List<Component> pendingTooltip;
    private boolean draggingBar;
    protected int left;
    protected int top;

    protected PanelScreen(Component title, Screen parent) {
        super(title, parent, null);
    }

    protected PanelScreen(Component title, Screen parent, Scrollbar scrollbar) {
        super(title, parent, scrollbar);
    }

    protected abstract int panelWidth();

    protected abstract int panelHeight();

    @Override
    protected int rowHeight() {
        return ScreenTheme.ROW_HEIGHT;
    }

    @Override
    protected int rowCount() {
        return 0;
    }

    @Override
    protected int listTop() {
        return top;
    }

    @Override
    protected int listBottom() {
        return top + panelHeight();
    }

    @Override
    protected void init() {
        left = (width - panelWidth()) / 2;
        top = (height - panelHeight()) / 2;
        layout();
    }

    protected void layout() {
    }

    protected int contentLeft() {
        return left + ScreenTheme.PADDING;
    }

    protected int contentWidth() {
        return panelWidth() - ScreenTheme.PADDING * 2;
    }

    protected int contentRight() {
        return left + panelWidth() - ScreenTheme.PADDING;
    }

    protected int footerTop() {
        return top + panelHeight() - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING;
    }

    protected int scrollbarX() {
        return contentRight() - SCROLLBAR_INSET;
    }

    protected void drawPanel(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, panelWidth(), panelHeight());
    }

    protected void drawWidgets(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        for (GuiEventListener child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partial);
            }
        }
    }

    protected void drawTitle(GuiGraphics graphics, Component title, int y) {
        graphics.drawString(font, title, contentLeft(), y, ScreenTheme.HEADING, false);
    }

    protected void drawScrollbar(GuiGraphics graphics) {
        ScreenDraw.scrollbar(graphics, scrollbarX(), listTop(),
                listBottom() - listTop(), visibleRows(), rowCount(), scroll);
    }

    protected boolean beginScrollDrag(double mouseX, double mouseY) {
        if (!ScreenDraw.onScrollbar(mouseX, mouseY, scrollbarX(), listTop(),
                listBottom() - listTop(), visibleRows(), rowCount())) {
            return false;
        }
        draggingBar = true;
        scroll = ScreenDraw.scrollFromDrag(mouseY, listTop(),
                listBottom() - listTop(), visibleRows(), rowCount());
        return true;
    }

    protected int rowAt(double mouseX, double mouseY) {
        int last = Math.min(rowCount(), scroll + visibleRows());
        for (int index = scroll; index < last; index++) {
            if (ScreenDraw.isWithin(mouseX, mouseY, contentLeft(), rowY(index),
                    contentWidth(), rowHeight())) {
                return index;
            }
        }
        return -1;
    }

    protected int rowY(int index) {
        return listTop() + (index - scroll) * rowHeight();
    }

    protected void queueTooltip(List<Component> lines) {
        pendingTooltip = lines;
    }

    protected void drawQueuedTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
            ScreenDraw.tooltip(graphics, font, pendingTooltip, mouseX, mouseY);
        }
        pendingTooltip = null;
    }

    protected int scrollStep() {
        return 1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.clamp(scroll - (int) Math.signum(scrollY) * scrollStep(),
                0, maxScroll());
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (beginScrollDrag(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (draggingBar) {
            scroll = ScreenDraw.scrollFromDrag(mouseY, listTop(),
                    listBottom() - listTop(), visibleRows(), rowCount());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
