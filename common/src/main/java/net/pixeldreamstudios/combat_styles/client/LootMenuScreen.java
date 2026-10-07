package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LootMenuScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 154;
    private static final int TITLE_Y = 10;
    private static final int FIRST_BUTTON_Y = 30;
    private static final int BUTTON_SPACING = 22;

    private static final String KEY_TITLE = "screen.combat_styles.loot_menu";
    private static final String KEY_EQUIPMENT = "screen.combat_styles.loot_menu_equipment";
    private static final String KEY_BOOKS = "screen.combat_styles.loot_menu_books";
    private static final String KEY_ROLLS = "screen.combat_styles.loot_menu_rolls";
    private static final String KEY_ABILITIES = "screen.combat_styles.loot_menu_abilities";
    private static final String KEY_CLOSE = "gui.back";

    private final boolean creative;

    public LootMenuScreen(Screen parent, boolean creative) {
        super(Component.translatable(KEY_TITLE), parent);
        this.creative = creative;
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
        int y = top + FIRST_BUTTON_Y;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_EQUIPMENT),
                        button -> minecraft.setScreen(new EquipmentEditorScreen(this)))
                .bounds(contentLeft(), y, contentWidth(), ScreenTheme.BUTTON_HEIGHT).build());
        y += BUTTON_SPACING;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_BOOKS),
                        button -> minecraft.setScreen(new StyleBookListScreen(this, creative)))
                .bounds(contentLeft(), y, contentWidth(), ScreenTheme.BUTTON_HEIGHT).build());
        y += BUTTON_SPACING;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_ROLLS),
                        button -> minecraft.setScreen(new WeaponRollListScreen(this, creative)))
                .bounds(contentLeft(), y, contentWidth(), ScreenTheme.BUTTON_HEIGHT).build());
        y += BUTTON_SPACING;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_ABILITIES),
                        button -> minecraft.setScreen(new SummonAbilityListScreen(this, creative)))
                .bounds(contentLeft(), y, contentWidth(), ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(contentLeft(), footerTop(), contentWidth(), ScreenTheme.BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics);
        drawTitle(graphics, title, top + TITLE_Y);
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }
}
