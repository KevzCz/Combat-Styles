package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.ExtraAttack;
import net.pixeldreamstudios.combat_styles.style.InheritGroups;
import net.pixeldreamstudios.combat_styles.style.InheritMode;
import net.pixeldreamstudios.combat_styles.style.SlashMode;
import net.pixeldreamstudios.combat_styles.style.SlashAnchor;
import net.pixeldreamstudios.combat_styles.style.SlashPlacement;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SlashListScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 224;
    private static final int HEADER = 48;
    private static final int FOOTER = 30;
    private static final int ROW_HEIGHT = 22;
    private static final int BUTTON_WIDTH = 70;
    private static final int GAP = 4;
    private static final int TITLE_Y = 10;
    private static final int HINT_Y = 24;
    private static final int INDEX_WIDTH = 16;
    private static final int REMOVE_WIDTH = 14;
    private static final int NAME_Y = 4;
    private static final int DESC_Y = 13;
    private static final int MAX_SLASHES = 12;

    private static final String KEY_TITLE = "screen.combat_styles.slash_list";
    private static final String KEY_HINT = "screen.combat_styles.slash_list_hint";
    private static final String KEY_ADD = "screen.combat_styles.slash_add";
    private static final String KEY_EMPTY = "screen.combat_styles.slash_list_empty";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_DEFAULT_TYPE = "screen.combat_styles.slash_type_default";
    private static final String KEY_REMOVE = "screen.combat_styles.slash_remove";
    private static final String KEY_MODE_PREFIX = "screen.combat_styles.slash_mode_";
    private static final String KEY_AT = "screen.combat_styles.slash_summary_at";
    private static final String KEY_INSTANT = "screen.combat_styles.slash_summary_instant";
    private static final String KEY_TIP_OFFSET = "screen.combat_styles.slash_tip_offset";
    private static final String KEY_TIP_ANGLE = "screen.combat_styles.slash_tip_angle";
    private static final String KEY_TIP_FOLLOW = "screen.combat_styles.slash_tip_follow";
    private static final String KEY_TIP_DAMAGE = "screen.combat_styles.slash_tip_damage";
    private static final String KEY_TIP_ATTRS = "screen.combat_styles.slash_tip_attrs";
    private static final String KEY_TIP_INHERIT = "screen.combat_styles.slash_tip_inherit";
    private static final String SUMMARY_SEPARATOR = ", ";
    private static final String SUMMARY_JOIN = ": ";

    private final Consumer<List<SlashPlacement>> onApply;
    private final List<SlashPlacement> slashes;
    private final StylePage page;

    private List<Component> tooltip;
    private boolean particleOnly;

    public SlashListScreen(Screen parent, List<SlashPlacement> current,
                           Consumer<List<SlashPlacement>> onApply) {
        this(parent, null, current, onApply);
    }

    public SlashListScreen(Screen parent, StylePage page, List<SlashPlacement> current,
                           Consumer<List<SlashPlacement>> onApply) {
        super(Component.translatable(KEY_TITLE), parent);
        this.page = page;
        this.onApply = onApply;
        this.slashes = new ArrayList<>(current == null ? List.of() : current);
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
        return slashes.size();
    }

    @Override
    protected void layout() {
        int buttonY = top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_ADD),
                        button -> addSlash())
                .bounds(left + ScreenTheme.PADDING, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH * 2 - GAP,
                        buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY), button -> {
            onApply.accept(List.copyOf(slashes));
            minecraft.setScreen(parent);
        }).bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH, buttonY,
                BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private void addSlash() {
        if (slashes.size() >= MAX_SLASHES) {
            return;
        }
        if (particleOnly) {
            slashes.add(SlashPlacement.DEFAULT.withMode(SlashMode.PARTICLE));
            openEditor(slashes.size() - 1);
            return;
        }
        minecraft.setScreen(new SlashTypeScreen(this, mode -> {
            SlashPlacement created = SlashPlacement.DEFAULT.withMode(mode);
            slashes.add(created);
            openEditor(slashes.size() - 1);
        }));
    }

    private void openEditor(int index) {
        SlashEditorScreen editor = new SlashEditorScreen(this, page, slashes.get(index),
                edited -> slashes.set(index, edited));
        minecraft.setScreen(particleOnly ? editor.particlesOnly() : editor);
    }

    public SlashListScreen particlesOnly() {
        this.particleOnly = true;
        return this;
    }

    @Override
    protected int listTop() {
        return top + HEADER;
    }

    @Override
    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER;
    }

    private int removeX() {
        return contentRight() - REMOVE_WIDTH;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (beginScrollDrag(mouseX, mouseY)) {
            return true;
        }
        int index = rowAt(mouseX, mouseY);
        if (index < 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (ScreenDraw.isWithin(mouseX, mouseY, removeX(), rowY(index),
                REMOVE_WIDTH, ROW_HEIGHT)) {
            slashes.remove(index);
            return true;
        }
        openEditor(index);
        return true;
    }

    private static int colourFor(SlashMode mode) {
        return switch (mode) {
            case PARTICLE -> ScreenTheme.ACCENT;
            case ATTACK -> ScreenTheme.BAD;
            case BOTH -> ScreenTheme.GOOD;
        };
    }

    private String titleOf(SlashPlacement slash) {
        String type = slash.particleType() == null || slash.particleType().isBlank()
                ? Component.translatable(KEY_DEFAULT_TYPE).getString()
                : slash.particleType();
        return slash.mode().spawnsParticle()
                ? type
                : Component.translatable(KEY_MODE_PREFIX
                        + slash.mode().getSerializedName()).getString();
    }

    private Component summaryOf(SlashPlacement slash) {
        Component timing = slash.delayTicks() <= 0
                ? Component.translatable(KEY_INSTANT)
                : Component.translatable(KEY_AT, slash.delayTicks());
        return Component.translatable(KEY_MODE_PREFIX + slash.mode().getSerializedName())
                .copy().append(Component.literal(" · ")).append(timing);
    }

    private List<Component> tooltipFor(SlashPlacement slash) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(titleOf(slash)).withStyle(ChatFormatting.GOLD));
        lines.add(summaryOf(slash).copy().withStyle(ChatFormatting.GRAY));
        if (slash.mode().spawnsParticle()) {
            lines.add(Component.translatable(KEY_TIP_OFFSET,
                            StyleText.formatDecimal(slash.x()),
                            StyleText.formatDecimal(slash.y()),
                            StyleText.formatDecimal(slash.z()))
                    .withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable(KEY_TIP_ANGLE,
                            StyleText.formatDecimal(slash.localYaw()),
                            StyleText.formatDecimal(slash.pitch()),
                            StyleText.formatDecimal(slash.roll()))
                    .withStyle(ChatFormatting.DARK_GRAY));
            if (slash.anchor() != SlashAnchor.WEAPON) {
                lines.add(Component.translatable(KEY_TIP_FOLLOW,
                                Component.translatable(slash.anchor().translationKey()))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        if (slash.mode().dealsDamage()) {
            ExtraAttack hit = slash.attack().orElse(ExtraAttack.DEFAULT);
            AttackOverride override = hit.overrideOrEmpty();
            override.damageMultiplier().ifPresent(value ->
                    lines.add(Component.translatable(KEY_TIP_DAMAGE,
                                    StyleText.formatMultiplier(value))
                            .withStyle(ChatFormatting.YELLOW)));
            if (!override.attributes().isEmpty()) {
                lines.add(Component.translatable(KEY_TIP_ATTRS,
                                override.attributes().size())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            InheritGroups groups = hit.inheritOrAll();
            if (!groups.all()) {
                lines.add(Component.translatable(KEY_TIP_INHERIT, inheritSummary(groups))
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        return lines;
    }

    private static String inheritSummary(InheritGroups groups) {
        StringBuilder builder = new StringBuilder();
        for (String group : InheritGroups.GROUPS) {
            InheritMode mode = groups.mode(group);
            if (mode == InheritMode.PARENT) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(SUMMARY_SEPARATOR);
            }
            builder.append(Component.translatable(InheritGroups.translationKey(group)).getString())
                    .append(SUMMARY_JOIN)
                    .append(Component.translatable(mode.translationKey()).getString());
        }
        return builder.toString();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        tooltip = null;
        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(font, Component.translatable(KEY_TITLE),
                left + ScreenTheme.PADDING, top + TITLE_Y, ScreenTheme.HEADING, false);
        graphics.drawString(font, Component.translatable(KEY_HINT),
                left + ScreenTheme.PADDING, top + HINT_Y, ScreenTheme.MUTED, false);

        ScreenDraw.sunken(graphics, left + ScreenTheme.PADDING, listTop() - 2,
                PANEL_WIDTH - ScreenTheme.PADDING * 2, visibleRows() * ROW_HEIGHT + 4);

        if (slashes.isEmpty()) {
            graphics.drawString(font, Component.translatable(KEY_EMPTY),
                    left + ScreenTheme.PADDING + INDEX_WIDTH, listTop() + NAME_Y,
                    ScreenTheme.DIM, false);
            drawWidgets(graphics, mouseX, mouseY, partialTick);
            return;
        }

        int hovered = rowAt(mouseX, mouseY);
        int first = scroll;
        int last = Math.min(slashes.size(), first + visibleRows());
        for (int index = first; index < last; index++) {
            SlashPlacement slash = slashes.get(index);
            int y = rowY(index);
            boolean isHovered = index == hovered;
            ScreenDraw.rowBackground(graphics, left + ScreenTheme.PADDING + 1, y,
                    PANEL_WIDTH - ScreenTheme.PADDING * 2 - 2, ROW_HEIGHT - 1,
                    index, isHovered, false);
            ScreenDraw.accentBar(graphics, left + ScreenTheme.PADDING + 1, y,
                    ROW_HEIGHT - 1, colourFor(slash.mode()));

            graphics.drawString(font, String.valueOf(index + 1),
                    left + ScreenTheme.PADDING + GAP + 2, y + NAME_Y,
                    ScreenTheme.MUTED, false);
            graphics.drawString(font, titleOf(slash),
                    left + ScreenTheme.PADDING + INDEX_WIDTH, y + NAME_Y,
                    ScreenTheme.TEXT, false);
            graphics.drawString(font, summaryOf(slash),
                    left + ScreenTheme.PADDING + INDEX_WIDTH, y + DESC_Y,
                    ScreenTheme.MUTED, false);
            graphics.drawString(font, Component.translatable(KEY_REMOVE),
                    removeX() + GAP, y + NAME_Y, ScreenTheme.BAD, false);

            if (isHovered) {
                tooltip = tooltipFor(slash);
            }
        }

        ScreenDraw.scrollbar(graphics, left + PANEL_WIDTH - ScreenTheme.PADDING - 2,
                listTop(), visibleRows() * ROW_HEIGHT, visibleRows(), slashes.size(), scroll);

        drawWidgets(graphics, mouseX, mouseY, partialTick);

        if (tooltip != null && !tooltip.isEmpty()) {
            ScreenDraw.tooltip(graphics, font, tooltip, mouseX, mouseY);
        }
    }

}
