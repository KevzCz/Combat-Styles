package net.pixeldreamstudios.combat_styles.equipment;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.config.OptionSources;
import net.pixeldreamstudios.combat_styles.entity.AbilityRegistry;
import net.pixeldreamstudios.combat_styles.entity.PassiveAbility;
import net.pixeldreamstudios.combat_styles.entity.PassiveTrigger;

import java.util.Optional;

public record SummonPassive(String ability, Optional<PassiveTrigger> on, Optional<Float> chance,
                            Optional<Integer> cooldownTicks, Optional<JsonObject> options,
                            Optional<AbilityTuning> tuning) {

    public static final String KEY_ABILITY = "ability";
    public static final String KEY_ON = "on";
    public static final String KEY_CHANCE = "chance";
    public static final String KEY_COOLDOWN_TICKS = "cooldown_ticks";
    public static final String KEY_OPTIONS = OptionSources.KEY_OPTIONS;
    public static final String KEY_TUNING = AbilityUse.KEY_TUNING;
    public static final float ALWAYS = 1.0F;
    public static final int NO_COOLDOWN = 0;

    private static final Codec<SummonPassive> FULL = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf(KEY_ABILITY).forGetter(SummonPassive::ability),
            PassiveTrigger.CODEC.optionalFieldOf(KEY_ON).forGetter(SummonPassive::on),
            Codec.FLOAT.optionalFieldOf(KEY_CHANCE).forGetter(SummonPassive::chance),
            Codec.INT.optionalFieldOf(KEY_COOLDOWN_TICKS).forGetter(SummonPassive::cooldownTicks),
            AbilityTuning.OPTIONS_CODEC.optionalFieldOf(KEY_OPTIONS).forGetter(SummonPassive::options),
            AbilityTuning.CODEC.optionalFieldOf(KEY_TUNING).forGetter(SummonPassive::tuning)
    ).apply(instance, SummonPassive::new));

    public static final Codec<SummonPassive> CODEC = Codec.either(Codec.STRING, FULL).xmap(
            either -> either.map(SummonPassive::of, full -> full),
            Either::right);

    public static final SummonPassive SAMPLE = of("");

    public SummonPassive {
        ability = ability == null ? "" : ability;
        on = on == null ? Optional.empty() : on;
        chance = chance == null ? Optional.empty() : chance;
        cooldownTicks = cooldownTicks == null
                ? Optional.empty()
                : cooldownTicks.map(ticks -> Math.max(NO_COOLDOWN, ticks));
        options = options == null ? Optional.empty() : options;
        tuning = tuning == null ? Optional.empty() : tuning;
    }

    public static SummonPassive of(String ability) {
        return new SummonPassive(ability, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    private PassiveAbility defaults() {
        return AbilityRegistry.get(ability) instanceof PassiveAbility passive ? passive : null;
    }

    public PassiveTrigger trigger() {
        PassiveAbility passive = defaults();
        return on.orElse(passive == null ? PassiveTrigger.HIT : passive.on());
    }

    public float odds() {
        PassiveAbility passive = defaults();
        return chance.orElse(passive == null ? ALWAYS : passive.chance());
    }

    public int cooldown() {
        return cooldownTicks.orElse(NO_COOLDOWN);
    }
}
