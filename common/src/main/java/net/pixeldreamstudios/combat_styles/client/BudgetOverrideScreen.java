package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.function.Consumer;

public class BudgetOverrideScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 260;
    private static final int HINT_BOTTOM_GAP = 8;
    private static final int FIELD_HEIGHT = 16;
    private static final int BUTTON_WIDTH = 62;
    private static final int GAP = 6;
    private static final int TITLE_Y = 10;
    private static final int FIELD_Y = 34;
    private static final int HINT_Y = 54;
    private static final int MAX_LENGTH = 12;

    private static final String KEY_TITLE = "screen.combat_styles.budget_override";
    private static final String KEY_HINT = "screen.combat_styles.budget_override_hint";
    private static final String KEY_CLEAR = "screen.combat_styles.budget_override_clear";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CLOSE = "gui.back";

    private final double initial;
    private final boolean hadOverride;
    private final Consumer<Double> onApply;

    private EditBox valueBox;

    public BudgetOverrideScreen(Screen parent, double initial, boolean hadOverride,
                                Consumer<Double> onApply) {
        super(Component.translatable(KEY_TITLE), parent);
        this.initial = initial;
        this.hadOverride = hadOverride;
        this.onApply = onApply;
    }

    @Override
    protected int panelWidth() {
        return PANEL_WIDTH;
    }

    @Override
    protected int panelHeight() {
        return HINT_Y + hintLines() * font.lineHeight + HINT_BOTTOM_GAP
                + ScreenTheme.BUTTON_HEIGHT + ScreenTheme.PADDING;
    }

    @Override
    protected void layout() {
        valueBox = new FieldBox(font, left + ScreenTheme.PADDING, top + FIELD_Y,
                PANEL_WIDTH - ScreenTheme.PADDING * 2, FIELD_HEIGHT, Component.empty());
        valueBox.setMaxLength(MAX_LENGTH);
        valueBox.setValue(StyleText.formatDecimal(initial));
        addRenderableWidget(valueBox);
        setInitialFocus(valueBox);

        int buttonY = top + panelHeight() - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING;
        if (hadOverride) {
            addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLEAR), button -> {
                onApply.accept(null);
                minecraft.setScreen(parent);
            }).bounds(left + ScreenTheme.PADDING, buttonY,
                    BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        }
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH * 2 - GAP,
                        buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY), button -> apply())
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private void apply() {
        try {
            onApply.accept(Math.max(0.0D, Double.parseDouble(valueBox.getValue().trim())));
        } catch (NumberFormatException ignored) {
            return;
        }
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, panelHeight());
        if (valueBox != null && valueBox.isVisible()) {
            ScreenDraw.sunken(graphics, valueBox.getX() - 2, valueBox.getY() - 2,
                    valueBox.getWidth() + 4, valueBox.getHeight() + 4);
            valueBox.render(graphics, mouseX, mouseY, partialTick);
        }
        graphics.drawString(font, Component.translatable(KEY_TITLE),
                left + ScreenTheme.PADDING, top + TITLE_Y, ScreenTheme.HEADING, false);
        int y = top + HINT_Y;
        for (FormattedCharSequence line : font.split(
                Component.translatable(KEY_HINT), hintWidth())) {
            graphics.drawString(font, line, left + ScreenTheme.PADDING, y,
                    ScreenTheme.MUTED, false);
            y += font.lineHeight;
        }
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    private int hintWidth() {
        return PANEL_WIDTH - ScreenTheme.PADDING * 2;
    }

    private int hintLines() {
        return Math.max(1, font.split(Component.translatable(KEY_HINT), hintWidth()).size());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
