package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.equipment.WornEquipment;
import net.pixeldreamstudios.combat_styles.skill.SkillEquipmentStacks;

import java.util.ArrayList;
import java.util.List;

public final class CooldownHud {

    private static final int PANEL = 0x80101018;
    private static final int EDGE = 0x40FFFFFF;
    private static final int SWEEP = 0xB0000000;
    private static final int TRACK = 0xFF1A202A;
    private static final int PROC_TINT = 0x00FFD46A;
    private static final int ARMED_TINT = 0x00FFE9A8;

    private static final int ARMED_MIN_ALPHA = 0x38;
    private static final int ARMED_SWING_ALPHA = 0x50;
    private static final float ARMED_PULSE_TICKS = 16.0F;
    private static final int FULL_ALPHA = 0xFF;
    private static final int READY_ALPHA = 0xFF;
    private static final int PASSIVE_ALPHA = 0x70;
    private static final float COOLING_FADE = 0.35F;

    private static final int ICON_INSET = 1;
    private static final int ICON_SIZE = HudLayout.SLOT - ICON_INSET * 2;
    private static final int TEXT_DROP = 10;
    private static final int TICKS_PER_SECOND = 20;
    private static final float OVERLAY_LAYER = 200.0F;

    private CooldownHud() {
    }

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        CombatStylesConfig config = CombatStylesConfig.get();
        if (!config.cooldownHudEnabled()) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        List<HudRow> rows = rowsFor(player, config.cooldownHudHideWhenReady(), config.cooldownHudSkillEquipment());
        if (!rows.isEmpty()) {
            draw(graphics, rows, screenWidth, screenHeight, config, false);
        }
    }

    public static void preview(GuiGraphics graphics, int screenWidth, int screenHeight,
                               CombatStylesConfig config) {
        Player player = Minecraft.getInstance().player;
        List<HudRow> rows = player == null ? List.of() : rowsFor(player, false, config.cooldownHudSkillEquipment());
        draw(graphics, rows.isEmpty() ? HudRow.samples() : rows,
                screenWidth, screenHeight, config, true);
    }

    public static HudLayout measure(CombatStylesConfig config) {
        Player player = Minecraft.getInstance().player;
        List<HudRow> rows = player == null ? List.of() : rowsFor(player, false, config.cooldownHudSkillEquipment());
        if (rows.isEmpty()) {
            rows = HudRow.samples();
        }
        HudDensity density = HudDensity.byKey(config.cooldownHudDensity());
        return HudLayout.of(rows, density, config.cooldownHudVertical(),
                config.cooldownHudFlipped(),
                widestTimer(Minecraft.getInstance().font, rows, density));
    }

    private static List<HudRow> rowsFor(Player player, boolean hideWhenReady, boolean showSkillEquipment) {
        List<HudRow> rows = new ArrayList<>();
        for (WornEquipment worn : WornEquipment.of(player)) {
            HudRow row = HudRow.of(worn.stack(), worn.definition(), worn.copy());
            if (SkillEquipmentStacks.isSkillStack(worn.stack()) && !showSkillEquipment) {
                continue;
            }
            if (!hideWhenReady || !row.idle()) {
                rows.add(row);
            }
        }
        return rows;
    }

    private static void draw(GuiGraphics graphics, List<HudRow> rows,
                             int screenWidth, int screenHeight,
                             CombatStylesConfig config, boolean outlined) {
        Font font = Minecraft.getInstance().font;
        HudDensity density = HudDensity.byKey(config.cooldownHudDensity());
        HudLayout layout = HudLayout.of(rows, density, config.cooldownHudVertical(),
                config.cooldownHudFlipped(),
                widestTimer(font, rows, density));

        HudAnchor anchor = HudAnchor.byKey(config.cooldownHudAnchor());
        int x = anchor.originX(screenWidth, layout.width()) + config.cooldownHudOffsetX();
        int y = anchor.originY(screenHeight, layout.height()) + config.cooldownHudOffsetY();

        graphics.fill(x, y, x + layout.width(), y + layout.height(), PANEL);
        if (outlined) {
            ScreenDraw.accentBar(graphics, x, y, layout.height(), EDGE);
            ScreenDraw.accentBar(graphics, x + layout.width() - 2, y, layout.height(), EDGE);
            graphics.fill(x, y, x + layout.width(), y + 1, EDGE);
            graphics.fill(x, y + layout.height() - 1, x + layout.width(),
                    y + layout.height(), EDGE);
        }

        int cellX = x + HudLayout.PAD;
        int cellY = y + HudLayout.PAD;
        for (HudRow row : rows) {
            drawRow(graphics, font, row, cellX, cellY, layout);
            if (layout.vertical()) {
                cellY += layout.advance();
            } else {
                cellX += layout.advance();
            }
        }
    }

    private static int widestTimer(Font font, List<HudRow> rows, HudDensity density) {
        if (density != HudDensity.EXPANDED) {
            return 0;
        }
        int widest = 0;
        for (HudRow row : rows) {
            int remaining = row.longestRemaining();
            if (remaining > 0) {
                widest = Math.max(widest, font.width(secondsOf(remaining)));
            }
        }
        return widest;
    }

    private static String secondsOf(int ticks) {
        return String.valueOf(Math.max(1, ticks / TICKS_PER_SECOND));
    }

    private static void drawRow(GuiGraphics graphics, Font font, HudRow row,
                                int x, int y, HudLayout layout) {
        int itemX = layout.itemX(x);
        drawHighlight(graphics, row, itemX, y);
        ResourceLocation texture = SkillEquipmentStacks.textureOf(row.stack());
        if (texture != null) {
            graphics.blit(texture, itemX + ICON_INSET, y + ICON_INSET, 0.0F, 0.0F,
                    ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        } else {
            graphics.renderItem(row.stack(), itemX + ICON_INSET, y + ICON_INSET);
        }
        drawSweep(graphics, font, row, itemX, y);

        if (layout.density() == HudDensity.MINIMAL) {
            drawTicks(graphics, row, layout.tickX(x), y);
            return;
        }
        drawLanes(graphics, row, x, y, layout);
        if (layout.density() == HudDensity.EXPANDED && row.anyCooling()) {
            String seconds = secondsOf(row.longestRemaining());
            drawTimer(graphics, font, seconds, layout.timerX(x, font.width(seconds)), y);
        }
    }

    private static void drawHighlight(GuiGraphics graphics, HudRow row, int x, int y) {
        if (row.procFlash() > 0.0F) {
            int alpha = (int) (row.procFlash() * FULL_ALPHA) << 24;
            outline(graphics, x, y, alpha | PROC_TINT);
        } else if (row.armed()) {
            outline(graphics, x, y, armedGlow() | ARMED_TINT);
        }
    }

    private static void outline(GuiGraphics graphics, int x, int y, int colour) {
        graphics.fill(x - 1, y - 1, x + HudLayout.SLOT + 1, y + HudLayout.SLOT + 1, colour);
    }

    private static void drawSweep(GuiGraphics graphics, Font font, HudRow row, int x, int y) {
        if (row.itemRemaining() <= 0) {
            return;
        }
        int covered = Math.round(HudLayout.SLOT * (1.0F - row.itemFill()));
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, OVERLAY_LAYER);
        if (covered > 0) {
            graphics.fill(x, y + HudLayout.SLOT - covered,
                    x + HudLayout.SLOT, y + HudLayout.SLOT, SWEEP);
        }
        String seconds = secondsOf(row.itemRemaining());
        graphics.drawString(font, seconds, x + HudLayout.SLOT - font.width(seconds),
                y + TEXT_DROP, ScreenTheme.TEXT, true);
        graphics.pose().popPose();
    }

    private static void drawTicks(GuiGraphics graphics, HudRow row, int tickX, int y) {
        List<HudRow.Lane> lanes = row.lanes();
        if (lanes.isEmpty()) {
            return;
        }
        int span = HudLayout.SLOT / lanes.size();
        for (int index = 0; index < lanes.size(); index++) {
            HudRow.Lane lane = lanes.get(index);
            int top = y + index * span;
            int bottom = index == lanes.size() - 1 ? y + HudLayout.SLOT : top + span - 1;
            graphics.fill(tickX, top, tickX + HudLayout.TICK, bottom, tint(lane));
        }
    }

    private static void drawLanes(GuiGraphics graphics, HudRow row, int x, int y,
                                  HudLayout layout) {
        List<HudRow.Lane> lanes = row.lanes();
        for (int index = 0; index < lanes.size(); index++) {
            HudRow.Lane lane = lanes.get(index);
            int laneX = layout.laneX(x, index);
            graphics.fill(laneX, y, laneX + HudLayout.LANE, y + HudLayout.SLOT, TRACK);
            if (lane.cooling()) {
                int filled = Math.max(1, Math.round(HudLayout.SLOT * lane.fill()));
                graphics.fill(laneX, y + HudLayout.SLOT - filled,
                        laneX + HudLayout.LANE, y + HudLayout.SLOT, tint(lane));
            } else {
                graphics.fill(laneX, y, laneX + HudLayout.LANE, y + HudLayout.SLOT,
                        tint(lane));
            }
        }
    }

    private static void drawTimer(GuiGraphics graphics, Font font, String seconds, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, OVERLAY_LAYER);
        graphics.drawString(font, seconds, x, y + TEXT_DROP,
                ScreenTheme.TEXT, true);
        graphics.pose().popPose();
    }

    private static int tint(HudRow.Lane lane) {
        int rgb = lane.colour() & 0x00FFFFFF;
        return switch (lane.state()) {
            case COOLING -> PlateColors.darken(0xFF000000 | rgb, COOLING_FADE);
            case PASSIVE -> (PASSIVE_ALPHA << 24) | rgb;
            case READY -> (READY_ALPHA << 24) | rgb;
        };
    }

    private static int armedGlow() {
        Minecraft minecraft = Minecraft.getInstance();
        float time = minecraft.player == null
                ? 0.0F
                : minecraft.player.tickCount
                        + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        float wave = (Mth.sin(time / ARMED_PULSE_TICKS * Mth.TWO_PI) + 1.0F) * 0.5F;
        return (ARMED_MIN_ALPHA + Math.round(wave * ARMED_SWING_ALPHA)) << 24;
    }
}
