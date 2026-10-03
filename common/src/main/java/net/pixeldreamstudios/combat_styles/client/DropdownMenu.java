package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class DropdownMenu {

    public record Option(Component label, BooleanSupplier visible, Runnable action) {

        public static Option always(Component label, Runnable action) {
            return new Option(label, () -> true, action);
        }
    }

    private static final int ROW_HEIGHT = 14;
    private static final int TEXT_PADDING = 8;
    private static final int GAP = 2;
    private static final int TEXT_OFFSET = 3;

    private final AbstractWidget anchor;
    private final List<Option> options;
    private boolean open;

    public DropdownMenu(AbstractWidget anchor, List<Option> options) {
        this.anchor = anchor;
        this.options = List.copyOf(options);
    }

    private List<Option> shown() {
        List<Option> shown = new ArrayList<>();
        for (Option option : options) {
            if (option.visible().getAsBoolean()) {
                shown.add(option);
            }
        }
        return shown;
    }

    private int width(Font font, List<Option> shown) {
        int widest = anchor.getWidth();
        for (Option option : shown) {
            widest = Math.max(widest, font.width(option.label()) + TEXT_PADDING * 2);
        }
        return widest;
    }

    private int left(int width) {
        return anchor.getX() + anchor.getWidth() - width;
    }

    private int top(List<Option> shown) {
        return anchor.getY() - GAP - shown.size() * ROW_HEIGHT;
    }

    private boolean overMenu(Font font, List<Option> shown, double mouseX, double mouseY) {
        int width = width(font, shown);
        int x = left(width);
        int y = top(shown);
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < anchor.getY();
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        List<Option> shown = shown();
        open = !shown.isEmpty() && anchor.visible
                && (anchor.isMouseOver(mouseX, mouseY) || open && overMenu(font, shown, mouseX, mouseY));
        if (!open) {
            return;
        }
        int width = width(font, shown);
        int x = left(width);
        int y = top(shown);
        ScreenDraw.panel(graphics, x, y, width, shown.size() * ROW_HEIGHT);
        for (int i = 0; i < shown.size(); i++) {
            int rowY = y + i * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + width
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            ScreenDraw.rowBackground(graphics, x, rowY, width, ROW_HEIGHT, i, hovered, false);
            graphics.drawString(font, shown.get(i).label(), x + TEXT_PADDING, rowY + TEXT_OFFSET,
                    hovered ? ScreenTheme.ACCENT : ScreenTheme.TEXT, false);
        }
    }

    public boolean mouseClicked(Font font, double mouseX, double mouseY) {
        if (!open) {
            return false;
        }
        List<Option> shown = shown();
        int width = width(font, shown);
        int x = left(width);
        int y = top(shown);
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= anchor.getY() - GAP) {
            return false;
        }
        int row = (int) ((mouseY - y) / ROW_HEIGHT);
        if (row < 0 || row >= shown.size()) {
            return false;
        }
        open = false;
        shown.get(row).action().run();
        return true;
    }

    public boolean isOpen() {
        return open;
    }
}
