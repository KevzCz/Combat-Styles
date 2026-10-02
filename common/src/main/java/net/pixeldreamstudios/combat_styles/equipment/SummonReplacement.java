package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;

import java.util.List;
import java.util.Optional;

public record SummonReplacement(String becomeEntity,
                                List<String> replacedSummonedEntities,
                                List<String> abilities,
                                Optional<Integer> maxActive,
                                Optional<Float> damageMultiplier,
                                Optional<Integer> lifespan,
                                Optional<AffixEffects.Scaling> damageScaling,
                                List<String> exempt) {

    public static final Codec<SummonReplacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("become_entity").forGetter(SummonReplacement::becomeEntity),
            Codec.STRING.listOf().optionalFieldOf("replaced_summoned_entities", List.of())
                    .forGetter(SummonReplacement::replacedSummonedEntities),
            Codec.STRING.listOf().optionalFieldOf("abilities", List.of())
                    .forGetter(SummonReplacement::abilities),
            Codec.INT.optionalFieldOf("max_active").forGetter(SummonReplacement::maxActive),
            Codec.FLOAT.optionalFieldOf("damage_multiplier")
                    .forGetter(SummonReplacement::damageMultiplier),
            Codec.INT.optionalFieldOf("lifespan").forGetter(SummonReplacement::lifespan),
            AffixEffects.Scaling.CODEC.optionalFieldOf("damage_scaling")
                    .forGetter(SummonReplacement::damageScaling),
            Codec.STRING.listOf().optionalFieldOf("exempt", List.of())
                    .forGetter(SummonReplacement::exempt)
    ).apply(instance, SummonReplacement::new));

    public SummonReplacement {
        replacedSummonedEntities = replacedSummonedEntities == null
                ? List.of()
                : List.copyOf(replacedSummonedEntities);
        abilities = abilities == null ? List.of() : List.copyOf(abilities);
        exempt = exempt == null ? List.of() : List.copyOf(exempt);
    }

    public String entity() {
        return becomeEntity;
    }

    public boolean replaces(String entityId) {
        if (becomeEntity.equals(entityId) || exempt.contains(entityId)) {
            return false;
        }
        return replacedSummonedEntities.isEmpty()
                || replacedSummonedEntities.contains(entityId);
    }

    public String abilityFor(int slot) {
        return abilities.isEmpty()
                ? null
                : abilities.get(Math.floorMod(slot, abilities.size()));
    }

    public float damageScale() {
        return damageMultiplier.orElse(1.0F);
    }
}
