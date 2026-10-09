package net.pixeldreamstudios.combat_styles.client;

import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.equipment.EquipmentPoses;
import net.pixeldreamstudios.combat_styles.equipment.PoseTuning;
import net.pixeldreamstudios.combat_styles.equipment.StackPose;
import net.pixeldreamstudios.combat_styles.network.ApplyPosePayload;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.Optional;

public class PoseEditorScreen extends PanelScreen {

    public interface Saved {
        void accept(Optional<String> pose, Optional<String> offHandPose, PoseTuning tuning);
    }

    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 246;
    private static final int TITLE_Y = 10;
    private static final int POSE_Y = 26;
    private static final int ROW_HEIGHT = 16;
    private static final int TEXT_INSET = 4;
    private static final int GAP = 4;
    private static final int LABEL_WIDTH = 62;
    private static final int FIELD_WIDTH = 34;
    private static final int FIELD_HEIGHT = 12;
    private static final int BONE_ROW_Y = 78;
    private static final int BONE_ROW_HEIGHT = 18;
    private static final int PREVIEW_SIZE = 96;
    private static final int PREVIEW_GAP = 6;
    private static final int BUTTON_WIDTH = 60;
    private static final float DRAG_SENSITIVITY = 1.4F;
    private static final float DEFAULT_PITCH = 15.0F;
    private static final float MIN_PITCH = -45.0F;
    private static final float MAX_PITCH = 60.0F;

    private static final String KEY_TITLE = "screen.combat_styles.pose_editor";
    private static final String KEY_TITLE_STACK = "screen.combat_styles.pose_editor_stack";
    private static final String KEY_SCOPE_STACK = "screen.combat_styles.pose_scope_stack";
    private static final String KEY_SCOPE_EQUIPMENT = "screen.combat_styles.pose_scope_equipment";
    private static final String KEY_INHERITED = "screen.combat_styles.pose_inherited";
    private static final String KEY_MAIN = "screen.combat_styles.pose_main";
    private static final String KEY_OFF = "screen.combat_styles.pose_off_hand";
    private static final String KEY_BONE = "screen.combat_styles.pose_bone";
    private static final String KEY_ROTATION = "screen.combat_styles.pose_rotation";
    private static final String KEY_OFFSET = "screen.combat_styles.pose_offset";
    private static final String KEY_CLEAR = "screen.combat_styles.pose_clear";
    private static final String KEY_REMOVE = "screen.combat_styles.pose_remove";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_APPLY = "screen.combat_styles.apply";
    private static final String KEY_HINT = "screen.combat_styles.pose_hint";

    private final Saved onSaved;
    private final boolean perStack;
    private final AnimationPreview preview = new AnimationPreview();

    private Optional<String> pose;
    private Optional<String> offHandPose;
    private PoseTuning tuning;
    private String selectedBone = PoseTuning.BONE_RIGHT_ITEM;

    private EquipmentSlot slot = EquipmentSlot.MAINHAND;
    private float modelYaw;
    private float modelPitch = DEFAULT_PITCH;
    private boolean draggingModel;

    private EditBox pitchBox;
    private EditBox yawBox;
    private EditBox rollBox;
    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;
    private boolean syncing;

    public PoseEditorScreen(Screen parent, Optional<String> pose, Optional<String> offHandPose,
                            PoseTuning tuning, Saved onSaved) {
        this(parent, pose, offHandPose, tuning, onSaved, false);
    }

    private PoseEditorScreen(Screen parent, Optional<String> pose, Optional<String> offHandPose,
                             PoseTuning tuning, Saved onSaved, boolean perStack) {
        super(Component.translatable(KEY_TITLE), parent);
        this.pose = pose;
        this.offHandPose = offHandPose;
        this.tuning = tuning;
        this.onSaved = onSaved;
        this.perStack = perStack;
    }

    public static PoseEditorScreen forHeldItem(Screen parent) {
        return forHeldItem(parent, EquipmentSlot.MAINHAND);
    }

