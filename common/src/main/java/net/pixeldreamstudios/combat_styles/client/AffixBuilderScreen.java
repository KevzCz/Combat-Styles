package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.AffixText;
import net.pixeldreamstudios.combat_styles.affix.CustomAffixConfig;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.style.AttackBehaviour;
import net.pixeldreamstudios.combat_styles.style.SlashPlacement;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import net.pixeldreamstudios.combat_styles.damage.DamageBonus;
import net.pixeldreamstudios.combat_styles.damage.DamageBonusList;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class AffixBuilderScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 336;
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 84;
    private static final int LABEL_WIDTH = 78;
    private static final int FORM_WIDTH = 214;
    private static final int FIELD_HEIGHT = 14;
    private static final int SWATCH_SIZE = 14;
    private static final int SWATCH_GAP = 4;
    private static final int HEX_LENGTH = 6;
    private static final int HEX_RADIX = 16;
    private static final int GAP = 6;
    private static final int FOOTER_GAP = 6;
    private static final int TITLE_Y = 10;
    private static final int FORM_TOP = 30;
    private static final int PREVIEW_LINE_HEIGHT = 10;
    private static final int MAX_ID_LENGTH = 64;

    private static final int DEFAULT_WEIGHT = 10;
    private static final double DEFAULT_MULTIPLIER = 1.0D;
    private static final List<String> HITBOX_CHOICES =
            List.of("HORIZONTAL_PLANE", "VERTICAL_PLANE", "FORWARD_BOX");

    private static final String KEY_TITLE = "screen.combat_styles.affix_builder";
    private static final String KEY_ID = "screen.combat_styles.affix_id";
    private static final String KEY_WEIGHT = "screen.combat_styles.affix_weight";
    private static final String KEY_COST = "screen.combat_styles.affix_cost_label";
    private static final String KEY_DAMAGE = "screen.combat_styles.affix_damage_label";
    private static final String KEY_RARITY = "screen.combat_styles.affix_rarity";
    private static final String KEY_CATEGORIES = "screen.combat_styles.affix_categories";
    private static final String KEY_HITBOXES = "screen.combat_styles.affix_hitboxes";
    private static final String KEY_ANIMATIONS = "screen.combat_styles.affix_animations";
    private static final String KEY_SHAPE = "screen.combat_styles.affix_shape";
    private static final String KEY_TRAIL = "screen.combat_styles.affix_trail";
    private static final String KEY_ON_HIT = "screen.combat_styles.affix_on_hit";
    private static final String KEY_ON_SWING = "screen.combat_styles.affix_on_swing";
    private static final String KEY_SLASHES = "screen.combat_styles.affix_slashes";
    private static final String KEY_DAMAGE_BONUS = "screen.combat_styles.affix_damage_bonus";
    private static final String KEY_PREVIEW = "screen.combat_styles.affix_preview";
    private static final String KEY_PREVIEW_EMPTY = "screen.combat_styles.affix_preview_empty";
    private static final String KEY_SAVE = "screen.combat_styles.save";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_NEEDS_ID = "screen.combat_styles.affix_needs_id";
    private static final String KEY_BAD_ID = "screen.combat_styles.affix_bad_id";
    private static final String KEY_EXISTS = "screen.combat_styles.affix_exists";
    private static final String KEY_SAVE_FAILED = "screen.combat_styles.affix_save_failed";
    private static final String KEY_ANY = "screen.combat_styles.affix_any";
    private static final String HINT_SUFFIX = ".hint";
    private static final String TIP_SUFFIX = ".tip";
    private static final String COUNT_SUFFIX = " %s";
    private static final String PREVIEW_ID = "preview";

    private final Consumer<StyleAffix> onSaved;

    private EditBox idBox;
    private EditBox weightBox;
    private EditBox costBox;
    private EditBox damageBox;
    private Button categoryButton;
    private Button hitboxButton;
    private Button animationButton;
    private List<String> categories = List.of();
    private List<String> hitboxes = List.of();
    private List<String> animations = List.of();
    private EditBox trailBox;
    private Button rarityButton;
    private Button onHitButton;
    private Button onSwingButton;
    private Button slashButton;
    private Button shapeButton;
    private Button damageBonusButton;

    private StyleRarity rarity = StyleRarity.UNCOMMON;
    private AffixEffects.OnHit onHit = AffixEffects.OnHit.EMPTY;
    private AffixEffects.OnHit onSwing = AffixEffects.OnHit.EMPTY;
    private Optional<AttackBehaviour> behaviour = Optional.empty();
    private List<DamageBonus> damageBonus = List.of();
    private String error;

    public AffixBuilderScreen(Screen parent, Consumer<StyleAffix> onSaved) {
        super(Component.translatable(KEY_TITLE), parent);
        this.onSaved = onSaved;
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

        int fieldX = left + ScreenTheme.PADDING + LABEL_WIDTH;
        int fieldWidth = FORM_WIDTH - LABEL_WIDTH;
        int y = top + FORM_TOP;

        idBox = field(fieldX, y, fieldWidth, idBox == null ? "" : idBox.getValue(),
                MAX_ID_LENGTH, KEY_ID);
        y += ROW_HEIGHT;
        weightBox = field(fieldX, y, fieldWidth,
                weightBox == null ? String.valueOf(DEFAULT_WEIGHT) : weightBox.getValue(),
                MAX_ID_LENGTH, KEY_WEIGHT);
        y += ROW_HEIGHT;
        costBox = field(fieldX, y, fieldWidth,
                costBox == null ? format(DEFAULT_MULTIPLIER) : costBox.getValue(),
                MAX_ID_LENGTH, KEY_COST);
        y += ROW_HEIGHT;
        damageBox = field(fieldX, y, fieldWidth,
                damageBox == null ? format(DEFAULT_MULTIPLIER) : damageBox.getValue(),
                MAX_ID_LENGTH, KEY_DAMAGE);
        y += ROW_HEIGHT;

        rarityButton = addRenderableWidget(tipped(PanelButton.of(rarityLabel(), button -> {
            rarity = next(rarity);
            button.setMessage(rarityLabel());
        }), KEY_RARITY).bounds(fieldX, y, fieldWidth, ScreenTheme.BUTTON_HEIGHT).build());
        y += ROW_HEIGHT;

        categoryButton = addRenderableWidget(tipped(PanelButton.of(listLabel(categories),
                        button -> openFilter(KEY_CATEGORIES, StyleRegistry.categories(),
                                categories, picked -> categories = picked)), KEY_CATEGORIES)
                .bounds(fieldX, y, fieldWidth, ScreenTheme.BUTTON_HEIGHT).build());
        y += ROW_HEIGHT;

        hitboxButton = addRenderableWidget(tipped(PanelButton.of(listLabel(hitboxes),
                        button -> openFilter(KEY_HITBOXES, HITBOX_CHOICES,
                                hitboxes, picked -> hitboxes = picked)), KEY_HITBOXES)
                .bounds(fieldX, y, fieldWidth, ScreenTheme.BUTTON_HEIGHT).build());
        y += ROW_HEIGHT;

        animationButton = addRenderableWidget(tipped(PanelButton.of(listLabel(animations),
                        button -> openFilter(KEY_ANIMATIONS, AnimationCatalogue.all(),
                                animations, picked -> animations = picked)), KEY_ANIMATIONS)
                .bounds(fieldX, y, fieldWidth, ScreenTheme.BUTTON_HEIGHT).build());
        y += ROW_HEIGHT;

        trailBox = field(fieldX, y, fieldWidth - SWATCH_SIZE - SWATCH_GAP,
                trailBox == null ? "" : trailBox.getValue(), MAX_ID_LENGTH, KEY_TRAIL);
        y += ROW_HEIGHT + GAP;

        int effectWidth = (FORM_WIDTH - GAP) / 2;
        int effectRight = left + ScreenTheme.PADDING + effectWidth + GAP;

        onHitButton = addRenderableWidget(tipped(PanelButton.of(effectLabel(KEY_ON_HIT, onHit),
                        button -> minecraft.setScreen(new EffectTreeScreen(this,
                                Component.translatable(KEY_ON_HIT), onHit,
                                applied -> onHit = applied.orElse(AffixEffects.OnHit.EMPTY))
                                .withTrigger(EffectSchema.Trigger.ON_HIT))), KEY_ON_HIT)
                .bounds(left + ScreenTheme.PADDING, y,
                        effectWidth, ScreenTheme.BUTTON_HEIGHT).build());
        onSwingButton = addRenderableWidget(tipped(PanelButton.of(effectLabel(KEY_ON_SWING, onSwing),
                        button -> minecraft.setScreen(new EffectTreeScreen(this,
                                Component.translatable(KEY_ON_SWING), onSwing,
                                applied -> onSwing = applied.orElse(AffixEffects.OnHit.EMPTY))
                                .withTrigger(EffectSchema.Trigger.ON_SWING))), KEY_ON_SWING)
                .bounds(effectRight, y, effectWidth, ScreenTheme.BUTTON_HEIGHT).build());
        y += ScreenTheme.BUTTON_HEIGHT + GAP;

        slashButton = addRenderableWidget(tipped(PanelButton.of(slashLabel(),
                        button -> minecraft.setScreen(new SlashListScreen(this,
                                behaviourOrNone().slashes(), this::applySlashes))), KEY_SLASHES)
                .bounds(left + ScreenTheme.PADDING, y,
                        effectWidth, ScreenTheme.BUTTON_HEIGHT).build());
        shapeButton = addRenderableWidget(tipped(PanelButton.of(shapeLabel(),
                        button -> minecraft.setScreen(new EffectTreeScreen(this,
                                Component.translatable(KEY_SHAPE), AttackBehaviour.CODEC,
                                behaviourOrNone(), this::applyBehaviour)
                                .withSchema(EffectSchema.ROOT_BEHAVIOUR))), KEY_SHAPE)
                .bounds(effectRight, y, effectWidth, ScreenTheme.BUTTON_HEIGHT).build());
        y += ScreenTheme.BUTTON_HEIGHT + GAP;

        damageBonusButton = addRenderableWidget(tipped(PanelButton.of(damageBonusLabel(),
                        button -> minecraft.setScreen(new EffectTreeScreen(this,
                                Component.translatable(KEY_DAMAGE_BONUS), DamageBonusList.CODEC,
                                new DamageBonusList(damageBonus), this::applyDamageBonus)
                                .withSchema(EffectSchema.ROOT_DAMAGE_BONUS))), KEY_DAMAGE_BONUS)
                .bounds(left + ScreenTheme.PADDING, y, FORM_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());

        int buttonY = top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH * 2 - GAP, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_SAVE), button -> save())
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private EditBox field(int x, int y, int width, String value, int maxLength, String labelKey) {
        EditBox box = new FieldBox(font, x, y, width, FIELD_HEIGHT, Component.empty());
        box.setMaxLength(maxLength);
        box.setValue(value);
        box.setResponder(text -> error = null);
        if (labelKey != null) {
            box.setHint(Component.translatable(labelKey + HINT_SUFFIX)
                    .withStyle(ChatFormatting.DARK_GRAY));
            box.setTooltip(Tooltip.create(Component.translatable(labelKey + TIP_SUFFIX)));
        }
        addRenderableWidget(box);
        return box;
    }

    private static PanelButton.Builder tipped(PanelButton.Builder builder, String labelKey) {
        return builder.tooltip(Component.translatable(labelKey + TIP_SUFFIX));
    }

    private Component rarityLabel() {
        return StyleText.rarityName(rarity);
    }

    private AttackBehaviour behaviourOrNone() {
        return behaviour.orElse(AttackBehaviour.NONE);
    }

    private void applySlashes(List<SlashPlacement> slashes) {
        applyBehaviour(Optional.of(behaviourOrNone().withSlashes(slashes)));
    }

    private void applyBehaviour(Optional<AttackBehaviour> applied) {
        AttackBehaviour updated = applied.orElse(AttackBehaviour.NONE);
        behaviour = updated.isEmpty() ? Optional.empty() : Optional.of(updated);
        if (slashButton != null) {
            slashButton.setMessage(slashLabel());
        }
        if (shapeButton != null) {
            shapeButton.setMessage(shapeLabel());
        }
    }

    private Component shapeLabel() {
        AttackBehaviour current = behaviourOrNone();
        boolean set = !current.motionOrNone().isEmpty() || !current.offsetOrNone().isEmpty();
        Component label = Component.translatable(KEY_SHAPE);
        return set
                ? label.copy().withStyle(ChatFormatting.WHITE)
                : label.copy().withStyle(ChatFormatting.GRAY);
    }

    private void applyDamageBonus(Optional<DamageBonusList> applied) {
        damageBonus = applied.orElse(DamageBonusList.EMPTY).damageBonus();
        if (damageBonusButton != null) {
            damageBonusButton.setMessage(damageBonusLabel());
        }
    }

    private Component damageBonusLabel() {
        Component label = Component.translatable(KEY_DAMAGE_BONUS);
        return damageBonus.isEmpty()
                ? label.copy().withStyle(ChatFormatting.GRAY)
                : label.copy().append(String.format(COUNT_SUFFIX, damageBonus.size()))
                        .withStyle(ChatFormatting.WHITE);
    }

    private Component slashLabel() {
        int count = behaviourOrNone().slashes().size();
        Component label = Component.translatable(KEY_SLASHES);
        return count == 0
                ? label.copy().withStyle(ChatFormatting.GRAY)
                : label.copy().append(String.format(COUNT_SUFFIX, count))
                        .withStyle(ChatFormatting.WHITE);
    }

    private Component effectLabel(String key, AffixEffects.OnHit effects) {
        int count = countOf(effects);
        Component label = Component.translatable(key);
        return count == 0
                ? label.copy().withStyle(ChatFormatting.GRAY)
                : label.copy().append(String.format(COUNT_SUFFIX, count))
                        .withStyle(ChatFormatting.WHITE);
    }

    private static int countOf(AffixEffects.OnHit effects) {
        if (effects == null || effects.isEmpty()) {
            return 0;
        }
        List<Component> lines = new ArrayList<>();
        AffixText.appendEffects(previewOf(effects), lines, false);
        return lines.size();
    }

    private static StyleAffix previewOf(AffixEffects.OnHit effects) {
        return new StyleAffix(CombatStyles.id(PREVIEW_ID), DEFAULT_WEIGHT, StyleRarity.COMMON,
                Optional.empty(), effects, AffixEffects.OnHit.EMPTY, StyleAffix.AppliesTo.ANY,
                DEFAULT_MULTIPLIER, DEFAULT_MULTIPLIER, Optional.empty(), List.of());
    }

    private static StyleRarity next(StyleRarity current) {
        StyleRarity[] all = StyleRarity.values();
        return all[(current.ordinal() + 1) % all.length];
    }

    private StyleAffix build(ResourceLocation id) {
        return new StyleAffix(id, parseInt(weightBox.getValue(), DEFAULT_WEIGHT), rarity,
                trailOf(trailBox.getValue()), onHit, onSwing, appliesTo(),
                parseDouble(costBox.getValue(), DEFAULT_MULTIPLIER),
                parseDouble(damageBox.getValue(), DEFAULT_MULTIPLIER),
                behaviour, damageBonus);
    }

    private StyleAffix.AppliesTo appliesTo() {
        return categories.isEmpty() && hitboxes.isEmpty() && animations.isEmpty()
                ? StyleAffix.AppliesTo.ANY
                : new StyleAffix.AppliesTo(hitboxes, categories, animations);
    }

    private Component listLabel(List<String> values) {
        if (values.isEmpty()) {
            return Component.translatable(KEY_ANY).copy()
                    .withStyle(ChatFormatting.DARK_GRAY);
        }
        return values.size() == 1
                ? Component.literal(StyleText.prettify(values.getFirst()))
                : Component.literal(values.size() + " selected");
    }

    private void openFilter(String labelKey, List<String> choices, List<String> current,
                            Consumer<List<String>> onPicked) {
        minecraft.setScreen(new MultiChoiceScreen(this,
                Component.translatable(labelKey), choices, current, picked -> {
                    onPicked.accept(picked);
                    refreshLabels();
                }));
    }

    private void refreshLabels() {
        if (categoryButton != null) {
            categoryButton.setMessage(listLabel(categories));
        }
        if (hitboxButton != null) {
            hitboxButton.setMessage(listLabel(hitboxes));
        }
        if (animationButton != null) {
            animationButton.setMessage(listLabel(animations));
        }
    }

    private static Optional<StyleAffix.TrailColor> trailOf(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        return trimmed.isEmpty()
                ? Optional.empty()
                : Optional.of(new StyleAffix.TrailColor(trimmed, true));
    }

    private ResourceLocation previewId() {
        String rawId = idBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (rawId.isEmpty()) {
            return CombatStyles.id(PREVIEW_ID);
        }
        ResourceLocation id = rawId.contains(":")
                ? ResourceLocation.tryParse(rawId)
                : CombatStyles.id(rawId);
        return id == null ? CombatStyles.id(PREVIEW_ID) : id;
    }

    private ResourceLocation parseId() {
        String rawId = idBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (rawId.isEmpty()) {
            error = Component.translatable(KEY_NEEDS_ID).getString();
            return null;
        }
        ResourceLocation id = rawId.contains(":")
                ? ResourceLocation.tryParse(rawId)
                : CombatStyles.id(rawId);
        if (id == null) {
            error = Component.translatable(KEY_BAD_ID).getString();
            return null;
        }
        return id;
    }

    private void save() {
        ResourceLocation id = parseId();
        if (id == null) {
            return;
        }
        StyleAffix affix = build(id);
        if (CustomAffixConfig.loaded().containsKey(affix.serializedId())) {
            error = Component.translatable(KEY_EXISTS).getString();
            return;
        }
        if (!ConfigEdits.permitted()) {
            error = ConfigEdits.operatorOnly().getString();
            return;
        }
        if (!ConfigEdits.save(EditConfigPayload.Kind.AFFIX, affix.serializedId(),
                StyleAffix.CODEC, affix)) {
            error = Component.translatable(KEY_SAVE_FAILED).getString();
            return;
        }
        AffixRegistry.acceptLocal(affix);
        if (onSaved != null) {
            onSaved.accept(affix);
        }
        minecraft.setScreen(parent);
    }

    private static String format(double value) {
        return StyleText.formatDecimal(value);
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String raw, double fallback) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        onHitButton.setMessage(effectLabel(KEY_ON_HIT, onHit));
        onSwingButton.setMessage(effectLabel(KEY_ON_SWING, onSwing));
        rarityButton.setMessage(rarityLabel());

        graphics.drawString(font, Component.translatable(KEY_TITLE),
                left + ScreenTheme.PADDING, top + TITLE_Y, ScreenTheme.HEADING, false);

        int y = top + FORM_TOP + (FIELD_HEIGHT - font.lineHeight) / 2 + 1;
        for (String key : List.of(KEY_ID, KEY_WEIGHT, KEY_COST, KEY_DAMAGE,
                KEY_RARITY, KEY_CATEGORIES, KEY_HITBOXES, KEY_ANIMATIONS, KEY_TRAIL)) {
            graphics.drawString(font, Component.translatable(key),
                    left + ScreenTheme.PADDING, y, ScreenTheme.TEXT, false);
            y += ROW_HEIGHT;
        }

        renderTrailSwatch(graphics);
        renderPreview(graphics);
        renderFooter(graphics);
            if (idBox != null && idBox.isVisible()) {
            idBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (weightBox != null && weightBox.isVisible()) {
            weightBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (costBox != null && costBox.isVisible()) {
            costBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (damageBox != null && damageBox.isVisible()) {
            damageBox.render(graphics, mouseX, mouseY, partialTick);
        }
        if (trailBox != null && trailBox.isVisible()) {
            trailBox.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderTrailSwatch(GuiGraphics graphics) {
        Integer colour = parseColour(trailBox.getValue());
        int swatchX = trailBox.getX() + trailBox.getWidth() + SWATCH_GAP;
        int swatchY = trailBox.getY();
        if (colour == null) {
            ScreenDraw.fieldOutline(graphics, swatchX, swatchY,
                    SWATCH_SIZE, FIELD_HEIGHT, false);
            return;
        }
        graphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + FIELD_HEIGHT,
                0xFF000000 | colour);
        ScreenDraw.fieldOutline(graphics, swatchX, swatchY, SWATCH_SIZE, FIELD_HEIGHT, false);
    }

    private static Integer parseColour(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.length() != HEX_LENGTH) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed, HEX_RADIX);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void renderPreview(GuiGraphics graphics) {
        int paneX = left + ScreenTheme.PADDING + FORM_WIDTH + ScreenTheme.PADDING;
        int paneWidth = left + PANEL_WIDTH - ScreenTheme.PADDING - paneX;
        int paneY = top + FORM_TOP - ScreenTheme.SECTION_HEIGHT;
        int paneBottom = footerTextY() - FOOTER_GAP;

        ScreenDraw.sectionHeader(graphics, font, Component.translatable(KEY_PREVIEW),
                paneX, paneY, paneWidth);
        int bodyY = paneY + ScreenTheme.SECTION_HEIGHT;
        ScreenDraw.sunken(graphics, paneX, bodyY, paneWidth, paneBottom - bodyY);

        int textX = paneX + GAP;
        int textWidth = paneWidth - GAP * 2;
        int lineY = bodyY + GAP;

        ResourceLocation id = previewId();
        StyleAffix affix = build(id);

        graphics.drawString(font, Component.literal(
                        ScreenDraw.trim(font, AffixText.name(affix).getString(), textWidth))
                        .withStyle(affix.rarity().color()),
                textX, lineY, ScreenTheme.TEXT, false);
        lineY += PREVIEW_LINE_HEIGHT + 2;

        List<Component> lines = new ArrayList<>();
        AffixText.appendEffects(affix, lines, true);
        if (lines.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_PREVIEW_EMPTY),
                    textX, lineY, ScreenTheme.DIM, false);
            return;
        }
        for (Component line : lines) {
            if (lineY + PREVIEW_LINE_HEIGHT > paneBottom - GAP) {
                break;
            }
            for (var wrapped : font.split(line, textWidth)) {
                if (lineY + PREVIEW_LINE_HEIGHT > paneBottom - GAP) {
                    break;
                }
                graphics.drawString(font, wrapped, textX, lineY, ScreenTheme.MUTED, false);
                lineY += PREVIEW_LINE_HEIGHT;
            }
        }
    }

    private int footerTextY() {
        return top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING
                - font.lineHeight - FOOTER_GAP;
    }

    private void renderFooter(GuiGraphics graphics) {
        int footerY = footerTextY();
        Component message = error != null
                ? Component.literal(error)
                : Component.translatable(KEY_ID + HINT_SUFFIX);
        graphics.drawString(font,
                ScreenDraw.trim(font, message.getString(), FORM_WIDTH + LABEL_WIDTH),
                left + ScreenTheme.PADDING, footerY,
                error != null ? ScreenTheme.BAD : ScreenTheme.MUTED, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
