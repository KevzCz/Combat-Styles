package net.pixeldreamstudios.combat_styles.client;

import com.google.gson.GsonBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.LibraryRef;
import net.pixeldreamstudios.combat_styles.style.InnateAffixes;
import net.pixeldreamstudios.combat_styles.equipment.ActiveIn;
import net.pixeldreamstudios.combat_styles.equipment.SwingHands;
import net.pixeldreamstudios.combat_styles.equipment.AttributeBonus;
import net.pixeldreamstudios.combat_styles.equipment.EffectCooldown;
import net.pixeldreamstudios.combat_styles.equipment.EffectGroup;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentEffect;
import net.pixeldreamstudios.combat_styles.equipment.PoseTuning;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;
import net.pixeldreamstudios.combat_styles.equipment.UseSpec;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentTrigger;
import net.pixeldreamstudios.combat_styles.equipment.TriggeredEffect;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleText;
import net.pixeldreamstudios.combat_styles.style.WeaponCategories;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class EquipmentDetailScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 564;
    private static final int PANEL_HEIGHT = 290;
    private static final int HEADER = 122;
    private static final int TARGET_BLOCK = 56;
    private static final int TARGETS_Y = 78;
    private static final int FOOTER = 30;
    private static final int ROW_HEIGHT = 20;
    private static final int GAP = 4;
    private static final int FOOTER_TEXT_PAD = 4;
    private static final int TITLE_Y = 10;
    private static final int ID_Y = 24;
    private static final int ID_HEIGHT = 14;
    private static final int META_Y = 44;
    private static final int TEXT_INSET = 6;
    private static final int REMOVE_WIDTH = 14;
    private static final int ID_MAX = 96;
    private static final int ID_LABEL_WIDTH = 60;
    private static final int SOURCE_WIDTH = 52;
    private static final String TAG_PREFIX = "#";
    private static final String CATEGORY_PREFIX = "@";
    private static final String EXCLUDE_PREFIX = "!";
    private static final int TARGET_ROWS = 3;
    private static final int TARGET_ROW_HEIGHT = 12;
    private static final int TARGET_BUTTON_WIDTH = 76;
    private static final int TARGET_BUTTON_HEIGHT = 12;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int NEST_INDENT = 10;
    private static final int NEST_BAR_X = 4;

    private final boolean perStack;
    private static final String KEY_GIVE = "screen.combat_styles.equipment_give";
    private static final String NEW_EFFECT = "effect";
    private static final String NEW_GROUP = "group";

    private static final String KEY_TITLE = "screen.combat_styles.equipment_detail";
    private static final String KEY_ID = "screen.combat_styles.equipment_id";
    private static final String KEY_RARITY = "screen.combat_styles.equipment_rarity";
    private static final String KEY_INNATE = "screen.combat_styles.equipment_innate";
    private static final String KEY_ADD = "screen.combat_styles.equipment_add_effect";
    private static final String KEY_PICK = "screen.combat_styles.equipment_pick_item";
    private static final String KEY_EXCLUDE = "screen.combat_styles.equipment_exclude";
    private static final String KEY_ADD_GROUP = "screen.combat_styles.equipment_add_group";
    private static final String KEY_GROUP_LABEL = "screen.combat_styles.group_label";
    private static final String KEY_EMPTY = "screen.combat_styles.equipment_no_effects";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_EXPORT = "screen.combat_styles.equipment_export";
    private static final String KEY_EXPORTED = "screen.combat_styles.equipment_exported";
    private static final String KEY_UNKNOWN_ITEM = "screen.combat_styles.equipment_unknown_item";
    private static final String KEY_REMOVE = "screen.combat_styles.slash_remove";
    private static final String KEY_NONE = "screen.combat_styles.value_none";
    private static final String KEY_RESET = "screen.combat_styles.equipment_reset";
    private static final String KEY_SOURCE_CONFIG = "screen.combat_styles.equipment_source_config";
    private static final String KEY_SOURCE_DATA = "screen.combat_styles.equipment_source_data";
    private static final String KEY_POSE = "screen.combat_styles.equipment_pose";
    private static final String KEY_CATEGORY = "screen.combat_styles.equipment_weapon_category";
    private static final String KEY_ACTIVE_IN = "screen.combat_styles.equipment_active_in";
    private static final String KEY_ACTIVE_IN_PREFIX = "screen.combat_styles.equipment_active_in.";
    private static final String KEY_ACTIVE_IN_HINT = "hint.combat_styles.active_in";
    private static final String KEY_SWINGS = "screen.combat_styles.equipment_swings";
    private static final String KEY_SWINGS_PREFIX = "screen.combat_styles.equipment_swings.";
    private static final String KEY_SWINGS_HINT = "hint.combat_styles.swings";
    private static final String KEY_SCOPE_HIDDEN = "screen.combat_styles.equipment_scope_hidden";
    private static final String KEY_RARITY_HINT = "hint.combat_styles.equipment_rarity";
    private static final int RIGHT_BUTTON = 1;
    private static final int META_COLUMNS = 4;
    private static final String KEY_CATEGORY_PICKER = "screen.combat_styles.weapon_category_picker";
    private static final String KEY_USE = "screen.combat_styles.equipment_use";
    private static final String KEY_PASSIVES = "screen.combat_styles.equipment_passives";
    private static final String KEY_TARGETS = "screen.combat_styles.equipment_targets";
    private static final String KEY_APPLIES_ID = "screen.combat_styles.equipment_applies_id";
    private static final String KEY_APPLIES_BOUND = "screen.combat_styles.equipment_applies_bound";
    private static final String KEY_PROJECTILES = "screen.combat_styles.equipment_projectiles";
    private static final String KEY_LIBRARY_NAME = "screen.combat_styles.equipment_library_name";
    private static final String KEY_SUMMON_PROFILES = "screen.combat_styles.equipment_summon_profiles";
    private static final String KEY_SUMMON_PROFILE_HINT = "screen.combat_styles.equipment_summon_profile.hint";
    private static final String KEY_ENTITIES = "screen.combat_styles.equipment_entities";
    private static final String KEY_REMOVE_OVERRIDE = "screen.combat_styles.equipment_remove_override";
    private static final String KEY_PROJECTILE_HINT = "screen.combat_styles.equipment_projectile.hint";
    private static final String NAME_SEPARATOR = "_";
    private static final String WHITESPACE = "\\s+";

    private final Consumer<EquipmentDefinition> onSave;
    private final String definitionId;
    private final Map<String, TriggeredEffect> effects = new LinkedHashMap<>();
    private final List<InnateAffixes.Slot> innate = new ArrayList<>();
    private final Map<String, EffectGroup> groups = new LinkedHashMap<>();
    private final Map<String, AttributeBonus> passiveAttributes = new LinkedHashMap<>();
    private final Map<String, AffixEffects.Projectile> projectiles = new LinkedHashMap<>();
    private final Map<String, SummonProfile> summonProfiles = new LinkedHashMap<>();
    private final List<DropdownMenu> menus = new ArrayList<>();
    private Runnable onClear;
    private static final Codec<Map<String, AttributeBonus>> ATTRIBUTE_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, AttributeBonus.EXPANDED);

    private EditBox idBox;
    private String editedId;
    private int targetScroll;
    private boolean draggingTargets;
    private Optional<StyleRarity> rarity;
    private Optional<String> pose;
    private Optional<String> offHandPose;
    private PoseTuning poseTuning;
    private UseSpec use = UseSpec.NONE;
    private Optional<String> weaponCategory;
    private ActiveIn activeIn;
    private SwingHands swings;
    private boolean hideScopeTooltip;
    private boolean hideRarity;
    private final List<String> matchItems = new ArrayList<>();
    private final List<String> matchTags = new ArrayList<>();
    private final List<String> matchCategories = new ArrayList<>();
    private final List<String> excludeItems = new ArrayList<>();
    private final List<String> excludeTags = new ArrayList<>();
    private final List<String> excludeCategories = new ArrayList<>();
    private final TooltipPreviewDock dock = new TooltipPreviewDock();
    private List<Component> tooltip;
    private boolean exported;

    public EquipmentDetailScreen(Screen parent, EquipmentDefinition definition,
                                 Consumer<EquipmentDefinition> onSave) {
        this(parent, definition, onSave, false);
    }

    public EquipmentDetailScreen(Screen parent, EquipmentDefinition definition,
                                 Consumer<EquipmentDefinition> onSave, boolean perStack) {
        super(Component.translatable(KEY_TITLE), parent);
        this.perStack = perStack;
        this.onSave = onSave;
        this.definitionId = definition.id();
        this.effects.putAll(definition.effects());
        this.innate.addAll(definition.innateAffixes());
        this.groups.putAll(definition.groups());
        this.passiveAttributes.putAll(definition.attributes());
        this.projectiles.putAll(definition.projectiles());
        this.summonProfiles.putAll(definition.summonProfiles());
        this.rarity = definition.rarity();
        this.pose = definition.pose();
        this.offHandPose = definition.offHandPose();
        this.poseTuning = definition.poseTuning();
        this.use = definition.use();
        this.weaponCategory = definition.weaponCategory();
        this.activeIn = definition.activeIn();
        this.swings = definition.swings();
        this.hideScopeTooltip = definition.hideScopeTooltip();
        this.hideRarity = definition.hideRarity();
        this.matchItems.addAll(definition.items());
        this.matchTags.addAll(definition.itemTags());
        this.matchCategories.addAll(definition.weaponCategories());
        this.excludeItems.addAll(definition.excludedItems());
        this.excludeTags.addAll(definition.excludedTags());
        this.excludeCategories.addAll(definition.excludedCategories());
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
        return rows().size();
    }

    private boolean canGive() {
        return minecraft != null && minecraft.player != null
                && minecraft.player.getAbilities().instabuild;
    }

    private int targetListWidth() {
        return contentWidth() - TARGET_BUTTON_WIDTH - GAP;
    }

    private List<String> targetRows() {
        List<String> rows = new ArrayList<>();
        rows.addAll(matchItems);
        for (String tag : matchTags) {
            rows.add(TAG_PREFIX + tag);
        }
        for (String category : matchCategories) {
            rows.add(CATEGORY_PREFIX + category);
        }
        for (String item : excludeItems) {
            rows.add(EXCLUDE_PREFIX + item);
        }
        for (String tag : excludeTags) {
            rows.add(EXCLUDE_PREFIX + TAG_PREFIX + tag);
        }
        for (String category : excludeCategories) {
            rows.add(EXCLUDE_PREFIX + CATEGORY_PREFIX + category);
        }
        return rows;
    }

    private void removeTarget(String row) {
        String body = row.startsWith(EXCLUDE_PREFIX)
                ? row.substring(EXCLUDE_PREFIX.length())
                : row;
        boolean excluding = row.startsWith(EXCLUDE_PREFIX);
        if (body.startsWith(TAG_PREFIX)) {
            (excluding ? excludeTags : matchTags).remove(body.substring(TAG_PREFIX.length()));
        } else if (body.startsWith(CATEGORY_PREFIX)) {
            (excluding ? excludeCategories : matchCategories)
                    .remove(body.substring(CATEGORY_PREFIX.length()));
        } else {
            (excluding ? excludeItems : matchItems).remove(body);
        }
        targetScroll = Math.clamp(targetScroll, 0,
                Math.max(0, targetRows().size() - TARGET_ROWS));
    }

    private String currentId() {
        String typed = idBox == null ? editedId : idBox.getValue();
        if (typed != null && !typed.isBlank()) {
            return typed.trim();
        }
        return definitionId.isBlank() ? primaryTarget() : definitionId;
    }

    private String primaryTarget() {
        if (!matchItems.isEmpty()) {
            return matchItems.getFirst();
        }
        if (!matchTags.isEmpty()) {
            return matchTags.getFirst();
        }
        if (!matchCategories.isEmpty()) {
            return matchCategories.getFirst();
        }
        return boundItemId();
    }

    private String boundItemId() {
        String id = editedId == null ? definitionId : editedId;
        if (id == null || id.isBlank()) {
            return "";
        }
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        return parsed != null && BuiltInRegistries.ITEM.containsKey(parsed) ? id : "";
    }

    @Override
    protected void layout() {
        EffectChoices.offerLibraries(projectiles::keySet, summonProfiles::keySet);
        menus.clear();
        dock.place(left, top, PANEL_WIDTH, width, height);
        refreshPreview();
        idBox = new FieldBox(font, contentLeft() + ID_LABEL_WIDTH, top + ID_Y,
                contentWidth() - ID_LABEL_WIDTH - SOURCE_WIDTH, ID_HEIGHT,
                Component.empty());
        idBox.setMaxLength(ID_MAX);
        idBox.setValue(editedId == null ? definitionId : editedId);
        idBox.setResponder(value -> editedId = value);
        idBox.setEditable(!perStack);
        addRenderableWidget(idBox);

        if (!perStack) {
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_PICK),
                        button -> minecraft.setScreen(new MatchTargetScreen(this,
                                matchItems, matchTags, matchCategories)))
                .bounds(contentRight() - TARGET_BUTTON_WIDTH, top + TARGETS_Y,
                        TARGET_BUTTON_WIDTH, TARGET_BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_EXCLUDE),
                        button -> minecraft.setScreen(new MatchTargetScreen(this,
                                excludeItems, excludeTags, excludeCategories)))
                .bounds(contentRight() - TARGET_BUTTON_WIDTH,
                        top + TARGETS_Y + TARGET_BUTTON_HEIGHT + 2,
                        TARGET_BUTTON_WIDTH, TARGET_BUTTON_HEIGHT).build());
        }
        if (!perStack && canGive()) {
            addRenderableWidget(PanelButton.of(Component.translatable(KEY_GIVE),
                            button -> giveTarget())
                    .bounds(contentRight() - TARGET_BUTTON_WIDTH,
                            top + TARGETS_Y + (TARGET_BUTTON_HEIGHT + 2) * 2,
                            TARGET_BUTTON_WIDTH, TARGET_BUTTON_HEIGHT).build());
        }

        boolean resettable = !perStack && EquipmentRegistry.isOverridden(definitionId);
        boolean removable = perStack && onClear != null;
        List<Button> footer = new ArrayList<>();
        Set<Button> anchors = new LinkedHashSet<>();

        footer.add(footerButton(Component.translatable(KEY_ADD), button -> addEffect()));
        footer.add(footerButton(Component.translatable(KEY_ADD_GROUP), button -> addGroup()));
        if (resettable) {
            footer.add(footerButton(Component.translatable(KEY_RESET), button -> {
                if (!ConfigEdits.permitted()) {
                    ConfigEdits.tell(ConfigEdits.operatorOnly());
                    return;
                }
                ConfigEdits.remove(EditConfigPayload.Kind.EQUIPMENT_CLEAR, definitionId);
                EquipmentRegistry.restoreLocal(definitionId);
                minecraft.setScreen(parent);
            }));
        }
        if (removable) {
            footer.add(footerButton(Component.translatable(KEY_REMOVE_OVERRIDE), button -> onClear.run()));
        }
        footer.add(footerButton(Component.translatable(KEY_PASSIVES), button -> openPassives()));
        Button entities = footerButton(Component.translatable(KEY_ENTITIES),
                button -> openLibrary(projectileLibrary()));
        footer.add(entities);
        anchors.add(entities);
        menus.add(new DropdownMenu(entities, List.of(
                DropdownMenu.Option.always(Component.translatable(KEY_PROJECTILES),
                        () -> openLibrary(projectileLibrary())),
                DropdownMenu.Option.always(Component.translatable(KEY_SUMMON_PROFILES),
                        () -> openLibrary(summonLibrary())))));
        Button looks = footerButton(Component.translatable(KEY_POSE), button -> openPoses());
        footer.add(looks);
        anchors.add(looks);
        menus.add(new DropdownMenu(looks, List.of(
                DropdownMenu.Option.always(Component.translatable(KEY_POSE), this::openPoses),
                DropdownMenu.Option.always(Component.translatable(KEY_CATEGORY),
                        this::openWeaponCategory))));
        footer.add(footerButton(useLabel(), button -> openUse()));
        footer.add(footerButton(Component.translatable(KEY_EXPORT), button -> export()));
        footer.add(footerButton(Component.translatable(KEY_CLOSE), button -> minecraft.setScreen(parent)));
        footer.add(footerButton(Component.translatable(KEY_APPLY), button -> save()));
        layoutFooter(footer, anchors);
    }

    private Button footerButton(Component label, Button.OnPress action) {
        return addRenderableWidget(PanelButton.of(label, action)
                .bounds(0, top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING,
                        0, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private void layoutFooter(List<Button> footer, Set<Button> anchors) {
        int span = PANEL_WIDTH - ScreenTheme.PADDING * 2 - GAP * (footer.size() - 1);
        int[] widths = new int[footer.size()];
        int natural = 0;
        for (int index = 0; index < footer.size(); index++) {
            Button button = footer.get(index);
            widths[index] = font.width(button.getMessage()) + FOOTER_TEXT_PAD * 2
                    + (anchors.contains(button) ? DropdownMenu.caretWidth(font) : 0);
            natural += widths[index];
        }
        int spare = span - natural;
        int x = left + ScreenTheme.PADDING;
        for (int index = 0; index < footer.size(); index++) {
            int share = spare >= 0
                    ? spare / footer.size() + (index < spare % footer.size() ? 1 : 0)
                    : (int) ((long) spare * widths[index] / natural);
            int width = widths[index] + share;
            if (index == footer.size() - 1) {
                width = left + ScreenTheme.PADDING + PANEL_WIDTH - ScreenTheme.PADDING * 2 - x;
            }
            Button button = footer.get(index);
            button.setX(x);
            button.setWidth(width);
            x += width + GAP;
        }
    }

    public EquipmentDetailScreen clearingWith(Runnable clear) {
        this.onClear = clear;
        return this;
    }

    private boolean matchesItem(String id) {
        if (id.isBlank() || !matchItems.contains(id)) {
            return true;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        return parsed != null && BuiltInRegistries.ITEM.containsKey(parsed);
    }

    private void renderTargets(GuiGraphics graphics, int mouseX, int mouseY) {
        if (perStack) {
            return;
        }
        List<String> rows = targetRows();
        int listY = top + TARGETS_Y;
        ScreenDraw.sunken(graphics, contentLeft(), listY - 1,
                targetListWidth(),
                TARGET_ROWS * TARGET_ROW_HEIGHT + 2);
        if (rows.isEmpty()) {
            String bound = boundItemId();
            Component label = bound.isEmpty()
                    ? Component.translatable(KEY_APPLIES_ID)
                    : Component.translatable(KEY_APPLIES_BOUND, bound);
            graphics.drawString(font,
                    ScreenDraw.trim(font, label.getString(), targetListWidth() - TEXT_INSET * 2),
                    contentLeft() + TEXT_INSET, listY + 2,
                    bound.isEmpty() ? ScreenTheme.DIM : ScreenTheme.GOOD, false);
            return;
        }
        int width = targetListWidth();
        if (rows.size() > TARGET_ROWS) {
            ScreenDraw.scrollbar(graphics, contentLeft() + width - SCROLLBAR_WIDTH - 1,
                    listY, TARGET_ROWS * TARGET_ROW_HEIGHT, TARGET_ROWS,
                    rows.size(), targetScroll);
        }
        int last = Math.min(rows.size(), targetScroll + TARGET_ROWS);
        for (int index = targetScroll; index < last; index++) {
            String row = rows.get(index);
            int rowY = listY + (index - targetScroll) * TARGET_ROW_HEIGHT;
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, contentLeft(), rowY,
                    width, TARGET_ROW_HEIGHT);
            if (hovered) {
                graphics.fill(contentLeft() + 1, rowY, contentLeft() + width - 1,
                        rowY + TARGET_ROW_HEIGHT, ScreenTheme.HOVER);
            }
            int colour = row.startsWith(EXCLUDE_PREFIX) ? ScreenTheme.BAD : ScreenTheme.TEXT;
            graphics.drawString(font,
                    ScreenDraw.trim(font, targetLabel(row), width - REMOVE_WIDTH - 8),
                    contentLeft() + TEXT_INSET, rowY + 2,
                    hovered ? ScreenTheme.ACCENT : colour, false);
            graphics.drawString(font, Component.translatable(KEY_REMOVE),
                    contentLeft() + width - REMOVE_WIDTH, rowY + 2,
                    hovered ? ScreenTheme.BAD : ScreenTheme.DIM, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int width = targetListWidth();
        int listY = top + TARGETS_Y;
        if (ScreenDraw.isWithin(mouseX, mouseY, contentLeft(), listY,
                width, TARGET_ROWS * TARGET_ROW_HEIGHT)) {
            targetScroll = Math.clamp(targetScroll - (int) Math.signum(scrollY), 0,
                    Math.max(0, targetRows().size() - TARGET_ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int targetScrollbarX() {
        return contentLeft() + targetListWidth() - SCROLLBAR_WIDTH - 1;
    }

    private int targetListHeight() {
        return TARGET_ROWS * TARGET_ROW_HEIGHT;
    }

    private boolean beginTargetScrollDrag(double mouseX, double mouseY) {
        if (perStack || !ScreenDraw.onScrollbar(mouseX, mouseY, targetScrollbarX(),
                top + TARGETS_Y, targetListHeight(), TARGET_ROWS, targetRows().size())) {
            return false;
        }
        draggingTargets = true;
        dragTargets(mouseY);
        return true;
    }

    private void dragTargets(double mouseY) {
        targetScroll = ScreenDraw.scrollFromDrag(mouseY, top + TARGETS_Y,
                targetListHeight(), TARGET_ROWS, targetRows().size());
    }

    private boolean clickedTarget(double mouseX, double mouseY) {
        List<String> rows = targetRows();
        if (rows.isEmpty()) {
            return false;
        }
        int listY = top + TARGETS_Y;
        int width = targetListWidth();
        int clickable = rows.size() > TARGET_ROWS ? width - SCROLLBAR_WIDTH - 2 : width;
        int last = Math.min(rows.size(), targetScroll + TARGET_ROWS);
        for (int index = targetScroll; index < last; index++) {
            int rowY = listY + (index - targetScroll) * TARGET_ROW_HEIGHT;
            if (ScreenDraw.isWithin(mouseX, mouseY, contentLeft(), rowY,
                    clickable, TARGET_ROW_HEIGHT)) {
                removeTarget(rows.get(index));
                return true;
            }
        }
        return false;
    }

    private String targetLabel(String row) {
        if (row.startsWith(EXCLUDE_PREFIX)) {
            return EXCLUDE_PREFIX + " " + targetLabel(row.substring(EXCLUDE_PREFIX.length()));
        }
        if (row.startsWith(TAG_PREFIX)) {
            return TAG_PREFIX + shortName(row.substring(TAG_PREFIX.length()));
        }
        if (row.startsWith(CATEGORY_PREFIX)) {
            return StyleText.prettify(shortName(row.substring(CATEGORY_PREFIX.length())));
        }
        return StyleText.prettify(shortName(row)) + "  " + row;
    }

    private void giveTarget() {
        minecraft.setScreen(new GiveItemScreen(this, List.copyOf(matchItems),
                List.copyOf(matchTags), List.copyOf(matchCategories)));
    }

    private static String shortName(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    private void openPassives() {
        minecraft.setScreen(new EffectTreeScreen(this,
                Component.translatable(KEY_PASSIVES),
                ATTRIBUTE_MAP_CODEC, Map.copyOf(passiveAttributes), saved -> {
                    passiveAttributes.clear();
                    passiveAttributes.putAll(saved.orElse(Map.of()));
                }).withSchema(EffectSchema.ROOT_ATTRIBUTE_MAP));
    }

    private record Library<T>(String titleKey, String hintKey, Map<String, T> entries, T sample,
                              Codec<T> codec, String schema) {
    }

    private Library<AffixEffects.Projectile> projectileLibrary() {
        return new Library<>(KEY_PROJECTILES, KEY_PROJECTILE_HINT, projectiles,
                EffectSchema.SAMPLE_PROJECTILE, AffixEffects.Projectile.CODEC,
                EffectSchema.ROOT_PROJECTILE);
    }

    private Library<SummonProfile> summonLibrary() {
        return new Library<>(KEY_SUMMON_PROFILES, KEY_SUMMON_PROFILE_HINT, summonProfiles,
                SummonProfile.EMPTY, SummonProfile.CODEC, EffectSchema.ROOT_SUMMON_PROFILE);
    }

    private <T> void openLibrary(Library<T> library) {
        minecraft.setScreen(new EquipmentListScreen(this,
                Component.translatable(library.titleKey()),
                library.entries().keySet().stream()
                        .map(name -> new EquipmentListScreen.Entry(name,
                                Component.literal(LibraryRef.refTo(name))))
                        .toList(),
                () -> minecraft.setScreen(new ChoicePickerScreen(this,
                        Component.translatable(KEY_LIBRARY_NAME),
                        List.copyOf(library.entries().keySet()), null, picked -> {
                            library.entries().putIfAbsent(picked, library.sample());
                            openLibraryEntry(library, picked);
                        }).creatingNew(EquipmentDetailScreen::libraryName).handingOffOnPick()),
                library.entries()::remove,
                name -> openLibraryEntry(library, name),
                name -> List.of(Component.literal(LibraryRef.refTo(name)),
                        Component.translatable(library.hintKey())
                                .withStyle(ChatFormatting.GRAY))));
    }

    private <T> void openLibraryEntry(Library<T> library, String name) {
        T current = library.entries().get(name);
        if (current == null) {
            return;
        }
        minecraft.setScreen(new EffectTreeScreen(this,
                Component.literal(LibraryRef.refTo(name)),
                library.codec(), current,
                saved -> library.entries().put(name, saved.orElse(current)))
                .withSchema(library.schema()));
    }

    private static Optional<String> libraryName(String typed) {
        String name = LibraryRef.nameOf(typed == null ? "" : typed.trim())
                .toLowerCase(Locale.ROOT).replaceAll(WHITESPACE, NAME_SEPARATOR);
        return name.isBlank() ? Optional.empty() : Optional.of(name);
    }
    private Component useLabel() {
        return Component.translatable(KEY_USE);
    }

    private void openUse() {
        minecraft.setScreen(new UseEditorScreen(this, use, chosen -> use = chosen));
    }

    private void openPoses() {
        minecraft.setScreen(new PoseEditorScreen(this, pose, offHandPose, poseTuning,
                (chosenPose, chosenOffHand, chosenTuning) -> {
                    pose = chosenPose;
                    offHandPose = chosenOffHand;
                    poseTuning = chosenTuning;
                }));
    }

    private void openWeaponCategory() {
        minecraft.setScreen(new ChoicePickerScreen(this, Component.translatable(KEY_CATEGORY_PICKER),
                WeaponCategories.allNames(), weaponCategory.orElse(null),
                chosen -> weaponCategory = WeaponCategories.normalize(chosen))
                .creatingNew(WeaponCategories::normalize));
    }

    private EquipmentDefinition currentDefinition(String id) {
        return new EquipmentDefinition(id, Map.copyOf(effects),
                Map.copyOf(passiveAttributes), rarity, List.copyOf(innate), Map.copyOf(groups),
                pose, offHandPose, poseTuning, List.copyOf(matchItems),
                List.copyOf(matchTags), List.copyOf(matchCategories),
                List.copyOf(excludeItems), List.copyOf(excludeTags),
                List.copyOf(excludeCategories), use, weaponCategory, activeIn, swings, hideScopeTooltip,
                hideRarity).withProjectiles(Map.copyOf(projectiles))
                .withSummonProfiles(Map.copyOf(summonProfiles));
    }

    private void export() {
        String id = currentId();
        if (id.isBlank()) {
            return;
        }
        EquipmentDefinition definition = currentDefinition(id);
        EquipmentDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .result()
                .ifPresent(json -> {
                    minecraft.keyboardHandler.setClipboard(
                            new GsonBuilder().setPrettyPrinting().create().toJson(json));
                    exported = true;
                });
    }

    private void save() {
        String id = currentId();
        if (id.isBlank()) {
            return;
        }
        if (!definitionId.isBlank() && !definitionId.equals(id)) {
            if (EquipmentRegistry.isOverridden(definitionId) && ConfigEdits.permitted()) {
                ConfigEdits.remove(EditConfigPayload.Kind.EQUIPMENT_CLEAR, definitionId);
                EquipmentRegistry.forgetLocal(definitionId);
            }
        }
        onSave.accept(currentDefinition(id));
        minecraft.setScreen(parent);
    }

    private void addEffect() {
        String name = NEW_EFFECT + (effects.size() + 1);
        effects.put(name, new TriggeredEffect(EquipmentTrigger.ALWAYS,
                EquipmentEffect.EMPTY, EffectCooldown.NONE, false));
        openEffect(name);
    }

    private void addGroup() {
        int index = groups.size() + 1;
        String name = NEW_GROUP + index;
        while (groups.containsKey(name)) {
            name = NEW_GROUP + (++index);
        }
        groups.put(name, EffectGroup.EMPTY);
        openGroup(name);
    }

    private void dropFromGroups(String effect) {
        for (Map.Entry<String, EffectGroup> named : new ArrayList<>(groups.entrySet())) {
            EffectGroup group = named.getValue();
            if (!group.holds(effect)) {
                continue;
            }
            List<EffectGroup.Entry> kept = new ArrayList<>();
            for (EffectGroup.Entry entry : group.entries()) {
                if (!entry.effect().equals(effect)) {
                    kept.add(entry);
                }
            }
            groups.put(named.getKey(), group.withEntries(kept));
        }
    }

    private List<String> freeEffects(String owner) {
        Set<String> claimed = new LinkedHashSet<>();
        for (Map.Entry<String, EffectGroup> named : groups.entrySet()) {
            if (named.getKey().equals(owner)) {
                continue;
            }
            for (EffectGroup.Entry entry : named.getValue().entries()) {
                claimed.add(entry.effect());
            }
        }
        List<String> free = new ArrayList<>();
        for (String name : names()) {
            if (!claimed.contains(name)) {
                free.add(name);
            }
        }
        return free;
    }

    private void openGroup(String name) {
        EffectGroup current = groups.get(name);
        if (current == null) {
            return;
        }
        minecraft.setScreen(new EffectGroupScreen(this, name, current, freeEffects(name),
                (key, edited) -> {
                    if (!key.equals(name)) {
                        groups.remove(name);
                    }
                    groups.put(key, edited);
                    pushEntryChances(edited);
                }));
    }

    private void pushEntryChances(EffectGroup group) {
        for (EffectGroup.Entry entry : group.entries()) {
            TriggeredEffect effect = effects.get(entry.effect());
            if (effect == null) {
                continue;
            }
            Optional<Float> chance = entry.chance() < EffectGroup.ALWAYS
                    ? Optional.of(entry.chance())
                    : Optional.empty();
            effects.put(entry.effect(), effect.withTrigger(effect.trigger().withChance(chance)));
        }
    }

    private void pullEntryChance(String effectKey, TriggeredEffect effect) {
        if (effect.trigger().chance().isEmpty()) {
            return;
        }
        float chance = effect.trigger().chance().get();
        for (Map.Entry<String, EffectGroup> named : groups.entrySet()) {
            EffectGroup group = named.getValue();
            EffectGroup.Entry entry = group.entryFor(effectKey);
            if (entry == null || entry.chance() == chance) {
                continue;
            }
            List<EffectGroup.Entry> replaced = new ArrayList<>(group.entries());
            replaced.set(replaced.indexOf(entry), new EffectGroup.Entry(effectKey, chance));
            named.setValue(group.withEntries(replaced));
        }
    }

    private void openEffect(String name) {
        TriggeredEffect current = effects.get(name);
        if (current == null) {
            return;
        }
        minecraft.setScreen(new EffectEditorScreen(this, name, current,
                () -> currentDefinition(currentId()), (key, edited) -> {
            if (!key.equals(name)) {
                effects.remove(name);
            }
            effects.put(key, edited);
            pullEntryChance(key, edited);
        }));
    }

    private List<String> names() {
        return new ArrayList<>(effects.keySet());
    }

    private record Row(String key, boolean group, boolean nested) {
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        Set<String> claimed = new LinkedHashSet<>();
        for (Map.Entry<String, EffectGroup> named : groups.entrySet()) {
            rows.add(new Row(named.getKey(), true, false));
            for (EffectGroup.Entry entry : named.getValue().entries()) {
                String effect = entry.effect();
                if (effect.isBlank() || !effects.containsKey(effect)
                        || !claimed.add(effect)) {
                    continue;
                }
                rows.add(new Row(effect, false, true));
            }
        }
        for (String name : names()) {
            if (!claimed.contains(name)) {
                rows.add(new Row(name, false, false));
            }
        }
        return rows;
    }

    private Row rowAtIndex(int index) {
        List<Row> rows = rows();
        return index >= 0 && index < rows.size() ? rows.get(index) : null;
    }

    private boolean isGroupRow(int index) {
        Row row = rowAtIndex(index);
        return row != null && row.group();
    }

    private String rowKey(int index) {
        Row row = rowAtIndex(index);
        return row == null ? null : row.key();
    }

    @Override
    protected int listTop() {
        return top + (perStack ? HEADER - TARGET_BLOCK : HEADER);
    }

    @Override
    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER;
    }

    private int removeX() {
        return contentRight() - REMOVE_WIDTH;
    }

    private int metaColumn() {
        return contentWidth() / META_COLUMNS;
    }

    private int rarityWidth() {
        return metaColumn() - GAP;
    }

    private int activeInX() {
        return contentLeft() + metaColumn();
    }

    private int activeInWidth() {
        return metaColumn() - GAP;
    }

    private int swingsX() {
        return activeInX() + metaColumn();
    }

    private int innateX() {
        return contentLeft() + metaColumn() * (META_COLUMNS - 1);
    }

    private Component scopeLabel(Component value) {
        return hideScopeTooltip ? Component.translatable(KEY_SCOPE_HIDDEN, value) : value;
    }

    private SwingHands nextSwings() {
        SwingHands[] values = SwingHands.values();
        return values[(swings.ordinal() + 1) % values.length];
    }

    private ActiveIn nextActiveIn() {
        ActiveIn[] values = ActiveIn.values();
        return values[(activeIn.ordinal() + 1) % values.length];
    }

    private boolean isWeapon() {
        ResourceLocation parsed = ResourceLocation.tryParse(primaryTarget());
        if (parsed == null || !BuiltInRegistries.ITEM.containsKey(parsed)) {
            return false;
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(parsed));
        return !stack.isEmpty() && WeaponRegistryAccess.rawAttributesFor(stack) != null;
    }

    private void openInnate() {
        minecraft.setScreen(new InnateAffixScreen(this, List.copyOf(innate), slots -> {
            innate.clear();
            innate.addAll(slots);
        }));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (DropdownMenu menu : menus) {
            if (menu.mouseClicked(font, mouseX, mouseY)) {
                return true;
            }
        }
        if (dock.click(mouseX, mouseY, minecraft)) {
            return true;
        }
        if (beginScrollDrag(mouseX, mouseY)) {
            return true;
        }
        if (beginTargetScrollDrag(mouseX, mouseY)) {
            return true;
        }
        if (clickedTarget(mouseX, mouseY)) {
            return true;
        }
        if (ScreenDraw.isWithin(mouseX, mouseY, left + ScreenTheme.PADDING, top + META_Y,
                rarityWidth(), ROW_HEIGHT)) {
            if (button == RIGHT_BUTTON) {
                hideRarity = !hideRarity;
            } else {
                rarity = nextRarity();
            }
            return true;
        }
        if (button == RIGHT_BUTTON && (ScreenDraw.isWithin(mouseX, mouseY, activeInX(), top + META_Y,
                activeInWidth(), ROW_HEIGHT) || ScreenDraw.isWithin(mouseX, mouseY, swingsX(),
                top + META_Y, activeInWidth(), ROW_HEIGHT))) {
            hideScopeTooltip = !hideScopeTooltip;
            return true;
        }
        if (ScreenDraw.isWithin(mouseX, mouseY, activeInX(), top + META_Y,
                activeInWidth(), ROW_HEIGHT)) {
            activeIn = nextActiveIn();
            return true;
        }
        if (ScreenDraw.isWithin(mouseX, mouseY, swingsX(), top + META_Y,
                activeInWidth(), ROW_HEIGHT)) {
            swings = nextSwings();
            return true;
        }
        if (isWeapon() && ScreenDraw.isWithin(mouseX, mouseY, innateX(), top + META_Y,
                contentRight() - innateX(), ROW_HEIGHT)) {
            openInnate();
            return true;
        }
        int index = rowAt(mouseX, mouseY);
        String key = index < 0 ? null : rowKey(index);
        if (key == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        boolean group = isGroupRow(index);
        if (ScreenDraw.isWithin(mouseX, mouseY, removeX(), rowY(index),
                REMOVE_WIDTH, ROW_HEIGHT)) {
            if (group) {
                groups.remove(key);
            } else {
                effects.remove(key);
                dropFromGroups(key);
            }
            return true;
        }
        if (group) {
            openGroup(key);
        } else {
            openEffect(key);
        }
        return true;
    }

    private Optional<StyleRarity> nextRarity() {
        StyleRarity[] values = StyleRarity.values();
        if (rarity.isEmpty()) {
            return Optional.of(values[0]);
        }
        int next = rarity.get().ordinal() + 1;
        return next >= values.length ? Optional.empty() : Optional.of(values[next]);
    }

    private int totalRows() {
        return rowCount();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        tooltip = null;
        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(font, Component.translatable(KEY_TITLE),
                left + ScreenTheme.PADDING, top + TITLE_Y, ScreenTheme.HEADING, false);
        if (exported) {
            Component note = Component.translatable(KEY_EXPORTED);
            graphics.drawString(font, note,
                    left + PANEL_WIDTH - ScreenTheme.PADDING - font.width(note),
                    top + TITLE_Y, ScreenTheme.ACCENT, false);
        }
        if (!matchesItem(primaryTarget())) {
            Component warning = Component.translatable(KEY_UNKNOWN_ITEM);
            graphics.drawString(font, warning,
                    left + PANEL_WIDTH - ScreenTheme.PADDING - font.width(warning),
                    top + META_Y - 10, ScreenTheme.BAD, false);
        }
        graphics.drawString(font, Component.translatable(KEY_ID),
                left + ScreenTheme.PADDING, top + ID_Y + 3, ScreenTheme.MUTED, false);
        if (!perStack) {
            boolean fromConfig = EquipmentRegistry.isOverridden(definitionId)
                    || !EquipmentRegistry.fromData(definitionId);
            Component source = Component.translatable(
                    fromConfig ? KEY_SOURCE_CONFIG : KEY_SOURCE_DATA);
            graphics.drawString(font, source,
                    contentRight() - font.width(source), top + ID_Y + 3,
                    fromConfig ? ScreenTheme.GOOD : ScreenTheme.DIM, false);

            graphics.drawString(font, Component.translatable(KEY_TARGETS),
                    left + ScreenTheme.PADDING, top + TARGETS_Y - 10,
                    ScreenTheme.MUTED, false);
        }

        renderTargets(graphics, mouseX, mouseY);

        boolean rarityHover = ScreenDraw.isWithin(mouseX, mouseY,
                contentLeft(), top + META_Y, rarityWidth(), ROW_HEIGHT);
        ScreenDraw.rowBackground(graphics, contentLeft(), top + META_Y,
                rarityWidth(), ROW_HEIGHT - 2, 0, rarityHover, false);
        graphics.drawString(font, Component.translatable(KEY_RARITY),
                contentLeft() + TEXT_INSET, top + META_Y + 5,
                ScreenTheme.TEXT, false);
        Component rarityValue = rarity
                .map(value -> (Component) Component.literal(StyleText.prettify(value.getSerializedName()))
                        .withStyle(value.color()))
                .orElse(Component.translatable(KEY_NONE).copy()
                        .withStyle(ChatFormatting.DARK_GRAY));
        Component rarityLabel = hideRarity
                ? Component.translatable(KEY_SCOPE_HIDDEN, rarityValue)
                : rarityValue;
        graphics.drawString(font, rarityLabel,
                contentLeft() + rarityWidth() - TEXT_INSET - font.width(rarityLabel),
                top + META_Y + 5, ScreenTheme.MUTED, false);
        if (rarityHover) {
            tooltip = List.of(Component.translatable(KEY_RARITY_HINT));
        }

        boolean activeHover = ScreenDraw.isWithin(mouseX, mouseY,
                activeInX(), top + META_Y, activeInWidth(), ROW_HEIGHT);
        ScreenDraw.rowBackground(graphics, activeInX(), top + META_Y,
                activeInWidth(), ROW_HEIGHT - 2, 1, activeHover, false);
        graphics.drawString(font, Component.translatable(KEY_ACTIVE_IN),
                activeInX() + TEXT_INSET, top + META_Y + 5, ScreenTheme.TEXT, false);
        Component activeLabel = scopeLabel(Component.translatable(KEY_ACTIVE_IN_PREFIX
                + activeIn.getSerializedName()));
        graphics.drawString(font, activeLabel,
                activeInX() + activeInWidth() - TEXT_INSET - font.width(activeLabel),
                top + META_Y + 5, ScreenTheme.MUTED, false);
        if (activeHover) {
            tooltip = List.of(Component.translatable(KEY_ACTIVE_IN_HINT));
        }

        boolean swingsHover = ScreenDraw.isWithin(mouseX, mouseY,
                swingsX(), top + META_Y, activeInWidth(), ROW_HEIGHT);
        ScreenDraw.rowBackground(graphics, swingsX(), top + META_Y,
                activeInWidth(), ROW_HEIGHT - 2, 1, swingsHover, false);
        graphics.drawString(font, Component.translatable(KEY_SWINGS),
                swingsX() + TEXT_INSET, top + META_Y + 5, ScreenTheme.TEXT, false);
        Component swingsLabel = scopeLabel(Component.translatable(KEY_SWINGS_PREFIX
                + swings.getSerializedName()));
        graphics.drawString(font, swingsLabel,
                swingsX() + activeInWidth() - TEXT_INSET - font.width(swingsLabel),
                top + META_Y + 5, ScreenTheme.MUTED, false);
        if (swingsHover) {
            tooltip = List.of(Component.translatable(KEY_SWINGS_HINT));
        }

        if (isWeapon()) {
            int innateWidth = contentRight() - innateX();
            boolean innateHover = ScreenDraw.isWithin(mouseX, mouseY,
                    innateX(), top + META_Y, innateWidth, ROW_HEIGHT);
            ScreenDraw.rowBackground(graphics, innateX(), top + META_Y,
                    innateWidth, ROW_HEIGHT - 2, 1, innateHover, false);
            graphics.drawString(font, Component.translatable(KEY_INNATE),
                    innateX() + TEXT_INSET, top + META_Y + 5, ScreenTheme.TEXT, false);
            Component innateLabel = innate.isEmpty()
                    ? Component.translatable(KEY_NONE).copy()
                            .withStyle(ChatFormatting.DARK_GRAY)
                    : Component.literal(String.valueOf(innate.size()));
            graphics.drawString(font, innateLabel,
                    contentRight() - TEXT_INSET - font.width(innateLabel),
                    top + META_Y + 5, ScreenTheme.MUTED, false);
        }

        ScreenDraw.sunken(graphics, left + ScreenTheme.PADDING, listTop() - 2,
                PANEL_WIDTH - ScreenTheme.PADDING * 2, visibleRows() * ROW_HEIGHT + 4);

        if (totalRows() == 0) {
            graphics.drawString(font, Component.translatable(KEY_EMPTY),
                    left + ScreenTheme.PADDING + TEXT_INSET, listTop() + TEXT_INSET,
                    ScreenTheme.DIM, false);
            drawFooter(graphics, mouseX, mouseY, partialTick);
            return;
        }

        int hovered = rowAt(mouseX, mouseY);
        int first = scroll;
        int last = Math.min(totalRows(), first + visibleRows());
        List<Row> visible = rows();
        for (int index = first; index < last; index++) {
            Row row = index < visible.size() ? visible.get(index) : null;
            if (row == null) {
                continue;
            }
            String name = row.key();
            boolean group = row.group();
            int y = rowY(index);
            boolean isHovered = index == hovered;
            ScreenDraw.rowBackground(graphics, left + ScreenTheme.PADDING + 1, y,
                    PANEL_WIDTH - ScreenTheme.PADDING * 2 - 2, ROW_HEIGHT - 1,
                    index, isHovered, false);
            int textX = left + ScreenTheme.PADDING + TEXT_INSET
                    + (row.nested() ? NEST_INDENT : 0);
            if (row.nested()) {
                ScreenDraw.accentBar(graphics, left + ScreenTheme.PADDING + NEST_BAR_X,
                        y, ROW_HEIGHT - 1, ScreenTheme.BRANCH);
            }
            graphics.drawString(font,
                    group ? Component.translatable(KEY_GROUP_LABEL, name)
                            : Component.literal(name),
                    textX, y + TEXT_INSET,
                    group ? ScreenTheme.HEADING : ScreenTheme.TEXT, false);
            if (group) {
                EffectGroup held = groups.get(name);
                StringBuilder text = new StringBuilder();
                if (held.gated()) {
                    text.append(held.trigger().describe()).append(" · ");
                }
                text.append(Math.round(held.chance() * 100.0F)).append("% · ")
                        .append(held.entries().size());
                if (held.onCooldown()) {
                    text.append(" · ").append(held.cooldown().ticks()).append('t');
                }
                if (held.windowed()) {
                    text.append(" · ").append(held.duration()).append('t');
                }
                if (held.limited()) {
                    text.append(" · ").append(held.uses()).append('x');
                }
                Component summary = Component.literal(text.toString());
                graphics.drawString(font, summary,
                        removeX() - GAP - font.width(summary), y + TEXT_INSET,
                        ScreenTheme.MUTED, false);
            }
            graphics.drawString(font, Component.translatable(KEY_REMOVE),
                    removeX() + GAP, y + TEXT_INSET, ScreenTheme.BAD, false);

            if (isHovered && !group) {
                tooltip = EffectSummary.describe(name, effects.get(name));
            }
        }

        ScreenDraw.scrollbar(graphics, left + PANEL_WIDTH - ScreenTheme.PADDING - 2,
                listTop(), visibleRows() * ROW_HEIGHT, visibleRows(), totalRows(), scroll);

        drawFooter(graphics, mouseX, mouseY, partialTick);
    }

    private void drawFooter(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshPreview();
        dock.render(graphics, font);

        drawWidgets(graphics, mouseX, mouseY, partialTick);
        for (DropdownMenu menu : menus) {
            menu.render(graphics, font, mouseX, mouseY);
        }

        if (tooltip != null && !tooltip.isEmpty()) {
            ScreenDraw.tooltip(graphics, font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        dock.drag(mouseX, mouseY, width, height);
        if (draggingTargets) {
            dragTargets(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dock.release();
        draggingTargets = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void refreshPreview() {
        ItemStack sample = PreviewSamples.first(matchItems, matchTags, matchCategories);
        if (sample.isEmpty()) {
            sample = PreviewSamples.ofItem(boundItemId());
        }
        dock.sample(sample);
        dock.preview(minecraft, currentDefinition(currentId()));
    }

}
