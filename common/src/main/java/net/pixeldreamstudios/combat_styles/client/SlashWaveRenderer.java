package net.pixeldreamstudios.combat_styles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bettercombat.api.fx.ParticlePlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.TrailPlacements;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.entity.SlashWaveEntity;
import net.pixeldreamstudios.combat_styles.entity.WaveShape;

import java.util.ArrayList;
import java.util.List;

public class SlashWaveRenderer extends EntityRenderer<SlashWaveEntity> {
    private static final ResourceLocation TEXTURE = CombatStyles.id("textures/entity/slash_wave.png");
    private static final String TEXTURE_PREFIX = "textures/entity/";
    private static final String TEXTURE_SUFFIX = ".png";
    private static final int FULL_BRIGHT = 15728880;
    private static final float HALF = 0.5F;
    private static final float MODEL_OFFSET = 0.0F;
    private static final String BOTTOM_SUFFIX = "_bottom";
    private static final float LAYER_GAP = 0.002F;
    private static final float[] CROSS_ROLLS = {-45.0F, 45.0F};
    private static final float[] SINGLE_ROLL = {0.0F};

    public SlashWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SlashWaveEntity entity) {
        String custom = entity.textureId();
        if (custom == null || custom.isBlank()) {
            return TEXTURE;
        }
        ResourceLocation id = ResourceLocation.tryParse(custom);
        if (id == null) {
            return TEXTURE;
        }
        return id.getPath().endsWith(TEXTURE_SUFFIX)
                ? id
                : ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                        TEXTURE_PREFIX + id.getPath() + TEXTURE_SUFFIX);
    }

    @Override
    public void render(SlashWaveEntity entity, float yaw, float partialTick,
                       PoseStack poses, MultiBufferSource buffers, int light) {
        poses.pushPose();

        float entityYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());

        int colour = entity.color();
        float alpha = ((colour >> 24) & 0xFF) / 255.0F;
        float red = ((colour >> 16) & 0xFF) / 255.0F;
        float green = ((colour >> 8) & 0xFF) / 255.0F;
        float blue = (colour & 0xFF) / 255.0F;

        ResourceLocation texture = getTextureLocation(entity);
        ResourceLocation bottom = bottomLayerOf(texture);

        int frames = entity.frameCount();
        float age = (entity.tickCount + partialTick) / Math.max(1.0F, entity.lifespan());
        float position = Math.clamp(age, 0.0F, 0.999F) * frames;

        int frame = Math.min((int) position, frames - 1);
        int next = Math.min(frame + 1, frames - 1);
        float blend = position - (int) position;

        boolean crossed = entity.shape() == WaveShape.FORWARD;
        CombatStylesConfig config = CombatStylesConfig.get();
        float renderWidth = entity.waveWidth() * (float) (crossed
                ? config.thrustWaveScale()
                : config.slashWaveScale());

        List<ParticlePlacement> sequence = TrailPlacements.sequence(entity.animation());
        for (Placement placement : placementsFor(entity, sequence, crossed)) {
            Matrix4f matrix = new Matrix4f();
            matrix.identity();
            matrix.rotate(-entityYaw * Mth.DEG_TO_RAD, new Vector3f(0.0F, 1.0F, 0.0F));
            matrix.rotate(entity.getXRot() * Mth.DEG_TO_RAD, new Vector3f(1.0F, 0.0F, 0.0F));
            matrix.rotate(placement.roll() * Mth.DEG_TO_RAD, new Vector3f(0.0F, 0.0F, 1.0F));
            matrix.rotate(-placement.yaw() * Mth.DEG_TO_RAD, new Vector3f(0.0F, 1.0F, 0.0F));
            matrix.rotate(placement.pitch() * Mth.DEG_TO_RAD, new Vector3f(1.0F, 0.0F, 0.0F));
            matrix.scale(renderWidth);

            Matrix4f pose = new Matrix4f(poses.last().pose()).mul(matrix);
            if (bottom != null) {
                layer(buffers, bottom, pose, -LAYER_GAP, frames, frame, next, blend,
                        red, green, blue, alpha);
            }
            layer(buffers, texture, pose, bottom == null ? 0.0F : LAYER_GAP,
                    frames, frame, next, blend, red, green, blue, alpha);
        }

        poses.popPose();
        super.render(entity, yaw, partialTick, poses, buffers, light);
    }

    private record Placement(float roll, float yaw, float pitch, String particleType) {
    }

    private List<Placement> placementsFor(SlashWaveEntity entity,
                                          List<ParticlePlacement> sequence, boolean crossed) {
        float[] rolls = crossed ? CROSS_ROLLS : SINGLE_ROLL;
        if (!sequence.isEmpty()) {
            List<Placement> placements = new ArrayList<>(sequence.size() * rolls.length);
            for (ParticlePlacement placement : sequence) {
                for (float rollOffset : rolls) {
                    placements.add(new Placement(placement.roll_set() + rollOffset,
                            placement.local_yaw(), placement.pitch_addition(),
                            placement.particle_type()));
                }
            }
            return placements;
        }
        List<Placement> placements = new ArrayList<>(rolls.length);
        for (float rollOffset : rolls) {
            placements.add(new Placement(entity.roll() + rollOffset, entity.localYaw(),
                    0.0F, ""));
        }
        return placements;
    }

        private ResourceLocation bottomLayerOf(ResourceLocation texture) {
        String path = texture.getPath();
        if (!path.endsWith(TEXTURE_SUFFIX)) {
            return null;
        }
        String base = path.substring(0, path.length() - TEXTURE_SUFFIX.length());
        if (base.endsWith(BOTTOM_SUFFIX)) {
            return null;
        }
        ResourceLocation bottom = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(),
                base + BOTTOM_SUFFIX + TEXTURE_SUFFIX);
        return Minecraft.getInstance().getResourceManager().getResource(bottom).isPresent()
                ? bottom
                : null;
    }

    private void layer(MultiBufferSource buffers, ResourceLocation texture, Matrix4f pose,
                       float offset, int frames, int frame, int next, float blend,
                       float red, float green, float blue, float alpha) {
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(texture));

        quad(buffer, pose, offset, frame / (float) frames, (frame + 1) / (float) frames,
                red, green, blue, alpha);
        if (blend > 0.0F && next != frame) {
            quad(buffer, pose, offset, next / (float) frames, (next + 1) / (float) frames,
                    red, green, blue, alpha * blend);
        }
    }

    private void quad(VertexConsumer buffer, Matrix4f pose, float offset, float v0, float v1,
                      float red, float green, float blue, float alpha) {
        vertex(buffer, pose, -HALF, -HALF, offset, 1.0F, v1, red, green, blue, alpha, 1.0F);
        vertex(buffer, pose, -HALF, HALF, offset, 1.0F, v0, red, green, blue, alpha, 1.0F);
        vertex(buffer, pose, HALF, HALF, offset, 0.0F, v0, red, green, blue, alpha, 1.0F);
        vertex(buffer, pose, HALF, -HALF, offset, 0.0F, v1, red, green, blue, alpha, 1.0F);

        vertex(buffer, pose, HALF, -HALF, offset, 0.0F, v1, red, green, blue, alpha, -1.0F);
        vertex(buffer, pose, HALF, HALF, offset, 0.0F, v0, red, green, blue, alpha, -1.0F);
        vertex(buffer, pose, -HALF, HALF, offset, 1.0F, v0, red, green, blue, alpha, -1.0F);
        vertex(buffer, pose, -HALF, -HALF, offset, 1.0F, v1, red, green, blue, alpha, -1.0F);
    }

    private void vertex(VertexConsumer buffer, Matrix4f pose, float x, float z, float offset,
                        float u, float v, float red, float green, float blue,
                        float alpha, float normal) {
        buffer.addVertex(pose, x, MODEL_OFFSET + offset, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(0.0F, normal, 0.0F);
    }
}
