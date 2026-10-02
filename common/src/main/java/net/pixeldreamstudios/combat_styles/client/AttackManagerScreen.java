package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.CustomAttack;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.style.CustomAttacks;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class AttackManagerScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 236;
    private static final int PADDING = ScreenTheme.PADDING;
    private static final int HEADER = 52;
    private static final int FOOTER = 44;
    private static final int ROW_HEIGHT = 20;
    private static final int SEARCH_HEIGHT = 14;
    private static final int BUTTON_WIDTH = 74;
    private static final int DELETE_WIDTH = 12;
    private static final int SEARCH_MAX = 64;

    private static final String KEY_TITLE = "screen.combat_styles.attack_manager";
    private static final String KEY_SEARCH = "screen.combat_styles.search_attacks";
    private static final String KEY_COUNT = "screen.combat_styles.attack_count";
    private static final String KEY_EMPTY = "screen.combat_styles.no_attacks";
    private static final String KEY_NEW = "screen.combat_styles.new_attack";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_HINT = "screen.combat_styles.attack_manager_hint";

    private final String defaultCategory;
    private final boolean creative;
    private final Consumer<CustomAttack> onSaved;
    private final Consumer<String> onDeleted;
    private final List<CustomAttack> available = new ArrayList<>();

    private EditBox searchBox;
    private String search = "";

    public AttackManagerScreen(Screen parent, String defaultCategory, boolean creative,
                               Consumer<CustomAttack> onSaved, Consumer<String> onDeleted) {
        super(Component.translatable(KEY_TITLE), parent,
                new Scrollbar(ScreenTheme.TRACK, ScreenTheme.ACCENT));
        this.defaultCategory = defaultCategory;
        this.creative = creative;
        this.onSaved = onSaved;
        this.onDeleted = onDeleted;
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

        searchBox = new FieldBox(font, left + PADDING, top + HEADER - SEARCH_HEIGHT - 4,
                PANEL_WIDTH - PADDING * 2, SEARCH_HEIGHT, Component.translatable(KEY_SEARCH));
        searchBox.setHint(Component.translatable(KEY_SEARCH).withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setMaxLength(SEARCH_MAX);
        searchBox.setValue(search);
        searchBox.setResponder(value -> {
            search = value;
            scroll = 0;
            rebuild();
        });
        addRenderableWidget(searchBox);

        int buttonY = top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - 6;
        if (creative) {
            addRenderableWidget(PanelButton.of(Component.translatable(KEY_NEW),
                            button -> openBuilder(null))
                    .bounds(left + PADDING, buttonY, BUTTON_WIDTH + 10,
                            ScreenTheme.BUTTON_HEIGHT).build());
        }
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());

        rebuild();
    }

    private void openBuilder(CustomAttack existing) {
        minecraft.setScreen(new AttackBuilderScreen(this, defaultCategory, created -> {
            onSaved.accept(created);
            rebuild();
        }, existing));
    }

    private void rebuild() {
        available.clear();
        String needle = search.trim().toLowerCase(Locale.ROOT);
        for (CustomAttack attack : CustomAttacks.all()) {
            if (needle.isEmpty() || attack.id().toLowerCase(Locale.ROOT).contains(needle)
                    || attack.attack().animation().toLowerCase(Locale.ROOT).contains(needle)) {
                available.add(attack);
            }
        }
        scroll = Math.clamp(scroll, 0, maxScroll());
    }

    @Override
    protected int listTop() {
        return top + HEADER;
    }

    @Override
    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER - 6;
    }

    @Override
    protected int rowHeight() {
        return ROW_HEIGHT;
    }

    @Override
    protected int rowCount() {
        return available.size();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(font, Component.translatable(KEY_TITLE),
                left + PADDING, top + 10, ScreenTheme.HEADING, false);
        graphics.drawString(font, Component.translatable(KEY_COUNT, available.size()),
                left + PADDING, top + 23, ScreenTheme.MUTED, false);

        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        ScreenDraw.sunken(graphics, x - 2, listTop() - 2, listWidth + 2,
                listBottom() - listTop() + 4);

        if (available.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_EMPTY),
                    x + 6, listTop() + 6, ScreenTheme.MUTED, false);
            drawWidgets(graphics, mouseX, mouseY, partialTick);
            return;
        }

        graphics.enableScissor(x - 2, listTop() - 2, x + listWidth, listBottom() + 2);
        int rowWidth = listWidth - Scrollbar.WIDTH - 2;
        int y = listTop();
        int last = Math.min(available.size(), scroll + visibleRows());
        for (int i = scroll; i < last; i++) {
            CustomAttack attack = available.get(i);
            boolean editable = CustomAttacks.isConfigOwned(attack.id());
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y, rowWidth, ROW_HEIGHT);
            ScreenDraw.rowBackground(graphics, x, y, rowWidth, ROW_HEIGHT, i, hovered, false);
            Integer tint = attack.rarityOrDefault().color().getColor();
            ScreenDraw.accentBar(graphics, x, y, ROW_HEIGHT,
                    tint == null ? ScreenTheme.MUTED : tint | 0xFF000000);

            String name = StyleText.prettify(shortId(attack.id()));
            int textWidth = rowWidth - DELETE_WIDTH - 16;
            graphics.drawString(font, ScreenDraw.trim(font, name, textWidth),
                    x + 8, y + 3, editable ? ScreenTheme.TEXT : ScreenTheme.MUTED, false);
            graphics.drawString(font,
                    ScreenDraw.trim(font, attack.attack().animation(), textWidth),
                    x + 8, y + 12, ScreenTheme.DIM, false);

            if (editable && creative) {
                boolean overDelete = ScreenDraw.isWithin(mouseX, mouseY,
                        x + rowWidth - DELETE_WIDTH - 2, y + 3, DELETE_WIDTH, ROW_HEIGHT - 6);
                graphics.drawString(font, "x", x + rowWidth - DELETE_WIDTH, y + 7,
                        overDelete ? ScreenTheme.BAD : ScreenTheme.MUTED, false);
            } else {
                graphics.drawString(font, "■", x + rowWidth - DELETE_WIDTH, y + 7,
                        ScreenTheme.DIM, false);
            }
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();

        scrollbar.render(graphics, x + listWidth - Scrollbar.WIDTH - 1, listTop(), listBottom(),
                scroll, maxScroll(), available.size(), visibleRows());

        graphics.drawString(font, Component.translatable(KEY_HINT),
                x, listBottom() + 8, ScreenTheme.DIM, false);
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    private static String shortId(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(searchBox);
            return true;
        }
        if (scrollbar.isOverTrack(mouseX, mouseY)) {
            scroll = scrollbar.beginDrag(mouseX, mouseY, scroll);
            return true;
        }

        int x = left + PADDING;
        int rowWidth = PANEL_WIDTH - PADDING * 2 - Scrollbar.WIDTH - 2;
        int y = listTop();
        int last = Math.min(available.size(), scroll + visibleRows());
        for (int i = scroll; i < last; i++) {
            CustomAttack attack = available.get(i);
            boolean editable = CustomAttacks.isConfigOwned(attack.id());
            if (editable && creative && ScreenDraw.isWithin(mouseX, mouseY,
                    x + rowWidth - DELETE_WIDTH - 2, y + 3, DELETE_WIDTH, ROW_HEIGHT - 6)) {
                if (!ConfigEdits.permitted()) {
                    ConfigEdits.tell(ConfigEdits.operatorOnly());
                } else if (ConfigEdits.remove(EditConfigPayload.Kind.ATTACK_DELETE, attack.id())) {
                    onDeleted.accept(attack.id());
                    rebuild();
                }
                return true;
            }
            if (ScreenDraw.isWithin(mouseX, mouseY, x, y, rowWidth, ROW_HEIGHT)) {
                if (editable && creative) {
                    openBuilder(attack);
                }
                return true;
            }
            y += ROW_HEIGHT;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.clamp(scroll - (int) scrollY, 0, maxScroll());
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
