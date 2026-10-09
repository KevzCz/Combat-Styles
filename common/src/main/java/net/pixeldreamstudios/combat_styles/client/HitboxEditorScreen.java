package net.pixeldreamstudios.combat_styles.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.ExtraAttack;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.HitboxOffset;
import net.pixeldreamstudios.combat_styles.style.HitboxSet;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HitboxEditorScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 560;
    private static final int PANEL_HEIGHT = 338;
    private static final int HEADER = 28;
    private static final int FOOTER = 100;
    private static final int HINT_HEIGHT = 9;
    private static final int LIST_WIDTH = 136;
    private static final int ROW_HEIGHT = 13;
    private static final int BUTTON_WIDTH = 54;
    private static final int WIDE_BUTTON = 96;
    private static final int GAP = 4;

    private static final int FIELD_WIDTH = 40;
    private static final int FIELD_HEIGHT = 13;
    private static final int FIELD_LABEL = 24;

    private static final float SCALE = 13.0F;
    private static final float ZOOM_STEP = 0.12F;
    private static final float MIN_ZOOM = 0.45F;
    private static final float MAX_ZOOM = 2.6F;
    private static final float DEFAULT_YAW = 32.0F;
    private static final float DEFAULT_PITCH = 24.0F;
    private static final float ORBIT_SPEED = 0.6F;
    private static final float MOVE_SPEED = 0.055F;
    private static final float PITCH_LIMIT = 85.0F;
    private static final float GRID_STEP = 1.0F;
    private static final int GRID_LINES = 8;
    private static final float NUDGE = 0.25F;
    private static final float ALPHA = 0.9F;
    private static final float BODY_HEIGHT = 1.8F;
    private static final float BODY_HALF = 0.3F;
    private static final float ORIGIN_HEIGHT = 1.0F;
    private static final float REACH_MARK = 0.12F;

    private static final int COLOR_GRID = 0x40FFFFFF;
    private static final int COLOR_AXIS = 0x80FFFFFF;
    private static final int COLOR_REACH = 0x6AD9A959;
    private static final float[] BODY = {0.55F, 0.62F, 0.72F};
    private static final float[] DIMMED = {0.42F, 0.44F, 0.50F};
    private static final float[] SELECTED = {1.0F, 0.82F, 0.28F};
    private static final float[][] PALETTE = {
            {1.0F, 0.45F, 0.15F},
            {0.35F, 0.75F, 1.0F},
            {1.0F, 0.25F, 0.85F},
            {0.55F, 1.0F, 0.35F}
    };

    private static final double DEFAULT_REACH = 3.0D;

    private static final String KEY_TITLE = "screen.combat_styles.hitbox_editor";
    private static final String KEY_EMPTY_SELECTION = "screen.combat_styles.hitbox_no_selection";
    private static final String KEY_ADD = "screen.combat_styles.hitbox_add";
    private static final String KEY_COPY = "screen.combat_styles.hitbox_copy";
    private static final String KEY_DELETE = "screen.combat_styles.hitbox_delete";
    private static final String KEY_RESOLVE = "screen.combat_styles.hitbox_resolve";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_GROUP_MAIN = "screen.combat_styles.hitbox_group_main";
    private static final String KEY_GROUP_EXTRA = "screen.combat_styles.hitbox_group_extra";
    private static final String KEY_BOX = "screen.combat_styles.hitbox_row";
    private static final String KEY_EMPTY_GROUP = "screen.combat_styles.hitbox_group_empty";
    private static final String KEY_TUNE = "screen.combat_styles.hitbox_tune";
    private static final String KEY_TUNED_MARK = "screen.combat_styles.hitbox_tuned";
    private static final String KEY_HINT_VIEW = "screen.combat_styles.hitbox_hint_view";
    private static final String KEY_HINT_EDIT = "screen.combat_styles.hitbox_hint_edit";
    private static final String KEY_TUNED = "screen.combat_styles.hitbox_tuned";

    private static final int KEY_UP = 265;
    private static final int KEY_DOWN = 264;
    private static final int KEY_LEFT = 263;
    private static final int KEY_RIGHT = 262;
    private static final int KEY_PAGE_UP = 266;
    private static final int KEY_PAGE_DOWN = 267;
    private static final int KEY_DELETE_BOX = 261;

    private record Row(int group, int shape, boolean header, int affix) {

        private Row(int group, int shape, boolean header) {
            this(group, shape, header, -1);
        }

        private boolean isAffix() {
            return affix >= 0;
        }
    }

    private static final class Group {
        private final String label;
        private final List<HitboxSet.Shape> shapes = new ArrayList<>();
        private HitboxSet.Resolve resolve;

        private Group(String label, HitboxSet source) {
            this.label = label;
            this.resolve = source.resolve();
            this.shapes.addAll(source.shapes());
        }

        private HitboxSet toSet() {
            return shapes.isEmpty() ? HitboxSet.NONE
                    : new HitboxSet(resolve, List.copyOf(shapes));
        }
    }

    private final Consumer<HitboxSet> onApply;
    private final BiConsumer<Integer, HitboxSet> onExtraApply;
    private final List<Group> groups = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final double reach;

    private int selectedGroup;
    private int selectedShape = -1;
    private float viewYaw = DEFAULT_YAW;
    private float viewPitch = DEFAULT_PITCH;
    private float zoom = 1.0F;
    private boolean orbiting;
    private boolean movingBox;

    private EditBox forwardBox;
    private EditBox upBox;
    private EditBox rightBox;
    private EditBox widthBox;
    private EditBox heightBox;
    private EditBox depthBox;
    private EditBox priorityBox;
    private EditBox delayBox;
    private PanelButton tuneButton;
    private final StylePage page;
    private boolean syncing;

    public HitboxEditorScreen(Screen parent, HitboxSet current, List<ExtraAttack> extras,
                              double reach, Consumer<HitboxSet> onApply,
                              BiConsumer<Integer, HitboxSet> onExtraApply, StylePage page) {
        super(Component.translatable(KEY_TITLE), parent);
        this.onApply = onApply;
        this.onExtraApply = onExtraApply;
        this.page = page;
        this.reach = reach <= 0.0D ? DEFAULT_REACH : reach;
        groups.add(new Group(Component.translatable(KEY_GROUP_MAIN).getString(),
                current == null ? HitboxSet.NONE : current));
        List<ExtraAttack> hits = extras == null ? List.of() : extras;
        for (int index = 0; index < hits.size(); index++) {
            groups.add(new Group(
                    Component.translatable(KEY_GROUP_EXTRA, index + 1).getString(),
                    hits.get(index).boxes()));
        }
        rebuildRows();
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
        return rows.size();
    }

    @Override
    protected int listTop() {
        return top + HEADER;
    }

    @Override
    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER;
    }

    private void rebuildRows() {
        rows.clear();
        for (int group = 0; group < groups.size(); group++) {
            rows.add(new Row(group, -1, true));
            List<HitboxSet.Shape> shapes = groups.get(group).shapes;
            for (int shape = 0; shape < shapes.size(); shape++) {
                rows.add(new Row(group, shape, false));
                List<String> affixes = shapes.get(shape).affixes();
                for (int affix = 0; affix < affixes.size(); affix++) {
                    rows.add(new Row(group, shape, false, affix));
                }
            }
            if (shapes.isEmpty()) {
                rows.add(new Row(group, -1, false));
            }
        }
        clampScroll();
    }

    @Override
    protected void layout() {
        int fieldY = top + PANEL_HEIGHT - FOOTER + 4;
        int x = left + ScreenTheme.PADDING + FIELD_LABEL;
        int step = FIELD_WIDTH + FIELD_LABEL + GAP;

        forwardBox = numberBox(x, fieldY);
        upBox = numberBox(x + step, fieldY);
        rightBox = numberBox(x + step * 2, fieldY);
        priorityBox = numberBox(x + step * 3, fieldY);
        delayBox = numberBox(x + step * 4, fieldY);

        int sizeY = fieldY + FIELD_HEIGHT + GAP;
        widthBox = numberBox(x, sizeY);
        heightBox = numberBox(x + step, sizeY);
        depthBox = numberBox(x + step * 2, sizeY);

        tuneButton = addRenderableWidget(PanelButton.of(tuneLabel(), button -> tuneShape())
                .bounds(x + step * 3, sizeY, WIDE_BUTTON, FIELD_HEIGHT).build());

        int buttonY = top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING;
        int buttonX = left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY), button -> apply())
                .bounds(buttonX, buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        buttonX -= BUTTON_WIDTH + GAP;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(buttonX, buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        buttonX -= BUTTON_WIDTH + GAP;
        addRenderableWidget(PanelButton.of(resolveLabel(), button -> {
            Group group = activeGroup();
            group.resolve = group.resolve == HitboxSet.Resolve.UNION
                    ? HitboxSet.Resolve.PRIORITY
                    : HitboxSet.Resolve.UNION;
            button.setMessage(resolveLabel());
        }).bounds(buttonX, buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());

        int leftX = left + ScreenTheme.PADDING;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_ADD), button -> addShape())
                .bounds(leftX, buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_COPY), button -> copyShape())
                .bounds(leftX + BUTTON_WIDTH + GAP, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_DELETE),
                        button -> deleteShape())
                .bounds(leftX + (BUTTON_WIDTH + GAP) * 2, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());

        syncFields();
    }

    private EditBox numberBox(int x, int y) {
        EditBox box = new FieldBox(font, x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
        box.setMaxLength(8);
        box.setResponder(text -> commitFields());
        addRenderableWidget(box);
        return box;
    }

    private Group activeGroup() {
        return groups.get(Math.clamp(selectedGroup, 0, groups.size() - 1));
    }

    private Component tuneLabel() {
        HitboxSet.Shape current = selectedShape();
        return Component.translatable(KEY_TUNE, current != null && current.tunes()
                ? Component.translatable(KEY_TUNED_MARK)
                : Component.empty());
    }

    private void tuneShape() {
        HitboxSet.Shape current = selectedShape();
        if (current == null || page == null) {
            return;
        }
        int index = selectedShape;
        int group = selectedGroup;
        minecraft.setScreen(new AttackEditorScreen(this, page, current.overrideOrEmpty(),
                applied -> applyTuning(group, index, applied), "", name -> {
                }, true)
                .withAffixes(current.affixes(),
                        chosen -> applyAffixes(group, index, chosen))
                .withTuning(current.tuning(),
                        tuned -> applyTuning(group, index, tuned)));
    }

    private void applyTuning(int group, int index, AttackOverride applied) {
        HitboxSet.Shape shape = shapeAt(group, index);
        if (shape == null) {
            return;
        }
        groups.get(group).shapes.set(index, shape.withOverride(
                applied == null || applied.equals(AttackOverride.EMPTY)
                        ? Optional.empty()
                        : Optional.of(applied)));
        rebuildRows();
    }

    private void applyAffixes(int group, int index, List<String> chosen) {
        HitboxSet.Shape shape = shapeAt(group, index);
        if (shape == null) {
            return;
        }
        groups.get(group).shapes.set(index, shape.withAffixes(chosen, shape.tuning()));
        rebuildRows();
    }

    private void applyTuning(int group, int index, Map<String, AffixTuning> tuned) {
        HitboxSet.Shape shape = shapeAt(group, index);
        if (shape == null) {
            return;
        }
        groups.get(group).shapes.set(index, shape.withAffixes(shape.affixes(), tuned));
        rebuildRows();
    }

    private HitboxSet.Shape shapeAt(int group, int index) {
        if (group < 0 || group >= groups.size()) {
            return null;
        }
        List<HitboxSet.Shape> shapes = groups.get(group).shapes;
        return index < 0 || index >= shapes.size() ? null : shapes.get(index);
    }

    private Component resolveLabel() {
        return Component.translatable(KEY_RESOLVE,
                Component.literal(activeGroup().resolve.getSerializedName()));
    }

    private void addShape() {
        Group group = activeGroup();
        group.shapes.add(new HitboxSet.Shape(HitboxOffset.NONE, group.shapes.size(),
                Optional.empty()));
        selectedShape = group.shapes.size() - 1;
        rebuildRows();
        syncFields();
    }

    private void copyShape() {
        HitboxSet.Shape current = selectedShape();
        if (current == null) {
            return;
        }
        Group group = activeGroup();
        group.shapes.add(new HitboxSet.Shape(current.offset(), current.priority(),
                current.damageSpec(), current.width(), current.height(), current.depth(),
                current.affixes(), current.tuning(), current.override(), current.delayTicks()));
        selectedShape = group.shapes.size() - 1;
        rebuildRows();
        syncFields();
    }

    private void deleteShape() {
        Group group = activeGroup();
        if (selectedShape < 0 || selectedShape >= group.shapes.size()) {
            return;
        }
        group.shapes.remove(selectedShape);
        selectedShape = group.shapes.isEmpty() ? -1
                : Math.min(selectedShape, group.shapes.size() - 1);
        rebuildRows();
        syncFields();
    }

    private HitboxSet.Shape selectedShape() {
        Group group = activeGroup();
        return selectedShape >= 0 && selectedShape < group.shapes.size()
                ? group.shapes.get(selectedShape)
                : null;
    }

    private void syncFields() {
        HitboxSet.Shape current = selectedShape();
        syncing = true;
        boolean has = current != null;
        for (EditBox box : List.of(forwardBox, upBox, rightBox, widthBox, heightBox,
                depthBox, priorityBox, delayBox)) {
            box.setVisible(has);
        }
        tuneButton.visible = has;
        if (has) {
            HitboxOffset offset = current.offset();
            forwardBox.setValue(StyleText.formatDecimal(offset.forward()));
            upBox.setValue(StyleText.formatDecimal(offset.up()));
            rightBox.setValue(StyleText.formatDecimal(offset.right()));
            widthBox.setValue(StyleText.formatDecimal(current.width()));
            heightBox.setValue(StyleText.formatDecimal(current.height()));
            depthBox.setValue(StyleText.formatDecimal(current.depth()));
            priorityBox.setValue(String.valueOf(current.priority()));
            delayBox.setValue(String.valueOf(current.delayTicks()));
            tuneButton.setMessage(tuneLabel());
        }
        syncing = false;
    }

    private void commitFields() {
        if (syncing) {
            return;
        }
        HitboxSet.Shape current = selectedShape();
        if (current == null) {
            return;
        }
        HitboxOffset offset = current.offset();
        activeGroup().shapes.set(selectedShape, new HitboxSet.Shape(
                new HitboxOffset(
                        parse(forwardBox, offset.forward()),
                        parse(upBox, offset.up()),
                        parse(rightBox, offset.right()),
                        offset.yaw(), offset.pitch(), offset.roll()),
                (int) parse(priorityBox, current.priority()),
                current.damageSpec(),
                parse(widthBox, current.width()),
                parse(heightBox, current.height()),
                parse(depthBox, current.depth()),
                current.affixes(), current.tuning(), current.override(),
                (int) parse(delayBox, current.delayTicks())));
    }

    private static float parse(EditBox box, double fallback) {
        try {
            return Float.parseFloat(box.getValue().trim());
        } catch (NumberFormatException ignored) {
            return (float) fallback;
        }
    }

    private void apply() {
        onApply.accept(groups.getFirst().toSet());
        if (onExtraApply != null) {
            for (int index = 1; index < groups.size(); index++) {
                onExtraApply.accept(index - 1, groups.get(index).toSet());
            }
        }
        minecraft.setScreen(parent);
    }

    private Vec3 combat_styles$screenAxis(float screenX, float screenY) {
        Quaternionf view = new Quaternionf()
                .rotateX(viewPitch * Mth.DEG_TO_RAD)
                .rotateY(viewYaw * Mth.DEG_TO_RAD);
        Vector3f axis = view.transformInverse(new Vector3f(screenX, 0.0F, screenY));
        Vec3 flat = new Vec3(axis.x(), 0.0D, axis.z());
        return flat.lengthSqr() < 1.0E-6D ? Vec3.ZERO : flat.normalize();
    }

    private void nudge(float forward, float up, float right) {
        HitboxSet.Shape current = selectedShape();
        if (current == null) {
            return;
        }
        HitboxOffset offset = current.offset();
        activeGroup().shapes.set(selectedShape, current.withOffset(
                new HitboxOffset(offset.forward() + forward, offset.up() + up,
                        offset.right() + right, offset.yaw(), offset.pitch(),
                        offset.roll())));
        syncFields();
    }

    private void resize(float width, float height, float depth) {
        HitboxSet.Shape current = selectedShape();
        if (current == null) {
            return;
        }
        activeGroup().shapes.set(selectedShape, current.withSize(
                current.width() + width, current.height() + height, current.depth() + depth));
        syncFields();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double scrollX, double scrollY) {
        if (!insideViewport(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        zoom = Math.clamp(zoom + (float) scrollY * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (getFocused() instanceof EditBox) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        boolean resizing = hasControlDown();
        switch (keyCode) {
            case KEY_UP -> {
                if (resizing) {
                    resize(0.0F, 0.0F, NUDGE);
                } else {
                    nudge(NUDGE, 0.0F, 0.0F);
                }
            }
            case KEY_DOWN -> {
                if (resizing) {
                    resize(0.0F, 0.0F, -NUDGE);
                } else {
                    nudge(-NUDGE, 0.0F, 0.0F);
                }
            }
            case KEY_LEFT -> {
                if (resizing) {
                    resize(-NUDGE, 0.0F, 0.0F);
                } else {
                    nudge(0.0F, 0.0F, -NUDGE);
                }
            }
            case KEY_RIGHT -> {
                if (resizing) {
                    resize(NUDGE, 0.0F, 0.0F);
                } else {
                    nudge(0.0F, 0.0F, NUDGE);
                }
            }
            case KEY_PAGE_UP -> {
                if (resizing) {
                    resize(0.0F, NUDGE, 0.0F);
                } else {
                    nudge(0.0F, NUDGE, 0.0F);
                }
            }
            case KEY_PAGE_DOWN -> {
                if (resizing) {
                    resize(0.0F, -NUDGE, 0.0F);
                } else {
                    nudge(0.0F, -NUDGE, 0.0F);
                }
            }
            case KEY_DELETE_BOX -> deleteShape();
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = rowAt(mouseX, mouseY);
        if (index >= 0 && index < rows.size()) {
            Row row = rows.get(index);
            selectedGroup = row.group();
            selectedShape = row.header() ? -1 : row.shape();
            syncFields();
            return true;
        }
        if (insideViewport(mouseX, mouseY)) {
            if (hasAltDown()) {
                orbiting = true;
            } else {
                movingBox = selectedShape() != null;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean insideViewport(double mouseX, double mouseY) {
        return mouseX > left + ScreenTheme.PADDING + LIST_WIDTH
                && mouseX < left + PANEL_WIDTH - ScreenTheme.PADDING
                && mouseY > listTop() && mouseY < listBottom();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (orbiting) {
            viewYaw += (float) dragX * ORBIT_SPEED;
            viewPitch = Mth.clamp(viewPitch + (float) dragY * ORBIT_SPEED,
                    -PITCH_LIMIT, PITCH_LIMIT);
            return true;
        }
        if (movingBox) {
            float speed = MOVE_SPEED / zoom;
            if (hasShiftDown()) {
                nudge(0.0F, (float) -dragY * speed, 0.0F);
                return true;
            }
            if (hasControlDown()) {
                nudge(0.0F, 0.0F, (float) dragX * speed);
                return true;
            }
            Vec3 moveRight = combat_styles$screenAxis(1.0F, 0.0F);
            Vec3 moveDepth = combat_styles$screenAxis(0.0F, 1.0F);
            double pushX = dragX * speed;
            double pushY = dragY * speed;
            float right = (float) (moveRight.x * pushX + moveDepth.x * pushY);
            float forward = (float) (moveRight.z * pushX + moveDepth.z * pushY);
            nudge(forward, 0.0F, right);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        orbiting = false;
        movingBox = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected int rowAt(double mouseX, double mouseY) {
        if (mouseX < left + ScreenTheme.PADDING
                || mouseX > left + ScreenTheme.PADDING + LIST_WIDTH) {
            return -1;
        }
        return super.rowAt(mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        drawPanel(graphics);
        drawTitle(graphics, getTitle(), top + 9);

        renderList(graphics, mouseX, mouseY);
        renderViewport(graphics);
        renderFieldLabels(graphics);
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    private void renderFieldLabels(GuiGraphics graphics) {
        int fieldY = top + PANEL_HEIGHT - FOOTER + 4;
        int sizeY = fieldY + FIELD_HEIGHT + GAP;
        int damageY = sizeY + FIELD_HEIGHT + GAP;
        int x = left + ScreenTheme.PADDING;
        int step = FIELD_WIDTH + FIELD_LABEL + GAP;

        if (selectedShape() == null) {
            graphics.drawString(font, Component.translatable(KEY_EMPTY_SELECTION),
                    x, fieldY + 3, ScreenTheme.DIM, false);
            graphics.drawString(font, Component.translatable(KEY_HINT_VIEW),
                    x, damageY + FIELD_HEIGHT + GAP + 2, ScreenTheme.MUTED, false);
            return;
        }

        sunkenField(graphics, x, fieldY);
        sunkenField(graphics, x + step, fieldY);
        sunkenField(graphics, x + step * 2, fieldY);
        sunkenField(graphics, x + step * 3, fieldY);
        sunkenField(graphics, x + step * 4, fieldY);
        sunkenField(graphics, x, sizeY);
        sunkenField(graphics, x + step, sizeY);
        sunkenField(graphics, x + step * 2, sizeY);

        label(graphics, "Fwd", x, fieldY);
        label(graphics, "Up", x + step, fieldY);
        label(graphics, "Rgt", x + step * 2, fieldY);
        label(graphics, "Pri", x + step * 3, fieldY);
        label(graphics, "Wait", x + step * 4, fieldY);
        label(graphics, "W", x, sizeY);
        label(graphics, "H", x + step, sizeY);
        label(graphics, "D", x + step * 2, sizeY);
        int hintY = damageY + 2;
        graphics.drawString(font, Component.translatable(KEY_HINT_VIEW),
                x, hintY, ScreenTheme.MUTED, false);
        graphics.drawString(font, Component.translatable(KEY_HINT_EDIT),
                x, hintY + HINT_HEIGHT, ScreenTheme.MUTED, false);
    }

    private void sunkenField(GuiGraphics graphics, int x, int y) {
        ScreenDraw.sunken(graphics, x + FIELD_LABEL - 2, y - 1, FIELD_WIDTH + 4,
                FIELD_HEIGHT + 2);
    }

    private void label(GuiGraphics graphics, String text, int x, int y) {
        graphics.drawString(font, text, x, y + 3, ScreenTheme.MUTED, false);
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = left + ScreenTheme.PADDING;
        ScreenDraw.sunken(graphics, x - 2, listTop() - 2, LIST_WIDTH + 4,
                listBottom() - listTop() + 4);
        graphics.enableScissor(x - 2, listTop() - 2, x + LIST_WIDTH + 2, listBottom() + 2);
        int hovered = rowAt(mouseX, mouseY);
        int last = Math.min(rows.size(), scroll + visibleRows());
        for (int index = scroll; index < last; index++) {
            Row row = rows.get(index);
            int y = rowY(index);
            if (row.header()) {
                ScreenDraw.drawTrimmed(graphics, font, groups.get(row.group()).label,
                        x + 2, y + 3, LIST_WIDTH - 6, ScreenTheme.HEADING);
                continue;
            }
            if (row.shape() < 0) {
                graphics.drawString(font, Component.translatable(KEY_EMPTY_GROUP),
                        x + 10, y + 3, ScreenTheme.DIM, false);
                continue;
            }
            boolean active = row.group() == selectedGroup && row.shape() == selectedShape;
            ScreenDraw.rowBackground(graphics, x, y, LIST_WIDTH, ROW_HEIGHT - 1,
                    index, index == hovered, active && !row.isAffix());
            HitboxSet.Shape shape = groups.get(row.group()).shapes.get(row.shape());
            if (row.isAffix()) {
                String affixId = shape.affixes().get(row.affix());
                String tuned = shape.tuning().containsKey(affixId)
                        ? Component.translatable(KEY_TUNED).getString() : "";
                ScreenDraw.drawTrimmed(graphics, font, "• " + StyleText.prettify(affixId) + tuned,
                        x + 18, y + 3, LIST_WIDTH - 20, ScreenTheme.MUTED);
                continue;
            }
            String label = Component.translatable(KEY_BOX,
                    row.shape() + 1, shape.priority()).getString();
            if (shape.tunes()) {
                label += Component.translatable(KEY_TUNED).getString();
            }
            ScreenDraw.drawTrimmed(graphics, font, label,
                    x + 10, y + 3, LIST_WIDTH - 12, shape.tunes() ? ScreenTheme.HEADING : ScreenTheme.TEXT);
        }
        graphics.disableScissor();
    }

    private void renderViewport(GuiGraphics graphics) {
        int viewLeft = left + ScreenTheme.PADDING + LIST_WIDTH + GAP;
        int viewRight = left + PANEL_WIDTH - ScreenTheme.PADDING;
        int viewTop = listTop();
        int viewBottom = listBottom();
        ScreenDraw.sunken(graphics, viewLeft, viewTop,
                viewRight - viewLeft, viewBottom - viewTop);
        graphics.enableScissor(viewLeft, viewTop, viewRight, viewBottom);

        PoseStack poses = graphics.pose();
        poses.pushPose();
        poses.translate((viewLeft + viewRight) / 2.0F, (viewTop + viewBottom) / 2.0F, 100.0F);
        poses.scale(SCALE * zoom, -SCALE * zoom, SCALE * zoom);
        poses.mulPose(new Quaternionf().rotateX(viewPitch * Mth.DEG_TO_RAD));
        poses.mulPose(new Quaternionf().rotateY(viewYaw * Mth.DEG_TO_RAD));
        poses.translate(0.0F, -BODY_HEIGHT / 2.0F, 0.0F);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        drawGrid(poses, lines);
        drawReach(poses, lines);
        drawBody(poses, lines);
        for (int group = 0; group < groups.size(); group++) {
            List<HitboxSet.Shape> shapes = groups.get(group).shapes;
            for (int index = 0; index < shapes.size(); index++) {
                boolean active = group == selectedGroup;
                float[] colour = active && index == selectedShape
                        ? SELECTED
                        : (active ? PALETTE[index % PALETTE.length] : DIMMED);
                drawShape(poses, lines, shapes.get(index), colour);
            }
        }

        buffers.endBatch(RenderType.lines());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poses.popPose();
        graphics.disableScissor();
    }

    private void drawGrid(PoseStack poses, VertexConsumer lines) {
        float span = GRID_LINES * GRID_STEP;
        for (int step = -GRID_LINES; step <= GRID_LINES; step++) {
            float at = step * GRID_STEP;
            int colour = step == 0 ? COLOR_AXIS : COLOR_GRID;
            line(poses, lines, new Vec3(-span, 0.0D, at), new Vec3(span, 0.0D, at), colour);
            line(poses, lines, new Vec3(at, 0.0D, -span), new Vec3(at, 0.0D, span), colour);
        }
    }

    private void drawReach(PoseStack poses, VertexConsumer lines) {
        line(poses, lines, new Vec3(0.0D, ORIGIN_HEIGHT, 0.0D),
                new Vec3(0.0D, ORIGIN_HEIGHT, reach), COLOR_REACH);
        line(poses, lines, new Vec3(-REACH_MARK * 2, ORIGIN_HEIGHT, reach),
                new Vec3(REACH_MARK * 2, ORIGIN_HEIGHT, reach), COLOR_REACH);
        for (int step = 1; step <= (int) Math.floor(reach); step++) {
            line(poses, lines, new Vec3(-REACH_MARK, ORIGIN_HEIGHT, step),
                    new Vec3(REACH_MARK, ORIGIN_HEIGHT, step), COLOR_REACH);
        }
    }

    private void drawBody(PoseStack poses, VertexConsumer lines) {
        box(poses, lines, new Vec3(-BODY_HALF, 0.0D, -BODY_HALF),
                new Vec3(BODY_HALF, BODY_HEIGHT, BODY_HALF),
                pack(BODY[0], BODY[1], BODY[2]));
    }

    private void drawShape(PoseStack poses, VertexConsumer lines, HitboxSet.Shape shape,
                           float[] colour) {
        HitboxOffset offset = shape.offset().clamped();
        Vec3 centre = new Vec3(offset.right(), offset.up() + ORIGIN_HEIGHT, offset.forward());
        Vec3 half = new Vec3(shape.clampedWidth() / 2.0D, shape.clampedHeight() / 2.0D,
                shape.clampedDepth() / 2.0D);
        box(poses, lines, centre.subtract(half), centre.add(half),
                pack(colour[0], colour[1], colour[2]));
    }

    private void box(PoseStack poses, VertexConsumer lines, Vec3 min, Vec3 max, int packed) {
        Vec3[] bottom = {
                new Vec3(min.x, min.y, min.z), new Vec3(max.x, min.y, min.z),
                new Vec3(max.x, min.y, max.z), new Vec3(min.x, min.y, max.z)};
        Vec3[] topFace = {
                new Vec3(min.x, max.y, min.z), new Vec3(max.x, max.y, min.z),
                new Vec3(max.x, max.y, max.z), new Vec3(min.x, max.y, max.z)};
        for (int index = 0; index < 4; index++) {
            line(poses, lines, bottom[index], bottom[(index + 1) % 4], packed);
            line(poses, lines, topFace[index], topFace[(index + 1) % 4], packed);
            line(poses, lines, bottom[index], topFace[index], packed);
        }
    }

    private static int pack(float red, float green, float blue) {
        return (Math.round(ALPHA * 255) << 24)
                | (Math.round(red * 255) << 16)
                | (Math.round(green * 255) << 8)
                | Math.round(blue * 255);
    }

    private void line(PoseStack poses, VertexConsumer lines, Vec3 from, Vec3 to, int colour) {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        direction = direction.normalize();
        lines.addVertex(poses.last(), (float) from.x, (float) from.y, (float) from.z)
                .setColor(colour)
                .setNormal(poses.last(), (float) direction.x, (float) direction.y,
                        (float) direction.z);
        lines.addVertex(poses.last(), (float) to.x, (float) to.y, (float) to.z)
                .setColor(colour)
                .setNormal(poses.last(), (float) direction.x, (float) direction.y,
                        (float) direction.z);
    }
}
