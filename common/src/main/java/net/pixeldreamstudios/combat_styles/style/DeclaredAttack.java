package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.fx.ParticlePlacement;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import net.pixeldreamstudios.combat_styles.affix.TrailPlacements;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record DeclaredAttack(String animation,
                             String hitbox,
                             double damageMultiplier,
                             double angle,
                             double upswing,
                             float rangeMultiplier,
                             float movementSpeedMultiplier,
                             Optional<String> swingSound,
                             Optional<List<ParticlePlacement>> trail,
                             List<SlashPlacement> slashes) {

    public static final String DEFAULT_HITBOX = "horizontal_plane";
    private static final double DEFAULT_DAMAGE = 1.0D;
    private static final double DEFAULT_ANGLE = 90.0D;
    private static final double DEFAULT_UPSWING = 0.5D;
    private static final float DEFAULT_RANGE = 1.0F;
    private static final float DEFAULT_MOVEMENT = 1.0F;

    private static final Codec<ParticlePlacement> TRAIL_CODEC =
            RecordCodecBuilder.create(entry -> entry.group(
                    Codec.STRING.optionalFieldOf("particle_type", "")
                            .forGetter(ParticlePlacement::particle_type),
                    Codec.FLOAT.optionalFieldOf("x_addition", 0.0F)
                            .forGetter(ParticlePlacement::x_addition),
                    Codec.FLOAT.optionalFieldOf("y_addition", 0.0F)
                            .forGetter(ParticlePlacement::y_addition),
                    Codec.FLOAT.optionalFieldOf("z_addition", 0.0F)
                            .forGetter(ParticlePlacement::z_addition),
                    Codec.FLOAT.optionalFieldOf("local_yaw", 0.0F)
                            .forGetter(ParticlePlacement::local_yaw),
                    Codec.FLOAT.optionalFieldOf("pitch_addition", 0.0F)
                            .forGetter(ParticlePlacement::pitch_addition),
                    Codec.FLOAT.optionalFieldOf("roll_set", 0.0F)
                            .forGetter(ParticlePlacement::roll_set)
            ).apply(entry, ParticlePlacement::new));

    public static final Codec<DeclaredAttack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("animation").forGetter(DeclaredAttack::animation),
            Codec.STRING.optionalFieldOf("hitbox", DEFAULT_HITBOX).forGetter(DeclaredAttack::hitbox),
            Codec.DOUBLE.optionalFieldOf("damage_multiplier", DEFAULT_DAMAGE)
                    .forGetter(DeclaredAttack::damageMultiplier),
            Codec.DOUBLE.optionalFieldOf("angle", DEFAULT_ANGLE).forGetter(DeclaredAttack::angle),
            Codec.DOUBLE.optionalFieldOf("upswing", DEFAULT_UPSWING)
                    .forGetter(DeclaredAttack::upswing),
            Codec.FLOAT.optionalFieldOf("range_multiplier", DEFAULT_RANGE)
                    .forGetter(DeclaredAttack::rangeMultiplier),
            Codec.FLOAT.optionalFieldOf("movement_speed_multiplier", DEFAULT_MOVEMENT)
                    .forGetter(DeclaredAttack::movementSpeedMultiplier),
            Codec.STRING.optionalFieldOf("swing_sound").forGetter(DeclaredAttack::swingSound),
            TRAIL_CODEC.listOf().optionalFieldOf("trail").forGetter(DeclaredAttack::trail),
            SlashPlacement.CODEC.listOf().optionalFieldOf("slashes", List.of())
                    .forGetter(DeclaredAttack::slashes)
    ).apply(instance, DeclaredAttack::new));

    public DeclaredAttack {
        slashes = slashes == null ? List.of() : List.copyOf(slashes);
    }

    public DeclaredAttack(String animation, String hitbox, double damageMultiplier,
                          double angle, double upswing, float rangeMultiplier,
                          float movementSpeedMultiplier, Optional<String> swingSound,
                          Optional<List<ParticlePlacement>> trail) {
        this(animation, hitbox, damageMultiplier, angle, upswing, rangeMultiplier,
                movementSpeedMultiplier, swingSound, trail, List.of());
    }

    public static WeaponAttributes.Attack neutral() {
        return new DeclaredAttack("", DEFAULT_HITBOX, DEFAULT_DAMAGE, DEFAULT_ANGLE,
                DEFAULT_UPSWING, DEFAULT_RANGE, DEFAULT_MOVEMENT,
                Optional.empty(), Optional.of(List.of())).toAttack();
    }

    public static final StreamCodec<ByteBuf, DeclaredAttack> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC);

    public WeaponAttributes.HitBoxShape shape() {
        return switch (hitbox.toLowerCase(Locale.ROOT)) {
            case "forward_box" -> WeaponAttributes.HitBoxShape.FORWARD_BOX;
            case "vertical_plane" -> WeaponAttributes.HitBoxShape.VERTICAL_PLANE;
            default -> WeaponAttributes.HitBoxShape.HORIZONTAL_PLANE;
        };
    }

    private List<ParticlePlacement> trailFor(String animationId) {
        if (trail.isPresent() || !slashes.isEmpty()) {
            return trail.orElse(List.of());
        }
        List<ParticlePlacement> inherited = TrailPlacements.sequence(animationId);
        return inherited == null ? List.of() : inherited;
    }

    public WeaponAttributes.Attack toAttack() {
        WeaponAttributes.Sound sound = swingSound()
                .map(WeaponAttributes.Sound::new)
                .orElse(null);
        return new WeaponAttributes.Attack(
                null,
                shape(),
                damageMultiplier,
                movementSpeedMultiplier,
                rangeMultiplier,
                angle,
                upswing,
                animation,
                sound,
                null,
                trailFor(animation));
    }
}
