package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import org.joml.Vector2i;

import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentTooltip;
import net.pixeldreamstudios.combat_styles.equipment.TooltipOverride;
import net.pixeldreamstudios.combat_styles.equipment.TriggeredEffect;
import net.pixeldreamstudios.combat_styles.item.ItemTooltip;
import net.pixeldreamstudios.combat_styles.item.TooltipGem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TooltipEditorScreen extends PanelScreen {

    private static final int MARGIN = 8;
    private static final int GAP = 8;
    private static final int PANEL_MIN = 300;
    private static final int PANEL_MAX = 520;
    private static final int PANEL_MAX_HEIGHT = 380;
    private static final float PANEL_SHARE = 0.6F;
    private static final int PREVIEW_MIN = 150;
    private static final int HEADER = 40;
    private static final int FOOTER = 30;
    private static final int LINE_HEIGHT = 10;
    private static final int ROW_PAD = 6;
    private static final int BADGE_WIDTH = 24;
    private static final int STATE_WIDTH = 60;
    private static final int ARROW_WIDTH = 10;
    private static final int CLEAR_WIDTH = 12;
    private static final int BUTTON_WIDTH = 70;
    private static final int SMALL_BUTTON_WIDTH = 54;
    private static final int TITLE_Y = 10;
    private static final int SUBTITLE_Y = 24;
    private static final int TEXT_INSET = 3;
    private static final int EDIT_HEIGHT = 14;
    private static final int EDIT_MAX = 512;
    private static final int DOCK_HEIGHT = 112;
    private static final int DOCK_PREVIEW_LINES = 3;
    private static final int MAX_NEST = 3;
    private static final int CHIP_HEIGHT = 11;
    private static final int CHIP_PAD = 3;
    private static final int SCROLL_STEP = 12;
    private static final int PREVIEW_INSET = 4;
    private static final int PREVIEW_TEXT_PAD = 10;
    private static final int HIDDEN_VEIL = 0xA0191713;
    private static final int CHIP_FILL = 0x30FFFFFF;
    private static final int CHIP_HOVER = 0x50FFFFFF;
    private static final int MOUSE_RIGHT = 1;
    private static final int NO_NUMBER = 0;
    private static final int UP = -1;
    private static final int DOWN = 1;
    private static final String UP_GLYPH = "▲";
    private static final String DOWN_GLYPH = "▼";
    private static final String CLEAR_GLYPH = "x";
    private static final String TOKEN_JOIN = " / ";
    private static final int VIS_WIDTH = 38;
    private static final int ICON_WIDTH = 14;
    private static final int INDENT_WIDTH = 8;
    private static final int INDENT_STEP = 6;
    private static final String INDENT_LESS_GLYPH = "‹";
    private static final String INDENT_MORE_GLYPH = "›";
    private static final String ICON_DEFAULT = "default";
    private static final String KEY_ICON_PICK = "screen.combat_styles.tooltip_editor_icon_pick";
    private static final String KEY_ICON_DEFAULT = "screen.combat_styles.tooltip_editor_icon_default";
    private static final String KEY_ICON_NONE = "screen.combat_styles.tooltip_editor_icon_none";
    private static final String KEY_ICON_HINT = "screen.combat_styles.tooltip_editor_icon.hint";
    private static final String KEY_INDENT_HINT = "screen.combat_styles.tooltip_editor_indent.hint";
    private static final int HANDLE_HEIGHT = 13;
    private static final int HANDLE_WIDTH = 150;
    private static final int FIRST_LINE_GAP = 2;
    private static final int DROP_MARK = 0xFF8FB3D9;
    private static final float DRAG_LAYER = 500.0F;
    private static final String KEY_VIS_ALL = "screen.combat_styles.tooltip_editor_vis_all";
    private static final String KEY_VIS_CTRL = "screen.combat_styles.tooltip_editor_vis_ctrl";
    private static final String KEY_VIS_BRIEF = "screen.combat_styles.tooltip_editor_vis_brief";
    private static final String KEY_VIS_HINT = "screen.combat_styles.tooltip_editor_vis.hint";
    private static final String KEY_PREVIEW_DRAG = "screen.combat_styles.tooltip_editor_preview_drag";
    private static final String KEY_PREVIEW_CTRL = "screen.combat_styles.tooltip_editor_preview_ctrl";

    private static int savedPreviewX = Integer.MIN_VALUE;
    private static int savedPreviewY = Integer.MIN_VALUE;
    private int previewX;
    private int previewY;
    private boolean draggingPreview;
    private int dragOffsetX;
    private int dragOffsetY;
    private int draggingLine = -1;
    private List<String> previewKeys = List.of();
    private List<Integer> previewOwners = List.of();
    private boolean previewDetailed;

    private static final String KEY_TITLE = "screen.combat_styles.tooltip_editor";
    private static final String KEY_SUBTITLE = "screen.combat_styles.tooltip_editor_subtitle";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_SAVE = "screen.combat_styles.tooltip_editor_save";
    private static final String KEY_CANCEL = "screen.combat_styles.tooltip_editor_cancel";
    private static final String KEY_HIDE_ALL = "screen.combat_styles.tooltip_editor_hide_all";
    private static final String KEY_HIDE_ALL_HINT = "screen.combat_styles.tooltip_editor_hide_all.hint";
    private static final String KEY_HEADER = "screen.combat_styles.tooltip_editor_header";
    private static final String KEY_HEADER_HINT = "screen.combat_styles.tooltip_editor_header.hint";
    private static final String KEY_NO_HEADER = "screen.combat_styles.tooltip_editor_no_header";
    private static final String KEY_STATE_HIDDEN = "screen.combat_styles.tooltip_editor_state_hidden";
    private static final String KEY_STATE_REWRITTEN = "screen.combat_styles.tooltip_editor_state_rewritten";
    private static final String KEY_STATE_ADDED = "screen.combat_styles.tooltip_editor_state_added";
    private static final String KEY_STATE_SHOWN = "screen.combat_styles.tooltip_editor_state_shown";
    private static final String KEY_MISSING = "screen.combat_styles.tooltip_editor_missing";
    private static final String KEY_MISSING_HINT = "screen.combat_styles.tooltip_editor_missing.hint";
    private static final String KEY_ADD_LINE = "screen.combat_styles.tooltip_editor_add_line";
    private static final String KEY_LINE_HINT = "screen.combat_styles.tooltip_editor_line.hint";
    private static final String KEY_ADDED_HINT = "screen.combat_styles.tooltip_editor_added.hint";
    private static final String KEY_MOVE_HINT = "screen.combat_styles.tooltip_editor_move.hint";
    private static final String KEY_ORIGINAL = "screen.combat_styles.tooltip_editor_original";
    private static final String KEY_KEY = "screen.combat_styles.tooltip_editor_key";
    private static final String KEY_PLACEHOLDERS = "screen.combat_styles.tooltip_editor_placeholders";
    private static final String KEY_PLACEHOLDER = "screen.combat_styles.tooltip_editor_placeholder";
    private static final String KEY_REFS = "screen.combat_styles.tooltip_editor_refs";
    private static final String KEY_EDITING_LINE = "screen.combat_styles.tooltip_editor_editing_line";
    private static final String KEY_EDITING_HEADER = "screen.combat_styles.tooltip_editor_editing_header";
    private static final String KEY_EDITING_ADDED = "screen.combat_styles.tooltip_editor_editing_added";
    private static final String KEY_EDITING_NEW = "screen.combat_styles.tooltip_editor_editing_new";
    private static final String KEY_PREVIEW = "screen.combat_styles.tooltip_editor_preview";
    private static final String KEY_NOTHING = "screen.combat_styles.tooltip_editor_nothing";
    private static final String KEY_NUMBER = "screen.combat_styles.tooltip_editor_number";
    private static final String KEY_ON = "screen.combat_styles.value_set";
    private static final String KEY_OFF = "screen.combat_styles.value_none";

    private enum Kind {
        HIDE_ALL, HEADER, LINE, MISSING, ADDED, ADD
    }

    private record Row(Kind kind, String key, Component line, int index, int number) {
    }

    private record Placed(Row row, int y, int height, List<FormattedCharSequence> text) {
    }

    private record Chip(String token, int x, int y, int width) {
    }

    private final Supplier<EquipmentDefinition> definition;
    private final String effectName;
    private final Consumer<TriggeredEffect> onApply;

    private TriggeredEffect effect;
    private int panelWidth = PANEL_MIN;
    private int panelHeight = PANEL_MAX_HEIGHT;
    private List<Row> rows = List.of();
    private List<Placed> placed = List.of();
    private List<Component> generated = List.of();
    private List<Component> preview = List.of();
    private List<Chip> chips = List.of();
    private int contentHeight;
    private int pixelScroll;
    private EditBox editBox;
    private Button saveButton;
    private Button cancelButton;
    private Row editing;

    public TooltipEditorScreen(Screen parent, Supplier<EquipmentDefinition> definition,
                               String effectName, TriggeredEffect effect,
                               Consumer<TriggeredEffect> onApply) {
        super(Component.translatable(KEY_TITLE), parent);
        this.definition = definition;
        this.effectName = effectName;
        this.effect = effect;
        this.onApply = onApply;
    }

    @Override
    protected int panelWidth() {
        return panelWidth;
    }

    @Override
    protected int panelHeight() {
        return panelHeight;
    }

    @Override
    protected void init() {
        int available = width - MARGIN * 3;
        panelWidth = Mth.clamp((int) (available * PANEL_SHARE), PANEL_MIN, PANEL_MAX);
        if (available - panelWidth < PREVIEW_MIN) {
            panelWidth = Math.max(PANEL_MIN, available - PREVIEW_MIN);
        }
        panelHeight = Math.min(height - MARGIN * 2, PANEL_MAX_HEIGHT);
        left = MARGIN;
        top = (height - panelHeight) / 2;
        previewX = savedPreviewX == Integer.MIN_VALUE ? left + panelWidth + GAP : savedPreviewX;
        previewY = savedPreviewY == Integer.MIN_VALUE ? top + TITLE_Y : savedPreviewY;
        clampPreview();
        layout();
    }

    private void clampPreview() {
        previewX = Mth.clamp(previewX, 0, Math.max(0, width - HANDLE_WIDTH));
        previewY = Mth.clamp(previewY, 0, Math.max(0, height - HANDLE_HEIGHT));
    }

    @Override
    protected void layout() {
        int dockTop = dockTop();
        editBox = new FieldBox(font, contentLeft(), dockTop + LINE_HEIGHT + 2, contentWidth(),
                EDIT_HEIGHT, Component.empty());
        editBox.setMaxLength(EDIT_MAX);
        editBox.setVisible(false);
        editBox.setResponder(value -> refreshChips());
        addRenderableWidget(editBox);

        int buttonY = top + panelHeight - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING;
        saveButton = addRenderableWidget(PanelButton.of(Component.translatable(KEY_SAVE),
                        button -> commitEdit())
                .bounds(contentLeft(), buttonY, SMALL_BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        cancelButton = addRenderableWidget(PanelButton.of(Component.translatable(KEY_CANCEL),
                        button -> cancelEdit())
                .bounds(contentLeft() + SMALL_BUTTON_WIDTH + GAP, buttonY, SMALL_BUTTON_WIDTH,
                        ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(contentRight() - BUTTON_WIDTH * 2 - GAP, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY), button -> {
            commitEdit();
            onApply.accept(effect);
            minecraft.setScreen(parent);
        }).bounds(contentRight() - BUTTON_WIDTH, buttonY,
                BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        showDock(false);
        rebuild();
    }

    private int listTopY() {
        return top + HEADER;
    }

    private int dockTop() {
        return top + panelHeight - FOOTER - DOCK_HEIGHT;
    }

    private int listBottomY() {
        return editing == null ? top + panelHeight - FOOTER : dockTop() - GAP;
    }

    private int viewHeight() {
        return listBottomY() - listTopY();
    }

    private int textLeft() {
        return contentLeft() + BADGE_WIDTH;
    }

    private int controlsLeft() {
        return indentLessX() - GAP;
    }

    private int iconX() {
        return visX() - GAP - ICON_WIDTH;
    }

    private int indentMoreX() {
        return iconX() - GAP - INDENT_WIDTH;
    }

    private int indentLessX() {
        return indentMoreX() - INDENT_WIDTH;
    }

    private void openIconPicker(Row row) {
        List<String> choices = new ArrayList<>();
        choices.add(ICON_DEFAULT);
        choices.addAll(TooltipGem.iconNames());
        String current = override().iconOf(row.key());
        minecraft.setScreen(new ChoicePickerScreen(this, Component.translatable(KEY_ICON_PICK),
                choices, current == null ? ICON_DEFAULT : current,
                picked -> setOverride(override().withIcon(row.key(),
                        ICON_DEFAULT.equals(picked) ? null : picked))));
    }

    private Component iconLabel(Row row) {
        String icon = override().iconOf(row.key());
        if (icon == null) {
            return Component.translatable(KEY_ICON_DEFAULT);
        }
        for (TooltipGem gem : TooltipGem.values()) {
            if (gem.serializedName().equals(icon)) {
                return gem.icon();
            }
        }
        return Component.translatable(KEY_ICON_NONE);
    }

    private int visX() {
        return upX() - GAP - STATE_WIDTH - VIS_WIDTH;
    }

    private boolean detailable(Row row) {
        return row.kind() == Kind.LINE || row.kind() == Kind.ADDED
                || (row.kind() == Kind.HEADER && row.number() != NO_NUMBER);
    }

    private void cycleDetail(Row row) {
        String mode = override().detailOf(row.key());
        String next = mode == null ? TooltipOverride.DETAIL_CTRL
                : TooltipOverride.DETAIL_CTRL.equals(mode) ? TooltipOverride.DETAIL_BRIEF : null;
        setOverride(override().withDetail(row.key(), next));
    }

    private Component detailLabel(Row row) {
        String mode = override().detailOf(row.key());
        return Component.translatable(mode == null ? KEY_VIS_ALL
                : TooltipOverride.DETAIL_CTRL.equals(mode) ? KEY_VIS_CTRL : KEY_VIS_BRIEF);
    }

    private int detailColour(Row row) {
        String mode = override().detailOf(row.key());
        return mode == null ? ScreenTheme.DIM
                : TooltipOverride.DETAIL_CTRL.equals(mode) ? ScreenTheme.ACCENT : ScreenTheme.BRANCH;
    }

    private int previewBoxY() {
        return previewY + HANDLE_HEIGHT + GAP;
    }

    private int previewWrap() {
        return Math.max(PREVIEW_MIN / 2, width - previewX - MARGIN - PREVIEW_TEXT_PAD);
    }

    private List<FormattedCharSequence> previewWrapped() {
        List<Component> shown = preview.isEmpty()
                ? List.of(Component.translatable(KEY_NOTHING).withStyle(ChatFormatting.GRAY))
                : preview;
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        List<Integer> owners = new ArrayList<>();
        for (int index = 0; index < shown.size(); index++) {
            List<FormattedCharSequence> split = font.split(shown.get(index), previewWrap());
            wrapped.addAll(split);
            for (int part = 0; part < split.size(); part++) {
                owners.add(index);
            }
        }
        previewOwners = List.copyOf(owners);
        return wrapped;
    }

    private int segmentTop(int segment) {
        return previewBoxY() + segment * LINE_HEIGHT + (segment > 0 ? FIRST_LINE_GAP : 0);
    }

    private int previewLineAt(double mouseX, double mouseY) {
        if (previewKeys.size() != preview.size() || mouseX < previewX
                || mouseX > previewX + previewWrap() + PREVIEW_TEXT_PAD) {
            return -1;
        }
        for (int segment = 0; segment < previewOwners.size(); segment++) {
            int y = segmentTop(segment);
            if (mouseY >= y && mouseY < y + LINE_HEIGHT) {
                return previewOwners.get(segment);
            }
        }
        return -1;
    }

    private void dropPreviewLine(int from, int to) {
        if (from < 0 || to < 0 || from == to) {
            return;
        }
        String moving = previewKeys.get(from);
        String target = previewKeys.get(to);
        List<String> order = new ArrayList<>();
        for (Row candidate : rows) {
            if (movable(candidate)) {
                order.add(candidate.key());
            }
        }
        if (!order.remove(moving)) {
            return;
        }
        int at = order.indexOf(target);
        if (at < 0) {
            return;
        }
        order.add(from < to ? at + 1 : at, moving);
        setOverride(override().withOrder(order));
    }

    private int textWidth() {
        return Math.max(1, controlsLeft() - textLeft() - GAP);
    }

    private TooltipOverride override() {
        return effect.tooltip();
    }

    private void setOverride(TooltipOverride value) {
        effect = effect.withTooltip(value);
        rebuild();
    }

    private void rebuild() {
        EquipmentDefinition owner = definition.get();
        List<EquipmentTooltip.EditableLine> lines = owner == null
                ? List.of()
                : EquipmentTooltip.editableLines(owner, effectName, effect);
        List<Component> components = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (EquipmentTooltip.EditableLine line : lines) {
            components.add(line.line());
            keys.add(line.key());
        }
        generated = List.copyOf(components);
        TooltipOverride override = override();
        List<Row> built = new ArrayList<>();
        built.add(new Row(Kind.HIDE_ALL, "", Component.empty(), -1, NO_NUMBER));
        boolean headed = false;
        for (String key : override.ordered(override.naturalKeys(keys))) {
            if (key.startsWith(TooltipOverride.ADDED_PREFIX)) {
                int index = Integer.parseInt(key.substring(TooltipOverride.ADDED_PREFIX.length()));
                built.add(new Row(Kind.ADDED, key, Component.empty(), index, NO_NUMBER));
                continue;
            }
            int position = keys.indexOf(key);
            boolean header = key.equals(TooltipOverride.HEADER_KEY);
            headed |= header;
            built.add(new Row(header ? Kind.HEADER : Kind.LINE, key, components.get(position), position,
                    position + 1));
        }
        if (!headed) {
            built.add(1, new Row(Kind.HEADER, TooltipOverride.HEADER_KEY,
                    Component.translatable(KEY_NO_HEADER), -1, NO_NUMBER));
        }
        Set<String> present = new HashSet<>(keys);
        for (Map.Entry<String, String> edit : override.edits().entrySet()) {
            if (!present.contains(edit.getKey())) {
                built.add(new Row(Kind.MISSING, edit.getKey(),
                        Component.translatable(KEY_MISSING, edit.getKey()), -1, NO_NUMBER));
            }
        }
        built.add(new Row(Kind.ADD, "", Component.translatable(KEY_ADD_LINE), -1, NO_NUMBER));
        rows = List.copyOf(built);
        preview = owner == null ? List.of() : EquipmentTooltip.shownLines(owner, effectName, effect);
        List<String> visible = new ArrayList<>();
        for (String key : override.naturalKeys(keys)) {
            if (!override.hides(key)) {
                visible.add(key);
            }
        }
        List<String> showing = new ArrayList<>();
        for (String key : override.ordered(visible)) {
            if (override.showsNow(key)) {
                showing.add(key);
            }
        }
        previewKeys = effect.hideTooltip() ? List.of() : List.copyOf(showing);
        previewDetailed = ItemTooltip.detailed();
        place();
    }

    private void place() {
        List<Placed> laid = new ArrayList<>();
        int y = 0;
        for (Row row : rows) {
            int indent = detailable(row) ? override().indentOf(row.key()) * INDENT_STEP : 0;
            List<FormattedCharSequence> text = font.split(displayOf(row), Math.max(1, textWidth() - indent));
            int height = Math.max(1, text.size()) * LINE_HEIGHT + ROW_PAD;
            laid.add(new Placed(row, y, height, text));
            y += height;
        }
        placed = List.copyOf(laid);
        contentHeight = y;
        clampPixelScroll();
    }

    private void clampPixelScroll() {
        pixelScroll = Mth.clamp(pixelScroll, 0, Math.max(0, contentHeight - viewHeight()));
    }

    private String editOf(Row row) {
        return override().edits().get(row.key());
    }

    private boolean hidden(Row row) {
        return (row.kind() == Kind.LINE || row.kind() == Kind.HEADER)
                && TooltipOverride.HIDDEN.equals(editOf(row));
    }

    private Component displayOf(Row row) {
        return switch (row.kind()) {
            case HIDE_ALL -> Component.translatable(KEY_HIDE_ALL);
            case HEADER -> Component.translatable(KEY_HEADER, row.line());
            case LINE -> {
                String edit = editOf(row);
                yield edit == null || edit.equals(TooltipOverride.HIDDEN)
                        ? row.line()
                        : EquipmentTooltip.rewritten(edit, row.line(), generated);
            }
            case ADDED -> EquipmentTooltip.rewritten(override().lines().get(row.index()), null, generated);
            case MISSING, ADD -> row.line();
        };
    }

    private boolean movable(Row row) {
        return row.kind() == Kind.LINE || row.kind() == Kind.ADDED
                || (row.kind() == Kind.HEADER && row.number() != NO_NUMBER);
    }

    private boolean isChanged(Row row) {
        return switch (row.kind()) {
            case HIDE_ALL -> effect.hideTooltip();
            case HEADER -> !effect.tooltipHeader().isBlank() || editOf(row) != null;
            case LINE, MISSING -> editOf(row) != null;
            case ADDED -> true;
            case ADD -> false;
        };
    }

    private Component stateOf(Row row) {
        return switch (row.kind()) {
            case HIDE_ALL -> Component.translatable(effect.hideTooltip() ? KEY_ON : KEY_OFF);
            case HEADER, LINE -> {
                if (hidden(row)) {
                    yield Component.translatable(KEY_STATE_HIDDEN);
                }
                boolean rewritten = editOf(row) != null
                        || (row.kind() == Kind.HEADER && !effect.tooltipHeader().isBlank());
                yield Component.translatable(rewritten ? KEY_STATE_REWRITTEN : KEY_STATE_SHOWN);
            }
            case ADDED -> Component.translatable(KEY_STATE_ADDED);
            case MISSING, ADD -> Component.empty();
        };
    }

    private int stateColour(Row row) {
        if (hidden(row)) {
            return ScreenTheme.BAD;
        }
        return isChanged(row) ? ScreenTheme.GOOD : ScreenTheme.MUTED;
    }

    private List<Component> hintFor(Row row) {
        List<Component> lines = new ArrayList<>();
        switch (row.kind()) {
            case HIDE_ALL -> lines.add(Component.translatable(KEY_HIDE_ALL_HINT)
                    .withStyle(ChatFormatting.GRAY));
            case HEADER -> {
                lines.add(row.line());
                lines.add(Component.translatable(KEY_HEADER_HINT).withStyle(ChatFormatting.GRAY));
            }
            case LINE -> {
                lines.add(Component.translatable(KEY_ORIGINAL, row.number())
                        .withStyle(ChatFormatting.GRAY));
                lines.add(row.line());
                lines.add(Component.translatable(KEY_PLACEHOLDERS).withStyle(ChatFormatting.GRAY));
                lines.add(placeholderLine(TooltipOverride.LINE_TOKEN + TOKEN_JOIN
                        + TooltipOverride.lineRef(row.number()), TooltipOverride.bodyOf(row.line())));
                for (Ref ref : refsOf(row.line(), null, 0)) {
                    lines.add(placeholderLine(TooltipOverride.wrap(ref.path()) + TOKEN_JOIN
                            + TooltipOverride.wrap(TooltipOverride.under(
                            TooltipOverride.lineName(row.number()), ref.path())), ref.value()));
                }
                lines.add(Component.translatable(KEY_LINE_HINT).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable(KEY_KEY, row.key())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            case MISSING -> lines.add(Component.translatable(KEY_MISSING_HINT)
                    .withStyle(ChatFormatting.GRAY));
            case ADDED -> {
                lines.add(Component.translatable(KEY_ADDED_HINT).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable(KEY_REFS).withStyle(ChatFormatting.DARK_GRAY));
            }
            case ADD -> lines.add(Component.translatable(KEY_REFS).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (movable(row)) {
            lines.add(Component.translatable(KEY_MOVE_HINT).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (detailable(row)) {
            lines.add(Component.translatable(KEY_VIS_HINT).withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable(KEY_INDENT_HINT).withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable(KEY_ICON_HINT).withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private record Ref(String path, Component value) {
    }

    private static List<Ref> refsOf(Component line, String parent, int depth) {
        List<Ref> refs = new ArrayList<>();
        if (depth >= MAX_NEST) {
            return refs;
        }
        List<Component> values = TooltipOverride.valuesOf(line);
        for (int index = 0; index < values.size(); index++) {
            String path = parent == null ? TooltipOverride.valueName(index)
                    : TooltipOverride.nestedRef(parent, index);
            refs.add(new Ref(path, values.get(index)));
            refs.addAll(refsOf(values.get(index), path, depth + 1));
        }
        return refs;
    }

    private static Component placeholderLine(String token, Component value) {
        return Component.translatable(KEY_PLACEHOLDER,
                Component.literal(token).withStyle(ChatFormatting.AQUA), value);
    }

    private Placed placedAt(double mouseX, double mouseY) {
        if (mouseY < listTopY() || mouseY >= listBottomY()
                || mouseX < contentLeft() || mouseX >= contentRight()) {
            return null;
        }
        double inList = mouseY - listTopY() + pixelScroll;
        for (Placed entry : placed) {
            if (inList >= entry.y() && inList < entry.y() + entry.height()) {
                return entry;
            }
        }
        return null;
    }

    private int rowTop(Placed entry) {
        return listTopY() + entry.y() - pixelScroll;
    }

    private int upX() {
        return contentRight() - CLEAR_WIDTH - ARROW_WIDTH * 2;
    }

    private int downX() {
        return contentRight() - CLEAR_WIDTH - ARROW_WIDTH;
    }

    private int clearX() {
        return contentRight() - CLEAR_WIDTH;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editing != null) {
            Chip chip = chipAt(mouseX, mouseY);
            if (chip != null) {
                editBox.insertText(chip.token());
                setFocused(editBox);
                return true;
            }
            if (editBox.mouseClicked(mouseX, mouseY, button)) {
                setFocused(editBox);
                return true;
            }
        }
        if (ScreenDraw.isWithin(mouseX, mouseY, previewX, previewY, HANDLE_WIDTH, HANDLE_HEIGHT)) {
            draggingPreview = true;
            dragOffsetX = (int) mouseX - previewX;
            dragOffsetY = (int) mouseY - previewY;
            return true;
        }
        int line = previewLineAt(mouseX, mouseY);
        if (line >= 0) {
            commitEdit();
            draggingLine = line;
            return true;
        }
        Placed entry = placedAt(mouseX, mouseY);
        if (entry == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        Row row = entry.row();
        int y = rowTop(entry);
        if (detailable(row) && ScreenDraw.isWithin(mouseX, mouseY, visX(), y, VIS_WIDTH, LINE_HEIGHT + ROW_PAD)) {
            commitEdit();
            cycleDetail(row);
            return true;
        }
        if (detailable(row) && ScreenDraw.isWithin(mouseX, mouseY, iconX(), y, ICON_WIDTH, LINE_HEIGHT + ROW_PAD)) {
            commitEdit();
            openIconPicker(row);
            return true;
        }
        if (detailable(row) && ScreenDraw.isWithin(mouseX, mouseY, indentLessX(), y, INDENT_WIDTH, LINE_HEIGHT + ROW_PAD)) {
            commitEdit();
            setOverride(override().withIndent(row.key(), override().indentOf(row.key()) - 1));
            return true;
        }
        if (detailable(row) && ScreenDraw.isWithin(mouseX, mouseY, indentMoreX(), y, INDENT_WIDTH, LINE_HEIGHT + ROW_PAD)) {
            commitEdit();
            setOverride(override().withIndent(row.key(), override().indentOf(row.key()) + 1));
            return true;
        }
        if (isChanged(row) && ScreenDraw.isWithin(mouseX, mouseY, clearX(), y, CLEAR_WIDTH, LINE_HEIGHT + ROW_PAD)) {
            commitEdit();
            clear(row);
            return true;
        }
        if (movable(row) && ScreenDraw.isWithin(mouseX, mouseY, upX(), y, ARROW_WIDTH, LINE_HEIGHT + ROW_PAD)) {
            commitEdit();
            move(row, UP);
            return true;
        }
        if (movable(row) && ScreenDraw.isWithin(mouseX, mouseY, downX(), y, ARROW_WIDTH, LINE_HEIGHT + ROW_PAD)) {
            commitEdit();
            move(row, DOWN);
            return true;
        }
        commitEdit();
        if (button == MOUSE_RIGHT) {
            toggleHidden(row);
            return true;
        }
        open(row);
        return true;
    }

    private void open(Row row) {
        switch (row.kind()) {
            case HIDE_ALL -> {
                effect = effect.withHideTooltip(!effect.hideTooltip());
                rebuild();
            }
            case HEADER -> beginEdit(row, effect.tooltipHeader());
            case LINE -> {
                String edit = editOf(row);
                beginEdit(row, edit == null || edit.equals(TooltipOverride.HIDDEN)
                        ? TooltipOverride.LINE_TOKEN : edit);
            }
            case ADDED -> beginEdit(row, override().lines().get(row.index()));
            case ADD -> beginEdit(row, "");
            case MISSING -> {
            }
        }
    }

    private void toggleHidden(Row row) {
        if (row.kind() != Kind.LINE && row.kind() != Kind.HEADER) {
            return;
        }
        setOverride(override().withEdit(row.key(), hidden(row) ? null : TooltipOverride.HIDDEN));
    }

    private void move(Row row, int direction) {
        List<String> order = new ArrayList<>();
        for (Row candidate : rows) {
            if (movable(candidate)) {
                order.add(candidate.key());
            }
        }
        int from = order.indexOf(row.key());
        int to = from + direction;
        if (from < 0 || to < 0 || to >= order.size()) {
            return;
        }
        order.set(from, order.get(to));
        order.set(to, row.key());
        setOverride(override().withOrder(order));
    }

    private void clear(Row row) {
        switch (row.kind()) {
            case HIDE_ALL -> {
                effect = effect.withHideTooltip(false);
                rebuild();
            }
            case HEADER -> setOverride(override().withHeader("").withEdit(row.key(), null));
            case LINE, MISSING -> setOverride(override().withEdit(row.key(), null));
            case ADDED -> removeAdded(row.index());
            case ADD -> {
            }
        }
    }

    private void removeAdded(int index) {
        List<String> lines = new ArrayList<>(override().lines());
        lines.remove(index);
        List<String> order = new ArrayList<>();
        for (String key : override().order()) {
            if (!key.startsWith(TooltipOverride.ADDED_PREFIX)) {
                order.add(key);
                continue;
            }
            int added = Integer.parseInt(key.substring(TooltipOverride.ADDED_PREFIX.length()));
            if (added != index) {
                order.add(TooltipOverride.addedKey(added > index ? added - 1 : added));
            }
        }
        setOverride(override().withLines(lines).withOrder(order));
    }

    private void beginEdit(Row row, String value) {
        editing = row;
        showDock(true);
        editBox.setValue(value);
        editBox.moveCursorToEnd(false);
        editBox.setFocused(true);
        setFocused(editBox);
        refreshChips();
        place();
        Placed entry = placed.stream().filter(candidate -> candidate.row().equals(row)).findFirst().orElse(null);
        if (entry != null && entry.y() + entry.height() > pixelScroll + viewHeight()) {
            pixelScroll = entry.y() + entry.height() - viewHeight();
            clampPixelScroll();
        }
    }

    private void showDock(boolean shown) {
        editBox.setVisible(shown);
        saveButton.visible = shown;
        cancelButton.visible = shown;
    }

    private void commitEdit() {
        if (editing == null) {
            return;
        }
        Row row = editing;
        String value = editBox.getValue().trim();
        cancelEdit();
        switch (row.kind()) {
            case HEADER -> setOverride(override().withHeader(value));
            case LINE -> setOverride(override().withEdit(row.key(),
                    value.isEmpty() || value.equals(TooltipOverride.LINE_TOKEN) ? null : value));
            case ADDED -> {
                if (value.isEmpty()) {
                    removeAdded(row.index());
                } else {
                    List<String> lines = new ArrayList<>(override().lines());
                    lines.set(row.index(), value);
                    setOverride(override().withLines(lines));
                }
            }
            case ADD -> {
                if (!value.isEmpty()) {
                    List<String> lines = new ArrayList<>(override().lines());
                    lines.add(value);
                    setOverride(override().withLines(lines));
                }
            }
            case HIDE_ALL, MISSING -> {
            }
        }
    }

    private void cancelEdit() {
        editing = null;
        chips = List.of();
        showDock(false);
        editBox.setFocused(false);
        setFocused(null);
        place();
    }

    private void refreshChips() {
        if (editing == null) {
            chips = List.of();
            return;
        }
        List<String> tokens = new ArrayList<>();
        if (editing.kind() == Kind.LINE) {
            tokens.add(TooltipOverride.LINE_TOKEN);
            for (Ref ref : refsOf(editing.line(), null, 0)) {
                tokens.add(TooltipOverride.wrap(ref.path()));
            }
        }
        for (int number = 1; number <= generated.size(); number++) {
            if (editing.number() != number) {
                tokens.add(TooltipOverride.lineRef(number));
            }
        }
        List<Chip> laid = new ArrayList<>();
        int x = contentLeft();
        int y = dockTop() + LINE_HEIGHT + EDIT_HEIGHT + GAP;
        for (String token : tokens) {
            int chipWidth = font.width(token) + CHIP_PAD * 2;
            if (x + chipWidth > contentRight()) {
                x = contentLeft();
                y += CHIP_HEIGHT + 2;
            }
            if (y + CHIP_HEIGHT > dockPreviewY() - FIRST_LINE_GAP) {
                break;
            }
            laid.add(new Chip(token, x, y, chipWidth));
            x += chipWidth + 2;
        }
        chips = List.copyOf(laid);
    }

    private Chip chipAt(double mouseX, double mouseY) {
        for (Chip chip : chips) {
            if (ScreenDraw.isWithin(mouseX, mouseY, chip.x(), chip.y(), chip.width(), CHIP_HEIGHT)) {
                return chip;
            }
        }
        return null;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingPreview) {
            previewX = (int) mouseX - dragOffsetX;
            previewY = (int) mouseY - dragOffsetY;
            clampPreview();
            return true;
        }
        if (draggingLine >= 0) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingPreview) {
            draggingPreview = false;
            savedPreviewX = previewX;
            savedPreviewY = previewY;
            return true;
        }
        if (draggingLine >= 0) {
            int from = draggingLine;
            draggingLine = -1;
            dropPreviewLine(from, previewLineAt(mouseX, mouseY));
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing != null && (keyCode == KEY_ENTER || keyCode == KEY_NUMPAD_ENTER)) {
            commitEdit();
            return true;
        }
        if (editing != null && keyCode == KEY_ESCAPE) {
            cancelEdit();
            return true;
        }
        if (keyCode == KEY_ESCAPE) {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        pixelScroll -= (int) Math.signum(scrollY) * SCROLL_STEP;
        clampPixelScroll();
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (previewDetailed != ItemTooltip.detailed() && editing == null) {
            rebuild();
        }

        drawPanel(graphics);
        drawTitle(graphics, Component.translatable(KEY_TITLE), top + TITLE_Y);
        graphics.drawString(font, Component.translatable(KEY_SUBTITLE, effectName),
                contentLeft(), top + SUBTITLE_Y, ScreenTheme.MUTED, false);
        ScreenDraw.sunken(graphics, contentLeft(), listTopY() - 2, contentWidth(), viewHeight() + 4);

        Placed hovered = placedAt(mouseX, mouseY);
        graphics.enableScissor(contentLeft(), listTopY(), contentRight(), listBottomY());
        int index = 0;
        for (Placed entry : placed) {
            int y = rowTop(entry);
            if (y + entry.height() >= listTopY() && y <= listBottomY()) {
                drawRow(graphics, entry, y, index, entry == hovered, mouseX, mouseY);
            }
            index++;
        }
        graphics.disableScissor();
        ScreenDraw.scrollbar(graphics, contentRight() - 2, listTopY(), viewHeight(), viewHeight(),
                contentHeight, pixelScroll);

        if (editing != null) {
            drawDock(graphics, mouseX, mouseY);
        }
        drawWidgets(graphics, mouseX, mouseY, partialTick);
        drawPreview(graphics, mouseX, mouseY);
        if (hovered != null && editing == null) {
            queueTooltip(hintFor(hovered.row()));
        }
        drawQueuedTooltip(graphics, mouseX, mouseY);
    }

    private void drawRow(GuiGraphics graphics, Placed entry, int y, int index, boolean hovered,
                         int mouseX, int mouseY) {
        Row row = entry.row();
        ScreenDraw.rowBackground(graphics, contentLeft() + 1, y, contentWidth() - 2,
                entry.height() - 1, index, hovered, row.equals(editing));
        if (row.number() != NO_NUMBER) {
            graphics.drawString(font, Component.translatable(KEY_NUMBER, row.number()),
                    contentLeft() + TEXT_INSET, y + TEXT_INSET, ScreenTheme.DIM, false);
        }
        int colour = row.kind() == Kind.MISSING ? ScreenTheme.MUTED
                : row.kind() == Kind.ADD ? ScreenTheme.ACCENT : ScreenTheme.TEXT;
        int lineY = y + TEXT_INSET;
        int indent = detailable(row) ? override().indentOf(row.key()) * INDENT_STEP : 0;
        for (FormattedCharSequence text : entry.text()) {
            graphics.drawString(font, text, textLeft() + indent, lineY, colour, false);
            lineY += LINE_HEIGHT;
        }
        if (hidden(row)) {
            graphics.fill(textLeft(), y + 1, controlsLeft(), y + entry.height() - 2, HIDDEN_VEIL);
        }
        Component state = stateOf(row);
        graphics.drawString(font, state, upX() - GAP - font.width(state), y + TEXT_INSET,
                stateColour(row), false);
        if (detailable(row)) {
            Component vis = detailLabel(row);
            boolean overVis = ScreenDraw.isWithin(mouseX, mouseY, visX(), y, VIS_WIDTH, LINE_HEIGHT + ROW_PAD);
            graphics.drawString(font, vis, visX() + (VIS_WIDTH - font.width(vis)) / 2, y + TEXT_INSET,
                    overVis ? ScreenTheme.ACCENT : detailColour(row), false);
            Component icon = iconLabel(row);
            boolean overIcon = ScreenDraw.isWithin(mouseX, mouseY, iconX(), y, ICON_WIDTH, LINE_HEIGHT + ROW_PAD);
            if (overIcon) {
                graphics.fill(iconX(), y + 1, iconX() + ICON_WIDTH, y + LINE_HEIGHT + ROW_PAD - 1, CHIP_HOVER);
            }
            graphics.drawString(font, icon, iconX() + (ICON_WIDTH - font.width(icon)) / 2, y + TEXT_INSET,
                    override().iconOf(row.key()) == null ? ScreenTheme.DIM : ScreenTheme.TEXT, false);
            int level = override().indentOf(row.key());
            drawIndentGlyph(graphics, INDENT_LESS_GLYPH, indentLessX(), y, mouseX, mouseY, level > 0);
            drawIndentGlyph(graphics, INDENT_MORE_GLYPH, indentMoreX(), y, mouseX, mouseY,
                    level < TooltipOverride.MAX_INDENT);
        }
        if (movable(row)) {
            drawGlyph(graphics, UP_GLYPH, upX(), y, mouseX, mouseY);
            drawGlyph(graphics, DOWN_GLYPH, downX(), y, mouseX, mouseY);
        }
        if (isChanged(row)) {
            boolean over = ScreenDraw.isWithin(mouseX, mouseY, clearX(), y, CLEAR_WIDTH, LINE_HEIGHT + ROW_PAD);
            graphics.drawString(font, CLEAR_GLYPH, clearX() + TEXT_INSET, y + TEXT_INSET,
                    over ? ScreenTheme.BAD : ScreenTheme.MUTED, false);
        }
    }

    private void drawGlyph(GuiGraphics graphics, String glyph, int x, int y, int mouseX, int mouseY) {
        boolean over = ScreenDraw.isWithin(mouseX, mouseY, x, y, ARROW_WIDTH, LINE_HEIGHT + ROW_PAD);
        graphics.drawString(font, glyph, x + (ARROW_WIDTH - font.width(glyph)) / 2, y + TEXT_INSET,
                over ? ScreenTheme.ACCENT : ScreenTheme.MUTED, false);
    }

    private void drawIndentGlyph(GuiGraphics graphics, String glyph, int x, int y, int mouseX, int mouseY,
                                 boolean enabled) {
        boolean over = enabled && ScreenDraw.isWithin(mouseX, mouseY, x, y, INDENT_WIDTH, LINE_HEIGHT + ROW_PAD);
        graphics.drawString(font, glyph, x + (INDENT_WIDTH - font.width(glyph)) / 2, y + TEXT_INSET,
                !enabled ? ScreenTheme.DIM : over ? ScreenTheme.ACCENT : ScreenTheme.MUTED, false);
    }

    private void drawDock(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = dockTop();
        Component title = switch (editing.kind()) {
            case HEADER -> Component.translatable(KEY_EDITING_HEADER);
            case LINE -> Component.translatable(KEY_EDITING_LINE, editing.number());
            case ADDED -> Component.translatable(KEY_EDITING_ADDED);
            default -> Component.translatable(KEY_EDITING_NEW);
        };
        graphics.drawString(font, title, contentLeft(), y, ScreenTheme.HEADING, false);
        for (Chip chip : chips) {
            boolean over = ScreenDraw.isWithin(mouseX, mouseY, chip.x(), chip.y(), chip.width(), CHIP_HEIGHT);
            graphics.fill(chip.x(), chip.y(), chip.x() + chip.width(), chip.y() + CHIP_HEIGHT,
                    over ? CHIP_HOVER : CHIP_FILL);
            graphics.drawString(font, chip.token(), chip.x() + CHIP_PAD, chip.y() + 2,
                    over ? ScreenTheme.ACCENT : ScreenTheme.TEXT, false);
        }
        int previewTop = dockPreviewY();
        ScreenDraw.sunken(graphics, contentLeft(), previewTop - FIRST_LINE_GAP, contentWidth(),
                DOCK_PREVIEW_LINES * LINE_HEIGHT + FIRST_LINE_GAP * 2);
        String value = editBox.getValue();
        if (value.isBlank()) {
            return;
        }
        String key = editing.key();
        Component live = editing.kind() == Kind.HEADER
                ?TooltipOverride.text(value).withStyle(ChatFormatting.GOLD)
                : TooltipOverride.indented(EquipmentTooltip.rewritten(value,
                        editing.kind() == Kind.LINE ? editing.line() : null, generated,
                        override().iconOf(key)), override().indentOf(key));
        List<FormattedCharSequence> wrapped = font.split(live, contentWidth() - TEXT_INSET * 2);
        int lineY = previewTop;
        for (int index = 0; index < Math.min(DOCK_PREVIEW_LINES, wrapped.size()); index++) {
            graphics.drawString(font, wrapped.get(index), contentLeft() + TEXT_INSET, lineY,
                    ScreenTheme.TEXT, false);
            lineY += LINE_HEIGHT;
        }
    }

    private int dockPreviewY() {
        return dockTop() + DOCK_HEIGHT - GAP - DOCK_PREVIEW_LINES * LINE_HEIGHT;
    }

    private void drawPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        ScreenDraw.panel(graphics, previewX, previewY, HANDLE_WIDTH, HANDLE_HEIGHT);
        graphics.drawString(font, Component.translatable(previewDetailed ? KEY_PREVIEW_CTRL : KEY_PREVIEW), previewX + TEXT_INSET * 2,
                previewY + TEXT_INSET, ScreenTheme.HEADING, false);
        Component drag = Component.translatable(KEY_PREVIEW_DRAG);
        graphics.drawString(font, drag, previewX + HANDLE_WIDTH - font.width(drag) - TEXT_INSET * 2,
                previewY + TEXT_INSET, draggingPreview ? ScreenTheme.ACCENT : ScreenTheme.MUTED, false);
        List<FormattedCharSequence> wrapped = previewWrapped();
        int boxX = previewX + PREVIEW_INSET;
        int boxY = previewBoxY();
        ClientTooltipPositioner fixed = (screenWidth, screenHeight, cursorX, cursorY,
                                         boxWidth, boxHeight) -> new Vector2i(boxX, boxY);
        graphics.renderTooltip(font, wrapped, fixed, 0, 0);
        if (draggingLine < 0) {
            return;
        }
        int target = previewLineAt(mouseX, mouseY);
        if (target < 0) {
            return;
        }
        int segment = previewOwners.indexOf(target);
        int markY = target > draggingLine
                ? segmentTop(previewOwners.lastIndexOf(target)) + LINE_HEIGHT
                : segmentTop(segment) - 1;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, DRAG_LAYER);
        graphics.fill(boxX, markY, boxX + previewWrap() + PREVIEW_TEXT_PAD, markY + 1, DROP_MARK);
        graphics.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
