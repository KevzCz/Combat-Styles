package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EffectFieldPickerScreen extends PickerScreen<EffectSchema.Template> {

    private static final int PANEL_WIDTH = 240;
    private static final int HEADER = 34;
    private static final int FOOTER = 34;
    private static final int ROW_HEIGHT = 16;
    private static final int SEARCH_ROOM = 22;
    private static final int LIST_PAD = 6;
    private static final int MARKER_GAP = 2;

    private static final String KEY_TITLE = "screen.combat_styles.add_field";

    private final List<EffectSchema.Template> options;
    private final Consumer<EffectSchema.Template> onPick;
    private final String scope;

    public EffectFieldPickerScreen(Screen parent, String scope, List<EffectSchema.Template> options,
                                   Consumer<EffectSchema.Template> onPick) {
        super(Component.translatable(KEY_TITLE), parent);
        this.scope = scope == null ? "" : scope;
        this.options = options;
        this.onPick = onPick;
    }

    private String pathOf(EffectSchema.Template row) {
        return EffectSchema.join(scope, row.key());
    }

    @Override
    protected int panelWidth() {
        return PANEL_WIDTH;
    }

    @Override
    protected int panelHeight() {
        int chrome = HEADER + FOOTER + LIST_PAD + (searchable() ? SEARCH_ROOM : 0);
        int wanted = chrome + Math.max(1, options.size()) * ROW_HEIGHT;
        int room = height - ScreenTheme.PADDING * 2;
        if (wanted <= room) {
            return wanted;
        }
        int rows = Math.max(1, (room - chrome) / ROW_HEIGHT);
        return chrome + rows * ROW_HEIGHT;
    }

    @Override
    protected int rowHeight() {
        return ROW_HEIGHT;
    }

    @Override
    protected List<EffectSchema.Template> choices() {
        return options;
    }

    @Override
    protected String searchTextOf(EffectSchema.Template row) {
        return row.key();
    }

    @Override
    protected void onPicked(EffectSchema.Template row) {
        onPick.accept(row);
    }

    @Override
    protected List<Component> tooltipFor(EffectSchema.Template row) {
        List<Component> lines = new ArrayList<>(FieldDependency.lines(pathOf(row), row.key()));
        Component hint = EffectLabels.hintFor(pathOf(row), row.key());
        if (hint != null) {
            lines.add(EffectLabels.labelFor(pathOf(row), row.key()).copy()
                    .withStyle(ChatFormatting.WHITE));
            lines.add(hint.copy().withStyle(ChatFormatting.GRAY));
        }
        return lines.isEmpty() ? null : lines;
    }

    @Override
    protected void renderRow(GuiGraphics graphics, EffectSchema.Template row, int index,
                             int x, int y, int width, boolean hovered) {
        ScreenDraw.rowBackground(graphics, x + 1, y, width - 2, ROW_HEIGHT - 1,
                index, hovered, false);
        Component label = EffectLabels.labelFor(pathOf(row), row.key());
        graphics.drawString(font, label, x + TEXT_INSET,
                y + TEXT_INSET, hovered ? ScreenTheme.ACCENT : ScreenTheme.TEXT, false);
        int markerX = x + TEXT_INSET + font.width(label) + MARKER_GAP;
        for (FieldDependency.Marker marker : FieldDependency.markers(pathOf(row), row.key())) {
            graphics.drawString(font, marker.glyph(), markerX, y + TEXT_INSET, marker.color(), false);
            markerX += font.width(marker.glyph()) + MARKER_GAP;
        }
    }
}
