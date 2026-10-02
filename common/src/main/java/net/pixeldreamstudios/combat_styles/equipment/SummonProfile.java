package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record SummonProfile(Optional<String> entity,
                            List<String> abilities,
                            Optional<Integer> maxActive,
                            Optional<Integer> anchored,
                            Optional<Float> damageMultiplier,
                            Optional<Integer> lifespan,
                            Optional<AffixEffects.Scaling> damageScaling,
                            Optional<String> finale,
                            Map<String, AbilityTuning> tuning,
                            List<ProfileVariant> variants,
                            List<String> exempt,
                            List<String> replacedSummonedEntities,
                            boolean replacesSummons) {

    public static final SummonProfile EMPTY = new SummonProfile(Optional.empty(), List.of(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Map.of(), List.of(), List.of(),
            List.of(), false);

    public static final Codec<SummonProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("entity").forGetter(SummonProfile::entity),
            Codec.STRING.listOf().optionalFieldOf("abilities", List.of())
                    .forGetter(SummonProfile::abilities),
            Codec.INT.optionalFieldOf("max_active").forGetter(SummonProfile::maxActive),
            Codec.INT.optionalFieldOf("anchored").forGetter(SummonProfile::anchored),
            Codec.FLOAT.optionalFieldOf("damage_multiplier")
                    .forGetter(SummonProfile::damageMultiplier),
            Codec.INT.optionalFieldOf("lifespan").forGetter(SummonProfile::lifespan),
            AffixEffects.Scaling.CODEC.optionalFieldOf("damage_scaling")
                    .forGetter(SummonProfile::damageScaling),
            Codec.STRING.optionalFieldOf("finale").forGetter(SummonProfile::finale),
            Codec.unboundedMap(Codec.STRING, AbilityTuning.CODEC)
                    .optionalFieldOf("tuning", Map.of())
                    .forGetter(SummonProfile::tuning),
            ProfileVariant.CODEC.listOf().optionalFieldOf("variants", List.of())
                    .forGetter(SummonProfile::variants),
            Codec.STRING.listOf().optionalFieldOf("exempt", List.of())
                    .forGetter(SummonProfile::exempt),
            Codec.STRING.listOf().optionalFieldOf("replaced_summoned_entities", List.of())
                    .forGetter(SummonProfile::replacedSummonedEntities),
            Codec.BOOL.optionalFieldOf("replaces_summons", Boolean.FALSE)
                    .forGetter(SummonProfile::replacesSummons)
    ).apply(instance, SummonProfile::new));

    public SummonProfile {
        abilities = abilities == null ? List.of() : List.copyOf(abilities);
        exempt = exempt == null ? List.of() : List.copyOf(exempt);
        replacedSummonedEntities = replacedSummonedEntities == null
                ? List.of()
                : List.copyOf(replacedSummonedEntities);
        tuning = AbilityConfig.keyed(tuning);
        variants = variants == null ? List.of() : List.copyOf(variants);
    }

    public Resolved resolve(Player player, ProfileCondition.ConditionContext context) {
        List<ProfileVariant> active = new ArrayList<>();
        for (ProfileVariant variant : variants) {
            if (ProfileCondition.all(variant.conditions(), player, context)) {
                active.add(variant);
            }
        }
        return new Resolved(this, active);
    }

    public boolean replaces(String entityId) {
        if (!replacesSummons || exempt.contains(entityId)) {
            return false;
        }
        if (entity.map(id -> id.equals(entityId)).orElse(true)) {
            return false;
        }
        return replacedSummonedEntities.isEmpty()
                || replacedSummonedEntities.contains(entityId);
    }

    public List<String> partners() {
        List<String> found = new ArrayList<>();
        for (ProfileVariant variant : variants) {
            for (ProfileCondition condition : variant.conditions()) {
                condition.holding().ifPresent(found::add);
                condition.wearing().ifPresent(found::add);
                condition.equipped().ifPresent(found::add);
            }
        }
        return List.copyOf(found);
    }

    public record Resolved(SummonProfile profile, List<ProfileVariant> active) {

        public Resolved {
            active = active == null ? List.of() : List.copyOf(active);
        }

        public boolean varied() {
            return !active.isEmpty();
        }

        public String finale() {
            for (int index = active.size() - 1; index >= 0; index--) {
                String declared = active.get(index).finale().orElse(null);
                if (declared != null) {
                    return declared;
                }
            }
            return profile.finale().orElse(null);
        }

        public String swap(String ability) {
            if (ability == null) {
                return null;
            }
            String key = AbilityConfig.key(ability);
            for (int index = active.size() - 1; index >= 0; index--) {
                String swapped = active.get(index).swaps().get(key);
                if (swapped != null) {
                    return swapped;
                }
            }
            return ability;
        }

        public String abilityFor(int slot) {
            List<String> pool = profile.abilities();
            for (int index = active.size() - 1; index >= 0; index--) {
                List<String> declared = active.get(index).abilities();
                if (!declared.isEmpty()) {
                    pool = declared;
                    break;
                }
            }
            return pool.isEmpty() ? null : pool.get(Math.floorMod(slot, pool.size()));
        }

        public float damageScale() {
            for (int index = active.size() - 1; index >= 0; index--) {
                Float declared = active.get(index).damageMultiplier().orElse(null);
                if (declared != null) {
                    return declared;
                }
            }
            return profile.damageMultiplier().orElse(1.0F);
        }

        public int maxActiveOr(int fallback) {
            for (int index = active.size() - 1; index >= 0; index--) {
                Integer declared = active.get(index).maxActive().orElse(null);
                if (declared != null) {
                    return declared;
                }
            }
            return profile.maxActive().orElse(fallback);
        }

        public int anchoredOr(int fallback) {
            for (int index = active.size() - 1; index >= 0; index--) {
                Integer declared = active.get(index).anchored().orElse(null);
                if (declared != null) {
                    return declared;
                }
            }
            return profile.anchored().orElse(fallback);
        }

        public AbilityConfig.Entry tuningFor(String ability) {
            AbilityConfig.Entry entry = AbilityConfig.get(ability);
            if (ability == null) {
                return entry;
            }
            String key = AbilityConfig.key(ability);
            AbilityTuning base = profile.tuning().get(key);
            if (base != null) {
                entry = base.over(entry);
            }
            for (ProfileVariant variant : active) {
                AbilityTuning override = variant.tuning().get(key);
                if (override != null) {
                    entry = override.over(entry);
                }
            }
            return entry;
        }
    }
}
