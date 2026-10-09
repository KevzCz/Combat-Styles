package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.equipment.WeaponMatch;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GiveItemScreen extends PickerScreen<ItemStack> {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 232;
    private static final int ROW_HEIGHT = 18;
    private static final int TAB_WIDTH = 80;
    private static final int TAB_Y = 46;
    private static final int ICON_SIZE = 16;
    private static final int EXTRA_HEADER = 22;
    private static final int ITEM_SLOT_OFFSET = 36;
    private static final int STACK_STEP = 8;

    private static final String KEY_TITLE = "screen.combat_styles.give_picker";
    private static final String KEY_SEARCH = "screen.combat_styles.search_items";
    private static final String KEY_COUNT = "screen.combat_styles.give_count";
    private static final String KEY_TAB_TARGETS = "screen.combat_styles.give_tab_targets";
    private static final String KEY_TAB_ALL = "screen.combat_styles.item_tab_all";
    private static final String KEY_EMPTY = "screen.combat_styles.no_items";
    private static final String KEY_GIVE = "screen.combat_styles.equipment_give";
    private static final String KEY_AMOUNT = "screen.combat_styles.give_amount";

    private final List<String> targetIds;
    private final List<String> targetTags;
    private final List<String> targetCategories;
    private final List<ItemStack> targets = new ArrayList<>();
    private final List<ItemStack> everything = new ArrayList<>();

    private boolean showingTargets = true;
    private ItemStack chosen = ItemStack.EMPTY;
    private int amount = 1;

    public GiveItemScreen(Screen parent, List<String> targetIds) {
        this(parent, targetIds, List.of(), List.of());
    }

    public GiveItemScreen(Screen parent, List<String> targetIds, List<String> targetTags,
                          List<String> targetCategories) {
        super(Component.translatable(KEY_TITLE), parent);
        this.targetIds = targetIds == null ? List.of() : List.copyOf(targetIds);
        this.targetTags = targetTags == null ? List.of() : List.copyOf(targetTags);
        this.targetCategories = targetCategories == null
                ? List.of()
                : List.copyOf(targetCategories);
    }

    private void collect() {
        targets.clear();
        everything.clear();
        Set<String> seen = new LinkedHashSet<>();
        for (String id : targetIds) {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed != null && BuiltInRegistries.ITEM.containsKey(parsed) && seen.add(id)) {
                targets.add(new ItemStack(BuiltInRegistries.ITEM.get(parsed)));
            }
        }
        collectTagged(seen);
        collectCategorised(seen);
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty()) {
                everything.add(stack);
            }
        }
        if (targets.isEmpty()) {
            showingTargets = false;
        }
    }

    private void collectTagged(Set<String> seen) {
        for (String tag : targetTags) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag);
            if (parsed == null) {
                continue;
            }
            TagKey<Item> key = TagKey.create(Registries.ITEM, parsed);
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty() && stack.is(key) && seen.add(idOf(stack))) {
                    targets.add(stack);
                }
            }
        }
    }

    private void collectCategorised(Set<String> seen) {
        if (targetCategories.isEmpty()) {
            return;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty() || seen.contains(idOf(stack))) {
                continue;
            }
            if (WeaponMatch.matches(stack, targetCategories, List.of())
                    && seen.add(idOf(stack))) {
                targets.add(stack);
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
    protected boolean searchable() {
        return true;
    }

    @Override
    protected boolean closeOnPick() {
        return false;
    }

    @Override
    protected List<ItemStack> choices() {
        if (everything.isEmpty()) {
            collect();
        }
        return showingTargets ? targets : everything;
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
        chosen = row;
    }

    @Override
    protected void addFooterButtons(int buttonY) {
        super.addFooterButtons(buttonY);
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_TAB_TARGETS),
                        button -> switchTab(true))
                .bounds(contentLeft(), top + TAB_Y, TAB_WIDTH, SEARCH_HEIGHT + 2).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_TAB_ALL),
                        button -> switchTab(false))
                .bounds(contentLeft() + TAB_WIDTH + 4, top + TAB_Y,
                        TAB_WIDTH, SEARCH_HEIGHT + 2).build());
        addRenderableWidget(PanelButton.of(amountLabel(), button -> {
            amount = amount >= STACK_STEP * STACK_STEP ? 1 : amount * STACK_STEP;
            button.setMessage(amountLabel());
        }).bounds(contentLeft() + (TAB_WIDTH + 4) * 2, top + TAB_Y,
                TAB_WIDTH / 2, SEARCH_HEIGHT + 2).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_GIVE),
                        button -> give())
                .bounds(contentRight() - BUTTON_WIDTH * 2 - 4, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private Component amountLabel() {
        return Component.translatable(KEY_AMOUNT, amount);
    }

    private void switchTab(boolean toTargets) {
        showingTargets = toTargets;
        scroll = 0;
        reload();
    }

    private void give() {
        if (chosen.isEmpty() || minecraft.player == null
                || !minecraft.player.getAbilities().instabuild) {
            return;
        }
        ItemStack stack = chosen.copy();
        stack.setCount(Math.min(amount, stack.getMaxStackSize()));
        Inventory inventory = minecraft.player.getInventory();
        int slot = inventory.getFreeSlot();
        if (slot < 0) {
            slot = inventory.selected;
        }
        inventory.setItem(slot, stack);
        minecraft.gameMode.handleCreativeModeItemAdd(stack, slotIndexOf(slot));
    }

    private static int slotIndexOf(int slot) {
        return slot < Inventory.getSelectionSize()
                ? slot + ITEM_SLOT_OFFSET
                : slot;
    }

    @Override
    protected void renderRow(GuiGraphics graphics, ItemStack row, int index,
                             int x, int y, int width, boolean hovered) {
        boolean selected = !chosen.isEmpty() && idOf(row).equals(idOf(chosen));
        ScreenDraw.rowBackground(graphics, x + 1, y, width - 2, ROW_HEIGHT - 1,
                index, hovered, selected);
        graphics.renderItem(row, x + 2, y);
        String shortId = shortId(idOf(row), width);
        int idWidth = font.width(shortId);
        ScreenDraw.drawTrimmed(graphics, font, row.getHoverName().getString(),
                x + ICON_SIZE + 6, y + TEXT_INSET, width - idWidth - ICON_SIZE - 14, selected ? ScreenTheme.GOOD : ScreenTheme.TEXT);
        drawId(graphics, idOf(row), x + width - 4, y + TEXT_INSET, width, ScreenTheme.MUTED);
    }
}
