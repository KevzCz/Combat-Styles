package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.affix.ResolvedAffixes;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.List;

public class PreviewScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 260;
    private static final int PADDING = 10;
    private static final int HEADER = 40;
    private static final int FOOTER = 30;
    private static final int BUTTON_WIDTH = 70;
    private static final int BUTTON_HEIGHT = 18;

    private static final int COLOR_SHADE = ScreenTheme.SHADE;
    private static final int COLOR_PANEL = ScreenTheme.PANEL;
    private static final int COLOR_STAGE = ScreenTheme.SUNK;
    private static final int COLOR_BORDER = ScreenTheme.BORDER;
    private static final int COLOR_HEADING = ScreenTheme.HEADING;
    private static final int COLOR_TEXT = ScreenTheme.TEXT;
    private static final int COLOR_MUTED = ScreenTheme.MUTED;
    private static final int COLOR_GROUND = 0x24FFFFFF;

    private static final String KEY_TITLE = "screen.combat_styles.preview_title";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_REPLAY = "screen.combat_styles.replay";
    private static final String KEY_RESET_VIEW = "screen.combat_styles.reset_view";
    private static final String KEY_DRAG_HINT = "screen.combat_styles.preview_hint";
    private static final String KEY_TRAIL = "screen.combat_styles.preview_trail";

    private static final float DEFAULT_PITCH = 15.0F;
    private static final float MIN_PITCH = -60.0F;
    private static final float MAX_PITCH = 60.0F;
    private static final float MIN_ZOOM = 0.5F;
    private static final float MAX_ZOOM = 2.5F;
    private static final float ZOOM_STEP = 0.15F;
    private static final float DRAG_SENSITIVITY = 1.4F;

    private final AnimationPreview preview;
    private final StylePage page;
    private final List<String> affixIds;

    private float yaw;
    private float pitch = DEFAULT_PITCH;
    private float zoom = 1.0F;
    private boolean dragging;

    public PreviewScreen(Screen parent, AnimationPreview preview, StylePage page, List<String> affixIds) {
        super(Component.translatable(KEY_TITLE), parent);
        this.preview = preview;
        this.page = page;
        this.affixIds = affixIds == null ? List.of() : List.copyOf(affixIds);
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

        int buttonY = top + PANEL_HEIGHT - FOOTER + 6;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_REPLAY), button -> preview.play())
                .bounds(left + PADDING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_RESET_VIEW), button -> resetView())
                .bounds(left + PADDING + BUTTON_WIDTH + 6, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE), button -> onClose())
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        preview.play();
    }

    private void resetView() {
        yaw = 0.0F;
        pitch = DEFAULT_PITCH;
        zoom = 1.0F;
    }

    private int stageTop() {
        return top + HEADER;
    }

    private int stageBottom() {
        return top + PANEL_HEIGHT - FOOTER - 4;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, width, height, COLOR_SHADE);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_PANEL);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER);

        graphics.drawString(font, StyleText.name(page), left + PADDING, top + 12, COLOR_HEADING, false);

        String animation = page.animation();
        graphics.drawString(font, animation, left + PADDING, top + 24, COLOR_MUTED, false);

        int stageTop = stageTop();
        int stageBottom = stageBottom();
        graphics.fill(left + PADDING, stageTop, left + PANEL_WIDTH - PADDING, stageBottom, COLOR_STAGE);
        graphics.renderOutline(left + PADDING, stageTop,
                PANEL_WIDTH - PADDING * 2, stageBottom - stageTop, COLOR_BORDER);

        int groundY = stageBottom - 18;
        graphics.fill(left + PADDING + 12, groundY, left + PANEL_WIDTH - PADDING - 12, groundY + 1, COLOR_GROUND);

        graphics.enableScissor(left + PADDING + 1, stageTop + 1,
                left + PANEL_WIDTH - PADDING - 1, stageBottom - 1);
        PreviewRenderer.render(graphics,
                left + PANEL_WIDTH / 2.0F,
                groundY,
                (stageBottom - stageTop) * 0.42F * zoom,
                preview, yaw, pitch);
        graphics.disableScissor();

        renderTrailSwatch(graphics, stageTop);

        String hint = Component.translatable(KEY_DRAG_HINT).getString();
        graphics.drawString(font, hint, left + PADDING,
                stageBottom + 6, COLOR_MUTED, false);
    }

    private void renderTrailSwatch(GuiGraphics graphics, int stageTop) {
        ResolvedAffixes resolved = ResolvedAffixes.of(affixIds);
        StyleAffix.TrailColor trail = resolved.blendedTrail().orElse(null);
        if (trail == null) {
            return;
        }
        String label = Component.translatable(KEY_TRAIL).getString();
        int labelWidth = font.width(label);
        int x = left + PANEL_WIDTH - PADDING - labelWidth - 24;
        graphics.drawString(font, label, x, stageTop - 14, COLOR_TEXT, false);
        graphics.fill(x + labelWidth + 4, stageTop - 14, x + labelWidth + 20, stageTop - 6, trail.argb());
        graphics.renderOutline(x + labelWidth + 4, stageTop - 14, 16, 8, COLOR_BORDER);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY >= stageTop() && mouseY < stageBottom()
                && mouseX >= left + PADDING && mouseX < left + PANEL_WIDTH - PADDING) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            yaw += (float) dragX * DRAG_SENSITIVITY;
            pitch = Math.clamp(pitch + (float) dragY * DRAG_SENSITIVITY, MIN_PITCH, MAX_PITCH);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        zoom = Math.clamp(zoom + (float) deltaY * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
        return true;
    }

    @Override
    public void tick() {
        preview.tick();
        if (!preview.isPlaying()) {
            preview.play();
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KEY_ESCAPE) {
            minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
