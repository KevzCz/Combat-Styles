package net.pixeldreamstudios.combat_styles.client;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.config.EquipmentDropConfig;
import net.pixeldreamstudios.combat_styles.config.ConfigOptions.Option;
import net.pixeldreamstudios.combat_styles.config.ConfigOptions;
import net.pixeldreamstudios.combat_styles.config.StatusEffectConfig;

import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigScreen extends PanelScreen {

    private static final String KEY_TITLE = "screen.combat_styles.config";
    private static final String KEY_SAVE = "screen.combat_styles.config.save";
    private static final String KEY_CANCEL = "screen.combat_styles.config.cancel";
    private static final String KEY_SEARCH = "screen.combat_styles.config.search";
    private static final String KEY_HUD_MOVE = "screen.combat_styles.hud_move";
    private static final String KEY_RESET = "screen.combat_styles.config.reset";
    private static final String KEY_RESET_HINT = "screen.combat_styles.config.reset.hint";
    private static final String KEY_TOGGLE_ON = "screen.combat_styles.config.on";
    private static final String KEY_TOGGLE_OFF = "screen.combat_styles.config.off";
    private static final String KEY_BACK = "screen.combat_styles.config_back";
    private static final String KEY_EMPTY = "screen.combat_styles.config.empty";

    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 232;
    private static final int SIDEBAR_WIDTH = 118;
    private static final int ROW_HEIGHT = 20;
    private static final int HEADER = 42;
    private static final int FOOTER = 28;
    private static final int FIELD_WIDTH = 92;
    private static final int FIELD_HEIGHT = 15;
    private static final int BUTTON_WIDTH = 74;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SEARCH_HEIGHT = 15;
    private static final int PAD = 8;
    private static final int GAP = 6;
    private static final int LABEL_INSET = 6;
    private static final int VALUE_GAP = 10;
    private static final int TOOLTIP_WIDTH = 180;
    private static final int SUGGESTION_LIMIT = 40;
    private static final int SUGGESTION_ROWS = 8;
    private static final int SUGGESTION_ROW = 10;
    private static final int SUGGESTION_WIDTH = 190;
    private static final int DISCARD_PASSES = 2;

    private final Map<String, List<Option>> grouped = new LinkedHashMap<>();
    private final Map<Option, EditBox> boxes = new LinkedHashMap<>();
    private final List<Option> visible = new ArrayList<>();
    private final List<String> groups = new ArrayList<>();

    private final List<String> subjects = new ArrayList<>();

    private final List<String> path = new ArrayList<>();

    private final List<String> matches = new ArrayList<>();

    private EditBox suggestBox;
    private int suggestLeft;
    private int suggestTop;
    private int suggestRows;

    private EditBox search;
    private Option hovered;
    private String selected;
    private int sidebarScroll;

    private final Set<EditConfigPayload> baseline = ConfigEdits.settings();
    private final Map<String, String> snapshot = new LinkedHashMap<>();
    private final JsonObject dropSnapshot = EquipmentDropConfig.toJson();

    public ConfigScreen(Screen parent) {
        super(Component.translatable(KEY_TITLE), parent);
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

        String previous = selected;
        String query = search == null ? "" : search.getValue();

        grouped.clear();
        boxes.clear();
        groups.clear();

        ConfigOptions.suggestions((kind, typed) ->
                DropSuggestions.matching(kind, typed, List.of(), SUGGESTION_LIMIT));

        List<Option> all = allOptions();
        all.add(ConfigOptions.action("hudMove",
                Component.translatable(KEY_HUD_MOVE).getString(),
                () -> minecraft.setScreen(new HudPlacementScreen(this)))
                .describedAs("Drag the cooldown HUD to a new spot.")
                .in(ConfigOptions.VISUALS));
        if (snapshot.isEmpty()) {
            for (Option option : all) {
                if (!option.isAction()) {
                    snapshot.put(option.key(), option.read());
                }
            }
        }

        for (Option option : all) {
            grouped.computeIfAbsent(option.group(), key -> new ArrayList<>()).add(option);
            if (option.isAction() || option.isToggle()) {
                continue;
            }
            EditBox box = new FieldBox(font, 0, 0, FIELD_WIDTH, FIELD_HEIGHT,
                    Component.literal(option.key()));
            box.setValue(option.read());
            box.setVisible(false);
            boxes.put(option, box);
            addRenderableWidget(box);
        }
        groups.addAll(grouped.keySet());
        selected = previous != null && grouped.containsKey(previous)
                ? previous
                : (groups.isEmpty() ? null : groups.getFirst());

        search = new FieldBox(font, left + SIDEBAR_WIDTH + PAD + GAP, top + PAD + 14,
                PANEL_WIDTH - SIDEBAR_WIDTH - PAD * 2 - GAP, SEARCH_HEIGHT,
                Component.translatable(KEY_SEARCH));
        search.setHint(Component.translatable(KEY_SEARCH));
        search.setResponder(text -> {
            scroll = 0;
            refresh();
        });
        search.setValue(query);
        addRenderableWidget(search);

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_RESET), button -> reset())
                .tooltip(Component.translatable(KEY_RESET_HINT))
                .bounds(left + PAD, top + PANEL_HEIGHT - FOOTER + 4,
                        BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_SAVE), button -> apply())
                .bounds(left + PANEL_WIDTH - BUTTON_WIDTH * 2 - PAD - GAP,
                        top + PANEL_HEIGHT - FOOTER + 4, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CANCEL),
                        button -> requestClose())
                .bounds(left + PANEL_WIDTH - BUTTON_WIDTH - PAD,
                        top + PANEL_HEIGHT - FOOTER + 4, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        refresh();
    }

    private static List<Option> allOptions() {
        List<Option> all = new ArrayList<>();
        all.addAll(ConfigOptions.of(CombatStylesConfig.get()));
        all.addAll(ConfigOptions.statusEffects());
        all.addAll(ConfigOptions.equipmentDrops());
        all.addAll(ConfigOptions.summons());
        return all;
    }

    private static List<Option> defaultOptions() {
        List<Option> defaults = new ArrayList<>();
        defaults.addAll(ConfigOptions.of(new CombatStylesConfig()));
        defaults.addAll(ConfigOptions.statusEffectDefaults());
        defaults.addAll(ConfigOptions.summonDefaults());
        return defaults;
    }

    private boolean dirty() {
        if (!dropSnapshot.equals(EquipmentDropConfig.toJson())) {
            return true;
        }
        for (Map.Entry<Option, EditBox> entry : boxes.entrySet()) {
            if (!entry.getValue().getValue().equals(entry.getKey().read())) {
                return true;
            }
        }
        for (Option option : allOptions()) {
            String was = snapshot.get(option.key());
            if (!option.isAction() && (was == null || !was.equals(option.read()))) {
                return true;
            }
        }
        return false;
    }

    private void requestClose() {
        if (!dirty()) {
            minecraft.setScreen(parent);
            return;
        }
        commitValues();
        minecraft.setScreen(new UnsavedChangesScreen(this, this::apply, this::discard));
    }

    private void discard() {
        EquipmentDropConfig.accept(dropSnapshot.deepCopy());
        for (int pass = 0; pass < DISCARD_PASSES; pass++) {
            for (Option option : allOptions()) {
                String was = snapshot.get(option.key());
                if (!option.isAction() && was != null && !was.equals(option.read())) {
                    option.write(was);
                }
            }
        }
        minecraft.setScreen(parent);
    }

    private void refresh() {
        String query = search == null ? "" : search.getValue();
        visible.clear();
        subjects.clear();
        if (query.isBlank()) {
            List<Option> inGroup = grouped.getOrDefault(selected, List.of());
            String prefix = pathPrefix();
            for (Option option : inGroup) {
                if (!option.key().startsWith(prefix)) {
                    continue;
                }
                String rest = option.key().substring(prefix.length());
                int dot = rest.indexOf('.');
                if (dot <= 0) {
                    visible.add(option);
                } else {
                    String owner = rest.substring(0, dot);
                    if (!subjects.contains(owner)) {
                        subjects.add(owner);
                    }
                }
            }
        } else {
            for (List<Option> options : grouped.values()) {
                for (Option option : options) {
                    if (option.matches(query)) {
                        visible.add(option);
                    }
                }
            }
        }
        clampScroll();
    }

    private String pathPrefix() {
        if (path.isEmpty()) {
            return "";
        }
        return String.join(".", path) + ".";
    }

    private boolean searching() {
        return search != null && !search.getValue().isBlank();
    }

    private void reset() {
        Map<String, String> defaults = new LinkedHashMap<>();
        for (Option option : defaultOptions()) {
            if (!option.isAction()) {
                defaults.put(option.key(), option.read());
            }
        }
        List<Option> targets = searching()
                ? new ArrayList<>(visible)
                : new ArrayList<>(grouped.getOrDefault(selected, List.of()));
        for (Option option : targets) {
            String fallback = defaults.get(option.key());
            if (!option.isAction() && fallback != null) {
                option.write(fallback);
            }
        }
        if (!searching() && ConfigOptions.EQUIPMENT.equals(selected)) {
            EquipmentDropConfig.restoreDefaults();
        }
        rebuild();
    }

    private boolean commitValues() {
        boolean changed = false;
        for (Map.Entry<Option, EditBox> entry : boxes.entrySet()) {
            Option option = entry.getKey();
            if (option.isAction()) {
                continue;
            }
            String typed = entry.getValue().getValue();
            if (typed.equals(option.read())) {
                continue;
            }
            option.write(typed);
            changed |= option.structural();
        }
        return changed;
    }

    private void commit() {
        if (commitValues()) {
            rebuild();
        }
    }

    private void rebuild() {
        List<String> keep = new ArrayList<>(path);
        int at = scroll;
        rebuildWidgets();
        path.clear();
        path.addAll(keep);
        refresh();
        scroll = at;
        clampScroll();
    }

    private void apply() {
        for (Map.Entry<Option, EditBox> entry : boxes.entrySet()) {
            entry.getKey().write(entry.getValue().getValue());
        }
        CombatStylesConfig.save();
        if (!ConfigEdits.connected()) {
            StatusEffectConfig.save();
            EquipmentDropConfig.save();
        } else {
            sendChanges();
        }
        minecraft.setScreen(parent);
    }

    private void sendChanges() {
        Set<EditConfigPayload> changed = ConfigEdits.changedSince(baseline);
        if (changed.isEmpty()) {
            return;
        }
        if (!ConfigEdits.permitted()) {
            ClientConfigSync.revert();
            ConfigEdits.tell(ConfigEdits.operatorOnly());
            return;
        }
        for (EditConfigPayload edit : changed) {
            if (!ConfigEdits.send(edit)) {
                ConfigEdits.tell(ConfigEdits.tooLarge());
            }
        }
    }

    @Override
    protected int rowHeight() {
        return ROW_HEIGHT;
    }

    private int optionsTop() {
        return listTop() + (path.isEmpty() ? 0 : ROW_HEIGHT);
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
    protected int rowCount() {
        return subjects.size() + visible.size();
    }

    private int sidebarTop() {
        return top + HEADER;
    }

    private int optionsLeft() {
        return left + SIDEBAR_WIDTH + GAP;
    }

    private int optionsWidth() {
        return PANEL_WIDTH - SIDEBAR_WIDTH - PAD - GAP;
    }

    @Override
    protected int scrollbarX() {
        return optionsLeft() + optionsWidth() + 1;
    }

    private int sidebarRows() {
        return Math.max(1, (listBottom() - sidebarTop()) / ROW_HEIGHT);
    }

    private int maxSidebarScroll() {
        return Math.max(0, groups.size() - sidebarRows());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickedSuggestion(mouseX, mouseY)) {
            return true;
        }
        if (beginScrollDrag(mouseX, mouseY)) {
            return true;
        }
        if (clickedToggle(mouseX, mouseY)) {
            return true;
        }
        if (clickedAction(mouseX, mouseY)) {
            return true;
        }
        if (!subjects.isEmpty() && clickedSubject(mouseX, mouseY)) {
            return true;
        }
        if (!path.isEmpty() && clickedBack(mouseX, mouseY)) {
            return true;
        }
        int rows = Math.min(sidebarRows(), groups.size() - sidebarScroll);
        for (int row = 0; row < rows; row++) {
            int y = sidebarTop() + row * ROW_HEIGHT;
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, left + PAD, y,
                    SIDEBAR_WIDTH - PAD, ROW_HEIGHT - 2)) {
                selected = groups.get(sidebarScroll + row);
                path.clear();
                search.setValue("");
                scroll = 0;
                refresh();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KEY_ENTER || keyCode == KEY_NUMPAD_ENTER) {
            commit();
            return true;
        }
        if (keyCode == KEY_ESCAPE) {
            requestClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < left + SIDEBAR_WIDTH) {
            sidebarScroll = Math.clamp(sidebarScroll - (int) Math.signum(scrollY),
                    0, maxSidebarScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.drawString(font, title, left + PAD, top + PAD, ScreenTheme.TEXT, false);

        hovered = null;
        renderSidebar(graphics, mouseX, mouseY);
        renderOptions(graphics, mouseX, mouseY);
        renderSuggestions(graphics, mouseX, mouseY);
        if (hovered != null && !hovered.description().isBlank() && matches.isEmpty()) {
            graphics.renderTooltip(font,
                    font.split(Component.literal(hovered.description()), TOOLTIP_WIDTH),
                    mouseX, mouseY);
        }
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    private Option focusedOption() {
        for (Map.Entry<Option, EditBox> entry : boxes.entrySet()) {
            if (entry.getValue().isFocused() && entry.getValue().visible
                    && !entry.getKey().kind().isEmpty()) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
        matches.clear();
        suggestBox = null;
        Option option = focusedOption();
        if (option == null) {
            return;
        }
        EditBox box = boxes.get(option);
        matches.addAll(option.suggestions(box.getValue()));
        if (matches.isEmpty()) {
            return;
        }
        suggestBox = box;
        int rows = Math.min(matches.size(), SUGGESTION_ROWS);
        int width = SUGGESTION_WIDTH;
        int listLeft = Math.min(box.getX(), left + PANEL_WIDTH - width - PAD);
        int top = box.getY() + FIELD_HEIGHT + 1;
        int height = rows * SUGGESTION_ROW + 2;

        graphics.fill(listLeft - 1, top - 1, listLeft + width + 1, top + height,
                ScreenTheme.PANEL);
        graphics.renderOutline(listLeft - 1, top - 1, width + 2, height + 1, ScreenTheme.ACCENT);
        for (int row = 0; row < rows; row++) {
            int y = top + row * SUGGESTION_ROW;
            boolean over = ScreenDraw.isWithin(mouseX, mouseY, listLeft, y,
                    width, SUGGESTION_ROW);
            if (over) {
                graphics.fill(listLeft, y, listLeft + width, y + SUGGESTION_ROW,
                        ScreenTheme.HOVER);
            }
            graphics.drawString(font,
                    ScreenDraw.trim(font, matches.get(row), width - 4),
                    listLeft + 2, y + 1, over ? ScreenTheme.ACCENT : ScreenTheme.TEXT, false);
        }
        suggestLeft = listLeft;
        suggestTop = top;
        suggestRows = rows;
    }

    private boolean clickedSuggestion(double mouseX, double mouseY) {
        if (suggestBox == null || matches.isEmpty()) {
            return false;
        }
        for (int row = 0; row < suggestRows; row++) {
            int y = suggestTop + row * SUGGESTION_ROW;
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, suggestLeft, y,
                    SUGGESTION_WIDTH, SUGGESTION_ROW)) {
                suggestBox.setValue(matches.get(row));
                commit();
                return true;
            }
        }
        return false;
    }

    private void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        int listLeft = left + PAD;
        int width = SIDEBAR_WIDTH - PAD;
        ScreenDraw.sunken(graphics, listLeft - 2, sidebarTop() - 2, width + 4,
                listBottom() - sidebarTop() + 4);

        graphics.enableScissor(listLeft - 2, sidebarTop() - 2,
                listLeft + width + 2, listBottom() + 2);
        int rows = Math.min(sidebarRows(), groups.size() - sidebarScroll);
        for (int row = 0; row < rows; row++) {
            int index = sidebarScroll + row;
            String group = groups.get(index);
            int y = sidebarTop() + row * ROW_HEIGHT;
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, listLeft, y,
                    width, ROW_HEIGHT - 2);
            boolean active = !searching() && group.equals(selected);

            ScreenDraw.rowBackground(graphics, listLeft, y, width, ROW_HEIGHT - 2,
                    index, hovered, active);
            graphics.drawString(font, ScreenDraw.trim(font, group, width - LABEL_INSET * 2),
                    listLeft + LABEL_INSET, y + 6,
                    active ? ScreenTheme.ACCENT : ScreenTheme.TEXT, false);
        }
        graphics.disableScissor();
    }

    private void drawToggle(GuiGraphics graphics, Option option, int x, int y,
                            int mouseX, int mouseY) {
        boolean on = ConfigOptions.TRUE.equals(option.read());
        boolean over = ScreenDraw.isWithin(mouseX, mouseY, x, y, FIELD_WIDTH, FIELD_HEIGHT);
        graphics.fill(x, y, x + FIELD_WIDTH, y + FIELD_HEIGHT,
                over ? ScreenTheme.HOVER : ScreenTheme.TRACK);
        ScreenDraw.fieldOutline(graphics, x, y, FIELD_WIDTH, FIELD_HEIGHT, over);
        Component value = Component.translatable(on ? KEY_TOGGLE_ON : KEY_TOGGLE_OFF);
        graphics.drawString(font, value,
                x + FIELD_WIDTH / 2 - font.width(value) / 2, y + 3,
                on ? ScreenTheme.GOOD : ScreenTheme.MUTED, false);
    }

    private boolean clickedToggle(double mouseX, double mouseY) {
        int listLeft = optionsLeft();
        int width = optionsWidth();
        int rows = visibleRows();
        for (int row = 0; row < rows && scroll + row < rowCount(); row++) {
            int index = scroll + row;
            if (index < subjects.size()) {
                continue;
            }
            Option option = visible.get(index - subjects.size());
            if (!option.isToggle()) {
                continue;
            }
            int y = optionsTop() + row * ROW_HEIGHT;
            if (y + ROW_HEIGHT > listBottom()) {
                break;
            }
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY,
                    listLeft + width - FIELD_WIDTH - LABEL_INSET, y + 3,
                    FIELD_WIDTH, FIELD_HEIGHT)) {
                option.toggle();
                if (option.structural()) {
                    rebuild();
                }
                return true;
            }
        }
        return false;
    }

    private boolean clickedAction(double mouseX, double mouseY) {
        int listLeft = optionsLeft();
        int width = optionsWidth();
        int rows = visibleRows();
        for (int row = 0; row < rows && scroll + row < rowCount(); row++) {
            int index = scroll + row;
            if (index < subjects.size()) {
                continue;
            }
            Option option = visible.get(index - subjects.size());
            if (!option.isAction()) {
                continue;
            }
            int y = optionsTop() + row * ROW_HEIGHT;
            if (y + ROW_HEIGHT > listBottom()) {
                break;
            }
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY,
                    listLeft, y, width, ROW_HEIGHT - 2)) {
                commitValues();
                option.run();
                rebuild();
                return true;
            }
        }
        return false;
    }

    private boolean clickedSubject(double mouseX, double mouseY) {
        int listLeft = optionsLeft();
        int width = optionsWidth();
        int rows = visibleRows();
        for (int row = 0; row < rows && scroll + row < subjects.size(); row++) {
            int y = optionsTop() + row * ROW_HEIGHT;
            if (y + ROW_HEIGHT > listBottom()) {
                break;
            }
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY,
                    listLeft, y, width, ROW_HEIGHT - 2)) {
                path.add(subjects.get(scroll + row));
                scroll = 0;
                refresh();
                return true;
            }
        }
        return false;
    }

    private boolean clickedBack(double mouseX, double mouseY) {
        int listLeft = optionsLeft();
        int width = optionsWidth();
        if (!ScreenDraw.isWithin((int) mouseX, (int) mouseY,
                listLeft, listTop(), width, ROW_HEIGHT - 2)) {
            return false;
        }
        path.removeLast();
        scroll = 0;
        refresh();
        return true;
    }

    private void renderOptions(GuiGraphics graphics, int mouseX, int mouseY) {
        int listLeft = optionsLeft();
        int width = optionsWidth();

        ScreenDraw.sunken(graphics, listLeft - 2, listTop() - 2, width + 4,
                listBottom() - listTop() + 4);

        for (EditBox box : boxes.values()) {
            box.setVisible(false);
        }

        graphics.enableScissor(listLeft - 2, listTop() - 2,
                listLeft + width + 2, listBottom() + 2);

        if (!path.isEmpty()) {
            boolean back = ScreenDraw.isWithin(mouseX, mouseY,
                    listLeft, listTop(), width, ROW_HEIGHT - 2);
            ScreenDraw.rowBackground(graphics, listLeft, listTop(), width, ROW_HEIGHT - 2,
                    0, back, false);
            graphics.drawString(font, Component.translatable(KEY_BACK,
                            ConfigOptions.prettify(path.getLast())),
                    listLeft + LABEL_INSET, listTop() + 6,
                    back ? ScreenTheme.ACCENT : ScreenTheme.MUTED, false);
        }

        if (subjects.isEmpty() && visible.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_EMPTY),
                    listLeft + LABEL_INSET, listTop() + 6, ScreenTheme.MUTED, false);
            graphics.disableScissor();
            return;
        }

        int labelWidth = width - FIELD_WIDTH - VALUE_GAP - LABEL_INSET * 2;
        int rows = visibleRows();
        int total = rowCount();
        for (int row = 0; row < rows && scroll + row < total; row++) {
            int index = scroll + row;
            int y = optionsTop() + row * ROW_HEIGHT;
            if (y + ROW_HEIGHT > listBottom()) {
                break;
            }
            boolean over = ScreenDraw.isWithin(mouseX, mouseY, listLeft, y,
                    width, ROW_HEIGHT - 2);
            ScreenDraw.rowBackground(graphics, listLeft, y, width, ROW_HEIGHT - 2,
                    index, over, false);

            if (index < subjects.size()) {
                graphics.drawString(font, ConfigOptions.prettify(subjects.get(index)),
                        listLeft + LABEL_INSET, y + 6,
                        over ? ScreenTheme.ACCENT : ScreenTheme.TEXT, false);
                continue;
            }

            Option option = visible.get(index - subjects.size());
            if (over) {
                hovered = option;
            }
            String label = searching() ? option.group() + " · " + option.label() : option.label();
            if (option.isAction()) {
                graphics.drawString(font, ScreenDraw.trim(font, label, width - LABEL_INSET * 2),
                        listLeft + LABEL_INSET, y + 6,
                        over ? ScreenTheme.ACCENT : ScreenTheme.MUTED, false);
                continue;
            }
            graphics.drawString(font, ScreenDraw.trim(font, label, labelWidth),
                    listLeft + LABEL_INSET, y + 6, ScreenTheme.TEXT, false);

            if (option.isToggle()) {
                drawToggle(graphics, option, listLeft + width - FIELD_WIDTH - LABEL_INSET,
                        y + 3, mouseX, mouseY);
                continue;
            }

            EditBox box = boxes.get(option);
            box.setPosition(listLeft + width - FIELD_WIDTH - LABEL_INSET, y + 3);
            box.setVisible(true);
        }
        graphics.disableScissor();

        drawScrollbar(graphics);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
