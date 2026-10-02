package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record SpellTuning(float powerMultiplier, float criticalChanceBonus,
                          float criticalDamageBonus, int ricochet, int pierce, int bounce,
                          float rangeAdd, int channelTicksAdd, float effectDurationAdd,
                          int effectAmplifierAdd, List<String> impactSchools,
                          List<String> impactTypes, float ricochetRange,
                          float knockbackMultiply, SpellLaunchTuning launch,
                          SpellChainTuning chainReaction, float cooldownReduction) {

    public static final float NO_MULTIPLIER = 1.0F;

    public static final SpellTuning NONE = new SpellTuning(NO_MULTIPLIER, 0.0F, 0.0F,
            0, 0, 0, 0.0F, 0, 0.0F, 0, List.of(), List.of(), 0.0F, 0.0F,
            SpellLaunchTuning.NONE, SpellChainTuning.NONE, 0.0F);

    private record Power(float powerMultiplier, float criticalChanceBonus,
                         float criticalDamageBonus, int ricochet, int pierce, int bounce,
                         float rangeAdd, int channelTicksAdd) {
    }

    private record Reach(float effectDurationAdd, int effectAmplifierAdd,
                         List<String> impactSchools, List<String> impactTypes,
                         float ricochetRange, float knockbackMultiply,
                         SpellLaunchTuning launch, SpellChainTuning chainReaction,
                         float cooldownReduction) {
    }

    private static final MapCodec<Power> POWER_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.FLOAT.optionalFieldOf("power_multiplier", NO_MULTIPLIER)
                            .forGetter(Power::powerMultiplier),
                    Codec.FLOAT.optionalFieldOf("critical_chance_bonus", 0.0F)
                            .forGetter(Power::criticalChanceBonus),
                    Codec.FLOAT.optionalFieldOf("critical_damage_bonus", 0.0F)
                            .forGetter(Power::criticalDamageBonus),
                    Codec.INT.optionalFieldOf("ricochet", 0).forGetter(Power::ricochet),
                    Codec.INT.optionalFieldOf("pierce", 0).forGetter(Power::pierce),
                    Codec.INT.optionalFieldOf("bounce", 0).forGetter(Power::bounce),
                    Codec.FLOAT.optionalFieldOf("range_add", 0.0F).forGetter(Power::rangeAdd),
                    Codec.INT.optionalFieldOf("channel_ticks_add", 0)
                            .forGetter(Power::channelTicksAdd)
            ).apply(instance, Power::new));

    private static final MapCodec<Reach> REACH_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.FLOAT.optionalFieldOf("effect_duration_add", 0.0F)
                            .forGetter(Reach::effectDurationAdd),
                    Codec.INT.optionalFieldOf("effect_amplifier_add", 0)
                            .forGetter(Reach::effectAmplifierAdd),
                    Codec.STRING.listOf().optionalFieldOf("impact_school", List.of())
                            .forGetter(Reach::impactSchools),
                    Codec.STRING.listOf().optionalFieldOf("impact_type", List.of())
                            .forGetter(Reach::impactTypes),
                    Codec.FLOAT.optionalFieldOf("ricochet_range", 0.0F)
                            .forGetter(Reach::ricochetRange),
                    Codec.FLOAT.optionalFieldOf("knockback_multiply", 0.0F)
                            .forGetter(Reach::knockbackMultiply),
                    SpellLaunchTuning.CODEC.optionalFieldOf("launch", SpellLaunchTuning.NONE)
                            .forGetter(Reach::launch),
                    SpellChainTuning.CODEC
                            .optionalFieldOf("chain_reaction", SpellChainTuning.NONE)
                            .forGetter(Reach::chainReaction),
                    Codec.FLOAT.optionalFieldOf("cooldown_reduction", 0.0F)
                            .forGetter(Reach::cooldownReduction)
            ).apply(instance, Reach::new));

    public static final MapCodec<SpellTuning> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    POWER_CODEC.forGetter(tuning -> new Power(tuning.powerMultiplier(),
                            tuning.criticalChanceBonus(), tuning.criticalDamageBonus(),
                            tuning.ricochet(), tuning.pierce(), tuning.bounce(),
                            tuning.rangeAdd(), tuning.channelTicksAdd())),
                    REACH_CODEC.forGetter(tuning -> new Reach(tuning.effectDurationAdd(),
                            tuning.effectAmplifierAdd(), tuning.impactSchools(),
                            tuning.impactTypes(), tuning.ricochetRange(),
                            tuning.knockbackMultiply(), tuning.launch(),
                            tuning.chainReaction(), tuning.cooldownReduction()))
            ).apply(instance, (power, reach) -> new SpellTuning(power.powerMultiplier(),
                    power.criticalChanceBonus(), power.criticalDamageBonus(), power.ricochet(),
                    power.pierce(), power.bounce(), power.rangeAdd(), power.channelTicksAdd(),
                    reach.effectDurationAdd(), reach.effectAmplifierAdd(),
                    reach.impactSchools(), reach.impactTypes(), reach.ricochetRange(),
                    reach.knockbackMultiply(), reach.launch(), reach.chainReaction(),
                    reach.cooldownReduction())));

    public static final Codec<SpellTuning> CODEC = MAP_CODEC.codec();

    public SpellTuning {
        impactSchools = impactSchools == null ? List.of() : List.copyOf(impactSchools);
        impactTypes = impactTypes == null ? List.of() : List.copyOf(impactTypes);
        launch = launch == null ? SpellLaunchTuning.NONE : launch;
        chainReaction = chainReaction == null ? SpellChainTuning.NONE : chainReaction;
    }

    public boolean isEmpty() {
        return powerMultiplier == NO_MULTIPLIER && criticalChanceBonus == 0.0F
                && criticalDamageBonus == 0.0F && !hasPerks() && rangeAdd == 0.0F
                && channelTicksAdd == 0 && effectDurationAdd == 0.0F
                && effectAmplifierAdd == 0 && knockbackMultiply == 0.0F
                && cooldownReduction == 0.0F && launch.isEmpty();
    }

    public boolean hasPerks() {
        return ricochet != 0 || pierce != 0 || bounce != 0 || ricochetRange != 0.0F
                || !chainReaction.isEmpty();
    }

    public boolean hasPower() {
        return powerMultiplier != NO_MULTIPLIER || criticalChanceBonus != 0.0F
                || criticalDamageBonus != 0.0F;
    }

    public boolean filtersImpacts() {
        return !impactSchools.isEmpty() || !impactTypes.isEmpty();
    }
}
