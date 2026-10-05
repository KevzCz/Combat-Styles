package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.api.AttackShape;
import net.pixeldreamstudios.combat_styles.client.tooltip.ArcTooltipComponent;
import net.pixeldreamstudios.combat_styles.client.tooltip.TooltipPanel;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.network.ApplySequencePayload;
import net.pixeldreamstudios.combat_styles.network.ApplyWeaponCategoryPayload;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.network.ExtractStylePayload;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.CategoryAffinity;
import net.pixeldreamstudios.combat_styles.style.CustomAttack;
import net.pixeldreamstudios.combat_styles.style.CustomAttacks;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.DefaultStyleConfig;
import net.pixeldreamstudios.combat_styles.style.DefaultStyles;
import net.pixeldreamstudios.combat_styles.style.SequenceLimits;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleCost;
import net.pixeldreamstudios.combat_styles.style.InnateAffixes;
import net.pixeldreamstudios.combat_styles.style.SequenceComposer;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;
import net.pixeldreamstudios.combat_styles.style.StyleText;
import net.pixeldreamstudios.combat_styles.style.StyleTooltip;
import net.pixeldreamstudios.combat_styles.style.WeaponCategories;

import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CustomizerScreen extends PanelScreen {
    private static final int PANEL_WIDTH = 500;
    private static final int PANEL_HEIGHT = 288;
    private static final int CATEGORY_WIDTH = 92;
    private static final int RAIL_WIDTH = 10;
    private static final float REVEAL_SPEED = 0.22F;
    private static final int LIBRARY_WIDTH = 168;
    private static final int ROW_HEIGHT = 15;
    private static final int SOUND_HEIGHT = 11;
    private static final int SLOT_HEIGHT = ROW_HEIGHT + SOUND_HEIGHT;
    private static final int PADDING = 8;
    private static final int GAP = 6;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int BUTTON_HIT_WIDTH = 12;
    private static final int PREVIEW_SIZE = 84;
    private static final int SOUND_TEXT_LEFT = 15;
    private static final int SOUND_ARROW_WIDTH = 12;
    private static final int BUDGET_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 16;
    private static final int HEADER_HEIGHT = 46;
    private static final int FOOTER_HEIGHT = 26;

    private static final int COLOR_SHADE = ScreenTheme.SHADE;
    private static final int COLOR_PANEL = ScreenTheme.PANEL;
    private static final int COLOR_SUNK = ScreenTheme.SUNK;
    private static final int COLOR_BORDER = ScreenTheme.BORDER_SOFT;
    private static final int COLOR_BORDER_LIT = ScreenTheme.BORDER;
    private static final int COLOR_ROW_ALT = ScreenTheme.ROW_ALT;
    private static final int COLOR_HOVER = ScreenTheme.HOVER;
    private static final int COLOR_SELECTED = ScreenTheme.SELECTED;
    private static final int COLOR_DRAG = ScreenTheme.DRAG;
    private static final int COLOR_ACCENT = ScreenTheme.ACCENT;
    private static final int COLOR_HEADING = ScreenTheme.HEADING;
    private static final int COLOR_TEXT = ScreenTheme.TEXT;
    private static final int COLOR_MUTED = ScreenTheme.MUTED;
    private static final int COLOR_LOCKED = ScreenTheme.LOCKED;
    private static final int COLOR_GOOD = ScreenTheme.GOOD;
    private static final int COLOR_BAD = ScreenTheme.BAD;
    private static final int COLOR_SCROLL = ScreenTheme.SCROLL;

    private static final String KEY_TITLE = "screen.combat_styles.customizer";
    private static final String KEY_SEQUENCE = "screen.combat_styles.sequence";
    private static final String KEY_EMPTY = "screen.combat_styles.empty_sequence";
    private static final String KEY_UNSUPPORTED = "screen.combat_styles.unsupported_weapon";
    private static final String KEY_IMPROVISE = "screen.combat_styles.improvise";
    private static final String KEY_IMPROVISE_HINT = "screen.combat_styles.improvise_hint";
    private static final int IMPROVISE_WIDTH = 120;
    private static final int IMPROVISE_HEIGHT = 16;
    private boolean improvised;
    private static final String KEY_NO_STYLES = "screen.combat_styles.no_styles";
    private static final String KEY_NO_MATCH = "screen.combat_styles.no_match";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CLEAR = "screen.combat_styles.clear";
    private static final String KEY_CLEAR_MENU = "screen.combat_styles.clear_menu";
    private static final String KEY_EXPORT_DEFAULT = "screen.combat_styles.export_default";
    private static final String KEY_REMOVE_DEFAULT = "screen.combat_styles.remove_default";
    private static final String KEY_SAVE_CUSTOM_STYLE = "screen.combat_styles.save_custom_style";
    private static final String KEY_CUSTOM_STYLES = "screen.combat_styles.open_custom_styles";
    private static final String KEY_DEFAULT_EXPORTED = "screen.combat_styles.default_exported";
    private static final String KEY_DEFAULT_REMOVED = "screen.combat_styles.default_removed";
    private static final String KEY_PICK_SLOT = "screen.combat_styles.pick_slot";
    private static final String SLOT_LABEL_SEPARATOR = " · ";
    private static final String KEY_AUTHORING_MENU = "screen.combat_styles.authoring_menu";
    private static final String KEY_CUSTOM_ATTACKS = "screen.combat_styles.open_custom_attacks";
    private static final String KEY_POSE = "screen.combat_styles.equipment_pose";
    private static final String KEY_CATEGORY_BUTTON = "screen.combat_styles.equipment_weapon_category";
    private static final String KEY_CATEGORY_PICKER = "screen.combat_styles.weapon_category_picker";
    private static final String KEY_BUILDER_FAILED = "screen.combat_styles.builder_failed";
    private static final String KEY_MAINHAND = "screen.combat_styles.main_hand";
    private static final String KEY_OFFHAND = "screen.combat_styles.off_hand";
    private static final String KEY_SEARCH = "screen.combat_styles.search";
    private static final String KEY_SORT = "screen.combat_styles.sort";
    private static final String KEY_BUDGET = "screen.combat_styles.budget";
    private static final String KEY_BUDGET_OVERRIDDEN = "screen.combat_styles.budget_overridden";
    private static final String KEY_OVER_BUDGET = "screen.combat_styles.over_budget";
    private static final String KEY_LOCKED = "screen.combat_styles.locked";
    private static final String KEY_LEARN_HINT = "screen.combat_styles.learn_hint";
    private static final String KEY_PROGRESS = "screen.combat_styles.progress";
    private static final String KEY_PREVIEW_UNAVAILABLE = "screen.combat_styles.preview_unavailable";
    private static final String KEY_EXPAND = "screen.combat_styles.expand";
    private static final String KEY_CREATIVE_ONLY = "screen.combat_styles.creative_only";
    private static final String KEY_DATAPACK_LOCKED = "screen.combat_styles.datapack_locked";
    private static final String KEY_DEFAULT_SAVED = "screen.combat_styles.default_saved";
    private static final String KEY_EDIT_ON_GROUP = "screen.combat_styles.edit_on_group";
    private static final String KEY_ALL_CATEGORIES = "screen.combat_styles.all_categories";
    private static final String KEY_CATEGORIES = "screen.combat_styles.categories";
    private static final String KEY_LIBRARY = "screen.combat_styles.library";
    private static final String KEY_TOO_EXPENSIVE = "screen.combat_styles.too_expensive";
    private static final String KEY_SLOT_EMPTY = "screen.combat_styles.slot_empty";
    private static final String KEY_SOUND_NONE = "screen.combat_styles.sound_none";
    private static final String KEY_THIS_WEAPON = "screen.combat_styles.this_weapon";
    private static final String KEY_NATIVE = "screen.combat_styles.native_node";
    private static final String KEY_NO_COPIES = "screen.combat_styles.no_copies";
    private static final String KEY_COPIES = "screen.combat_styles.copies";
    private static final String KEY_REPEAT_COST = "screen.combat_styles.repeat_cost";
    private static final String KEY_AFFINITY_RELATED = "screen.combat_styles.affinity_related";
    private static final String KEY_AFFINITY_ALIEN = "screen.combat_styles.affinity_alien";
    private static final String KEY_ETCHED = "screen.combat_styles.etched";
    private static final String KEY_SUBSTITUTED = "screen.combat_styles.substituted";
    private static final String KEY_DEFAULT_SEQUENCE = "screen.combat_styles.default_sequence";
    private static final String KEY_ITEM_LOCKED = "screen.combat_styles.item_locked";
    private static final String KEY_BELOW_MINIMUM = "screen.combat_styles.below_minimum";
    private static final String KEY_NAMED_LOCKED = "screen.combat_styles.named_locked";
    private static final String KEY_COPY_SENT = "screen.combat_styles.copy_sent";
    private static final String KEY_SHORTCUTS = "screen.combat_styles.shortcuts";
    private static final int MIN_GROUP_SIZE = 2;
    private static final int GROUP_INDENT = 8;
    private static final float HINT_DEPTH = 400.0F;
    private static final String KEY_ALREADY_ETCHED = "message.combat_styles.already_etched";

    private static final String MARKER_CONDITIONAL = "*";

    private static final int KEY_TAB = 258;
    private static final int KEY_BACKSPACE = 259;
    private static final int KEY_DELETE = 261;
    private static final int KEY_F = 70;
    private static final int KEY_R = 82;
    private static final int KEY_S = 83;

    private final CustomizerState state;
    private final AnimationPreview preview = new AnimationPreview();
    private ItemStack trackedWeapon = ItemStack.EMPTY;
    private final List<LibraryRow> libraryRows = new ArrayList<>();
    private final Scrollbar categoryScrollbar = new Scrollbar(COLOR_SUNK, COLOR_SCROLL);
    private final Scrollbar libraryScrollbar = new Scrollbar(COLOR_SUNK, COLOR_SCROLL);
    private final Scrollbar sequenceScrollbar = new Scrollbar(COLOR_SUNK, COLOR_SCROLL);
    private final Set<String> expanded = new HashSet<>();
    private final Set<String> openGroups = new HashSet<>();
    private final List<Button> panelButtons = new ArrayList<>();
    private DropdownMenu clearMenu;
    private DropdownMenu authoringMenu;

    private static boolean categoriesOpen;

    private float categoryReveal;

    private Button sortButton;

    private int libraryScroll;
    private int categoryScroll;
    private int sequenceScroll;
    private StylePage selected;
    private StyleEntry selectedVariant;
    private String selectedAffix;
    private EditBox searchBox;
    private Component toast = Component.empty();
    private int toastTicks;

    private int dragFrom = -1;
    private int dragHoverIndex = -1;
    private boolean dragging;

    public CustomizerScreen(CustomizerState state) {
        super(Component.translatable(KEY_TITLE), null);
        this.state = state;
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
        panelButtons.clear();
        categoryReveal = categoriesOpen ? 1.0F : 0.0F;

        int searchX = libraryX();
        int searchWidth = LIBRARY_WIDTH - 58;
        searchBox = new FieldBox(font, searchX, top + 28, searchWidth, 14, Component.translatable(KEY_SEARCH));
        searchBox.setHint(Component.translatable(KEY_SEARCH).withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setMaxLength(48);
        searchBox.setValue(state.search());
        searchBox.setResponder(value -> {
            state.setSearch(value);
            libraryScroll = 0;
            rebuildLibrary();
        });
        addRenderableWidget(searchBox);

        sortButton = addRenderableWidget(PanelButton.of(sortLabel(), button -> {
            state.cycleSort();
            button.setMessage(sortLabel());
            rebuildLibrary();
        }).bounds(searchX + searchWidth + 4, top + 28, 54, 14).build());
        panelButtons.add(sortButton);

        int buttonY = top + PANEL_HEIGHT - PADDING - BUTTON_HEIGHT;
        int right = left + PANEL_WIDTH - PADDING;

        panelButtons.add(addRenderableWidget(PanelButton.of(slotLabel(), button -> {
            state.toggleSlot();
            button.setMessage(slotLabel());
            selected = null;
            selectedVariant = null;
            preview.select(null);
            libraryScroll = 0;
            categoryScroll = 0;
            rebuildLibrary();
        }).bounds(left + PADDING, buttonY, 64, BUTTON_HEIGHT).build()));

        if (state.creative()) {
            Button authoringButton = addRenderableWidget(PanelButton.of(
                    Component.translatable(KEY_AUTHORING_MENU), button -> openAttackBuilder())
                    .bounds(left + PADDING + 68, buttonY, 74, BUTTON_HEIGHT).build());
            panelButtons.add(authoringButton);
            authoringMenu = new DropdownMenu(authoringButton, List.of(
                    DropdownMenu.Option.always(Component.translatable(KEY_CUSTOM_ATTACKS),
                            this::openAttackBuilder),
                    DropdownMenu.Option.always(Component.translatable(KEY_CUSTOM_STYLES),
                            this::openCustomStyles)));
            panelButtons.add(addRenderableWidget(PanelButton.of(
                    Component.translatable(KEY_POSE),
                    button -> minecraft.setScreen(
                            PoseEditorScreen.forHeldItem(this, state.slot())))
                    .bounds(left + PADDING + 146, buttonY, 48, BUTTON_HEIGHT).build()));
            panelButtons.add(addRenderableWidget(PanelButton.of(
                    Component.translatable(KEY_CATEGORY_BUTTON),
                    button -> openWeaponCategory())
                    .bounds(left + PADDING + 198, buttonY, 72, BUTTON_HEIGHT).build()));
        }

        Button clearButton = addRenderableWidget(PanelButton.of(
                Component.translatable(KEY_CLEAR_MENU), button -> clearSequence())
                .bounds(right - 112, buttonY, 52, BUTTON_HEIGHT).build());
        panelButtons.add(clearButton);
        clearMenu = new DropdownMenu(clearButton, List.of(
                DropdownMenu.Option.always(Component.translatable(KEY_CLEAR), this::clearSequence),
                new DropdownMenu.Option(Component.translatable(KEY_EXPORT_DEFAULT),
                        state::creative, this::exportDefault),
                new DropdownMenu.Option(Component.translatable(KEY_REMOVE_DEFAULT),
                        () -> state.creative() && DefaultStyles.hasConfigDefault(state.weapon()),
                        this::removeDefault),
                new DropdownMenu.Option(Component.translatable(KEY_SAVE_CUSTOM_STYLE),
                        () -> state.creative() && !state.sequence().isEmpty(),
                        this::saveSlotAsCustomStyle)));

        panelButtons.add(addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY), button -> apply())
                .bounds(right - 56, buttonY, 56, BUTTON_HEIGHT).build()));

        rebuildLibrary();
    }

    private Component slotLabel() {
        return Component.translatable(state.slot() == EquipmentSlot.MAINHAND ? KEY_MAINHAND : KEY_OFFHAND);
    }

    private Component sortLabel() {
        return Component.translatable(KEY_SORT, Component.translatable(state.sort().translationKey()));
    }

    private record LibraryRow(StylePage page, StyleAffix affix, boolean expandable,
                              StyleEntry variant, String groupAnimation, int groupSize) {
        LibraryRow(StylePage page, StyleAffix affix, boolean expandable) {
            this(page, affix, expandable, null, null, 0);
        }

        LibraryRow(StylePage page, StyleAffix affix, boolean expandable, StyleEntry variant) {
            this(page, affix, expandable, variant, null, 0);
        }

        static LibraryRow group(String animation, StylePage sample, int size) {
            return new LibraryRow(sample, null, false, null, animation, size);
        }

        boolean isGroup() {
            return groupAnimation != null;
        }

        String affixId() {
            return affix == null ? null : affix.serializedId();
        }

        boolean isVariant() {
            return variant != null;
        }
    }

    private void rebuildLibrary() {
        libraryRows.clear();
        Set<String> variantStyleIds = new HashSet<>();
        for (StyleEntry variant : state.visibleVariants()) {
            StyleEntry resolved = CustomStyles.resolve(variant);
            StylePage page = StyleRegistry.get(resolved.styleId());
            if (page != null) {
                libraryRows.add(new LibraryRow(page, null, false, resolved));
                variantStyleIds.add(page.styleId());
            }
        }
        for (StylePage page : state.visiblePages()) {
            if (variantStyleIds.contains(page.styleId())
                    && !state.ownsPlainExplicitly(page.styleId())) {
                continue;
            }
            libraryRows.add(new LibraryRow(page, null, false));
        }
        libraryRows.sort(Comparator
                .comparing((LibraryRow row) -> !rowUsable(row))
                .thenComparing(this::sortKeyOf)
                .thenComparing(row -> rowLabel(row).toLowerCase(Locale.ROOT))
                .thenComparing(row -> row.page().styleId()));
        applyAnimationGrouping();
        libraryScroll = Math.clamp(libraryScroll, 0, maxLibraryScroll());
    }

    private void applyAnimationGrouping() {
        Map<String, List<LibraryRow>> byAnimation = new LinkedHashMap<>();
        List<LibraryRow> ungrouped = new ArrayList<>();
        for (LibraryRow row : libraryRows) {
            String animation = row.page().animation();
            if (animation == null || animation.isBlank() || !rowUsable(row)) {
                ungrouped.add(row);
                continue;
            }
            byAnimation.computeIfAbsent(animation, key -> new ArrayList<>()).add(row);
        }

        List<LibraryRow> grouped = new ArrayList<>(libraryRows.size());
        for (Map.Entry<String, List<LibraryRow>> entry : byAnimation.entrySet()) {
            List<LibraryRow> rows = entry.getValue();
            if (rows.size() < MIN_GROUP_SIZE) {
                grouped.addAll(rows);
                continue;
            }
            grouped.add(LibraryRow.group(entry.getKey(), groupOwner(rows), rows.size()));
            if (!openGroups.contains(entry.getKey())) {
                continue;
            }
            grouped.addAll(rows);
        }
        grouped.addAll(ungrouped);
        libraryRows.clear();
        libraryRows.addAll(grouped);
    }

    private StylePage groupOwner(List<LibraryRow> rows) {
        String animation = rows.getFirst().page().animation();
        StylePage nativePage = nativeStyleFor(animation);
        if (nativePage != null) {
            return nativePage;
        }
        for (LibraryRow row : rows) {
            if (AttackOverrides.isEditable(row.page().styleId())) {
                return row.page();
            }
        }
        return rows.getFirst().page();
    }

    private StylePage nativeStyleFor(String animation) {
        if (animation == null || animation.isBlank()) {
            return null;
        }
        for (StylePage page : StyleRegistry.all()) {
            if (animation.equals(page.animation())
                    && AttackOverrides.isEditable(page.styleId())) {
                return page;
            }
        }
        return null;
    }

    private boolean isGrouped(LibraryRow row) {
        String animation = row.page().animation();
        return animation != null && openGroups.contains(animation);
    }

    private Double sortKeyOf(LibraryRow row) {
        StylePage page = row.isVariant()
                ? variantPage(row.variant(), row.page())
                : row.page();
        return switch (state.sort()) {
            case NAME -> 0.0D;
            case DAMAGE -> -page.damageMultiplier();
            case ARC -> -Math.abs(page.angle());
            case COST -> row.isVariant()
                    ? state.nextCostOf(row.variant(), page)
                    : state.nextCostOf(page, row.affixId());
            case RARITY -> (double) -page.rarity().ordinal();
        };
    }

    private boolean rowUsable(LibraryRow row) {
        return row.isVariant()
                ? state.canUseVariant(row.variant())
                : (row.affix() == null
                        ? state.ownsPlain(row.page().styleId())
                        : state.canUse(row.page().styleId()));
    }

    private String rowLabel(LibraryRow row) {
        if (row.affix() != null) {
            return affixName(row.affix());
        }
        return row.isVariant()
                ? StyleText.affixedName(row.variant(), row.page()).getString()
                : StyleText.name(row.page()).getString();
    }

    protected int listTop() {
        return top + HEADER_HEIGHT + 14;
    }

    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER_HEIGHT - PADDING;
    }

    protected int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_HEIGHT);
    }

    private int maxLibraryScroll() {
        return Math.max(0, libraryRows.size() - visibleRows());
    }

    private String categoryLabel(String category) {
        if (category == null) {
            return Component.translatable(KEY_ALL_CATEGORIES).getString();
        }
        if (CustomizerState.NATIVE_CATEGORY.equals(category)) {
            return Component.translatable(KEY_THIS_WEAPON).getString();
        }
        return StyleText.categoryName(category).getString();
    }

    private List<String> categoryEntries() {
        List<String> entries = new ArrayList<>();
        entries.add(null);
        entries.addAll(state.tabCategories());
        return entries;
    }

    private int maxCategoryScroll() {
        return Math.max(0, categoryEntries().size() - visibleRows());
    }


    private int categoryWidth() {
        return Math.round(categoryReveal * CATEGORY_WIDTH);
    }

    private boolean categoriesVisible() {
        return categoryWidth() > 0;
    }

    private int categoryX() {
        return left + PADDING;
    }

    private int railX() {
        return left + PADDING + categoryWidth() + (categoriesVisible() ? GAP : 0);
    }

    private int libraryX() {
        return railX() + RAIL_WIDTH + GAP;
    }

    private int rightX() {
        return libraryX() + LIBRARY_WIDTH + GAP + 4;
    }

    private int rightWidth() {
        return left + PANEL_WIDTH - PADDING - rightX();
    }

    private void promptBudgetOverride() {
        minecraft.setScreen(new BudgetOverrideScreen(this,
                state.budgetOverride().orElse(state.budget()),
                state.budgetOverride().isPresent(),
                value -> state.setBudgetOverride(value)));
    }

    private void showToast(Component message) {
        toast = message;
        toastTicks = 60;
    }

    private int sequenceSlotsBottom() {
        return listBottom() - PREVIEW_SIZE - GAP;
    }

    private int visibleSequenceSlots() {
        int slotSpace = sequenceSlotsBottom() - (listTop() + BUDGET_HEIGHT);
        return Math.max(1, slotSpace / (SLOT_HEIGHT + 2));
    }

    private int lastVisibleSequenceSlot() {
        return Math.min(state.maxAttacks(), sequenceScroll + visibleSequenceSlots());
    }

    private int sequenceSlotWidth() {
        return state.maxAttacks() > visibleSequenceSlots()
                ? rightWidth() - SCROLLBAR_WIDTH - 3
                : rightWidth();
    }

    private int removeButtonX(int x, int slotWidth) {
        return x + slotWidth - BUTTON_HIT_WIDTH - 2;
    }

    private int affixButtonX(int x, int slotWidth, int index) {
        List<StylePage> pages = state.sequence().resolve();
        StylePage page = index < pages.size() ? pages.get(index) : null;
        String cost = page == null
                ? "?"
                : StyleText.formatDecimal(state.costAt(index));
        return removeButtonX(x, slotWidth) - font.width(cost) - BUTTON_HIT_WIDTH - 6;
    }

    private static String variantKeyOf(StyleEntry variant) {
        return variant == null ? null : variant.learnedKey();
    }

    private AttackOverride effectiveOverride(StylePage page, int index) {
        AttackOverride fromStyle = page == null ? null : AttackOverrides.get(page.styleId());
        AttackOverride fromSlot = state.attackOverrideFor(index);
        if (fromStyle == null) {
            return fromSlot;
        }
        return fromSlot == null ? fromStyle : fromStyle.overriddenBy(fromSlot);
    }

    private void repositionWidgets() {
        int searchX = libraryX();
        int searchWidth = LIBRARY_WIDTH - 58;
        if (searchBox != null) {
            searchBox.setX(searchX);
        }
        if (sortButton != null) {
            sortButton.setX(searchX + searchWidth + 4);
        }
    }

    private AttackOverride libraryOverride(LibraryRow row, StylePage page) {
        AttackOverride fromStyle = page == null ? null : AttackOverrides.get(page.styleId());
        AttackOverride fromEntry = row.isVariant()
                ? row.variant().attackOverride().orElse(null)
                : nativeOverrideOf(page);
        if (fromStyle == null) {
            return fromEntry;
        }
        return fromEntry == null ? fromStyle : fromStyle.overriddenBy(fromEntry);
    }

    private String librarySound(LibraryRow row, StylePage page) {
        if (row.isVariant()) {
            return row.variant().soundId().orElse(null);
        }
        StyleEntry native_ = page == null
                ? null
                : state.nativeEntryOf(page.styleId());
        return native_ == null ? null : native_.soundId().orElse(null);
    }

    private AttackOverride nativeOverrideOf(StylePage page) {
        if (page == null || !state.isNative(page.styleId())) {
            return null;
        }
        for (StyleEntry entry : SequenceService.baselineOf(state.weapon()).entries()) {
            if (entry.customStyleId().isPresent() || !entry.affixIds().isEmpty()) {
                continue;
            }
            StylePage resolved = StyleRegistry.get(entry.styleId());
            String styleId = resolved == null ? entry.styleId() : resolved.styleId();
            if (page.styleId().equals(styleId) && entry.attackOverride().isPresent()) {
                return entry.attackOverride().get();
            }
        }
        return null;
    }

    private List<StyleAffix> innateAt(int index) {
        String affixId = InnateAffixes.affixFor(state.weapon(), index);
        if (affixId == null) {
            return List.of();
        }
        StyleAffix affix = AffixRegistry.get(affixId);
        return affix == null ? List.of() : List.of(affix);
    }

    private StylePage variantPage(StyleEntry variant, StylePage page) {
        return variant.attackOverride()
                .map(override -> AttackOverrides.applyTo(page, override))
                .orElse(page);
    }

    private void copyStyle(int index) {
        if (state.sequence().entry(index) == null) {
            return;
        }
        if (SequenceService.isEtched(state.weapon())
                && CombatStylesConfig.get().etchOncePerWeapon()) {
            showToast(Component.translatable(KEY_ALREADY_ETCHED).withStyle(ChatFormatting.RED));
            return;
        }
        NetworkManager.sendExtractStyle(new ExtractStylePayload(index));
        showToast(Component.translatable(KEY_COPY_SENT).withStyle(ChatFormatting.AQUA));
    }

    private void openAttackBuilder() {
        if (!state.creative()) {
            showToast(Component.translatable(KEY_CREATIVE_ONLY).withStyle(ChatFormatting.RED));
            return;
        }
        minecraft.setScreen(new AttackManagerScreen(this, state.weaponCategory(), state.creative(), created -> {
            if (!ConfigEdits.permitted()) {
                showToast(ConfigEdits.operatorOnly().copy().withStyle(ChatFormatting.RED));
            } else if (ConfigEdits.save(EditConfigPayload.Kind.ATTACK, created.id(),
                    CustomAttack.CODEC, created)) {
                CustomAttacks.acceptLocal(created);
                refreshStyles();
            } else {
                showToast(Component.translatable(KEY_BUILDER_FAILED)
                        .withStyle(ChatFormatting.RED));
            }
        }, removed -> {
            CustomAttacks.removeLocal(removed);
            refreshStyles();
        }));
    }

    private void refreshStyles() {
        StyleRegistry.rebuild();
        state.refresh();
        rebuildLibrary();
    }

    private void openAttackEditor(int index) {
        if (!state.creative()) {
            showToast(Component.translatable(KEY_CREATIVE_ONLY).withStyle(ChatFormatting.RED));
            return;
        }
        StyleEntry entry = state.sequence().entry(index);
        StylePage page = StyleRegistry.get(entry == null ? null : entry.styleId());
        if (entry == null || page == null) {
            return;
        }
        AttackOverride current = effectiveOverride(page, index);
        StylePage edited = state.isNative(page.styleId())
                || SequenceComposer.ignoresAffinity(entry, state.attackOverrideFor(index),
                        page.styleId())
                ? page
                : CategoryAffinity.scaled(page, state.weaponCategory());
        minecraft.setScreen(new AttackEditorScreen(this, edited, current, override -> {
            state.setAttackOverride(index, override);
            minecraft.setScreen(this);
        }, entry.name().orElse(""), name -> state.rename(index, name)));
    }

    private void openDefaultEditor(StylePage page) {
        if (!state.creative()) {
            showToast(Component.translatable(KEY_CREATIVE_ONLY).withStyle(ChatFormatting.RED));
            return;
        }
        if (!AttackOverrides.isEditable(page.styleId())) {
            showToast(Component.translatable(KEY_DATAPACK_LOCKED).withStyle(ChatFormatting.RED));
            return;
        }
        AttackOverride current = AttackOverrides.get(page.styleId());
        AttackOverride base = current == null ? AttackOverride.EMPTY : current;
        List<String> defaultAffixes = new ArrayList<>(base.affixes());
        Map<String, AffixTuning> defaultTuning = new LinkedHashMap<>(base.affixTuning());
        minecraft.setScreen(new AttackEditorScreen(this, page, base,
                override -> {
                    AttackOverride saved = (override == null ? AttackOverride.EMPTY : override)
                            .withAffixes(defaultAffixes, defaultTuning);
                    if (!ConfigEdits.permitted()) {
                        showToast(ConfigEdits.operatorOnly().copy().withStyle(ChatFormatting.RED));
                    } else if (ConfigEdits.save(EditConfigPayload.Kind.ATTACK_OVERRIDE,
                            page.styleId(), AttackOverride.CODEC, saved)) {
                        AttackOverrides.acceptLocal(page.styleId(), saved);
                        rebuildLibrary();
                        showToast(Component.translatable(KEY_DEFAULT_SAVED)
                                .withStyle(ChatFormatting.GREEN));
                    } else {
                        showToast(ConfigEdits.tooLarge().copy().withStyle(ChatFormatting.RED));
                    }
                    minecraft.setScreen(this);
                })
                .withStylePage()
                .withAffixes(defaultAffixes, picked -> {
                    defaultAffixes.clear();
                    defaultAffixes.addAll(picked);
                })
                .withTuning(defaultTuning, tuned -> {
                    defaultTuning.clear();
                    defaultTuning.putAll(tuned);
                }));
    }

    private void openExpandedPreview() {
        List<String> affixIds = List.of();
        for (int i = 0; i < state.sequence().entries().size(); i++) {
            StyleEntry entry = state.sequence().entry(i);
            if (entry != null && entry.styleId().equals(selected.styleId())) {
                affixIds = entry.affixIds();
                break;
            }
        }
        minecraft.setScreen(new PreviewScreen(this, preview, selected, affixIds));
    }

    private void openAffixPicker(int index) {
        StyleEntry entry = state.sequence().entry(index);
        StylePage page = StyleRegistry.get(entry == null ? null : entry.styleId());
        if (entry == null || page == null) {
            return;
        }
        if (entry.isNamed() && !state.creative() && CombatStylesConfig.get().namedEntriesLocked()) {
            showToast(Component.translatable(KEY_NAMED_LOCKED).withStyle(ChatFormatting.RED));
            return;
        }
        minecraft.setScreen(new AffixPickerScreen(this, page, entry, index, state, chosen -> {
            state.setAffixes(index, chosen);
            minecraft.setScreen(this);
        }));
    }

    private void openSoundPicker(int index) {
        if (!state.creative()) {
            showToast(Component.translatable(KEY_CREATIVE_ONLY).withStyle(ChatFormatting.RED));
            return;
        }
        StyleEntry entry = state.sequence().entry(index);
        StylePage page = entry == null ? null : StyleRegistry.get(entry.styleId());
        if (page == null || minecraft == null) {
            return;
        }
        minecraft.setScreen(new SoundPickerScreen(this, page, state.soundIdFor(index),
                soundId -> state.setSound(index, soundId)));
    }

    private void apply() {
        if (sendApply()) {
            onClose();
        }
    }

    private boolean sendApply() {
        if (SequenceService.isLocked(state.weapon())) {
            showToast(Component.translatable(KEY_ITEM_LOCKED).withStyle(ChatFormatting.RED));
            return false;
        }
        int minimum = SequenceLimits.minimumFor(state.weapon());
        if (!state.sequence().isEmpty() && state.sequence().size() < minimum) {
            showToast(Component.translatable(KEY_BELOW_MINIMUM,
                    minimum, state.sequence().size()).withStyle(ChatFormatting.RED));
            return false;
        }
        StyleSequence outgoing = state.matchesDefault() ? StyleSequence.EMPTY : state.sequence();
        NetworkManager.sendApplySequence(new ApplySequencePayload(state.slot(), outgoing,
                state.attackOverrides(), improvised, state.budgetOverride()));
        return true;
    }

    private List<DropdownMenu> menus() {
        List<DropdownMenu> menus = new ArrayList<>(2);
        if (clearMenu != null) {
            menus.add(clearMenu);
        }
        if (authoringMenu != null && state.creative()) {
            menus.add(authoringMenu);
        }
        return menus;
    }

    private void clearSequence() {
        state.clear();
        selected = null;
        selectedVariant = null;
        preview.select(null);
    }

    private boolean requireOperator() {
        if (ConfigEdits.permitted()) {
            return true;
        }
        showToast(ConfigEdits.operatorOnly().copy().withStyle(ChatFormatting.RED));
        return false;
    }

    private void exportDefault() {
        if (!requireOperator() || !sendApply()) {
            return;
        }
        if (ConfigEdits.send(new EditConfigPayload(EditConfigPayload.Kind.DEFAULT_STYLE,
                state.slot().getName(), EditConfigPayload.NONE))) {
            showToast(Component.translatable(KEY_DEFAULT_EXPORTED).withStyle(ChatFormatting.GREEN));
        }
    }

    private void removeDefault() {
        ResourceLocation itemId = DefaultStyleConfig.itemIdOf(state.weapon());
        if (itemId == null || !requireOperator()) {
            return;
        }
        if (ConfigEdits.remove(EditConfigPayload.Kind.DEFAULT_STYLE_CLEAR, itemId.toString())) {
            showToast(Component.translatable(KEY_DEFAULT_REMOVED).withStyle(ChatFormatting.GREEN));
        }
    }

    private void saveSlotAsCustomStyle() {
        List<StyleEntry> entries = state.sequence().entries();
        List<String> choices = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            StyleEntry entry = entries.get(i);
            StylePage page = StyleRegistry.get(CustomStyles.resolve(entry).styleId());
            String label = page == null
                    ? entry.styleId()
                    : StyleText.affixedName(entry, page).getString();
            choices.add((i + 1) + SLOT_LABEL_SEPARATOR + label);
        }
        minecraft.setScreen(new ChoicePickerScreen(this,
                Component.translatable(KEY_PICK_SLOT), choices, null, picked -> {
                    int index = choices.indexOf(picked);
                    if (index < 0) {
                        minecraft.setScreen(this);
                        return;
                    }
                    StyleEntry seed = CustomStyles.resolve(entries.get(index));
                    AttackOverride fromSlot = state.attackOverrideFor(index);
                    if (fromSlot != null) {
                        seed = seed.withAttackOverride(seed.attackOverride()
                                .map(current -> current.overriddenBy(fromSlot))
                                .orElse(fromSlot));
                    }
                    minecraft.setScreen(new CustomStyleEditorScreen(this, null, seed,
                            saved -> refreshStyles()));
                }).handingOffOnPick());
    }

    private void openCustomStyles() {
        minecraft.setScreen(new CustomStyleManagerScreen(this, state.creative(), this::refreshStyles));
    }

    private void openWeaponCategory() {
        EquipmentSlot slot = state.slot();
        minecraft.setScreen(new ChoicePickerScreen(this, Component.translatable(KEY_CATEGORY_PICKER),
                WeaponCategories.allNames(),
                WeaponCategories.stackCategoryOf(state.weapon()).orElse(null),
                chosen -> {
                    String category = WeaponCategories.normalize(chosen).orElse("");
                    NetworkManager.sendApplyWeaponCategory(new ApplyWeaponCategoryPayload(slot, category));
                    WeaponCategories.store(state.weapon(), category);
                    state.refreshWeaponCategory();
                    rebuildLibrary();
                })
                .creatingNew(WeaponCategories::normalize));
    }

    @Override
    public void tick() {
        preview.tick();
        if (toastTicks > 0) {
            toastTicks--;
        }

        ItemStack held = state.weapon();
        if (!ItemStack.isSameItemSameComponents(held, trackedWeapon)) {
            trackedWeapon = held.copy();
            state.refresh();
            rebuildLibrary();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        float target = categoriesOpen ? 1.0F : 0.0F;
        if (categoryReveal != target) {
            float step = REVEAL_SPEED * Math.max(1.0F, partialTick);
            categoryReveal = categoryReveal < target
                    ? Math.min(target, categoryReveal + step)
                    : Math.max(target, categoryReveal - step);
            repositionWidgets();
        }

        graphics.fill(0, 0, width, height, COLOR_SHADE);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_PANEL);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER_LIT);
        graphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + 2, 0x14FFFFFF);

        renderHeader(graphics);
        renderColumnHeadings(graphics);
        renderCategories(graphics, mouseX, mouseY);
        renderRail(graphics, mouseX, mouseY);
        renderLibrary(graphics, mouseX, mouseY);
        renderRightPanel(graphics, mouseX, mouseY);

        for (Button button : panelButtons) {
            button.render(graphics, mouseX, mouseY, partialTick);
        }
        searchBox.render(graphics, mouseX, mouseY, partialTick);

        renderShortcuts(graphics);
        renderToast(graphics);
        boolean menuOpen = false;
        for (DropdownMenu menu : menus()) {
            menu.render(graphics, font, mouseX, mouseY);
            menuOpen |= menu.isOpen();
        }
        if (!menuOpen) {
            renderTooltips(graphics, mouseX, mouseY);
        }
    }

    private void renderHeader(GuiGraphics graphics) {
        graphics.drawString(font, title, left + PADDING, top + PADDING, COLOR_TEXT, false);

        ItemStack weapon = state.weapon();
        if (!weapon.isEmpty()) {
            String name = trim(weapon.getHoverName().getString(), 170);
            int nameWidth = font.width(name);
            graphics.drawString(font, name, left + PANEL_WIDTH - PADDING - nameWidth - 20,
                    top + PADDING, COLOR_MUTED, false);
            graphics.renderItem(weapon, left + PANEL_WIDTH - PADDING - 17, top + PADDING - 4);
        }

        graphics.drawString(font, Component.translatable(KEY_PROGRESS, state.learnedCount(), state.totalCount()),
                left + PADDING, top + 19, COLOR_LOCKED, false);

        if (state.substituted()) {
            Component swapped = Component.translatable(KEY_SUBSTITUTED);
            graphics.drawString(font, swapped,
                    left + PANEL_WIDTH - PADDING - font.width(swapped), top + 19,
                    COLOR_ACCENT, false);
        } else if (SequenceService.isLocked(state.weapon())) {
            Component locked = Component.translatable(KEY_ITEM_LOCKED);
            graphics.drawString(font, locked,
                    left + PANEL_WIDTH - PADDING - font.width(locked), top + 19, COLOR_BAD, false);
        } else if (!SequenceService.usesDefault(state.weapon())) {
            Component etched = Component.translatable(KEY_ETCHED);
            graphics.drawString(font, etched,
                    left + PANEL_WIDTH - PADDING - font.width(etched), top + 19, COLOR_ACCENT, false);
        } else {
            Component stock = Component.translatable(KEY_DEFAULT_SEQUENCE);
            graphics.drawString(font, stock,
                    left + PANEL_WIDTH - PADDING - font.width(stock), top + 19, COLOR_LOCKED, false);
        }

        graphics.fill(left + PADDING, top + HEADER_HEIGHT, left + PANEL_WIDTH - PADDING,
                top + HEADER_HEIGHT + 1, COLOR_BORDER);
    }

    private void renderColumnHeadings(GuiGraphics graphics) {
        int y = top + HEADER_HEIGHT + 4;
        if (categoriesVisible()) {
            graphics.drawString(font, Component.translatable(KEY_CATEGORIES), categoryX(), y, COLOR_HEADING, false);
        }
        graphics.drawString(font, Component.translatable(KEY_LIBRARY), libraryX(), y, COLOR_HEADING, false);
        graphics.drawString(font, Component.translatable(KEY_SEQUENCE), rightX(), y, COLOR_HEADING, false);
    }

    private void renderRail(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = railX();
        int listTop = listTop();
        int listBottom = listBottom();
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, listTop,
                RAIL_WIDTH, listBottom - listTop);

        graphics.fill(x, listTop, x + RAIL_WIDTH, listBottom,
                hovered ? COLOR_HOVER : COLOR_SUNK);
        graphics.fill(x, listTop, x + 1, listBottom, COLOR_ACCENT);

        String glyph = categoriesOpen ? "<" : ">";
        graphics.drawString(font, glyph, x + (RAIL_WIDTH - font.width(glyph)) / 2,
                (listTop + listBottom) / 2 - 4,
                hovered ? COLOR_ACCENT : COLOR_LOCKED, false);
    }

    private void renderCategories(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!categoriesVisible()) {
            return;
        }
        int x = categoryX();
        int listTop = listTop();
        int listBottom = listBottom();
        int revealed = categoryWidth();

        graphics.enableScissor(x - 2, listTop - 2, x + revealed, listBottom + 2);
        graphics.fill(x - 2, listTop - 2, x + CATEGORY_WIDTH + 2, listBottom + 2, COLOR_SUNK);

        List<String> entries = categoryEntries();
        int y = listTop;
        int limit = Math.min(entries.size(), categoryScroll + visibleRows());

        for (int i = categoryScroll; i < limit; i++) {
            String category = entries.get(i);
            boolean active = category == null
                    ? state.activeCategory() == null
                    : category.equals(state.activeCategory());
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y, CATEGORY_WIDTH, ROW_HEIGHT);

            if (active) {
                graphics.fill(x, y, x + CATEGORY_WIDTH, y + ROW_HEIGHT, COLOR_SELECTED);
                graphics.fill(x, y, x + 2, y + ROW_HEIGHT, COLOR_ACCENT);
            } else if (hovered) {
                graphics.fill(x, y, x + CATEGORY_WIDTH, y + ROW_HEIGHT, COLOR_HOVER);
            } else if ((i & 1) == 1) {
                graphics.fill(x, y, x + CATEGORY_WIDTH, y + ROW_HEIGHT, COLOR_ROW_ALT);
            }

            String label = categoryLabel(category);
            String count = String.valueOf(state.categoryCount(category));
            int countWidth = font.width(count);
            graphics.drawString(font, trim(label, CATEGORY_WIDTH - countWidth - 14), x + 6, y + 3,
                    active ? COLOR_TEXT : COLOR_MUTED, false);
            graphics.drawString(font, count, x + CATEGORY_WIDTH - countWidth - 4, y + 3, COLOR_LOCKED, false);
            y += ROW_HEIGHT;
        }

        categoryScrollbar.render(graphics, x + CATEGORY_WIDTH + 3, listTop, listBottom,
                categoryScroll, maxCategoryScroll(), entries.size(), visibleRows());
        graphics.disableScissor();
    }

    private void renderLibrary(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = libraryX();
        int listTop = listTop();
        int listBottom = listBottom();

        graphics.fill(x - 2, listTop - 2, x + LIBRARY_WIDTH + 2, listBottom + 2, COLOR_SUNK);

        if (libraryRows.isEmpty()) {
            String key = state.totalCount() == 0 ? KEY_NO_STYLES : KEY_NO_MATCH;
            graphics.drawString(font, trim(Component.translatable(key).getString(), LIBRARY_WIDTH - 8),
                    x + 4, listTop + 4, COLOR_LOCKED, false);
            return;
        }

        int y = listTop;
        int limit = Math.min(libraryRows.size(), libraryScroll + visibleRows());

        for (int i = libraryScroll; i < limit; i++) {
            LibraryRow row = libraryRows.get(i);
            if (row.isGroup()) {
                renderGroupHeader(graphics, row, x, y, mouseX, mouseY);
                y += ROW_HEIGHT;
                continue;
            }
            int rowIndent = isGrouped(row) ? GROUP_INDENT : 0;
            StylePage page = row.page();
            String affixId = row.affixId();
            boolean variant = row.affix() != null;

            boolean usable = row.isVariant()
                    ? state.canUseVariant(row.variant())
                    : rowUsable(row);
            boolean spare = row.isVariant()
                    ? state.hasSpareVariant(row.variant())
                    : state.hasSpareCopy(page.styleId(), affixId);
            boolean affordable = usable && state.canAfford(
                    row.isVariant() ? variantPage(row.variant(), page) : page, affixId);
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y, LIBRARY_WIDTH, ROW_HEIGHT);
            boolean isSelected = selected != null && selected.styleId().equals(page.styleId())
                    && Objects.equals(selectedAffix, affixId)
                    && Objects.equals(variantKeyOf(selectedVariant), variantKeyOf(row.variant()));

            if (isSelected) {
                graphics.fill(x, y, x + LIBRARY_WIDTH, y + ROW_HEIGHT, COLOR_SELECTED);
            } else if (hovered) {
                graphics.fill(x, y, x + LIBRARY_WIDTH, y + ROW_HEIGHT, COLOR_HOVER);
            } else if ((i & 1) == 1) {
                graphics.fill(x, y, x + LIBRARY_WIDTH, y + ROW_HEIGHT, COLOR_ROW_ALT);
            }

            boolean isNative = state.isNative(page.styleId());
            int stripe = variant ? affixColor(row.affix()) : (usable ? rarityColor(page) : COLOR_LOCKED);
            graphics.fill(x, y, x + 2, y + ROW_HEIGHT, stripe);
            if (isNative && !variant) {
                graphics.fill(x + 2, y, x + 3, y + ROW_HEIGHT, COLOR_ACCENT);
            }

            int textLeft = x + 7 + rowIndent + (variant ? 8 : 0);
            if (row.expandable()) {
                graphics.drawString(font, expanded.contains(page.styleId()) ? "-" : "+",
                        x + 6, y + 3, COLOR_ACCENT, false);
                textLeft += 7;
            }

            String cost = usable
                    ? StyleText.formatDecimal(row.isVariant()
                            ? state.nextCostOf(row.variant(), page)
                            : state.nextCostOf(page, affixId))
                    : "x";
            int costWidth = font.width(cost);

            String copies = "";
            boolean tracked = usable && !state.creative()
                    && !(row.isVariant() && state.isNativeVariant(row.variant()))
                    && (variant || row.isVariant() || !isNative);
            if (tracked) {
                int owned;
                int used;
                if (row.isVariant()) {
                    owned = state.variantCopies(row.variant());
                    used = state.variantUsed(row.variant());
                } else {
                    owned = state.copiesOwned(page.styleId(), affixId);
                    used = state.copiesUsed(page.styleId(), affixId);
                }
                copies = (owned - used) + "/" + owned;
            }
            int copiesWidth = copies.isEmpty() ? 0 : font.width(copies) + 6;
            int textWidth = LIBRARY_WIDTH - costWidth - copiesWidth - (textLeft - x) - 9;

            String label = variant
                    ? affixName(row.affix())
                    : (row.isVariant()
                            ? StyleText.affixedName(row.variant(), page).getString()
                            : StyleText.name(page).getString());
            if (page.isConditional()) {
                label = label + " " + MARKER_CONDITIONAL;
            }
            int nameColor = !usable ? COLOR_LOCKED : (affordable && spare ? COLOR_TEXT : COLOR_BAD);
            if (variant) {
                nameColor = spare && affordable ? affixColor(row.affix()) : COLOR_BAD;
            }
            graphics.drawString(font, trim(label, textWidth), textLeft, y + 3, nameColor, false);

            if (!copies.isEmpty()) {
                graphics.drawString(font, copies, x + LIBRARY_WIDTH - costWidth - copiesWidth - 5, y + 3,
                        spare ? COLOR_MUTED : COLOR_BAD, false);
            }
            graphics.drawString(font, cost, x + LIBRARY_WIDTH - costWidth - 5, y + 3,
                    !usable ? COLOR_LOCKED : (affordable ? COLOR_HEADING : COLOR_BAD), false);
            y += ROW_HEIGHT;
        }

        libraryScrollbar.render(graphics, x + LIBRARY_WIDTH + 3, listTop, listBottom,
                libraryScroll, maxLibraryScroll(), libraryRows.size(), visibleRows());
    }

    private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = rightX();
        int width = rightWidth();

        if (!state.weaponSupported()) {
            graphics.drawString(font, trim(Component.translatable(KEY_UNSUPPORTED).getString(), width),
                    x, listTop(), COLOR_BAD, false);
            if (state.creative()) {
                int buttonY = listTop() + 16;
                boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, buttonY,
                        IMPROVISE_WIDTH, IMPROVISE_HEIGHT);
                graphics.fill(x, buttonY, x + IMPROVISE_WIDTH, buttonY + IMPROVISE_HEIGHT,
                        hovered ? 0x40FFFFFF : 0x20FFFFFF);
                graphics.renderOutline(x, buttonY, IMPROVISE_WIDTH, IMPROVISE_HEIGHT, COLOR_SCROLL);
                graphics.drawString(font, Component.translatable(KEY_IMPROVISE), x + 6, buttonY + 4,
                        hovered ? COLOR_ACCENT : COLOR_TEXT, false);
                graphics.drawString(font,
                        trim(Component.translatable(KEY_IMPROVISE_HINT).getString(), width),
                        x, buttonY + IMPROVISE_HEIGHT + 6, COLOR_MUTED, false);
            }
            return;
        }

        int y = listTop();
        renderBudget(graphics, x, y, width);
        y += BUDGET_HEIGHT;

        List<String> styleIds = state.sequence().styleIds();
        List<StylePage> pages = state.sequence().resolve();

        int slots = state.maxAttacks();
        int slotsBottom = sequenceSlotsBottom();
        int visibleSlots = visibleSequenceSlots();
        boolean scrollable = slots > visibleSlots;
        int slotWidth = sequenceSlotWidth();

        sequenceScroll = Math.clamp(sequenceScroll, 0, Math.max(0, slots - visibleSlots));
        int firstSlot = sequenceScroll;
        int lastSlot = lastVisibleSequenceSlot();

        graphics.fill(x - 2, y - 2, x + width, slotsBottom, COLOR_SUNK);

        int slotsTop = y;
        for (int i = firstSlot; i < lastSlot; i++) {
            boolean filled = i < styleIds.size();
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y, slotWidth, SLOT_HEIGHT);

            if (dragging && dragHoverIndex == i && dragFrom != i) {
                graphics.fill(x, y - 2, x + slotWidth, y - 1, COLOR_ACCENT);
            }

            if (filled) {
                StylePage page = i < pages.size() ? pages.get(i) : null;
                int background = dragging && dragFrom == i ? COLOR_DRAG : (hovered ? COLOR_HOVER : COLOR_ROW_ALT);
                graphics.fill(x, y, x + slotWidth, y + SLOT_HEIGHT, background);
                graphics.fill(x, y, x + 2, y + SLOT_HEIGHT, page == null ? COLOR_BAD : rarityColor(page));

                String prefix = (i + 1) + ".";
                graphics.drawString(font, prefix, x + 6, y + 3, COLOR_MUTED, false);

                int textLeft = x + 8 + font.width(prefix);
                String cost = page == null
                        ? "?"
                        : StyleText.formatDecimal(state.costAt(i));
                int costWidth = font.width(cost);
                int available = (x + slotWidth - 16 - costWidth) - textLeft - 4;

                StyleEntry slotEntry = state.sequence().entry(i);
                List<StyleAffix> slotAffixes = slotEntry == null
                        ? List.of()
                        : slotEntry.resolveAffixes().affixes();
                StyleAffix leadAffix = slotAffixes.isEmpty() ? null : slotAffixes.getFirst();

                String label = page == null
                        ? styleIds.get(i)
                        : StyleText.affixedName(slotEntry, page).getString();
                if (EquipmentHints.affectedInAnyStance(minecraft.player, state.sequence().entries(), slotEntry, i)) {
                    label = EquipmentHints.MARKER_EQUIPMENT + " " + label;
                }
                boolean unlearned = slotEntry != null && !slotAffixes.isEmpty()
                        && !state.variantAllowed(slotEntry);
                int labelColour = page == null || unlearned
                        ? COLOR_BAD
                        : (leadAffix != null ? affixColor(leadAffix) : COLOR_TEXT);
                graphics.drawString(font, trim(label, available - 14), textLeft, y + 3,
                        labelColour, false);
                if (unlearned) {
                    graphics.drawString(font, "!", textLeft - 6, y + 3, COLOR_BAD, false);
                }
                graphics.drawString(font, cost, x + slotWidth - 16 - costWidth, y + 3, COLOR_HEADING, false);

                int affixButtonX = affixButtonX(x, slotWidth, i);
                boolean overAffix = ScreenDraw.isWithin(mouseX, mouseY, affixButtonX, y, BUTTON_HIT_WIDTH, ROW_HEIGHT);
                graphics.drawString(font, "+", affixButtonX + 2, y + 3,
                        overAffix ? COLOR_HEADING : (slotAffixes.isEmpty() ? COLOR_LOCKED : COLOR_ACCENT), false);

                int removeX = removeButtonX(x, slotWidth);
                boolean overX = ScreenDraw.isWithin(mouseX, mouseY, removeX, y, BUTTON_HIT_WIDTH, ROW_HEIGHT);
                graphics.drawString(font, "x", removeX + 2, y + 3, overX ? COLOR_BAD : COLOR_LOCKED, false);

                renderSoundRow(graphics, x, y + ROW_HEIGHT, slotWidth, i, mouseX, mouseY);
            } else {
                graphics.renderOutline(x, y, slotWidth, ROW_HEIGHT, COLOR_BORDER);
                String hint = i == styleIds.size()
                        ? Component.translatable(state.sequence().isEmpty() ? KEY_EMPTY : KEY_SLOT_EMPTY).getString()
                        : "";
                if (!hint.isEmpty()) {
                    graphics.drawString(font, trim(hint, slotWidth - 10), x + 7, y + 3, COLOR_LOCKED, false);
                }
            }
            y += (filled ? SLOT_HEIGHT : ROW_HEIGHT) + 2;
        }

        if (scrollable) {
            sequenceScrollbar.render(graphics, x + width - SCROLLBAR_WIDTH - 1, slotsTop,
                    slotsBottom - 2, sequenceScroll, Math.max(0, slots - visibleSlots),
                    slots, visibleSlots);
        }

        int previewX = x + width - PREVIEW_SIZE;
        int previewY = listBottom() - PREVIEW_SIZE;

        renderPreview(graphics, previewX, previewY, mouseX, mouseY);
    }

    private void renderSoundRow(GuiGraphics graphics, int x, int y, int width, int index,
                                int mouseX, int mouseY) {
        graphics.fill(x + 2, y, x + width, y + SOUND_HEIGHT, COLOR_SUNK);

        String soundId = state.soundIdFor(index);
        String label = soundId == null
                ? Component.translatable(KEY_SOUND_NONE).getString()
                : StyleText.soundName(soundId);
        boolean custom = state.sequence().entry(index) != null
                && state.sequence().entry(index).hasCustomSound();

        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x + 2, y, width - 2, SOUND_HEIGHT);
        if (hovered) {
            graphics.fill(x + 2, y, x + width, y + SOUND_HEIGHT, COLOR_HOVER);
        }

        graphics.drawString(font, "~", x + 6, y + 1, custom ? COLOR_ACCENT : COLOR_LOCKED, false);
        String text = trim(label, width - SOUND_TEXT_LEFT - SOUND_ARROW_WIDTH);
        graphics.drawString(font, text, x + SOUND_TEXT_LEFT, y + 1,
                custom ? COLOR_ACCENT : COLOR_LOCKED, false);
        graphics.drawString(font, ">", x + width - 9, y + 1, hovered ? COLOR_ACCENT : COLOR_LOCKED, false);
    }

    private void renderBudget(GuiGraphics graphics, int x, int y, int width) {
        double budget = state.budget();
        double spent = state.spent();
        boolean over = state.overBudget();

        graphics.fill(x, y, x + width, y + 4, COLOR_SUNK);
        double ratio = budget <= 0 ? 0 : Math.min(1.0D, spent / budget);
        int filled = (int) Math.round(width * ratio);
        graphics.fill(x, y, x + filled, y + 4, over ? COLOR_BAD : COLOR_GOOD);
        graphics.renderOutline(x, y, width, 4, COLOR_BORDER);

        String label = over
                ? Component.translatable(KEY_OVER_BUDGET).getString()
                : Component.translatable(KEY_BUDGET, StyleText.formatDecimal(spent),
                        StyleText.formatDecimal(budget)).getString();
        if (state.budgetOverride().isPresent()) {
            label = label + " " + Component.translatable(KEY_BUDGET_OVERRIDDEN).getString();
        }

        int minimum = SequenceLimits.minimumFor(state.weapon());
        int size = state.sequence().size();
        boolean short_ = size > 0 && size < minimum;
        String slots = short_
                ? size + " of " + minimum
                : size + "/" + state.maxAttacks();
        int slotsWidth = font.width(slots);
        graphics.drawString(font, trim(label, width - slotsWidth - 6), x, y + 7,
                over ? COLOR_BAD : COLOR_MUTED, false);
        graphics.drawString(font, slots, x + width - slotsWidth, y + 7,
                short_ ? COLOR_BAD : COLOR_LOCKED, false);
    }

    private void renderPreview(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, COLOR_SUNK);
        graphics.renderOutline(x, y, PREVIEW_SIZE, PREVIEW_SIZE, COLOR_BORDER);

        if (selected == null) {
            return;
        }
        if (!CombatStylesConfig.get().animationPreviewEnabled() || !preview.isAvailable()) {
            graphics.drawString(font, trim(Component.translatable(KEY_PREVIEW_UNAVAILABLE).getString(),
                            PREVIEW_SIZE - 6), x + 4, y + PREVIEW_SIZE / 2 - 4, COLOR_LOCKED, false);
            return;
        }
        graphics.enableScissor(x + 1, y + 1, x + PREVIEW_SIZE - 1, y + PREVIEW_SIZE - 1);
        PreviewRenderer.render(graphics, x, y, PREVIEW_SIZE, preview);
        graphics.disableScissor();

        if (ScreenDraw.isWithin(mouseX, mouseY, x, y, PREVIEW_SIZE, PREVIEW_SIZE)) {
            graphics.renderOutline(x, y, PREVIEW_SIZE, PREVIEW_SIZE, COLOR_ACCENT);
            String expand = Component.translatable(KEY_EXPAND).getString();
            graphics.drawString(font, expand, x + PREVIEW_SIZE - font.width(expand) - 3,
                    y + PREVIEW_SIZE - 10, COLOR_ACCENT, false);
        }
    }

    private void renderToast(GuiGraphics graphics) {
        if (toastTicks <= 0) {
            return;
        }
        int textWidth = font.width(toast);
        int x = left + (PANEL_WIDTH - textWidth) / 2;
        int y = top + PANEL_HEIGHT - FOOTER_HEIGHT - 12;
        graphics.fill(x - 4, y - 2, x + textWidth + 4, y + 10, COLOR_SUNK);
        graphics.drawString(font, toast, x, y, COLOR_TEXT, false);
    }

    private void renderGroupHeader(GuiGraphics graphics, LibraryRow row, int x, int y,
                                   int mouseX, int mouseY) {
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y, LIBRARY_WIDTH, ROW_HEIGHT);
        graphics.fill(x, y, x + LIBRARY_WIDTH, y + ROW_HEIGHT,
                hovered ? COLOR_HOVER : COLOR_SUNK);
        graphics.fill(x, y, x + 2, y + ROW_HEIGHT, COLOR_ACCENT);

        boolean collapsed = !openGroups.contains(row.groupAnimation());
        graphics.drawString(font, collapsed ? "+" : "-", x + 6, y + 3, COLOR_MUTED, false);

        String label = StyleText.prettify(animationLabel(row.groupAnimation()));
        String count = String.valueOf(row.groupSize());
        int countWidth = font.width(count);
        graphics.drawString(font, trim(label, LIBRARY_WIDTH - countWidth - 24),
                x + 16, y + 3, COLOR_TEXT, false);
        graphics.drawString(font, count, x + LIBRARY_WIDTH - countWidth - 6, y + 3,
                COLOR_MUTED, false);
    }

    private static String animationLabel(String animation) {
        if (animation == null || animation.isBlank()) {
            return "";
        }
        int colon = animation.indexOf(':');
        return colon < 0 ? animation : animation.substring(colon + 1);
    }

    private void renderShortcuts(GuiGraphics graphics) {
        if (!Screen.hasAltDown()) {
            return;
        }
        String hint = Component.translatable(KEY_SHORTCUTS).getString();
        int width = font.width(hint) + 8;
        int x = left + (PANEL_WIDTH - width) / 2;
        int y = top + PANEL_HEIGHT - FOOTER_HEIGHT - 24;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, HINT_DEPTH);
        graphics.fill(x, y - 2, x + width, y + 11, COLOR_SUNK);
        graphics.renderOutline(x, y - 2, width, 13, COLOR_BORDER);
        graphics.drawString(font, hint, x + 4, y + 1, COLOR_MUTED, false);
        graphics.pose().popPose();
    }

    private boolean renderSequenceTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = rightX();
        int width = sequenceSlotWidth();
        int y = listTop() + BUDGET_HEIGHT;
        List<StylePage> pages = state.sequence().resolve();
        int count = Math.min(state.sequence().size(), lastVisibleSequenceSlot());

        for (int index = sequenceScroll; index < count; index++) {
            if (ScreenDraw.isWithin(mouseX, mouseY, x, y, width, ROW_HEIGHT)) {
                StyleEntry entry = state.sequence().entry(index);
                StylePage page = index < pages.size() ? pages.get(index) : null;
                if (page == null) {
                    return false;
                }
                CategoryAffinity.Tier tier = state.isNative(page.styleId())
                        || SequenceComposer.ignoresAffinity(entry, state.attackOverrideFor(index),
                                page.styleId())
                        ? CategoryAffinity.Tier.NATIVE
                        : CategoryAffinity.tierOf(CustomStyles.categoryOf(entry, page),
                                state.weaponCategory());
                StylePage shown = tier == CategoryAffinity.Tier.NATIVE
                        ? page
                        : CategoryAffinity.scaled(page, state.weaponCategory());

                List<Component> lines = new ArrayList<>();
                lines.add(StyleText.affixedName(entry, page));
                List<StyleAffix> affixes = entry == null
                        ? List.of()
                        : entry.resolveAffixes().affixes();
                StylePage slotPage = entry == null ? shown : variantPage(entry, shown);
                lines.add(StyleTooltip.subtitle(slotPage, state.costAt(index)));
                if (tier != CategoryAffinity.Tier.NATIVE
                        && CombatStylesConfig.get().showAffinityInTooltip()) {
                    lines.add(Component.translatable(tier == CategoryAffinity.Tier.RELATED
                                    ? KEY_AFFINITY_RELATED : KEY_AFFINITY_ALIEN)
                            .withStyle(tier == CategoryAffinity.Tier.RELATED
                                    ? ChatFormatting.YELLOW : ChatFormatting.RED));
                }
                if (!affixes.isEmpty()) {
                    lines.add(Component.empty());
                    StyleTooltip.appendAffixes(affixes, lines);
                }
                List<Component> defaults = new ArrayList<>();
                StyleTooltip.appendDefaultAffixes(slotPage, defaults);
                if (!defaults.isEmpty()) {
                    lines.add(Component.empty());
                    lines.addAll(defaults);
                }
                List<StyleAffix> innate = innateAt(index);
                if (!innate.isEmpty()) {
                    lines.add(Component.empty());
                    StyleTooltip.appendInnateAffixes(innate, lines);
                }
                lines.add(Component.empty());
                AttackOverride effective = effectiveOverride(slotPage, index);
                StylePage shownStats = effective == null
                        ? slotPage
                        : AttackOverrides.applyTo(slotPage, effective);
                StyleTooltip.appendStatGrid(shownStats, lines);
                List<StyleAffix> acting = new ArrayList<>(affixes);
                acting.addAll(innate);
                StyleTooltip.appendTiming(effective, acting, lines);
                StyleTooltip.appendAttributes(effective, lines);
                EquipmentHints.appendAffectingGear(minecraft.player, state.sequence().entries(), entry, index, lines);
                if (Screen.hasShiftDown()) {
                    StyleTooltip.appendDetails(shownStats, state.soundIdFor(index), lines);
                } else {
                    lines.add(StyleTooltip.expandHint());
                }
                renderWithArc(graphics, lines, shownStats, mouseX, mouseY);
                return true;
            }
            y += SLOT_HEIGHT + 2;
        }
        return false;
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (renderSequenceTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        int x = libraryX();
        int y = listTop();
        int limit = Math.min(libraryRows.size(), libraryScroll + visibleRows());

        for (int i = libraryScroll; i < limit; i++) {
            if (ScreenDraw.isWithin(mouseX, mouseY, x, y, LIBRARY_WIDTH, ROW_HEIGHT)) {
                LibraryRow row = libraryRows.get(i);
                if (row.isGroup()) {
                    return;
                }
                StylePage page = row.page();
                String affixId = row.affixId();

                boolean rowUsable = row.isVariant()
                        ? state.canUseVariant(row.variant())
                        : state.canUse(page.styleId());

                List<Component> lines = new ArrayList<>();
                lines.add(row.isVariant()
                        ? StyleText.affixedName(row.variant(), page)
                        : StyleText.name(page));
                StylePage shownPage = row.isVariant() ? variantPage(row.variant(), page) : page;
                List<StyleAffix> rowAffixes = new ArrayList<>();
                if (row.affix() != null) {
                    rowAffixes.add(row.affix());
                }
                if (row.isVariant()) {
                    rowAffixes.addAll(row.variant().resolveAffixes().affixes());
                }
                double rowCost = StyleCost.of(shownPage);
                for (StyleAffix affix : rowAffixes) {
                    rowCost *= affix.costModifier();
                }
                lines.add(StyleTooltip.subtitle(shownPage, rowCost));
                if (!rowAffixes.isEmpty()) {
                    lines.add(Component.empty());
                    StyleTooltip.appendAffixes(rowAffixes, lines);
                }
                List<Component> rowDefaults = new ArrayList<>();
                StyleTooltip.appendDefaultAffixes(shownPage, rowDefaults);
                if (!rowDefaults.isEmpty()) {
                    lines.add(Component.empty());
                    lines.addAll(rowDefaults);
                }
                lines.add(Component.empty());
                AttackOverride rowOverride = libraryOverride(row, shownPage);
                AttackOverride statOverride = row.isVariant()
                        ? null
                        : nativeOverrideOf(shownPage);
                StylePage rowStats = statOverride == null
                        ? shownPage
                        : AttackOverrides.applyTo(shownPage, statOverride);
                StyleTooltip.appendStatGrid(rowStats, lines);
                StyleTooltip.appendTiming(rowOverride, rowAffixes, lines);
                StyleTooltip.appendAttributes(rowOverride, lines);
                if (Screen.hasShiftDown()) {
                    StyleTooltip.appendDetails(shownPage, librarySound(row, shownPage), lines);
                }

                int used = state.copiesUsed(page.styleId(), affixId);
                if (!row.isVariant() && state.copiesUsed(page.styleId()) > 0) {
                    lines.add(Component.empty());
                    lines.add(Component.translatable(KEY_REPEAT_COST,
                                    StyleText.formatDecimal(state.nextCostOf(page, affixId)))
                            .withStyle(ChatFormatting.GOLD));
                }
                if (row.isVariant()) {
                    if (!state.creative()) {
                        int total = Math.max(state.variantCopies(row.variant()),
                                state.availableCopies(row.variant()));
                        lines.add(Component.translatable(KEY_COPIES,
                                        Math.max(0, total - state.variantUsed(row.variant())),
                                        total)
                                .withStyle(ChatFormatting.DARK_GRAY));
                    }
                } else if (state.isNative(page.styleId()) && row.affix() == null) {
                    lines.add(Component.empty());
                    lines.add(Component.translatable(KEY_NATIVE).withStyle(ChatFormatting.AQUA));
                    if (!state.creative()) {
                        int granted = state.grantedCopies(page.styleId());
                        lines.add(Component.translatable(KEY_COPIES,
                                        Math.max(0, granted - state.styleUsed(page.styleId())),
                                        granted)
                                .withStyle(ChatFormatting.DARK_GRAY));
                    }
                } else if (rowUsable && !state.creative()) {
                    int total = state.copiesOwned(page.styleId(), affixId);
                    lines.add(Component.translatable(KEY_COPIES,
                                    Math.max(0, total - used), total)
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
                if (!rowUsable) {
                    lines.add(Component.empty());
                    lines.add(Component.translatable(KEY_LOCKED).withStyle(ChatFormatting.RED));
                    lines.add(Component.translatable(KEY_LEARN_HINT).withStyle(ChatFormatting.DARK_GRAY));
                } else if (!state.canAfford(page)) {
                    lines.add(Component.empty());
                    lines.add(Component.translatable(KEY_TOO_EXPENSIVE).withStyle(ChatFormatting.RED));
                }
                if (!Screen.hasShiftDown()) {
                    lines.add(StyleTooltip.expandHint());
                }
                renderWithArc(graphics, lines, shownPage, mouseX, mouseY);
                return;
            }
            y += ROW_HEIGHT;
        }
    }

    private void renderWithArc(GuiGraphics graphics, List<Component> lines, StylePage page,
                               int mouseX, int mouseY) {
        List<ClientTooltipComponent> components = new ArrayList<>(lines.size() + 1);
        for (Component line : lines) {
            components.add(ClientTooltipComponent.create(line.getVisualOrderText()));
        }
        if (page != null) {
            components.add(new ArcTooltipComponent(
                    AttackShape.of(page.hitbox(), page.angle()),
                    page.angle(), page.rangeMultiplier(), 1.0D));
        }
        TooltipPanel.render(graphics, font, components, mouseX, mouseY, width, height);
    }

    private String affixName(StyleAffix affix) {
        return Component.translatableWithFallback(affix.nameKey(), affix.fallbackName()).getString();
    }

    private int affixColor(StyleAffix affix) {
        if (affix == null) {
            return COLOR_MUTED;
        }
        if (affix.trail().isPresent()) {
            return affix.trail().get().argb();
        }
        Integer color = affix.rarity().color().getColor();
        return color == null ? COLOR_ACCENT : 0xFF000000 | color;
    }

    private int rarityColor(StylePage page) {
        Integer color = page.rarity().color().getColor();
        return color == null ? COLOR_MUTED : 0xFF000000 | color;
    }

    private String trim(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (DropdownMenu menu : menus()) {
            if (menu.mouseClicked(font, mouseX, mouseY)) {
                return true;
            }
        }
        if (searchBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(searchBox);
            return true;
        }
        if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, railX(), listTop(),
                RAIL_WIDTH, listBottom() - listTop())) {
            categoriesOpen = !categoriesOpen;
            categoryScroll = 0;
            return true;
        }
        if (state.creative() && state.weaponSupported()
                && ScreenDraw.isWithin((int) mouseX, (int) mouseY,
                        rightX(), listTop(), rightWidth(), BUDGET_HEIGHT)) {
            promptBudgetOverride();
            return true;
        }
        if (!state.weaponSupported() && state.creative()
                && ScreenDraw.isWithin((int) mouseX, (int) mouseY, rightX(), listTop() + 16,
                        IMPROVISE_WIDTH, IMPROVISE_HEIGHT)) {
            List<StyleEntry> seed = state.visibleVariants().stream().limit(1).toList();
            if (seed.isEmpty()) {
                showToast(Component.translatable(KEY_NO_STYLES).withStyle(ChatFormatting.RED));
                return true;
            }
            improvised = true;
            NetworkManager.sendApplySequence(new ApplySequencePayload(state.slot(),
                    new StyleSequence(state.sequence().name(), seed),
                    state.attackOverrides(), true, state.budgetOverride()));
            onClose();
            return true;
        }

        if (categoryScrollbar.isOverTrack(mouseX, mouseY)) {
            categoryScroll = categoryScrollbar.beginDrag(mouseX, mouseY, categoryScroll);
            return true;
        }
        if (libraryScrollbar.isOverTrack(mouseX, mouseY)) {
            libraryScroll = libraryScrollbar.beginDrag(mouseX, mouseY, libraryScroll);
            return true;
        }
        if (sequenceScrollbar.isOverTrack(mouseX, mouseY)) {
            sequenceScroll = sequenceScrollbar.beginDrag(mouseX, mouseY, sequenceScroll);
            return true;
        }

        List<String> entries = categoryEntries();
        int catY = listTop();
        int catLimit = categoriesVisible()
                ? Math.min(entries.size(), categoryScroll + visibleRows())
                : categoryScroll;
        for (int i = categoryScroll; i < catLimit; i++) {
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, categoryX(), catY, CATEGORY_WIDTH, ROW_HEIGHT)) {
                state.setActiveCategory(entries.get(i));
                libraryScroll = 0;
                rebuildLibrary();
                return true;
            }
            catY += ROW_HEIGHT;
        }

        int libY = listTop();
        int libLimit = Math.min(libraryRows.size(), libraryScroll + visibleRows());
        for (int i = libraryScroll; i < libLimit; i++) {
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, libraryX(), libY, LIBRARY_WIDTH, ROW_HEIGHT)) {
                LibraryRow row = libraryRows.get(i);
                if (row.isGroup()) {
                    if (button == 1) {
                        openDefaultEditor(row.page());
                        return true;
                    }
                    if (!openGroups.remove(row.groupAnimation())) {
                        openGroups.add(row.groupAnimation());
                    }
                    rebuildLibrary();
                    return true;
                }
                StylePage page = row.page();
                String affixId = row.affixId();

                if (row.expandable() && mouseX < libraryX() + 14) {
                    if (!expanded.remove(page.styleId())) {
                        expanded.add(page.styleId());
                    }
                    rebuildLibrary();
                    return true;
                }

                selected = page;
                selectedAffix = affixId;
                selectedVariant = row.variant();
                preview.select(page.animation());
                if (button == 1) {
                    if (isGrouped(row) && !AttackOverrides.isEditable(page.styleId())) {
                        showToast(Component.translatable(KEY_EDIT_ON_GROUP)
                                .withStyle(ChatFormatting.YELLOW));
                        return true;
                    }
                    openDefaultEditor(page);
                    return true;
                }
                if (button == 0) {
                    boolean usable = row.isVariant()
                            ? state.canUseVariant(row.variant())
                            : rowUsable(row);
                    boolean spare = row.isVariant()
                            ? state.hasSpareVariant(row.variant())
                            : state.hasSpareCopy(page.styleId(), affixId);
                    int owned = row.isVariant()
                            ? state.variantCopies(row.variant())
                            : state.copiesOwned(page.styleId(), affixId);
                    if (!usable) {
                        showToast(Component.translatable(KEY_LOCKED).withStyle(ChatFormatting.RED));
                    } else if (!spare) {
                        showToast(Component.translatable(KEY_NO_COPIES, owned)
                                .withStyle(ChatFormatting.RED));
                    } else if (!state.canAfford(page, affixId)) {
                        showToast(Component.translatable(KEY_TOO_EXPENSIVE).withStyle(ChatFormatting.RED));
                    } else if (state.isFull()) {
                        showToast(Component.translatable(KEY_SLOT_EMPTY).withStyle(ChatFormatting.RED));
                    } else if (row.isVariant()) {
                        state.add(row.variant());
                    } else {
                        state.add(page.styleId(), affixId);
                    }
                }
                return true;
            }
            libY += ROW_HEIGHT;
        }

        int previewX = rightX() + rightWidth() - PREVIEW_SIZE;
        int previewY = listBottom() - PREVIEW_SIZE;
        if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, previewX, previewY, PREVIEW_SIZE, PREVIEW_SIZE)
                && selected != null
                && CombatStylesConfig.get().animationPreviewEnabled()
                && preview.isAvailable()) {
            openExpandedPreview();
            return true;
        }

        int seqX = rightX();
        int slotWidth = sequenceSlotWidth();
        int seqY = listTop() + BUDGET_HEIGHT;
        int count = Math.min(state.sequence().size(), lastVisibleSequenceSlot());
        for (int i = sequenceScroll; i < count; i++) {
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, seqX, seqY, slotWidth, ROW_HEIGHT)) {
                int removeX = removeButtonX(seqX, slotWidth);
                int affixX = affixButtonX(seqX, slotWidth, i);
                if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, removeX, seqY, BUTTON_HIT_WIDTH, ROW_HEIGHT)) {
                    state.remove(i);
                } else if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, affixX, seqY, BUTTON_HIT_WIDTH, ROW_HEIGHT)) {
                    openAffixPicker(i);
                } else if (button == 1) {
                    openAttackEditor(i);
                } else if (button == 2) {
                    copyStyle(i);
                } else {
                    dragFrom = i;
                    dragHoverIndex = i;
                    dragging = true;
                    List<StylePage> pages = state.sequence().resolve();
                    if (i < pages.size() && pages.get(i) != null) {
                        selected = pages.get(i);
                        selectedVariant = state.sequence().entry(i);
                        preview.select(selected.animation());
                    }
                }
                return true;
            }
            int soundY = seqY + ROW_HEIGHT;
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, seqX, soundY, slotWidth, SOUND_HEIGHT)) {
                openSoundPicker(i);
                return true;
            }
            seqY += SLOT_HEIGHT + 2;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (categoryScrollbar.isDragging()) {
            categoryScroll = categoryScrollbar.drag(mouseY, categoryScroll);
            return true;
        }
        if (libraryScrollbar.isDragging()) {
            libraryScroll = libraryScrollbar.drag(mouseY, libraryScroll);
            return true;
        }
        if (sequenceScrollbar.isDragging()) {
            sequenceScroll = sequenceScrollbar.drag(mouseY, sequenceScroll);
            return true;
        }
        if (dragging) {
            int size = state.sequence().size();
            int relative = sequenceScroll + (int) ((mouseY - (listTop() + BUDGET_HEIGHT)) / (SLOT_HEIGHT + 2));
            dragHoverIndex = Math.clamp(relative, 0, Math.max(0, size - 1));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        categoryScrollbar.endDrag();
        libraryScrollbar.endDrag();
        sequenceScrollbar.endDrag();
        if (dragging) {
            if (dragFrom >= 0 && dragHoverIndex >= 0 && dragFrom != dragHoverIndex) {
                state.move(dragFrom, dragHoverIndex);
            }
            dragging = false;
            dragFrom = -1;
            dragHoverIndex = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = (int) Math.signum(scrollY);
        if (categoriesVisible() && mouseX < railX()) {
            categoryScroll = Math.clamp(categoryScroll - step, 0, maxCategoryScroll());
            return true;
        }
        if (mouseX < rightX() - GAP) {
            libraryScroll = Math.clamp(libraryScroll - step, 0, maxLibraryScroll());
            return true;
        }
        sequenceScroll = Math.max(0, sequenceScroll - step);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused() && keyCode != KEY_ESCAPE) {
            return searchBox.keyPressed(keyCode, scanCode, modifiers)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == KEY_F && Screen.hasControlDown()) {
            setFocused(searchBox);
            searchBox.setFocused(true);
            return true;
        }
        if (keyCode == KEY_S && Screen.hasControlDown()) {
            apply();
            return true;
        }
        if (keyCode == KEY_DELETE || keyCode == KEY_BACKSPACE) {
            int last = state.sequence().size() - 1;
            if (last >= 0) {
                state.remove(last);
                return true;
            }
        }
        if (keyCode == KEY_TAB) {
            state.toggleSlot();
            rebuildLibrary();
            return true;
        }
        if (keyCode == KEY_R && Screen.hasControlDown()) {
            state.setSequence(StyleSequence.EMPTY);
            state.refresh();
            rebuildLibrary();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (searchBox.isFocused()) {
            return searchBox.charTyped(character, modifiers);
        }
        return super.charTyped(character, modifiers);
    }
}
