package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public class ItemPickerScreen extends PickerScreen<ItemStack> {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 232;
    private static final int ROW_HEIGHT = 18;
    private static final int TAB_WIDTH = 80;
    private static final int TAB_Y = 24;
    private static final int ICON_SIZE = 16;
    private static final int EXTRA_HEADER = 22;

    private static final String KEY_TITLE = "screen.combat_styles.item_picker";
    private static final String KEY_SEARCH = "screen.combat_styles.search_items";
    private static final String KEY_COUNT = "screen.combat_styles.item_count";
    private static final String KEY_TAB_INVENTORY = "screen.combat_styles.item_tab_inventory";
    private static final String KEY_TAB_ALL = "screen.combat_styles.item_tab_all";
    private static final String KEY_EMPTY = "screen.combat_styles.no_items";

    private final Consumer<String> onPick;
    private final String current;
    private final List<ItemStack> inventory = new ArrayList<>();
    private final List<ItemStack> everything = new ArrayList<>();

    private boolean showingInventory = true;

    public ItemPickerScreen(Screen parent, String current, Consumer<String> onPick) {
        super(Component.translatable(KEY_TITLE), parent);
        this.current = current == null ? "" : current;
        this.onPick = onPick;
    }

    private void collect() {
        inventory.clear();
        everything.clear();
        Set<String> seen = new LinkedHashSet<>();
        if (minecraft != null && minecraft.player != null) {
            for (ItemStack stack : minecraft.player.getInventory().items) {
                if (!stack.isEmpty() && seen.add(idOf(stack))) {
                    inventory.add(stack);
                }
            }
            ItemStack offhand = minecraft.player.getOffhandItem();
            if (!offhand.isEmpty() && seen.add(idOf(offhand))) {
                inventory.add(offhand);
            }
        }
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty()) {
                everything.add(stack);
            }
        }
    }

    private static String idOf(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    @Override
    protected int panelWidth() {
        return fittedWidth(PANEL_WIDTH);
    }

    @Override
    protected int rowWidth(ItemStack row) {
        return splitRowWidth(font.width(row.getHoverName().getString()) + ICON_SIZE, idOf(row), 18);
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
    protected int listTop() {
        return super.listTop() + EXTRA_HEADER;
    }

    @Override
    protected List<ItemStack> choices() {
        if (everything.isEmpty()) {
            collect();
        }
        return showingInventory ? inventory : everything;
    }

    @Override
    protected String searchTextOf(ItemStack row) {
        return idOf(row) + " " + row.getHoverName().getString().toLowerCase(Locale.ROOT);
    }

    @Override
    protected Component searchHint() {
        return Component.translatable(KEY_SEARCH);
    }

    @Override
    protected Component headerNote() {
        return Component.translatable(KEY_COUNT, visible().size());
    }

    @Override
    protected Component emptyLabel() {
        return Component.translatable(KEY_EMPTY);
    }

    @Override
    protected void onPicked(ItemStack row) {
        onPick.accept(idOf(row));
    }

    @Override
    protected void addFooterButtons(int buttonY) {
        super.addFooterButtons(buttonY);
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_TAB_INVENTORY),
                        button -> switchTab(true))
                .bounds(contentLeft(), top + TAB_Y, TAB_WIDTH, SEARCH_HEIGHT + 2).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_TAB_ALL),
                        button -> switchTab(false))
                .bounds(contentLeft() + TAB_WIDTH + 4, top + TAB_Y,
                        TAB_WIDTH, SEARCH_HEIGHT + 2).build());
    }

    private void switchTab(boolean toInventory) {
        showingInventory = toInventory;
        scroll = 0;
        reload();
    }

    @Override
    protected void renderRow(GuiGraphics graphics, ItemStack row, int index,
                             int x, int y, int width, boolean hovered) {
        String id = idOf(row);
        boolean selected = id.equals(current);
        ScreenDraw.rowBackground(graphics, x + 1, y, width - 2, ROW_HEIGHT - 1,
                index, hovered, selected);
        graphics.renderItem(row, x + 2, y);
        graphics.drawString(font, row.getHoverName(), x + ICON_SIZE + 6, y + TEXT_INSET,
                selected ? ScreenTheme.GOOD : ScreenTheme.TEXT, false);
        String shortId = shortId(id, width);
        graphics.drawString(font, shortId, x + width - font.width(shortId) - 4,
                y + TEXT_INSET, ScreenTheme.MUTED, false);
    }
}
