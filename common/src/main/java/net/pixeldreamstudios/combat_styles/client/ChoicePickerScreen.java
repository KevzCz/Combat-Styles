package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChoicePickerScreen extends PickerScreen<String> {

    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 220;
    private static final int ROW_HEIGHT = 14;
    private static final int TRIM_MARGIN = 40;
    private static final int ID_WIDTH = 84;

    private static final String KEY_TITLE = "screen.combat_styles.choice_picker";
    private static final String KEY_SEARCH = "screen.combat_styles.choice_search";
    private static final String KEY_COUNT = "screen.combat_styles.choice_count";
    private static final String KEY_EMPTY = "screen.combat_styles.choice_empty";
    private static final String KEY_CLEAR = "screen.combat_styles.choice_clear";
    private static final String KEY_CANCEL = "gui.cancel";
    private static final String KEY_ANY = "screen.combat_styles.choice_any";
    private static final String KEY_ANY_HINT = "screen.combat_styles.choice_any_hint";
    private static final String ANY_ROW = "*";

    private static final int PREVIEW_SIZE = 76;
    private static final int PREVIEW_GAP = 6;
    private static final int PREVIEW_LIFT = 8;
    private static final int PREVIEW_LABEL = 14;
    private static final int PREVIEW_PAD = 4;

    private final List<String> options = new ArrayList<>();
    private final String current;
    private final Consumer<String> onPick;
    private boolean previewsEntities;
    private boolean previewsSpells;
    private boolean handsOff;
    private String hovered;
    private Runnable onAny;
    private boolean anySelected;

    public ChoicePickerScreen(Screen parent, Component subtitle, List<String> choices,
                              String current, Consumer<String> onPick) {
        super(subtitle == null ? Component.translatable(KEY_TITLE) : subtitle, parent);
        this.current = current == null ? "" : current;
        this.onPick = onPick;
        this.options.addAll(choices);
    }

    public ChoicePickerScreen offeringAny(boolean selected, Runnable onPickAny) {
        this.onAny = onPickAny;
        this.anySelected = selected;
        if (!options.contains(ANY_ROW)) {
            options.addFirst(ANY_ROW);
        }
        return this;
    }

    public ChoicePickerScreen handingOffOnPick() {
        this.handsOff = true;
        return this;
    }

    @Override
    protected boolean closeOnPick() {
        return !handsOff;
    }

    public ChoicePickerScreen previewingEntities() {
        this.previewsEntities = true;
        return this;
    }

    public ChoicePickerScreen previewingSpells() {
        this.previewsSpells = true;
        return this;
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
        return options;
    }

    @Override
    protected String searchTextOf(String row) {
        if (ANY_ROW.equals(row)) {
            return Component.translatable(KEY_ANY).getString();
        }
        return StyleText.choiceName(row).getString() + " " + row;
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
    protected void onPicked(String row) {
        if (ANY_ROW.equals(row) && onAny != null) {
            onAny.run();
            return;
        }
        onPick.accept(row);
    }

    @Override
    protected List<Component> tooltipFor(String row) {
        if (ANY_ROW.equals(row)) {
            return List.of(Component.translatable(KEY_ANY_HINT));
        }
        if (previewsSpells) {
            return RefTooltip.forSpell(row);
        }
        if (previewsEntities) {
            return RefTooltip.forEntity(row);
        }
        return RefTooltip.forChoice(row);
    }

    @Override
    protected void addFooterButtons(int buttonY) {
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLEAR), button -> {
            onPick.accept("");
            minecraft.setScreen(parent);
        }).bounds(contentLeft(), buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CANCEL),
                        button -> minecraft.setScreen(parent))
                .bounds(contentRight() - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    @Override
    protected void onHover(String row) {
        hovered = row;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hovered = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null && (previewsEntities || previewsSpells) && !ANY_ROW.equals(hovered)) {
            renderPreview(graphics, hovered);
        }
    }

    private void renderPreview(GuiGraphics graphics, String id) {
        boolean entity = previewsEntities && EntityPreviewRenderer.renderable(id);
        boolean spell = previewsSpells && SpellPreviewRenderer.renderable(id);
        if (!entity && !spell) {
            return;
        }
        int panelX = left + PANEL_WIDTH + PREVIEW_GAP;
        int panelY = listTop() - PREVIEW_LIFT;
        int inner = PREVIEW_SIZE - PREVIEW_PAD * 2;
        ScreenDraw.panel(graphics, panelX, panelY, PREVIEW_SIZE, PREVIEW_SIZE + PREVIEW_LABEL);
        if (entity) {
            EntityPreviewRenderer.render(graphics, panelX + PREVIEW_PAD, panelY + PREVIEW_PAD,
                    inner, id);
        } else {
            SpellPreviewRenderer.render(graphics, panelX + PREVIEW_PAD, panelY + PREVIEW_PAD,
                    inner, id);
        }
        graphics.drawString(font,
                ScreenDraw.trim(font, StyleText.choiceName(id).getString(), inner),
                panelX + PREVIEW_PAD, panelY + PREVIEW_SIZE + 2, ScreenTheme.MUTED, false);
    }

    @Override
    protected void renderRow(GuiGraphics graphics, String row, int index,
                             int x, int y, int width, boolean hovered) {
        if (ANY_ROW.equals(row)) {
            ScreenDraw.rowBackground(graphics, x + 1, y, width - 2, rowHeight() - 1,
                    index, hovered, anySelected);
            graphics.drawString(font, Component.translatable(KEY_ANY), x + TEXT_INSET,
                    y + TEXT_INSET, anySelected ? ScreenTheme.GOOD : ScreenTheme.ACCENT, false);
            return;
        }
        boolean selected = !anySelected && row.equals(current);
        ScreenDraw.rowBackground(graphics, x + 1, y, width - 2, rowHeight() - 1,
                index, hovered, selected);
        String name = StyleText.choiceName(row).getString();
        if (name.isBlank() || name.equals(row)) {
            graphics.drawString(font,
                    ScreenDraw.trim(font, StyleText.prettify(row), width - TRIM_MARGIN),
                    x + TEXT_INSET, y + TEXT_INSET,
                    selected ? ScreenTheme.GOOD : ScreenTheme.TEXT, false);
            return;
        }
        String shortId = ScreenDraw.trim(font, row, Math.min(ID_WIDTH, width / 2));
        int idWidth = font.width(shortId);
        graphics.drawString(font,
                ScreenDraw.trim(font, name, width - idWidth - TRIM_MARGIN),
                x + TEXT_INSET, y + TEXT_INSET,
                selected ? ScreenTheme.GOOD : ScreenTheme.TEXT, false);
        graphics.drawString(font, shortId, x + width - idWidth - TEXT_INSET,
                y + TEXT_INSET, ScreenTheme.DIM, false);
    }

}
