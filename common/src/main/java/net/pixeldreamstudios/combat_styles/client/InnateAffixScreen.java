package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.InnateAffixes;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class InnateAffixScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 224;
    private static final int HEADER = 46;
    private static final int FOOTER = 30;
    private static final int ROW_HEIGHT = 18;
    private static final int BUTTON_WIDTH = 70;
    private static final int GAP = 4;
    private static final int TITLE_Y = 10;
    private static final int HINT_Y = 24;
    private static final int TEXT_INSET = 5;
    private static final int REMOVE_WIDTH = 14;
    private static final int SLOT_WIDTH = 46;
    private static final int ANY_SLOT = -1;

    private static final String KEY_TITLE = "screen.combat_styles.innate_title";
    private static final String KEY_HINT = "screen.combat_styles.innate_hint";
    private static final String KEY_EMPTY = "screen.combat_styles.innate_empty";
    private static final String KEY_ADD = "screen.combat_styles.innate_add";
    private static final String KEY_ANY = "screen.combat_styles.innate_any_slot";
    private static final String KEY_SLOT = "screen.combat_styles.innate_slot";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_REMOVE = "screen.combat_styles.slash_remove";

    private final Consumer<List<InnateAffixes.Slot>> onApply;
    private final List<InnateAffixes.Slot> slots = new ArrayList<>();

    public InnateAffixScreen(Screen parent, List<InnateAffixes.Slot> current,
                             Consumer<List<InnateAffixes.Slot>> onApply) {
        super(Component.translatable(KEY_TITLE), parent);
        this.onApply = onApply;
        this.slots.addAll(current);
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
    protected int rowCount() {
        return slots.size();
    }

    @Override
    protected int listTop() {
        return top + HEADER;
    }

    @Override
    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER;
    }

    @Override
    protected void layout() {
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_ADD),
                        button -> addSlot())
                .bounds(contentLeft(), footerTop(),
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(contentRight() - BUTTON_WIDTH * 2 - GAP, footerTop(),
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY), button -> {
            onApply.accept(List.copyOf(slots));
            minecraft.setScreen(parent);
        }).bounds(contentRight() - BUTTON_WIDTH, footerTop(),
                BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private void addSlot() {
        minecraft.setScreen(new EquipmentAffixPickerScreen(this, List.of(),
                picked -> slots.add(new InnateAffixes.Slot(
                        Optional.of(slots.size()), picked))));
    }

    private int slotX() {
        return contentRight() - REMOVE_WIDTH - GAP - SLOT_WIDTH;
    }

    private Component slotLabel(InnateAffixes.Slot slot) {
        return slot.slot().isEmpty()
                ? Component.translatable(KEY_ANY)
                : Component.translatable(KEY_SLOT, slot.slot().get() + 1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (beginScrollDrag(mouseX, mouseY)) {
            return true;
        }
        int index = rowAt(mouseX, mouseY);
        if (index < 0 || index >= slots.size()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (ScreenDraw.isWithin(mouseX, mouseY, contentRight() - REMOVE_WIDTH, rowY(index),
                REMOVE_WIDTH, ROW_HEIGHT)) {
            slots.remove(index);
            return true;
        }
        if (ScreenDraw.isWithin(mouseX, mouseY, slotX(), rowY(index),
                SLOT_WIDTH, ROW_HEIGHT)) {
            cycleSlot(index, button == 1 ? -1 : 1);
            return true;
        }
        InnateAffixes.Slot slot = slots.get(index);
        minecraft.setScreen(new EquipmentAffixPickerScreen(this, List.of(),
                picked -> slots.set(index, new InnateAffixes.Slot(slot.slot(), picked))));
        return true;
    }

    private void cycleSlot(int index, int direction) {
        InnateAffixes.Slot slot = slots.get(index);
        int next = slot.slot().orElse(ANY_SLOT) + direction;
        slots.set(index, new InnateAffixes.Slot(
                next < 0 ? Optional.empty() : Optional.of(next), slot.affix()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        drawPanel(graphics);
        drawTitle(graphics, Component.translatable(KEY_TITLE), top + TITLE_Y);
        graphics.drawString(font, Component.translatable(KEY_HINT),
                contentLeft(), top + HINT_Y, ScreenTheme.MUTED, false);

        ScreenDraw.sunken(graphics, contentLeft() - 2, listTop() - 2,
                contentWidth() + 4, listBottom() - listTop() + 4);

        if (slots.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_EMPTY),
                    contentLeft() + TEXT_INSET, listTop() + TEXT_INSET,
                    ScreenTheme.DIM, false);
        }

        int hovered = rowAt(mouseX, mouseY);
        int last = Math.min(slots.size(), scroll + visibleRows());
        for (int index = scroll; index < last; index++) {
            InnateAffixes.Slot slot = slots.get(index);
            int y = rowY(index);
            ScreenDraw.rowBackground(graphics, contentLeft() + 1, y,
                    contentWidth() - 2, ROW_HEIGHT - 1, index, index == hovered, false);
            graphics.drawString(font,
                    ScreenDraw.trim(font, StyleText.prettify(slot.affix()), slotX() - contentLeft() - 10),
                    contentLeft() + TEXT_INSET, y + TEXT_INSET, ScreenTheme.TEXT, false);

            Component label = slotLabel(slot);
            ScreenDraw.hoverField(graphics, slotX(), y + 2, SLOT_WIDTH - 6,
                    ROW_HEIGHT - 5, mouseX, mouseY);
            graphics.drawString(font, label, slotX(), y + TEXT_INSET,
                    ScreenTheme.ACCENT, false);
            graphics.drawString(font, Component.translatable(KEY_REMOVE),
                    contentRight() - REMOVE_WIDTH + 3, y + TEXT_INSET,
                    ScreenTheme.BAD, false);
        }

        drawScrollbar(graphics);
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }
}
