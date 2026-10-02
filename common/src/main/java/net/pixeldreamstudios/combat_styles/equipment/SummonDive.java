package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.entity.SummonTargeting;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record SummonDive(float damageShare, int maxDiving, List<String> entities,
                         Optional<String> ability, SummonTargeting targeting,
                         Optional<AbilityTuning> tuning) {

    public static final float DEFAULT_SHARE = 1.0F;
    public static final int ALL_DIVING = 0;
    private static final float SAMPLE_SHARE = 0.5F;
    public static final AbilityTuning SAMPLE_TUNING =
            new AbilityTuning(AbilityTuning.Damage.NONE, AbilityTuning.Timing.NONE,
                    AbilityTuning.Shape.NONE, AbilityTuning.Presentation.NONE, Map.of());

    public static final Codec<SummonDive> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("damage_share", DEFAULT_SHARE)
                    .forGetter(SummonDive::damageShare),
            Codec.INT.optionalFieldOf("max_diving", ALL_DIVING)
                    .forGetter(SummonDive::maxDiving),
            Codec.STRING.listOf().optionalFieldOf("entities", List.of())
                    .forGetter(SummonDive::entities),
            Codec.STRING.optionalFieldOf("ability").forGetter(SummonDive::ability),
            SummonTargeting.CODEC.optionalFieldOf("targeting", SummonTargeting.ALL)
                    .forGetter(SummonDive::targeting),
            AbilityTuning.CODEC.optionalFieldOf("tuning").forGetter(SummonDive::tuning)
    ).apply(instance, SummonDive::new));

    public static final SummonDive SAMPLE =
            new SummonDive(SAMPLE_SHARE, 1, List.of(""),
                    Optional.of(""), SummonTargeting.SINGLE, Optional.of(SAMPLE_TUNING));

    public SummonDive {
        entities = entities == null ? List.of() : List.copyOf(entities);
        targeting = targeting == null ? SummonTargeting.ALL : targeting;
    }

    public boolean accepts(String entityId) {
        return entities.isEmpty() || entities.contains(entityId);
    }
}
