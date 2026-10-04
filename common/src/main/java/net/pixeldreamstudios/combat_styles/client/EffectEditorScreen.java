package net.pixeldreamstudios.combat_styles.client;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.damage.DamageBonusList;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.equipment.AttributeBonus;
import net.pixeldreamstudios.combat_styles.equipment.EffectCooldown;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentEffect;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentEffects;
import net.pixeldreamstudios.combat_styles.equipment.SpellLocks;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentTrigger;
import net.pixeldreamstudios.combat_styles.equipment.EventKind;
import net.pixeldreamstudios.combat_styles.equipment.EventPayloads;
import net.pixeldreamstudios.combat_styles.equipment.StyleSubstitution;
import net.pixeldreamstudios.combat_styles.equipment.SummonDive;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;
import net.pixeldreamstudios.combat_styles.equipment.SelfEffectList;
import net.pixeldreamstudios.combat_styles.equipment.TooltipOverride;
import net.pixeldreamstudios.combat_styles.equipment.TriggeredEffect;
import net.pixeldreamstudios.combat_styles.style.DeclaredAttack;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EffectEditorScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 340;
    private static final int MAX_VISIBLE = 12;
    private static final int HEADER = 62;
    private static final int FOOTER = 30;
    private static final int ROW_HEIGHT = 19;
    private static final int BUTTON_WIDTH = 70;
    private static final int GAP = 4;
    private static final int TITLE_Y = 10;
    private static final int NAME_Y = 26;
    private static final int NAME_HEIGHT = 14;
    private static final int LABEL_INSET = 6;
    private static final int TEXT_INSET = 5;
    private static final int NAME_MAX = 48;
    private static final int SCROLL_STEP = 1;
    private static final int COOLDOWN_WIDTH = 60;
    private static final int COOLDOWN_MAX = 6;
    private static final int CLEAR_WIDTH = 14;
    private static final int KEY_ENTER = 257;
    private static final int KEY_NUMPAD_ENTER = 335;

    private static final String KEY_TITLE = "screen.combat_styles.effect_editor";
    private static final String KEY_NAME = "screen.combat_styles.effect_name";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_EDIT = "screen.combat_styles.slash_edit";
    private static final String KEY_NONE = "screen.combat_styles.value_none";
    private static final String KEY_SET = "screen.combat_styles.value_set";
    private static final String KEY_CLEAR_HINT = "screen.combat_styles.effect_clear_hint";
    private static final String HINT_SUFFIX = ".hint";
    private static final String AUGMENT_BASE_ID = "any_attack";
    private static final String KEY_AUGMENT_SCOPE = "screen.combat_styles.augment_scope";
    private static final String KEY_CLEAR = "screen.combat_styles.slash_remove";
    private static final String KEY_NEEDS_TARGET = "screen.combat_styles.effect_needs_target";
    private static final String KEY_NEEDS_ARMING = "screen.combat_styles.effect_needs_arming";
    private static final String KEY_LIVE_WHILE_USING = "screen.combat_styles.effect_live_while_using";
    private static final String KEY_SWING_GATE_IGNORED = "screen.combat_styles.effect_swing_gate_ignored";
    private static final String KEY_NEEDS_EDGE_WINDOW = "screen.combat_styles.effect_needs_edge_window";
    private static final String KEY_ARMS_ON_SWING = "screen.combat_styles.effect_arms_on_swing";
    private static final String KEY_LIVE_WHILE_SWING = "screen.combat_styles.effect_live_while_swing";
    private static final String KEY_USE_SUBSTITUTION = "screen.combat_styles.effect_use_substitution";

    private static final String ROW_TRIGGER = "screen.combat_styles.effect_row_trigger";
    private static final String ROW_COOLDOWN = "screen.combat_styles.effect_row_cooldown";
    private static final String ROW_SUBSTITUTE = "screen.combat_styles.effect_row_substitute";
    private static final String ROW_SUBSTITUTION = "screen.combat_styles.effect_row_substitution";
    private static final String ROW_AUGMENT = "screen.combat_styles.effect_row_augment";
    private static final String ROW_GRANTS = "screen.combat_styles.effect_row_grants";
    private static final String ROW_TUNES = "screen.combat_styles.effect_row_tunes";
    private static final String ROW_ATTRS = "screen.combat_styles.effect_row_attributes";
    private static final String ROW_DAMAGE_BONUS = "screen.combat_styles.effect_row_damage_bonus";
    private static final Codec<Map<String, AttributeBonus>> ATTRIBUTE_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, AttributeBonus.EXPANDED);
    private static final String ROW_ON_SWING = "screen.combat_styles.effect_row_on_swing";
    private static final String ROW_ON_HIT = "screen.combat_styles.effect_row_on_hit";
    private static final String ROW_EVENTS = "screen.combat_styles.effect_row_events";
    private static final String ROW_ECHO = "screen.combat_styles.effect_row_echo";
    private static final String ROW_SPELLS = "screen.combat_styles.effect_row_spells";
    private static final String ROW_RESET = "screen.combat_styles.effect_row_reset";
    private static final String ROW_SUMMON_PROFILE = "screen.combat_styles.effect_row_summon_profile";
    private static final String ROW_SUMMON_DIVE = "screen.combat_styles.effect_row_summon_dive";
    private static final String ROW_HIDE_TOOLTIP = "screen.combat_styles.effect_row_hide_tooltip";
    private static final String ROW_TOOLTIP_TEXT = "screen.combat_styles.effect_row_tooltip_text";
    private static final String ROW_DELAY = "screen.combat_styles.effect_row_delay";
    private static final String ROW_SELF_EFFECTS = "screen.combat_styles.effect_row_self_effects";

    private static final StyleSubstitution BLANK_SUBSTITUTION = new StyleSubstitution(
            List.of(), List.of(), StyleSubstitution.MODE_REPLACE_ALL, List.of(), List.of(),
            List.of(), StyleSubstitution.DEFAULT_LINGER_TICKS,
            StyleSubstitution.NO_CHARGE_DECAY, Map.of());

    private static final Set<String> REST_ROWS = Set.of(ROW_TRIGGER, ROW_COOLDOWN,
            ROW_SUBSTITUTE, ROW_SUBSTITUTION, ROW_ATTRS, ROW_SPELLS, ROW_DAMAGE_BONUS);

    private static final String[] ROWS = rows();

    private static String[] rows() {
        List<String> rows = new ArrayList<>(List.of(
                ROW_TRIGGER, ROW_COOLDOWN, ROW_SUBSTITUTE, ROW_SUBSTITUTION, ROW_AUGMENT,
                ROW_GRANTS, ROW_TUNES, ROW_ATTRS, ROW_DAMAGE_BONUS, ROW_ON_SWING, ROW_ON_HIT,
                ROW_EVENTS, ROW_ECHO,
                ROW_RESET, ROW_SUMMON_PROFILE, ROW_SUMMON_DIVE,
                ROW_HIDE_TOOLTIP, ROW_TOOLTIP_TEXT, ROW_DELAY, ROW_SELF_EFFECTS));
        if (SpellEngineCompat.isLoaded()) {
            rows.add(rows.indexOf(ROW_ECHO), ROW_SPELLS);
        }
        return rows.toArray(String[]::new);
    }

    private final BiConsumer<String, TriggeredEffect> onApply;
    private final String originalName;

    private TriggeredEffect effect;
    private EditBox nameBox;
    private EditBox cooldownBox;
    private boolean editingCooldown;
    private String editingRow;
    private List<Component> tooltip;

    public EffectEditorScreen(Screen parent, String name, TriggeredEffect effect,
                              BiConsumer<String, TriggeredEffect> onApply) {
        super(Component.translatable(KEY_TITLE), parent);
        this.originalName = name;
        this.effect = effect;
        this.onApply = onApply;
    }

    @Override
    protected int panelWidth() {
        return PANEL_WIDTH;
    }

    @Override
    protected int panelHeight() {
        return HEADER + Math.min(ROWS.length, MAX_VISIBLE) * ROW_HEIGHT + FOOTER;
    }

    @Override
    protected int rowHeight() {
        return ROW_HEIGHT;
    }

    @Override
    protected int rowCount() {
        return ROWS.length;
    }

    @Override
    protected void layout() {
        nameBox = new FieldBox(font, left + ScreenTheme.PADDING + 46, top + NAME_Y,
                PANEL_WIDTH - ScreenTheme.PADDING * 2 - 46, NAME_HEIGHT, Component.empty());
        nameBox.setMaxLength(NAME_MAX);
        nameBox.setValue(originalName);
        addRenderableWidget(nameBox);

        cooldownBox = new FieldBox(font, left + PANEL_WIDTH - ScreenTheme.PADDING - COOLDOWN_WIDTH,
                listTop(), COOLDOWN_WIDTH, NAME_HEIGHT, Component.empty());
        cooldownBox.setMaxLength(COOLDOWN_MAX);
        cooldownBox.setVisible(false);
        addRenderableWidget(cooldownBox);

        int buttonY = top + panelHeight() - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH * 2 - GAP,
                        buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY), button -> {
            commitCooldown();
            String name = nameBox.getValue().trim();
            onApply.accept(name.isBlank() ? originalName : name, effect);
            minecraft.setScreen(parent);
        }).bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH, buttonY,
                BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private List<Component> restNotes(String row) {
        if (!REST_ROWS.contains(row)) {
            return List.of();
        }
        List<Component> notes = new ArrayList<>();
        if (effect.needsArming()) {
            notes.add(Component.translatable(KEY_NEEDS_ARMING));
        } else if (effect.needsEdgeWindow()) {
            notes.add(Component.translatable(KEY_NEEDS_EDGE_WINDOW));
        } else if (effect.liveWhileUsing()) {
            notes.add(Component.translatable(KEY_LIVE_WHILE_USING));
            if (EquipmentDefinition.substitutes(payload())) {
                notes.add(Component.translatable(KEY_USE_SUBSTITUTION));
            }
        }
        if (effect.armsOnSwing()) {
            notes.add(Component.translatable(KEY_ARMS_ON_SWING));
        } else if (effect.liveWhileSwingMatches()) {
            notes.add(Component.translatable(KEY_LIVE_WHILE_SWING));
        }
        if (effect.swingGateIgnored()) {
            notes.add(Component.translatable(KEY_SWING_GATE_IGNORED));
        }
        return notes;
    }

    private EquipmentEffect payload() {
        return effect.effect();
    }

    private void setPayload(EquipmentEffect updated) {
        effect = effect.withEffect(updated);
    }

    protected int listTop() {
        return top + HEADER;
    }

    protected int listBottom() {
        return top + panelHeight() - FOOTER;
    }

    protected int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_HEIGHT);
    }

    protected int rowY(int index) {
        return listTop() + (index - scroll) * ROW_HEIGHT;
    }

    protected int rowAt(double mouseX, double mouseY) {
        int first = scroll;
        int last = Math.min(ROWS.length, first + visibleRows());
        for (int index = first; index < last; index++) {
            if (ScreenDraw.isWithin(mouseX, mouseY, left + ScreenTheme.PADDING, rowY(index),
                    PANEL_WIDTH - ScreenTheme.PADDING * 2, ROW_HEIGHT)) {
                return index;
            }
        }
        return -1;
    }

    private Component valueOf(String row) {
        EquipmentEffect payload = payload();
        return switch (row) {
            case ROW_TRIGGER -> Component.translatable(KEY_EDIT);
            case ROW_COOLDOWN -> cooldownLabel();
            case ROW_SUBSTITUTE -> text(payload.substituteStyle().orElse(null));
            case ROW_SUBSTITUTION -> flag(payload.substitution().isPresent());
            case ROW_AUGMENT -> flag(payload.augment().isPresent());
            case ROW_GRANTS -> count(payload.grantAffixes().size());
            case ROW_TUNES -> count(payload.tuneAffixes().size());
            case ROW_ATTRS -> count(payload.attributes().size());
            case ROW_DAMAGE_BONUS -> count(payload.damageBonus().size());
            case ROW_ON_SWING -> flag(payload.onSwing().isPresent());
            case ROW_ON_HIT -> flag(payload.onHit().isPresent());
            case ROW_EVENTS -> count(payload.events().payloads().size());
            case ROW_ECHO -> flag(payload.echo().isPresent());
            case ROW_SPELLS -> flag(!payload.spells().isEmpty());
            case ROW_RESET -> count(payload.resetCombo().orElse(0));
            case ROW_HIDE_TOOLTIP -> flag(effect.hideTooltip());
            case ROW_TOOLTIP_TEXT -> flag(!effect.tooltip().isEmpty());
            case ROW_DELAY -> delayLabel();
            case ROW_SELF_EFFECTS -> count(effect.selfEffects().size());
            case ROW_SUMMON_PROFILE -> flag(payload.summonProfile().isPresent());
            case ROW_SUMMON_DIVE -> flag(payload.summonDive().isPresent());
            default -> Component.empty();
        };
    }

    private Component text(String value) {
        return value == null || value.isBlank()
                ? Component.translatable(KEY_NONE)
                : StyleText.choiceName(value);
    }

    private Component flag(boolean value) {
        return Component.translatable(value ? KEY_SET : KEY_NONE);
    }

    private Component count(int value) {
        return value <= 0
                ? Component.translatable(KEY_NONE)
                : Component.literal(String.valueOf(value));
    }

    private List<Component> tooltipFor(String row) {
        EquipmentEffect payload = payload();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(row).copy().withStyle(ChatFormatting.WHITE));
        lines.add(Component.translatable(row + HINT_SUFFIX)
                .copy().withStyle(ChatFormatting.GRAY));

        if (row.equals(ROW_TRIGGER) && effect.trigger().requiresTarget()
                && payload.onHit().isEmpty()) {
            lines.add(Component.translatable(KEY_NEEDS_TARGET)
                    .copy().withStyle(ChatFormatting.GOLD));
        }
        for (Component note : restNotes(row)) {
            lines.add(Component.literal(FieldDependency.WINDOW.marker() + " ")
                    .withColor(FieldDependency.WINDOW.color())
                    .append(note.copy().withStyle(ChatFormatting.GOLD)));
        }

        if (row.equals(ROW_EVENTS)) {
            for (EventKind kind : EventKind.values()) {
                if (payload.events().has(kind)) {
                    lines.add(Component.translatable(kind.labelKey())
                            .copy().withStyle(ChatFormatting.AQUA));
                }
            }
        }
        switch (row) {
            case ROW_SUBSTITUTE -> payload.substituteStyle()
                    .ifPresent(style -> lines.addAll(RefTooltip.forStyle(style)));
            case ROW_GRANTS -> {
                for (String affix : payload.grantAffixes()) {
                    lines.addAll(RefTooltip.forAffix(affix, payload.tuneAffixes().get(affix)));
                }
            }
            case ROW_TUNES -> {
                for (Map.Entry<String, AffixTuning> tuned : payload.tuneAffixes().entrySet()) {
                    lines.addAll(RefTooltip.forAffix(tuned.getKey(), tuned.getValue()));
                }
            }
            case ROW_SUMMON_PROFILE -> payload.summonProfile()
                    .flatMap(SummonProfile::entity)
                    .ifPresent(entity -> lines.addAll(RefTooltip.forEntity(entity)));
            default -> {
            }
        }

        if (isSet(row)) {
            lines.add(Component.translatable(KEY_CLEAR_HINT)
                    .copy().withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (beginScrollDrag(mouseX, mouseY)) {
            return true;
        }
        if (editingCooldown && cooldownBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(cooldownBox);
            return true;
        }
        commitCooldown();
        if (nameBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(nameBox);
            return true;
        }
        int index = rowAt(mouseX, mouseY);
        if (index < 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        String row = ROWS[index];
        if (isSet(row) && ScreenDraw.isWithin(mouseX, mouseY, clearX(), rowY(index),
                CLEAR_WIDTH, ROW_HEIGHT)) {
            clearRow(row);
            return true;
        }
        openRow(row);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingCooldown && (keyCode == KEY_ENTER || keyCode == KEY_NUMPAD_ENTER)) {
            commitCooldown();
            return true;
        }
        if (editingCooldown && keyCode == KEY_ESCAPE) {
            editingCooldown = false;
            cooldownBox.setVisible(false);
            setFocused(null);
            return true;
        }
        if (keyCode == KEY_ESCAPE) {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void openRow(String row) {
        EquipmentEffect payload = payload();
        switch (row) {
            case ROW_COOLDOWN -> openCooldown();
            case ROW_RESET -> beginRepeatEdit();
            case ROW_HIDE_TOOLTIP -> effect = effect.withHideTooltip(!effect.hideTooltip());
            case ROW_TOOLTIP_TEXT -> openTooltipText();
            case ROW_DELAY -> beginDelayEdit();
            case ROW_SELF_EFFECTS -> openSelfEffects();
            case ROW_AUGMENT -> openAugment();
            case ROW_SUBSTITUTE -> openSubstitute();
            case ROW_TRIGGER -> openTree(ROW_TRIGGER, EquipmentTrigger.CODEC,
                    effect.trigger(), applied -> effect = effect.withTrigger(applied.orElse(EquipmentTrigger.ALWAYS)));
            case ROW_SUBSTITUTION -> minecraft.setScreen(new SubstitutionEditorScreen(this,
                    payload.substitution().orElse(BLANK_SUBSTITUTION),
                    applied -> setPayload(EquipmentEffects.withSubstitution(payload(), applied))));
            case ROW_GRANTS -> openGrants();
            case ROW_TUNES -> openTunes();
            case ROW_DAMAGE_BONUS -> openDamageBonus();
            case ROW_ATTRS -> openTree(ROW_ATTRS, ATTRIBUTE_MAP_CODEC,
                    Map.copyOf(payload.attributes()),
                    applied -> setPayload(EquipmentEffects.withAttributes(payload(),
                            applied.orElse(Map.of()))));
            case ROW_ON_SWING -> openTree(ROW_ON_SWING, AffixEffects.OnHit.CODEC,
                    payload.onSwing().orElse(AffixEffects.OnHit.EMPTY),
                    applied -> setPayload(EquipmentEffects.withOnSwing(payload(), applied)));
            case ROW_ON_HIT -> openTree(ROW_ON_HIT, AffixEffects.OnHit.CODEC,
                    payload.onHit().orElse(AffixEffects.OnHit.EMPTY),
                    applied -> setPayload(EquipmentEffects.withOnHit(payload(), applied)));
            case ROW_EVENTS -> openEvents();
            case ROW_ECHO -> openTree(ROW_ECHO, AffixEffects.Echo.CODEC,
                    payload.echo().orElse(null),
                    applied -> setPayload(EquipmentEffects.withEcho(payload(), applied)));
            case ROW_SPELLS -> openTree(ROW_SPELLS, SpellLocks.CODEC, payload.spells(),
                    applied -> setPayload(EquipmentEffects.withSpells(payload(),
                            applied.orElse(SpellLocks.NONE))));
            case ROW_SUMMON_PROFILE -> openTree(ROW_SUMMON_PROFILE, SummonProfile.CODEC,
                    payload.summonProfile().orElse(SummonProfile.EMPTY),
                    applied -> setPayload(
                            EquipmentEffects.withSummonProfile(payload(), applied)));
            case ROW_SUMMON_DIVE -> openTree(ROW_SUMMON_DIVE, SummonDive.CODEC,
                    payload.summonDive().orElse(null),
                    applied -> setPayload(EquipmentEffects.withSummonDive(payload(), applied)));
            default -> {
            }
        }
    }

    private void openEvents() {
        List<String> kinds = new ArrayList<>();
        for (EventKind kind : EventKind.values()) {
            kinds.add(kind.serializedName());
        }
        minecraft.setScreen(new ChoicePickerScreen(this, Component.translatable(ROW_EVENTS),
                kinds, "", picked -> {
                    EventKind kind = eventNamed(picked);
                    if (kind == null) {
                        minecraft.setScreen(this);
                        return;
                    }
                    openEvent(kind);
                }).handingOffOnPick());
    }

    private static EventKind eventNamed(String name) {
        for (EventKind kind : EventKind.values()) {
            if (kind.serializedName().equals(name)) {
                return kind;
            }
        }
        return null;
    }

    private void openEvent(EventKind kind) {
        List<AffixEffects.OnHit> current = payload().events().get(kind);
        minecraft.setScreen(new EffectTreeScreen(this,
                Component.translatable(kind.labelKey()),
                AffixEffects.OnHit.CODEC,
                current.isEmpty() ? AffixEffects.OnHit.EMPTY : current.getFirst(),
                applied -> {
                    List<AffixEffects.OnHit> next = new ArrayList<>(payload().events().get(kind));
                    AffixEffects.OnHit value = applied.orElse(AffixEffects.OnHit.EMPTY);
                    if (next.isEmpty()) {
                        next.add(value);
                    } else {
                        next.set(0, value);
                    }
                    setPayload(EquipmentEffects.withEvents(payload(),
                            payload().events().with(kind, next)));
                }).withSchema(EffectSchema.ROOT_EFFECT));
    }

    private void openDamageBonus() {
        minecraft.setScreen(new EffectTreeScreen(this,
                Component.translatable(ROW_DAMAGE_BONUS),
                DamageBonusList.CODEC, new DamageBonusList(payload().damageBonus()),
                applied -> setPayload(EquipmentEffects.withDamageBonus(payload(),
                        applied.orElse(DamageBonusList.EMPTY).damageBonus())))
                .withSchema(EffectSchema.ROOT_DAMAGE_BONUS));
    }

    private void openSelfEffects() {
        minecraft.setScreen(new EffectTreeScreen(this,
                Component.translatable(ROW_SELF_EFFECTS),
                SelfEffectList.CODEC, new SelfEffectList(effect.selfEffects()),
                applied -> effect = effect.withSelfEffects(applied.orElse(SelfEffectList.EMPTY).selfEffects()))
                .withSchema(EffectSchema.ROOT_SELF_EFFECTS));
    }

    private <T> void openTree(String labelKey, Codec<T> codec,
                              T current, Consumer<Optional<T>> onApply) {
        minecraft.setScreen(new EffectTreeScreen(this, Component.translatable(labelKey),
                codec, current, onApply).withSchema(schemaFor(labelKey)));
    }

    private static String schemaFor(String labelKey) {
        return switch (labelKey) {
            case ROW_TRIGGER -> EffectSchema.ROOT_TRIGGER;
            case ROW_SUBSTITUTION -> EffectSchema.ROOT_SUBSTITUTION;
            case ROW_SUMMON_PROFILE -> EffectSchema.ROOT_SUMMON_PROFILE;
            case ROW_SUMMON_DIVE -> EffectSchema.ROOT_SUMMON_DIVE;
            case ROW_ECHO -> EffectSchema.ROOT_ECHO;
            case ROW_SPELLS -> EffectSchema.ROOT_SPELLS;
            case ROW_ATTRS -> EffectSchema.ROOT_CONDITIONAL_ATTRIBUTES;
            case ROW_DAMAGE_BONUS -> EffectSchema.ROOT_DAMAGE_BONUS;
            default -> EffectSchema.ROOT_EFFECT;
        };
    }

    private int clearX() {
        return left + PANEL_WIDTH - ScreenTheme.PADDING - CLEAR_WIDTH;
    }

    private Component delayLabel() {
        return effect.delayed()
                ? Component.literal(String.valueOf(effect.delayTicks()))
                : Component.translatable(KEY_NONE);
    }

    private void beginDelayEdit() {
        beginNumberEdit(ROW_DELAY, effect.delayTicks());
    }

    private Component cooldownLabel() {
        EffectCooldown cooldown = effect.cooldown();
        if (cooldown.isEmpty()) {
            return Component.translatable(KEY_NONE);
        }
        if (!cooldown.windowed()) {
            return Component.literal(String.valueOf(cooldown.ticks()));
        }
        return Component.literal(cooldown.ticks() + "t · "
                + (cooldown.limited() ? cooldown.uses() + " uses" : "open")
                + " · " + cooldown.span() + "t");
    }

    private void openCooldown() {
        minecraft.setScreen(new EffectTreeScreen(this,
                Component.translatable(ROW_COOLDOWN),
                EffectCooldown.EXPANDED, effect.cooldown(), applied -> effect =
                        effect.withCooldown(applied.orElse(EffectCooldown.NONE)))
                .withSchema(EffectSchema.ROOT_COOLDOWN));
    }

    private void openTooltipText() {
        minecraft.setScreen(new EffectTreeScreen(this,
                Component.translatable(ROW_TOOLTIP_TEXT),
                TooltipOverride.CODEC, effect.tooltip(), applied -> effect =
                        effect.withTooltip(applied.orElse(TooltipOverride.NONE)))
                .withSchema(EffectSchema.ROOT_TOOLTIP));
    }

    private void beginRepeatEdit() {
        beginNumberEdit(ROW_RESET, payload().resetCombo().orElse(0));
    }

    private void beginNumberEdit(String row, int value) {
        editingRow = row;
        editingCooldown = true;
        cooldownBox.setPosition(clearX() - COOLDOWN_WIDTH - GAP, rowY(rowIndexOf(row)) + 2);
        cooldownBox.setValue(String.valueOf(value));
        cooldownBox.setVisible(true);
        cooldownBox.setFocused(true);
        setFocused(cooldownBox);
    }

    private int rowIndexOf(String row) {
        for (int index = 0; index < ROWS.length; index++) {
            if (ROWS[index].equals(row)) {
                return index;
            }
        }
        return 0;
    }

    private void commitCooldown() {
        if (!editingCooldown) {
            return;
        }
        try {
            int value = Math.max(0, Integer.parseInt(cooldownBox.getValue().trim()));
            if (ROW_RESET.equals(editingRow)) {
                setPayload(EquipmentEffects.withResetCombo(payload(), value));
            } else if (ROW_DELAY.equals(editingRow)) {
                effect = effect.withDelayTicks(value);
            } else {
                effect = effect.withCooldown(effect.cooldown().withTicks(value));
            }
        } catch (NumberFormatException ignored) {
            editingCooldown = false;
        }
        editingRow = null;
        editingCooldown = false;
        cooldownBox.setVisible(false);
        cooldownBox.setFocused(false);
        setFocused(null);
    }

    private void clearRow(String row) {
        EquipmentEffect payload = payload();
        switch (row) {
            case ROW_SUBSTITUTE -> setPayload(EquipmentEffects.withSubstituteStyle(
                    payload, Optional.empty()));
            case ROW_SUBSTITUTION -> setPayload(EquipmentEffects.withSubstitution(
                    payload, Optional.empty()));
            case ROW_AUGMENT -> setPayload(EquipmentEffects.withAugment(
                    payload, Optional.empty()));
            case ROW_ON_SWING -> setPayload(EquipmentEffects.withOnSwing(
                    payload, Optional.empty()));
            case ROW_ON_HIT -> setPayload(EquipmentEffects.withOnHit(
                    payload, Optional.empty()));
            case ROW_EVENTS -> setPayload(EquipmentEffects.withEvents(payload, EventPayloads.EMPTY));
            case ROW_ECHO -> setPayload(EquipmentEffects.withEcho(payload, Optional.empty()));
            case ROW_SPELLS -> setPayload(EquipmentEffects.withSpells(payload, SpellLocks.NONE));
            case ROW_SUMMON_PROFILE -> setPayload(EquipmentEffects.withSummonProfile(
                    payload, Optional.empty()));
            case ROW_SUMMON_DIVE -> setPayload(EquipmentEffects.withSummonDive(
                    payload, Optional.empty()));
            case ROW_GRANTS -> setPayload(EquipmentEffects.withGrantAffixes(payload, List.of()));
            case ROW_TUNES -> setPayload(EquipmentEffects.withTuneAffixes(payload, Map.of()));
            case ROW_ATTRS -> setPayload(EquipmentEffects.withAttributes(payload, Map.of()));
            case ROW_DAMAGE_BONUS -> setPayload(EquipmentEffects.withDamageBonus(payload, List.of()));
            case ROW_COOLDOWN -> effect = effect.withCooldown(EffectCooldown.NONE);
            case ROW_TRIGGER -> effect = effect.withTrigger(EquipmentTrigger.ALWAYS);
            case ROW_HIDE_TOOLTIP -> effect = effect.withHideTooltip(false);
            case ROW_TOOLTIP_TEXT -> effect = effect.withTooltip(TooltipOverride.NONE);
            case ROW_DELAY -> effect = effect.withDelayTicks(TriggeredEffect.NO_DELAY);
            case ROW_SELF_EFFECTS -> effect = effect.withSelfEffects(List.of());
            default -> {
            }
        }
    }

    private boolean isSet(String row) {
        EquipmentEffect payload = payload();
        return switch (row) {
            case ROW_SUBSTITUTE -> payload.substituteStyle().isPresent();
            case ROW_SUBSTITUTION -> payload.substitution().isPresent();
            case ROW_AUGMENT -> payload.augment().isPresent();
            case ROW_ON_SWING -> payload.onSwing().isPresent();
            case ROW_ON_HIT -> payload.onHit().isPresent();
            case ROW_EVENTS -> !payload.events().isEmpty();
            case ROW_ECHO -> payload.echo().isPresent();
            case ROW_SPELLS -> !payload.spells().isEmpty();
            case ROW_SUMMON_PROFILE -> payload.summonProfile().isPresent();
            case ROW_SUMMON_DIVE -> payload.summonDive().isPresent();
            case ROW_GRANTS -> !payload.grantAffixes().isEmpty();
            case ROW_TUNES -> !payload.tuneAffixes().isEmpty();
            case ROW_ATTRS -> !payload.attributes().isEmpty();
            case ROW_DAMAGE_BONUS -> !payload.damageBonus().isEmpty();
            case ROW_COOLDOWN -> !effect.cooldown().isEmpty();
            case ROW_TRIGGER -> !effect.trigger().equals(EquipmentTrigger.ALWAYS);
            case ROW_HIDE_TOOLTIP -> effect.hideTooltip();
            case ROW_TOOLTIP_TEXT -> !effect.tooltip().isEmpty();
            default -> false;
        };
    }

    private void openGrants() {
        minecraft.setScreen(new EquipmentListScreen(this,
                Component.translatable(ROW_GRANTS),
                payload().grantAffixes().stream()
                        .map(affix -> new EquipmentListScreen.Entry(affix, null))
                        .toList(),
                () -> minecraft.setScreen(new EquipmentAffixPickerScreen(this,
                        payload().grantAffixes(), affix -> {
                            List<String> granted = new ArrayList<>(payload().grantAffixes());
                            granted.add(affix);
                            setPayload(EquipmentEffects.withGrantAffixes(payload(), granted));
                            openGrants();
                        }).handlingNavigation()),
                affix -> {
                    List<String> granted = new ArrayList<>(payload().grantAffixes());
                    granted.remove(affix);
                    setPayload(EquipmentEffects.withGrantAffixes(payload(), granted));
                },
                this::openTuningFor,
                affix -> RefTooltip.forAffix(affix, payload().tuneAffixes().get(affix))));
    }

    private void openTunes() {
        minecraft.setScreen(new EquipmentListScreen(this,
                Component.translatable(ROW_TUNES),
                payload().tuneAffixes().keySet().stream()
                        .map(affix -> new EquipmentListScreen.Entry(affix,
                                Component.translatable(KEY_EDIT)))
                        .toList(),
                () -> minecraft.setScreen(new EquipmentAffixPickerScreen(this,
                        payload().tuneAffixes().keySet(), picked -> {
                            Map<String, AffixTuning> tuned =
                                    new LinkedHashMap<>(payload().tuneAffixes());
                            tuned.putIfAbsent(picked, AffixTuning.NONE);
                            setPayload(EquipmentEffects.withTuneAffixes(payload(), tuned));
                            openTuningFor(picked);
                        }).handlingNavigation()),
                affix -> {
                    Map<String, AffixTuning> tuned = new LinkedHashMap<>(payload().tuneAffixes());
                    tuned.remove(affix);
                    setPayload(EquipmentEffects.withTuneAffixes(payload(), tuned));
                },
                this::openTuningFor,
                affix -> RefTooltip.forAffix(affix, payload().tuneAffixes().get(affix))));
    }

    private void openTuningFor(String affixId) {
        StyleAffix affix = AffixRegistry.get(affixId);
        if (affix == null) {
            return;
        }
        minecraft.setScreen(new AffixTuningScreen(this, affix,
                payload().tuneAffixes().get(affixId), applied -> {
                    Map<String, AffixTuning> tuned = new LinkedHashMap<>(payload().tuneAffixes());
                    tuned.put(affixId, applied == null ? AffixTuning.NONE : applied);
                    setPayload(EquipmentEffects.withTuneAffixes(payload(), tuned));
                }));
    }

    private void openAugment() {
        AttackOverride current = payload().augment().orElse(AttackOverride.EMPTY);
        StylePage page = augmentBase();
        if (page == null) {
            return;
        }
        minecraft.setScreen(new AttackEditorScreen(this, page, current, applied ->
                setPayload(EquipmentEffects.withAugment(payload(),
                        applied == null ? Optional.empty() : Optional.of(applied))))
                .withSubtitle(Component.translatable(KEY_AUGMENT_SCOPE)));
    }

    private StylePage augmentBase() {
        return new StylePage(AUGMENT_BASE_ID, "", 0, DeclaredAttack.neutral(),
                null, StyleRarity.COMMON);
    }

    private void openSubstitute() {
        pickSubstitute();
    }

    private void pickSubstitute() {
        minecraft.setScreen(new AttackPickerScreen(this, (page, override) ->
                setPayload(EquipmentEffects.withSubstituteStyle(payload(),
                        Optional.of(page.styleId())))));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        scroll = Math.clamp(scroll - (int) Math.signum(deltaY) * SCROLL_STEP, 0,
                Math.max(0, ROWS.length - visibleRows()));
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        tooltip = null;
        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, panelHeight());

        graphics.drawString(font, Component.translatable(KEY_TITLE),
                left + ScreenTheme.PADDING, top + TITLE_Y, ScreenTheme.HEADING, false);
        graphics.drawString(font, Component.translatable(KEY_NAME),
                left + ScreenTheme.PADDING, top + NAME_Y + 3, ScreenTheme.MUTED, false);

        ScreenDraw.sunken(graphics, left + ScreenTheme.PADDING, listTop() - 2,
                PANEL_WIDTH - ScreenTheme.PADDING * 2, visibleRows() * ROW_HEIGHT + 4);

        if (editingCooldown) {
            int row = rowIndexOf(editingRow == null ? ROW_COOLDOWN : editingRow);
            if (row < scroll || row >= scroll + visibleRows()) {
                commitCooldown();
            } else {
                cooldownBox.setPosition(clearX() - COOLDOWN_WIDTH - GAP, rowY(row) + 2);
            }
        }

        int hovered = rowAt(mouseX, mouseY);
        int first = scroll;
        int last = Math.min(ROWS.length, first + visibleRows());
        for (int index = first; index < last; index++) {
            String row = ROWS[index];
            int y = rowY(index);
            boolean isHovered = index == hovered;
            ScreenDraw.rowBackground(graphics, left + ScreenTheme.PADDING + 1, y,
                    PANEL_WIDTH - ScreenTheme.PADDING * 2 - 2, ROW_HEIGHT - 1,
                    index, isHovered, false);
            Component rowLabel = Component.translatable(row);
            graphics.drawString(font, rowLabel,
                    left + ScreenTheme.PADDING + LABEL_INSET, y + TEXT_INSET,
                    ScreenTheme.TEXT, false);
            if (!restNotes(row).isEmpty()) {
                graphics.drawString(font, FieldDependency.WINDOW.marker(),
                        left + ScreenTheme.PADDING + LABEL_INSET + font.width(rowLabel) + GAP,
                        y + TEXT_INSET, FieldDependency.WINDOW.color(), false);
            }
            boolean set = isSet(row);
            if (!editingCooldown || !row.equals(editingRow)) {
                Component value = valueOf(row);
                int colour = set ? ScreenTheme.GOOD : ScreenTheme.MUTED;
                graphics.drawString(font, value,
                        clearX() - font.width(value) - LABEL_INSET,
                        y + TEXT_INSET, isHovered ? ScreenTheme.ACCENT : colour, false);
            }
            if (set) {
                boolean overClear = ScreenDraw.isWithin(mouseX, mouseY, clearX(), y,
                        CLEAR_WIDTH, ROW_HEIGHT);
                graphics.drawString(font, Component.translatable(KEY_CLEAR),
                        clearX() + 3, y + TEXT_INSET,
                        overClear ? ScreenTheme.BAD : ScreenTheme.MUTED, false);
            }

            if (isHovered) {
                List<Component> lines = tooltipFor(row);
                if (!lines.isEmpty()) {
                    tooltip = lines;
                }
            }
        }

        ScreenDraw.scrollbar(graphics, left + PANEL_WIDTH - ScreenTheme.PADDING - 2,
                listTop(), visibleRows() * ROW_HEIGHT, visibleRows(), ROWS.length, scroll);

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
