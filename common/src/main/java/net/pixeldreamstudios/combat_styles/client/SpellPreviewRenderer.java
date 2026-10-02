package net.pixeldreamstudios.combat_styles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;

public final class SpellPreviewRenderer {

    private static final int LIGHT = 0xF000F0;
    private static final float TILT = 15.0F;
    private static final float BASE_SCALE = 0.55F;
    private static final float DEGREES = 180.0F;
    private static final float IDLE_SPIN = 2.0F;
    private static final long TICK_MILLIS = 50L;
    private static final float FULL_TURN = 360.0F;

    private SpellPreviewRenderer() {
    }

    public static boolean renderable(String spellId) {
        Level level = Minecraft.getInstance().level;
        ResourceLocation id = idOf(spellId);
        return id != null && level != null
                && SpellEngineCompat.hasProjectileModel(id, level);
    }

    public static void render(GuiGraphics graphics, int x, int y, int size, String spellId) {
        Level level = Minecraft.getInstance().level;
        ResourceLocation id = idOf(spellId);
        if (id == null || level == null || !SpellEngineCompat.hasProjectileModel(id, level)) {
            return;
        }
        float scale = SpellEngineCompat.projectileScaleOf(id, level);
        float spin = SpellEngineCompat.projectileSpinOf(id, level);

        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        PoseStack poses = graphics.pose();
        poses.pushPose();
        try {
            poses.translate(x + size / 2.0F, y + size / 2.0F, 100.0F);
            float drawn = size * BASE_SCALE * scale;
            poses.scale(drawn, -drawn, drawn);
            poses.mulPose(new Quaternionf().rotateX(TILT * ((float) Math.PI / DEGREES)));
            poses.mulPose(new Quaternionf()
                    .rotateY(angle(spin) * ((float) Math.PI / DEGREES)));

            SpellEngineCompat.renderProjectileModel(id, level, poses, buffers, LIGHT);
            buffers.endBatch();
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.debug("Spell preview render failed", throwable);
        } finally {
            poses.popPose();
        }
    }

    private static float angle(float spin) {
        double perTick = spin + IDLE_SPIN;
        double period = FULL_TURN / perTick * TICK_MILLIS;
        return (float) (System.currentTimeMillis() % (long) period / period * FULL_TURN);
    }

    private static ResourceLocation idOf(String spellId) {
        if (spellId == null || spellId.isBlank() || !SpellEngineCompat.isLoaded()) {
            return null;
        }
        return ResourceLocation.tryParse(spellId);
    }
}
