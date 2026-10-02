package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class MultiChoiceScreen extends PickerScreen<String> {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 224;
    private static final int ROW_HEIGHT = 16;
    private static final int MARK_WIDTH = 14;

    private static final String KEY_TITLE = "screen.combat_styles.multi_picker";
    private static final String KEY_SEARCH = "screen.combat_styles.multi_search";
    private static final String KEY_COUNT = "screen.combat_styles.multi_count";
    private static final String KEY_EMPTY = "screen.combat_styles.multi_empty";
    private static final String KEY_CLEAR = "screen.combat_styles.multi_clear";
    private static final String KEY_DONE = "screen.combat_styles.apply";
    private static final String MARK_ON = "✔";
    private static final String MARK_OFF = "•";

    private final Component subtitle;
    private final List<String> options = new ArrayList<>();
    private final Set<String> picked = new LinkedHashSet<>();
    private final Consumer<List<String>> onApply;

    public MultiChoiceScreen(Screen parent, Component subtitle, List<String> choices,
                             List<String> current, Consumer<List<String>> onApply) {
        super(Component.translatable(KEY_TITLE), parent);
        this.subtitle = subtitle;
        this.onApply = onApply;
        this.options.addAll(choices);
        if (current != null) {
            this.picked.addAll(current);
        }
    }

    @Override
    protected int panelWidth() {
        return PANEL_WIDTH;
    }

    @Override
    protected int panelHeight() {
        return PANEL_HEIGHT;
    }

    @Override
    protected int rowHeight() {
        return ROW_HEIGHT;
    }

    @Override
    protected List<String> choices() {
        return options;
    }

    @Override
    protected String searchTextOf(String row) {
        return StyleText.choiceName(row).getString() + " " + row;
    }

    @Override
    protected List<Component> tooltipFor(String row) {
        return RefTooltip.forChoice(row);
    }

    @Override
    protected boolean closeOnPick() {
        return false;
    }

    @Override
    protected Component searchHint() {
        return Component.translatable(KEY_SEARCH);
    }

    @Override
    protected Component headerNote() {
        return Component.translatable(KEY_COUNT, picked.size());
    }

    @Override
    protected Component emptyLabel() {
        return Component.translatable(KEY_EMPTY);
    }

    @Override
    protected void onPicked(String row) {
        if (!picked.remove(row)) {
            picked.add(row);
        }
    }

    @Override
    protected void addFooterButtons(int buttonY) {
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLEAR),
                        button -> picked.clear())
                .bounds(contentLeft(), buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_DONE), button -> {
            onApply.accept(List.copyOf(picked));
            minecraft.setScreen(parent);
        }).bounds(contentRight() - BUTTON_WIDTH, buttonY,
                BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    @Override
    protected void renderRow(GuiGraphics graphics, String row, int index,
                             int x, int y, int width, boolean hovered) {
        boolean on = picked.contains(row);
        ScreenDraw.rowBackground(graphics, x + 1, y, width - 2, rowHeight() - 1,
                index, hovered, on);
        graphics.drawString(font, on ? MARK_ON : MARK_OFF, x + TEXT_INSET, y + TEXT_INSET,
                on ? ScreenTheme.GOOD : ScreenTheme.DIM, false);
        graphics.drawString(font,
                ScreenDraw.trim(font, StyleText.choiceName(row).getString(),
                        width - MARK_WIDTH - 8),
                x + MARK_WIDTH, y + TEXT_INSET,
                on ? ScreenTheme.TEXT : ScreenTheme.MUTED, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        drawTitle(graphics, subtitle, top + TITLE_Y);
    }
}
