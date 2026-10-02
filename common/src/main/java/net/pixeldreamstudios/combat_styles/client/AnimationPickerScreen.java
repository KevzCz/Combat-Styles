package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.equipment.PoseTuning;

import java.util.List;
import java.util.function.Consumer;

public class AnimationPickerScreen extends PickerScreen<String> {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 214;
    private static final int ROW_HEIGHT = 16;
    private static final int PREVIEW_SIZE = 76;
    private static final int PREVIEW_GAP = 6;
    private static final int PREVIEW_LIFT = 8;
    private static final int PREVIEW_LABEL = 14;

    private static final String KEY_TITLE = "screen.combat_styles.animation_picker";
    private static final String KEY_SEARCH = "screen.combat_styles.search_animations";
    private static final String KEY_COUNT = "screen.combat_styles.animation_count";
    private static final String KEY_EMPTY = "screen.combat_styles.no_animations";

    private final Consumer<String> onPick;
    private final AnimationPreview preview = new AnimationPreview();
    private String previewing;

    public AnimationPickerScreen(Screen parent, Consumer<String> onPick) {
        this(parent, onPick, Component.translatable(KEY_TITLE));
    }

    protected AnimationPickerScreen(Screen parent, Consumer<String> onPick, Component title) {
        super(title, parent);
        this.onPick = onPick;
        AnimationCatalogue.reload();
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
    protected List<String> choices() {
        return AnimationCatalogue.all();
    }

    @Override
    protected String searchTextOf(String row) {
        return row;
    }

    @Override
    protected Component searchHint() {
        return Component.translatable(KEY_SEARCH);
    }

    @Override
    protected Component headerNote() {
        return Component.translatable(KEY_COUNT, visible().size(),
                AnimationCatalogue.all().size());
    }

    @Override
    protected Component emptyLabel() {
        return Component.translatable(KEY_EMPTY);
    }

    @Override
    protected void onPicked(String row) {
        onPick.accept(row);
    }

    @Override
    protected void onHover(String row) {
        if (row != null && !row.equals(previewing)) {
            preview.select(row, previewTuning());
            preview.play();
        }
        previewing = row;
    }

    protected PoseTuning previewTuning() {
        return PoseTuning.NONE;
    }

    protected AnimationPreview preview() {
        return preview;
    }

    @Override
    protected void renderRow(GuiGraphics graphics, String row, int index,
                             int x, int y, int width, boolean hovered) {
        ScreenDraw.rowBackground(graphics, x, y, width, ROW_HEIGHT, index, hovered, false);
        graphics.drawString(font, ScreenDraw.trim(font, rowText(row), width - 16),
                x + TEXT_INSET, y + TEXT_INSET,
                hovered ? ScreenTheme.ACCENT : ScreenTheme.TEXT, false);
    }

    protected String rowText(String row) {
        return row;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (previewing != null) {
            renderPreview(graphics, previewing);
        }
    }

    private void renderPreview(GuiGraphics graphics, String animation) {
        if (!preview.isAvailable() || animation.isBlank()) {
            return;
        }
        int panelX = left + PANEL_WIDTH + PREVIEW_GAP;
        int panelY = listTop() - PREVIEW_LIFT;
        ScreenDraw.panel(graphics, panelX, panelY, PREVIEW_SIZE, PREVIEW_SIZE + PREVIEW_LABEL);
        PreviewRenderer.render(graphics, panelX + 4, panelY + 4, PREVIEW_SIZE - 8, preview);
        graphics.drawString(font,
                ScreenDraw.trim(font, shortName(animation), PREVIEW_SIZE - 8),
                panelX + 4, panelY + PREVIEW_SIZE + 2, ScreenTheme.MUTED, false);
    }

    private static String shortName(String animation) {
        int colon = animation.indexOf(':');
        return colon < 0 ? animation : animation.substring(colon + 1);
    }

    @Override
    public void tick() {
        preview.tick();
    }
}
