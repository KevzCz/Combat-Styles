package net.pixeldreamstudios.combat_styles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.HashMap;
import java.util.Map;

public final class EntityPreviewRenderer {

    private static final float ROTATION_X = 15.0F;
    private static final float FACING = 180.0F;
    private static final float LEVEL = 0.0F;
    private static final float MAX_SIZE = 2.0F;
    private static final float MIN_SIZE = 0.5F;
    private static final int MAX_CACHED = 64;
    private static final int LIGHT = 0xF000F0;
    private static final float FLAT_SCALE = 0.85F;
    private static final long TICK_MILLIS = 50L;
    private static final long SPIN_PERIOD_MILLIS = 9000L;
    private static final float FULL_TURN = 360.0F;

    private static final Map<ResourceLocation, Entity> CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> FAILED = new HashMap<>();

    private EntityPreviewRenderer() {
    }

    public static void clear() {
        CACHE.clear();
        FAILED.clear();
    }

    public static boolean renderable(String entityId) {
        return previewFor(entityId) != null;
    }

    public static void render(GuiGraphics graphics, int x, int y, int size, String entityId) {
        Entity entity = previewFor(entityId);
        if (entity == null) {
            return;
        }
        if (entity instanceof LivingEntity living) {
            render(graphics, x + size / 2.0F, y + size - 2.0F, scaleFor(entity, size), living);
            return;
        }
        renderFlat(graphics, x + size / 2.0F, y + size / 2.0F, scaleFor(entity, size), entity);
    }

    public static void render(GuiGraphics graphics, float centreX, float baseY, float scale,
                              LivingEntity entity) {
        float bodyRot = entity.yBodyRot;
        float bodyRotO = entity.yBodyRotO;
        float rotY = entity.getYRot();
        float rotYO = entity.yRotO;
        float rotX = entity.getXRot();
        float rotXO = entity.xRotO;
        float headRot = entity.yHeadRot;
        float headRotO = entity.yHeadRotO;
        try {
            faceViewer(entity);
            float spun = FACING + spin();
            entity.yBodyRot = spun;
            entity.yBodyRotO = spun;
            entity.setYRot(spun);
            entity.yRotO = spun;
            entity.yHeadRot = spun;
            entity.yHeadRotO = spun;

            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
            rotation.mul(new Quaternionf().rotateX(ROTATION_X * ((float) Math.PI / 180F)));

            InventoryScreen.renderEntityInInventory(graphics, centreX, baseY, (int) scale,
                    new Vector3f(), rotation, null, entity);
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.debug("Entity preview render failed", throwable);
        } finally {
            entity.yBodyRot = bodyRot;
            entity.yBodyRotO = bodyRotO;
            entity.setYRot(rotY);
            entity.yRotO = rotYO;
            entity.setXRot(rotX);
            entity.xRotO = rotXO;
            entity.yHeadRot = headRot;
            entity.yHeadRotO = headRotO;
        }
    }

    private static void renderFlat(GuiGraphics graphics, float centreX, float centreY,
                                   float scale, Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        float rotY = entity.getYRot();
        float rotYO = entity.yRotO;
        float rotX = entity.getXRot();
        float rotXO = entity.xRotO;
        PoseStack poses = graphics.pose();
        poses.pushPose();
        try {
            float spun = spin();
            entity.setYRot(spun);
            entity.yRotO = spun;
            entity.setXRot(LEVEL);
            entity.xRotO = LEVEL;
            animate(entity);

            poses.translate(centreX, centreY, 100.0F);
            poses.scale(scale * FLAT_SCALE, -scale * FLAT_SCALE, scale * FLAT_SCALE);
            poses.mulPose(new Quaternionf().rotateX(-ROTATION_X * ((float) Math.PI / 180F)));

            dispatcher.setRenderShadow(false);
            dispatcher.render(entity, 0.0D, 0.0D, 0.0D, spun, 1.0F, poses, buffers, LIGHT);
            dispatcher.setRenderShadow(true);
            buffers.endBatch();
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.debug("Entity preview render failed", throwable);
        } finally {
            poses.popPose();
            entity.setYRot(rotY);
            entity.yRotO = rotYO;
            entity.setXRot(rotX);
            entity.xRotO = rotXO;
        }
    }

    public static void faceViewer(LivingEntity entity) {
        entity.yBodyRot = FACING;
        entity.yBodyRotO = FACING;
        entity.setYRot(FACING);
        entity.yRotO = FACING;
        entity.setXRot(LEVEL);
        entity.xRotO = LEVEL;
        entity.yHeadRot = FACING;
        entity.yHeadRotO = FACING;
        animate(entity);
    }

    private static float spin() {
        return (float) ((System.currentTimeMillis() % SPIN_PERIOD_MILLIS)
                / (double) SPIN_PERIOD_MILLIS * FULL_TURN);
    }

    private static long ticks() {
        return System.currentTimeMillis() / TICK_MILLIS;
    }

    private static void animate(Entity entity) {
        entity.tickCount = (int) (ticks() % Integer.MAX_VALUE);
        if (entity instanceof LivingEntity living) {
            living.walkAnimation.setSpeed(0.0F);
        }
    }

    private static float scaleFor(Entity entity, int size) {
        float tallest = Math.max(entity.getBbHeight(), entity.getBbWidth());
        float clamped = Math.clamp(tallest, MIN_SIZE, MAX_SIZE);
        return size / clamped / 2.0F;
    }

    private static Entity previewFor(String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id == null || FAILED.containsKey(id)) {
            return null;
        }
        Entity cached = CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        Entity created = create(id);
        if (created == null) {
            FAILED.put(id, Boolean.TRUE);
            return null;
        }
        if (CACHE.size() >= MAX_CACHED) {
            CACHE.clear();
        }
        CACHE.put(id, created);
        return created;
    }

    private static Entity create(ResourceLocation id) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) {
            return null;
        }
        try {
            return type.create(level);
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.debug("Entity preview unavailable for {}", id, throwable);
            return null;
        }
    }
}
