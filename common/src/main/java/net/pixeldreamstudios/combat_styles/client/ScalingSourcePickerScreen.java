package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ScalingSourcePickerScreen extends PickerScreen<ScalingSourcePickerScreen.Row> {

    public record Row(String source, String label, boolean header) {
    }

    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 232;
    private static final int ROW_HEIGHT = 14;
    private static final int TRIM_MARGIN = 40;

    private static final String KEY_TITLE = "screen.combat_styles.source_picker";
    private static final String KEY_SEARCH = "screen.combat_styles.source_search";
    private static final String KEY_COUNT = "screen.combat_styles.choice_count";
    private static final String KEY_EMPTY = "screen.combat_styles.choice_empty";

    private final String current;
    private final Consumer<String> onPick;
    private int sourceCount;

    public ScalingSourcePickerScreen(Screen parent, Component subtitle, String current,
                                     Consumer<String> onPick) {
        super(subtitle == null ? Component.translatable(KEY_TITLE) : subtitle, parent);
        this.current = current == null ? "" : current;
        this.onPick = onPick;
    }

    @Override
    protected int panelWidth() {
        return fittedWidth(PANEL_WIDTH);
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
    protected int rowWidth(Row row) {
        return row.header()
                ? font.width(row.label()) + TEXT_INSET * 2
                : splitRowWidth(font.width(row.label()), row.source(), TRIM_MARGIN);
    }

    @Override
    protected boolean searchable() {
        return true;
    }

    @Override
    protected boolean rebuildsOnSearch() {
        return true;
    }

    @Override
    protected boolean selectable(Row row) {
        return !row.header();
    }

    @Override
    protected Component searchHint() {
        return Component.translatable(KEY_SEARCH);
    }

    @Override
    protected Component headerNote() {
        return Component.translatable(KEY_COUNT, sourceCount);
    }

    @Override
    protected Component emptyLabel() {
        return Component.translatable(KEY_EMPTY);
    }

    @Override
    protected String searchTextOf(Row row) {
        return row.label() + " " + row.source();
    }

    @Override
    protected List<Row> choices() {
        return rowsFor("");
    }

    @Override
    protected List<Row> rowsFor(String needle) {
        List<Row> rows = new ArrayList<>();
        int count = 0;
        for (ScalingSources.Category category : ScalingSources.categories()) {
            String categoryName = category.name().getString();
            boolean categoryMatches = matches(categoryName, needle);
            List<Row> matching = new ArrayList<>();
            for (String source : category.sources()) {
                String label = StyleText.scalingSourceName(source).getString();
                if (categoryMatches || matches(label + " " + source, needle)) {
                    matching.add(new Row(source, label, false));
                }
            }
            if (!matching.isEmpty()) {
                rows.add(new Row(null, categoryName, true));
                rows.addAll(matching);
                count += matching.size();
            }
        }
        sourceCount = count;
        return rows;
    }

    private static boolean matches(String text, String needle) {
        return needle == null || needle.isEmpty() || text.toLowerCase(Locale.ROOT).contains(needle);
    }

    @Override
    protected void onPicked(Row row) {
        onPick.accept(row.source());
    }

    @Override
    protected List<Component> tooltipFor(Row row) {
        return row.header() ? null : RefTooltip.forChoice(row.source());
    }

    @Override
    protected void renderRow(GuiGraphics graphics, Row row, int index,
                             int x, int y, int width, boolean hovered) {
        if (row.header()) {
            graphics.drawString(font, row.label(), x + TEXT_INSET, y + TEXT_INSET,
                    ScreenTheme.HEADING, false);
            return;
        }
        boolean selected = row.source().equals(current);
        ScreenDraw.rowBackground(graphics, x + 1, y, width - 2, ROW_HEIGHT - 1,
                index, hovered, selected);
        String shortId = shortId(row.source(), width);
        int idWidth = font.width(shortId);
        ScreenDraw.drawTrimmed(graphics, font, row.label(),
                x + TEXT_INSET, y + TEXT_INSET, width - idWidth - TRIM_MARGIN, selected ? ScreenTheme.GOOD : ScreenTheme.TEXT);
        drawId(graphics, row.source(), x + width - TEXT_INSET, y + TEXT_INSET, width, ScreenTheme.DIM);
    }
}
