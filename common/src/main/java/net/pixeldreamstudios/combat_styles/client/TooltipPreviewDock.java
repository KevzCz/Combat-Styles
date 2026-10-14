package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.pixeldreamstudios.combat_styles.client.tooltip.StyleTooltipRenderer;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentTooltip;
import net.pixeldreamstudios.combat_styles.item.TooltipContextAccess;


import java.util.ArrayList;
import java.util.List;

public class TooltipPreviewDock {

    private static final int MIN_WIDTH = 180;
    private static final int MAX_WIDTH = 340;
    private static final int BOX_PADDING = 4;
    private static final int TITLE_HEIGHT = 14;
    private static final int PADDING = 6;
    private static final int GAP = 4;
    private static final int EDGE_MARGIN = 4;
    private static final int TOGGLE_WIDTH = 11;
    private static final int HEADER_WIDTH = 132;
    private static final float RAISE = 400.0F;
    private static final int TEXT_INSET = 3;
    private static final int MAX_LINES = 48;

    private static final String KEY_TITLE = "screen.combat_styles.equipment_preview_title";
    private static final String KEY_NO_MATCH = "screen.combat_styles.equipment_preview_no_match";

    private static int savedX = Integer.MIN_VALUE;
    private static int savedY = Integer.MIN_VALUE;
    private static boolean savedOpen = true;

    private ItemStack sample = ItemStack.EMPTY;
    private List<Component> lines = List.of();
    private EquipmentDefinition edited;
    private int signature = Integer.MIN_VALUE;
    private int x;
    private int y;
    private int bodyWidth = MIN_WIDTH;
    private boolean open = savedOpen;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public void place(int panelLeft, int panelTop, int panelWidth,
                      int screenWidth, int screenHeight) {
        if (savedX != Integer.MIN_VALUE) {
            x = savedX;
            y = savedY;
            clampInto(screenWidth, screenHeight);
            return;
        }
        x = panelLeft + panelWidth + GAP;
        y = panelTop;
        clampInto(screenWidth, screenHeight);
    }

    public void sample(ItemStack stack) {
        sample = stack == null ? ItemStack.EMPTY : stack;
    }

    public ItemStack current() {
        return sample;
    }

    public void preview(Minecraft minecraft, EquipmentDefinition definition) {
        int stamp = definition == null ? 0 : definition.hashCode();
        ItemStack stack = current();
        stamp = stamp * 31 + (stack.isEmpty() ? 0 : stack.getItem().hashCode());
        stamp = stamp * 31 + (TooltipContextAccess.expanded() ? 1 : 0);
        stamp = stamp * 31 + (TooltipContextAccess.detailed() ? 1 : 0);
        if (stamp == signature) {
            return;
        }
        signature = stamp;
        edited = definition;
        refresh(minecraft);
    }

    public void refresh(Minecraft minecraft) {
        ItemStack stack = current();
        if (stack.isEmpty() || minecraft == null) {
            lines = List.of();
            return;
        }
        TooltipFlag flag = minecraft.options.advancedItemTooltips
                ? TooltipFlag.ADVANCED
                : TooltipFlag.NORMAL;
        Item.TooltipContext context = Item.TooltipContext.of(minecraft.level);
        List<Component> built = new ArrayList<>(edited == null
                ? stack.getTooltipLines(context, minecraft.player, flag)
                : EquipmentTooltip.previewing(stack, edited,
                        () -> stack.getTooltipLines(context, minecraft.player, flag)));
        lines = built.size() > MAX_LINES
                ? List.copyOf(built.subList(0, MAX_LINES))
                : List.copyOf(built);
        Font font = minecraft.font;
        int widest = 0;
        for (Component line : lines) {
            widest = Math.max(widest, font.width(line));
        }
        bodyWidth = widest + BOX_PADDING * 2;
    }

    public boolean click(double mouseX, double mouseY, Minecraft minecraft) {
        int hx = headerLeft();
        if (!within(mouseX, mouseY, hx, y, HEADER_WIDTH, TITLE_HEIGHT)) {
            return false;
        }
        if (mouseX >= hx + HEADER_WIDTH - TOGGLE_WIDTH - TEXT_INSET) {
            open = !open;
            savedOpen = open;
            return true;
        }
        dragging = true;
        dragOffsetX = (int) (mouseX - x);
        dragOffsetY = (int) (mouseY - y);
        return true;
    }

    public void drag(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (!dragging) {
            return;
        }
        x = (int) (mouseX - dragOffsetX);
        y = (int) (mouseY - dragOffsetY);
        clampInto(screenWidth, screenHeight);
    }

    public void release() {
        if (dragging) {
            dragging = false;
            savedX = x;
            savedY = y;
        }
    }

    private void clampInto(int screenWidth, int screenHeight) {
        x = Math.max(EDGE_MARGIN - width() + HEADER_WIDTH,
                Math.min(x, screenWidth - HEADER_WIDTH));
        y = Math.max(EDGE_MARGIN, Math.min(y, screenHeight - TITLE_HEIGHT - EDGE_MARGIN));
    }

    public void render(GuiGraphics graphics, Font font) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, RAISE);
        renderHeader(graphics, font);
        if (open) {
            renderBody(graphics, font);
        }
        graphics.pose().popPose();
    }

    private int headerLeft() {
        return x;
    }

    private void renderHeader(GuiGraphics graphics, Font font) {
        int hx = headerLeft();
        ScreenDraw.panel(graphics, hx, y, HEADER_WIDTH, TITLE_HEIGHT);
        graphics.drawString(font, Component.translatable(KEY_TITLE),
                hx + PADDING, y + TEXT_INSET, ScreenTheme.HEADING, false);
        graphics.drawString(font, open ? "-" : "+",
                hx + HEADER_WIDTH - TOGGLE_WIDTH, y + TEXT_INSET,
                ScreenTheme.MUTED, false);
    }

    private void renderBody(GuiGraphics graphics, Font font) {
        int top = y + TITLE_HEIGHT + GAP;
        ItemStack stack = current();
        if (stack.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_NO_MATCH),
                    x + PADDING, top, ScreenTheme.MUTED, false);
            return;
        }
        StyleTooltipRenderer.renderLinesAt(graphics, font, lines, stack.getTooltipImage(),
                x + BOX_PADDING, top);
    }

    private static boolean within(double mouseX, double mouseY,
                                  int boxX, int boxY, int boxWidth, int boxHeight) {
        return mouseX >= boxX && mouseX <= boxX + boxWidth
                && mouseY >= boxY && mouseY <= boxY + boxHeight;
    }

    public void toggle() {
        open = !open;
        savedOpen = open;
    }

    public boolean open() {
        return open;
    }

    public int width() {
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, bodyWidth));
    }
}
