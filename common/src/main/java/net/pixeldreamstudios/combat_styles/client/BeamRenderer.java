package net.pixeldreamstudios.combat_styles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BeamRenderer {

    private static final ResourceLocation BEAM =
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    private static final float SCROLL_SPEED = 0.06F;
    private static final float TILE_LENGTH = 2.0F;
    private static final float MIN_LENGTH = 1.0E-3F;
    private static final float PARALLEL = 0.9995F;
    private static final float FLIP_DEGREES = 180.0F;
    private static final float CHANNEL = 255.0F;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int CHANNEL_MASK = 0xFF;

    private BeamRenderer() {
    }

    public static void draw(PoseStack poses, MultiBufferSource buffers, Vec3 from, Vec3 to,
                            float width, int rgb, float alpha, float ageInTicks) {
        draw(poses, buffers, from, to, width,
                ((rgb >> RED_SHIFT) & CHANNEL_MASK) / CHANNEL,
                ((rgb >> GREEN_SHIFT) & CHANNEL_MASK) / CHANNEL,
                (rgb & CHANNEL_MASK) / CHANNEL, alpha, ageInTicks);
    }

    public static void draw(PoseStack poses, MultiBufferSource buffers, Vec3 from, Vec3 to,
                            float width, float red, float green, float blue, float alpha,
                            float ageInTicks) {
        Vec3 delta = to.subtract(from);
        float length = (float) delta.length();
        if (length < MIN_LENGTH || alpha <= 0.0F) {
            return;
        }

        poses.pushPose();
        poses.translate(from.x, from.y, from.z);

        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f dir = new Vector3f((float) delta.x, (float) delta.y, (float) delta.z).normalize();
        float dot = up.dot(dir);
        if (dot < -PARALLEL) {
            poses.mulPose(Axis.XP.rotationDegrees(FLIP_DEGREES));
        } else if (dot < PARALLEL) {
            Vector3f axis = new Vector3f(up).cross(dir).normalize();
            poses.mulPose(new Quaternionf().fromAxisAngleRad(axis, (float) Math.acos(dot)));
        }

        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(BEAM));
        Matrix4f pose = poses.last().pose();

        float scroll = -(ageInTicks * SCROLL_SPEED) % 1.0F;
        float v0 = scroll;
        float v1 = scroll + length / TILE_LENGTH;

        quad(buffer, pose, -width, width, 0.0F, 0.0F, length, v0, v1, red, green, blue, alpha);
        quad(buffer, pose, 0.0F, 0.0F, -width, width, length, v0, v1, red, green, blue, alpha);

        poses.popPose();
    }

    private static void quad(VertexConsumer buffer, Matrix4f pose,
                             float x0, float x1, float z0, float z1, float length,
                             float v0, float v1,
                             float red, float green, float blue, float alpha) {
        vertex(buffer, pose, x0, 0.0F, z0, 0.0F, v0, red, green, blue, alpha);
        vertex(buffer, pose, x0, length, z0, 0.0F, v1, red, green, blue, alpha);
        vertex(buffer, pose, x1, length, z1, 1.0F, v1, red, green, blue, alpha);
        vertex(buffer, pose, x1, 0.0F, z1, 1.0F, v0, red, green, blue, alpha);

        vertex(buffer, pose, x1, 0.0F, z1, 0.0F, v0, red, green, blue, alpha);
        vertex(buffer, pose, x1, length, z1, 0.0F, v1, red, green, blue, alpha);
        vertex(buffer, pose, x0, length, z0, 1.0F, v1, red, green, blue, alpha);
        vertex(buffer, pose, x0, 0.0F, z0, 1.0F, v0, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f pose, float x, float y, float z,
                               float u, float v,
                               float red, float green, float blue, float alpha) {
        buffer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, Mth.clamp(alpha, 0.0F, 1.0F))
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
