package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.item.TooltipGem;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.AffixText;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.affix.ResolvedAffixes;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.style.StyleCost;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class AffixPickerScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 216;
    private static final int ROW_HEIGHT = 13;
    private static final int PADDING = 8;
    private static final int HEADER = 60;
    private static final int SEARCH_HEIGHT = 14;
    private static final int SEARCH_MAX_LENGTH = 48;
    private static final int FOOTER = 26;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SWATCH_WIDTH = 18;
    private static final int GAP = 6;
    private static final int BUTTON_WIDTH = 74;
    private static final int BUTTON_HEIGHT = 18;

    private static final int COLOR_SHADE = ScreenTheme.SHADE;
    private static final int COLOR_PANEL = ScreenTheme.PANEL;
    private static final int COLOR_SUNK = ScreenTheme.SUNK;
    private static final int COLOR_BORDER = ScreenTheme.BORDER;
    private static final int COLOR_ROW_ALT = ScreenTheme.ROW_ALT;
    private static final int COLOR_HOVER = ScreenTheme.HOVER;
    private static final int COLOR_SELECTED = ScreenTheme.SELECTED;
    private static final int COLOR_ACCENT = ScreenTheme.ACCENT;
    private static final int COLOR_HEADING = ScreenTheme.HEADING;
    private static final int COLOR_TEXT = ScreenTheme.TEXT;
    private static final int COLOR_MUTED = ScreenTheme.MUTED;
    private static final int COLOR_LOCKED = ScreenTheme.LOCKED;
    private static final int COLOR_BAD = ScreenTheme.BAD;
    private static final int COLOR_SCROLL = ScreenTheme.SCROLL;

    private static final String KEY_TITLE = "screen.combat_styles.pick_affixes";
    private static final String KEY_SELECTED = "screen.combat_styles.affixes_selected";
    private static final String KEY_NONE = "screen.combat_styles.affix_none";
    private static final String KEY_CANCEL = "gui.cancel";
    private static final String KEY_CONFIRM = "screen.combat_styles.confirm";
    private static final String KEY_MANAGE_AFFIXES = "screen.combat_styles.manage_affixes";
    private static final String KEY_COMBINATIONS = "screen.combat_styles.affix_combinations";
    private static final String KEY_COST = "screen.combat_styles.affix_cost";
    private static final String KEY_NO_AFFIX = "screen.combat_styles.affix_none_option";
    private static final String KEY_TIP_COST = "screen.combat_styles.affix_tip_cost";
    private static final String KEY_TIP_OWNED = "screen.combat_styles.affix_tip_owned";
    private static final String KEY_TIP_GRANTED = "screen.combat_styles.affix_tip_granted";
    private static final String KEY_SEARCH = "screen.combat_styles.search_affixes";

    private final StylePage page;
    private final StyleEntry entry;
    private final CustomizerState state;
    private final Consumer<List<String>> onChosen;
    private final int slotIndex;
    private final Map<String, AffixTuning> tuning = new LinkedHashMap<>();
    private final List<List<String>> available = new ArrayList<>();
    private final List<List<String>> pinned = new ArrayList<>();
    private List<String> chosen = List.of();

    private List<Component> hoverTooltip;
    private EditBox searchBox;
    private String search = "";

    public AffixPickerScreen(Screen parent, StylePage page, StyleEntry entry, int slotIndex,
                             CustomizerState state, Consumer<List<String>> onChosen) {
        super(Component.translatable(KEY_TITLE), parent,
                new Scrollbar(COLOR_SUNK, COLOR_SCROLL));
        this.page = page;
        this.entry = entry;
        this.state = state;
        this.onChosen = onChosen;
        this.slotIndex = slotIndex;
        this.chosen = StyleEntry.sorted(entry.affixIds());
        this.tuning.putAll(entry.affixTuning());
        if (!this.chosen.isEmpty()) {
            this.pinned.add(this.chosen);
        }
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

        searchBox = new FieldBox(font, left + PADDING, top + HEADER - SEARCH_HEIGHT - 4,
                PANEL_WIDTH - PADDING * 2, SEARCH_HEIGHT, Component.translatable(KEY_SEARCH));
        searchBox.setHint(Component.translatable(KEY_SEARCH).withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setMaxLength(SEARCH_MAX_LENGTH);
        searchBox.setValue(search);
        searchBox.setResponder(value -> {
            search = value;
            scroll = 0;
            rebuild();
        });
        addRenderableWidget(searchBox);

        rebuild();

        int buttonY = top + PANEL_HEIGHT - FOOTER + 4;
        if (state != null && state.creative()) {
            addRenderableWidget(PanelButton.of(Component.translatable(KEY_MANAGE_AFFIXES),
                            button -> minecraft.setScreen(new AffixManagerScreen(this,
                                    state.creative(), this::rebuild)))
                    .bounds(left + PADDING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CANCEL),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH * 2 - 6, buttonY,
                        BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CONFIRM),
                        button -> confirm())
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void rebuild() {
        available.clear();
        String needle = search.trim().toLowerCase(Locale.ROOT);
        List<List<String>> rest = new ArrayList<>();
        for (List<String> combination : state.learnedCombinationsFor(page.styleId(), entry)) {
            if (!combination.isEmpty() && !matches(combination, needle)) {
                continue;
            }
            if (!combination.isEmpty() && pinned.contains(combination)) {
                available.add(combination);
            } else {
                rest.add(combination);
            }
        }
        available.addAll(rest);
    }

    private boolean matches(List<String> combination, String needle) {
        if (needle.isEmpty()) {
            return true;
        }
        for (StyleAffix affix : ResolvedAffixes.of(combination).affixes()) {
            if (affix.serializedId().toLowerCase(Locale.ROOT).contains(needle)
                    || AffixText.name(affix).getString().toLowerCase(Locale.ROOT).contains(needle)
                    || affix.rarity().name().toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected int listTop() {
        return top + HEADER;
    }

    @Override
    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER - 4;
    }

    @Override
    protected int rowHeight() {
        return ROW_HEIGHT;
    }

    @Override
    protected int rowCount() {
        return available.size();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        hoverTooltip = null;

        graphics.fill(0, 0, width, height, COLOR_SHADE);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_PANEL);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, COLOR_BORDER);

        String count = multiSelect()
                ? Component.translatable(KEY_SELECTED, chosen.size(),
                        Math.max(1, CombatStylesConfig.get().maxAffixesPerSlot())).getString()
                : Component.translatable(KEY_COMBINATIONS, available.size() - 1).getString();
        int countX = left + PANEL_WIDTH - PADDING - font.width(count);
        graphics.drawString(font, count, countX, top + 10, COLOR_MUTED, false);
        graphics.drawString(font, trim(title.getString(), countX - left - PADDING - GAP),
                left + PADDING, top + 10, COLOR_HEADING, false);

        ResolvedAffixes preview = ResolvedAffixes.of(chosen);
        String cost = Component.translatable(KEY_COST,
                StyleText.formatDecimal(StyleCost.of(page, chosen)),
                StyleText.formatDecimal(preview.costMultiplier())).getString();
        int costX = left + PANEL_WIDTH - PADDING - font.width(cost);
        graphics.drawString(font, cost, costX, top + 22, COLOR_ACCENT, false);

        int swatchWidth = preview.blendedTrail().isPresent() ? SWATCH_WIDTH + GAP : 0;
        int nameWidth = costX - left - PADDING - GAP - swatchWidth;
        String name = trim(StyleText.affixedName(entry, page).getString(), nameWidth);
        graphics.drawString(font, name, left + PADDING, top + 22, COLOR_MUTED, false);

        preview.blendedTrail().ifPresent(trail -> {
            int swatchX = left + PADDING + font.width(name) + GAP;
            graphics.fill(swatchX, top + 22, swatchX + SWATCH_WIDTH, top + 30, trail.argb());
            graphics.renderOutline(swatchX, top + 22, SWATCH_WIDTH, 8, COLOR_BORDER);
        });

        renderList(graphics, mouseX, mouseY);
        ScreenDraw.sunken(graphics, searchBox.getX() - 2, searchBox.getY() - 2,
                searchBox.getWidth() + 4, searchBox.getHeight() + 4);
        searchBox.render(graphics, mouseX, mouseY, partialTick);

        if (hoverTooltip != null && !hoverTooltip.isEmpty()) {
            ScreenDraw.tooltip(graphics, font, hoverTooltip, mouseX, mouseY);
        }
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        int y = listTop();
        int bottom = listBottom();

        graphics.fill(x - 2, y - 2, x + listWidth, bottom, COLOR_SUNK);

        boolean scrollable = available.size() > visibleRows();
        int rowWidth = scrollable ? listWidth - SCROLLBAR_WIDTH - 3 : listWidth;
        scroll = Math.clamp(scroll, 0, Math.max(0, available.size() - visibleRows()));

        if (available.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_NONE), x + 4, y + 4, COLOR_LOCKED, false);
            return;
        }

        int last = Math.min(available.size(), scroll + visibleRows());
        for (int i = scroll; i < last; i++) {
            List<String> combination = available.get(i);
            ResolvedAffixes resolved = ResolvedAffixes.of(combination);
            boolean picked = isPicked(combination);
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y, rowWidth, ROW_HEIGHT);
            boolean granted = state.isGranted(page.styleId(), combination);
            int owned = state.copiesOwned(page.styleId(), combination);

            int background = picked ? COLOR_SELECTED : (hovered ? COLOR_HOVER
                    : (i % 2 == 0 ? COLOR_ROW_ALT : 0));
            if (background != 0) {
                graphics.fill(x, y, x + rowWidth, y + ROW_HEIGHT, background);
            }

            String marker = multiSelect()
                    ? (picked ? "[x]" : "[ ]")
                    : (picked ? "◉" : "○");
            graphics.drawString(font, marker, x + 4, y + 3,
                    picked ? COLOR_ACCENT : COLOR_MUTED, false);

            String label = combination.isEmpty()
                    ? Component.translatable(KEY_NO_AFFIX).getString()
                    : combinationLabel(resolved);
            int colour = combination.isEmpty()
                    ? COLOR_MUTED
                    : resolved.blendedTrail().map(StyleAffix.TrailColor::argb).orElse(COLOR_TEXT);
            int labelX = x + (multiSelect() ? 22 : 16);
            graphics.drawString(font, trim(label, rowWidth - 76), labelX, y + 3, colour, false);

            if (!combination.isEmpty()) {
                String modifier = "×" + StyleText.formatDecimal(resolved.costMultiplier());
                int right = x + rowWidth - 8 - font.width(modifier);
                graphics.drawString(font, modifier, right, y + 3, COLOR_MUTED, false);

                if (!state.creative() && !granted) {
                    String copies = String.valueOf(owned);
                    graphics.drawString(font, copies, right - font.width(copies) - 8, y + 3,
                            owned > 0 ? COLOR_MUTED : COLOR_BAD, false);
                }
            }

            if (hovered && !combination.isEmpty()) {
                hovered(resolved, granted, owned);
            }

            y += ROW_HEIGHT;
        }

        if (scrollable) {
            renderScrollbar(graphics, x + listWidth - SCROLLBAR_WIDTH - 1, listTop(), bottom - 2);
        }

    }

    private void renderScrollbar(GuiGraphics graphics, int x, int top, int bottom) {
        scrollbar.render(graphics, x, top, bottom, scroll, maxScroll(),
                available.size(), visibleRows());
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbar.isDragging()) {
            scroll = scrollbar.drag(mouseY, scroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private String trim(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        return font.width(text) <= maxWidth ? text : font.plainSubstrByWidth(text, maxWidth - 6) + "…";
    }

    private void confirm() {
        tuning.keySet().retainAll(chosen);
        state.setAffixTuning(slotIndex, tuning);
        onChosen.accept(List.copyOf(chosen));
    }

    private void openTuning(String affixId) {
        StyleAffix target = AffixRegistry.get(affixId);
        if (target == null) {
            return;
        }
        minecraft.setScreen(new AffixTuningScreen(this, target, tuning.get(affixId), result -> {
            if (result == null) {
                tuning.remove(affixId);
            } else {
                tuning.put(affixId, result);
            }
            state.setAffixTuning(slotIndex, tuning);
        }));
    }

    private boolean multiSelect() {
        return state.creative();
    }

    private boolean isPicked(List<String> combination) {
        if (!multiSelect()) {
            return chosen.equals(combination);
        }
        if (combination.isEmpty()) {
            return chosen.isEmpty();
        }
        return chosen.containsAll(combination);
    }

    private void toggle(List<String> combination) {
        if (!combination.isEmpty() && !pinned.contains(combination)) {
            pinned.add(combination);
        }
        if (!multiSelect()) {
            chosen = combination;
            rebuild();
            return;
        }
        if (combination.isEmpty()) {
            chosen = List.of();
            rebuild();
            return;
        }
        List<String> next = new ArrayList<>(chosen);
        if (next.containsAll(combination)) {
            next.removeAll(combination);
        } else {
            int cap = Math.max(1, CombatStylesConfig.get().maxAffixesPerSlot());
            for (String id : combination) {
                if (!next.contains(id) && next.size() < cap) {
                    next.add(id);
                }
            }
        }
        chosen = StyleEntry.sorted(next);
        rebuild();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(searchBox);
            return true;
        }
        if (scrollbar.isOverTrack(mouseX, mouseY)) {
            scroll = scrollbar.beginDrag(mouseX, mouseY, scroll);
            return true;
        }

        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        int y = listTop();

        int last = Math.min(available.size(), scroll + visibleRows());
        for (int i = scroll; i < last; i++) {
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, x, y, listWidth, ROW_HEIGHT)) {
                List<String> combination = available.get(i);
                if (button == 1 && multiSelect() && combination.size() == 1) {
                    openTuning(combination.get(0));
                    return true;
                }
                toggle(combination);
                return true;
            }
            y += ROW_HEIGHT;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void hovered(ResolvedAffixes resolved, boolean granted, int owned) {
        List<Component> lines = new ArrayList<>();
        for (StyleAffix affix : resolved.affixes()) {
            if (!lines.isEmpty()) {
                lines.add(Component.empty());
            }
            lines.add(AffixText.name(affix).copy().withStyle(affix.rarity().color()));
            appendComparedEffects(affix, lines);
        }

        lines.add(Component.empty());
        lines.add(Component.translatable(KEY_TIP_COST,
                        StyleText.formatDecimal(StyleCost.of(page, chosenIdsOf(resolved))),
                        StyleText.formatDecimal(resolved.costMultiplier()))
                .withStyle(ChatFormatting.GOLD));

        if (granted) {
            lines.add(Component.translatable(KEY_TIP_GRANTED).withStyle(ChatFormatting.AQUA));
        } else if (!state.creative()) {
            lines.add(Component.translatable(KEY_TIP_OWNED, owned)
                    .withStyle(owned > 0 ? ChatFormatting.GRAY : ChatFormatting.RED));
        }
        hoverTooltip = lines;
    }

    private void appendComparedEffects(StyleAffix affix, List<Component> lines) {
        AffixTuning applied = tuning.get(affix.serializedId());
        List<Component> tuned = new ArrayList<>();
        AffixText.appendEffects(affix, tuned);
        if (applied == null) {
            lines.addAll(TooltipGem.indentDetails(tuned));
            return;
        }

        StyleAffix base = AffixRegistry.get(affix.serializedId());
        List<Component> original = new ArrayList<>();
        if (base != null) {
            AffixText.appendEffects(base, original);
        }

        for (int i = 0; i < tuned.size(); i++) {
            Component now = tuned.get(i);
            String before = i < original.size() ? original.get(i).getString() : null;
            if (before == null || before.equals(now.getString())) {
                lines.add(now);
                continue;
            }
            lines.add(Component.literal(before).withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(" \u2192 ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(now));
        }
    }

    private List<String> chosenIdsOf(ResolvedAffixes resolved) {
        List<String> ids = new ArrayList<>();
        for (StyleAffix affix : resolved.affixes()) {
            ids.add(affix.serializedId());
        }
        return ids;
    }

    private String combinationLabel(ResolvedAffixes resolved) {
        StringBuilder label = new StringBuilder();
        for (StyleAffix affix : resolved.affixes()) {
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(AffixText.name(affix).getString());
        }
        return label.toString();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (available.size() > visibleRows()) {
            scroll = Math.clamp(scroll - (int) Math.signum(deltaY), 0,
                    Math.max(0, available.size() - visibleRows()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
