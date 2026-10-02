package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.ExtraAttack;
import net.pixeldreamstudios.combat_styles.style.InheritGroups;
import net.pixeldreamstudios.combat_styles.style.SlashMode;
import net.pixeldreamstudios.combat_styles.style.SlashPlacement;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import net.pixeldreamstudios.combat_styles.affix.AffixTuning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SlashEditorScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 300;
    private static final int HEADER = 46;
    private static final int FOOTER = 30;
    private static final int ROW_HEIGHT = 18;
    private static final int BUTTON_WIDTH = 70;
    private static final int GAP = 4;
    private static final int TITLE_Y = 10;
    private static final int HINT_Y = 24;
    private static final int LABEL_X = 6;
    private static final int TEXT_INSET = 5;
    private static final int VALUE_WIDTH = 52;
    private static final int BOX_HEIGHT = 14;
    private static final int KEY_ENTER = 257;

    private static final String KEY_TITLE = "screen.combat_styles.slash_editor";
    private static final String KEY_HINT = "screen.combat_styles.slash_editor_hint";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_TYPE = "screen.combat_styles.slash_field_type";
    private static final String KEY_MODE = "screen.combat_styles.slash_field_mode";
    private static final String KEY_FOLLOW = "screen.combat_styles.slash_field_anchor";
    private static final String KEY_ATTACK = "screen.combat_styles.slash_field_attack";
    private static final String KEY_DEFAULT_TYPE = "screen.combat_styles.slash_type_default";
    private static final String KEY_EDIT = "screen.combat_styles.slash_edit";

    private static final String[] NUMBERS = {"x", "y", "z", "yaw", "pitch", "roll", "delay"};
    private static final String LABEL_PREFIX = "screen.combat_styles.slash_field_";

    private final StylePage page;
    private final Consumer<SlashPlacement> onApply;

    private SlashPlacement slash;
    private EditBox valueBox;
    private int editing = -1;

    private boolean particleOnly;

    public SlashEditorScreen(Screen parent, StylePage page, SlashPlacement current,
                             Consumer<SlashPlacement> onApply) {
        super(Component.translatable(KEY_TITLE), parent);
        this.page = page;
        this.onApply = onApply;
        this.slash = current == null ? SlashPlacement.DEFAULT : current;
    }

    public SlashEditorScreen particlesOnly() {
        this.particleOnly = true;
        this.slash = this.slash.withMode(SlashMode.PARTICLE);
        return this;
    }

    @Override
    protected int panelWidth() {
        return PANEL_WIDTH;
    }

    @Override
    protected int panelHeight() {
        return HEADER + rowCount() * ROW_HEIGHT + FOOTER;
    }

    @Override
    protected void layout() {
        valueBox = new FieldBox(font, left + ScreenTheme.PADDING,
                top + panelHeight() - FOOTER - BOX_HEIGHT, VALUE_WIDTH, BOX_HEIGHT,
                Component.empty());
        valueBox.setMaxLength(10);
        valueBox.setVisible(false);
        addRenderableWidget(valueBox);

        int buttonY = top + panelHeight() - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH * 2 - GAP,
                        buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY), button -> {
            commit();
            onApply.accept(slash);
            minecraft.setScreen(parent);
        }).bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH, buttonY,
                BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private boolean showsParticle() {
        return slash.mode().spawnsParticle();
    }

    private boolean showsAttack() {
        return slash.mode().dealsDamage() && page != null;
    }

    protected int rowCount() {
        int rows = 2;
        if (showsParticle()) {
            rows += NUMBERS.length + 1;
        } else {
            rows += 1;
        }
        return showsAttack() ? rows + 1 : rows;
    }

    protected int rowY(int index) {
        return top + HEADER + index * ROW_HEIGHT;
    }

    private int valueX() {
        return left + PANEL_WIDTH - ScreenTheme.PADDING - VALUE_WIDTH;
    }

    private String labelOf(int row) {
        if (row == 0) {
            return KEY_MODE;
        }
        if (row == 1) {
            return KEY_TYPE;
        }
        if (!showsParticle()) {
            return row == 2 ? LABEL_PREFIX + "delay" : KEY_ATTACK;
        }
        int index = row - 2;
        if (index < NUMBERS.length) {
            return LABEL_PREFIX + NUMBERS[index];
        }
        return index == NUMBERS.length ? KEY_FOLLOW : KEY_ATTACK;
    }

    private String numberKeyOf(int row) {
        if (!showsParticle()) {
            return row == 2 ? "delay" : null;
        }
        int index = row - 2;
        return index >= 0 && index < NUMBERS.length ? NUMBERS[index] : null;
    }

    private String valueOf(int row) {
        if (row == 0) {
            return slash.mode().getSerializedName();
        }
        if (row == 1) {
            return slash.particleType() == null || slash.particleType().isBlank()
                    ? Component.translatable(KEY_DEFAULT_TYPE).getString()
                    : slash.particleType();
        }
        String number = numberKeyOf(row);
        if (number != null) {
            return raw(number);
        }
        String label = labelOf(row);
        if (KEY_FOLLOW.equals(label)) {
            return Component.translatable(slash.anchor().translationKey()).getString();
        }
        return Component.translatable(KEY_EDIT).getString();
    }

    private String raw(String key) {
        double value = switch (key) {
            case "x" -> slash.x();
            case "y" -> slash.y();
            case "z" -> slash.z();
            case "yaw" -> slash.localYaw();
            case "pitch" -> slash.pitch();
            case "roll" -> slash.roll();
            case "delay" -> slash.delayTicks();
            default -> 0.0D;
        };
        return key.equals("delay")
                ? String.valueOf((int) value)
                : StyleText.formatDecimal(value);
    }

    private void beginEdit(int row) {
        commit();
        editing = row;
        valueBox.setPosition(valueX(), rowY(row) + 2);
        valueBox.setVisible(true);
        valueBox.setValue(raw(numberKeyOf(row)));
        valueBox.setFocused(true);
        setFocused(valueBox);
    }

    private void commit() {
        if (editing < 0) {
            cancel();
            return;
        }
        String key = numberKeyOf(editing);
        if (key != null) {
            try {
                slash = slash.withField(key, Double.parseDouble(valueBox.getValue().trim()));
            } catch (NumberFormatException ignored) {
                cancel();
                return;
            }
        }
        cancel();
    }

    private void cancel() {
        editing = -1;
        valueBox.setVisible(false);
        valueBox.setFocused(false);
        setFocused(null);
    }

    private void openAttackEditor() {
        ExtraAttack current = slash.attack().orElse(ExtraAttack.DEFAULT);
        InheritGroups[] groups = {current.inheritOrAll()};
        List<String> affixes = new ArrayList<>(current.affixes());
        Map<String, AffixTuning> tuning = new LinkedHashMap<>(current.tuning());
        minecraft.setScreen(new AttackEditorScreen(this, page, current.asOverride(),
                applied -> {
                    slash = slash.withAttack(current.fromOverride(
                                    applied == null ? AttackOverride.EMPTY : applied)
                            .withInherit(groups[0])
                            .withAffixes(affixes)
                            .withTuning(tuning));
                    minecraft.setScreen(this);
                }).withInherit(groups[0], picked -> groups[0] = picked)
                .withAffixes(affixes, picked -> {
                    affixes.clear();
                    affixes.addAll(picked);
                })
                .withTuning(tuning, picked -> {
                    tuning.clear();
                    tuning.putAll(picked);
                }));
    }

    private static String nextType(String current) {
        List<String> types = SlashPlacement.TYPES;
        int index = types.indexOf(current == null ? "" : current);
        return types.get((index + 1 + types.size()) % types.size());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing >= 0) {
            if (keyCode == KEY_ENTER) {
                commit();
                return true;
            }
            if (keyCode == KEY_ESCAPE) {
                cancel();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editing >= 0 && valueBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        for (int row = 0; row < rowCount(); row++) {
            if (!ScreenDraw.isWithin(mouseX, mouseY, left + ScreenTheme.PADDING, rowY(row),
                    PANEL_WIDTH - ScreenTheme.PADDING * 2, ROW_HEIGHT)) {
                continue;
            }
            commit();
            if (row == 0) {
                if (particleOnly) {
                    return true;
                }
                slash = slash.withMode(slash.mode().next());
                rebuildWidgets();
                return true;
            }
            if (row == 1) {
                slash = slash.withParticleType(nextType(slash.particleType()));
                return true;
            }
            if (numberKeyOf(row) != null) {
                beginEdit(row);
                return true;
            }
            String label = labelOf(row);
            if (KEY_FOLLOW.equals(label)) {
                slash = slash.withAnchor(slash.anchor().next());
                return true;
            }
            openAttackEditor();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static int colourFor(SlashMode mode) {
        return switch (mode) {
            case PARTICLE -> ScreenTheme.ACCENT;
            case ATTACK -> ScreenTheme.BAD;
            case BOTH -> ScreenTheme.GOOD;
        };
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
        graphics.drawString(font, Component.translatable(KEY_HINT),
                left + ScreenTheme.PADDING, top + HINT_Y, ScreenTheme.MUTED, false);

        for (int row = 0; row < rowCount(); row++) {
            int y = rowY(row);
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY,
                    left + ScreenTheme.PADDING, y,
                    PANEL_WIDTH - ScreenTheme.PADDING * 2, ROW_HEIGHT);
            ScreenDraw.rowBackground(graphics, left + ScreenTheme.PADDING, y,
                    PANEL_WIDTH - ScreenTheme.PADDING * 2, ROW_HEIGHT - 1,
                    row, hovered, row == editing);

            graphics.drawString(font, Component.translatable(labelOf(row)),
                    left + ScreenTheme.PADDING + LABEL_X, y + TEXT_INSET,
                    ScreenTheme.TEXT, false);

            if (row == editing) {
                continue;
            }
            String value = valueOf(row);
            int colour = row == 0 ? colourFor(slash.mode())
                    : hovered ? ScreenTheme.ACCENT : ScreenTheme.MUTED;
            int valueLeft = left + PANEL_WIDTH - ScreenTheme.PADDING - font.width(value);
            ScreenDraw.hoverField(graphics, valueLeft, y + 2, font.width(value),
                    ROW_HEIGHT - 5, mouseX, mouseY);
            graphics.drawString(font, value, valueLeft, y + TEXT_INSET, colour, false);
        }
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
