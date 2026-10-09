package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.equipment.PoseTuning;
import net.pixeldreamstudios.combat_styles.equipment.UseSpec;
import net.pixeldreamstudios.combat_styles.equipment.TriggeredEffect;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentProviders;
import net.pixeldreamstudios.combat_styles.network.ApplyStackEquipmentPayload;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;

public class EquipmentEditorScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 240;
    private static final int HEADER = 66;
    private static final int FOOTER = 30;
    private static final int ROW_HEIGHT = 22;
    private static final int BUTTON_WIDTH = 70;
    private static final int GAP = 4;
    private static final int TITLE_Y = 10;
    private static final int HINT_Y = 22;
    private static final int SEARCH_Y = 38;
    private static final int SEARCH_HEIGHT = 14;
    private static final int NAME_Y = 4;
    private static final int DESC_Y = 13;
    private static final int INDEX_WIDTH = 14;
    private static final int SEARCH_MAX = 64;

    private static final String KEY_TITLE = "screen.combat_styles.equipment_editor";
    private static final String KEY_HINT = "screen.combat_styles.equipment_editor_hint";
    private static final String KEY_SEARCH = "screen.combat_styles.search_equipment";
    private static final String KEY_EMPTY = "screen.combat_styles.no_equipment";
    private static final String KEY_NEW = "screen.combat_styles.equipment_new";
    private static final String KEY_CURRENT = "screen.combat_styles.equipment_list_current";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_EFFECTS = "screen.combat_styles.equipment_effects";
    private static final String KEY_TIP_ID = "screen.combat_styles.equipment_tip_id";
    private static final String KEY_TIP_RARITY = "screen.combat_styles.equipment_tip_rarity";
    private static final String KEY_TIP_INNATE = "screen.combat_styles.equipment_tip_innate";
    private static final String KEY_OVERRIDDEN = "screen.combat_styles.equipment_overridden";
    private static final String BULLET = " \u00b7 ";

    private final List<EquipmentDefinition> all = new ArrayList<>();
    private final List<EquipmentDefinition> shown = new ArrayList<>();

    private List<Component> tooltip;
    private EditBox searchBox;

    public EquipmentEditorScreen(Screen parent) {
        super(Component.translatable(KEY_TITLE), parent);
        reload();
    }

    private void reload() {
        all.clear();
        all.addAll(EquipmentRegistry.all());
        all.sort((a, b) -> a.id().compareToIgnoreCase(b.id()));
        refilter(searchBox == null ? "" : searchBox.getValue());
    }

    private void refilter(String text) {
        String needle = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        shown.clear();
        for (EquipmentDefinition definition : all) {
            if (needle.isEmpty()
                    || definition.id().toLowerCase(Locale.ROOT).contains(needle)) {
                shown.add(definition);
            }
        }
        scroll = Math.clamp(scroll, 0, Math.max(0, shown.size() - visibleRows()));
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
        return shown.size();
    }

    @Override
    protected void layout() {
        reload();
        searchBox = new FieldBox(font, left + ScreenTheme.PADDING, top + SEARCH_Y,
                PANEL_WIDTH - ScreenTheme.PADDING * 2, SEARCH_HEIGHT, Component.empty());
        searchBox.setMaxLength(SEARCH_MAX);
        searchBox.setHint(Component.translatable(KEY_SEARCH));
        searchBox.setResponder(this::refilter);
        addRenderableWidget(searchBox);

        int buttonY = top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_NEW),
                        button -> openEffects(blank()))
                .bounds(left + ScreenTheme.PADDING, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CURRENT),
                        button -> openCurrent())
                .bounds(left + ScreenTheme.PADDING + BUTTON_WIDTH + ScreenTheme.PADDING,
                        buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private void openCurrent() {
        Player viewer = minecraft.player;
        ItemStack held = viewer == null ? ItemStack.EMPTY : viewer.getMainHandItem();
        if (held.isEmpty()) {
            return;
        }
        EquipmentDefinition existing = held.get(ModComponents.STACK_EQUIPMENT.get());
        if (existing == null) {
            existing = EquipmentProviders.definitionFor(held);
        }
        EquipmentDefinition base = existing == null
                ? blankForStack(held)
                : withStackId(existing, held);
        minecraft.setScreen(new EquipmentDetailScreen(this, base, saved -> {
            NetworkManager.sendApplyStackEquipment(ApplyStackEquipmentPayload.applying(saved));
            minecraft.setScreen(this);
        }, true).clearingWith(held.has(ModComponents.STACK_EQUIPMENT.get()) ? () -> {
            NetworkManager.sendApplyStackEquipment(ApplyStackEquipmentPayload.clearing());
            minecraft.setScreen(this);
        } : null));
    }

    private EquipmentDefinition withStackId(EquipmentDefinition source, ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String wanted = id == null ? source.id() : id.toString();
        if (wanted.equals(source.id())) {
            return source;
        }
        return new EquipmentDefinition(wanted, source.effects(), source.attributes(),
                source.rarity(), source.innateAffixes(), source.groups(), source.pose(),
                source.offHandPose(), source.poseTuning(), source.items(), source.itemTags(),
                source.weaponCategories(), source.excludedItems(), source.excludedTags(),
                source.excludedCategories(), source.use(), source.weaponCategory(),
                source.activeIn(), source.swings(), source.hideScopeTooltip(), source.hideRarity(),
                source.projectiles(), source.summonProfiles(), source.crits());
    }

    private EquipmentDefinition blankForStack(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return new EquipmentDefinition(id == null ? "stack" : id.toString(),
                Map.of(), Map.of(), Optional.empty(), List.of(), Map.of(),
                Optional.empty(), Optional.empty(), PoseTuning.NONE,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                UseSpec.NONE);
    }

    private EquipmentDefinition blank() {
        return new EquipmentDefinition("", Map.of(), Map.of(), Optional.empty(),
                List.of(), Map.of(), Optional.empty(), Optional.empty(), PoseTuning.NONE,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                UseSpec.NONE);
    }

    private void openEffects(EquipmentDefinition definition) {
        minecraft.setScreen(new EquipmentDetailScreen(this, definition, saved -> {
            if (!ConfigEdits.permitted()) {
                ConfigEdits.tell(ConfigEdits.operatorOnly());
            } else if (ConfigEdits.save(EditConfigPayload.Kind.EQUIPMENT, saved.id(),
                    EquipmentDefinition.CODEC, saved)) {
                EquipmentRegistry.acceptLocal(saved);
            } else {
                ConfigEdits.tell(ConfigEdits.tooLarge());
            }
            reload();
        }));
    }

    @Override
    protected int listTop() {
        return top + HEADER;
    }

    @Override
    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (beginScrollDrag(mouseX, mouseY)) {
            return true;
        }
        int index = rowAt(mouseX, mouseY);
        if (index < 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        openEffects(shown.get(index));
        return true;
    }

    private Component summaryOf(EquipmentDefinition definition) {
        return Component.translatable(KEY_EFFECTS, definition.effects().size());
    }

    private List<Component> tooltipFor(EquipmentDefinition definition) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(StyleText.prettify(definition.id()))
                .withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable(KEY_TIP_ID, definition.id())
                .withStyle(ChatFormatting.DARK_GRAY));
        definition.rarity().ifPresent(rarity ->
                lines.add(Component.translatable(KEY_TIP_RARITY,
                        StyleText.prettify(rarity.getSerializedName()))
                        .withStyle(ChatFormatting.GRAY)));
        if (!definition.innateAffixes().isEmpty()) {
            lines.add(Component.translatable(KEY_TIP_INNATE,
                    definition.innateAffixes().size()).withStyle(ChatFormatting.GRAY));
        }
        for (Map.Entry<String, TriggeredEffect> entry : definition.effects().entrySet()) {
            lines.add(Component.literal(BULLET + entry.getKey())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
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

        ScreenDraw.sunken(graphics, left + ScreenTheme.PADDING, listTop() - 2,
                PANEL_WIDTH - ScreenTheme.PADDING * 2, visibleRows() * ROW_HEIGHT + 4);

        if (shown.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_EMPTY),
                    left + ScreenTheme.PADDING + INDEX_WIDTH, listTop() + NAME_Y,
                    ScreenTheme.DIM, false);
            drawWidgets(graphics, mouseX, mouseY, partialTick);
            return;
        }

        int hovered = rowAt(mouseX, mouseY);
        int first = scroll;
        int last = Math.min(shown.size(), first + visibleRows());
        for (int index = first; index < last; index++) {
            EquipmentDefinition definition = shown.get(index);
            int y = rowY(index);
            boolean isHovered = index == hovered;
            ScreenDraw.rowBackground(graphics, left + ScreenTheme.PADDING + 1, y,
                    PANEL_WIDTH - ScreenTheme.PADDING * 2 - 2, ROW_HEIGHT - 1,
                    index, isHovered, false);

            graphics.drawString(font, StyleText.prettify(definition.id()),
                    left + ScreenTheme.PADDING + GAP, y + NAME_Y, ScreenTheme.TEXT, false);
            graphics.drawString(font, summaryOf(definition),
                    left + ScreenTheme.PADDING + GAP, y + DESC_Y, ScreenTheme.MUTED, false);

            if (EquipmentRegistry.isOverridden(definition.id())
                    || !EquipmentRegistry.fromData(definition.id())) {
                Component mark = Component.translatable(KEY_OVERRIDDEN);
                graphics.drawString(font, mark,
                        left + PANEL_WIDTH - ScreenTheme.PADDING - GAP - font.width(mark),
                        y + NAME_Y, ScreenTheme.GOOD, false);
            }
            if (isHovered) {
                tooltip = tooltipFor(definition);
            }
        }

        ScreenDraw.scrollbar(graphics, left + PANEL_WIDTH - ScreenTheme.PADDING - 2,
                listTop(), visibleRows() * ROW_HEIGHT, visibleRows(), shown.size(), scroll);

        drawWidgets(graphics, mouseX, mouseY, partialTick);

        if (tooltip != null && !tooltip.isEmpty()) {
            ScreenDraw.tooltip(graphics, font, tooltip, mouseX, mouseY);
        }
    }

}
