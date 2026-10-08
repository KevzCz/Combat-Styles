package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

public class AttackPickerScreen extends PickerScreen<StylePage> {

    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 224;
    private static final int ROW_HEIGHT = 14;
    private static final int TAG_GAP = 6;
    private static final int NAME_MARGIN = 16;

    private static final String KEY_TITLE = "screen.combat_styles.attack_picker";
    private static final String KEY_SEARCH = "screen.combat_styles.search_attacks";
    private static final String KEY_COUNT = "screen.combat_styles.attack_count";
    private static final String KEY_EMPTY = "screen.combat_styles.no_borrow_attacks";
    private static final String KEY_TUNED = "screen.combat_styles.attack_tuned";

    private final BiConsumer<StylePage, AttackOverride> onPick;
    private final List<StylePage> all = new ArrayList<>();

    public AttackPickerScreen(Screen parent, BiConsumer<StylePage, AttackOverride> onPick) {
        super(Component.translatable(KEY_TITLE), parent);
        this.onPick = onPick;
        all.addAll(StyleRegistry.distinct());
        all.sort(Comparator.comparing(AttackPickerScreen::labelOf)
                .thenComparing(StylePage::styleId));
    }

    private static String labelOf(StylePage page) {
        return StyleText.name(page).getString();
    }

    @Override
    protected int panelWidth() {
        return fittedWidth(PANEL_WIDTH);
    }

    @Override
    protected int rowWidth(StylePage row) {
        int tag = AttackOverrides.get(row.styleId()) == null
                ? 0
                : font.width(Component.translatable(KEY_TUNED)) + TAG_GAP;
        return splitRowWidth(font.width(StyleText.name(row).getString()) + tag,
                row.styleId(), NAME_MARGIN + TAG_GAP);
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
    protected List<StylePage> choices() {
        return all;
    }

    @Override
    protected String searchTextOf(StylePage row) {
        return labelOf(row) + " " + row.styleId() + " " + row.attack().animation();
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
    protected void onPicked(StylePage row) {
        AttackOverride tuned = AttackOverrides.get(row.styleId());
        onPick.accept(row, tuned == null ? AttackOverride.EMPTY : tuned);
    }

    @Override
    protected List<Component> tooltipFor(StylePage row) {
        return RefTooltip.forStyle(row.styleId());
    }

    @Override
    protected void renderRow(GuiGraphics graphics, StylePage row, int index,
                             int x, int y, int width, boolean hovered) {
        ScreenDraw.rowBackground(graphics, x + 1, y, width - 2, ROW_HEIGHT - 1,
                index, hovered, false);
        int tagWidth = 0;
        if (AttackOverrides.get(row.styleId()) != null) {
            Component tuned = Component.translatable(KEY_TUNED);
            tagWidth = font.width(tuned) + TAG_GAP;
            graphics.drawString(font, tuned, x + width - font.width(tuned) - TAG_GAP,
                    y + TEXT_INSET, ScreenTheme.ACCENT, false);
        }
        String id = shortId(row.styleId(), width);
        int idWidth = font.width(id);
        graphics.drawString(font, id,
                x + width - tagWidth - idWidth - TAG_GAP, y + TEXT_INSET,
                ScreenTheme.DIM, false);
        graphics.drawString(font,
                ScreenDraw.trim(font, StyleText.name(row).getString(),
                        width - tagWidth - idWidth - NAME_MARGIN),
                x + TEXT_INSET, y + TEXT_INSET, ScreenTheme.TEXT, false);
    }
}
