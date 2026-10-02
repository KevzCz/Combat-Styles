package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class AttributePickerScreen extends PickerScreen<String> {

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 210;
    private static final int ROW_HEIGHT = 16;
    private static final String ATTRIBUTE_PREFIX = "attribute.name.";

    private static final String KEY_TITLE = "screen.combat_styles.attribute_picker";
    private static final String KEY_SEARCH = "screen.combat_styles.search_attributes";
    private static final String KEY_COUNT = "screen.combat_styles.attribute_count";
    private static final String KEY_EMPTY = "screen.combat_styles.no_attributes";

    private final Collection<String> existing;
    private final Consumer<String> onPick;
    private boolean keepOpen;
    private final List<String> all = new ArrayList<>();

    public AttributePickerScreen(Screen parent, Collection<String> existing,
                                 Consumer<String> onPick) {
        super(Component.translatable(KEY_TITLE), parent);
        this.existing = existing;
        this.onPick = onPick;
        BuiltInRegistries.ATTRIBUTE.keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(all::add);
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
        List<String> free = new ArrayList<>();
        for (String id : all) {
            if (existing == null || !existing.contains(id)) {
                free.add(id);
            }
        }
        return free;
    }

    @Override
    protected String searchTextOf(String row) {
        return row + " " + labelOf(row).getString().toLowerCase(Locale.ROOT);
    }

    @Override
    protected Component searchHint() {
        return Component.translatable(KEY_SEARCH);
    }

    @Override
    protected Component headerNote() {
        return Component.translatable(KEY_COUNT, visible().size(), all.size());
    }

    @Override
    protected Component emptyLabel() {
        return Component.translatable(KEY_EMPTY);
    }

    public AttributePickerScreen handlingNavigation() {
        this.keepOpen = true;
        return this;
    }

    @Override
    protected boolean closeOnPick() {
        return !keepOpen;
    }

    @Override
    protected void onPicked(String row) {
        onPick.accept(row);
    }

    private Component labelOf(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            return Component.literal(id);
        }
        String key = ATTRIBUTE_PREFIX + parsed.toString().replace(':', '.');
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(id);
    }

    @Override
    protected void renderRow(GuiGraphics graphics, String row, int index,
                             int x, int y, int width, boolean hovered) {
        ScreenDraw.rowBackground(graphics, x + 1, y, width - 2, ROW_HEIGHT - 1,
                index, hovered, false);
        String idShown = ScreenDraw.trim(font, row, width / 2);
        int idWidth = font.width(idShown);
        graphics.drawString(font,
                ScreenDraw.trim(font, labelOf(row).getString(), width - idWidth - 16),
                x + TEXT_INSET, y + TEXT_INSET, ScreenTheme.TEXT, false);
        graphics.drawString(font, idShown, x + width - idWidth - 6, y + TEXT_INSET,
                ScreenTheme.MUTED, false);
    }
}
