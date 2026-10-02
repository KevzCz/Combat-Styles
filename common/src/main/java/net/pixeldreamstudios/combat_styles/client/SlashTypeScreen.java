package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.SlashMode;

import java.util.function.Consumer;

public class SlashTypeScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 148;
    private static final int HEADER = 44;
    private static final int ROW_HEIGHT = 26;
    private static final int TITLE_Y = 10;
    private static final int HINT_Y = 24;
    private static final int LABEL_X = 14;
    private static final int NAME_Y = 5;
    private static final int DESC_Y = 15;
    private static final int BUTTON_WIDTH = 70;

    private static final String KEY_TITLE = "screen.combat_styles.slash_type_title";
    private static final String KEY_HINT = "screen.combat_styles.slash_type_hint";
    private static final String KEY_CANCEL = "gui.cancel";
    private static final String KEY_NAME_PREFIX = "screen.combat_styles.slash_mode_";
    private static final String KEY_DESC_SUFFIX = "_desc";

    private final Consumer<SlashMode> onPick;

    public SlashTypeScreen(Screen parent, Consumer<SlashMode> onPick) {
        super(Component.translatable(KEY_TITLE), parent);
        this.onPick = onPick;
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

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CANCEL),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH,
                        top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    protected int rowY(int index) {
        return top + HEADER + index * ROW_HEIGHT;
    }

    private static int colourFor(SlashMode mode) {
        return switch (mode) {
            case PARTICLE -> ScreenTheme.ACCENT;
            case ATTACK -> ScreenTheme.BAD;
            case BOTH -> ScreenTheme.GOOD;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        SlashMode[] modes = SlashMode.values();
        for (int index = 0; index < modes.length; index++) {
            if (ScreenDraw.isWithin(mouseX, mouseY, left + ScreenTheme.PADDING,
                    rowY(index), PANEL_WIDTH - ScreenTheme.PADDING * 2, ROW_HEIGHT)) {
                onPick.accept(modes[index]);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(font, Component.translatable(KEY_TITLE),
                left + ScreenTheme.PADDING, top + TITLE_Y, ScreenTheme.HEADING, false);
        graphics.drawString(font, Component.translatable(KEY_HINT),
                left + ScreenTheme.PADDING, top + HINT_Y, ScreenTheme.MUTED, false);

        SlashMode[] modes = SlashMode.values();
        for (int index = 0; index < modes.length; index++) {
            SlashMode mode = modes[index];
            int y = rowY(index);
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY,
                    left + ScreenTheme.PADDING, y,
                    PANEL_WIDTH - ScreenTheme.PADDING * 2, ROW_HEIGHT);
            ScreenDraw.rowBackground(graphics, left + ScreenTheme.PADDING, y,
                    PANEL_WIDTH - ScreenTheme.PADDING * 2, ROW_HEIGHT - 2,
                    index, hovered, false);
            ScreenDraw.accentBar(graphics, left + ScreenTheme.PADDING, y,
                    ROW_HEIGHT - 2, colourFor(mode));

            String key = KEY_NAME_PREFIX + mode.getSerializedName();
            graphics.drawString(font, Component.translatable(key),
                    left + LABEL_X, y + NAME_Y, ScreenTheme.TEXT, false);
            graphics.drawString(font, Component.translatable(key + KEY_DESC_SUFFIX),
                    left + LABEL_X, y + DESC_Y, ScreenTheme.MUTED, false);
        }
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
