package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class PickerScreen<T> extends PanelScreen {

    protected static final int TITLE_Y = 10;
    protected static final int SEARCH_Y = 26;
    protected static final int SEARCH_HEIGHT = 14;
    protected static final int SEARCH_MAX = 64;
    protected static final int BUTTON_WIDTH = 74;
    protected static final int BUTTON_GAP = 4;
    protected static final int TEXT_INSET = 4;
    protected static final int ID_CAP = 140;
    private static final int MAX_PANEL_WIDTH = 440;
    private static final int SCREEN_MARGIN = 24;
    private static final int ROW_SLACK = 12;
    private static final int UNSIZED = -1;

    private static final String KEY_CANCEL = "gui.cancel";

    private final List<T> all = new ArrayList<>();
    private final List<T> shown = new ArrayList<>();

    private EditBox searchBox;
    private int fitted = UNSIZED;
    private int fittedFor = UNSIZED;

    protected PickerScreen(Component title, Screen parent) {
        super(title, parent);
    }

    protected abstract List<T> choices();

    protected abstract String searchTextOf(T row);

    protected abstract void renderRow(GuiGraphics graphics, T row, int index,
                                      int x, int y, int width, boolean hovered);

    protected abstract void onPicked(T row);

    protected boolean searchable() {
        return choices().size() > MIN_SEARCHABLE;
    }

    protected static final int MIN_SEARCHABLE = 8;

    protected boolean closeOnPick() {
        return true;
    }

    protected boolean selectable(T row) {
        return true;
    }

    protected Component searchHint() {
        return Component.translatable(KEY_SEARCH_DEFAULT);
    }

    private static final String KEY_SEARCH_DEFAULT = "screen.combat_styles.multi_search";

    protected Component headerNote() {
        return null;
    }

    protected Component emptyLabel() {
        return null;
    }

    protected void onHover(T row) {
    }

    protected List<Component> tooltipFor(T row) {
        return null;
    }

    protected int rowWidth(T row) {
        return 0;
    }

    protected final int fittedWidth(int baseWidth) {
        if (fittedFor != width) {
            int widest = 0;
            for (T row : choices()) {
                widest = Math.max(widest, rowWidth(row));
            }
            int wanted = widest + ROW_SLACK + ScreenTheme.PADDING * 2;
            int cap = Math.min(MAX_PANEL_WIDTH, width - SCREEN_MARGIN * 2);
            fitted = Math.max(baseWidth, Math.min(wanted, cap));
            fittedFor = width;
        }
        return fitted;
    }

    protected int sidePanelX(int sideWidth, int gap) {
        int right = left + panelWidth() + gap;
        if (right + sideWidth <= width - SCREEN_MARGIN) {
            return right;
        }
        return Math.max(SCREEN_MARGIN, left - gap - sideWidth);
    }

    protected int idWidth(String id) {
        return Math.min(font.width(id), ID_CAP);
    }

    protected int splitRowWidth(int labelWidth, String id, int margin) {
        int idWidth = idWidth(id);
        return Math.max(labelWidth + idWidth + margin, idWidth * 2);
    }

    protected String shortId(String id, int rowWidth) {
        return ScreenDraw.trim(font, id, idCap(rowWidth));
    }

    protected int idCap(int rowWidth) {
        return Math.min(ID_CAP, rowWidth / 2);
    }

    protected void drawId(GuiGraphics graphics, String id, int right, int y, int rowWidth, int color) {
        ScreenDraw.drawTrimmed(graphics, font, id, right - font.width(shortId(id, rowWidth)), y,
                idCap(rowWidth), color);
    }

    protected void addFooterButtons(int buttonY) {
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CANCEL),
                        button -> minecraft.setScreen(parent))
                .bounds(contentRight() - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    protected List<T> visible() {
        return shown;
    }

    @Override
    protected int rowCount() {
        return shown.size();
    }

    @Override
    protected int listTop() {
        return top + (searchable() ? SEARCH_Y + SEARCH_HEIGHT + 8 : SEARCH_Y);
    }

    @Override
    protected int listBottom() {
        return footerTop() - 6;
    }

    public void reload() {
        all.clear();
        all.addAll(choices());
        refilter(searchBox == null ? "" : searchBox.getValue());
    }

    protected void refilter(String text) {
        String needle = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
        shown.clear();
        if (rebuildsOnSearch()) {
            all.clear();
            all.addAll(rowsFor(needle));
            shown.addAll(all);
            clampScroll();
            return;
        }
        for (T row : all) {
            String haystack = searchTextOf(row);
            if (needle.isEmpty() || haystack == null
                    || haystack.toLowerCase(Locale.ROOT).contains(needle)) {
                shown.add(row);
            }
        }
        clampScroll();
    }

    protected boolean rebuildsOnSearch() {
        return false;
    }

    protected List<T> rowsFor(String needle) {
        return choices();
    }

    @Override
    protected void layout() {
        if (all.isEmpty()) {
            all.addAll(choices());
            shown.addAll(all);
        }
        if (searchable()) {
            searchBox = new FieldBox(font, contentLeft(), top + SEARCH_Y,
                    contentWidth(), SEARCH_HEIGHT, Component.empty());
            searchBox.setMaxLength(SEARCH_MAX);
            searchBox.setHint(searchHint());
            searchBox.setResponder(this::refilter);
            addRenderableWidget(searchBox);
            setInitialFocus(searchBox);
        }
        addFooterButtons(footerTop());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (beginScrollDrag(mouseX, mouseY)) {
            return true;
        }
        int index = rowAt(mouseX, mouseY);
        if (index >= 0 && index < shown.size()) {
            T row = shown.get(index);
            if (!selectable(row)) {
                return true;
            }
            onPicked(row);
            if (closeOnPick()) {
                minecraft.setScreen(parent);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        drawPanel(graphics);
        drawTitle(graphics, getTitle(), top + TITLE_Y);

        Component note = headerNote();
        if (note != null) {
            graphics.drawString(font, note, contentRight() - font.width(note),
                    top + TITLE_Y, ScreenTheme.MUTED, false);
        }

        ScreenDraw.sunken(graphics, contentLeft() - 2, listTop() - 2,
                contentWidth() + 4, listBottom() - listTop() + 4);

        Component empty = emptyLabel();
        if (shown.isEmpty() && empty != null) {
            graphics.drawString(font, empty, contentLeft() + TEXT_INSET,
                    listTop() + TEXT_INSET, ScreenTheme.DIM, false);
        }

        int hovered = rowAt(mouseX, mouseY);
        int last = Math.min(shown.size(), scroll + visibleRows());
        graphics.enableScissor(contentLeft() - 2, listTop() - 2,
                contentRight() + 2, listBottom() + 2);
        for (int index = scroll; index < last; index++) {
            renderRow(graphics, shown.get(index), index, contentLeft(), rowY(index),
                    contentWidth(), index == hovered);
        }
        graphics.disableScissor();

        T hoveredRow = hovered >= 0 && hovered < shown.size() ? shown.get(hovered) : null;
        onHover(hoveredRow);

        drawScrollbar(graphics);

        if (hoveredRow != null) {
            List<Component> tip = tooltipFor(hoveredRow);
            if (tip != null && !tip.isEmpty()) {
                ScreenDraw.tooltip(graphics, font, tip, mouseX, mouseY);
            }
        }
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }
}
