package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class ContentListScreen<T> extends PanelScreen {

    public enum Source {
        DATAPACK("screen.combat_styles.source_datapack"),
        CONFIG("screen.combat_styles.source_config"),
        OVERRIDDEN("screen.combat_styles.source_overridden"),
        BUILT_IN("screen.combat_styles.source_built_in");

        private final String key;

        Source(String key) {
            this.key = key;
        }

        public Component label() {
            return Component.translatable(key);
        }
    }

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 236;
    private static final int PADDING = ScreenTheme.PADDING;
    private static final int HEADER = 52;
    private static final int FOOTER = 44;
    private static final int ROW_HEIGHT = 20;
    private static final int SEARCH_HEIGHT = 14;
    private static final int BUTTON_WIDTH = 74;
    private static final int ACTION_WIDTH = 12;
    private static final int SEARCH_MAX = 64;
    private static final int TITLE_Y = 10;
    private static final int COUNT_Y = 23;
    private static final int NAME_Y = 3;
    private static final int SUBTITLE_Y = 12;
    private static final int ACTION_Y = 7;
    private static final int TEXT_INSET = 8;
    private static final int SOURCE_GAP = 6;
    private static final int OPAQUE = 0xFF000000;
    private static final String DELETE_GLYPH = "x";
    private static final String RESTORE_GLYPH = "↺";

    private static final String KEY_COUNT = "screen.combat_styles.content_count";
    private static final String KEY_EMPTY = "screen.combat_styles.content_empty";
    private static final String KEY_HINT = "screen.combat_styles.content_hint";
    private static final String KEY_NEW = "screen.combat_styles.content_new";
    private static final String KEY_SEARCH = "screen.combat_styles.content_search";
    private static final String KEY_CLOSE = "gui.back";

    private final boolean creative;
    private final List<T> visible = new ArrayList<>();
    private EditBox searchBox;
    private String search = "";
    private Component error;

    protected void showError(Component message) {
        error = message;
    }

    protected ContentListScreen(Component title, Screen parent, boolean creative) {
        super(title, parent, new Scrollbar(ScreenTheme.TRACK, ScreenTheme.ACCENT));
        this.creative = creative;
    }

    protected abstract List<T> entries();

    protected abstract String idOf(T entry);

    protected abstract String subtitleOf(T entry);

    protected abstract int tintOf(T entry);

    protected abstract Source sourceOf(T entry);

    protected abstract void openEditor(T existing);

    protected abstract boolean remove(T entry);

    protected boolean canCreate() {
        return true;
    }

    protected Component searchHint() {
        return Component.translatable(KEY_SEARCH);
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
                PANEL_WIDTH - PADDING * 2, SEARCH_HEIGHT, searchHint());
        searchBox.setHint(searchHint().copy().withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setMaxLength(SEARCH_MAX);
        searchBox.setValue(search);
        searchBox.setResponder(value -> {
            search = value;
            error = null;
            scroll = 0;
            refresh();
        });
        addRenderableWidget(searchBox);

        int buttonY = top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - 6;
        if (creative && canCreate()) {
            addRenderableWidget(PanelButton.of(Component.translatable(KEY_NEW),
                            button -> openEditor(null))
                    .bounds(left + PADDING, buttonY, BUTTON_WIDTH + 10,
                            ScreenTheme.BUTTON_HEIGHT).build());
        }
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());

        refresh();
    }

    protected void refresh() {
        visible.clear();
        String needle = search.trim().toLowerCase(Locale.ROOT);
        for (T entry : entries()) {
            if (needle.isEmpty() || idOf(entry).toLowerCase(Locale.ROOT).contains(needle)
                    || subtitleOf(entry).toLowerCase(Locale.ROOT).contains(needle)) {
                visible.add(entry);
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
        return visible.size();
    }

    private static boolean removable(Source source) {
        return source != Source.DATAPACK && source != Source.BUILT_IN;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(font, title, left + PADDING, top + TITLE_Y, ScreenTheme.HEADING, false);
        graphics.drawString(font, Component.translatable(KEY_COUNT, visible.size()),
                left + PADDING, top + COUNT_Y, ScreenTheme.MUTED, false);

        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        ScreenDraw.sunken(graphics, x - 2, listTop() - 2, listWidth + 2,
                listBottom() - listTop() + 4);

        if (visible.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_EMPTY),
                    x + 6, listTop() + 6, ScreenTheme.MUTED, false);
            drawFooter(graphics, x, listWidth);
            drawWidgets(graphics, mouseX, mouseY, partialTick);
            return;
        }

        graphics.enableScissor(x - 2, listTop() - 2, x + listWidth, listBottom() + 2);
        int rowWidth = listWidth - Scrollbar.WIDTH - Scrollbar.GRAB_MARGIN - 2;
        int y = listTop();
        int last = Math.min(visible.size(), scroll + visibleRows());
        for (int i = scroll; i < last; i++) {
            T entry = visible.get(i);
            Source source = sourceOf(entry);
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y, rowWidth, ROW_HEIGHT);
            ScreenDraw.rowBackground(graphics, x, y, rowWidth, ROW_HEIGHT, i, hovered, false);
            ScreenDraw.accentBar(graphics, x, y, ROW_HEIGHT, tintOf(entry) | OPAQUE);

            Component sourceLabel = source.label();
            int sourceWidth = font.width(sourceLabel);
            int textWidth = rowWidth - ACTION_WIDTH - sourceWidth - TEXT_INSET * 2 - SOURCE_GAP;
            ScreenDraw.drawTrimmed(graphics, font, StyleText.prettify(shortId(idOf(entry))),
                    x + TEXT_INSET, y + NAME_Y, textWidth, ScreenTheme.TEXT);
            ScreenDraw.drawTrimmed(graphics, font, subtitleOf(entry),
                    x + TEXT_INSET, y + SUBTITLE_Y, textWidth, ScreenTheme.DIM);
            graphics.drawString(font, sourceLabel,
                    x + rowWidth - ACTION_WIDTH - SOURCE_GAP - sourceWidth, y + ACTION_Y,
                    source == Source.OVERRIDDEN ? ScreenTheme.CHANGED : ScreenTheme.MUTED, false);

            if (creative && removable(source)) {
                boolean overAction = ScreenDraw.isWithin(mouseX, mouseY,
                        x + rowWidth - ACTION_WIDTH - 2, y + 3, ACTION_WIDTH, ROW_HEIGHT - 6);
                graphics.drawString(font,
                        source == Source.OVERRIDDEN ? RESTORE_GLYPH : DELETE_GLYPH,
                        x + rowWidth - ACTION_WIDTH, y + ACTION_Y,
                        overAction ? ScreenTheme.BAD : ScreenTheme.MUTED, false);
            }
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();

        scrollbar.render(graphics, x + listWidth - Scrollbar.WIDTH - 1, listTop(), listBottom(),
                scroll, maxScroll(), visible.size(), visibleRows());

        drawFooter(graphics, x, listWidth);
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    private void drawFooter(GuiGraphics graphics, int x, int listWidth) {
        Component footer = error == null ? Component.translatable(KEY_HINT) : error;
        ScreenDraw.drawTrimmed(graphics, font, footer.getString(),
                x, listBottom() + 8, listWidth, error == null ? ScreenTheme.DIM : ScreenTheme.BAD);
    }

    protected static String shortId(String id) {
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
        int rowWidth = PANEL_WIDTH - PADDING * 2 - Scrollbar.WIDTH - Scrollbar.GRAB_MARGIN - 2;
        int y = listTop();
        int last = Math.min(visible.size(), scroll + visibleRows());
        for (int i = scroll; i < last; i++) {
            T entry = visible.get(i);
            if (creative && removable(sourceOf(entry)) && ScreenDraw.isWithin(mouseX, mouseY,
                    x + rowWidth - ACTION_WIDTH - 2, y + 3, ACTION_WIDTH, ROW_HEIGHT - 6)) {
                if (!ConfigEdits.permitted()) {
                    ConfigEdits.tell(ConfigEdits.operatorOnly());
                } else if (remove(entry)) {
                    refresh();
                }
                return true;
            }
            if (ScreenDraw.isWithin(mouseX, mouseY, x, y, rowWidth, ROW_HEIGHT)) {
                if (creative) {
                    openEditor(entry);
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
