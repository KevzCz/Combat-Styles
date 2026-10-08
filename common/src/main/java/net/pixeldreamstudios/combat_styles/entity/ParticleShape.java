package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.Locale;

public record ParticleShape(Form form, float radius, int points, float height, float turns,
                            float yOffset, boolean grow) {

    public static final String KEY = "particle_shape";
    public static final String KEY_FORM = "form";
    public static final String KEY_RADIUS = "radius";
    public static final String KEY_POINTS = "points";
    public static final String KEY_HEIGHT = "height";
    public static final String KEY_TURNS = "turns";
    public static final String KEY_Y_OFFSET = "y_offset";
    public static final String KEY_GROW = "grow";
    public static final float FROM_TUNING = 0.0F;
    public static final int POINTS_FROM_TUNING = 0;
    public static final float DEFAULT_HEIGHT = 2.0F;
    public static final float DEFAULT_TURNS = 2.0F;
    public static final float NO_OFFSET = 0.0F;
    private static final double FULL_TURN = Math.PI * 2.0D;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0D - Math.sqrt(5.0D));
    private static final double STILL = 0.0D;
    private static final int SINGLE = 1;
    private static final int MIN_POINTS = 2;
    private static final double MIN_RADIUS = 0.1D;
    private static final double FULL_PROGRESS = 1.0D;
    private static final double SPHERE_SPAN = 2.0D;
    private static final double CLOUD_SPEED = 0.05D;

    public enum Form implements StringRepresentable {
        CLOUD,
        RING,
        SPHERE,
        LINE,
        SPIRAL,
        COLUMN;

        public static final Codec<Form> CODEC = StringRepresentable.fromEnum(Form::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final ParticleShape DEFAULT = new ParticleShape(Form.CLOUD, FROM_TUNING,
            POINTS_FROM_TUNING, DEFAULT_HEIGHT, DEFAULT_TURNS, NO_OFFSET, false);

    public static final Codec<ParticleShape> CODEC = Customs.wrap(ParticleShape.class, RecordCodecBuilder.create(instance -> instance.group(
            Form.CODEC.optionalFieldOf(KEY_FORM, Form.CLOUD).forGetter(ParticleShape::form),
            Codec.FLOAT.optionalFieldOf(KEY_RADIUS, FROM_TUNING).forGetter(ParticleShape::radius),
            Codec.INT.optionalFieldOf(KEY_POINTS, POINTS_FROM_TUNING).forGetter(ParticleShape::points),
            Codec.FLOAT.optionalFieldOf(KEY_HEIGHT, DEFAULT_HEIGHT).forGetter(ParticleShape::height),
            Codec.FLOAT.optionalFieldOf(KEY_TURNS, DEFAULT_TURNS).forGetter(ParticleShape::turns),
            Codec.FLOAT.optionalFieldOf(KEY_Y_OFFSET, NO_OFFSET).forGetter(ParticleShape::yOffset),
            Codec.BOOL.optionalFieldOf(KEY_GROW, false).forGetter(ParticleShape::grow)
    ).apply(instance, ParticleShape::new)));

    public ParticleShape {
        form = form == null ? Form.CLOUD : form;
    }

    public void draw(ServerLevel level, ParticleOptions particle, Vec3 centre, Vec3 from,
                     double tunedRadius, int tunedPoints, double spread, double progress) {
        int count = Math.max(MIN_POINTS, points > POINTS_FROM_TUNING ? points : tunedPoints);
        double base = radius > FROM_TUNING ? radius : Math.max(MIN_RADIUS, tunedRadius);
        double size = grow ? base * Math.clamp(progress, STILL, FULL_PROGRESS) : base;
        Vec3 at = centre.add(STILL, yOffset, STILL);
        switch (form) {
            case CLOUD -> level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread,
                    CLOUD_SPEED);
            case RING -> {
                for (int index = 0; index < count; index++) {
                    double angle = FULL_TURN * index / count;
                    point(level, particle, at.add(Math.cos(angle) * size, STILL, Math.sin(angle) * size));
                }
            }
            case SPHERE -> {
                for (int index = 0; index < count; index++) {
                    double y = FULL_PROGRESS - (index / (double) (count - SINGLE)) * SPHERE_SPAN;
                    double ring = Math.sqrt(Math.max(STILL, FULL_PROGRESS - y * y));
                    double angle = GOLDEN_ANGLE * index;
                    point(level, particle, at.add(Math.cos(angle) * ring * size, y * size,
                            Math.sin(angle) * ring * size));
                }
            }
            case LINE -> {
                Vec3 start = from.add(STILL, yOffset, STILL);
                Vec3 step = at.subtract(start);
                for (int index = 0; index < count; index++) {
                    point(level, particle, start.add(step.scale(index / (double) (count - SINGLE))));
                }
            }
            case SPIRAL -> {
                for (int index = 0; index < count; index++) {
                    double along = index / (double) (count - SINGLE);
                    double angle = FULL_TURN * turns * along;
                    point(level, particle, at.add(Math.cos(angle) * size, height * along,
                            Math.sin(angle) * size));
                }
            }
            case COLUMN -> {
                for (int index = 0; index < count; index++) {
                    point(level, particle, at.add(STILL, height * index / (count - SINGLE), STILL));
                }
            }
        }
    }

    private static void point(ServerLevel level, ParticleOptions particle, Vec3 at) {
        level.sendParticles(particle, at.x, at.y, at.z, SINGLE, STILL, STILL, STILL, STILL);
    }
}
