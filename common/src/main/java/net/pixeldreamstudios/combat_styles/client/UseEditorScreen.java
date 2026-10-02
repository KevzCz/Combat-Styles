package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.UseAnim;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.equipment.EffectCooldown;
import net.pixeldreamstudios.combat_styles.equipment.UseKind;
import net.pixeldreamstudios.combat_styles.equipment.UseSpec;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class UseEditorScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 300;
    private static final int FOOTER_GAP = 10;
    private static final int ROW_HEIGHT = 18;
    private static final int ROWS_TOP = 38;
    private static final int TEXT_INSET = 6;
    private static final int STEP_WIDTH = 14;
    private static final int STEP_GAP = 4;
    private static final int EDIT_WIDTH = 56;
    private static final int EDIT_HEIGHT = 14;
    private static final int EDIT_LENGTH = 6;
    private static final int BUTTON_WIDTH = 74;
    private static final int BUTTON_GAP = 6;
    private static final int TITLE_Y = 10;
    private static final int HINT_GAP = 12;
    private static final int LABEL_LIFT = 5;
    private static final int TICKS_PER_SECOND = 20;
    private static final int SHIFT_STEP = 20;
    private static final int PLAIN_STEP = 1;
    private static final int NO_ROW = -1;

    private static final String KEY_TITLE = "screen.combat_styles.use_editor";
    private static final String KEY_HINT = "screen.combat_styles.use_hint";
    private static final String KEY_MODE = "field.combat_styles.mode";
    private static final String KEY_POSE = "field.combat_styles.use_pose";
    private static final String KEY_HOLD = "field.combat_styles.hold_ticks";
    private static final String KEY_CHANNEL = "field.combat_styles.channel_animation";
    private static final String KEY_RELEASE = "field.combat_styles.release_animation";
    private static final String KEY_COOLDOWN = "field.combat_styles.cooldown";
    private static final String KEY_SECONDS = "screen.combat_styles.use_seconds";
    private static final String KEY_INSTANT = "screen.combat_styles.use_instant";
    private static final String KEY_INFINITE = "screen.combat_styles.use_infinite";
    private static final String KEY_CHANNEL_TICKS = "field.combat_styles.channel_ticks";
    private static final String KEY_THRESHOLD = "field.combat_styles.threshold_ticks";
    private static final String KEY_CHANNEL_START = "field.combat_styles.channel_start";
    private static final String KEY_NO_THRESHOLD = "screen.combat_styles.use_no_threshold";
    private static final String KEY_IMMEDIATE = "screen.combat_styles.use_immediate";
    private static final String KEY_ONCE = "screen.combat_styles.use_once";
    private static final String KEY_NONE = "screen.combat_styles.choice_unset";
    private static final String KEY_CLEAR = "screen.combat_styles.choice_clear";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CANCEL = "gui.cancel";

    private static final int ROW_MODE = 0;
    private static final int ROW_POSE = 1;
    private static final int ROW_HOLD = 2;
    private static final int ROW_THRESHOLD = 3;
    private static final int ROW_CHANNEL_START = 4;
    private static final int ROW_CHANNEL_TICKS = 5;
    private static final int ROW_CHANNEL = 6;
    private static final int ROW_RELEASE = 7;
    private static final int ROW_COOLDOWN = 8;
    private static final int ROWS = 9;

    private final Consumer<UseSpec> onSaved;

    private UseKind mode;
    private UseAnim pose;
    private int holdTicks;
    private int thresholdTicks;
    private int channelStart;
    private int channelTicks;
    private Optional<String> channelAnimation;
    private Optional<String> releaseAnimation;
    private int cooldownTicks;

    private FieldBox valueBox;
    private int editingRow = NO_ROW;

    public UseEditorScreen(Screen parent, UseSpec spec, Consumer<UseSpec> onSaved) {
        super(Component.translatable(KEY_TITLE), parent);
        this.onSaved = onSaved;
        UseSpec source = spec == null ? UseSpec.NONE : spec;
        this.mode = source.kind();
        this.pose = source.useAnimation();
        this.holdTicks = source.holdTicks();
        this.thresholdTicks = source.thresholdTicks();
        this.channelStart = source.channelStart();
        this.channelTicks = source.channelTicks();
        this.channelAnimation = source.channelAnimation();
        this.releaseAnimation = source.releaseAnimation();
        this.cooldownTicks = source.cooldown().ticks();
    }

    @Override
    protected int panelWidth() {
        return PANEL_WIDTH;
    }

    @Override
    protected int panelHeight() {
        return ROWS_TOP + visible() * ROW_HEIGHT + FOOTER_GAP
                + ScreenTheme.BUTTON_HEIGHT + ScreenTheme.PADDING;
    }

    private int visible() {
        int shown = 0;
        for (int row = 0; row < ROWS; row++) {
            if (!hidden(row)) {
                shown++;
            }
        }
        return shown;
    }

    private int slotOf(int row) {
        int slot = 0;
        for (int other = 0; other < row; other++) {
            if (!hidden(other)) {
                slot++;
            }
        }
        return slot;
    }

    @Override
    protected void layout() {
        valueBox = new FieldBox(font, 0, 0, EDIT_WIDTH, EDIT_HEIGHT, Component.empty());
        valueBox.setMaxLength(EDIT_LENGTH);
        valueBox.setVisible(false);
        addRenderableWidget(valueBox);

        int buttonY = footerTop();
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLEAR),
                        button -> clear())
                .bounds(contentLeft(), buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CANCEL),
                        button -> minecraft.setScreen(parent))
                .bounds(contentRight() - BUTTON_WIDTH * 2 - BUTTON_GAP, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY),
                        button -> save())
                .bounds(contentRight() - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private void clear() {
        cancelEdit();
        mode = UseKind.PRESS;
        pose = UseSpec.DEFAULT_POSE;
        holdTicks = UseSpec.NO_HOLD;
        thresholdTicks = UseSpec.NO_THRESHOLD;
        channelStart = UseSpec.IMMEDIATE;
        channelTicks = UseSpec.NO_CHANNEL;
        channelAnimation = Optional.empty();
        releaseAnimation = Optional.empty();
        cooldownTicks = 0;
    }

    private void save() {
        commitEdit();
        if (onSaved != null) {
            onSaved.accept(new UseSpec(mode, holdTicks, thresholdTicks, channelStart,
                    channelTicks, pose, channelAnimation, releaseAnimation,
                    EffectCooldown.of(cooldownTicks)));
        }
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        drawPanel(graphics);
        drawTitle(graphics, Component.translatable(KEY_TITLE), top + TITLE_Y);
        graphics.drawString(font, ScreenDraw.trim(font,
                        Component.translatable(KEY_HINT).getString(), contentWidth()),
                contentLeft(), top + TITLE_Y + HINT_GAP, ScreenTheme.MUTED, false);
        for (int row = 0; row < ROWS; row++) {
            drawRow(graphics, row, mouseX, mouseY);
        }
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    private void drawRow(GuiGraphics graphics, int row, int mouseX, int mouseY) {
        if (hidden(row)) {
            return;
        }
        int y = rowY(row);
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, contentLeft(), y,
                contentWidth(), ROW_HEIGHT);
        ScreenDraw.rowBackground(graphics, contentLeft(), y, contentWidth(), ROW_HEIGHT,
                slotOf(row), hovered, false);
        graphics.drawString(font, labelOf(row), contentLeft() + TEXT_INSET, y + LABEL_LIFT,
                ScreenTheme.TEXT, false);
        if (editingRow == row) {
            return;
        }
        if (numeric(row)) {
            drawNumber(graphics, row, y, mouseX, mouseY);
            return;
        }
        Component value = valueOf(row);
        int valueLeft = contentRight() - TEXT_INSET - font.width(value);
        ScreenDraw.hoverField(graphics, valueLeft, y + 1, font.width(value),
                ROW_HEIGHT - 2, mouseX, mouseY);
        graphics.drawString(font, value, valueLeft, y + LABEL_LIFT, ScreenTheme.ACCENT, false);
    }

    private void drawNumber(GuiGraphics graphics, int row, int y, int mouseX, int mouseY) {
        Component shown = valueOf(row);
        int numberRight = minusX() - BUTTON_GAP;
        int numberLeft = numberRight - font.width(shown);
        ScreenDraw.hoverField(graphics, numberLeft, y + 1, font.width(shown),
                ROW_HEIGHT - 2, mouseX, mouseY);
        graphics.drawString(font, shown, numberLeft, y + LABEL_LIFT, ScreenTheme.ACCENT, false);
        drawStep(graphics, minusX(), y, "-", mouseX, mouseY);
        drawStep(graphics, plusX(), y, "+", mouseX, mouseY);
    }

    private void drawStep(GuiGraphics graphics, int x, int y, String label,
                          int mouseX, int mouseY) {
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y + 1,
                STEP_WIDTH, ROW_HEIGHT - 2);
        graphics.fill(x, y + 1, x + STEP_WIDTH, y + ROW_HEIGHT - 1,
                hovered ? ScreenTheme.HOVER : ScreenTheme.TRACK);
        graphics.drawString(font, label, x + STEP_WIDTH / 2 - font.width(label) / 2,
                y + LABEL_LIFT, hovered ? ScreenTheme.ACCENT : ScreenTheme.TEXT, false);
    }

    private int plusX() {
        return contentRight() - TEXT_INSET - STEP_WIDTH;
    }

    private int minusX() {
        return plusX() - STEP_WIDTH - STEP_GAP;
    }

    private boolean numeric(int row) {
        return row == ROW_HOLD || row == ROW_THRESHOLD || row == ROW_CHANNEL_START
                || row == ROW_CHANNEL_TICKS || row == ROW_COOLDOWN;
    }

    private boolean hidden(int row) {
        return switch (row) {
            case ROW_POSE -> !mode.holds() || channelAnimation.isPresent();
            case ROW_HOLD, ROW_CHANNEL -> !mode.holds();
            case ROW_THRESHOLD -> !mode.completesOnRelease();
            case ROW_CHANNEL_START, ROW_CHANNEL_TICKS -> !mode.repeats();
            case ROW_RELEASE -> mode.repeats();
            default -> false;
        };
    }

    @Override
    protected int rowY(int row) {
        return top + ROWS_TOP + slotOf(row) * ROW_HEIGHT;
    }

    private Component labelOf(int row) {
        return switch (row) {
            case ROW_MODE -> Component.translatable(KEY_MODE);
            case ROW_POSE -> Component.translatable(KEY_POSE);
            case ROW_HOLD -> Component.translatable(KEY_HOLD);
            case ROW_THRESHOLD -> Component.translatable(KEY_THRESHOLD);
            case ROW_CHANNEL_START -> Component.translatable(KEY_CHANNEL_START);
            case ROW_CHANNEL_TICKS -> Component.translatable(KEY_CHANNEL_TICKS);
            case ROW_CHANNEL -> Component.translatable(KEY_CHANNEL);
            case ROW_RELEASE -> Component.translatable(KEY_RELEASE);
            default -> Component.translatable(KEY_COOLDOWN);
        };
    }

    private Component valueOf(int row) {
        return switch (row) {
            case ROW_MODE -> Component.literal(StyleText.prettify(mode.serializedName()));
            case ROW_POSE -> Component.literal(StyleText.prettify(poseName(pose)));
            case ROW_HOLD -> holdTicks == UseSpec.INFINITE
                    ? Component.translatable(KEY_INFINITE)
                    : ticks(holdTicks);
            case ROW_THRESHOLD -> thresholdTicks <= UseSpec.NO_THRESHOLD
                    ? Component.translatable(KEY_NO_THRESHOLD)
                    : ticks(thresholdTicks);
            case ROW_CHANNEL_START -> channelStart <= UseSpec.IMMEDIATE
                    ? Component.translatable(KEY_IMMEDIATE)
                    : ticks(channelStart);
            case ROW_CHANNEL_TICKS -> channelTicks <= UseSpec.NO_CHANNEL
                    ? Component.translatable(KEY_ONCE)
                    : ticks(channelTicks);
            case ROW_CHANNEL -> named(channelAnimation);
            case ROW_RELEASE -> named(releaseAnimation);
            default -> ticks(cooldownTicks);
        };
    }

    private Component ticks(int value) {
        if (value <= 0) {
            return Component.translatable(KEY_INSTANT);
        }
        return Component.translatable(KEY_SECONDS, value,
                String.format("%.2f", value / (double) TICKS_PER_SECOND));
    }

    private Component named(Optional<String> animation) {
        return animation.isEmpty()
                ? Component.translatable(KEY_NONE)
                : Component.literal(StyleText.prettify(animation.get()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (valueBox.isVisible() && valueBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(valueBox);
            return true;
        }
        if (editingRow != NO_ROW) {
            commitEdit();
        }
        for (int row = 0; row < ROWS; row++) {
            if (hidden(row) || !onRow(row, mouseX, mouseY)) {
                continue;
            }
            click(row, (int) mouseX, (int) mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean onRow(int row, double mouseX, double mouseY) {
        return ScreenDraw.isWithin(mouseX, mouseY, contentLeft(), rowY(row),
                contentWidth(), ROW_HEIGHT);
    }

    private void click(int row, int mouseX, int mouseY) {
        if (row == ROW_MODE) {
            pickMode();
            return;
        }
        if (row == ROW_POSE) {
            pickPose();
            return;
        }
        if (row == ROW_CHANNEL || row == ROW_RELEASE) {
            pickAnimation(row == ROW_CHANNEL);
            return;
        }
        int y = rowY(row);
        int step = hasShiftDown() ? SHIFT_STEP : PLAIN_STEP;
        if (ScreenDraw.isWithin(mouseX, mouseY, plusX(), y + 1,
                STEP_WIDTH, ROW_HEIGHT - 2)) {
            setNumber(row, numberOf(row) + step);
            return;
        }
        if (ScreenDraw.isWithin(mouseX, mouseY, minusX(), y + 1,
                STEP_WIDTH, ROW_HEIGHT - 2)) {
            setNumber(row, numberOf(row) - step);
            return;
        }
        beginEdit(row);
    }

    private int numberOf(int row) {
        return switch (row) {
            case ROW_HOLD -> holdTicks;
            case ROW_THRESHOLD -> thresholdTicks;
            case ROW_CHANNEL_START -> channelStart;
            case ROW_CHANNEL_TICKS -> channelTicks;
            default -> cooldownTicks;
        };
    }

    private void setNumber(int row, int value) {
        if (row == ROW_HOLD) {
            holdTicks = Math.max(UseSpec.INFINITE, value);
            return;
        }
        if (row == ROW_THRESHOLD) {
            thresholdTicks = Math.max(UseSpec.NO_THRESHOLD, value);
            return;
        }
        if (row == ROW_CHANNEL_START) {
            channelStart = Math.max(UseSpec.IMMEDIATE, value);
            return;
        }
        if (row == ROW_CHANNEL_TICKS) {
            channelTicks = Math.max(UseSpec.NO_CHANNEL, value);
            return;
        }
        cooldownTicks = Math.max(0, value);
    }

    private void beginEdit(int row) {
        editingRow = row;
        valueBox.setPosition(contentRight() - TEXT_INSET - EDIT_WIDTH, rowY(row) + 2);
        valueBox.setVisible(true);
        valueBox.setValue(String.valueOf(numberOf(row)));
        valueBox.setFocused(true);
        setFocused(valueBox);
    }

    private void commitEdit() {
        if (editingRow == NO_ROW) {
            return;
        }
        try {
            setNumber(editingRow, Integer.parseInt(valueBox.getValue().trim()));
        } catch (NumberFormatException ignored) {
            setNumber(editingRow, numberOf(editingRow));
        }
        cancelEdit();
    }

    private void cancelEdit() {
        editingRow = NO_ROW;
        valueBox.setVisible(false);
        valueBox.setFocused(false);
        setFocused(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingRow != NO_ROW) {
            if (keyCode == KEY_ENTER) {
                commitEdit();
                return true;
            }
            if (keyCode == KEY_ESCAPE) {
                cancelEdit();
                return true;
            }
            return valueBox.keyPressed(keyCode, scanCode, modifiers)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == KEY_ESCAPE) {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (editingRow != NO_ROW) {
            return valueBox.charTyped(character, modifiers);
        }
        return super.charTyped(character, modifiers);
    }

    private void chooseMode(UseKind picked) {
        if (mode == picked) {
            return;
        }
        mode = picked;
        if (!mode.holds()) {
            holdTicks = UseSpec.NO_HOLD;
            channelAnimation = Optional.empty();
            pose = UseSpec.DEFAULT_POSE;
        }
        if (!mode.completesOnRelease()) {
            thresholdTicks = UseSpec.NO_THRESHOLD;
        }
        if (mode.repeats()) {
            releaseAnimation = Optional.empty();
            channelTicks = Math.max(1, channelTicks);
        } else {
            channelStart = UseSpec.IMMEDIATE;
            channelTicks = UseSpec.NO_CHANNEL;
        }
    }

    private static String poseName(UseAnim option) {
        return option.name().toLowerCase(Locale.ROOT);
    }

    private void pickPose() {
        List<String> poses = new ArrayList<>();
        for (UseAnim option : UseAnim.values()) {
            if (option != UseAnim.NONE) {
                poses.add(poseName(option));
            }
        }
        minecraft.setScreen(new ChoicePickerScreen(this, Component.translatable(KEY_POSE),
                poses, poseName(pose), picked -> pose = UseSpec.poseByName(picked)));
    }

    private void pickMode() {
        List<String> modes = new ArrayList<>();
        for (UseKind option : UseKind.values()) {
            modes.add(option.serializedName());
        }
        minecraft.setScreen(new ChoicePickerScreen(this, Component.translatable(KEY_MODE),
                modes, mode.serializedName(), picked -> chooseMode(UseKind.byName(picked))));
    }

    private void pickAnimation(boolean channel) {
        minecraft.setScreen(new AnimationPickerScreen(this, picked -> {
            Optional<String> chosen = picked == null || picked.isBlank()
                    ? Optional.empty()
                    : Optional.of(picked);
            if (channel) {
                channelAnimation = chosen;
            } else {
                releaseAnimation = chosen;
            }
        }));
    }
}
