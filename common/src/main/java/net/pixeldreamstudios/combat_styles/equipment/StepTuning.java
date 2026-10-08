package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;

import java.util.Optional;

public record StepTuning(int step, Optional<DamageSpec> damageSpec) {

    public static final String KEY_STEP = "step";
    public static final String KEY_DAMAGE_SPEC = "damage_spec";
    public static final int FIRST_STEP = 0;

    public static final Codec<StepTuning> CODEC = Customs.wrap(StepTuning.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf(KEY_STEP, FIRST_STEP).forGetter(StepTuning::step),
            DamageSpec.CODEC.optionalFieldOf(KEY_DAMAGE_SPEC).forGetter(StepTuning::damageSpec)
    ).apply(instance, StepTuning::new)));

    public StepTuning {
        step = Math.max(FIRST_STEP, step);
        damageSpec = damageSpec == null ? Optional.empty() : damageSpec;
    }
}
