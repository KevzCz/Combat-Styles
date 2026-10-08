package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.LibraryRef;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.entity.AbilityCaster;
import net.pixeldreamstudios.combat_styles.entity.AbilityPick;
import net.pixeldreamstudios.combat_styles.entity.SummonAim;
import net.pixeldreamstudios.combat_styles.entity.SummonCompanion;

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
                            boolean replacesSummons,
                            Optional<SummonCompanion> companion,
                            Optional<DamageSpec> damageSpec,
                            boolean ownDamage,
                            SummonCadence cadence) {

    private record Core(Optional<String> entity, List<String> abilities,
                        Optional<Integer> maxActive, Optional<Integer> anchored,
                        Optional<Float> damageMultiplier, Optional<Integer> lifespan,
                        Optional<AffixEffects.Scaling> damageScaling, Optional<String> finale,
                        Map<String, AbilityTuning> tuning, List<ProfileVariant> variants) {
    }

    private record Rules(List<String> exempt, List<String> replacedSummonedEntities,
                         boolean replacesSummons, Optional<SummonCompanion> companion,
                         Optional<DamageSpec> damageSpec, boolean ownDamage) {
    }

    public static final String SCHEMA = "@summon_profile";

    public static final SummonProfile EMPTY = new SummonProfile(Optional.empty(), List.of(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Map.of(), List.of(), List.of(),
            List.of(), false, Optional.empty(), Optional.empty(), false, SummonCadence.NONE);

    private static final MapCodec<Core> CORE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("entity").forGetter(Core::entity),
            Codec.STRING.listOf().optionalFieldOf("abilities", List.of())
                    .forGetter(Core::abilities),
            Codec.INT.optionalFieldOf("max_active").forGetter(Core::maxActive),
            Codec.INT.optionalFieldOf("anchored").forGetter(Core::anchored),
            Codec.FLOAT.optionalFieldOf("damage_multiplier").forGetter(Core::damageMultiplier),
            Codec.INT.optionalFieldOf("lifespan").forGetter(Core::lifespan),
            AffixEffects.Scaling.CODEC.optionalFieldOf("damage_scaling")
                    .forGetter(Core::damageScaling),
            Codec.STRING.optionalFieldOf("finale").forGetter(Core::finale),
            Codec.unboundedMap(Codec.STRING, AbilityTuning.CODEC)
                    .optionalFieldOf("tuning", Map.of())
                    .forGetter(Core::tuning),
            ProfileVariant.CODEC.listOf().optionalFieldOf("variants", List.of())
                    .forGetter(Core::variants)
    ).apply(instance, Core::new));

    private static final MapCodec<Rules> RULES_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("exempt", List.of()).forGetter(Rules::exempt),
            Codec.STRING.listOf().optionalFieldOf("replaced_summoned_entities", List.of())
                    .forGetter(Rules::replacedSummonedEntities),
            Codec.BOOL.optionalFieldOf("replaces_summons", Boolean.FALSE)
                    .forGetter(Rules::replacesSummons),
            SummonCompanion.CODEC.optionalFieldOf("companion").forGetter(Rules::companion),
            DamageSpec.CODEC.optionalFieldOf("damage_spec").forGetter(Rules::damageSpec),
            Codec.BOOL.optionalFieldOf("own_damage", Boolean.FALSE).forGetter(Rules::ownDamage)
    ).apply(instance, Rules::new));

    public static final Codec<SummonProfile> CODEC = Customs.wrap(SummonProfile.class, LibraryRef.wrap(RecordCodecBuilder.create(instance -> instance.group(
            CORE_CODEC.forGetter(profile -> new Core(profile.entity(), List.of(),
                    profile.maxActive(), profile.anchored(), profile.damageMultiplier(),
                    profile.lifespan(), profile.damageScaling(), profile.finale(),
                    profile.tuning(), profile.variants())),
            RULES_CODEC.forGetter(profile -> new Rules(profile.exempt(),
                    profile.replacedSummonedEntities(), profile.replacesSummons(),
                    profile.companion(), profile.damageSpec(), profile.ownDamage())),
            SummonCadence.MAP_CODEC.forGetter(profile -> profile.cadence().carrying(profile.abilities()))
    ).apply(instance, (core, rules, cadence) -> new SummonProfile(core.entity(), core.abilities(),
            core.maxActive(), core.anchored(), core.damageMultiplier(), core.lifespan(),
            core.damageScaling(), core.finale(), core.tuning(), core.variants(), rules.exempt(),
            rules.replacedSummonedEntities(), rules.replacesSummons(), rules.companion(),
            rules.damageSpec(), rules.ownDamage(), cadence))), LibraryRef.SUMMON_KEY));

    public SummonProfile(Optional<String> entity, List<String> abilities,
                         Optional<Integer> maxActive, Optional<Integer> anchored,
                         Optional<Float> damageMultiplier, Optional<Integer> lifespan,
                         Optional<AffixEffects.Scaling> damageScaling, Optional<String> finale,
                         Map<String, AbilityTuning> tuning, List<ProfileVariant> variants,
                         List<String> exempt, List<String> replacedSummonedEntities,
                         boolean replacesSummons, Optional<SummonCompanion> companion,
                         Optional<DamageSpec> damageSpec, boolean ownDamage) {
        this(entity, abilities, maxActive, anchored, damageMultiplier, lifespan, damageScaling,
                finale, tuning, variants, exempt, replacedSummonedEntities, replacesSummons,
                companion, damageSpec, ownDamage, SummonCadence.NONE);
    }

    public SummonProfile {
        cadence = cadence == null ? SummonCadence.NONE : cadence;
        abilities = List.copyOf(cadence.abilitiesOr(abilities == null ? List.of() : abilities));
        cadence = cadence.carrying(List.of());
        exempt = exempt == null ? List.of() : List.copyOf(exempt);
        replacedSummonedEntities = replacedSummonedEntities == null
                ? List.of()
                : List.copyOf(replacedSummonedEntities);
        tuning = AbilityConfig.keyed(tuning);
        variants = variants == null ? List.of() : List.copyOf(variants);
        companion = companion == null ? Optional.empty() : companion;
        damageSpec = damageSpec == null ? Optional.empty() : damageSpec;
        if (companion.isEmpty() && anchored != null && anchored.isPresent()) {
            companion = Optional.of(SummonCompanion.ofCount(anchored.get()));
        }
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

        public List<String> abilityPool() {
            for (int index = active.size() - 1; index >= 0; index--) {
                List<String> declared = active.get(index).abilities();
                if (!declared.isEmpty()) {
                    return declared;
                }
            }
            return profile.abilities();
        }

        public String abilityFor(int slot) {
            List<String> pool = abilityPool();
            return pool.isEmpty() ? null : pool.get(Math.floorMod(slot, pool.size()));
        }

        public List<AbilityUse> abilityUses() {
            List<String> pool = abilityPool();
            SummonCadence cadence = poolCadence();
            List<AbilityUse> uses = new ArrayList<>();
            for (int index = 0; index < pool.size(); index++) {
                uses.add(new AbilityUse(pool.get(index), cadence.tuningAt(index)));
            }
            return uses;
        }

        private SummonCadence poolCadence() {
            for (int index = active.size() - 1; index >= 0; index--) {
                if (!active.get(index).abilities().isEmpty()) {
                    return active.get(index).cadence();
                }
            }
            return profile.cadence();
        }

        public AbilityPick abilityPick() {
            for (int index = active.size() - 1; index >= 0; index--) {
                AbilityPick declared = active.get(index).cadence().abilityPick().orElse(null);
                if (declared != null) {
                    return declared;
                }
            }
            return profile.cadence().abilityPick().orElse(AbilityPick.IN_ORDER);
        }

        public List<SummonPassive> passives() {
            for (int index = active.size() - 1; index >= 0; index--) {
                List<SummonPassive> declared = active.get(index).cadence().passives();
                if (!declared.isEmpty()) {
                    return declared;
                }
            }
            return profile.cadence().passives();
        }

        public int recastTicks() {
            for (int index = active.size() - 1; index >= 0; index--) {
                Integer declared = active.get(index).cadence().recastTicks().orElse(null);
                if (declared != null) {
                    return declared;
                }
            }
            return profile.cadence().recastTicks().orElse(SummonCadence.NEVER_RECASTS);
        }

        public SummonAim aim() {
            for (int index = active.size() - 1; index >= 0; index--) {
                SummonAim declared = active.get(index).cadence().aim().orElse(null);
                if (declared != null) {
                    return declared;
                }
            }
            return profile.cadence().aim().orElse(SummonAim.HIT_TARGET);
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

        public Optional<SummonCompanion> companion() {
            return profile.companion().map(base -> {
                int count = anchoredOr(base.count());
                return count == base.count() ? base : new SummonCompanion(count,
                        base.respawnTicks(), base.equipment(), base.attributes(), base.spectral(),
                        base.follow());
            });
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

        public AbilityConfig.Entry tuningFor(String ability, AbilityCaster caster) {
            AbilityConfig.Entry entry = tuningFor(ability);
            if (caster == null) {
                return entry;
            }
            if (caster.holdsEntryFor(ability)) {
                return caster.tuned(ability, entry);
            }
            return entryTuningFor(ability, caster.slot()).map(tuning -> tuning.over(entry)).orElse(entry);
        }

        public Optional<AbilityTuning> entryTuningFor(String ability, int slot) {
            List<String> pool = abilityPool();
            if (ability == null || pool.isEmpty()) {
                return Optional.empty();
            }
            SummonCadence cadence = poolCadence();
            String key = AbilityConfig.key(ability);
            int own = Math.floorMod(slot, pool.size());
            if (entryMatches(pool.get(own), key)) {
                return cadence.tuningAt(own);
            }
            for (int index = 0; index < pool.size(); index++) {
                if (entryMatches(pool.get(index), key)) {
                    return cadence.tuningAt(index);
                }
            }
            return Optional.empty();
        }

        private boolean entryMatches(String entry, String key) {
            return entry != null && (key.equals(AbilityConfig.key(entry))
                    || key.equals(AbilityConfig.key(swap(entry))));
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
