package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.PoseTuning;
import net.pixeldreamstudios.combat_styles.equipment.UseSpec;
import net.pixeldreamstudios.combat_styles.network.ApplyStackEquipmentPayload;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.components.AbstractWidget;

public class EquipmentListScreen extends PanelScreen {

    public record Entry(String id, Component value) {
    }

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 224;
    private static final int HEADER = 46;
    private static final int FOOTER = 30;
    private static final int ROW_HEIGHT = 18;
    private static final int BUTTON_WIDTH = 70;

    private final List<AbstractWidget> footerButtons = new ArrayList<>();
    private static final int TITLE_Y = 10;
    private static final int HINT_Y = 24;
    private static final int TEXT_INSET = 5;
    private static final int REMOVE_WIDTH = 14;

    private static final String KEY_TITLE = "screen.combat_styles.equipment_list";
    private static final String KEY_HINT = "screen.combat_styles.equipment_list_hint";
    private static final String KEY_EMPTY = "screen.combat_styles.equipment_list_empty";
    private static final String KEY_ADD = "screen.combat_styles.equipment_list_add";
    private static final String KEY_CURRENT = "screen.combat_styles.equipment_list_current";
    private static final String KEY_REMOVE = "screen.combat_styles.slash_remove";
    private static final String KEY_CLOSE = "gui.back";

    private final Component subtitle;
    private final List<Entry> entries = new ArrayList<>();
    private final Runnable onAdd;
    private final Consumer<String> onRemove;
    private final Consumer<String> onEdit;
    private final Function<String, List<Component>> tooltips;

    private List<Component> tooltip;

    public EquipmentListScreen(Screen parent, Component subtitle, List<Entry> entries,
                               Runnable onAdd, Consumer<String> onRemove,
                               Consumer<String> onEdit,
                               Function<String, List<Component>> tooltips) {
        super(Component.translatable(KEY_TITLE), parent);
        this.subtitle = subtitle;
        this.entries.addAll(entries);
        this.onAdd = onAdd;
        this.onRemove = onRemove;
        this.onEdit = onEdit;
        this.tooltips = tooltips;
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
        return entries.size();
    }

    private <T extends AbstractWidget> T combat_styles$footer(T widget) {
        footerButtons.add(widget);
        return addRenderableWidget(widget);
    }

    private void renderFooter(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        for (AbstractWidget button : footerButtons) {
            button.render(graphics, mouseX, mouseY, partial);
        }
    }

    private void openCurrent() {
        Player viewer = minecraft.player;
        ItemStack held = viewer == null ? ItemStack.EMPTY : viewer.getMainHandItem();
        if (held.isEmpty()) {
            return;
        }
        EquipmentDefinition existing = held.get(ModComponents.STACK_EQUIPMENT.get());
        EquipmentDefinition base = existing != null ? existing : blankForStack(held);
        minecraft.setScreen(new EquipmentDetailScreen(this, base, saved -> {
            NetworkManager.sendApplyStackEquipment(ApplyStackEquipmentPayload.applying(saved));
            minecraft.setScreen(this);
        }, true).clearingWith(held.has(ModComponents.STACK_EQUIPMENT.get()) ? () -> {
            NetworkManager.sendApplyStackEquipment(ApplyStackEquipmentPayload.clearing());
            minecraft.setScreen(this);
        } : null));
    }

    private EquipmentDefinition blankForStack(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return new EquipmentDefinition(id == null ? "stack" : id.getPath(),
                Map.of(), Map.of(), Optional.empty(), List.of(), Map.of(),
                Optional.empty(), Optional.empty(), PoseTuning.NONE,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                UseSpec.NONE);
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
    protected void layout() {
        footerButtons.clear();
        int buttonY = footerTop();
        combat_styles$footer(PanelButton.of(Component.translatable(KEY_ADD),
                        button -> onAdd.run())
                .bounds(contentLeft(), buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        combat_styles$footer(PanelButton.of(Component.translatable(KEY_CURRENT),
                        button -> openCurrent())
                .bounds(contentLeft() + BUTTON_WIDTH + ScreenTheme.PADDING, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        combat_styles$footer(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(contentRight() - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private int removeX() {
        return contentRight() - REMOVE_WIDTH;
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
        String id = entries.get(index).id();
        if (ScreenDraw.isWithin(mouseX, mouseY, removeX(), rowY(index),
                REMOVE_WIDTH, ROW_HEIGHT)) {
            entries.remove(index);
            onRemove.accept(id);
            return true;
        }
        if (onEdit != null) {
            onEdit.accept(id);
        }
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        tooltip = null;
        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(font, subtitle,
                left + ScreenTheme.PADDING, top + TITLE_Y, ScreenTheme.HEADING, false);
        graphics.drawString(font, Component.translatable(KEY_HINT),
                left + ScreenTheme.PADDING, top + HINT_Y, ScreenTheme.MUTED, false);

        ScreenDraw.sunken(graphics, left + ScreenTheme.PADDING, listTop() - 2,
                PANEL_WIDTH - ScreenTheme.PADDING * 2, visibleRows() * ROW_HEIGHT + 4);

        if (entries.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_EMPTY),
                    left + ScreenTheme.PADDING + TEXT_INSET, listTop() + TEXT_INSET,
                    ScreenTheme.DIM, false);
            renderFooter(graphics, mouseX, mouseY, partialTick);
            return;
        }

        int hovered = rowAt(mouseX, mouseY);
        int first = scroll;
        int last = Math.min(entries.size(), first + visibleRows());
        for (int index = first; index < last; index++) {
            Entry entry = entries.get(index);
            int y = rowY(index);
            boolean isHovered = index == hovered;
            ScreenDraw.rowBackground(graphics, left + ScreenTheme.PADDING + 1, y,
                    PANEL_WIDTH - ScreenTheme.PADDING * 2 - 2, ROW_HEIGHT - 1,
                    index, isHovered, false);
            ScreenDraw.drawTrimmed(graphics, font, StyleText.prettify(entry.id()),
                    left + ScreenTheme.PADDING + TEXT_INSET, y + TEXT_INSET, PANEL_WIDTH - 150, ScreenTheme.TEXT);
            if (entry.value() != null) {
                graphics.drawString(font, entry.value(),
                        removeX() - font.width(entry.value()) - 6, y + TEXT_INSET,
                        isHovered ? ScreenTheme.ACCENT : ScreenTheme.MUTED, false);
            }
            graphics.drawString(font, Component.translatable(KEY_REMOVE),
                    removeX() + 3, y + TEXT_INSET, ScreenTheme.BAD, false);
            if (isHovered && tooltips != null) {
                List<Component> lines = tooltips.apply(entry.id());
                if (lines != null && !lines.isEmpty()) {
                    tooltip = lines;
                }
            }
        }

        ScreenDraw.scrollbar(graphics, left + PANEL_WIDTH - ScreenTheme.PADDING - 2,
                listTop(), visibleRows() * ROW_HEIGHT, visibleRows(), entries.size(), scroll);

        if (tooltip != null && !tooltip.isEmpty()) {
            ScreenDraw.tooltip(graphics, font, tooltip, mouseX, mouseY);
        }
            renderFooter(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
