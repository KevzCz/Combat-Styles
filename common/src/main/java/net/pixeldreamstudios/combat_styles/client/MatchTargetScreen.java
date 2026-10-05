package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import net.pixeldreamstudios.combat_styles.style.WeaponCategories;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public class MatchTargetScreen extends PickerScreen<MatchTargetScreen.Row> {

    public record Row(String id, ItemStack icon, Component label) {
    }

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 232;
    private static final int ROW_HEIGHT = 18;
    private static final int TAB_WIDTH = 68;
    private static final int TAB_Y = 46;
    private static final int TAB_GAP = 4;
    private static final int ICON_SIZE = 16;
    private static final int EXTRA_HEADER = 22;
    private static final String TAG_MARKER = "#";
    private static final String SLOT_LABEL_PREFIX = "field.combat_styles.";

    private static final String KEY_TITLE = "screen.combat_styles.match_targets";
    private static final String KEY_SEARCH = "screen.combat_styles.match_search";
    private static final String KEY_COUNT = "screen.combat_styles.match_count";
    private static final String KEY_EMPTY = "screen.combat_styles.match_empty";
    private static final String KEY_TAB_INVENTORY = "screen.combat_styles.item_tab_inventory";
    private static final String KEY_TAB_ALL = "screen.combat_styles.item_tab_all";
    private static final String KEY_TAB_TAGS = "screen.combat_styles.match_kind_tag";
    private static final String KEY_TAB_CATEGORIES = "screen.combat_styles.match_kind_category";
    private static final String KEY_COUNT_ANY = "screen.combat_styles.match_count_any";
    private static final String KEY_ANY = "screen.combat_styles.choice_any";
    private static final String KEY_SLOT_TITLE = "screen.combat_styles.slot_picker";
    private static final String KEY_SLOT_COUNT = "screen.combat_styles.slot_count";
    private static final String KEY_SLOT_NONE = "screen.combat_styles.slot_none";
    private static final String KEY_TAB_FITS = "screen.combat_styles.match_kind_fits";

    public enum Tab {
        INVENTORY,
        FITS,
        ALL,
        TAGS,
        CATEGORIES
    }

    private final List<String> items;
    private final List<String> tags;
    private final List<String> categories;
    private final Runnable onChanged;

    private static final Runnable NO_CHANGE = () -> {
    };

    private final List<Row> inventory = new ArrayList<>();
    private final List<Row> everything = new ArrayList<>();
    private final List<Row> tagRows = new ArrayList<>();
    private final List<Row> categoryRows = new ArrayList<>();
    private final List<Row> fitting = new ArrayList<>();

    private EquipmentSlot slot;
    private String picked = "";
    private Consumer<String> onPick;

    private Tab tab = Tab.INVENTORY;
    private boolean emptyMeansAny;

    public MatchTargetScreen(Screen parent, List<String> items, List<String> tags,
                             List<String> categories) {
        this(parent, items, tags, categories, Tab.INVENTORY);
    }

    public MatchTargetScreen(Screen parent, List<String> items, List<String> tags,
                             List<String> categories, Tab opening) {
        this(parent, items, tags, categories, opening, NO_CHANGE);
    }

    public MatchTargetScreen(Screen parent, List<String> items, List<String> tags,
                             List<String> categories, Tab opening, Runnable onChanged) {
        super(Component.translatable(KEY_TITLE), parent);
        this.items = items;
        this.tags = tags;
        this.categories = categories;
        this.tab = opening == null ? Tab.INVENTORY : opening;
        this.onChanged = onChanged == null ? NO_CHANGE : onChanged;
    }

    private MatchTargetScreen(Screen parent, Component title) {
        super(title, parent);
        this.items = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.categories = new ArrayList<>();
        this.onChanged = NO_CHANGE;
    }

    public static MatchTargetScreen forSlot(Screen parent, EquipmentSlot slot, String current,
                                            Consumer<String> onPick) {
        MatchTargetScreen screen = new MatchTargetScreen(parent, Component.translatable(KEY_SLOT_TITLE,
                Component.translatable(SLOT_LABEL_PREFIX + slot.getName())));
        screen.slot = slot;
        screen.picked = current == null ? "" : current;
        screen.onPick = onPick;
        screen.tab = Tab.FITS;
        return screen;
    }

    private static boolean fits(EquipmentSlot slot, ItemStack stack) {
        Equipable equipable = Equipable.get(stack);
        if (slot.isArmor() || slot == EquipmentSlot.BODY) {
            return equipable != null && equipable.getEquipmentSlot() == slot;
        }
        Item item = stack.getItem();
        if (item instanceof ProjectileWeaponItem || item instanceof TridentItem) {
            return true;
        }
        if (slot == EquipmentSlot.OFFHAND && item instanceof ShieldItem) {
            return true;
        }
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .modifiers().stream()
                .anyMatch(entry -> entry.attribute().equals(Attributes.ATTACK_DAMAGE));
    }

    public MatchTargetScreen emptyMeansAny() {
        this.emptyMeansAny = true;
        return this;
    }

    private void collect() {
        inventory.clear();
        everything.clear();
        tagRows.clear();
        categoryRows.clear();
        fitting.clear();
        Set<String> seen = new LinkedHashSet<>();
        if (minecraft != null && minecraft.player != null) {
            for (ItemStack stack : minecraft.player.getInventory().items) {
                if (!stack.isEmpty() && seen.add(idOf(stack))) {
                    inventory.add(rowOf(stack));
                }
            }
            ItemStack offhand = minecraft.player.getOffhandItem();
            if (!offhand.isEmpty() && seen.add(idOf(offhand))) {
                inventory.add(rowOf(offhand));
            }
        }
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty()) {
                everything.add(rowOf(stack));
                if (slot != null && fits(slot, stack)) {
                    fitting.add(rowOf(stack));
                }
            }
        }
        BuiltInRegistries.ITEM.getTagNames()
                .map(tag -> tag.location().toString())
                .sorted()
                .forEach(id -> tagRows.add(new Row(id, ItemStack.EMPTY,
                        Component.literal(TAG_MARKER + shortName(id)))));
        for (String category : WeaponCategories.allNames()) {
            categoryRows.add(new Row(category, ItemStack.EMPTY,
                    Component.literal(StyleText.prettify(shortName(category)))));
        }
    }

    private static Row rowOf(ItemStack stack) {
        return new Row(idOf(stack), stack, stack.getHoverName());
    }

    private static String idOf(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private static String shortName(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    private List<String> targetFor(Tab which) {
        return switch (which) {
            case TAGS -> tags;
            case CATEGORIES -> categories;
            default -> items;
        };
    }

    @Override
    protected int panelWidth() {
        return fittedWidth(PANEL_WIDTH);
    }

    @Override
    protected int rowWidth(Row row) {
        int icon = row.icon().isEmpty() ? 2 : ICON_SIZE + 6;
        return splitRowWidth(font.width(row.label().getString()) + icon, row.id(), 12);
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
    protected List<Row> choices() {
        if (everything.isEmpty()) {
            collect();
        }
        return switch (tab) {
            case ALL -> everything;
            case FITS -> fitting;
            case TAGS -> tagRows;
            case CATEGORIES -> categoryRows;
            default -> inventory;
        };
    }

    @Override
    protected String searchTextOf(Row row) {
        return row.id() + " " + row.label().getString().toLowerCase(Locale.ROOT);
    }

    @Override
    protected Component searchHint() {
        return Component.translatable(KEY_SEARCH);
    }

    @Override
    protected Component headerNote() {
        if (slot != null) {
            return Component.translatable(KEY_SLOT_COUNT, visible().size());
        }
        if (emptyMeansAny && chosenCount() == 0) {
            return Component.translatable(KEY_COUNT_ANY, visible().size());
        }
        return Component.translatable(KEY_COUNT, visible().size(), chosenCount());
    }

    private int chosenCount() {
        return items.size() + tags.size() + categories.size();
    }

    @Override
    protected Component emptyLabel() {
        return Component.translatable(KEY_EMPTY);
    }

    @Override
    protected boolean closeOnPick() {
        return slot != null;
    }

    @Override
    protected boolean searchable() {
        return true;
    }

    @Override
    protected void onPicked(Row row) {
        if (slot != null) {
            onPick.accept(row.id());
            return;
        }
        List<String> target = targetFor(tab);
        if (!target.remove(row.id())) {
            target.add(row.id());
        }
        onChanged.run();
    }

    @Override
    protected void addFooterButtons(int buttonY) {
        super.addFooterButtons(buttonY);
        if (slot != null) {
            addRenderableWidget(PanelButton.of(Component.translatable(KEY_SLOT_NONE), button -> {
                onPick.accept("");
                minecraft.setScreen(parent);
            }).bounds(contentLeft(), buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
            addTab(KEY_TAB_INVENTORY, Tab.INVENTORY, 0);
            addTab(KEY_TAB_FITS, Tab.FITS, 1);
            addTab(KEY_TAB_ALL, Tab.ALL, 2);
            return;
        }
        if (emptyMeansAny) {
            addRenderableWidget(PanelButton.of(Component.translatable(KEY_ANY), button -> {
                items.clear();
                tags.clear();
                categories.clear();
                onChanged.run();
            }).bounds(contentLeft(), buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        }
        addTab(KEY_TAB_INVENTORY, Tab.INVENTORY, 0);
        addTab(KEY_TAB_ALL, Tab.ALL, 1);
        addTab(KEY_TAB_TAGS, Tab.TAGS, 2);
        addTab(KEY_TAB_CATEGORIES, Tab.CATEGORIES, 3);
    }

    private void addTab(String labelKey, Tab which, int slot) {
        addRenderableWidget(PanelButton.of(Component.translatable(labelKey),
                        button -> switchTab(which))
                .bounds(contentLeft() + (TAB_WIDTH + TAB_GAP) * slot, top + TAB_Y,
                        TAB_WIDTH, SEARCH_HEIGHT + 2).build());
    }

    private void switchTab(Tab which) {
        tab = which;
        scroll = 0;
        reload();
    }

    @Override
    protected void renderRow(GuiGraphics graphics, Row row, int index,
                             int x, int y, int width, boolean hovered) {
        boolean chosen = slot != null ? row.id().equals(picked) : targetFor(tab).contains(row.id());
        ScreenDraw.rowBackground(graphics, x + 1, y, width - 2, ROW_HEIGHT - 1,
                index, hovered, chosen);
        int textX = x + 2;
        if (!row.icon().isEmpty()) {
            graphics.renderItem(row.icon(), x + 2, y);
            textX = x + ICON_SIZE + 6;
        }
        String shortId = shortId(row.id(), width);
        int idWidth = font.width(shortId);
        graphics.drawString(font,
                ScreenDraw.trim(font, row.label().getString(),
                        width - idWidth - (textX - x) - 8),
                textX, y + TEXT_INSET,
                chosen ? ScreenTheme.GOOD : ScreenTheme.TEXT, false);
        graphics.drawString(font, shortId, x + width - idWidth - 4,
                y + TEXT_INSET, ScreenTheme.MUTED, false);
    }
}
