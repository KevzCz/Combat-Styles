package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.entity.SummonAim;
import net.pixeldreamstudios.combat_styles.entity.SummonTargeting;

import java.util.List;
import java.util.Optional;

public record SummonDive(float damageShare, int maxDiving, List<String> entities,
                         Optional<String> ability, SummonTargeting targeting,
                         Optional<AbilityTuning> tuning,
                         Optional<SummonAim> aim) {

    public static final float DEFAULT_SHARE = 1.0F;
    public static final int ALL_DIVING = 0;
    private static final float SAMPLE_SHARE = 0.5F;
    public static final AbilityTuning SAMPLE_TUNING = AbilityTuning.NONE;

    public static final Codec<SummonDive> CODEC = Customs.wrap(SummonDive.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("damage_share", DEFAULT_SHARE)
                    .forGetter(SummonDive::damageShare),
            Codec.INT.optionalFieldOf("max_diving", ALL_DIVING)
                    .forGetter(SummonDive::maxDiving),
            Codec.STRING.listOf().optionalFieldOf("entities", List.of())
                    .forGetter(SummonDive::entities),
            Codec.STRING.optionalFieldOf("ability").forGetter(SummonDive::ability),
            SummonTargeting.CODEC.optionalFieldOf("targeting", SummonTargeting.ALL)
                    .forGetter(SummonDive::targeting),
            AbilityTuning.CODEC.optionalFieldOf("tuning").forGetter(SummonDive::tuning),
            SummonAim.CODEC.optionalFieldOf(SummonCadence.KEY_AIM).forGetter(SummonDive::aim)
    ).apply(instance, SummonDive::new)));

    public static final SummonDive SAMPLE =
            new SummonDive(SAMPLE_SHARE, 1, List.of(""),
                    Optional.of(""), SummonTargeting.SINGLE, Optional.of(SAMPLE_TUNING),
                    Optional.empty());

    public SummonDive {
        entities = entities == null ? List.of() : List.copyOf(entities);
        targeting = targeting == null ? SummonTargeting.ALL : targeting;
        aim = aim == null ? Optional.empty() : aim;
    }

    public boolean accepts(String entityId) {
        return entities.isEmpty() || entities.contains(entityId);
    }
}
