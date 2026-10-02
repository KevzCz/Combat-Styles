package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class ListScreen extends Screen {

    protected static final int KEY_ESCAPE = 256;
    protected static final int KEY_ENTER = 257;
    protected static final int KEY_NUMPAD_ENTER = 335;

    protected final Screen parent;
    protected final Scrollbar scrollbar;
    protected int scroll;

    protected ListScreen(Component title, Screen parent, Scrollbar scrollbar) {
        super(title);
        this.parent = parent;
        this.scrollbar = scrollbar;
    }

    protected abstract int rowHeight();

    protected abstract int listTop();

    protected abstract int listBottom();

    protected abstract int rowCount();

    protected int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / rowHeight());
    }

    protected int maxScroll() {
        return Math.max(0, rowCount() - visibleRows());
    }

    protected void clampScroll() {
        scroll = Math.clamp(scroll, 0, maxScroll());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.clamp(scroll - (int) scrollY, 0, maxScroll());
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (scrollbar != null && scrollbar.isDragging()) {
            scroll = scrollbar.drag(mouseY, scroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollbar != null) {
            scrollbar.endDrag();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KEY_ESCAPE) {
            if (parent == null) {
                onClose();
            } else {
                minecraft.setScreen(parent);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
