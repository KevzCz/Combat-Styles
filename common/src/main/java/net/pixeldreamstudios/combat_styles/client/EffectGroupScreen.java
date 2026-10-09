package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.equipment.EffectCooldown;
import net.pixeldreamstudios.combat_styles.equipment.EffectGroup;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentTrigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class EffectGroupScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 410;
    private static final int PANEL_HEIGHT = 326;
    private static final int HEADER = 180;
    private static final int FOOTER = 52;
    private static final int ROW_HEIGHT = 18;
    private static final int BUTTON_WIDTH = 70;
    private static final int GAP = 4;
    private static final int TITLE_Y = 10;
    private static final int NAME_Y = 28;
    private static final int FIELD_HEIGHT = 14;
    private static final int NAME_MAX = 64;
    private static final int CHANCE_Y = 48;
    private static final int PICKS_Y = 68;
    private static final int COUNT_Y = 88;
    private static final int TRIGGER_Y = 108;
    private static final int WINDOW_Y = 128;
    private static final int TOOLTIP_Y = 148;
    private static final int TOOLTIP_MAX = 128;
    private static final int TRIGGER_BUTTON_WIDTH = 96;
    private static final int LABEL_WIDTH = 84;
    private static final int SMALL_FIELD = 34;
    private static final int PERCENT_WIDTH = 12;
    private static final int REMOVE_WIDTH = 14;
    private static final int PERCENT = 100;
    private static final int NUMBER_MAX = 3;
    private static final int TEXT_INSET = 5;
    private static final int HEAD_OFFSET = 11;
    private static final int UNSET = -1;
    private static final int MAX_ONLY_MIN = 1;

    private static final String KEY_TITLE = "screen.combat_styles.group_title";
    private static final String KEY_NAME = "screen.combat_styles.group_name";
    private static final String KEY_CHANCE = "screen.combat_styles.group_chance";
    private static final String KEY_NOTHING = "screen.combat_styles.group_nothing";
    private static final String KEY_NOTHING_HINT = "screen.combat_styles.group_nothing_hint";
    private static final String KEY_FIXED = "screen.combat_styles.group_fixed_count";
    private static final String KEY_RANGE = "screen.combat_styles.group_range_count";
    private static final String KEY_MIN = "screen.combat_styles.group_min";
    private static final String KEY_MAX = "screen.combat_styles.group_max";
    private static final String KEY_ADD = "screen.combat_styles.group_add_entry";
    private static final String KEY_EMPTY = "screen.combat_styles.group_no_entries";
    private static final String KEY_HINT = "screen.combat_styles.group_hint";
    private static final String KEY_COL_EFFECT = "screen.combat_styles.group_col_effect";
    private static final String KEY_COL_CHANCE = "screen.combat_styles.group_col_chance";
    private static final String KEY_SUMMARY = "screen.combat_styles.group_summary";
    private static final String KEY_TRIGGER = "screen.combat_styles.group_trigger";
    private static final String KEY_TOOLTIP = "screen.combat_styles.tooltip_header";
    private static final String KEY_TOOLTIP_HINT = "screen.combat_styles.tooltip_header_hint";
    private static final String KEY_NEEDS_ARMING = "screen.combat_styles.effect_needs_arming";
    private static final String KEY_LIVE_WHILE_USING = "screen.combat_styles.effect_live_while_using";
    private static final String KEY_NEEDS_EDGE_WINDOW = "screen.combat_styles.effect_needs_edge_window";
    private static final String KEY_ARMS_ON_SWING = "screen.combat_styles.effect_arms_on_swing";
    private static final String KEY_LIVE_WHILE_SWING = "screen.combat_styles.effect_live_while_swing";
    private static final String KEY_TRIGGER_EDIT = "screen.combat_styles.group_trigger_edit";
    private static final String KEY_COOLDOWN = "screen.combat_styles.group_cooldown";
    private static final String KEY_COOLDOWN_EDIT = "screen.combat_styles.group_cooldown_edit";
    private static final String KEY_ALWAYS = "screen.combat_styles.effect_always";
    private static final String KEY_NONE = "screen.combat_styles.value_none";
    private static final String KEY_OPEN = "screen.combat_styles.group_window_open";
    private static final String KEY_USES_COUNT = "screen.combat_styles.group_uses_count";
    private static final String TICK_MARK = "t";
    private static final String SUMMARY_JOIN = " · ";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_REMOVE = "screen.combat_styles.slash_remove";
    private static final String PERCENT_MARK = "%";

    private final String originalName;
    private final BiConsumer<String, EffectGroup> onApply;
    private final List<String> available;
    private final List<EffectGroup.Entry> entries = new ArrayList<>();

    private EditBox nameBox;
    private EditBox chanceBox;
    private EditBox nothingBox;
    private EditBox fixedBox;
    private EditBox minBox;
    private EditBox maxBox;
    private EditBox tooltipBox;
    private final String startTooltip;
    private EquipmentTrigger trigger;
    private EffectCooldown cooldown;
    private Component pendingRestNote;
    private final List<EditBox> entryChance = new ArrayList<>();
    private final int startChance;
    private final int startFixed;
    private final int startMin;
    private final int startNothing;
    private final int startMax;

    public EffectGroupScreen(Screen parent, String name, EffectGroup group,
                             List<String> available,
                             BiConsumer<String, EffectGroup> onApply) {
        super(Component.translatable(KEY_TITLE), parent);
        this.originalName = name;
        this.onApply = onApply;
        this.available = List.copyOf(available);
        this.entries.addAll(group.entries());
        this.startChance = Math.round(group.chance() * PERCENT);
        this.startFixed = group.picks().orElse(UNSET);
        this.startMin = group.minPicks().orElse(UNSET);
        this.startNothing = group.nothingChance().map(value -> Math.round(value * PERCENT))
                .orElse(UNSET);
        this.startMax = group.maxPicks().orElse(UNSET);
        this.trigger = group.trigger();
        this.cooldown = group.cooldown();
        this.startTooltip = group.tooltipHeader();
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

        String keptName = nameBox == null ? originalName : nameBox.getValue();
        String keptChance = chanceBox == null
                ? String.valueOf(startChance) : chanceBox.getValue();
        String keptFixed = fixedBox == null ? blankIfUnset(startFixed) : fixedBox.getValue();
        String keptMin = minBox == null ? blankIfUnset(startMin) : minBox.getValue();
        String keptNothing = nothingBox == null
                ? blankIfUnset(startNothing) : nothingBox.getValue();
        String keptMax = maxBox == null ? blankIfUnset(startMax) : maxBox.getValue();
        String keptTooltip = tooltipBox == null ? startTooltip : tooltipBox.getValue();

        int fieldX = left + ScreenTheme.PADDING + LABEL_WIDTH;

        nameBox = new FieldBox(font, fieldX, top + NAME_Y,
                PANEL_WIDTH - ScreenTheme.PADDING * 2 - LABEL_WIDTH, FIELD_HEIGHT,
                Component.empty());
        nameBox.setMaxLength(NAME_MAX);
        nameBox.setValue(keptName);
        addRenderableWidget(nameBox);

        chanceBox = numberBox(fieldX, top + CHANCE_Y, keptChance);
        addRenderableWidget(chanceBox);

        nothingBox = numberBox(fieldX + SMALL_FIELD + LABEL_WIDTH - 22, top + CHANCE_Y, keptNothing);
        nothingBox.setTooltip(Tooltip.create(Component.translatable(KEY_NOTHING_HINT)));
        addRenderableWidget(nothingBox);

        fixedBox = numberBox(fieldX, top + PICKS_Y, keptFixed);
        addRenderableWidget(fixedBox);

        minBox = numberBox(fieldX, top + COUNT_Y, keptMin);
        addRenderableWidget(minBox);

        maxBox = numberBox(fieldX + SMALL_FIELD + LABEL_WIDTH - 22, top + COUNT_Y, keptMax);
        addRenderableWidget(maxBox);

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_TRIGGER_EDIT),
                        button -> openTrigger())
                .bounds(fieldX, top + TRIGGER_Y, TRIGGER_BUTTON_WIDTH, FIELD_HEIGHT).build());

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_COOLDOWN_EDIT),
                        button -> openCooldown())
                .bounds(fieldX, top + WINDOW_Y, TRIGGER_BUTTON_WIDTH, FIELD_HEIGHT).build());

        tooltipBox = new FieldBox(font, fieldX, top + TOOLTIP_Y,
                PANEL_WIDTH - ScreenTheme.PADDING * 2 - LABEL_WIDTH, FIELD_HEIGHT,
                Component.empty());
        tooltipBox.setMaxLength(TOOLTIP_MAX);
        tooltipBox.setValue(keptTooltip);
        tooltipBox.setHint(Component.translatable(KEY_TOOLTIP_HINT));
        addRenderableWidget(tooltipBox);

        int buttonY = top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_ADD),
                        button -> addEntry())
                .bounds(left + ScreenTheme.PADDING, buttonY,
                        BUTTON_WIDTH + 16, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH * 2 - GAP,
                        buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY),
                        button -> apply())
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());

        rebuildEntryBoxes();
    }

    private static String blankIfUnset(int value) {
        return value <= UNSET ? "" : String.valueOf(value);
    }

    private EditBox numberBox(int x, int y, String value) {
        EditBox box = new FieldBox(font, x, y, SMALL_FIELD, FIELD_HEIGHT, Component.empty());
        box.setMaxLength(NUMBER_MAX);
        box.setValue(value);
        box.setFilter(EffectGroupScreen::digitsOnly);
        return box;
    }

    private static boolean digitsOnly(String raw) {
        for (int i = 0; i < raw.length(); i++) {
            if (!Character.isDigit(raw.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void rebuildEntryBoxes() {
        for (EditBox box : entryChance) {
            removeWidget(box);
        }
        entryChance.clear();
        for (EffectGroup.Entry entry : entries) {
            EditBox chance = numberBox(chanceX(), 0,
                    String.valueOf(Math.round(entry.chance() * PERCENT)));
            entryChance.add(chance);
            addRenderableWidget(chance);
        }
        positionEntryBoxes();
    }

    private void positionEntryBoxes() {
        int last = Math.min(entries.size(), scroll + visibleRows());
        for (int index = 0; index < entries.size(); index++) {
            boolean shown = index >= scroll && index < last;
            EditBox chance = entryChance.get(index);
            chance.setVisible(shown);
            chance.active = shown;
            if (!shown) {
                continue;
            }
            int y = rowY(index) + 2;
            chance.setPosition(chanceX(), y);
        }
    }

    private int readNumber(EditBox box, int fallback) {
        String raw = box.getValue().trim();
        if (raw.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Optional<Float> nothingChance() {
        int nothing = readNumber(nothingBox, UNSET);
        return nothing > UNSET
                ? Optional.of(Math.clamp(nothing, 0, PERCENT) / (float) PERCENT)
                : Optional.empty();
    }

    private Component summary() {
        int fixed = readNumber(fixedBox, UNSET);
        if (fixed > 0) {
            return Component.translatable(KEY_SUMMARY, fixed);
        }
        int low = readNumber(minBox, UNSET);
        int high = readNumber(maxBox, UNSET);
        if (low > UNSET || high > UNSET) {
            int from = low > UNSET ? low : MAX_ONLY_MIN;
            int to = high > UNSET ? Math.max(from, high) : Math.max(from, entries.size());
            return Component.translatable(KEY_RANGE, from, to);
        }
        return Component.translatable(KEY_SUMMARY, Math.max(1, entries.size()));
    }

    private void apply() {
        String name = nameBox.getValue().trim();
        int percent = Math.clamp(readNumber(chanceBox, PERCENT), 0, PERCENT);
        List<EffectGroup.Entry> built = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            EffectGroup.Entry entry = entries.get(index);
            int entryPercent = Math.clamp(
                    readNumber(entryChance.get(index), PERCENT), 0, PERCENT);
            built.add(new EffectGroup.Entry(entry.effect(),
                    entryPercent / (float) PERCENT));
        }
        int fixed = readNumber(fixedBox, UNSET);
        int low = readNumber(minBox, UNSET);
        int high = readNumber(maxBox, UNSET);
        Optional<Integer> picks = fixed > 0 ? Optional.of(fixed) : Optional.empty();
        Optional<Integer> minPicks = Optional.empty();
        Optional<Integer> maxPicks = Optional.empty();
        if (fixed <= 0 && (low > UNSET || high > UNSET)) {
            int from = low > UNSET ? low : MAX_ONLY_MIN;
            minPicks = Optional.of(from);
            maxPicks = high > UNSET ? Optional.of(Math.max(from, high)) : Optional.empty();
        }
        EquipmentTrigger synced = trigger.chance().isPresent()
                ? trigger.withChance(Optional.of(percent / (float) PERCENT))
                : trigger;
        onApply.accept(name.isBlank() ? originalName : name,
                new EffectGroup(percent / (float) PERCENT, built,
                        picks, minPicks, maxPicks, synced, cooldown,
                        tooltipBox.getValue().trim(), nothingChance()));
        minecraft.setScreen(parent);
    }

    private void openCooldown() {
        minecraft.setScreen(new EffectTreeScreen(this,
                Component.translatable(KEY_COOLDOWN),
                EffectCooldown.EXPANDED, cooldown,
                applied -> cooldown = applied.orElse(EffectCooldown.NONE))
                .withSchema(EffectSchema.ROOT_COOLDOWN));
    }

    private String cooldownSummary() {
        if (cooldown.isEmpty()) {
            return Component.translatable(KEY_NONE).getString();
        }
        if (!cooldown.windowed()) {
            return cooldown.ticks() + TICK_MARK;
        }
        return cooldown.ticks() + TICK_MARK + SUMMARY_JOIN
                + (cooldown.limited()
                        ? Component.translatable(KEY_USES_COUNT, cooldown.uses()).getString()
                        : Component.translatable(KEY_OPEN).getString())
                + SUMMARY_JOIN + cooldown.span() + TICK_MARK;
    }

    private void openTrigger() {
        minecraft.setScreen(new EffectTreeScreen(this, Component.translatable(KEY_TRIGGER),
                EquipmentTrigger.CODEC, trigger,
                applied -> trigger = applied.orElse(EquipmentTrigger.ALWAYS))
                .withSchema(EffectSchema.ROOT_TRIGGER));
    }

    private void addEntry() {
        List<String> free = new ArrayList<>();
        for (String candidate : available) {
            boolean taken = false;
            for (EffectGroup.Entry entry : entries) {
                if (entry.effect().equals(candidate)) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                free.add(candidate);
            }
        }
        if (free.isEmpty()) {
            return;
        }
        minecraft.setScreen(new ChoicePickerScreen(this,
                Component.translatable(KEY_ADD), free, "", picked -> {
                    entries.add(new EffectGroup.Entry(picked, EffectGroup.ALWAYS));
                    rebuildEntryBoxes();
                }));
    }

    protected int listTop() {
        return top + HEADER;
    }

    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER;
    }

    protected int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_HEIGHT);
    }

    protected int rowY(int index) {
        return listTop() + (index - scroll) * ROW_HEIGHT;
    }

    private int removeX() {
        return left + PANEL_WIDTH - ScreenTheme.PADDING - REMOVE_WIDTH;
    }

    private int chanceX() {
        return removeX() - GAP - SMALL_FIELD - PERCENT_WIDTH;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int last = Math.min(entries.size(), scroll + visibleRows());
        for (int index = scroll; index < last; index++) {
            if (ScreenDraw.isWithin(mouseX, mouseY, removeX(), rowY(index),
                    REMOVE_WIDTH, ROW_HEIGHT)) {
                entries.remove(index);
                rebuildEntryBoxes();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        scroll = Math.clamp(scroll - (int) Math.signum(deltaY), 0,
                Math.max(0, entries.size() - visibleRows()));
        positionEntryBoxes();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KEY_ESCAPE) {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        pendingRestNote = null;

        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        int labelX = left + ScreenTheme.PADDING;
        graphics.drawString(font, Component.translatable(KEY_TITLE),
                labelX, top + TITLE_Y, ScreenTheme.HEADING, false);

        Component note = summary();
        graphics.drawString(font, note,
                left + PANEL_WIDTH - ScreenTheme.PADDING - font.width(note),
                top + TITLE_Y, ScreenTheme.ACCENT, false);

        graphics.drawString(font, Component.translatable(KEY_NAME),
                labelX, top + NAME_Y + 3, ScreenTheme.MUTED, false);
        graphics.drawString(font, Component.translatable(KEY_CHANCE),
                labelX, top + CHANCE_Y + 3, ScreenTheme.MUTED, false);
        graphics.drawString(font, PERCENT_MARK,
                chanceBox.getX() + SMALL_FIELD + 4, top + CHANCE_Y + 3,
                ScreenTheme.MUTED, false);
        graphics.drawString(font, Component.translatable(KEY_NOTHING),
                nothingBox.getX() - font.width(Component.translatable(KEY_NOTHING)) - 6,
                top + CHANCE_Y + 3, ScreenTheme.MUTED, false);
        graphics.drawString(font, PERCENT_MARK,
                nothingBox.getX() + SMALL_FIELD + 4, top + CHANCE_Y + 3,
                ScreenTheme.MUTED, false);
        graphics.drawString(font, Component.translatable(KEY_FIXED),
                labelX, top + PICKS_Y + 3, ScreenTheme.MUTED, false);
        graphics.drawString(font, Component.translatable(KEY_MIN),
                labelX, top + COUNT_Y + 3, ScreenTheme.MUTED, false);
        graphics.drawString(font, Component.translatable(KEY_MAX),
                maxBox.getX() - font.width(Component.translatable(KEY_MAX)) - 6,
                top + COUNT_Y + 3, ScreenTheme.MUTED, false);

        Component triggerLabel = Component.translatable(KEY_TRIGGER);
        graphics.drawString(font, triggerLabel,
                labelX, top + TRIGGER_Y + 3, ScreenTheme.MUTED, false);
        Component restNote = restNote();
        if (restNote != null) {
            int markerX = labelX + font.width(triggerLabel) + GAP;
            graphics.drawString(font, FieldDependency.WINDOW.marker(), markerX,
                    top + TRIGGER_Y + 3, FieldDependency.WINDOW.color(), false);
            if (ScreenDraw.isWithin(mouseX, mouseY, labelX, top + TRIGGER_Y,
                    markerX + font.width(FieldDependency.WINDOW.marker()) - labelX,
                    FIELD_HEIGHT)) {
                pendingRestNote = restNote;
            }
        }
        int summaryX = labelX + LABEL_WIDTH + TRIGGER_BUTTON_WIDTH + GAP;
        String summary = trigger.isAlways()
                ? Component.translatable(KEY_ALWAYS).getString()
                : trigger.describe();
        ScreenDraw.drawTrimmed(graphics, font, summary,
                summaryX, top + TRIGGER_Y + 3, left + PANEL_WIDTH - ScreenTheme.PADDING - summaryX, trigger.isAlways() ? ScreenTheme.DIM : ScreenTheme.ACCENT);
        graphics.drawString(font, Component.translatable(KEY_COOLDOWN),
                labelX, top + WINDOW_Y + 3, ScreenTheme.MUTED, false);
        graphics.drawString(font, Component.translatable(KEY_TOOLTIP),
                labelX, top + TOOLTIP_Y + 3, ScreenTheme.MUTED, false);
        ScreenDraw.drawTrimmed(graphics, font, cooldownSummary(),
                summaryX, top + WINDOW_Y + 3, left + PANEL_WIDTH - ScreenTheme.PADDING - summaryX, cooldown.isEmpty() ? ScreenTheme.DIM : ScreenTheme.ACCENT);

        int listWidth = PANEL_WIDTH - ScreenTheme.PADDING * 2;
        graphics.drawString(font, Component.translatable(KEY_COL_EFFECT),
                labelX + TEXT_INSET, listTop() - HEAD_OFFSET, ScreenTheme.DIM, false);
        graphics.drawString(font, Component.translatable(KEY_COL_CHANCE),
                chanceX(), listTop() - HEAD_OFFSET, ScreenTheme.DIM, false);

        ScreenDraw.sunken(graphics, labelX - 2, listTop() - 2, listWidth + 4,
                listBottom() - listTop() + 4);

        if (entries.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_EMPTY),
                    labelX + TEXT_INSET, listTop() + TEXT_INSET, ScreenTheme.DIM, false);
        }

        int last = Math.min(entries.size(), scroll + visibleRows());
        for (int index = scroll; index < last; index++) {
            EffectGroup.Entry entry = entries.get(index);
            int y = rowY(index);
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, labelX, y,
                    listWidth, ROW_HEIGHT);
            ScreenDraw.rowBackground(graphics, labelX + 1, y, listWidth - 2,
                    ROW_HEIGHT - 1, index, hovered, false);
            ScreenDraw.drawTrimmed(graphics, font, entry.effect(),
                    labelX + TEXT_INSET, y + TEXT_INSET, chanceX() - labelX - 12, ScreenTheme.TEXT);
            graphics.drawString(font, PERCENT_MARK,
                    chanceX() + SMALL_FIELD + 2, y + TEXT_INSET, ScreenTheme.MUTED, false);
            graphics.drawString(font, Component.translatable(KEY_REMOVE),
                    removeX() + 3, y + TEXT_INSET, ScreenTheme.BAD, false);
        }

        graphics.drawString(font, Component.translatable(KEY_HINT),
                labelX, listBottom() + 8, ScreenTheme.MUTED, false);

        for (int index = scroll; index < last; index++) {
            entryChance.get(index).render(graphics, mouseX, mouseY, partialTick);
        }
        drawWidgets(graphics, mouseX, mouseY, partialTick);
        if (pendingRestNote != null) {
            ScreenDraw.tooltip(graphics, font, pendingRestNote, mouseX, mouseY);
        }
    }

    private Component restNote() {
        if (trigger.isAlways()) {
            return null;
        }
        if (cooldown.windowed()) {
            return !trigger.isEvent() && trigger.swingDriven()
                    ? Component.translatable(KEY_ARMS_ON_SWING)
                    : null;
        }
        if (trigger.needsArmingAtRest()) {
            return Component.translatable(KEY_NEEDS_ARMING);
        }
        if (!cooldown.holdWhileActive()) {
            return Component.translatable(KEY_NEEDS_EDGE_WINDOW);
        }
        if (trigger.swingOnlyAtRest()) {
            return Component.translatable(KEY_LIVE_WHILE_SWING);
        }
        return trigger.liveWhileUsing() ? Component.translatable(KEY_LIVE_WHILE_USING) : null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
