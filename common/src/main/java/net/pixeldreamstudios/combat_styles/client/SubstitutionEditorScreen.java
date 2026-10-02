package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.equipment.StyleSubstitution;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import java.util.Optional;
import java.util.function.Consumer;

public class SubstitutionEditorScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 272;
    private static final int HEADER = 98;
    private static final int FOOTER = 30;
    private static final int ROW_HEIGHT = 16;
    private static final int BUTTON_WIDTH = 70;
    private static final int GAP = 4;
    private static final int TITLE_Y = 10;
    private static final int HINT_Y = 22;
    private static final int MODE_Y = 38;
    private static final int FIELD_Y = 58;
    private static final int DECAY_Y = 76;
    private static final int FIELD_HEIGHT = 14;
    private static final int LABEL_WIDTH = 92;
    private static final int TEXT_INSET = 4;
    private static final int REMOVE_WIDTH = 14;
    private static final int FIELD_MAX = 64;

    private static final String KEY_TITLE = "screen.combat_styles.substitution_editor";
    private static final String KEY_HINT = "screen.combat_styles.substitution_hint";
    private static final String KEY_MODE = "screen.combat_styles.substitution_mode";
    private static final String KEY_SLOTS = "screen.combat_styles.substitution_slots";
    private static final String KEY_DECAY = "screen.combat_styles.substitution_decay";
    private static final String KEY_EMPTY = "screen.combat_styles.substitution_empty";
    private static final String KEY_ADD = "screen.combat_styles.substitution_add";
    private static final String KEY_MORE = "screen.combat_styles.substitution_more";
    private static final String KEY_REMOVE = "screen.combat_styles.slash_remove";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CLOSE = "gui.back";
    private static final String MODE_PREFIX = "screen.combat_styles.substitution_mode_";
    private static final String LIST_SEPARATOR = ",";

    private static final String APPLY_PREFIX = "screen.combat_styles.substitution_apply_";
    private static final int APPLY_WIDTH = 74;
    private static final int APPLY_GAP = 4;

    private static final List<String> MODES = List.of(
            StyleSubstitution.MODE_REPLACE_ALL,
            StyleSubstitution.MODE_REPLACE_SLOT,
            StyleSubstitution.MODE_APPEND);

    private static final List<String> APPLY_MODES = List.of(
            StyleSubstitution.APPLY_ON_SLOT,
            StyleSubstitution.APPLY_AFTER_SLOT);

    private final Consumer<Optional<StyleSubstitution>> onApply;
    private final List<String> styles = new ArrayList<>();

    private String mode;
    private int lingerTicks;
    private int chargeDecayTicks;
    private boolean restartCombo;
    private String applyMode = StyleSubstitution.APPLY_ON_SLOT;
    private Map<String, AttackOverride> tuning;
    private List<Integer> slots;
    private List<String> weaponCategories;
    private List<String> itemTags;
    private List<Integer> borrowSlots;

    private EditBox slotsBox;
    private EditBox decayBox;
    private List<Component> tooltip;

    public SubstitutionEditorScreen(Screen parent, StyleSubstitution current,
                                    Consumer<Optional<StyleSubstitution>> onApply) {
        super(Component.translatable(KEY_TITLE), parent);
        this.onApply = onApply;
        StyleSubstitution source = current == null
                ? new StyleSubstitution(List.of(), List.of(), StyleSubstitution.MODE_REPLACE_ALL,
                        List.of(), List.of(), List.of(),
                        StyleSubstitution.DEFAULT_LINGER_TICKS,
                        StyleSubstitution.NO_CHARGE_DECAY, Map.of())
                : current;
        this.styles.addAll(source.styles());
        this.mode = source.mode();
        this.slots = new ArrayList<>(source.slots());
        this.lingerTicks = source.lingerTicks();
        this.chargeDecayTicks = source.chargeDecayTicks();
        this.restartCombo = source.restartCombo();
        this.applyMode = source.apply();
        this.tuning = new LinkedHashMap<>(source.tuning());
        this.weaponCategories = new ArrayList<>(source.weaponCategories());
        this.itemTags = new ArrayList<>(source.itemTags());
        this.borrowSlots = new ArrayList<>(source.borrowSlots());
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
    protected int rowCount() {
        return styles.size();
    }

    @Override
    protected void layout() {
        slotsBox = new FieldBox(font, left + ScreenTheme.PADDING + LABEL_WIDTH, top + FIELD_Y,
                PANEL_WIDTH - ScreenTheme.PADDING * 2 - LABEL_WIDTH, FIELD_HEIGHT,
                Component.empty());
        slotsBox.setMaxLength(FIELD_MAX);
        slotsBox.setValue(joinInts(slots));
        addRenderableWidget(slotsBox);

        decayBox = new FieldBox(font, left + ScreenTheme.PADDING + LABEL_WIDTH,
                top + DECAY_Y, PANEL_WIDTH - ScreenTheme.PADDING * 2 - LABEL_WIDTH,
                FIELD_HEIGHT, Component.empty());
        decayBox.setMaxLength(FIELD_MAX);
        decayBox.setValue(String.valueOf(chargeDecayTicks));
        addRenderableWidget(decayBox);

        int buttonY = top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_ADD), button -> addStyle())
                .bounds(left + ScreenTheme.PADDING, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_MORE), button -> openTree())
                .bounds(left + ScreenTheme.PADDING + BUTTON_WIDTH + GAP, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH * 2 - GAP,
                        buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY),
                        button -> apply())
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private void openTuning(String styleId) {
        StyleEntry entry = SequenceService.entryFor(styleId);
        StylePage page = entry == null ? null : StyleRegistry.get(entry.styleId());
        if (page == null) {
            return;
        }
        AttackOverride current = tuning.getOrDefault(styleId, AttackOverride.EMPTY);
        minecraft.setScreen(new AttackEditorScreen(this, page, current, applied -> {
            if (applied == null || AttackOverride.EMPTY.equals(applied)) {
                tuning.remove(styleId);
            } else {
                tuning.put(styleId, applied);
            }
            minecraft.setScreen(this);
        }).withSubtitle(styleName(styleId)));
    }

    private static Component styleName(String styleId) {
        StyleEntry entry = SequenceService.entryFor(styleId);
        StylePage page = entry == null ? null : StyleRegistry.get(entry.styleId());
        return page == null
                ? Component.literal(StyleText.prettify(styleId))
                : StyleText.name(entry, page);
    }

    private void addStyle() {
        minecraft.setScreen(new AttackPickerScreen(this, (page, override) -> {
            styles.add(page.styleId());
            minecraft.setScreen(this);
        }));
    }

    private void openTree() {
        minecraft.setScreen(new EffectTreeScreen(this, Component.translatable(KEY_TITLE),
                StyleSubstitution.CODEC, build(), applied -> applied.ifPresent(this::adopt))
                .withSchema(EffectSchema.ROOT_SUBSTITUTION));
    }

    private void adopt(StyleSubstitution edited) {
        styles.clear();
        styles.addAll(edited.styles());
        mode = edited.mode();
        slots = new ArrayList<>(edited.slots());
        lingerTicks = edited.lingerTicks();
        chargeDecayTicks = edited.chargeDecayTicks();
        restartCombo = edited.restartCombo();
        applyMode = edited.apply();
        tuning = new LinkedHashMap<>(edited.tuning());
        if (decayBox != null) {
            decayBox.setValue(String.valueOf(chargeDecayTicks));
        }
        weaponCategories = new ArrayList<>(edited.weaponCategories());
        itemTags = new ArrayList<>(edited.itemTags());
        borrowSlots = new ArrayList<>(edited.borrowSlots());
        if (slotsBox != null) {
            slotsBox.setValue(joinInts(slots));
        }
    }

    private StyleSubstitution build() {
        return new StyleSubstitution(List.copyOf(styles), parseInts(slotsBox == null
                        ? joinInts(slots) : slotsBox.getValue()),
                mode, List.copyOf(weaponCategories), List.copyOf(itemTags),
                List.copyOf(borrowSlots), lingerTicks, parsedDecay(),
                Map.copyOf(tuning), restartCombo, applyMode);
    }

    private int parsedDecay() {
        if (decayBox == null) {
            return chargeDecayTicks;
        }
        try {
            return Integer.parseInt(decayBox.getValue().trim());
        } catch (NumberFormatException ignored) {
            return chargeDecayTicks;
        }
    }

    private void apply() {
        onApply.accept(styles.isEmpty() ? Optional.empty() : Optional.of(build()));
        minecraft.setScreen(parent);
    }

    private static String joinInts(List<Integer> values) {
        StringBuilder builder = new StringBuilder();
        for (int value : values) {
            if (!builder.isEmpty()) {
                builder.append(LIST_SEPARATOR);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static List<Integer> parseInts(String raw) {
        List<Integer> values = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return values;
        }
        for (String part : raw.split(LIST_SEPARATOR)) {
            try {
                values.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                return values;
            }
        }
        return values;
    }

    @Override
    protected int listTop() {
        return top + HEADER;
    }

    @Override
    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER;
    }

    private int removeX() {
        return contentRight() - REMOVE_WIDTH;
    }

    private int modeWidth() {
        int full = PANEL_WIDTH - ScreenTheme.PADDING * 2 - LABEL_WIDTH;
        return showsApply() ? full - applyWidth() - APPLY_GAP : full;
    }

    private boolean showsApply() {
        return StyleSubstitution.MODE_REPLACE_ALL.equals(mode);
    }

    private int applyWidth() {
        return APPLY_WIDTH;
    }

    private int applyX() {
        return left + ScreenTheme.PADDING + LABEL_WIDTH + modeWidth() + APPLY_GAP;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ScreenDraw.isWithin(mouseX, mouseY, left + ScreenTheme.PADDING + LABEL_WIDTH,
                top + MODE_Y, modeWidth(), FIELD_HEIGHT)) {
            mode = MODES.get((MODES.indexOf(mode) + 1 + MODES.size()) % MODES.size());
            return true;
        }
        if (showsApply() && ScreenDraw.isWithin(mouseX, mouseY, applyX(),
                top + MODE_Y, applyWidth(), FIELD_HEIGHT)) {
            applyMode = APPLY_MODES.get(
                    (APPLY_MODES.indexOf(applyMode) + 1) % APPLY_MODES.size());
            return true;
        }
        if (beginScrollDrag(mouseX, mouseY)) {
            return true;
        }
        int first = scroll;
        int last = Math.min(styles.size(), first + visibleRows());
        for (int index = first; index < last; index++) {
            int y = rowY(index);
            if (!ScreenDraw.isWithin(mouseX, mouseY, left + ScreenTheme.PADDING, y,
                    PANEL_WIDTH - ScreenTheme.PADDING * 2, ROW_HEIGHT)) {
                continue;
            }
            if (ScreenDraw.isWithin(mouseX, mouseY, removeX(), y, REMOVE_WIDTH, ROW_HEIGHT)) {
                String removed = styles.remove(index);
                tuning.remove(removed);
                scroll = Math.clamp(scroll, 0, Math.max(0, styles.size() - visibleRows()));
                return true;
            }
            openTuning(styles.get(index));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        tooltip = null;
        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(font, Component.translatable(KEY_TITLE),
                left + ScreenTheme.PADDING, top + TITLE_Y, ScreenTheme.HEADING, false);
        graphics.drawString(font, Component.translatable(KEY_HINT),
                left + ScreenTheme.PADDING, top + HINT_Y, ScreenTheme.MUTED, false);

        graphics.drawString(font, Component.translatable(KEY_MODE),
                left + ScreenTheme.PADDING, top + MODE_Y + 3, ScreenTheme.TEXT, false);
        boolean modeHover = ScreenDraw.isWithin(mouseX, mouseY,
                left + ScreenTheme.PADDING + LABEL_WIDTH, top + MODE_Y,
                modeWidth(), FIELD_HEIGHT);
        ScreenDraw.sunken(graphics, left + ScreenTheme.PADDING + LABEL_WIDTH, top + MODE_Y,
                modeWidth(), FIELD_HEIGHT);
        graphics.drawString(font,
                Component.translatable(MODE_PREFIX + mode.toLowerCase(Locale.ROOT)),
                left + ScreenTheme.PADDING + LABEL_WIDTH + TEXT_INSET, top + MODE_Y + 3,
                modeHover ? ScreenTheme.ACCENT : ScreenTheme.TEXT, false);

        if (showsApply()) {
            boolean applyHover = ScreenDraw.isWithin(mouseX, mouseY, applyX(),
                    top + MODE_Y, applyWidth(), FIELD_HEIGHT);
            ScreenDraw.sunken(graphics, applyX(), top + MODE_Y, applyWidth(), FIELD_HEIGHT);
            graphics.drawString(font,
                    Component.translatable(APPLY_PREFIX + applyMode),
                    applyX() + TEXT_INSET, top + MODE_Y + 3,
                    applyHover ? ScreenTheme.ACCENT : ScreenTheme.TEXT, false);
        }

        graphics.drawString(font, Component.translatable(KEY_SLOTS),
                left + ScreenTheme.PADDING, top + FIELD_Y + 3, ScreenTheme.TEXT, false);
        graphics.drawString(font, Component.translatable(KEY_DECAY),
                left + ScreenTheme.PADDING, top + DECAY_Y + 3, ScreenTheme.TEXT, false);

        ScreenDraw.sunken(graphics, left + ScreenTheme.PADDING, listTop() - 2,
                PANEL_WIDTH - ScreenTheme.PADDING * 2, visibleRows() * ROW_HEIGHT + 4);

        if (styles.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_EMPTY),
                    left + ScreenTheme.PADDING + TEXT_INSET, listTop() + TEXT_INSET,
                    ScreenTheme.DIM, false);
        } else {
            int first = scroll;
            int last = Math.min(styles.size(), first + visibleRows());
            for (int index = first; index < last; index++) {
                String style = styles.get(index);
                int y = rowY(index);
                boolean hovered = ScreenDraw.isWithin(mouseX, mouseY,
                        left + ScreenTheme.PADDING, y,
                        PANEL_WIDTH - ScreenTheme.PADDING * 2, ROW_HEIGHT);
                ScreenDraw.rowBackground(graphics, left + ScreenTheme.PADDING + 1, y,
                        PANEL_WIDTH - ScreenTheme.PADDING * 2 - 2, ROW_HEIGHT - 1,
                        index, hovered, false);
                graphics.drawString(font, Component.literal(index + 1 + "."),
                        left + ScreenTheme.PADDING + TEXT_INSET, y + TEXT_INSET,
                        ScreenTheme.MUTED, false);
                graphics.drawString(font, ScreenDraw.trim(font, styleName(style).getString(),
                                PANEL_WIDTH - 80),
                        left + ScreenTheme.PADDING + TEXT_INSET + 18, y + TEXT_INSET,
                        ScreenTheme.TEXT, false);
                graphics.drawString(font, Component.translatable(KEY_REMOVE),
                        removeX() + 3, y + TEXT_INSET, ScreenTheme.BAD, false);
                if (hovered) {
                    tooltip = RefTooltip.forStyle(style);
                }
            }
        }

        ScreenDraw.scrollbar(graphics, left + PANEL_WIDTH - ScreenTheme.PADDING - 2,
                listTop(), visibleRows() * ROW_HEIGHT, visibleRows(), styles.size(), scroll);

        drawWidgets(graphics, mouseX, mouseY, partialTick);

        if (tooltip != null && !tooltip.isEmpty()) {
            ScreenDraw.tooltip(graphics, font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
