package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.AffixTargets;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SlashWaveEntity extends Entity {

    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROLL =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOCAL_YAW =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> TEXTURE =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> FRAMES =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHAPE =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ARC =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURVE_START =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURVE_TROUGH =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURVE_END =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CURVE_STALL =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CURVE_RECOVER =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> ANIMATION =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.STRING);

    private static final String TAG_DAMAGE = "Damage";
    private static final String TAG_LIFE = "Life";
    private static final String TAG_WIDTH = "Width";
    private static final String TAG_ROLL = "Roll";
    private static final String TAG_LOCAL_YAW = "LocalYaw";
    private static final String TAG_COLOR = "Color";
    private static final String TAG_AFFIX = "Affix";
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_TEXTURE = "Texture";
    private static final String TAG_FRAMES = "Frames";
    private static final String TAG_SHAPE = "Shape";
    private static final String TAG_ARC = "Arc";
    private static final String TAG_ANIMATION = "Animation";

    private static final int DEFAULT_LIFE = 20;
    private static final int DEFAULT_FRAMES = 10;

    public static final String THRUST_TEXTURE = CombatStyles.MOD_ID + ":thrust_wave";
    public static final int THRUST_FRAMES = 6;
    private static final float VERTICAL_THICKNESS = 0.7F;
    private static final float HEIGHT_RATIO = 0.5F;
    private static final float REACH_RATIO = 0.3F;

    private final Set<Entity> alreadyHit = new HashSet<>();
    private float damage;
    private Vec3 baseMotion;
    private int life = DEFAULT_LIFE;
    private String affixId = "";
    private String inheritedType = "";
    private UUID ownerId;
    private boolean pierces = true;

    public SlashWaveEntity(EntityType<? extends SlashWaveEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WIDTH, 2.0F);
        builder.define(ROLL, 0.0F);
        builder.define(LOCAL_YAW, 0.0F);
        builder.define(COLOR, 0xFFFFFFFF);
        builder.define(LIFE, DEFAULT_LIFE);
        builder.define(TEXTURE, "");
        builder.define(FRAMES, DEFAULT_FRAMES);
        builder.define(SHAPE, WaveShape.HORIZONTAL.ordinal());
        builder.define(ARC, 0.0F);
        builder.define(ANIMATION, "");
        builder.define(CURVE_START, 1.0F);
        builder.define(CURVE_TROUGH, 1.0F);
        builder.define(CURVE_END, 1.0F);
        builder.define(CURVE_STALL, 0);
        builder.define(CURVE_RECOVER, 0);
    }

    public void setPierces(boolean pierces) {
        this.pierces = pierces;
    }

    public void configure(float damage, float width, float roll, float localYaw,
                          int color, int life, String affixId) {
        entityData.set(LOCAL_YAW, localYaw);
        entityData.set(LIFE, life);
        this.damage = damage;
        this.life = life;
        this.affixId = affixId == null ? "" : affixId;
        entityData.set(WIDTH, width);
        entityData.set(ROLL, roll);
        entityData.set(COLOR, color);
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float width = entityData.get(WIDTH);
        WaveShape shape = shape();
        float span = Math.max(shape.widthRatio(), shape.depthRatio());
        return EntityDimensions.scalable(width * span, width * shape.heightRatio() * HEIGHT_RATIO);
    }

    public void setFacing(float yaw, float pitch) {
        setYRot(yaw);
        setXRot(pitch);
        yRotO = yaw;
        xRotO = pitch;
    }

    public void setOwner(Entity owner) {
        this.ownerId = owner == null ? null : owner.getUUID();
    }

    public Entity getOwner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getEntity(ownerId);
    }

    public float waveWidth() {
        return entityData.get(WIDTH);
    }

    public float roll() {
        return entityData.get(ROLL);
    }

    public float localYaw() {
        return entityData.get(LOCAL_YAW);
    }

    public int lifespan() {
        return entityData.get(LIFE);
    }

    public int color() {
        return entityData.get(COLOR);
    }

    public String textureId() {
        return entityData.get(TEXTURE);
    }

    public int frameCount() {
        return Math.max(1, entityData.get(FRAMES));
    }

    public WaveShape shape() {
        return WaveShape.ordinalOf(entityData.get(SHAPE));
    }

    public float arc() {
        return entityData.get(ARC);
    }

    public void setArc(float arc) {
        entityData.set(ARC, Math.abs(arc));
    }

    public String animation() {
        return entityData.get(ANIMATION);
    }

    public void setAnimation(String animation) {
        entityData.set(ANIMATION, animation == null ? "" : animation);
    }

    public void setAppearance(String texture, int frames, WaveShape shape) {
        entityData.set(TEXTURE, texture == null ? "" : texture);
        entityData.set(FRAMES, frames > 0 ? frames : DEFAULT_FRAMES);
        entityData.set(SHAPE, (shape == null ? WaveShape.HORIZONTAL : shape).ordinal());
        refreshDimensions();
    }

    public String affixId() {
        return affixId;
    }

    public String inheritedType() {
        return inheritedType;
    }

    public void setInheritedType(String type) {
        this.inheritedType = type == null ? "" : type;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        yRotO = getYRot();
        xRotO = getXRot();
    }

    @Override
    public void tick() {
        super.tick();

        yRotO = getYRot();
        xRotO = getXRot();

        if (baseMotion == null) {
            baseMotion = getDeltaMovement();
        }
        Vec3 motion = baseMotion.scale(speedMultiplier());
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (level().isClientSide()) {
            return;
        }

        if (ownerId != null && getOwner() == null) {
            discard();
            return;
        }

        sweep();
        if (tickCount > life) {
            discard();
        }
    }

    private float speedMultiplier() {
        int stall = entityData.get(CURVE_STALL);
        int recover = entityData.get(CURVE_RECOVER);
        if (stall <= 0 && recover <= 0) {
            return 1.0F;
        }
        return new AffixEffects.WaveSpeed(entityData.get(CURVE_START),
                entityData.get(CURVE_TROUGH), entityData.get(CURVE_END), stall, recover)
                .multiplierAt(tickCount);
    }

    public void setSpeedCurve(AffixEffects.WaveSpeed curve) {
        AffixEffects.WaveSpeed resolved = curve == null ? AffixEffects.WaveSpeed.FLAT : curve;
        entityData.set(CURVE_START, resolved.start());
        entityData.set(CURVE_TROUGH, resolved.trough());
        entityData.set(CURVE_END, resolved.end());
        entityData.set(CURVE_STALL, resolved.stallTicks());
        entityData.set(CURVE_RECOVER, resolved.recoverTicks());
    }

    private void sweep() {
        WaveShape shape = shape();
        float reach = waveWidth() * REACH_RATIO;
        float halfWidth = reach * shape.widthRatio();
        float halfHeight = Math.max(VERTICAL_THICKNESS * shape.heightRatio(),
                reach * shape.heightRatio());
        float halfDepth = reach * shape.depthRatio();

        Vec3 motion = getDeltaMovement();
        AABB broad = getBoundingBox()
                .inflate(Math.max(halfWidth, halfDepth), halfHeight, Math.max(halfWidth, halfDepth))
                .expandTowards(motion.x, motion.y, motion.z);

        double facing = Math.toRadians(getYRot());
        double sin = Math.sin(facing);
        double cos = Math.cos(facing);
        Vec3 centre = position();

        Entity owner = getOwner();
        for (Entity entity : level().getEntities(this, broad, candidate ->
                candidate instanceof LivingEntity
                        && candidate.isAlive()
                        && candidate != owner
                        && (!(owner instanceof LivingEntity shooter)
                                || AffixTargets.canHitFrom(shooter, candidate)))) {
            if (!withinShape(entity, centre, sin, cos, halfWidth, halfHeight, halfDepth)) {
                continue;
            }
            if (alreadyHit.add(entity) && entity instanceof LivingEntity living) {
                SlashWaveDamage.apply(this, living, damage);
                if (!pierces) {
                    discard();
                    return;
                }
            }
        }
    }

    private boolean withinShape(Entity entity, Vec3 centre, double sin, double cos,
                                float halfWidth, float halfHeight, float halfDepth) {
        AABB box = entity.getBoundingBox();
        double dx = Mth.clamp(centre.x, box.minX, box.maxX) - centre.x;
        double dy = Mth.clamp(centre.y, box.minY, box.maxY) - centre.y;
        double dz = Mth.clamp(centre.z, box.minZ, box.maxZ) - centre.z;

        double localX = dx * cos - dz * sin;
        double localZ = dx * sin + dz * cos;

        return Math.abs(localX) <= halfWidth
                && Math.abs(dy) <= halfHeight
                && Math.abs(localZ) <= halfDepth;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat(TAG_DAMAGE, damage);
        tag.putInt(TAG_LIFE, life);
        tag.putFloat(TAG_WIDTH, waveWidth());
        tag.putFloat(TAG_ROLL, roll());
        tag.putFloat(TAG_LOCAL_YAW, localYaw());
        tag.putInt(TAG_COLOR, color());
        tag.putString(TAG_AFFIX, affixId);
        tag.putString(TAG_TEXTURE, textureId());
        tag.putInt(TAG_FRAMES, frameCount());
        tag.putInt(TAG_SHAPE, entityData.get(SHAPE));
        tag.putFloat(TAG_ARC, entityData.get(ARC));
        tag.putString(TAG_ANIMATION, entityData.get(ANIMATION));
        if (ownerId != null) {
            tag.putUUID(TAG_OWNER, ownerId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        damage = tag.getFloat(TAG_DAMAGE);
        life = tag.contains(TAG_LIFE) ? tag.getInt(TAG_LIFE) : DEFAULT_LIFE;
        entityData.set(LIFE, life);
        affixId = tag.getString(TAG_AFFIX);
        ownerId = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
        entityData.set(WIDTH, tag.getFloat(TAG_WIDTH));
        entityData.set(ROLL, tag.getFloat(TAG_ROLL));
        entityData.set(LOCAL_YAW, tag.getFloat(TAG_LOCAL_YAW));
        entityData.set(COLOR, tag.getInt(TAG_COLOR));
        entityData.set(TEXTURE, tag.getString(TAG_TEXTURE));
        entityData.set(FRAMES, tag.contains(TAG_FRAMES) ? tag.getInt(TAG_FRAMES) : DEFAULT_FRAMES);
        entityData.set(SHAPE, tag.getInt(TAG_SHAPE));
        entityData.set(ARC, tag.getFloat(TAG_ARC));
        entityData.set(ANIMATION, tag.getString(TAG_ANIMATION));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
