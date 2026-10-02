package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class EquipmentAffixPickerScreen extends PickerScreen<StyleAffix> {

    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 224;
    private static final int ROW_HEIGHT = 14;
    private static final int NAME_MARGIN = 90;

    private static final String KEY_TITLE = "screen.combat_styles.affix_picker";
    private static final String KEY_SEARCH = "screen.combat_styles.search_affixes";
    private static final String KEY_COUNT = "screen.combat_styles.affix_count";
    private static final String KEY_EMPTY = "screen.combat_styles.no_affixes";

    private final Consumer<String> onPick;
    private final List<StyleAffix> all = new ArrayList<>();
    private boolean keepOpen;

    public EquipmentAffixPickerScreen(Screen parent, Collection<String> existing,
                                      Consumer<String> onPick) {
        super(Component.translatable(KEY_TITLE), parent);
        this.onPick = onPick;
        for (StyleAffix affix : AffixRegistry.all()) {
            if (existing == null || !existing.contains(affix.serializedId())) {
                all.add(affix);
            }
        }
        all.sort(Comparator.comparing(StyleAffix::serializedId));
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
    protected List<StyleAffix> choices() {
        return all;
    }

    @Override
    protected String searchTextOf(StyleAffix row) {
        return row.serializedId();
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

    public EquipmentAffixPickerScreen handlingNavigation() {
        this.keepOpen = true;
        return this;
    }

    @Override
    protected boolean closeOnPick() {
        return !keepOpen;
    }

    @Override
    protected void onPicked(StyleAffix row) {
        onPick.accept(row.serializedId());
    }

    @Override
    protected List<Component> tooltipFor(StyleAffix row) {
        return RefTooltip.forAffix(row.serializedId(), null);
    }

    @Override
    protected void renderRow(GuiGraphics graphics, StyleAffix row, int index,
                             int x, int y, int width, boolean hovered) {
        ScreenDraw.rowBackground(graphics, x + 1, y, width - 2, ROW_HEIGHT - 1,
                index, hovered, false);
        graphics.drawString(font,
                ScreenDraw.trim(font, StyleText.prettify(row.serializedId()),
                        width - NAME_MARGIN),
                x + TEXT_INSET, y + TEXT_INSET, ScreenTheme.TEXT, false);
        Component rarity = Component.translatable(row.rarity().translationKey())
                .copy().withStyle(row.rarity().color());
        graphics.drawString(font, rarity, x + width - font.width(rarity) - 6,
                y + TEXT_INSET, ScreenTheme.MUTED, false);
    }
}