    public static PoseEditorScreen forHeldItem(Screen parent, EquipmentSlot slot) {
        Minecraft client = Minecraft.getInstance();
        StackPose held = client.player == null
                ? StackPose.NONE
                : EquipmentPoses.stackPoseOf(client.player.getItemBySlot(slot));
        PoseEditorScreen screen = new PoseEditorScreen(parent, held.pose(),
                held.offHandPose(), held.tuning(), null, true);
        screen.slot = slot;
        return screen;
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
    protected int rowCount() {
        return 0;
    }

    @Override
    protected void layout() {
        int fieldsY = top + BONE_ROW_Y + BONE_ROW_HEIGHT + GAP + ROW_HEIGHT;
        pitchBox = numberBox(fieldColumn(0), fieldsY);
        yawBox = numberBox(fieldColumn(1), fieldsY);
        rollBox = numberBox(fieldColumn(2), fieldsY);

        int offsetY = fieldsY + FIELD_HEIGHT + GAP + ROW_HEIGHT - 4;
        xBox = numberBox(fieldColumn(0), offsetY);
        yBox = numberBox(fieldColumn(1), offsetY);
        zBox = numberBox(fieldColumn(2), offsetY);

        int buttonY = top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - ScreenTheme.PADDING;
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLEAR),
                        button -> {
                            tuning = PoseTuning.NONE;
                            syncFields();
                            refreshPreview();
                        })
                .bounds(left + ScreenTheme.PADDING, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_REMOVE),
                        button -> removePose())
                .bounds(left + ScreenTheme.PADDING + BUTTON_WIDTH + GAP, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH * 2 - GAP,
                        buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_APPLY),
                        button -> apply())
                .bounds(left + PANEL_WIDTH - ScreenTheme.PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());

        syncFields();
        refreshPreview();
    }

    private int fieldColumn(int index) {
        return contentLeft() + LABEL_WIDTH + index * (FIELD_WIDTH + GAP);
    }

    private EditBox numberBox(int x, int y) {
        EditBox box = new FieldBox(font, x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
        box.setMaxLength(8);
        box.setResponder(text -> commitFields());
        addRenderableWidget(box);
        return box;
    }

    private void syncFields() {
        syncing = true;
        PoseTuning.BoneOffset offset = tuning.boneOf(selectedBone);
        pitchBox.setValue(StyleText.formatDecimal(offset.pitch()));
        yawBox.setValue(StyleText.formatDecimal(offset.yaw()));
        rollBox.setValue(StyleText.formatDecimal(offset.roll()));
        xBox.setValue(StyleText.formatDecimal(offset.x()));
        yBox.setValue(StyleText.formatDecimal(offset.y()));
        zBox.setValue(StyleText.formatDecimal(offset.z()));
        syncing = false;
    }

    private void commitFields() {
        if (syncing) {
            return;
        }
        PoseTuning.BoneOffset current = tuning.boneOf(selectedBone);
        PoseTuning.BoneOffset edited = new PoseTuning.BoneOffset(
                parse(pitchBox, current.pitch()),
                parse(yawBox, current.yaw()),
                parse(rollBox, current.roll()),
                parse(xBox, current.x()),
                parse(yBox, current.y()),
                parse(zBox, current.z()));
        tuning = tuning.withBone(selectedBone, edited);
        refreshPreview();
    }

    private static float parse(EditBox box, double fallback) {
        try {
            return Float.parseFloat(box.getValue().trim());
        } catch (NumberFormatException ignored) {
            return (float) fallback;
        }
    }

    private void refreshPreview() {
        preview.select(previewedPose(), tuning);
        preview.play();
    }

    private String previewedPose() {
        if (pose.isPresent() && !pose.get().isBlank()) {
            return pose.get();
        }
        Optional<String> inherited = inheritedPose(true);
        if (inherited.isPresent() && !inherited.get().isBlank()) {
            return inherited.get();
        }
        if (minecraft.player == null) {
            return PosePickerScreen.NONE;
        }
        Pose current = PlayerAttackHelper.poseForPlayer(minecraft.player);
        return current == null || current.base() == null || current.base().isBlank()
                ? PosePickerScreen.NONE
                : current.base();
    }

    private void apply() {
        if (perStack) {
            NetworkManager.sendApplyPose(new ApplyPosePayload(slot,
                    new StackPose(pose, offHandPose, tuning), false, false));
        }
        if (onSaved != null) {
            onSaved.accept(pose, offHandPose, tuning);
        }
        minecraft.setScreen(parent);
    }

    private void removePose() {
        if (perStack) {
            NetworkManager.sendApplyPose(new ApplyPosePayload(slot,
                    StackPose.NONE, false, true));
        }
        pose = Optional.empty();
        offHandPose = Optional.empty();
        tuning = PoseTuning.NONE;
        if (onSaved != null) {
            onSaved.accept(pose, offHandPose, tuning);
        }
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        drawPanel(graphics);
        drawTitle(graphics, Component.translatable(perStack ? KEY_TITLE_STACK : KEY_TITLE),
                top + TITLE_Y);
        Component scope = Component.translatable(
                perStack ? KEY_SCOPE_STACK : KEY_SCOPE_EQUIPMENT);
        graphics.drawString(font, scope,
                contentRight() - font.width(scope), top + TITLE_Y, ScreenTheme.MUTED, false);

        poseRow(graphics, mouseX, mouseY, KEY_MAIN, pose, top + POSE_Y, true);
        poseRow(graphics, mouseX, mouseY, KEY_OFF, offHandPose,
                top + POSE_Y + ROW_HEIGHT + 2, false);

        graphics.drawString(font, Component.translatable(KEY_BONE),
                contentLeft(), top + BONE_ROW_Y - 10, ScreenTheme.MUTED, false);
        for (int index = 0; index < PoseTuning.BONES.size(); index++) {
            String bone = PoseTuning.BONES.get(index);
            int x = boneX(index);
            int y = top + BONE_ROW_Y + (index / 3) * BONE_ROW_HEIGHT;
            boolean active = bone.equals(selectedBone);
            boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, x, y, boneWidth(), ROW_HEIGHT - 2);
            ScreenDraw.rowBackground(graphics, x, y, boneWidth(), ROW_HEIGHT - 2,
                    index, hovered, active);
            Component label = Component.translatable(boneKey(bone));
            ScreenDraw.drawTrimmed(graphics, font, label.getString(),
                    x + TEXT_INSET, y + 4, boneWidth() - 8, active ? ScreenTheme.HEADING
                            : tuning.boneOf(bone).isEmpty() ? ScreenTheme.MUTED : ScreenTheme.TEXT);
        }

        int fieldsY = top + BONE_ROW_Y + BONE_ROW_HEIGHT + GAP + ROW_HEIGHT;
        graphics.drawString(font, Component.translatable(KEY_ROTATION),
                contentLeft(), fieldsY + 2, ScreenTheme.MUTED, false);
        int offsetY = fieldsY + FIELD_HEIGHT + GAP + ROW_HEIGHT - 4;
        graphics.drawString(font, Component.translatable(KEY_OFFSET),
                contentLeft(), offsetY + 2, ScreenTheme.MUTED, false);
        for (int index = 0; index < 3; index++) {
            ScreenDraw.sunken(graphics, fieldColumn(index) - 2, fieldsY - 1,
                    FIELD_WIDTH + 4, FIELD_HEIGHT + 2);
            ScreenDraw.sunken(graphics, fieldColumn(index) - 2, offsetY - 1,
                    FIELD_WIDTH + 4, FIELD_HEIGHT + 2);
        }

        graphics.drawString(font, Component.translatable(KEY_HINT),
                contentLeft(), offsetY + FIELD_HEIGHT + GAP + 2, ScreenTheme.DIM, false);

        renderPreview(graphics);
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPreview(GuiGraphics graphics) {
        if (!preview.isAvailable()) {
            return;
        }
        int panelX = previewX();
        int panelY = previewY();
        ScreenDraw.panel(graphics, panelX, panelY, PREVIEW_SIZE, PREVIEW_SIZE);
        PreviewRenderer.render(graphics,
                panelX + PREVIEW_SIZE / 2.0F,
                panelY + PREVIEW_SIZE - 6.0F,
                (PREVIEW_SIZE - 8) / 2.0F,
                preview, modelYaw, modelPitch);
    }

    private int previewX() {
        return contentRight() - PREVIEW_SIZE;
    }

    private int previewY() {
        return top + BONE_ROW_Y - PREVIEW_GAP;
    }

    private Optional<String> inheritedPose(boolean main) {
        if (!perStack || minecraft.player == null) {
            return Optional.empty();
        }
        StackPose equipment = EquipmentPoses.equipmentPoseOf(
                minecraft.player.getItemBySlot(slot));
        return main ? equipment.pose() : equipment.offHandPose();
    }

    private void poseRow(GuiGraphics graphics, int mouseX, int mouseY,
                         String labelKey, Optional<String> value, int y, boolean main) {
        boolean hovered = ScreenDraw.isWithin(mouseX, mouseY, contentLeft(), y,
                poseRowWidth(), ROW_HEIGHT - 2);
        ScreenDraw.rowBackground(graphics, contentLeft(), y, poseRowWidth(),
                ROW_HEIGHT - 2, 0, hovered, false);
        graphics.drawString(font, Component.translatable(labelKey),
                contentLeft() + TEXT_INSET, y + 4, ScreenTheme.TEXT, false);
        Component shown;
        int colour;
        if (value.isPresent() && !value.get().isBlank()) {
            shown = PosePickerScreen.label(value.get());
            colour = ScreenTheme.MUTED;
        } else {
            Optional<String> inherited = inheritedPose(main);
            shown = inherited.isPresent()
                    ? Component.translatable(KEY_INHERITED,
                            PosePickerScreen.label(inherited.get()).getString())
                    : PosePickerScreen.label(null);
            colour = ScreenTheme.DIM;
        }
        graphics.drawString(font, shown,
                contentLeft() + poseRowWidth() - TEXT_INSET - font.width(shown),
                y + 4, colour, false);
    }

    private int poseRowWidth() {
        return contentWidth() - PREVIEW_SIZE - PREVIEW_GAP;
    }

    private int boneWidth() {
        return (poseRowWidth() - GAP * 2) / 3;
    }

    private int boneX(int index) {
        return contentLeft() + (index % 3) * (boneWidth() + GAP);
    }

    private static String boneKey(String bone) {
        return "screen.combat_styles.pose_bone_" + bone;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, previewX(), previewY(),
                PREVIEW_SIZE, PREVIEW_SIZE)) {
            draggingModel = true;
            return true;
        }
        int mainY = top + POSE_Y;
        if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, contentLeft(), mainY,
                poseRowWidth(), ROW_HEIGHT - 2)) {
            minecraft.setScreen(new PosePickerScreen(this, picked -> {
                pose = picked == null || picked.isBlank()
                        ? Optional.empty()
                        : Optional.of(picked);
                refreshPreview();
            }));
            return true;
        }
        int offY = mainY + ROW_HEIGHT + 2;
        if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, contentLeft(), offY,
                poseRowWidth(), ROW_HEIGHT - 2)) {
            minecraft.setScreen(new PosePickerScreen(this, picked -> offHandPose =
                    picked == null || picked.isBlank()
                            ? Optional.empty()
                            : Optional.of(picked)));
            return true;
        }
        for (int index = 0; index < PoseTuning.BONES.size(); index++) {
            int x = boneX(index);
            int y = top + BONE_ROW_Y + (index / 3) * BONE_ROW_HEIGHT;
            if (ScreenDraw.isWithin((int) mouseX, (int) mouseY, x, y,
                    boneWidth(), ROW_HEIGHT - 2)) {
                selectedBone = PoseTuning.BONES.get(index);
                syncFields();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (draggingModel) {
            modelYaw += (float) dragX * DRAG_SENSITIVITY;
            modelPitch = Math.clamp(modelPitch + (float) dragY * DRAG_SENSITIVITY,
                    MIN_PITCH, MAX_PITCH);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingModel = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        preview.tick();
    }
}
