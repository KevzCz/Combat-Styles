package net.pixeldreamstudios.combat_styles.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.config.OptionSources;
import net.pixeldreamstudios.combat_styles.loot.DropEntry;
import net.pixeldreamstudios.combat_styles.loot.DropPool;
import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.damage.ScaledNumber;
import net.pixeldreamstudios.combat_styles.equipment.ItemMatch;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class EffectTreeScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 232;
    private static final int PADDING = 10;
    private static final int HEADER = 46;
    private static final int FOOTER = 58;
    private static final int ERROR_ROW = 46;
    private static final int ROW_HEIGHT = 14;
    private static final int INDENT = 10;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int BUTTON_WIDTH = 68;
    private static final int BUTTON_HEIGHT = 18;
    private static final int STEP_WIDTH = 14;
    private static final int REMOVE_WIDTH = 12;
    private static final int EDIT_WIDTH = 78;

    private static final int COLOR_SHADE = ScreenTheme.SHADE;
    private static final int COLOR_ROW_ALT = ScreenTheme.ROW_ALT;
    private static final int COLOR_HEADING = ScreenTheme.HEADING;
    private static final int COLOR_TEXT = ScreenTheme.TEXT;
    private static final int COLOR_MUTED = ScreenTheme.MUTED;
    private static final int COLOR_ACCENT = ScreenTheme.ACCENT;
    private static final int COLOR_BAD = ScreenTheme.BAD;
    private static final int COLOR_BRANCH = ScreenTheme.BRANCH;
    private static final int COLOR_GOOD = ScreenTheme.GOOD;

    private static final String KEY_TITLE = "screen.combat_styles.effect_tree";
    private static final String KEY_HINT = "screen.combat_styles.effect_tree_hint";
    private static final int VALUE_RESERVE = 64;
    private static final String KEY_ADD = "screen.combat_styles.add_field";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_INVALID = "screen.combat_styles.effect_invalid";
    private static final String KEY_EMPTY = "screen.combat_styles.effect_empty";
    private static final String KEY_CHOOSE = "screen.combat_styles.choice_choose";
    private static final String KEY_UNSET = "screen.combat_styles.choice_unset";
    private static final String KEY_ANY = "screen.combat_styles.choice_any";
    private static final String KEY_YES = "screen.combat_styles.toggle_yes";
    private static final String KEY_NO = "screen.combat_styles.toggle_no";
    private static final String CHOICE_MARK = " ▾";
    private static final String KEY_NOT_NUMBER = "screen.combat_styles.effect_not_number";
    private static final int SCALE_WIDTH = 10;
    private static final int ADD_WIDTH = 12;
    private static final int OPTIONS_OFFSET = 1;
    private static final int CUSTOM_OFFSET = 3;
    private static final String SCALE_GLYPH = "↗";
    private static final String PATH_SEPARATOR = "/";
    private static final float PERCENT = 100.0F;
    private static final String KEY_SCALE_MAKE = "screen.combat_styles.scaled_number_make";
    private static final String KEY_VALUE = "screen.combat_styles.field_value";
    private static final String KEY_VALUE_NAMED = "screen.combat_styles.field_value_named";
    private static final char ID_SEPARATOR = ':';
    private static final String KEY_SCALE_OPEN = "screen.combat_styles.scaled_number_open";
    private static final String KEY_SCALE_TITLE = "screen.combat_styles.scaled_number_title";
    private static final String KEY_SCALE_CRIT = "screen.combat_styles.scaled_number_crit";
    private static final String KEY_SCALE_CRIT_ADD = "screen.combat_styles.scaled_number_crit_add";
    private static final String KEY_SCALE_CRIT_GROUP = "screen.combat_styles.scaled_number_crit_group";
    private static final String KEY_SCALE_BASE = "screen.combat_styles.scaled_number_base";
    private static final String KEY_SCALE_ENTRY = "screen.combat_styles.scaled_number_entry";
    private static final String KEY_SCALE_STRONGEST = "screen.combat_styles.scaled_number_strongest";
    private static final String KEY_SCALE_MIN = "screen.combat_styles.scaled_number_min";
    private static final String KEY_SCALE_MAX = "screen.combat_styles.scaled_number_max";
    private static final String KEY_SCALE_STEP = "screen.combat_styles.scaled_number_step";
    private static final String KEY_SCALE_NOW = "screen.combat_styles.scaled_number_now";
    private static final String SCALED_FIELD_PREFIX = "field.combat_styles.scaled_number.";
    private static final String SCALED_HINT_PREFIX = "hint.combat_styles.scaled_number.";
    private static final Set<String> UNSCALABLE = Set.of("cooldown", "condition", "conditions", "requires");

    private static final int KEY_ENTER = 257;

    private final Component subtitle;
    private record Binding<T>(Codec<T> codec, T fallback, Consumer<Optional<T>> onApply) {
    }

    private final Binding<?> binding;
    private final Set<String> collapsed = new HashSet<>();
    private EffectSchema.Trigger trigger = EffectSchema.Trigger.ANY;
    private String schemaRoot = EffectSchema.ROOT_EFFECT;

    private JsonObject root;
    private EffectSchema.Context context = EffectSchema.Context.NONE;
    private List<JsonTreeModel.Node> rows = List.of();
    private EditBox valueBox;
    private String editingPath;
    private String error;
    private String acknowledged;

    public EffectTreeScreen(Screen parent, Component subtitle, AffixEffects.OnHit current,
                            Consumer<Optional<AffixEffects.OnHit>> onApply) {
        this(parent, subtitle, AffixEffects.OnHit.CODEC,
                current == null ? AffixEffects.OnHit.EMPTY : current, onApply);
    }

    public <T> EffectTreeScreen(Screen parent, Component subtitle, Codec<T> codec, T current,
                                Consumer<Optional<T>> onApply) {
        this(parent, Component.translatable(KEY_TITLE), subtitle, codec, current, onApply);
    }

    public <T> EffectTreeScreen(Screen parent, Component title, Component subtitle, Codec<T> codec,
                                T current, Consumer<Optional<T>> onApply) {
        super(title, parent, new Scrollbar(ScreenTheme.TRACK, COLOR_ACCENT));
        this.subtitle = subtitle;
        this.binding = new Binding<>(codec, current, onApply);
        this.root = encode(codec, current);
    }

    private static <T> JsonObject encode(Codec<T> codec, T value) {
        if (value == null) {
            return new JsonObject();
        }
        JsonElement encoded = codec
                .encodeStart(JsonOps.INSTANCE, value)
                .resultOrPartial(problem -> CombatStyles.LOGGER.debug(
                        "Effect encode failed: {}", problem))
                .orElse(new JsonObject());
        return encoded.isJsonObject() ? encoded.getAsJsonObject() : new JsonObject();
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

        valueBox = new FieldBox(font, left + PADDING, top + PANEL_HEIGHT - ERROR_ROW + 4,
                PANEL_WIDTH - PADDING * 2 - BUTTON_WIDTH - 6, 14, Component.empty());
        valueBox.setMaxLength(96);
        valueBox.setVisible(false);
        addRenderableWidget(valueBox);

        int buttonY = top + PANEL_HEIGHT - 24;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_ADD),
                        button -> openAdd(""))
                .bounds(left + PADDING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH * 2 - 6, buttonY,
                        BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY), button -> apply())
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, BUTTON_HEIGHT).build());

        rebuild();
    }

    private void rebuild() {
        rows = JsonTreeModel.flatten(root, collapsed).stream()
                .filter(node -> !insideScaled(node.path()))
                .toList();
        scroll = Math.clamp(scroll, 0, maxScroll());
    }

    private List<Component> pendingTooltip;

    private static List<Component> referenceTooltip(JsonTreeModel.Node node) {
        if (node.value() == null || !node.value().isJsonPrimitive()) {
            return null;
        }
        String raw = node.value().getAsString();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (node.key()) {
            case "style_id", "substitute_style", "custom_style_id" ->
                    RefTooltip.forStyle(raw);
            case "affix_id", "affix" -> RefTooltip.forAffix(raw, null);
            case "entity", "summon" -> RefTooltip.forEntity(raw);
            case "spell" -> RefTooltip.forSpell(raw);
            default -> null;
        };
    }

    private String schemaPathOf(JsonTreeModel.Node node) {
        return EffectSchema.join(schemaRoot, node.path());
    }

    private Component labelFor(JsonTreeModel.Node node) {
        if (scaledParent(node.path())) {
            return Component.translatableWithFallback(SCALED_FIELD_PREFIX + node.key(),
                    StyleText.prettify(node.key()));
        }
        return EffectLabels.labelFor(schemaPathOf(node), node.key());
    }

    private Component hintFor(JsonTreeModel.Node node) {
        if (scaledParent(node.path())) {
            return Component.translatable(SCALED_HINT_PREFIX + node.key());
        }
        return EffectLabels.hintFor(schemaPathOf(node), node.key());
    }

    private boolean scaledParent(String path) {
        String parent = parentPathOf(path);
        return !parent.equals(path) && ScaledNumber.isScaled(JsonTreeModel.resolve(root, parent));
    }

    private boolean insideScaled(String path) {
        String current = path;
        while (!current.isEmpty()) {
            current = parentPathOf(current);
            if (!current.isEmpty() && ScaledNumber.isScaled(JsonTreeModel.resolve(root, current))) {
                return true;
            }
        }
        return false;
    }

    private boolean scalable(JsonTreeModel.Node node) {
        if (node.kind() != JsonTreeModel.Kind.NUMBER || insideScaled(node.path())) {
            return false;
        }
        String[] parts = node.path().split("/");
        for (String part : parts) {
            if (UNSCALABLE.contains(part)) {
                return false;
            }
        }
        int last = parts.length - 1;
        boolean hookOption = last >= CUSTOM_OFFSET
                && OptionSources.KEY_OPTIONS.equals(parts[last - OPTIONS_OFFSET])
                && Customs.KEY.equals(parts[last - CUSTOM_OFFSET]);
        boolean critPayload = Arrays.asList(parts).contains(ScaledNumber.Crit.KEY_ON_CRIT)
                || Arrays.asList(parts).contains(ScaledNumber.Crit.KEY_ON_NO_CRIT);
        return hookOption || critPart(node) || critPayload
                || binding.codec() == AffixEffects.OnHit.CODEC;
    }

    private boolean critPart(JsonTreeModel.Node node) {
        if (!ScaledNumber.Crit.KEY_CHANCE.equals(node.key())
                && !ScaledNumber.Crit.KEY_VALUE.equals(node.key())) {
            return false;
        }
        String parent = parentPathOf(node.path());
        return ScaledNumber.KEY_CRIT.equals(keyOf(parent))
                || parent.isEmpty() && EffectSchema.ROOT_CRIT.equals(schemaRoot);
    }

    private List<Component> scaledTooltip(JsonTreeModel.Node node) {
        ScaledNumber number = ScaledNumber.parse(node.value()).orElse(null);
        if (number == null) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        lines.add(labelFor(node).copy().withStyle(ChatFormatting.WHITE));
        lines.add(Component.translatable(KEY_SCALE_BASE, trimNumber(number.base()))
                .withStyle(ChatFormatting.GRAY));
        for (DamageSpec.ScalingEntry entry : number.by()) {
            lines.add(Component.translatable(KEY_SCALE_ENTRY, trimNumber(entry.perPoint()),
                    StyleText.scalingSourceName(entry.source())).withStyle(ChatFormatting.AQUA));
        }
        if (number.mode() == DamageSpec.ScalingMode.STRONGEST && number.by().size() > 1) {
            lines.add(Component.translatable(KEY_SCALE_STRONGEST).withStyle(ChatFormatting.GRAY));
        }
        number.min().ifPresent(min -> lines.add(Component.translatable(KEY_SCALE_MIN,
                trimNumber(min)).withStyle(ChatFormatting.GRAY)));
        number.max().ifPresent(max -> lines.add(Component.translatable(KEY_SCALE_MAX,
                trimNumber(max)).withStyle(ChatFormatting.GRAY)));
        number.step().ifPresent(step -> lines.add(Component.translatable(KEY_SCALE_STEP,
                trimNumber(step)).withStyle(ChatFormatting.GRAY)));
        number.crit().ifPresent(crit -> {
            Player viewer = minecraft == null ? null : minecraft.player;
            lines.add(Component.translatable(crit.modeOrDefault() == ScaledNumber.Crit.Mode.ADD_VALUE
                            ? KEY_SCALE_CRIT_ADD : KEY_SCALE_CRIT,
                    trimNumber(crit.chanceFor(viewer) * PERCENT),
                    trimNumber(crit.valueFor(viewer))).withStyle(ChatFormatting.GOLD));
            crit.group().ifPresent(group -> lines.add(Component.translatable(KEY_SCALE_CRIT_GROUP,
                    group).withStyle(ChatFormatting.GOLD)));
        });
        if (minecraft != null && minecraft.player != null) {
            lines.add(Component.translatable(KEY_SCALE_NOW,
                    trimNumber(number.resolve(minecraft.player))).withStyle(ChatFormatting.GREEN));
        }
        lines.add(Component.translatable(KEY_SCALE_OPEN).withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private void drawGlyph(GuiGraphics graphics, int x, int y, String glyph, int colour,
                           int mouseX, int mouseY) {
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y + 1, SCALE_WIDTH, ROW_HEIGHT - 2);
        graphics.drawString(font, glyph, x + SCALE_WIDTH / 2 - font.width(glyph) / 2, y + 3,
                hovered ? COLOR_ACCENT : colour, false);
    }

    private int scaleX(int valueRight) {
        return valueRight - STEP_WIDTH * 2 - 4 - SCALE_WIDTH - 2;
    }

    private Component valueLine(JsonTreeModel.Node node) {
        JsonElement value = node.value();
        if (EffectSchema.ROOT_DROP_POOL.equals(schemaRoot) && DropEntry.KEY_STACK.equals(node.key())
                && value != null && value.isJsonObject() && minecraft != null && minecraft.level != null) {
            ItemStack stack = new DropEntry(Optional.empty(), Optional.of(value.getAsJsonObject()),
                    DropEntry.DEFAULT_WEIGHT, Map.of(), true).create(minecraft.level.registryAccess());
            return stack.isEmpty() ? null : Component.translatable(KEY_VALUE, stack.getHoverName())
                    .withStyle(ChatFormatting.AQUA);
        }
        if (value == null || !value.isJsonPrimitive() || value.getAsJsonPrimitive().isBoolean()) {
            return null;
        }
        String raw = value.getAsString();
        if (raw.isBlank()) {
            return null;
        }
        String named = raw.indexOf(ID_SEPARATOR) > 0 ? StyleText.choiceName(raw).getString() : raw;
        Component shown = named.isBlank() || named.equals(raw)
                ? Component.literal(raw)
                : Component.translatable(KEY_VALUE_NAMED, named, raw);
        return Component.translatable(KEY_VALUE, shown).withStyle(ChatFormatting.AQUA);
    }

    private List<Component> tooltipFor(JsonTreeModel.Node node) {
        if (ScaledNumber.isScaled(node.value())) {
            return scaledTooltip(node);
        }
        List<Component> lines = new ArrayList<>(
                FieldDependency.lines(EffectSchema.join(schemaRoot, node.path()), node.key()));
        Component hint = hintFor(node);
        Component value = valueLine(node);
        if (hint != null || value != null) {
            lines.add(labelFor(node).copy()
                    .withStyle(ChatFormatting.WHITE));
        }
        if (value != null) {
            lines.add(value);
        }
        if (hint != null) {
            lines.add(hint.copy().withStyle(ChatFormatting.GRAY));
        }
        List<Component> reference = referenceTooltip(node);
        if (reference != null) {
            lines.addAll(reference);
        }
        lines.addAll(FieldDependency.related(schemaPathOf(node), node.key(), root));
        lines.addAll(FieldUses.lines(schemaRoot, node, root));
        if (scalable(node)) {
            lines.add(Component.translatable(KEY_SCALE_MAKE).withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines.isEmpty() ? null : lines;
    }

    public EffectTreeScreen withTrigger(EffectSchema.Trigger value) {
        this.trigger = value == null ? EffectSchema.Trigger.ANY : value;
        return this;
    }

    public EffectTreeScreen withSchema(String value) {
        this.schemaRoot = value == null ? EffectSchema.ROOT_EFFECT : value;
        return this;
    }

    public EffectTreeScreen withAllowed(List<String> paths) {
        this.context = context.withAllowed(paths == null ? null : List.copyOf(paths));
        return this;
    }

    public EffectTreeScreen withOptionSource(String selector, String value, String scope) {
        this.context = context.withSource(selector, value, scope);
        return this;
    }

    private void openAdd(String path) {
        List<EffectSchema.Template> options =
                EffectSchema.addableAt(schemaRoot, path, root, trigger, context);
        if (options.isEmpty()) {
            return;
        }
        minecraft.setScreen(new EffectFieldPickerScreen(this,
                EffectSchema.join(schemaRoot, path), options, template -> {
            JsonElement parent = JsonTreeModel.resolve(root, path);
            if (parent != null && parent.isJsonObject()) {
                parent.getAsJsonObject().add(template.key(), template.value().deepCopy());
                collapsed.remove(path);
            }
            rebuild();
        }));
    }

    private void apply() {
        applyWith(binding);
    }

    private <T> void applyWith(Binding<T> bound) {
        JsonObject saving = withoutBlankEntries(root).getAsJsonObject();
        var result = bound.codec().parse(JsonOps.INSTANCE, saving);
        if (result.error().isPresent()) {
            String raw = result.error().get().message();
            minecraft.setScreen(new SchemaErrorScreen(this,
                    SchemaError.causeOf(raw), SchemaError.whyOf(raw)));
            return;
        }
        T parsed = result.result().orElse(bound.fallback());
        String dropped = droppedKeys(bound, saving, parsed);
        if (dropped != null && !dropped.equals(acknowledged)) {
            acknowledged = dropped;
            minecraft.setScreen(new SchemaErrorScreen(this,
                    SchemaError.droppedCause(dropped), SchemaError.droppedWhy()));
            return;
        }
        bound.onApply().accept(saving.isEmpty() ? Optional.empty() : Optional.of(parsed));
        minecraft.setScreen(parent);
    }

    private <T> String droppedKeys(Binding<T> bound, JsonObject saving, T parsed) {
        JsonObject encoded = encode(bound.codec(), parsed);
        StringBuilder builder = new StringBuilder();
        for (String key : saving.keySet()) {
            if (encoded.has(key) || omittedAsDefault(bound, saving, key, parsed)) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(key);
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private <T> boolean omittedAsDefault(Binding<T> bound, JsonObject saving, String key,
                                         T parsed) {
        if (!EffectSchema.knowsField(schemaRoot, key)) {
            return false;
        }
        JsonObject without = saving.deepCopy();
        without.remove(key);
        return bound.codec().parse(JsonOps.INSTANCE, without).result()
                .map(parsed::equals)
                .orElse(false);
    }

    protected int listTop() {
        return top + HEADER;
    }

    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER - 4;
    }

    protected int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_HEIGHT);
    }

    protected int maxScroll() {
        return Math.max(0, rows.size() - visibleRows());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, width, height, COLOR_SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(font, title, left + PADDING, top + 9, COLOR_HEADING, false);
        graphics.drawString(font, subtitle, left + PADDING, top + 21, COLOR_TEXT, false);
        graphics.drawString(font, Component.translatable(KEY_HINT),
                left + PADDING, top + 32, COLOR_MUTED, false);

        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        ScreenDraw.sunken(graphics, x - 2, listTop() - 2, listWidth + 2, (listBottom()) - (listTop()) + 4);

        if (rows.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_EMPTY),
                    x + 6, listTop() + 6, COLOR_MUTED, false);
        }

        graphics.enableScissor(x - 2, listTop() - 2, x + listWidth, listBottom() + 2);
        int y = listTop();
        int last = Math.min(rows.size(), scroll + visibleRows());
        List<Component> hoverTip = null;
        boolean overBar = scrollbar.isOverTrack(mouseX, mouseY);
        int rowMouseX = overBar ? Integer.MIN_VALUE : mouseX;
        for (int i = scroll; i < last; i++) {
            JsonTreeModel.Node node = rows.get(i);
            renderRow(graphics, node, i, x, y, listWidth, rowMouseX, mouseY);
            if (!overBar && ScreenDraw.isWithin(mouseX, mouseY, x, y, listWidth, ROW_HEIGHT)) {
                hoverTip = tooltipFor(node);
            }
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();
        pendingTooltip = hoverTip;

        scrollbar.render(graphics, x + listWidth - SCROLLBAR_WIDTH - 1, listTop(), listBottom(),
                scroll, maxScroll(), rows.size(), visibleRows());

        if (error != null) {
            ScreenDraw.drawTrimmed(graphics, font, Component.translatable(KEY_INVALID).getString()
                            + " " + error,
                    x, top + PANEL_HEIGHT - FOOTER + 2, PANEL_WIDTH - PADDING * 2, COLOR_BAD);
        }

        if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
            ScreenDraw.tooltip(graphics, font, pendingTooltip, mouseX, mouseY);
        }
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRow(GuiGraphics graphics, JsonTreeModel.Node node, int index,
                           int x, int y, int listWidth, int mouseX, int mouseY) {
        if ((index & 1) == 1) {
            graphics.fill(x, y, x + listWidth - SCROLLBAR_WIDTH - 2, y + ROW_HEIGHT, COLOR_ROW_ALT);
        }
        int labelX = x + 4 + node.depth() * INDENT;
        boolean scaledValue = ScaledNumber.isScaled(node.value());
        boolean branch = node.expandable() && !scaledValue;

        if (branch) {
            String marker = collapsed.contains(node.path()) ? "+" : "-";
            graphics.drawString(font, marker, labelX, y + 3, COLOR_MUTED, false);
            labelX += 8;
        }

        Component label = labelFor(node);
        graphics.drawString(font, label, labelX, y + 3, branch ? COLOR_BRANCH : COLOR_TEXT, false);

        int right = x + listWidth - SCROLLBAR_WIDTH - 6;
        int markerX = labelX + font.width(label) + 4;
        for (FieldDependency.Marker marker
                : FieldDependency.markers(EffectSchema.join(schemaRoot, node.path()), node.key())) {
            int markerWidth = font.width(marker.glyph());
            if (markerX + markerWidth >= right - VALUE_RESERVE) {
                break;
            }
            graphics.drawString(font, marker.glyph(), markerX, y + 3, marker.color(), false);
            markerX += markerWidth + 2;
        }

        boolean removable = node.depth() >= 0;
        if (removable) {
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, right - REMOVE_WIDTH, y + 1,
                    REMOVE_WIDTH, ROW_HEIGHT - 2);
            graphics.drawString(font, "x", right - REMOVE_WIDTH + 3, y + 3,
                    hovered ? COLOR_BAD : COLOR_MUTED, false);
        }
        int valueRight = right - REMOVE_WIDTH - 4;

        if (scaledValue) {
            if (!editingScaled(node)) {
                drawNumber(graphics, node, scaledBase(node), y, valueRight, mouseX, mouseY, true);
            }
            return;
        }

        if (node.expandable()) {
            String summary = summaryOf(node);
            int summaryLeft = valueRight - font.width(summary);
            graphics.drawString(font, summary, summaryLeft, y + 3, COLOR_MUTED, false);
            int controls = summaryLeft;
            if (canGrow(node)) {
                boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, controls - 16,
                        y + 1, ADD_WIDTH, ROW_HEIGHT - 2);
                graphics.drawString(font, "+", controls - 13, y + 3,
                        hovered ? COLOR_ACCENT : COLOR_MUTED, false);
            }
            return;
        }

        if (node.path().equals(editingPath)) {
            return;
        }

        if (node.kind() == JsonTreeModel.Kind.BOOLEAN) {
            boolean value = node.value().getAsBoolean();
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, valueRight - 30, y + 1, 30, ROW_HEIGHT - 2);
            Component shown = Component.translatable(value ? KEY_YES : KEY_NO);
            if (hovered) {
                outlineIfHovered(graphics, valueRight - font.width(shown), y,
                        font.width(shown), mouseX, mouseY);
            }
            graphics.drawString(font, shown, valueRight - font.width(shown), y + 3,
                    hovered ? COLOR_ACCENT : (value ? COLOR_GOOD : COLOR_MUTED), false);
            return;
        }

        if (node.kind() == JsonTreeModel.Kind.NUMBER) {
            drawNumber(graphics, node, node.value().getAsDouble(), y, valueRight, mouseX, mouseY,
                    false);
            return;
        }

        String raw = node.value().getAsString();
        boolean hasChoices = !EffectChoices.choicesFor(EffectSchema.join(schemaRoot, node.path()), node.key()).isEmpty();
        boolean blank = raw == null || raw.isBlank();
        boolean any = !blank && raw.equals(EffectChoices.anyValueFor(schemaPathOf(node), node.key()));
        String shown = blank
                ? Component.translatable(hasChoices ? KEY_CHOOSE : KEY_UNSET).getString()
                : ScreenDraw.trim(font, any ? Component.translatable(KEY_ANY).getString() : raw, EDIT_WIDTH);
        if (!blank && hasChoices) {
            shown = shown + CHOICE_MARK;
        }
        int stringLeft = valueRight - font.width(shown);
        outlineIfHovered(graphics, stringLeft, y, font.width(shown), mouseX, mouseY);
        graphics.drawString(font, shown, stringLeft, y + 3,
                blank ? COLOR_BRANCH : COLOR_MUTED, false);
    }

    private void drawNumber(GuiGraphics graphics, JsonTreeModel.Node node, double value, int y,
                            int valueRight, int mouseX, int mouseY, boolean scaled) {
        String shown = trimNumber(value);
        boolean canScale = scaled || scalable(node);
        if (canScale) {
            drawGlyph(graphics, scaleX(valueRight), y, SCALE_GLYPH,
                    scaled ? COLOR_ACCENT : COLOR_MUTED, mouseX, mouseY);
        }
        int numberLeft = (canScale ? scaleX(valueRight) - 2 : valueRight - STEP_WIDTH * 2 - 8)
                - font.width(shown);
        outlineIfHovered(graphics, numberLeft, y, font.width(shown), mouseX, mouseY);
        graphics.drawString(font, shown, numberLeft, y + 3, scaled ? COLOR_ACCENT : COLOR_TEXT, false);
        drawStep(graphics, valueRight - STEP_WIDTH * 2 - 4, y, "-", mouseX, mouseY);
        drawStep(graphics, valueRight - STEP_WIDTH, y, "+", mouseX, mouseY);
    }

    private static String scaledBasePath(JsonTreeModel.Node node) {
        return node.path() + PATH_SEPARATOR + ScaledNumber.KEY;
    }

    private static double scaledBase(JsonTreeModel.Node node) {
        return node.value().getAsJsonObject().get(ScaledNumber.KEY).getAsDouble();
    }

    private boolean editingScaled(JsonTreeModel.Node node) {
        return scaledBasePath(node).equals(editingPath);
    }

    private void outlineIfHovered(GuiGraphics graphics, int textLeft, int y, int textWidth,
                                  int mouseX, int mouseY) {
        ScreenDraw.hoverField(graphics, textLeft, y + 1, textWidth,
                ROW_HEIGHT - 2, mouseX, mouseY);
    }

    private void drawStep(GuiGraphics graphics, int x, int y, String label,
                          int mouseX, int mouseY) {
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y + 1, STEP_WIDTH, ROW_HEIGHT - 2);
        graphics.fill(x, y + 1, x + STEP_WIDTH, y + ROW_HEIGHT - 1,
                hovered ? 0x40FFFFFF : 0x20FFFFFF);
        graphics.drawString(font, label, x + STEP_WIDTH / 2 - font.width(label) / 2, y + 3,
                hovered ? COLOR_ACCENT : COLOR_TEXT, false);
    }

    private static String trimNumber(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1.0E9D) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String exactNumber(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1.0E9D) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private void commitEdit() {
        if (editingPath == null) {
            return;
        }
        JsonElement existing = JsonTreeModel.resolve(root, editingPath);
        String raw = valueBox.getValue();
        if (existing != null && existing.isJsonPrimitive()
                && existing.getAsJsonPrimitive().isNumber()) {
            try {
                JsonTreeModel.set(root, editingPath, new JsonPrimitive(Double.parseDouble(raw)));
            } catch (NumberFormatException ignored) {
                error = Component.translatable(KEY_NOT_NUMBER, raw).getString();
                return;
            }
        } else {
            JsonTreeModel.set(root, editingPath, new JsonPrimitive(raw));
        }
        editingPath = null;
        valueBox.setVisible(false);
        setFocused(null);
        rebuild();
    }

    private void beginEdit(JsonTreeModel.Node node) {
        beginEdit(node.path(), node.kind() == JsonTreeModel.Kind.NUMBER
                ? exactNumber(node.value().getAsDouble())
                : node.value().getAsString());
    }

    private void beginEdit(String path, String text) {
        editingPath = path;
        valueBox.setVisible(true);
        valueBox.setValue(text);
        valueBox.setFocused(true);
        setFocused(valueBox);
    }

    private boolean openDropEntryPicker(JsonTreeModel.Node node) {
        if (!EffectSchema.ROOT_DROP_POOL.equals(schemaRoot) || !DropPool.KEY_ENTRIES.equals(node.path())
                || !node.value().isJsonArray()) {
            return false;
        }
        JsonArray entries = node.value().getAsJsonArray();
        minecraft.setScreen(MatchTargetScreen.forStack(this, picked -> {
            JsonObject entry = new JsonObject();
            writeDropItem(entry, picked);
            entries.add(entry);
            collapsed.remove(node.path());
            rebuild();
        }));
        return true;
    }

    private boolean openDropItemPicker(JsonTreeModel.Node node) {
        if (!EffectSchema.ROOT_DROP_POOL.equals(schemaRoot) || !DropEntry.KEY_ITEM.equals(node.key())) {
            return false;
        }
        JsonObject entry = JsonTreeModel.objectAt(root, parentPathOf(node.path()));
        if (entry == null) {
            return false;
        }
        minecraft.setScreen(MatchTargetScreen.forStack(this, picked -> {
            writeDropItem(entry, picked);
            rebuild();
        }));
        return true;
    }

    private void writeDropItem(JsonObject entry, ItemStack picked) {
        entry.remove(DropEntry.KEY_ITEM);
        entry.remove(DropEntry.KEY_STACK);
        JsonObject stack = picked.getComponentsPatch().isEmpty() || minecraft.level == null
                ? null
                : DropEntry.encodeStack(picked, minecraft.level.registryAccess()).orElse(null);
        if (stack != null) {
            entry.add(DropEntry.KEY_STACK, stack);
        } else {
            entry.addProperty(DropEntry.KEY_ITEM, BuiltInRegistries.ITEM.getKey(picked.getItem()).toString());
        }
    }

    private boolean openSlotPicker(JsonTreeModel.Node node) {
        if (!EffectSchema.SUMMON_SLOTS.contains(node.key())
                || !EffectSchema.KEY_SUMMON_EQUIPMENT.equals(keyOf(parentPathOf(node.path())))
                || !node.value().isJsonPrimitive()) {
            return false;
        }
        EquipmentSlot slot = EquipmentSlot.byName(node.key());
        minecraft.setScreen(MatchTargetScreen.forSlot(this, slot, node.value().getAsString(),
                picked -> {
                    if (picked.isEmpty()) {
                        JsonTreeModel.remove(root, node.path());
                    } else {
                        JsonTreeModel.set(root, node.path(), new JsonPrimitive(picked));
                    }
                    rebuild();
                }));
        return true;
    }

    private boolean openMatchTargets(JsonTreeModel.Node node) {
        String listPath = node.value().isJsonArray() ? node.path() : parentPathOf(node.path());
        if (!node.value().isJsonArray() && !(JsonTreeModel.resolve(root, listPath) instanceof JsonArray)) {
            return false;
        }
        String listKey = keyOf(listPath);
        ItemMatch.Lists lists = EffectSchema.matchListsAt(EffectSchema.join(schemaRoot, listPath), listKey);
        JsonObject owner = JsonTreeModel.objectAt(root, parentPathOf(listPath));
        if (lists == null || owner == null) {
            return false;
        }
        List<String> items = stringsOf(owner, lists.items());
        List<String> tags = stringsOf(owner, lists.tags());
        List<String> categories = stringsOf(owner, lists.categories());
        MatchTargetScreen.Tab opening = listKey.equals(lists.tags())
                ? MatchTargetScreen.Tab.TAGS
                : listKey.equals(lists.categories())
                ? MatchTargetScreen.Tab.CATEGORIES
                : MatchTargetScreen.Tab.INVENTORY;
        MatchTargetScreen picker = new MatchTargetScreen(this, items, tags, categories, opening, () -> {
            owner.add(lists.items(), arrayOf(items));
            owner.add(lists.tags(), arrayOf(tags));
            owner.add(lists.categories(), arrayOf(categories));
        });
        if (lists == ItemMatch.PICK) {
            picker.emptyMeansAny();
        }
        minecraft.setScreen(picker);
        return true;
    }

    private void offerAny(ChoicePickerScreen picker, JsonTreeModel.Node node) {
        String listPath = parentPathOf(node.path());
        JsonElement parent = JsonTreeModel.resolve(root, listPath);
        if (parent instanceof JsonArray array
                && EffectChoices.anyWhenEmpty(EffectSchema.join(schemaRoot, listPath), keyOf(listPath))) {
            picker.offeringAny(stringsOf(array).isEmpty(), () -> {
                JsonObject owner = JsonTreeModel.objectAt(root, parentPathOf(listPath));
                for (String key : EffectChoices.anyTogetherWith(keyOf(listPath))) {
                    if (owner != null && owner.has(key)) {
                        owner.add(key, new JsonArray());
                    }
                }
                rebuild();
            });
            return;
        }
        String anyValue = EffectChoices.anyValueFor(schemaPathOf(node), node.key());
        if (anyValue != null) {
            picker.offeringAny(anyValue.equals(node.value().getAsString()), () -> {
                JsonTreeModel.set(root, node.path(), new JsonPrimitive(anyValue));
                rebuild();
            });
            return;
        }
        if (EffectChoices.anyWhenAbsent(schemaPathOf(node), node.key())) {
            picker.offeringAny(node.value().getAsString().isBlank(), () -> {
                JsonTreeModel.remove(root, node.path());
                forget(node.path());
                rebuild();
            });
        }
    }

    private String summaryOf(JsonTreeModel.Node node) {
        if (node.value().isJsonObject()) {
            return "{" + node.value().getAsJsonObject().size() + "}";
        }
        JsonArray array = node.value().getAsJsonArray();
        if (stringsOf(array).isEmpty()
                && EffectChoices.anyWhenEmpty(schemaPathOf(node), node.key())
                || picksAnyItem(node)) {
            return Component.translatable(KEY_ANY).getString();
        }
        return "[" + array.size() + "]";
    }

    private boolean picksAnyItem(JsonTreeModel.Node node) {
        ItemMatch.Lists lists = EffectSchema.matchListsAt(schemaPathOf(node), node.key());
        JsonObject owner = JsonTreeModel.objectAt(root, parentPathOf(node.path()));
        return lists == ItemMatch.PICK && owner != null
                && stringsOf(owner, lists.items()).isEmpty()
                && stringsOf(owner, lists.tags()).isEmpty()
                && stringsOf(owner, lists.categories()).isEmpty();
    }

    private static List<String> stringsOf(JsonArray array) {
        List<String> values = new ArrayList<>();
        for (JsonElement entry : array) {
            if (entry.isJsonPrimitive() && !entry.getAsString().isBlank()) {
                values.add(entry.getAsString());
            }
        }
        return values;
    }

    private static JsonElement withoutBlankEntries(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject copy = new JsonObject();
            for (var entry : element.getAsJsonObject().entrySet()) {
                copy.add(entry.getKey(), withoutBlankEntries(entry.getValue()));
            }
            return copy;
        }
        if (element.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (JsonElement entry : element.getAsJsonArray()) {
                boolean blank = entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()
                        && entry.getAsString().isBlank();
                if (!blank) {
                    copy.add(withoutBlankEntries(entry));
                }
            }
            return copy;
        }
        return element.deepCopy();
    }

    private static String parentPathOf(String path) {
        int cut = path.lastIndexOf('/');
        return cut < 0 ? "" : path.substring(0, cut);
    }

    private static String keyOf(String path) {
        int cut = path.lastIndexOf('/');
        return cut < 0 ? path : path.substring(cut + 1);
    }

    private static List<String> stringsOf(JsonObject owner, String key) {
        JsonElement element = owner.get(key);
        return element != null && element.isJsonArray()
                ? stringsOf(element.getAsJsonArray())
                : new ArrayList<>();
    }

    private static JsonArray arrayOf(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    private void forget(String path) {
        collapsed.remove(path);
        collapsed.removeIf(kept -> kept.startsWith(path + "/"));
    }

    private JsonElement seedElementFor(JsonTreeModel.Node node) {
        JsonArray array = node.value().getAsJsonArray();
        if (!array.isEmpty()) {
            return array.get(array.size() - 1).deepCopy();
        }
        return EffectSchema.elementFor(schemaRoot, node.path());
    }

    private boolean canGrow(JsonTreeModel.Node node) {
        if (node.value().isJsonArray()) {
            return !node.value().getAsJsonArray().isEmpty()
                    || EffectSchema.elementFor(schemaRoot, node.path()) != null;
        }
        return EffectSchema.hasTemplates(schemaRoot, node.path());
    }

    private void stepNumber(String path, double value, int direction) {
        double step = hasShiftDown() ? 5.0D : (Math.abs(value) < 5.0D ? 0.05D : 1.0D);
        double next = Math.round((value + step * direction) * 1000.0D) / 1000.0D;
        JsonTreeModel.set(root, path, new JsonPrimitive(next));
        rebuild();
    }

    private void clickNumber(JsonTreeModel.Node node, String valuePath, double value,
                             double mouseX, double mouseY, int y, int valueRight, boolean scaled) {
        if ((scaled || scalable(node)) && ScreenDraw.isWithin((int) mouseX, (int) mouseY,
                scaleX(valueRight), y + 1, SCALE_WIDTH, ROW_HEIGHT - 2)) {
            openScaledEditor(node);
            return;
        }
        if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, valueRight - STEP_WIDTH * 2 - 4, y + 1,
                STEP_WIDTH, ROW_HEIGHT - 2)) {
            stepNumber(valuePath, value, -1);
            return;
        }
        if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, valueRight - STEP_WIDTH, y + 1,
                STEP_WIDTH, ROW_HEIGHT - 2)) {
            stepNumber(valuePath, value, 1);
            return;
        }
        beginEdit(valuePath, exactNumber(value));
    }

    private void openScaledEditor(JsonTreeModel.Node node) {
        String path = node.path();
        ScaledNumber current = ScaledNumber.parse(node.value())
                .orElseGet(() -> ScaledNumber.plain(node.value().getAsFloat()));
        minecraft.setScreen(new EffectTreeScreen(this,
                Component.translatable(KEY_SCALE_TITLE, labelFor(node)),
                ScaledNumber.CODEC, current, applied -> {
                    ScaledNumber number = applied.orElse(current);
                    JsonElement written = number.isPlain()
                            ? new JsonPrimitive(number.base())
                            : ScaledNumber.CODEC.encodeStart(JsonOps.INSTANCE, number).result()
                                    .orElse(null);
                    if (written != null) {
                        JsonTreeModel.set(root, path, written);
                    }
                    rebuild();
                }).withSchema(ScaledNumber.SCHEMA));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        error = null;
        if (valueBox.isVisible() && valueBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(valueBox);
            return true;
        }
        if (editingPath != null) {
            commitEdit();
        }
        if (mouseY < listTop() || mouseY > listBottom()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (scrollbar.isOverTrack(mouseX, mouseY)) {
            scroll = scrollbar.beginDrag(mouseX, mouseY, scroll);
            return true;
        }

        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        int y = listTop();
        int last = Math.min(rows.size(), scroll + visibleRows());
        for (int i = scroll; i < last; i++) {
            JsonTreeModel.Node node = rows.get(i);
            if (!ScreenDraw.isWithin((int) mouseX, (int) mouseY, x, y, listWidth, ROW_HEIGHT)) {
                y += ROW_HEIGHT;
                continue;
            }
            int right = x + listWidth - SCROLLBAR_WIDTH - 6;
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, right - REMOVE_WIDTH, y + 1,
                    REMOVE_WIDTH, ROW_HEIGHT - 2)) {
                JsonTreeModel.remove(root, node.path());
                forget(node.path());
                rebuild();
                return true;
            }
            int valueRight = right - REMOVE_WIDTH - 4;

            if (openMatchTargets(node) || openSlotPicker(node) || openDropItemPicker(node)) {
                return true;
            }

            if (ScaledNumber.isScaled(node.value())) {
                clickNumber(node, scaledBasePath(node), scaledBase(node), mouseX, mouseY, y,
                        valueRight, true);
                return true;
            }

            if (node.expandable()) {
                String summary = summaryOf(node);
                int controls = valueRight - font.width(summary);
                if (canGrow(node) && ScreenDraw.isWithin((int) mouseX, (int) mouseY,
                                controls - 16, y + 1, ADD_WIDTH, ROW_HEIGHT - 2)) {
                    if (openDropEntryPicker(node)) {
                        return true;
                    }
                    if (node.value().isJsonArray()) {
                        JsonElement seed = seedElementFor(node);
                        if (seed != null) {
                            node.value().getAsJsonArray().add(seed);
                            collapsed.remove(node.path());
                            rebuild();
                        }
                    } else {
                        openAdd(node.path());
                    }
                    return true;
                }
                if (collapsed.contains(node.path())) {
                    collapsed.remove(node.path());
                } else {
                    collapsed.add(node.path());
                }
                rebuild();
                return true;
            }

            if (node.kind() == JsonTreeModel.Kind.BOOLEAN) {
                JsonTreeModel.set(root, node.path(),
                        new JsonPrimitive(!node.value().getAsBoolean()));
                rebuild();
                return true;
            }

            if (node.kind() == JsonTreeModel.Kind.NUMBER) {
                clickNumber(node, node.path(), node.value().getAsDouble(), mouseX, mouseY, y,
                        valueRight, false);
                return true;
            }

            if (EffectChoices.picksScalingSource(node.key())
                    && EffectChoices.choicesFor(EffectSchema.join(schemaRoot, node.path()), node.key())
                            .equals(ScalingSources.ids())) {
                minecraft.setScreen(new ScalingSourcePickerScreen(this, labelFor(node),
                        node.value().getAsString(), picked -> {
                            JsonTreeModel.set(root, node.path(), new JsonPrimitive(picked));
                            rebuild();
                        }));
                return true;
            }
            List<String> choices = EffectChoices.narrowedFor(node.key(),
                    EffectChoices.choicesFor(EffectSchema.join(schemaRoot, node.path()), node.key()),
                    nearestEntity(node.path()));
            if (!choices.isEmpty()) {
                ChoicePickerScreen picker = new ChoicePickerScreen(this,
                        labelFor(node), choices,
                        node.value().getAsString(), picked -> {
                            JsonTreeModel.set(root, node.path(), new JsonPrimitive(picked));
                            rebuild();
                        });
                offerAny(picker, node);
                if (EffectChoices.picksEntity(node.key())) {
                    picker.previewingEntities();
                } else if (EffectChoices.picksSpell(node.key())) {
                    picker.previewingSpells();
                }
                minecraft.setScreen(picker);
                return true;
            }
            beginEdit(node);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private String nearestEntity(String path) {
        String current = path == null ? "" : path;
        while (true) {
            int split = current.lastIndexOf('/');
            current = split < 0 ? "" : current.substring(0, split);
            JsonObject holder = JsonTreeModel.objectAt(root, current);
            if (holder != null && holder.has(EffectChoices.entityKey())
                    && holder.get(EffectChoices.entityKey()).isJsonPrimitive()) {
                return holder.get(EffectChoices.entityKey()).getAsString();
            }
            if (current.isEmpty()) {
                return null;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.clamp(scroll - (int) scrollY, 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingPath != null) {
            if (keyCode == KEY_ENTER) {
                commitEdit();
                return true;
            }
            if (keyCode == KEY_ESCAPE) {
                editingPath = null;
                valueBox.setVisible(false);
                setFocused(null);
                return true;
            }
            return valueBox.keyPressed(keyCode, scanCode, modifiers)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == KEY_ESCAPE) {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (editingPath != null) {
            return valueBox.charTyped(character, modifiers);
        }
        return super.charTyped(character, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
