package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.AffixMechanics;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.style.AttackAttribute;
import net.pixeldreamstudios.combat_styles.style.AttackBehaviour;
import net.pixeldreamstudios.combat_styles.style.AttackTrail;
import net.pixeldreamstudios.combat_styles.style.AttackDrag;
import net.pixeldreamstudios.combat_styles.style.InheritGroups;
import net.pixeldreamstudios.combat_styles.style.InheritMode;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.AttackPlacement;
import net.pixeldreamstudios.combat_styles.style.ExtraAttack;
import net.pixeldreamstudios.combat_styles.style.HitboxOffset;
import net.pixeldreamstudios.combat_styles.style.HitboxSet;
import net.pixeldreamstudios.combat_styles.style.SlashPlacement;
import net.pixeldreamstudios.combat_styles.style.StyleCost;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StylePool;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleText;
import net.pixeldreamstudios.combat_styles.equipment.AttackConditions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class AttackEditorScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 546;
    private static final int PANEL_HEIGHT = 246;
    private static final int PADDING = 10;
    private static final int HEADER = 62;
    private static final int NAME_BOX_Y = 38;
    private static final int NAME_BOX_HEIGHT = 16;
    private static final int NAME_MAX_LENGTH = 48;
    private static final int FOOTER = 50;
    private static final int LIST_BOTTOM_GAP = 14;
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 62;
    private static final int BUTTON_GAP = 4;
    private static final int BUTTON_HEIGHT = 18;
    private static final int STEP_WIDTH = 16;
    private static final int REMOVE_GAP = 6;
    private static final int OP_WIDTH = 30;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SECTION_HEIGHT = 13;

    private static final int COLOR_SHADE = ScreenTheme.SHADE;
    private static final int COLOR_PANEL = ScreenTheme.PANEL;
    private static final int COLOR_SUNK = ScreenTheme.SUNK;
    private static final int COLOR_BORDER = ScreenTheme.BORDER;
    private static final int COLOR_ROW_ALT = ScreenTheme.ROW_ALT;
    private static final int COLOR_HEADING = ScreenTheme.HEADING;
    private static final int COLOR_TEXT = ScreenTheme.TEXT;
    private static final int COLOR_MUTED = ScreenTheme.MUTED;
    private static final int COLOR_GOOD = ScreenTheme.GOOD;
    private static final int COLOR_ACCENT = ScreenTheme.ACCENT;
    private static final int COLOR_BAD = ScreenTheme.BAD;
    private static final int COLOR_CHANGED = ScreenTheme.CHANGED;
    private static final int COLOR_LOCKED = ScreenTheme.LOCKED;

    private static final String KEY_TITLE = "screen.combat_styles.attack_editor";
    private static final String KEY_HINT = "screen.combat_styles.attack_editor_hint";
    private static final String KEY_AFFINITY_ON = "screen.combat_styles.affinity_on";
    private static final String KEY_AFFINITY_OFF = "screen.combat_styles.affinity_off";
    private static final String KEY_DRAG_ON = "screen.combat_styles.drag_on";
    private static final String KEY_DRAG_OFF = "screen.combat_styles.drag_off";
    private static final String KEY_SCALE_ANIMATION_ON = "screen.combat_styles.scale_animation_on";
    private static final String KEY_SCALE_ANIMATION_OFF = "screen.combat_styles.scale_animation_off";
    private static final int SCALE_ANIMATION_EXTRA_WIDTH = 50;
    private static final int TRAIL_SWATCH_SIZE = 12;
    private static final String KEY_RESET = "screen.combat_styles.reset";
    private static final String KEY_BORROW = "screen.combat_styles.borrow_attack";
    private static final String KEY_NAME = "screen.combat_styles.attack_name";

    private static final int KEY_ENTER = 257;
    private static final int KEY_R = 82;
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_APPLY = "screen.combat_styles.apply";

    private record Field(String key, String labelKey, double base, double step,
                         double min, double max, String sectionKey) {
    }

    private static final String SECTION_ATTACK = "screen.combat_styles.section_attack";
    private static final String SECTION_ATTRIBUTES = "screen.combat_styles.section_attributes";
    private static final String SECTION_TIMING = "screen.combat_styles.section_attack_timing";
    private static final String SECTION_DRAG = "screen.combat_styles.section_attack_drag";
    private static final String SECTION_MOTION = "screen.combat_styles.section_attack_motion";
    private static final String SECTION_HITBOX = "screen.combat_styles.section_attack_hitbox";
    private static final String SECTION_IDENTITY = "screen.combat_styles.section_attack_identity";
    private static final String SECTION_AFFIXES = "screen.combat_styles.section_attack_affixes";

    private static final Set<String> HIDDEN_FOR_BOXES =
            Set.of(SECTION_DRAG, SECTION_MOTION, SECTION_HITBOX);
    private static final String KEY_HITBOX_TYPE = "screen.combat_styles.field_hitbox_type";
    private static final String KEY_SWING_SOUND = "screen.combat_styles.field_swing_sound";
    private static final int IDENTITY_ROWS = 4;
    private static final String KEY_STYLE_PAGE = "screen.combat_styles.field_style_page";
    private static final String KEY_STYLE_PAGE_HINT = "hint.combat_styles.style_page";
    private static final String KEY_NATURAL = "screen.combat_styles.style_page_natural";
    private static final String KEY_NOT_NATURAL = "screen.combat_styles.style_page_hidden";
    private static final String KEY_CONDITIONS = "screen.combat_styles.field_conditions";
    private static final String KEY_NO_CONDITIONS = "screen.combat_styles.conditions_none";
    private static final String KEY_CONDITIONS_CHANGED = "screen.combat_styles.conditions_changed";
    private static final String CONDITION_HINT_PREFIX = "hint.combat_styles.attack_condition.";
    private static final String KEY_DAMAGE_TYPE = "screen.combat_styles.field_damage_type";
    private static final String KEY_PHYSICAL = "screen.combat_styles.damage_type_physical";
    private static final String KEY_INHERITED = "screen.combat_styles.value_inherited";
    private static final String KEY_NO_SOUND = "screen.combat_styles.sound_none";
    private static final String[] HITBOX_TYPES = {
            "", "horizontal_plane", "vertical_plane", "forward_box"};
    private static final String KEY_ADD_ATTRIBUTE = "screen.combat_styles.add_attribute";
    private static final String KEY_ADD_AFFIX_ROW = "screen.combat_styles.add_attack_affix";
    private static final String KEY_SLASHES_BUTTON = "screen.combat_styles.slashes_button";
    private static final String KEY_BOXES_BUTTON = "screen.combat_styles.boxes_button";
    private static final double DEFAULT_SWING_REACH = 3.0D;

    private static final String ATTRIBUTE_PREFIX = "attribute:";
    private static final double ATTRIBUTE_LIMIT = 1024.0D;
    private static final double PERCENT = 100.0D;
    private static final int REMOVE_WIDTH = 10;
    private static final String REMOVE_LABEL = "x";
    private static final List<String> DEFAULT_ATTRIBUTES = List.of(
            Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED, Attributes.MOVEMENT_SPEED,
            Attributes.ARMOR, Attributes.KNOCKBACK_RESISTANCE, Attributes.ATTACK_KNOCKBACK)
            .stream()
            .map(holder -> BuiltInRegistries.ATTRIBUTE.getKey(holder.value()))
            .filter(Objects::nonNull)
            .map(ResourceLocation::toString)
            .toList();

    private static final String FIELD_DAMAGE = "damage_multiplier";
    private static final String FIELD_ANGLE = "angle";
    private static final String FIELD_UPSWING = "upswing";
    private static final String FIELD_RANGE = "range_multiplier";
    private static final String FIELD_MOVEMENT = "movement_speed_multiplier";
    private static final String FIELD_COOLDOWN_SCALE = "cooldown_scale";
    private static final String FIELD_HASTENS_NEXT = "hastens_next";
    private static final String FIELD_COST = "cost";
    private static final String FIELD_DRAG_TICKS = "drag_ticks";
    private static final String KEY_TRAIL = "screen.combat_styles.trail_color";
    private static final String KEY_TRAIL_HINT = "screen.combat_styles.trail_hint";
    private static final String KEY_TRAIL_FORCED_ON = "screen.combat_styles.trail_forced_on";
    private static final String KEY_TRAIL_FORCED_OFF = "screen.combat_styles.trail_forced_off";
    private static final String FIELD_DRAG_MAX_TARGETS = "drag_max_targets";
    private static final String FIELD_DASH_FORWARD = "dash_forward";
    private static final String FIELD_DASH_UPWARD = "dash_upward";
    private static final String FIELD_BLINK_DISTANCE = "blink_distance";
    private static final String FIELD_MOTION_IFRAMES = "motion_iframes";
    private static final String FIELD_HITBOX_FORWARD = "hitbox_forward";
    private static final String FIELD_HITBOX_UP = "hitbox_up";
    private static final String FIELD_HITBOX_RIGHT = "hitbox_right";
    private static final String FIELD_HITBOX_YAW = "hitbox_yaw";
    private static final String FIELD_HITBOX_PITCH = "hitbox_pitch";
    private static final String FIELD_HITBOX_ROLL = "hitbox_roll";
    private static final String FIELD_CURSOR_RANGE = "cursor_range";
    private static final double MIN_UPSWING = 0.05D;
    private static final double NEUTRAL_SCALE = 1.0D;
    private static final double MIN_TIMING_SCALE = 0.05D;
    private static final double MAX_TIMING_SCALE = 4.0D;
    private static final double MAX_COST = 512.0D;
    private static final double MAX_IFRAMES = 100.0D;

    private final StylePage page;
    private final Consumer<AttackOverride> onApply;
    private final Consumer<String> onRename;
    private final String currentName;
    private final List<Field> fields = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();

    private EditBox nameBox;
    private boolean ignoreAffinity;
    private boolean dragEnabled;
    private boolean scaleAnimation;
    private boolean combat_styles$draggingBar;
    private String hitboxChoice = "";
    private String soundChoice = "";
    private String damageTypeChoice = "";
    private final List<String> inheritedConditions;
    private List<String> conditionChoice;
    private boolean stylePageShown;
    private Boolean stylePageChoice;
    private boolean stylePageHovered;
    private boolean dragTrail;
    private EditBox trailBox;
    private final List<AbstractWidget> footerButtons = new ArrayList<>();
    private String trailColor = "";
    private boolean trailForced;
    private final Map<String, AttributeModifier.Operation> operations = new LinkedHashMap<>();
    private final Map<String, Boolean> stacking = new LinkedHashMap<>();
    private EditBox valueBox;
    private int editingRow = -1;
    private final List<SlashPlacement> slashes = new ArrayList<>();
    private HitboxSet boxes = HitboxSet.NONE;
    private AttackPlacement placement = AttackPlacement.NONE;

    private final boolean boxMode;
    private InheritGroups inherit;
    private List<String> ownAffixes;
    private Consumer<List<String>> onAffixes;
    private Map<String, AffixTuning> ownTuning;
    private Consumer<Map<String, AffixTuning>> onTuning;
    private Consumer<InheritGroups> onInherit;
    private Component subtitle;

    private static final int INHERIT_WIDTH = 40;

    public AttackEditorScreen(Screen parent, StylePage page, AttackOverride current,
                              Consumer<AttackOverride> onApply) {
        this(parent, page, current, onApply, "", name -> {
        });
    }

    public AttackEditorScreen withStylePage() {
        this.stylePageShown = true;
        return this;
    }

    public AttackEditorScreen withSubtitle(Component value) {
        this.subtitle = value;
        return this;
    }

    public AttackEditorScreen withInherit(InheritGroups groups,
                                          Consumer<InheritGroups> callback) {
        this.inherit = groups;
        this.onInherit = callback;
        return this;
    }

    public AttackEditorScreen withAffixes(List<String> current,
                                          Consumer<List<String>> callback) {
        this.ownAffixes = new ArrayList<>(current);
        this.onAffixes = callback;
        return this;
    }

    public AttackEditorScreen withTuning(Map<String, AffixTuning> current,
                                         Consumer<Map<String, AffixTuning>> callback) {
        this.ownTuning = new LinkedHashMap<>(current);
        this.onTuning = callback;
        return this;
    }

    private static String combat_styles$groupOf(String section) {
        if (SECTION_TIMING.equals(section) || SECTION_DRAG.equals(section)) {
            return InheritGroups.KEY_TIMING;
        }
        if (SECTION_MOTION.equals(section) || SECTION_HITBOX.equals(section)) {
            return InheritGroups.KEY_BEHAVIOUR;
        }
        if (SECTION_AFFIXES.equals(section)) {
            return InheritGroups.KEY_AFFIXES;
        }
        if (SECTION_ATTACK.equals(section)) {
            return InheritGroups.KEY_SHAPE;
        }
        return InheritGroups.KEY_META;
    }

    private int combat_styles$inheritX(int x, int listWidth) {
        return x + listWidth - INHERIT_WIDTH - 6;
    }

    private void combat_styles$renderInherit(GuiGraphics graphics, String section,
                                             int x, int y, int listWidth) {
        if (inherit == null) {
            return;
        }
        InheritMode mode = inherit.mode(combat_styles$groupOf(section));
        graphics.drawString(font, Component.translatable(mode.translationKey()),
                combat_styles$inheritX(x, listWidth), y + 3,
                mode.inherits() ? COLOR_GOOD : COLOR_MUTED, false);
    }

    public AttackEditorScreen(Screen parent, StylePage page, AttackOverride current,
                              Consumer<AttackOverride> onApply, String currentName,
                              Consumer<String> onRename) {
        this(parent, page, current, onApply, currentName, onRename, false);
    }

    public AttackEditorScreen(Screen parent, StylePage page, AttackOverride current,
                              Consumer<AttackOverride> onApply, String currentName,
                              Consumer<String> onRename, boolean forBox) {
        super(Component.translatable(KEY_TITLE), parent);
        this.boxMode = forBox;
        this.page = page;
        this.onApply = onApply;
        this.onRename = onRename;
        this.currentName = currentName == null ? "" : currentName;

        AttackOverride override = current == null ? AttackOverride.EMPTY : current;
        ignoreAffinity = override.ignoreAffinity().orElse(false);
        scaleAnimation = override.scalesAnimation();
        hitboxChoice = override.hitbox().orElse("");
        soundChoice = override.swingSound().orElse("");
        damageTypeChoice = override.damageType().orElse("");
        StylePage original = StyleRegistry.raw(page.styleId());
        inheritedConditions = AttackOverride.namesOf(
                (original == null ? page : original).attack().conditions());
        stylePageChoice = override.stylePage().orElse(null);
        conditionChoice = override.conditions()
                .filter(chosen -> !AttackOverride.sameConditions(chosen, inheritedConditions))
                .orElse(null);
        addField(FIELD_DAMAGE, page.damageMultiplier(),
                override.damageMultiplier().orElse(page.damageMultiplier()), 0.1D, 0.0D, 100.0D);
        addField(FIELD_ANGLE, page.angle(),
                override.angle().orElse(page.angle()), 5.0D, 0.0D, 360.0D);
        addField(FIELD_UPSWING, page.upswing(),
                override.upswing().orElse(page.upswing()), 0.05D, MIN_UPSWING, 10.0D);
        addField(FIELD_RANGE, page.rangeMultiplier(),
                override.rangeMultiplier().orElse(page.rangeMultiplier()), 0.1D, 0.1D, 20.0D);
        addField(FIELD_MOVEMENT, page.movementSpeedMultiplier(),
                override.movementSpeedMultiplier().orElse(page.movementSpeedMultiplier()),
                0.05D, 0.0D, 20.0D);
        addTiming(FIELD_COOLDOWN_SCALE, override.cooldownScale().orElse(null), NEUTRAL_SCALE,
                MIN_TIMING_SCALE, MAX_TIMING_SCALE);
        addTiming(FIELD_HASTENS_NEXT, override.hastensNext().orElse(null), NEUTRAL_SCALE,
                MIN_TIMING_SCALE, MAX_TIMING_SCALE);
        double baseCost = StyleCost.of(AttackOverrides.applyTo(page, override));
        addField(FIELD_COST, baseCost, override.costOverride().orElse(baseCost), 0.5D,
                0.0D, MAX_COST, SECTION_TIMING);

        AttackDrag drag = override.dragOrDefault();
        dragEnabled = drag.enabled();
        dragTrail = drag.trail();
        AttackTrail trail = override.behaviourOrDefault().trail().orElse(AttackTrail.NONE);
        trailColor = trail.color().orElse("");
        trailForced = trail.overridesAffixes();
        addField(FIELD_DRAG_TICKS, AttackDrag.DEFAULT_TICKS, drag.ticks(), 1.0D,
                1.0D, AttackDrag.MAX_AUTHORED_TICKS, SECTION_DRAG);
        addField(FIELD_DRAG_MAX_TARGETS, AttackDrag.DEFAULT_MAX_TARGETS, drag.maxTargets(), 1.0D,
                1.0D, AttackDrag.MAX_TARGETS_LIMIT, SECTION_DRAG);

        AffixEffects.Motion motion = override.behaviourOrDefault().motionOrNone();
        addField(FIELD_DASH_FORWARD, 0.0D,
                motion.dash().map(AffixEffects.Dash::forward).orElse(0.0F), 0.05D,
                -AffixMechanics.MAX_DASH_FORWARD, AffixMechanics.MAX_DASH_FORWARD,
                SECTION_MOTION);
        addField(FIELD_DASH_UPWARD, 0.0D,
                motion.dash().map(AffixEffects.Dash::upward).orElse(0.0F), 0.05D,
                -AffixMechanics.MAX_DASH_UPWARD, AffixMechanics.MAX_DASH_UPWARD,
                SECTION_MOTION);
        addField(FIELD_BLINK_DISTANCE, 0.0D,
                motion.blink().map(AffixEffects.Blink::distance).orElse(0.0F), 0.05D,
                -AffixMechanics.MAX_DASH_FORWARD, AffixMechanics.MAX_DASH_FORWARD,
                SECTION_MOTION);
        addField(FIELD_MOTION_IFRAMES, 0.0D,
                motion.dash().map(AffixEffects.Dash::iframes)
                        .orElseGet(() -> motion.blink().map(AffixEffects.Blink::iframes).orElse(0)),
                1.0D, 0.0D, MAX_IFRAMES, SECTION_MOTION);

        HitboxOffset offset = override.behaviourOrDefault().offsetOrNone();
        addField(FIELD_HITBOX_FORWARD, 0.0D, offset.forward(), 0.1D,
                -HitboxOffset.LIMIT, HitboxOffset.LIMIT, SECTION_HITBOX);
        addField(FIELD_HITBOX_UP, 0.0D, offset.up(), 0.1D,
                -HitboxOffset.LIMIT, HitboxOffset.LIMIT, SECTION_HITBOX);
        addField(FIELD_HITBOX_RIGHT, 0.0D, offset.right(), 0.1D,
                -HitboxOffset.LIMIT, HitboxOffset.LIMIT, SECTION_HITBOX);
        addField(FIELD_HITBOX_YAW, 0.0D, offset.yaw(), 5.0D,
                -HitboxOffset.ANGLE_LIMIT, HitboxOffset.ANGLE_LIMIT, SECTION_HITBOX);
        addField(FIELD_HITBOX_PITCH, 0.0D, offset.pitch(), 5.0D,
                -HitboxOffset.ANGLE_LIMIT, HitboxOffset.ANGLE_LIMIT, SECTION_HITBOX);
        addField(FIELD_HITBOX_ROLL, 0.0D, offset.roll(), 5.0D,
                -HitboxOffset.ANGLE_LIMIT, HitboxOffset.ANGLE_LIMIT, SECTION_HITBOX);
        addField(FIELD_CURSOR_RANGE, 0.0D,
                override.behaviourOrDefault().placementOrNone().followsCursor()
                        ? override.behaviourOrDefault().placementOrNone().clampedRange()
                        : 0.0D,
                1.0D, 0.0D, AttackPlacement.MAX_RANGE, SECTION_HITBOX);

        slashes.addAll(override.behaviourOrDefault().slashes());
        boxes = override.behaviourOrDefault().boxesOrNone();
        placement = override.behaviourOrDefault().placementOrNone();

        List<String> shown = new ArrayList<>(DEFAULT_ATTRIBUTES);
        for (String extra : override.attributes().keySet()) {
            if (!shown.contains(extra)) {
                shown.add(extra);
            }
        }
        for (String attribute : shown) {
            AttackAttribute existing = override.attributes().get(attribute);
            double value = existing == null
                    ? 0.0D
                    : toDisplayed(existing.amount(), existing.operation());
            operations.put(attribute, existing == null
                    ? AttributeModifier.Operation.ADD_VALUE
                    : existing.operation());
            stacking.put(attribute, existing == null || existing.stacks());
            addField(ATTRIBUTE_PREFIX + attribute, 0.0D, value, 0.5D,
                    -ATTRIBUTE_LIMIT, ATTRIBUTE_LIMIT,
                    SECTION_ATTRIBUTES);
        }
    }

    private AttackAttribute timingOf(Field field, double displayed) {
        AttributeModifier.Operation operation = operations.getOrDefault(field.key(),
                AttributeModifier.Operation.ADD_VALUE);
        return new AttackAttribute(toStored(displayed, operation), operation,
                stacking.getOrDefault(field.key(), true));
    }

    private void addTiming(String key, AttackAttribute existing, double fallback,
                           double min, double max) {
        double value = existing == null
                ? fallback
                : toDisplayed(existing.amount(), existing.operation());
        operations.put(key, existing == null
                ? AttributeModifier.Operation.ADD_VALUE
                : existing.operation());
        stacking.put(key, existing == null || existing.stacks());
        addField(key, fallback, value, 0.05D, min, max, SECTION_TIMING);
    }

    private static double toStored(double displayed, AttributeModifier.Operation operation) {
        return operation == AttributeModifier.Operation.ADD_VALUE
                ? displayed
                : displayed / PERCENT;
    }

    private static double toDisplayed(double stored, AttributeModifier.Operation operation) {
        return operation == AttributeModifier.Operation.ADD_VALUE
                ? stored
                : stored * PERCENT;
    }

    private void removeAttributeField(int index) {
        Field field = fields.get(index);
        operations.remove(attributeIdOf(field));
        fields.remove(index);
        values.remove(index);
        if (editingRow == index) {
            cancelEdit();
        }
        scroll = Math.clamp(scroll, 0, maxScroll());
    }

    private void beginEdit(int index) {
        editingRow = index;
        valueBox.setVisible(true);
        valueBox.setValue(StyleText.formatDecimal(values.get(index)));
        valueBox.setFocused(true);
        setFocused(valueBox);
    }

    private void cancelEdit() {
        editingRow = -1;
        valueBox.setVisible(false);
        valueBox.setFocused(false);
        setFocused(null);
    }

    private void commitEdit() {
        if (editingRow < 0) {
            return;
        }
        Field field = fields.get(editingRow);
        try {
            double parsed = Double.parseDouble(valueBox.getValue().trim());
            values.set(editingRow, Math.clamp(parsed, field.min(), field.max()));
        } catch (NumberFormatException ignored) {
        }
        cancelEdit();
    }

    private void addAttributeField(String attributeId) {
        if (fields.stream().anyMatch(field -> field.key().equals(ATTRIBUTE_PREFIX + attributeId))) {
            return;
        }
        operations.putIfAbsent(attributeId, AttributeModifier.Operation.ADD_VALUE);
        addField(ATTRIBUTE_PREFIX + attributeId, 0.0D, 0.0D, 0.5D,
                -ATTRIBUTE_LIMIT, ATTRIBUTE_LIMIT,
                SECTION_ATTRIBUTES);
    }

    private List<String> currentAttributeIds() {
        return fields.stream()
                .filter(this::isAttribute)
                .map(this::attributeIdOf)
                .toList();
    }

    private void openAttributePicker() {
        minecraft.setScreen(new AttributePickerScreen(this, currentAttributeIds(),
                this::addAttributeField));
    }

    private void addField(String key, double base, double value, double step,
                          double min, double max) {
        addField(key, base, value, step, min, max, SECTION_ATTACK);
    }

    private void addField(String key, double base, double value, double step,
                          double min, double max, String sectionKey) {
        if (boxMode && HIDDEN_FOR_BOXES.contains(sectionKey)) {
            return;
        }
        String labelKey = key.startsWith(ATTRIBUTE_PREFIX)
                ? "attribute.name." + key.substring(ATTRIBUTE_PREFIX.length()).replace(':', '.')
                : "screen.combat_styles.field_" + key;
        fields.add(new Field(key, labelKey, base, step, min, max, sectionKey));
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
        footerButtons.clear();

        nameBox = new FieldBox(font, left + PADDING, top + NAME_BOX_Y,
                PANEL_WIDTH - PADDING * 2, NAME_BOX_HEIGHT,
                Component.translatable(KEY_NAME));
        nameBox.setHint(Component.translatable(KEY_NAME).withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setMaxLength(NAME_MAX_LENGTH);
        nameBox.setValue(currentName);
        addRenderableWidget(nameBox);

        trailBox = new FieldBox(font, left + PADDING, top + PANEL_HEIGHT - FOOTER + 3,
                80, 14, Component.translatable(KEY_TRAIL));
        trailBox.setMaxLength(9);
        trailBox.setHint(Component.translatable(KEY_TRAIL_HINT)
                .withStyle(ChatFormatting.DARK_GRAY));
        trailBox.setValue(trailColor);
        trailBox.setResponder(value -> trailColor = value);
        addRenderableWidget(trailBox);

        combat_styles$footer(PanelButton.of(trailForcedLabel(), button -> {
            trailForced = !trailForced;
            button.setMessage(trailForcedLabel());
        }).bounds(left + PADDING + 84, top + PANEL_HEIGHT - FOOTER + 2,
                BUTTON_WIDTH + 30, 16).build());

        combat_styles$footer(PanelButton.of(scaleAnimationLabel(), button -> {
            scaleAnimation = !scaleAnimation;
            button.setMessage(scaleAnimationLabel());
        }).bounds(left + PADDING + 86 + BUTTON_WIDTH + 30 + TRAIL_SWATCH_SIZE + BUTTON_GAP,
                top + PANEL_HEIGHT - FOOTER + 2,
                BUTTON_WIDTH + SCALE_ANIMATION_EXTRA_WIDTH, 16).build());

        valueBox = new FieldBox(font, left + PADDING, top + NAME_BOX_Y, 60, 12, Component.empty());
        valueBox.setMaxLength(12);
        valueBox.setVisible(false);
        addRenderableWidget(valueBox);

        int buttonY = top + PANEL_HEIGHT - FOOTER + 27;
        combat_styles$footer(PanelButton.of(Component.translatable(KEY_RESET), button -> resetAll())
                .bounds(left + PADDING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        combat_styles$footer(PanelButton.of(affinityLabel(), button -> {
            ignoreAffinity = !ignoreAffinity;
            button.setMessage(affinityLabel());
        }).bounds(left + PADDING + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        combat_styles$footer(PanelButton.of(dragLabel(), button -> {
            dragEnabled = !dragEnabled;
            button.setMessage(dragLabel());
        }).bounds(left + PADDING + (BUTTON_WIDTH + BUTTON_GAP) * 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        combat_styles$footer(PanelButton.of(Component.translatable(KEY_BOXES_BUTTON, boxes.shapes().size()),
                        button -> minecraft.setScreen(new HitboxEditorScreen(this, boxes,
                                combat_styles$extras(),
                                combat_styles$valueOf(FIELD_RANGE, 1.0D) * DEFAULT_SWING_REACH,
                                applied -> boxes = applied,
                                this::combat_styles$applyExtraBoxes, page)))
                .bounds(left + PADDING + (BUTTON_WIDTH + BUTTON_GAP) * 4, buttonY,
                        BUTTON_WIDTH, BUTTON_HEIGHT).build());

        combat_styles$footer(PanelButton.of(slashesLabel(),
                        button -> {
                            SlashListScreen list = new SlashListScreen(this, page, slashes,
                                    applied -> {
                                        slashes.clear();
                                        slashes.addAll(applied);
                                    });
                            minecraft.setScreen(boxMode || inherit != null
                                    ? list.particlesOnly() : list);
                        })
                .bounds(left + PADDING + (BUTTON_WIDTH + BUTTON_GAP) * 3, buttonY,
                        BUTTON_WIDTH, BUTTON_HEIGHT).build());

        if (inherit != null) {
            combat_styles$footer(PanelButton.of(Component.translatable(KEY_BORROW),
                            button -> combat_styles$openBorrow())
                    .bounds(left + PADDING + (BUTTON_WIDTH + BUTTON_GAP) * 5, buttonY,
                            BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

        combat_styles$footer(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH * 2 - BUTTON_GAP, buttonY,
                        BUTTON_WIDTH, BUTTON_HEIGHT).build());

        combat_styles$footer(PanelButton.of(Component.translatable(KEY_APPLY), button -> apply())
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (!combat_styles$draggingBar) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        combat_styles$scrollTo(mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        combat_styles$draggingBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void combat_styles$scrollTo(double mouseY) {
        int listTop = editorListTop();
        int listBottom = editorListBottom();
        int viewport = listBottom - listTop;
        int content = contentHeight();
        if (content <= viewport) {
            return;
        }
        double ratio = (mouseY - listTop) / Math.max(1, viewport);
        scroll = Math.clamp((int) (ratio * (content - viewport)), 0, content - viewport);
    }

    private boolean combat_styles$clickScrollbar(double mouseX, double mouseY) {
        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        int listTop = editorListTop();
        int listBottom = editorListBottom();
        if (contentHeight() <= listBottom - listTop) {
            return false;
        }
        int barX = x + listWidth - SCROLLBAR_WIDTH - 1;
        if (!ScreenDraw.isWithin(mouseX, mouseY, barX, listTop,
                SCROLLBAR_WIDTH, listBottom - listTop)) {
            return false;
        }
        combat_styles$draggingBar = true;
        combat_styles$scrollTo(mouseY);
        return true;
    }

    private void combat_styles$openBorrow() {
        minecraft.setScreen(new AttackPickerScreen(this, (source, borrowed) -> {
            if (borrowed == null) {
                return;
            }
            onApply.accept(borrowed);
            if (onInherit != null) {
                onInherit.accept(InheritGroups.NONE);
            }
            AttackEditorScreen reopened = new AttackEditorScreen(parent, page, borrowed, onApply,
                    currentName, onRename).withInherit(InheritGroups.NONE, onInherit);
            if (ownAffixes != null) {
                reopened.withAffixes(ownAffixes, onAffixes);
            }
            minecraft.setScreen(reopened);
        }));
    }

    private Component trailForcedLabel() {
        return Component.translatable(trailForced ? KEY_TRAIL_FORCED_ON : KEY_TRAIL_FORCED_OFF);
    }

    private void resetAll() {
        for (int i = 0; i < fields.size(); i++) {
            values.set(i, fields.get(i).base());
        }
        conditionChoice = null;
    }

    private Component slashesLabel() {
        return Component.translatable(KEY_SLASHES_BUTTON, slashes.size());
    }

    private Component affinityLabel() {
        return Component.translatable(ignoreAffinity ? KEY_AFFINITY_OFF : KEY_AFFINITY_ON);
    }

    private Component scaleAnimationLabel() {
        return Component.translatable(scaleAnimation ? KEY_SCALE_ANIMATION_ON : KEY_SCALE_ANIMATION_OFF);
    }

    private Component dragLabel() {
        return Component.translatable(dragEnabled ? KEY_DRAG_ON : KEY_DRAG_OFF);
    }

    private void apply() {
        AttackOverride override = AttackOverride.EMPTY;
        boolean any = false;
        int dragTicks = AttackDrag.DEFAULT_TICKS;
        int dragMaxTargets = AttackDrag.DEFAULT_MAX_TARGETS;
        float dashForward = 0.0F;
        float dashUpward = 0.0F;
        float blinkDistance = 0.0F;
        int motionIframes = 0;
        float hitboxForward = 0.0F;
        float hitboxUp = 0.0F;
        float hitboxRight = 0.0F;
        float hitboxYaw = 0.0F;
        float hitboxPitch = 0.0F;
        float hitboxRoll = 0.0F;
        float cursorRange = 0.0F;
        Map<String, AttackAttribute> attributes = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            double value = values.get(i);
            if (value == field.base() && !field.key().startsWith(ATTRIBUTE_PREFIX)
                    && !SECTION_TIMING.equals(field.sectionKey())
                    && !SECTION_DRAG.equals(field.sectionKey())
                    && !SECTION_MOTION.equals(field.sectionKey())) {
                continue;
            }
            if (field.key().startsWith(ATTRIBUTE_PREFIX)) {
                String attributeId = field.key().substring(ATTRIBUTE_PREFIX.length());
                AttributeModifier.Operation operation = operations.getOrDefault(attributeId,
                        AttributeModifier.Operation.ADD_VALUE);
                attributes.put(attributeId, new AttackAttribute(
                        toStored(value, operation), operation,
                        stacking.getOrDefault(attributeId, true)));
                any = true;
                continue;
            }
            any = true;
            override = switch (field.key()) {
                case FIELD_DAMAGE -> override.withDamageMultiplier(value);
                case FIELD_ANGLE -> override.withAngle(value);
                case FIELD_UPSWING -> override.withUpswing(value);
                case FIELD_RANGE -> override.withRangeMultiplier((float) value);
                case FIELD_MOVEMENT -> override.withMovementSpeedMultiplier((float) value);
                case FIELD_COOLDOWN_SCALE -> override.withCooldownScale(timingOf(field, value));
                case FIELD_HASTENS_NEXT -> override.withHastensNext(timingOf(field, value));
                case FIELD_COST -> override.withCostOverride(value);
                case FIELD_DRAG_TICKS -> {
                    dragTicks = (int) Math.round(value);
                    yield override;
                }
                case FIELD_DRAG_MAX_TARGETS -> {
                    dragMaxTargets = (int) Math.round(value);
                    yield override;
                }
                case FIELD_DASH_FORWARD -> {
                    dashForward = (float) value;
                    yield override;
                }
                case FIELD_DASH_UPWARD -> {
                    dashUpward = (float) value;
                    yield override;
                }
                case FIELD_MOTION_IFRAMES -> {
                    motionIframes = (int) Math.round(value);
                    yield override;
                }
                case FIELD_HITBOX_FORWARD -> {
                    hitboxForward = (float) value;
                    yield override;
                }
                case FIELD_HITBOX_UP -> {
                    hitboxUp = (float) value;
                    yield override;
                }
                case FIELD_HITBOX_RIGHT -> {
                    hitboxRight = (float) value;
                    yield override;
                }
                case FIELD_HITBOX_YAW -> {
                    hitboxYaw = (float) value;
                    yield override;
                }
                case FIELD_HITBOX_PITCH -> {
                    hitboxPitch = (float) value;
                    yield override;
                }
                case FIELD_HITBOX_ROLL -> {
                    hitboxRoll = (float) value;
                    yield override;
                }
                case FIELD_CURSOR_RANGE -> {
                    cursorRange = (float) value;
                    yield override;
                }
                case FIELD_BLINK_DISTANCE -> {
                    blinkDistance = (float) value;
                    yield override;
                }
                default -> override;
            };
        }
        AttackDrag drag = new AttackDrag(dragEnabled, dragTicks, dragMaxTargets, dragTrail);
        if (!drag.equals(AttackDrag.NONE)) {
            override = override.withDrag(drag);
            any = true;
        }
        AffixEffects.Motion motion = new AffixEffects.Motion(
                dashForward != 0.0F || dashUpward != 0.0F
                        ? Optional.of(new AffixEffects.Dash(dashForward, dashUpward, false,
                                1.0F, 0.0F, 0.0F, motionIframes))
                        : Optional.empty(),
                blinkDistance != 0.0F
                        ? Optional.of(new AffixEffects.Blink(blinkDistance, false, motionIframes))
                        : Optional.empty(),
                0);
        HitboxOffset offset = new HitboxOffset(hitboxForward, hitboxUp, hitboxRight,
                hitboxYaw, hitboxPitch, hitboxRoll).clamped();
        AttackPlacement shaped = cursorRange > 0.0F
                ? new AttackPlacement(AttackPlacement.Mode.CURSOR, cursorRange,
                        placement.stopAtEntities(), placement.stopMargin())
                : AttackPlacement.NONE;
        AttackTrail authoredTrail = trailColor == null || trailColor.isBlank()
                ? AttackTrail.NONE
                : new AttackTrail(Optional.of(trailColor.trim()), true, trailForced);
        if (!motion.isEmpty() || !offset.isEmpty() || !slashes.isEmpty()
                || !boxes.isEmpty() || !shaped.isEmpty() || !authoredTrail.isEmpty()) {
            override = override.withBehaviour(new AttackBehaviour(
                    motion.isEmpty() ? Optional.empty() : Optional.of(motion),
                    offset.isEmpty() ? Optional.empty() : Optional.of(offset),
                    slashes, List.of(),
                    shaped.isEmpty() ? Optional.empty() : Optional.of(shaped),
                    boxes.isEmpty() ? Optional.empty() : Optional.of(boxes),
                    authoredTrail.isEmpty() ? Optional.empty() : Optional.of(authoredTrail)));
            any = true;
        }
        if (!attributes.isEmpty()) {
            override = override.withAttributes(attributes);
            any = true;
        }
        if (!hitboxChoice.isBlank()) {
            override = override.withHitbox(hitboxChoice);
            any = true;
        }
        if (!damageTypeChoice.isBlank()) {
            override = override.withDamageType(damageTypeChoice);
            any = true;
        }
        if (!soundChoice.isBlank()) {
            override = override.withSwingSound(soundChoice);
            any = true;
        }
        if (ignoreAffinity) {
            override = override.withIgnoreAffinity(true);
            any = true;
        }
        if (scaleAnimation) {
            override = override.withScaleAnimation(Optional.of(Boolean.TRUE));
            any = true;
        }
        if (conditionChoice != null) {
            override = override.withConditions(Optional.of(List.copyOf(conditionChoice)));
            any = true;
        }
        if (stylePageShown && stylePageChoice != null
                && stylePageChoice != StylePool.definedNatural(page.styleId())) {
            override = override.withStylePage(Optional.of(stylePageChoice));
            any = true;
        }
        onApply.accept(any ? override : null);
        onRename.accept(nameBox == null ? currentName : nameBox.getValue().trim());
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, width, height, COLOR_SHADE);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_PANEL);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER);

        graphics.drawString(font, title, left + PADDING, top + 10, COLOR_HEADING, false);
        graphics.drawString(font, subtitle == null ? StyleText.name(page) : subtitle,
                left + PADDING, top + 22, COLOR_MUTED, false);

        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        int listTop = editorListTop();
        int listBottom = editorListBottom();
        ScreenDraw.sunken(graphics, x - 2, listTop - 2, listWidth + 2, (listBottom) - (listTop) + 4);

        graphics.enableScissor(x - 2, listTop - 2, x + listWidth, listBottom + 2);
        String section = null;
        int y = listTop - scroll;
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (!field.sectionKey().equals(section)) {
                section = field.sectionKey();
                if (y + SECTION_HEIGHT > listTop - 2 && y < listBottom) {
                    graphics.drawString(font, Component.translatable(section),
                            x + 4, y + 3, COLOR_HEADING, false);
                    combat_styles$renderInherit(graphics, section, x, y, listWidth);
                    graphics.fill(x + 4, y + SECTION_HEIGHT - 2,
                            x + listWidth - 4, y + SECTION_HEIGHT - 1, 0x30FFFFFF);
                }
                y += SECTION_HEIGHT;
            }
            if (y + ROW_HEIGHT > listTop - 2 && y < listBottom) {
                renderRow(graphics, field, values.get(i), x, y, listWidth, i, mouseX, mouseY);
            }
            y += ROW_HEIGHT;
        }

        if (y + ROW_HEIGHT > listTop - 2 && y < listBottom) {
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y, listWidth - SCROLLBAR_WIDTH - 2,
                    ROW_HEIGHT);
            graphics.fill(x, y + 2, x + listWidth - SCROLLBAR_WIDTH - 2, y + ROW_HEIGHT - 2,
                    hovered ? 0x40FFFFFF : 0x18FFFFFF);
            Component label = Component.translatable(KEY_ADD_ATTRIBUTE);
            graphics.drawString(font, label, x + 6, y + 6,
                    hovered ? COLOR_ACCENT : COLOR_MUTED, false);
        }
        y += ROW_HEIGHT;

        if (y + SECTION_HEIGHT > listTop - 2 && y < listBottom) {
            graphics.drawString(font, Component.translatable(SECTION_IDENTITY),
                    x + 4, y + 3, COLOR_HEADING, false);
            combat_styles$renderInherit(graphics, SECTION_IDENTITY, x, y, listWidth);
            graphics.fill(x + 4, y + SECTION_HEIGHT - 2,
                    x + listWidth - 4, y + SECTION_HEIGHT - 1, 0x30FFFFFF);
        }
        y += SECTION_HEIGHT;

        if (y + ROW_HEIGHT > listTop - 2 && y < listBottom) {
            combat_styles$identityRow(graphics, KEY_HITBOX_TYPE,
                    combat_styles$hitboxLabel(), x, y, listWidth, mouseX, mouseY);
        }
        y += ROW_HEIGHT;

        if (y + ROW_HEIGHT > listTop - 2 && y < listBottom) {
            combat_styles$identityRow(graphics, KEY_SWING_SOUND,
                    combat_styles$soundLabel(), x, y, listWidth, mouseX, mouseY);
        }
        y += ROW_HEIGHT;

        if (y + ROW_HEIGHT > listTop - 2 && y < listBottom) {
            combat_styles$identityRow(graphics, KEY_DAMAGE_TYPE,
                    combat_styles$damageTypeLabel(), x, y, listWidth, mouseX, mouseY);
        }
        y += ROW_HEIGHT;

        if (y + ROW_HEIGHT > listTop - 2 && y < listBottom) {
            combat_styles$identityRow(graphics, KEY_CONDITIONS,
                    combat_styles$conditionsLabel(), x, y, listWidth, mouseX, mouseY);
        }
        y += ROW_HEIGHT;

        stylePageHovered = false;
        if (stylePageShown) {
            if (y + ROW_HEIGHT > listTop - 2 && y < listBottom) {
                stylePageHovered = ScreenDraw.isWithin(mouseX, mouseY, x, y,
                        listWidth - SCROLLBAR_WIDTH - 2, ROW_HEIGHT);
                combat_styles$identityRow(graphics, KEY_STYLE_PAGE,
                        Component.translatable(combat_styles$natural() ? KEY_NATURAL : KEY_NOT_NATURAL)
                                .getString(), x, y, listWidth, mouseX, mouseY);
            }
            y += ROW_HEIGHT;
        }

        if (combat_styles$hasAffixSection()) {
            if (y + SECTION_HEIGHT > listTop - 2 && y < listBottom) {
                graphics.drawString(font, Component.translatable(SECTION_AFFIXES),
                        x + 4, y + 3, COLOR_HEADING, false);
                combat_styles$renderInherit(graphics, SECTION_AFFIXES, x, y, listWidth);
                graphics.fill(x + 4, y + SECTION_HEIGHT - 2,
                        x + listWidth - 4, y + SECTION_HEIGHT - 1, 0x30FFFFFF);
            }
            y += SECTION_HEIGHT;
        }
        if (ownAffixes != null) {
            for (String affix : ownAffixes) {
                if (y + ROW_HEIGHT > listTop - 2 && y < listBottom) {
                    combat_styles$affixRow(graphics, affix, x, y, listWidth, mouseX, mouseY);
                }
                y += ROW_HEIGHT;
            }
            if (y + ROW_HEIGHT > listTop - 2 && y < listBottom) {
                combat_styles$addAffixRow(graphics, x, y, listWidth, mouseX, mouseY);
            }
        }
        graphics.disableScissor();

        int viewport = listBottom - listTop;
        ScreenDraw.scrollbar(graphics, x + listWidth - SCROLLBAR_WIDTH - 1, listTop,
                viewport, viewport, contentHeight(), scroll);

        graphics.drawString(font,
                Component.translatable(stylePageHovered ? KEY_STYLE_PAGE_HINT : KEY_HINT),
                left + PADDING, top + PANEL_HEIGHT - FOOTER - 10, COLOR_MUTED, false);

        int trailY = top + PANEL_HEIGHT - FOOTER + 3;
        graphics.fill(left + PADDING - 1, trailY - 1, left + PADDING + 81, trailY + 15,
                COLOR_SUNK);
        graphics.renderOutline(left + PADDING - 1, trailY - 1, 82, 16, COLOR_BORDER);
        int swatchX = left + PADDING + 86 + BUTTON_WIDTH + 30;
        graphics.fill(swatchX, trailY + 2, swatchX + 12, trailY + 14, trailSwatch());
        graphics.renderOutline(swatchX, trailY + 2, 12, 12, COLOR_BORDER);

        nameBox.render(graphics, mouseX, mouseY, partialTick);
        trailBox.render(graphics, mouseX, mouseY, partialTick);
        for (AbstractWidget button : footerButtons) {
            button.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private <T extends AbstractWidget> T combat_styles$footer(T widget) {
        footerButtons.add(widget);
        return addRenderableWidget(widget);
    }

    private int trailSwatch() {
        if (trailColor == null || trailColor.isBlank()) {
            return 0xFF3A352C;
        }
        return new StyleAffix.TrailColor(trailColor.trim(), true).argb();
    }

    private boolean isAttribute(Field field) {
        return field.key().startsWith(ATTRIBUTE_PREFIX);
    }

    private String attributeIdOf(Field field) {
        return field.key().substring(ATTRIBUTE_PREFIX.length());
    }

    private int contentHeight() {
        int height = 0;
        String section = null;
        for (Field field : fields) {
            if (!field.sectionKey().equals(section)) {
                section = field.sectionKey();
                height += SECTION_HEIGHT;
            }
            height += ROW_HEIGHT;
        }
        height += ROW_HEIGHT;
        height += SECTION_HEIGHT + ROW_HEIGHT * combat_styles$identityRows();
        if (combat_styles$hasAffixSection()) {
            height += SECTION_HEIGHT;
        }
        if (ownAffixes != null) {
            height += ROW_HEIGHT * (ownAffixes.size() + 1);
        }
        return height;
    }

    protected int maxScroll() {
        int viewport = (top + PANEL_HEIGHT - FOOTER - 14) - (top + HEADER);
        return Math.max(0, contentHeight() - viewport);
    }

    private static String operationLabel(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADD_MULTIPLIED_BASE -> "%base";
            case ADD_MULTIPLIED_TOTAL -> "%tot";
            default -> "flat";
        };
    }

    private static AttributeModifier.Operation nextOperation(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADD_VALUE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    private boolean combat_styles$inherited(Field field) {
        return inherit != null
                && inherit.mode(combat_styles$groupOf(field.sectionKey())) == InheritMode.PARENT;
    }

    private void renderRow(GuiGraphics graphics, Field field, double raw, int x, int y,
                           int listWidth, int index, int mouseX, int mouseY) {
        boolean locked = combat_styles$inherited(field);
        double value = locked ? field.base() : raw;
        boolean changed = !locked && value != field.base();
        if ((index & 1) == 1) {
            graphics.fill(x, y, x + listWidth - SCROLLBAR_WIDTH - 2, y + ROW_HEIGHT, COLOR_ROW_ALT);
        }
        if (changed) {
            graphics.fill(x, y, x + 2, y + ROW_HEIGHT, COLOR_CHANGED);
        }

        graphics.drawString(font, Component.translatable(field.labelKey()),
                x + 6, y + 6, changed ? COLOR_CHANGED : COLOR_TEXT, false);

        int right = x + listWidth - SCROLLBAR_WIDTH - 6 - (isAttribute(field) ? REMOVE_GAP + 8 : 0);
        int minusX = right - STEP_WIDTH * 2 - 4;
        int plusX = right - STEP_WIDTH;

        if (isAttribute(field)) {
            AttributeModifier.Operation operation = operations.getOrDefault(
                    attributeIdOf(field), AttributeModifier.Operation.ADD_VALUE);
            int opX = minusX - OP_WIDTH - 4;
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, opX, y + 3, OP_WIDTH, ROW_HEIGHT - 6);
            graphics.fill(opX, y + 3, opX + OP_WIDTH, y + ROW_HEIGHT - 3,
                    hovered ? 0x40FFFFFF : 0x20FFFFFF);
            String label = operationLabel(operation);
            graphics.drawString(font, label, opX + OP_WIDTH / 2 - font.width(label) / 2, y + 6,
                    hovered ? COLOR_ACCENT : COLOR_MUTED, false);
        }

        if (isAttribute(field)) {
            boolean removeHover = ScreenDraw.isWithin(mouseX, mouseY, x + listWidth - SCROLLBAR_WIDTH - 12,
                    y + 4, REMOVE_WIDTH, ROW_HEIGHT - 8);
            graphics.drawString(font, REMOVE_LABEL, x + listWidth - SCROLLBAR_WIDTH - 10, y + 6,
                    removeHover ? COLOR_BAD : COLOR_MUTED, false);
        }

        if (index == editingRow) {
            valueBox.setX(minusX - OP_WIDTH - 74);
            valueBox.setY(y + 4);
            return;
        }

        int valueRight = isAttribute(field) ? minusX - OP_WIDTH - 10 : minusX - 8;
        String shown = StyleText.formatDecimal(value);
        int valueLeft = valueRight - font.width(shown);
        if (!locked) {
            ScreenDraw.hoverField(graphics, valueLeft, y + 3, font.width(shown),
                    ROW_HEIGHT - 6, mouseX, mouseY);
        }
        graphics.drawString(font, shown, valueLeft, y + 6,
                locked ? COLOR_LOCKED : changed ? COLOR_CHANGED : COLOR_MUTED, false);

        if (locked) {
            return;
        }
        drawStep(graphics, minusX, y, "-", mouseX, mouseY);
        drawStep(graphics, plusX, y, "+", mouseX, mouseY);
    }

    private void drawStep(GuiGraphics graphics, int x, int y, String label, int mouseX, int mouseY) {
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y + 3, STEP_WIDTH, ROW_HEIGHT - 6);
        graphics.fill(x, y + 3, x + STEP_WIDTH, y + ROW_HEIGHT - 3,
                hovered ? 0x40FFFFFF : 0x20FFFFFF);
        graphics.drawString(font, label, x + STEP_WIDTH / 2 - font.width(label) / 2, y + 6,
                hovered ? COLOR_ACCENT : COLOR_TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (combat_styles$clickScrollbar(mouseX, mouseY)) {
            return true;
        }
        if (combat_styles$clickInherit(mouseX, mouseY)) {
            return true;
        }
        if (combat_styles$clickAffixes(mouseX, mouseY)) {
            return true;
        }
        if (combat_styles$clickIdentity(mouseX, mouseY)) {
            return true;
        }
        if (nameBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(nameBox);
            return true;
        }
        if (valueBox.isVisible() && valueBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(valueBox);
            return true;
        }
        if (editingRow >= 0) {
            commitEdit();
        }
        if (clickedAddAttribute(mouseX, mouseY)) {
            openAttributePicker();
            return true;
        }

        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        int listTop = editorListTop();
        if (outsideList(mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        String section = null;
        int y = listTop - scroll;
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (!field.sectionKey().equals(section)) {
                section = field.sectionKey();
                y += SECTION_HEIGHT;
            }
            int right = x + listWidth - SCROLLBAR_WIDTH - 6
                    - (isAttribute(field) ? REMOVE_GAP + 8 : 0);
            int minusX = right - STEP_WIDTH * 2 - 4;
            int plusX = right - STEP_WIDTH;

            if (isAttribute(field) && ScreenDraw.isWithin((int) mouseX, (int) mouseY,
                    x + listWidth - SCROLLBAR_WIDTH - 12, y + 4, REMOVE_WIDTH, ROW_HEIGHT - 8)) {
                removeAttributeField(i);
                return true;
            }
            int valueRight = isAttribute(field) ? minusX - OP_WIDTH - 10 : minusX - 8;
            int valueLeft = minusX - OP_WIDTH - 74;
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, valueLeft, y + 3,
                    Math.max(1, valueRight - valueLeft), ROW_HEIGHT - 6)) {
                beginEdit(i);
                return true;
            }
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, minusX, y + 3, STEP_WIDTH, ROW_HEIGHT - 6)) {
                step(i, field, hasShiftDown() ? -5 : -1);
                return true;
            }
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, plusX, y + 3, STEP_WIDTH, ROW_HEIGHT - 6)) {
                step(i, field, hasShiftDown() ? 5 : 1);
                return true;
            }
            if (isAttribute(field)) {
                int opX = minusX - OP_WIDTH - 4;
                if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, opX, y + 3, OP_WIDTH, ROW_HEIGHT - 6)) {
                    String id = attributeIdOf(field);
                    AttributeModifier.Operation from = operations.getOrDefault(id,
                            AttributeModifier.Operation.ADD_VALUE);
                    AttributeModifier.Operation to = nextOperation(from);
                    operations.put(id, to);
                    values.set(i, toDisplayed(toStored(values.get(i), from), to));
                    return true;
                }
            }
            y += ROW_HEIGHT;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void step(int index, Field field, int direction) {
        if (combat_styles$inherited(field)) {
            return;
        }
        double multiplier = hasShiftDown() ? 5.0D : 1.0D;
        double next = values.get(index) + field.step() * direction * multiplier;
        next = Math.clamp(next, field.min(), field.max());
        values.set(index, Math.round(next * 1000.0D) / 1000.0D);
    }

    private int editorListTop() {
        return top + HEADER;
    }

    private int editorListBottom() {
        return top + PANEL_HEIGHT - FOOTER - LIST_BOTTOM_GAP;
    }

    private boolean outsideList(double mouseY) {
        return mouseY < editorListTop() || mouseY > editorListBottom();
    }

    private boolean clickedAddAttribute(double mouseX, double mouseY) {
        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        int listTop = editorListTop();
        if (outsideList(mouseY)) {
            return false;
        }
        String section = null;
        int y = listTop - scroll;
        for (Field field : fields) {
            if (!field.sectionKey().equals(section)) {
                section = field.sectionKey();
                y += SECTION_HEIGHT;
            }
            y += ROW_HEIGHT;
        }
        return ScreenDraw.isWithin((int) mouseX, (int) mouseY, x, y,
                listWidth - SCROLLBAR_WIDTH - 2, ROW_HEIGHT);
    }

    private String combat_styles$hitboxLabel() {
        return hitboxChoice.isBlank()
                ? Component.translatable(KEY_INHERITED).getString()
                : StyleText.prettify(hitboxChoice);
    }

    private String combat_styles$soundLabel() {
        if (soundChoice.isBlank()) {
            return Component.translatable(KEY_INHERITED).getString();
        }
        return AttackOverride.isSilent(soundChoice)
                ? Component.translatable(KEY_NO_SOUND).getString()
                : StyleText.soundName(soundChoice);
    }

    private String combat_styles$damageTypeLabel() {
        return damageTypeChoice.isBlank()
                ? Component.translatable(KEY_PHYSICAL).getString()
                : StyleText.prettify(damageTypeChoice);
    }

    private int combat_styles$identityRows() {
        return stylePageShown ? IDENTITY_ROWS + 1 : IDENTITY_ROWS;
    }

    private boolean combat_styles$natural() {
        return stylePageChoice != null
                ? stylePageChoice
                : StylePool.definedNatural(page.styleId());
    }

    private String combat_styles$conditionsLabel() {
        List<String> shown = conditionChoice == null ? inheritedConditions : conditionChoice;
        String names = shown.isEmpty()
                ? Component.translatable(KEY_NO_CONDITIONS).getString()
                : StyleText.conditionNames(AttackOverride.conditionsOf(shown)).getString();
        return conditionChoice == null
                ? names
                : Component.translatable(KEY_CONDITIONS_CHANGED, names).getString();
    }

    private void combat_styles$openConditions() {
        List<String> current = conditionChoice == null ? inheritedConditions : conditionChoice;
        minecraft.setScreen(new MultiChoiceScreen(this, Component.translatable(KEY_CONDITIONS),
                AttackConditions.names(), current,
                picked -> conditionChoice = AttackOverride.sameConditions(picked,
                        inheritedConditions) ? null : List.copyOf(picked))
                .withDefaults(inheritedConditions)
                .withLabels(name -> StyleText.conditionName(AttackOverride.conditionNamed(name)))
                .withHints(name -> List.of(Component.translatable(
                        CONDITION_HINT_PREFIX + name.toLowerCase(Locale.ROOT)))));
    }

    private void combat_styles$identityRow(GuiGraphics graphics, String labelKey, String value,
                                           int x, int y, int listWidth,
                                           int mouseX, int mouseY) {
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y,
                listWidth - SCROLLBAR_WIDTH - 2, ROW_HEIGHT);
        ScreenDraw.rowBackground(graphics, x, y, listWidth - SCROLLBAR_WIDTH - 2,
                ROW_HEIGHT, 0, hovered, false);
        graphics.drawString(font, Component.translatable(labelKey), x + 6, y + 6,
                COLOR_TEXT, false);
        int right = x + listWidth - SCROLLBAR_WIDTH - 8;
        ScreenDraw.hoverField(graphics, right - font.width(value), y + 3,
                font.width(value), ROW_HEIGHT - 6, mouseX, mouseY);
        graphics.drawString(font, value, right - font.width(value), y + 6,
                hovered ? COLOR_ACCENT : COLOR_MUTED, false);
    }

    private void combat_styles$affixRow(GuiGraphics graphics, String affix,
                                        int x, int y, int listWidth,
                                        int mouseX, int mouseY) {
        int width = listWidth - SCROLLBAR_WIDTH - 2;
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y, width, ROW_HEIGHT);
        ScreenDraw.rowBackground(graphics, x, y, width, ROW_HEIGHT, 0, hovered, false);
        boolean known = AffixRegistry.get(affix) != null;
        boolean tuned = ownTuning != null && ownTuning.containsKey(affix);
        graphics.drawString(font,
                ScreenDraw.trim(font, StyleText.prettify(affix), width - REMOVE_WIDTH - 12),
                x + 6, y + 6,
                !known ? COLOR_BAD : tuned ? COLOR_CHANGED : COLOR_TEXT, false);
        int removeX = x + width - REMOVE_WIDTH - 4;
        boolean removeHover = ScreenDraw.isWithin(mouseX, mouseY, removeX, y + 4,
                REMOVE_WIDTH, ROW_HEIGHT - 8);
        graphics.drawString(font, REMOVE_LABEL, removeX, y + 6,
                removeHover ? COLOR_BAD : COLOR_MUTED, false);
    }

    private void combat_styles$addAffixRow(GuiGraphics graphics, int x, int y, int listWidth,
                                           int mouseX, int mouseY) {
        int width = listWidth - SCROLLBAR_WIDTH - 2;
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y, width, ROW_HEIGHT);
        ScreenDraw.rowBackground(graphics, x, y, width, ROW_HEIGHT, 0, hovered, false);
        Component label = Component.translatable(KEY_ADD_AFFIX_ROW);
        ScreenDraw.hoverField(graphics, x + 6, y + 3, font.width(label), ROW_HEIGHT - 6,
                mouseX, mouseY);
        graphics.drawString(font, label, x + 6, y + 6,
                hovered ? COLOR_ACCENT : COLOR_MUTED, false);
    }

    private boolean combat_styles$hasAffixSection() {
        return inherit != null || ownAffixes != null;
    }

    private int combat_styles$affixTop() {
        int y = combat_styles$identityTop() + ROW_HEIGHT * combat_styles$identityRows();
        if (combat_styles$hasAffixSection()) {
            y += SECTION_HEIGHT;
        }
        return y;
    }

    private boolean combat_styles$clickAffixes(double mouseX, double mouseY) {
        if (ownAffixes == null) {
            return false;
        }
        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        if (outsideList(mouseY)) {
            return false;
        }
        int width = listWidth - SCROLLBAR_WIDTH - 2;
        int y = combat_styles$affixTop();
        for (int index = 0; index < ownAffixes.size(); index++) {
            if (ScreenDraw.isWithin(mouseX, mouseY,
                    x + width - REMOVE_WIDTH - 4, y + 4, REMOVE_WIDTH, ROW_HEIGHT - 8)) {
                ownAffixes.remove(index);
                combat_styles$pushAffixes();
                return true;
            }
            if (ScreenDraw.isWithin(mouseX, mouseY, x, y, width, ROW_HEIGHT)) {
                combat_styles$openAffixTuning(ownAffixes.get(index));
                return true;
            }
            y += ROW_HEIGHT;
        }
        if (ScreenDraw.isWithin(mouseX, mouseY, x, y, width, ROW_HEIGHT)) {
            minecraft.setScreen(new EquipmentAffixPickerScreen(this, ownAffixes, picked -> {
                if (picked != null && !ownAffixes.contains(picked)) {
                    ownAffixes.add(picked);
                    combat_styles$pushAffixes();
                }
            }));
            return true;
        }
        return false;
    }

    private List<ExtraAttack> combat_styles$extras() {
        List<ExtraAttack> found = new ArrayList<>();
        for (SlashPlacement slash : slashes) {
            if (slash.mode().dealsDamage()) {
                found.add(slash.asAttack());
            }
        }
        return found;
    }

    private void combat_styles$applyExtraBoxes(int index, HitboxSet applied) {
        int seen = 0;
        for (int at = 0; at < slashes.size(); at++) {
            SlashPlacement slash = slashes.get(at);
            if (!slash.mode().dealsDamage()) {
                continue;
            }
            if (seen == index) {
                slashes.set(at, slash.withAttack(
                        slash.asAttack().withBoxes(applied)));
                return;
            }
            seen++;
        }
    }

    private double combat_styles$valueOf(String key, double fallback) {
        for (int index = 0; index < fields.size(); index++) {
            if (fields.get(index).key().equals(key)) {
                return values.get(index);
            }
        }
        return fallback;
    }

    private void combat_styles$pushAffixes() {
        if (onAffixes != null) {
            onAffixes.accept(List.copyOf(ownAffixes));
        }
    }

    private void combat_styles$openAffixTuning(String affixId) {
        StyleAffix target = AffixRegistry.get(affixId);
        if (target == null || ownTuning == null) {
            return;
        }
        minecraft.setScreen(new AffixTuningScreen(this, target, ownTuning.get(affixId),
                result -> {
                    if (result == null) {
                        ownTuning.remove(affixId);
                    } else {
                        ownTuning.put(affixId, result);
                    }
                    if (onTuning != null) {
                        onTuning.accept(Map.copyOf(ownTuning));
                    }
                }));
    }

    private int combat_styles$identityTop() {
        int y = top + HEADER - scroll;
        String section = null;
        for (Field field : fields) {
            if (!field.sectionKey().equals(section)) {
                section = field.sectionKey();
                y += SECTION_HEIGHT;
            }
            y += ROW_HEIGHT;
        }
        return y + ROW_HEIGHT + SECTION_HEIGHT;
    }

    private boolean combat_styles$clickIdentity(double mouseX, double mouseY) {
        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        if (outsideList(mouseY)) {
            return false;
        }
        int y = combat_styles$identityTop();
        int width = listWidth - SCROLLBAR_WIDTH - 2;
        if (ScreenDraw.isWithin(mouseX, mouseY, x, y, width, ROW_HEIGHT)) {
            int next = indexOf(HITBOX_TYPES, hitboxChoice) + 1;
            hitboxChoice = HITBOX_TYPES[Math.floorMod(next, HITBOX_TYPES.length)];
            return true;
        }
        if (ScreenDraw.isWithin(mouseX, mouseY, x, y + ROW_HEIGHT, width, ROW_HEIGHT)) {
            minecraft.setScreen(new SoundPickerScreen(this, page, soundChoice,
                    picked -> soundChoice = picked == null ? "" : picked));
            return true;
        }
        if (ScreenDraw.isWithin(mouseX, mouseY, x, y + ROW_HEIGHT * 2, width, ROW_HEIGHT)) {
            minecraft.setScreen(new ChoicePickerScreen(this,
                    Component.translatable(KEY_DAMAGE_TYPE), EffectChoices.damageTypeIds(),
                    damageTypeChoice,
                    picked -> damageTypeChoice = picked == null ? "" : picked));
            return true;
        }
        if (ScreenDraw.isWithin(mouseX, mouseY, x, y + ROW_HEIGHT * 3, width, ROW_HEIGHT)) {
            combat_styles$openConditions();
            return true;
        }
        if (stylePageShown
                && ScreenDraw.isWithin(mouseX, mouseY, x, y + ROW_HEIGHT * IDENTITY_ROWS, width, ROW_HEIGHT)) {
            stylePageChoice = !combat_styles$natural();
            return true;
        }
        return false;
    }

    private static int indexOf(String[] values, String value) {
        for (int index = 0; index < values.length; index++) {
            if (values[index].equals(value)) {
                return index;
            }
        }
        return 0;
    }

    private boolean combat_styles$clickInherit(double mouseX, double mouseY) {
        if (inherit == null) {
            return false;
        }
        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        int listTop = editorListTop();
        if (outsideList(mouseY)) {
            return false;
        }
        String section = null;
        int y = listTop - scroll;
        for (Field field : fields) {
            if (!field.sectionKey().equals(section)) {
                section = field.sectionKey();
                if (ScreenDraw.isWithin((int) mouseX, (int) mouseY,
                        combat_styles$inheritX(x, listWidth), y,
                        INHERIT_WIDTH, SECTION_HEIGHT)) {
                    inherit = inherit.toggle(combat_styles$groupOf(section));
                    if (onInherit != null) {
                        onInherit.accept(inherit);
                    }
                    return true;
                }
                y += SECTION_HEIGHT;
            }
            y += ROW_HEIGHT;
        }
        y += ROW_HEIGHT;
        if (combat_styles$clickSection(mouseX, mouseY, SECTION_IDENTITY, x, listWidth, y)) {
            return true;
        }
        y += SECTION_HEIGHT + ROW_HEIGHT * combat_styles$identityRows();
        return combat_styles$clickSection(mouseX, mouseY, SECTION_AFFIXES, x, listWidth, y);
    }

    private boolean combat_styles$clickSection(double mouseX, double mouseY, String section,
                                               int x, int listWidth, int y) {
        if (!ScreenDraw.isWithin((int) mouseX, (int) mouseY,
                combat_styles$inheritX(x, listWidth), y, INHERIT_WIDTH, SECTION_HEIGHT)) {
            return false;
        }
        inherit = inherit.toggle(combat_styles$groupOf(section));
        if (onInherit != null) {
            onInherit.accept(inherit);
        }
        return true;
    }

    protected int rowAt(double mouseX, double mouseY) {
        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        int listTop = editorListTop();
        if (outsideList(mouseY)) {
            return -1;
        }
        String section = null;
        int y = listTop - scroll;
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            if (!field.sectionKey().equals(section)) {
                section = field.sectionKey();
                y += SECTION_HEIGHT;
            }
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, x, y, listWidth, ROW_HEIGHT)) {
                return i;
            }
            y += ROW_HEIGHT;
        }
        return -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int row = rowAt(mouseX, mouseY);
        if (hasShiftDown() && row >= 0) {
            step(row, fields.get(row), (int) Math.signum(scrollY));
            return true;
        }
        scroll = Math.clamp(scroll - (int) (scrollY * ROW_HEIGHT), 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingRow >= 0) {
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
        if (nameBox.isFocused() && keyCode != KEY_ESCAPE) {
            return nameBox.keyPressed(keyCode, scanCode, modifiers)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == KEY_ENTER) {
            apply();
            return true;
        }
        if (keyCode == KEY_R && hasControlDown()) {
            resetAll();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (editingRow >= 0) {
            return valueBox.charTyped(character, modifiers);
        }
        if (nameBox.isFocused()) {
            return nameBox.charTyped(character, modifiers);
        }
        return super.charTyped(character, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
