package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class SchemaErrorScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 260;
    private static final int TITLE_Y = 10;
    private static final int BODY_TOP = 26;
    private static final int LINE_HEIGHT = 11;
    private static final int WHY_GAP = 4;
    private static final int BODY_GAP = 10;
    private static final int BUTTON_WIDTH = 70;

    private static final String KEY_TITLE = "screen.combat_styles.schema_error";
    private static final String KEY_BACK = "gui.back";

    private final Component cause;
    private final Component why;
    private final List<FormattedCharSequence> causeLines = new ArrayList<>();
    private final List<FormattedCharSequence> whyLines = new ArrayList<>();

    public SchemaErrorScreen(Screen parent, Component cause, Component why) {
        super(Component.translatable(KEY_TITLE), parent);
        this.cause = cause;
        this.why = why;
    }

    @Override
    protected int panelWidth() {
        return PANEL_WIDTH;
    }

    @Override
    protected int panelHeight() {
        wrap();
        int body = causeLines.size() * LINE_HEIGHT;
        if (!whyLines.isEmpty()) {
            body += WHY_GAP + whyLines.size() * LINE_HEIGHT;
        }
        return BODY_TOP + body + BODY_GAP + ScreenTheme.BUTTON_HEIGHT + ScreenTheme.PADDING;
    }

    private void wrap() {
        if (!causeLines.isEmpty()) {
            return;
        }
        causeLines.addAll(font.split(cause, contentWidth()));
        if (why != null) {
            whyLines.addAll(font.split(why, contentWidth()));
        }
    }

    @Override
    protected void layout() {
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_BACK),
                        button -> minecraft.setScreen(parent))
                .bounds(contentRight() - BUTTON_WIDTH, footerTop(),
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        drawPanel(graphics);
        drawTitle(graphics, Component.translatable(KEY_TITLE), top + TITLE_Y);

        int y = top + BODY_TOP;
        for (FormattedCharSequence line : causeLines) {
            graphics.drawString(font, line, contentLeft(), y, ScreenTheme.TEXT, false);
            y += LINE_HEIGHT;
        }
        y += WHY_GAP;
        for (FormattedCharSequence line : whyLines) {
            graphics.drawString(font, line, contentLeft(), y, ScreenTheme.MUTED, false);
            y += LINE_HEIGHT;
        }
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KEY_ESCAPE) {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
