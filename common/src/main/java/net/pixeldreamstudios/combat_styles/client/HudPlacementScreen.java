package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;

public class HudPlacementScreen extends Screen {
    private static final String KEY_TITLE = "screen.combat_styles.hud_placement";
    private static final String KEY_HINT = "screen.combat_styles.hud_placement_hint";
    private static final String KEY_ANCHOR = "screen.combat_styles.hud_anchor";
    private static final String KEY_RESET = "screen.combat_styles.hud_reset";
    private static final String KEY_DONE = "gui.done";

    private static final int DIM = 0xB0000000;
    private static final int FALLBACK_W = 96;
    private static final int FALLBACK_H = 40;
    private static final int BUTTON_W = 80;
    private static final int BUTTON_H = 20;
    private static final int BUTTON_GAP = 4;
    private static final int MARGIN = 8;
    private static final int SNAP = 6;
    private static final int NUDGE = 1;
    private static final int TEXT_DROP = 22;

    private final Screen parent;

    private boolean dragging;

    private int panelWidth() {
        HudLayout layout = CooldownHud.measure(CombatStylesConfig.get());
        return layout == null ? FALLBACK_W : layout.width();
    }

    private int panelHeight() {
        HudLayout layout = CooldownHud.measure(CombatStylesConfig.get());
        return layout == null ? FALLBACK_H : layout.height();
    }
    private int grabX;
    private int grabY;

    public HudPlacementScreen(Screen parent) {
        super(Component.translatable(KEY_TITLE));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = height - BUTTON_H - MARGIN;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_RESET), button -> {
            CombatStylesConfig.get().placeCooldownHud(HudAnchor.TOP_LEFT.key(), MARGIN, MARGIN);
        }).bounds(width / 2 - BUTTON_W - BUTTON_GAP, y, BUTTON_W, BUTTON_H).build());

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_DONE), button -> onClose())
                .bounds(width / 2 + BUTTON_GAP, y, BUTTON_W, BUTTON_H).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, width, height, DIM);
        CooldownHud.preview(graphics, width, height, CombatStylesConfig.get());

        CombatStylesConfig config = CombatStylesConfig.get();
        graphics.drawCenteredString(font, title, width / 2, MARGIN, 0xFFECE8E1);
        graphics.drawCenteredString(font, Component.translatable(KEY_HINT),
                width / 2, MARGIN + TEXT_DROP / 2, 0xFF8A8177);
        graphics.drawCenteredString(font,
                Component.translatable(KEY_ANCHOR, config.cooldownHudAnchor(),
                        config.cooldownHudOffsetX(), config.cooldownHudOffsetY()),
                width / 2, MARGIN + TEXT_DROP, 0xFF8A8177);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        dragging = true;
        CombatStylesConfig config = CombatStylesConfig.get();
        HudAnchor anchor = HudAnchor.byKey(config.cooldownHudAnchor());
        grabX = (int) mouseX - (anchor.originX(width, panelWidth()) + config.cooldownHudOffsetX());
        grabY = (int) mouseY - (anchor.originY(height, panelHeight()) + config.cooldownHudOffsetY());
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (!dragging) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        place((int) mouseX - grabX, (int) mouseY - grabY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        CombatStylesConfig config = CombatStylesConfig.get();
        HudAnchor anchor = HudAnchor.byKey(config.cooldownHudAnchor());
        int x = anchor.originX(width, panelWidth()) + config.cooldownHudOffsetX();
        int y = anchor.originY(height, panelHeight()) + config.cooldownHudOffsetY();
        switch (key) {
            case 263 -> place(x - NUDGE, y);
            case 262 -> place(x + NUDGE, y);
            case 265 -> place(x, y - NUDGE);
            case 264 -> place(x, y + NUDGE);
            default -> {
                return super.keyPressed(key, scanCode, modifiers);
            }
        }
        return true;
    }

    private void place(int x, int y) {
        int clampedX = Math.max(0, Math.min(width - panelWidth(), x));
        int clampedY = Math.max(0, Math.min(height - panelHeight(), y));

        if (clampedX < SNAP) {
            clampedX = 0;
        } else if (clampedX > width - panelWidth() - SNAP) {
            clampedX = width - panelWidth();
        }
        if (clampedY < SNAP) {
            clampedY = 0;
        } else if (clampedY > height - panelHeight() - SNAP) {
            clampedY = height - panelHeight();
        }

        HudAnchor anchor = HudAnchor.nearest(clampedX + panelWidth() / 2, clampedY + panelHeight() / 2,
                width, height);
        CombatStylesConfig.get().placeCooldownHud(anchor.key(),
                clampedX - anchor.originX(width, panelWidth()),
                clampedY - anchor.originY(height, panelHeight()));
    }

    @Override
    public void onClose() {
        CombatStylesConfig.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
