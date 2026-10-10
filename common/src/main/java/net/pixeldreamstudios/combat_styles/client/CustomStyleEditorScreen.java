package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.style.WeaponCategories;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.CustomStyle;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class CustomStyleEditorScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 218;
    private static final int PADDING = ScreenTheme.PADDING;
    private static final int TITLE_Y = 10;
    private static final int FIELD_TOP = 24;
    private static final int FIELD_STEP = 18;
    private static final int LABEL_DROP = 3;
    private static final int SECTION_Y = 78;
    private static final int HEADER = 88;
    private static final int FOOTER = 40;
    private static final int ROW_HEIGHT = 18;
    private static final int FIELD_HEIGHT = 14;
    private static final int BUTTON_WIDTH = 74;
    private static final int BUTTON_BOTTOM = 6;
    private static final int GAP = 6;
    private static final int LABEL_WIDTH = 54;
    private static final int NOTICE_DROP = 2;
    private static final int MAX_ID_LENGTH = 64;
    private static final int ROW_BASE = 0;
    private static final int ROW_CATEGORY = 1;
    private static final int ROW_RARITY = 2;
    private static final int ROW_STYLE_PAGE = 3;
    private static final int ROW_COUNT = 4;
    private static final String NAMESPACE_SEPARATOR = ":";

    private static final String KEY_TITLE = "screen.combat_styles.custom_style_editor";
    private static final String KEY_SECTION = "screen.combat_styles.section_identity";
    private static final String KEY_HINT = "screen.combat_styles.custom_style_hint";
    private static final String KEY_ID = "screen.combat_styles.custom_style_id";
    private static final String KEY_GROUP = "screen.combat_styles.custom_style_group";
    private static final String KEY_BASE = "screen.combat_styles.custom_style_base";
    private static final String KEY_CATEGORY = "screen.combat_styles.custom_style_category";
    private static final String KEY_RARITY = "screen.combat_styles.custom_style_rarity";
    private static final String KEY_COST = "screen.combat_styles.custom_style_cost";
    private static final String KEY_STYLE_PAGE = "screen.combat_styles.custom_style_page";
    private static final String KEY_NATURAL = "screen.combat_styles.style_page_natural";
    private static final String KEY_NOT_NATURAL = "screen.combat_styles.style_page_hidden";
    private static final String KEY_DETAILS = "screen.combat_styles.custom_style_details";
    private static final String KEY_INHERIT = "screen.combat_styles.custom_style_inherit";
    private static final String KEY_NO_BASE = "screen.combat_styles.custom_style_no_base";
    private static final String KEY_NEEDS_ID = "screen.combat_styles.affix_needs_id";
    private static final String KEY_BAD_ID = "screen.combat_styles.affix_bad_id";
    private static final String KEY_EXISTS = "screen.combat_styles.custom_style_exists";
    private static final String KEY_NEEDS_BASE = "screen.combat_styles.custom_style_needs_base";
    private static final String KEY_SAVE_FAILED = "screen.combat_styles.affix_save_failed";
    private static final String KEY_SAVE = "screen.combat_styles.save";
    private static final String KEY_CLOSE = "gui.back";
    private static final String HINT_SUFFIX = ".hint";
    private static final String TIP_SUFFIX = ".tip";
    private static final String[] ROW_KEYS = {KEY_BASE, KEY_CATEGORY, KEY_RARITY, KEY_STYLE_PAGE};

    private final CustomStyle existing;
    private final Consumer<CustomStyle> onSaved;

    private EditBox idBox;
    private EditBox groupBox;
    private EditBox costBox;
    private Button detailsButton;
    private boolean natural;

    private String baseStyleId;
    private Optional<String> category;
    private Optional<StyleRarity> rarity;
    private Optional<AttackOverride> attackOverride;
    private List<String> affixIds;
    private Map<String, AffixTuning> affixTuning;
    private Optional<String> name;
    private Optional<String> nameKey;
    private Optional<String> soundId;
    private String error;

    public CustomStyleEditorScreen(Screen parent, CustomStyle existing, Consumer<CustomStyle> onSaved) {
        this(parent, existing, existing == null ? null : existing.entry(), onSaved);
    }

    public CustomStyleEditorScreen(Screen parent, CustomStyle existing, StyleEntry seed,
                                   Consumer<CustomStyle> onSaved) {
        super(Component.translatable(KEY_TITLE), parent);
        this.existing = existing;
        this.onSaved = onSaved;
        StyleEntry start = seed == null ? StyleEntry.of("") : CustomStyles.resolve(seed);
        this.baseStyleId = start.styleId();
        this.category = existing == null ? Optional.empty() : existing.category();
        this.rarity = existing == null ? Optional.empty() : existing.rarity();
        this.natural = existing == null || existing.stylePage().orElse(Boolean.TRUE);
        this.attackOverride = start.attackOverride();
        this.affixIds = new ArrayList<>(start.affixIds());
        this.affixTuning = new LinkedHashMap<>(start.affixTuning());
        this.name = start.name();
        this.nameKey = start.nameKey();
        this.soundId = start.soundId();
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
        int fieldX = left + PADDING + LABEL_WIDTH;
        int fieldWidth = PANEL_WIDTH - PADDING * 2 - LABEL_WIDTH;
        int y = top + FIELD_TOP;

        idBox = field(fieldX, y, fieldWidth, idBox == null
                ? (existing == null ? "" : existing.id()) : idBox.getValue(), KEY_ID);
        y += FIELD_STEP;
        groupBox = field(fieldX, y, fieldWidth, groupBox == null
                ? (existing == null ? CustomStyle.DEFAULT_GROUP : existing.group())
                : groupBox.getValue(), KEY_GROUP);
        y += FIELD_STEP;
        costBox = field(fieldX, y, fieldWidth, costBox == null
                ? (existing == null ? "" : existing.costOverride().map(StyleText::formatDecimal).orElse(""))
                : costBox.getValue(), KEY_COST);

        int buttonY = top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - BUTTON_BOTTOM;
        int detailsWidth = PANEL_WIDTH - PADDING * 2 - BUTTON_WIDTH * 2 - GAP * 2;
        detailsButton = addRenderableWidget(PanelButton.of(
                        Component.translatable(KEY_DETAILS), button -> openDetails())
                .tooltip(Component.translatable(KEY_DETAILS + TIP_SUFFIX))
                .bounds(left + PADDING, buttonY, detailsWidth, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH * 2 - GAP, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_SAVE), button -> save())
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private EditBox field(int x, int y, int width, String value, String labelKey) {
        EditBox box = new FieldBox(font, x, y, width, FIELD_HEIGHT, Component.empty());
        box.setMaxLength(MAX_ID_LENGTH);
        box.setValue(value);
        box.setResponder(text -> error = null);
        box.setHint(Component.translatable(labelKey + HINT_SUFFIX).withStyle(ChatFormatting.DARK_GRAY));
        box.setTooltip(Tooltip.create(Component.translatable(labelKey + TIP_SUFFIX)));
        addRenderableWidget(box);
        return box;
    }

    private static List<String> rarityKeys() {
        return Arrays.stream(StyleRarity.values()).map(StyleRarity::key).toList();
    }

    private static Optional<StyleRarity> rarityOf(String key) {
        for (StyleRarity value : StyleRarity.values()) {
            if (value.key().equalsIgnoreCase(key)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private StylePage basePage() {
        return baseStyleId == null || baseStyleId.isBlank() ? null : StyleRegistry.get(baseStyleId);
    }

    private Component baseLabel() {
        StylePage page = basePage();
        return page == null
                ? Component.translatable(KEY_NO_BASE).withStyle(ChatFormatting.DARK_GRAY)
                : StyleText.name(page);
    }

    private Component categoryLabel() {
        return category.isEmpty()
                ? Component.translatable(KEY_INHERIT).withStyle(ChatFormatting.DARK_GRAY)
                : Component.literal(StyleText.prettify(category.get()));
    }

    private Component stylePageLabel() {
        return Component.translatable(natural ? KEY_NATURAL : KEY_NOT_NATURAL);
    }

    private Component rarityLabel() {
        return rarity.isEmpty()
                ? Component.translatable(KEY_INHERIT).withStyle(ChatFormatting.DARK_GRAY)
                : StyleText.rarityName(rarity.get());
    }

    private Component rowValue(int row) {
        return switch (row) {
            case ROW_BASE -> baseLabel();
            case ROW_CATEGORY -> categoryLabel();
            case ROW_RARITY -> rarityLabel();
            default -> stylePageLabel();
        };
    }

    private void clickRow(int row) {
        switch (row) {
            case ROW_BASE -> minecraft.setScreen(new AttackPickerScreen(this, (page, override) -> {
                baseStyleId = page.styleId();
                minecraft.setScreen(this);
            }));
            case ROW_CATEGORY -> minecraft.setScreen(new ChoicePickerScreen(this,
                    Component.translatable(KEY_CATEGORY), WeaponCategories.allNames(),
                    category.orElse(""), picked -> category = Optional.of(picked))
                    .offeringAny(category.isEmpty(), () -> category = Optional.empty()));
            case ROW_RARITY -> minecraft.setScreen(new ChoicePickerScreen(this,
                    Component.translatable(KEY_RARITY), rarityKeys(),
                    rarity.map(StyleRarity::key).orElse(""),
                    picked -> rarity = rarityOf(picked))
                    .offeringAny(rarity.isEmpty(), () -> rarity = Optional.empty()));
            case ROW_STYLE_PAGE -> natural = !natural;
            default -> {
            }
        }
    }

    @Override
    protected int listTop() {
        return top + HEADER;
    }

    @Override
    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER - BUTTON_BOTTOM;
    }

    @Override
    protected int rowHeight() {
        return ROW_HEIGHT;
    }

    @Override
    protected int rowCount() {
        return ROW_COUNT;
    }

    private void openDetails() {
        StylePage page = basePage();
        if (page == null) {
            error = Component.translatable(KEY_NEEDS_BASE).getString();
            return;
        }
        minecraft.setScreen(new AttackEditorScreen(this, page,
                attackOverride.orElse(AttackOverride.EMPTY),
                applied -> {
                    attackOverride = applied == null || applied.isBlank()
                            ? Optional.empty()
                            : Optional.of(applied);
                    minecraft.setScreen(this);
                },
                name.orElse(""),
                renamed -> name = renamed == null || renamed.isBlank()
                        ? Optional.empty()
                        : Optional.of(renamed))
                .withAffixes(affixIds, picked -> affixIds = new ArrayList<>(picked))
                .withTuning(affixTuning, tuned -> affixTuning = new LinkedHashMap<>(tuned)));
    }

    private String parseId() {
        String raw = idBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (raw.isEmpty()) {
            error = Component.translatable(KEY_NEEDS_ID).getString();
            return null;
        }
        ResourceLocation id = raw.contains(NAMESPACE_SEPARATOR)
                ? ResourceLocation.tryParse(raw)
                : CombatStyles.id(raw);
        if (id == null) {
            error = Component.translatable(KEY_BAD_ID).getString();
            return null;
        }
        return id.toString();
    }

    private Optional<Double> parseCost() {
        String raw = costBox.getValue().trim();
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(raw));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private void save() {
        String id = parseId();
        if (id == null) {
            return;
        }
        if (basePage() == null) {
            error = Component.translatable(KEY_NEEDS_BASE).getString();
            return;
        }
        boolean editingSame = existing != null && existing.id().equals(id);
        if (!editingSame && CustomStyles.contains(id)) {
            error = Component.translatable(KEY_EXISTS).getString();
            return;
        }
        if (!ConfigEdits.permitted()) {
            error = ConfigEdits.operatorOnly().getString();
            return;
        }
        String group = groupBox.getValue().trim();
        StyleEntry entry = new StyleEntry(baseStyleId, soundId, List.copyOf(affixIds),
                attackOverride, name, nameKey, Map.copyOf(affixTuning));
        CustomStyle style = new CustomStyle(id, group.isEmpty() ? CustomStyle.DEFAULT_GROUP : group,
                entry, category, rarity, parseCost(),
                natural ? Optional.empty() : Optional.of(Boolean.FALSE));
        if (!ConfigEdits.save(EditConfigPayload.Kind.CUSTOM_STYLE, id, CustomStyle.CODEC, style)) {
            error = Component.translatable(KEY_SAVE_FAILED).getString();
            return;
        }
        CustomStyles.acceptLocal(style);
        StyleRegistry.rebuild();
        if (onSaved != null) {
            onSaved.accept(style);
        }
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        detailsButton.active = basePage() != null;

        graphics.drawString(font, title, left + PADDING, top + TITLE_Y, ScreenTheme.HEADING, false);
        int labelY = top + FIELD_TOP + LABEL_DROP;
        for (String key : List.of(KEY_ID, KEY_GROUP, KEY_COST)) {
            graphics.drawString(font, Component.translatable(key), left + PADDING, labelY,
                    ScreenTheme.MUTED, false);
            labelY += FIELD_STEP;
        }
        ScreenDraw.sectionHeader(graphics, font, Component.translatable(KEY_SECTION),
                left + PADDING, top + SECTION_Y, PANEL_WIDTH - PADDING * 2);

        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        ScreenDraw.sunken(graphics, x - 2, listTop() - 2, listWidth + 2, listBottom() - listTop() + 4);
        int y = listTop();
        for (int row = 0; row < ROW_COUNT; row++) {
            y = ScreenDraw.valueRow(graphics, font, Component.translatable(ROW_KEYS[row]), rowValue(row),
                    x, y, listWidth, ROW_HEIGHT, mouseX, mouseY);
        }

        Component notice = error != null ? Component.literal(error) : Component.translatable(KEY_HINT);
        graphics.drawString(font, notice, x, top + PANEL_HEIGHT - FOOTER + NOTICE_DROP,
                error == null ? ScreenTheme.DIM : ScreenTheme.BAD, false);
        drawWidgets(graphics, mouseX, mouseY, partialTick);

        int hovered = rowAt(mouseX, mouseY);
        if (hovered >= 0) {
            ScreenDraw.tooltip(graphics, font, Component.translatable(ROW_KEYS[hovered] + TIP_SUFFIX),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int row = rowAt(mouseX, mouseY);
        if (row >= 0) {
            setFocused(null);
            clickRow(row);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
