package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class UnsavedChangesScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 92;
    private static final int TITLE_Y = 10;
    private static final int MESSAGE_Y = 30;
    private static final int BUTTON_WIDTH = 74;
    private static final int BUTTON_GAP = 6;
    private static final int LINE_HEIGHT = 10;

    private static final String KEY_TITLE = "screen.combat_styles.unsaved_changes";
    private static final String KEY_MESSAGE = "screen.combat_styles.unsaved_changes.message";
    private static final String KEY_SAVE = "screen.combat_styles.unsaved_changes.save";
    private static final String KEY_DISCARD = "screen.combat_styles.unsaved_changes.discard";
    private static final String KEY_KEEP = "screen.combat_styles.unsaved_changes.keep";

    private final Runnable onSave;
    private final Runnable onDiscard;

    public UnsavedChangesScreen(Screen parent, Runnable onSave, Runnable onDiscard) {
        super(Component.translatable(KEY_TITLE), parent);
        this.onSave = onSave;
        this.onDiscard = onDiscard;
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
        int x = contentLeft();
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_SAVE), button -> onSave.run())
                .bounds(x, footerTop(), BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        x += BUTTON_WIDTH + BUTTON_GAP;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_DISCARD),
                        button -> onDiscard.run())
                .bounds(x, footerTop(), BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_KEEP),
                        button -> minecraft.setScreen(parent))
                .bounds(contentRight() - BUTTON_WIDTH, footerTop(), BUTTON_WIDTH,
                        ScreenTheme.BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics);
        drawTitle(graphics, title, top + TITLE_Y);
        int y = top + MESSAGE_Y;
        for (FormattedCharSequence line : font.split(Component.translatable(KEY_MESSAGE), contentWidth())) {
            graphics.drawString(font, line, contentLeft(), y, ScreenTheme.TEXT, false);
            y += LINE_HEIGHT;
        }
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
