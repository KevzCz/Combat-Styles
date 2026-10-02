package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.AffixText;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class AffixTuningScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 224;
    private static final int PADDING = 10;
    private static final int HEADER = 58;
    private static final int FOOTER = 46;
    private static final int ROW_HEIGHT = 20;
    private static final int SECTION_HEIGHT = 13;
    private static final int STEP_WIDTH = 16;
    private static final int VALUE_WIDTH = 46;
    private static final int VALUE_MAX = 10;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int BUTTON_WIDTH = 74;
    private static final int BUTTON_HEIGHT = 18;

    private static final int COLOR_SHADE = ScreenTheme.SHADE;
    private static final int COLOR_BORDER = ScreenTheme.BORDER;
    private static final int COLOR_ROW_ALT = ScreenTheme.ROW_ALT;
    private static final int COLOR_HEADING = ScreenTheme.HEADING;
    private static final int COLOR_TEXT = ScreenTheme.TEXT;
    private static final int COLOR_MUTED = ScreenTheme.MUTED;
    private static final int COLOR_ACCENT = ScreenTheme.ACCENT;
    private static final int COLOR_CHANGED = ScreenTheme.CHANGED;

    private static final String KEY_TITLE = "screen.combat_styles.affix_tuning";
    private static final String KEY_HINT = "screen.combat_styles.affix_tuning_hint";
    private static final String KEY_RESET = "screen.combat_styles.reset";
    private static final String KEY_CLEARED = "screen.combat_styles.effect_cleared";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_SECTION_COST = "screen.combat_styles.section_cost";
    private static final String KEY_SECTION_TIMING = "screen.combat_styles.section_timing";
    private static final String KEY_SECTION_DISPLAY = "screen.combat_styles.section_display";
    private static final String KEY_RARITY = "screen.combat_styles.field_rarity";
    private static final String KEY_ON_HIT = "screen.combat_styles.field_on_hit";
    private static final String KEY_ON_SWING = "screen.combat_styles.field_on_swing";
    private static final String KEY_SECTION_EFFECTS = "screen.combat_styles.section_effects";
    private static final String KEY_EDIT = "screen.combat_styles.edit";
    private static final String KEY_TRAIL = "screen.combat_styles.field_trail";
    private static final String KEY_TRAIL_HINT = "screen.combat_styles.field_trail_hint";
    private static final String KEY_GLOWS = "screen.combat_styles.field_glows";

    private static final String FIELD_COST = "cost_modifier";
    private static final String FIELD_DAMAGE = "damage_multiplier";
    private static final String FIELD_CHANCE = "chance";
    private static final String FIELD_COOLDOWN = "cooldown";

    private static final int KEY_ENTER = 257;

    private record Field(String key, double base, double step, double min, double max,
                         String sectionKey) {
    }

    private final StyleAffix affix;
    private final Consumer<AffixTuning> onApply;

    private final List<Field> fields = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();

    private StyleRarity rarity;
    private EditBox trailBox;
    private EditBox valueBox;
    private int editingIndex = -1;
    private String trailColor;
    private boolean trailGlows;
    private boolean draggingList;
    private final Optional<StyleAffix.TrailColor> baseTrail;
    private Optional<AffixEffects.OnHit> onHit;
    private Optional<AffixEffects.OnHit> onSwing;

    public AffixTuningScreen(Screen parent, StyleAffix affix, AffixTuning current,
                             Consumer<AffixTuning> onApply) {
        super(Component.translatable(KEY_TITLE), parent);
        this.affix = affix;
        this.onApply = onApply;

        AffixTuning tuning = current == null ? AffixTuning.NONE : current;
        rarity = tuning.rarity().orElse(affix.rarity());
        onHit = tuning.onHit();
        onSwing = tuning.onSwing();
        baseTrail = affix.trail();
        StyleAffix.TrailColor startTrail = tuning.trail().or(() -> affix.trail()).orElse(null);
        trailColor = startTrail == null ? "" : startTrail.color();
        trailGlows = startTrail == null || startTrail.glows();

        add(FIELD_COST, affix.costModifier(), tuning.costModifier().orElse(affix.costModifier()),
                0.05D, 0.0D, 10.0D, KEY_SECTION_COST);
        add(FIELD_DAMAGE, affix.damageMultiplier(),
                tuning.damageMultiplier().orElse(affix.damageMultiplier()),
                0.05D, 0.0D, 10.0D, KEY_SECTION_COST);
        add(FIELD_CHANCE, 1.0D, tuning.chance().orElse(1.0D), 0.05D, 0.0D, 1.0D,
                KEY_SECTION_TIMING);
        add(FIELD_COOLDOWN, 0.0D, tuning.cooldown().orElse(0), 5.0D, 0.0D, 600.0D,
                KEY_SECTION_TIMING);
    }

    private void add(String key, double base, double value, double step, double min, double max,
                     String sectionKey) {
        fields.add(new Field(key, base, step, min, max, sectionKey));
        values.add(value);
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
    protected void layout() {
        scroll = Math.clamp(scroll, 0, maxScroll());

        trailBox = new FieldBox(font, left + PADDING + 74, trailRowY(),
                72, 12, Component.translatable(KEY_TRAIL));
        trailBox.setMaxLength(9);
        trailBox.setHint(Component.translatable(KEY_TRAIL_HINT).withStyle(ChatFormatting.DARK_GRAY));
        trailBox.setValue(trailColor);
        trailBox.setResponder(value -> trailColor = value);
        addRenderableWidget(trailBox);

        valueBox = new FieldBox(font, left + PADDING, listTop(),
                VALUE_WIDTH, ROW_HEIGHT - 6, Component.empty());
        valueBox.setMaxLength(VALUE_MAX);
        valueBox.setVisible(false);
        addRenderableWidget(valueBox);
        editingIndex = -1;

        int buttonY = top + PANEL_HEIGHT - 24;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_RESET), button -> resetAll())
                .bounds(left + PADDING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH * 2 - 6, buttonY,
                        BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY), button -> apply())
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private void resetAll() {
        for (int i = 0; i < fields.size(); i++) {
            values.set(i, fields.get(i).base());
        }
        onHit = Optional.empty();
        onSwing = Optional.empty();
        rarity = affix.rarity();
        trailColor = baseTrail.map(StyleAffix.TrailColor::color).orElse("");
        trailGlows = baseTrail.map(StyleAffix.TrailColor::glows).orElse(true);
        trailBox.setValue(trailColor);
    }

    private Optional<StyleAffix.TrailColor> parsedTrail() {
        String raw = trailColor == null ? "" : trailColor.trim();
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;
        if (hex.length() != 6 && hex.length() != 8) {
            return Optional.empty();
        }
        try {
            Long.parseLong(hex, 16);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
        return Optional.of(new StyleAffix.TrailColor("#" + hex.toLowerCase(Locale.ROOT), trailGlows));
    }

    private Optional<StyleAffix.TrailColor> tunedTrail() {
        Optional<StyleAffix.TrailColor> parsed = parsedTrail();
        return parsed.isPresent() && parsed.equals(baseTrail) ? Optional.empty() : parsed;
    }

    private void apply() {
        Optional<Double> cost = changed(0) ? Optional.of(values.get(0)) : Optional.empty();
        Optional<Double> damage = changed(1) ? Optional.of(values.get(1)) : Optional.empty();
        Optional<Double> chance = changed(2) ? Optional.of(values.get(2)) : Optional.empty();
        Optional<Integer> cooldown = changed(3)
                ? Optional.of((int) Math.round(values.get(3)))
                : Optional.empty();
        Optional<StyleRarity> tunedRarity = rarity == affix.rarity()
                ? Optional.empty()
                : Optional.of(rarity);

        AffixTuning tuning = new AffixTuning(cost, damage, chance, cooldown, tunedTrail(),
                Optional.empty(), tunedRarity, Optional.empty(), onHit, onSwing);
        onApply.accept(tuning.isEmpty() ? null : tuning);
        minecraft.setScreen(parent);
    }

    private void openEffectTree(String labelKey, Optional<AffixEffects.OnHit> current,
                                Consumer<Optional<AffixEffects.OnHit>> sink) {
        AffixEffects.OnHit seed = current.orElseGet(() -> labelKey.equals(KEY_ON_HIT)
                ? affix.onHit()
                : affix.onSwing());
        minecraft.setScreen(new EffectTreeScreen(this,
                Component.translatable(labelKey), seed, sink)
                .withTrigger(labelKey.equals(KEY_ON_HIT)
                        ? EffectSchema.Trigger.ON_HIT
                        : EffectSchema.Trigger.ON_SWING));
    }

    private boolean changed(int index) {
        return values.get(index) != fields.get(index).base();
    }

    private int contentHeight() {
        int height = SECTION_HEIGHT + ROW_HEIGHT * 2;
        String section = null;
        for (Field field : fields) {
            if (!field.sectionKey().equals(section)) {
                section = field.sectionKey();
                height += SECTION_HEIGHT;
            }
            height += ROW_HEIGHT;
        }
        return height + SECTION_HEIGHT + ROW_HEIGHT;
    }

    private int trailRowY() {
        return top + PANEL_HEIGHT - FOOTER + 4;
    }

    protected int listTop() {
        return top + HEADER;
    }

    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER - 14;
    }

    protected int maxScroll() {
        return Math.max(0, contentHeight() - (listBottom() - listTop()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, width, height, COLOR_SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(font, Component.translatable(KEY_TITLE),
                left + PADDING, top + 10, COLOR_HEADING, false);
        graphics.drawString(font, AffixText.name(affix).copy().withStyle(affix.rarity().color()),
                left + PADDING, top + 24, COLOR_TEXT, false);
        graphics.drawString(font, Component.translatable(KEY_HINT),
                left + PADDING, top + 38, COLOR_MUTED, false);

        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        ScreenDraw.sunken(graphics, x - 2, listTop() - 2, listWidth + 2, (listBottom()) - (listTop()) + 4);

        graphics.enableScissor(x - 2, listTop() - 2, x + listWidth, listBottom() + 2);
        String section = null;
        int y = listTop() - scroll;
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (!field.sectionKey().equals(section)) {
                section = field.sectionKey();
                drawSection(graphics, section, x, y, listWidth);
                y += SECTION_HEIGHT;
            }
            drawRow(graphics, field, values.get(i), x, y, listWidth, i, mouseX, mouseY);
            y += ROW_HEIGHT;
        }

        drawSection(graphics, KEY_SECTION_DISPLAY, x, y, listWidth);
        y += SECTION_HEIGHT;
        drawRarityRow(graphics, x, y, listWidth, mouseX, mouseY);
        y += ROW_HEIGHT;
        drawSection(graphics, KEY_SECTION_EFFECTS, x, y, listWidth);
        y += SECTION_HEIGHT;
        drawEffectRow(graphics, KEY_ON_HIT, onHit, x, y, listWidth, mouseX, mouseY);
        y += ROW_HEIGHT;
        drawEffectRow(graphics, KEY_ON_SWING, onSwing, x, y, listWidth, mouseX, mouseY);
        graphics.disableScissor();

        int trailY = trailRowY();
        boolean trailChanged = tunedTrail().isPresent();
        graphics.drawString(font, Component.translatable(KEY_TRAIL), left + PADDING, trailY + 2,
                trailChanged ? COLOR_CHANGED : COLOR_TEXT, false);
        Optional<StyleAffix.TrailColor> preview = parsedTrail();
        int swatchX = left + PADDING + 150;
        graphics.fill(swatchX, trailY, swatchX + 12, trailY + 12,
                preview.map(StyleAffix.TrailColor::argb).orElse(0xFF3A352C));
        graphics.renderOutline(swatchX, trailY, 12, 12, COLOR_BORDER);
        int glowX = swatchX + 18;
        boolean glowHovered = ScreenDraw.isWithin(mouseX, mouseY, glowX, trailY, 58, 12);
        graphics.fill(glowX, trailY, glowX + 58, trailY + 12, glowHovered ? 0x40FFFFFF : 0x20FFFFFF);
        graphics.drawString(font, Component.translatable(KEY_GLOWS).copy()
                        .append(trailGlows ? " ✔" : " ✘"),
                glowX + 3, trailY + 2, trailGlows ? COLOR_ACCENT : COLOR_MUTED, false);

        int viewport = listBottom() - listTop();
        ScreenDraw.scrollbar(graphics, x + listWidth - SCROLLBAR_WIDTH - 1, listTop(),
                viewport, viewport, contentHeight(), scroll);
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSection(GuiGraphics graphics, String key, int x, int y, int listWidth) {
        if (y + SECTION_HEIGHT < listTop() || y > listBottom()) {
            return;
        }
        graphics.drawString(font, Component.translatable(key), x + 4, y + 3, COLOR_HEADING, false);
        graphics.fill(x + 4, y + SECTION_HEIGHT - 2, x + listWidth - 4,
                y + SECTION_HEIGHT - 1, 0x30FFFFFF);
    }

    private void drawRow(GuiGraphics graphics, Field field, double value, int x, int y,
                         int listWidth, int index, int mouseX, int mouseY) {
        if (y + ROW_HEIGHT < listTop() || y > listBottom()) {
            return;
        }
        boolean isChanged = value != field.base();
        if ((index & 1) == 1) {
            graphics.fill(x, y, x + listWidth - SCROLLBAR_WIDTH - 2, y + ROW_HEIGHT, COLOR_ROW_ALT);
        }
        if (isChanged) {
            graphics.fill(x, y, x + 2, y + ROW_HEIGHT, COLOR_CHANGED);
        }

        graphics.drawString(font, Component.translatable("screen.combat_styles.field_" + field.key()),
                x + 6, y + 6, isChanged ? COLOR_CHANGED : COLOR_TEXT, false);

        int right = x + listWidth - SCROLLBAR_WIDTH - 6;
        if (editingIndex != index) {
            String shown = StyleText.formatDecimal(value);
            int valueLeft = right - STEP_WIDTH * 2 - 12 - font.width(shown);
            ScreenDraw.hoverField(graphics, valueLeft, y + 3, font.width(shown),
                    ROW_HEIGHT - 6, mouseX, mouseY);
            graphics.drawString(font, shown, valueLeft, y + 6,
                    isChanged ? COLOR_CHANGED : COLOR_MUTED, false);
        }

        drawStep(graphics, right - STEP_WIDTH * 2 - 4, y, "-", mouseX, mouseY);
        drawStep(graphics, right - STEP_WIDTH, y, "+", mouseX, mouseY);
    }

    private void drawRarityRow(GuiGraphics graphics, int x, int y, int listWidth,
                               int mouseX, int mouseY) {
        if (y + ROW_HEIGHT < listTop() || y > listBottom()) {
            return;
        }
        boolean isChanged = rarity != affix.rarity();
        if (isChanged) {
            graphics.fill(x, y, x + 2, y + ROW_HEIGHT, COLOR_CHANGED);
        }
        graphics.drawString(font, Component.translatable(KEY_RARITY), x + 6, y + 6,
                isChanged ? COLOR_CHANGED : COLOR_TEXT, false);

        int right = x + listWidth - SCROLLBAR_WIDTH - 6;
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, right - 90, y + 3, 90, ROW_HEIGHT - 6);
        graphics.fill(right - 90, y + 3, right, y + ROW_HEIGHT - 3,
                hovered ? 0x40FFFFFF : 0x20FFFFFF);
        Component label = Component.translatable(rarity.translationKey()).copy()
                .withStyle(rarity.color());
        graphics.drawString(font, label, right - 86, y + 6, COLOR_TEXT, false);
    }

    private void drawEffectRow(GuiGraphics graphics, String labelKey,
                               Optional<AffixEffects.OnHit> value, int x, int y,
                               int listWidth, int mouseX, int mouseY) {
        if (y + ROW_HEIGHT < listTop() || y > listBottom()) {
            return;
        }
        boolean isChanged = value.isPresent();
        if (isChanged) {
            graphics.fill(x, y, x + 2, y + ROW_HEIGHT, COLOR_CHANGED);
        }
        graphics.drawString(font, Component.translatable(labelKey), x + 6, y + 6,
                isChanged ? COLOR_CHANGED : COLOR_TEXT, false);

        int right = x + listWidth - SCROLLBAR_WIDTH - 6;
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, right - 60, y + 3, 60, ROW_HEIGHT - 6);
        graphics.fill(right - 60, y + 3, right, y + ROW_HEIGHT - 3,
                hovered ? 0x40FFFFFF : 0x20FFFFFF);
        Component label = Component.translatable(KEY_EDIT);
        graphics.drawString(font, label, right - 30 - font.width(label) / 2, y + 6,
                hovered ? COLOR_ACCENT : COLOR_MUTED, false);

        if (value.filter(AffixEffects.OnHit.EMPTY::equals).isPresent()) {
            Component cleared = Component.translatable(KEY_CLEARED);
            graphics.drawString(font, cleared,
                    right - 66 - font.width(cleared), y + 6, COLOR_MUTED, false);
        }
    }

    private void drawStep(GuiGraphics graphics, int x, int y, String label,
                          int mouseX, int mouseY) {
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y + 3, STEP_WIDTH, ROW_HEIGHT - 6);
        graphics.fill(x, y + 3, x + STEP_WIDTH, y + ROW_HEIGHT - 3,
                hovered ? 0x40FFFFFF : 0x20FFFFFF);
        graphics.drawString(font, label, x + STEP_WIDTH / 2 - font.width(label) / 2, y + 6,
                hovered ? COLOR_ACCENT : COLOR_TEXT, false);
    }

    private void step(int index, Field field, int direction) {
        double next = values.get(index) + field.step() * direction;
        values.set(index, Math.clamp(Math.round(next * 1000.0D) / 1000.0D,
                field.min(), field.max()));
    }

    private void beginEdit(int index, int y) {
        commitEdit();
        editingIndex = index;
        int right = left + PANEL_WIDTH - PADDING - SCROLLBAR_WIDTH - 6;
        valueBox.setPosition(right - STEP_WIDTH * 2 - 10 - VALUE_WIDTH, y + 3);
        valueBox.setWidth(VALUE_WIDTH);
        valueBox.setValue(StyleText.formatDecimal(values.get(index)));
        valueBox.setVisible(true);
        valueBox.setFocused(true);
        setFocused(valueBox);
    }

    private void commitEdit() {
        if (editingIndex < 0) {
            return;
        }
        Field field = fields.get(editingIndex);
        try {
            double typed = Double.parseDouble(valueBox.getValue().trim());
            values.set(editingIndex, Math.clamp(typed, field.min(), field.max()));
        } catch (NumberFormatException ignored) {
            CombatStyles.LOGGER.debug("Ignored non-numeric tuning value");
        }
        editingIndex = -1;
        valueBox.setVisible(false);
        setFocused(null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (beginBarDrag(mouseX, mouseY)) {
            return true;
        }
        int trailY = trailRowY();
        if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, left + PADDING + 168, trailY, 58, 12)) {
            trailGlows = !trailGlows;
            return true;
        }
        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        if (mouseY < listTop() || mouseY > listBottom()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        String section = null;
        int y = listTop() - scroll;
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (!field.sectionKey().equals(section)) {
                section = field.sectionKey();
                y += SECTION_HEIGHT;
            }
            int right = x + listWidth - SCROLLBAR_WIDTH - 6;
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, right - STEP_WIDTH * 2 - 4, y + 3,
                    STEP_WIDTH, ROW_HEIGHT - 6)) {
                step(i, field, hasShiftDown() ? -5 : -1);
                return true;
            }
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, right - STEP_WIDTH, y + 3,
                    STEP_WIDTH, ROW_HEIGHT - 6)) {
                step(i, field, hasShiftDown() ? 5 : 1);
                return true;
            }
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY,
                    right - STEP_WIDTH * 2 - 10 - VALUE_WIDTH, y + 3,
                    VALUE_WIDTH, ROW_HEIGHT - 6)) {
                beginEdit(i, y);
                return true;
            }
            y += ROW_HEIGHT;
        }

        y += SECTION_HEIGHT;
        int right = x + listWidth - SCROLLBAR_WIDTH - 6;
        if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, right - 90, y + 3, 90, ROW_HEIGHT - 6)) {
            StyleRarity[] values = StyleRarity.values();
            rarity = values[(rarity.ordinal() + 1) % values.length];
            return true;
        }
        y += ROW_HEIGHT + SECTION_HEIGHT;
        if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, right - 60, y + 3, 60, ROW_HEIGHT - 6)) {
            openEffectTree(KEY_ON_HIT, onHit, result -> onHit = result);
            return true;
        }
        y += ROW_HEIGHT;
        if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, right - 60, y + 3, 60, ROW_HEIGHT - 6)) {
            openEffectTree(KEY_ON_SWING, onSwing, result -> onSwing = result);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean beginBarDrag(double mouseX, double mouseY) {
        int viewport = listBottom() - listTop();
        int barX = left + PADDING + PANEL_WIDTH - PADDING * 2 - SCROLLBAR_WIDTH - 1;
        if (!ScreenDraw.onScrollbar(mouseX, mouseY, barX, listTop(),
                viewport, viewport, contentHeight())) {
            return false;
        }
        draggingList = true;
        scroll = ScreenDraw.scrollFromDrag(mouseY, listTop(), viewport,
                viewport, contentHeight());
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (draggingList) {
            int viewport = listBottom() - listTop();
            scroll = ScreenDraw.scrollFromDrag(mouseY, listTop(), viewport,
                    viewport, contentHeight());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingList = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.clamp(scroll - (int) (scrollY * ROW_HEIGHT), 0, maxScroll());
        return true;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (editingIndex >= 0) {
            return valueBox.charTyped(character, modifiers);
        }
        return super.charTyped(character, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingIndex >= 0) {
            if (keyCode == KEY_ENTER) {
                commitEdit();
                return true;
            }
            if (keyCode == KEY_ESCAPE) {
                editingIndex = -1;
                valueBox.setVisible(false);
                setFocused(null);
                return true;
            }
            return valueBox.keyPressed(keyCode, scanCode, modifiers)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (trailBox != null && trailBox.isFocused() && keyCode != KEY_ENTER && keyCode != KEY_ESCAPE) {
            return trailBox.keyPressed(keyCode, scanCode, modifiers)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == KEY_ENTER) {
            apply();
            return true;
        }
        if (keyCode == KEY_ESCAPE) {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
