package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public final class PreviewRenderer {

    private static final float ROTATION_X = 15.0F;
    private static final int SCALE_DIVISOR = 2;
    private static final float FACING = 180.0F;
    private static final float LEVEL = 0.0F;

    private PreviewRenderer() {
    }

    public static void render(GuiGraphics graphics, int x, int y, int size, AnimationPreview preview) {
        render(graphics, x + size / 2.0F, y + size - 4.0F, size / SCALE_DIVISOR,
                preview, 0.0F, ROTATION_X);
    }

    public static void render(GuiGraphics graphics, float centreX, float baseY, float scale,
                              AnimationPreview preview, float yaw, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        PreviewAnimationBinding.bind(player, preview);
        float bodyRot = player.yBodyRot;
        float bodyRotO = player.yBodyRotO;
        float rotY = player.getYRot();
        float rotYO = player.yRotO;
        float rotX = player.getXRot();
        float rotXO = player.xRotO;
        float headRot = player.yHeadRot;
        float headRotO = player.yHeadRotO;
        try {
            player.yBodyRot = FACING;
            player.yBodyRotO = FACING;
            player.setYRot(FACING);
            player.yRotO = FACING;
            player.setXRot(LEVEL);
            player.xRotO = LEVEL;
            player.yHeadRot = FACING;
            player.yHeadRotO = FACING;

            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf tilt = new Quaternionf().rotateX(pitch * ((float) Math.PI / 180F));
            rotation.mul(tilt);
            rotation.mul(new Quaternionf().rotateY(yaw * ((float) Math.PI / 180F)));

            InventoryScreen.renderEntityInInventory(
                    graphics,
                    centreX,
                    baseY,
                    (int) scale,
                    new Vector3f(),
                    rotation,
                    null,
                    player);
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.debug("Animation preview render failed", throwable);
        } finally {
            player.yBodyRot = bodyRot;
            player.yBodyRotO = bodyRotO;
            player.setYRot(rotY);
            player.yRotO = rotYO;
            player.setXRot(rotX);
            player.xRotO = rotXO;
            player.yHeadRot = headRot;
            player.yHeadRotO = headRotO;
            PreviewAnimationBinding.unbind(player, preview);
        }
    }
}
