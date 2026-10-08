package net.pixeldreamstudios.combat_styles.entity;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.ProjectileLaunches;
import net.pixeldreamstudios.combat_styles.affix.AffixTargets;
import net.pixeldreamstudios.combat_styles.affix.SpellShotPlacement;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.config.SummonTuning;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;
import net.pixeldreamstudios.combat_styles.equipment.SpellTuning;
import net.pixeldreamstudios.combat_styles.affix.CastSpell;
import net.pixeldreamstudios.combat_styles.custom.CustomScopes;
import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public record ScriptedAbility(AbilityKind kind, PassiveTrigger on, float chance,
                              List<String> usedBy, AbilityFocus focus, AbilityTuning tuning,
                              List<Step> steps, Optional<String> description) {

    public static final String KEY_KIND = "kind";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_ON = "on";
    public static final String KEY_CHANCE = "chance";
    public static final float ALWAYS = 1.0F;

    public static final int AT_WINDUP = -1;
    public static final int AT_END = -2;
    private static final int AT_START = 0;
    private static final String PHASE_START = "start";
    private static final String PHASE_WINDUP = "windup";
    private static final String PHASE_END = "end";
    private static final double MIN_LENGTH = 1.0E-3D;
    private static final double DASH_STANDOFF = 1.2D;
    private static final double DASH_LIFT = 0.6D;
    private static final double BURST_FLOOR = 0.5D;

    public static final String KEY_TYPE = "type";
    public static final String KEY_DAMAGE_RATIO = "damage_ratio";
    public static final String KEY_AT_FOCUS = "at_focus";
    public static final String KEY_EFFECT = "effect";
    public static final String KEY_STACKS = "stacks";
    public static final String KEY_DURATION_TICKS = "duration_ticks";
    public static final String KEY_MAX_STACKS = "max_stacks";
    public static final String KEY_PER_STACK = "per_stack";
    public static final String KEY_HEAL_RATIO = "heal_ratio";
    public static final String KEY_SOUND = "sound";
    public static final String KEY_PARTICLE = "particle";
    public static final String KEY_PROJECTILE = "projectile";
    public static final String KEY_SPELL = "spell";
    public static final String KEY_SPELL_TUNING = "spell_tuning";
    public static final String KEY_DAMAGE_SPEC = "damage_spec";
    public static final String TUNING_RANGE = "tuning/shape/range";
    public static final String TUNING_RADIUS = "tuning/shape/radius";
    public static final String TUNING_MAX_TARGETS = "tuning/shape/max_targets";
    public static final String TUNING_PULL = "tuning/shape/pull";
    public static final String TUNING_LIFT = "tuning/shape/lift";
    public static final String TUNING_RISE = "tuning/shape/rise";
    public static final String TUNING_HITS = "tuning/damage/hits";
    public static final String TUNING_APPLIES = "tuning/damage/applies";
    public static final String TUNING_WINDUP = "tuning/timing/windup_ticks";
    public static final String TUNING_VOLUME = "tuning/fx/volume";
    public static final String TUNING_PITCH = "tuning/fx/pitch";
    public static final String TUNING_PARTICLES = "tuning/fx/particles";
    public static final String TUNING_SPREAD = "tuning/fx/spread";

    public enum ActionType implements StringRepresentable {
        BEAM(List.of(KEY_DAMAGE_RATIO, KEY_DAMAGE_SPEC, BeamLook.KEY),
                List.of(TUNING_RANGE, TUNING_MAX_TARGETS)),
        BURST(List.of(KEY_DAMAGE_RATIO, KEY_DAMAGE_SPEC, KEY_AT_FOCUS),
                List.of(TUNING_RADIUS, TUNING_MAX_TARGETS, TUNING_HITS)),
        PROJECTILE(List.of(KEY_PROJECTILE, KEY_DAMAGE_RATIO, KEY_DAMAGE_SPEC), List.of()),
        DASH_STRIKE(List.of(KEY_DAMAGE_RATIO, KEY_DAMAGE_SPEC), List.of()),
        PULL(List.of(KEY_DAMAGE_RATIO, KEY_DAMAGE_SPEC, KEY_AT_FOCUS),
                List.of(TUNING_RADIUS, TUNING_PULL, TUNING_LIFT, TUNING_MAX_TARGETS)),
        APPLY_STACKS(List.of(KEY_EFFECT, KEY_STACKS, KEY_DURATION_TICKS, KEY_MAX_STACKS),
                List.of(TUNING_APPLIES, TUNING_RADIUS)),
        CONSUME_STACKS(List.of(KEY_EFFECT, KEY_PER_STACK, KEY_DAMAGE_RATIO, KEY_DAMAGE_SPEC),
                List.of(TUNING_APPLIES, TUNING_RADIUS)),
        HEAL_OWNER(List.of(KEY_HEAL_RATIO, KEY_DAMAGE_SPEC), List.of()),
        RISE(List.of(), List.of(TUNING_RISE, TUNING_WINDUP)),
        SOUND(List.of(KEY_SOUND), List.of(TUNING_VOLUME, TUNING_PITCH)),
        PARTICLES(List.of(KEY_PARTICLE, KEY_AT_FOCUS), List.of(TUNING_PARTICLES, TUNING_SPREAD)),
        CAST_SPELL(List.of(KEY_SPELL, Action.KEY_SPELL_TARGET, Action.KEY_ROLL_SPELL_CHANCE,
                KEY_SPELL_TUNING, KEY_DAMAGE_RATIO, KEY_DAMAGE_SPEC), List.of());

        public static final Codec<ActionType> CODEC = StringRepresentable.fromEnum(ActionType::values);
        public static final List<String> ACCOMPANYING = List.of(KEY_SOUND, KEY_PARTICLE, ParticleShape.KEY);

        private final List<String> actionKeys;
        private final List<String> tuningPaths;

        ActionType(List<String> actionKeys, List<String> tuningPaths) {
            this.actionKeys = actionKeys;
            this.tuningPaths = tuningPaths;
        }

        public List<String> actionKeys() {
            return actionKeys;
        }

        public List<String> tuningPaths() {
            return tuningPaths;
        }

        public boolean reads(String actionKey) {
            return KEY_TYPE.equals(actionKey) || ACCOMPANYING.contains(actionKey)
                    || actionKeys.contains(actionKey);
        }

        public static List<ActionType> readersOf(String key) {
            List<ActionType> found = new ArrayList<>();
            for (ActionType type : values()) {
                if (type.actionKeys.contains(key) || type.tuningPaths.contains(key)) {
                    found.add(type);
                }
            }
            return found;
        }

        public static ActionType byName(String name) {
            for (ActionType type : values()) {
                if (type.getSerializedName().equals(name)) {
                    return type;
                }
            }
            return null;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final Codec<Integer> AT_CODEC = Codec.either(Codec.INT, Codec.STRING).xmap(
            either -> either.map(tick -> Math.max(AT_START, tick), ScriptedAbility::phaseOf),
            at -> at >= AT_START ? Either.left(at) : Either.right(phaseName(at)));

    public record Action(ActionType type, float damageRatio, Optional<DamageSpec> damageSpec,
                         boolean atFocus, Optional<String> effect, int stacks, int durationTicks,
                         int maxStacks, float perStack, float healRatio, Optional<String> sound,
                         Optional<String> particle, Optional<AffixEffects.Projectile> projectile,
                         Optional<String> spell, Optional<SpellTuning> spellTuning,
                         SummonCastTarget spellTarget, boolean rollSpellChance,
                         Optional<BeamLook> beam, Optional<ParticleShape> particleShape) {

        public static final float FULL_DAMAGE = 1.0F;
        public static final float DEFAULT_PER_STACK = 0.25F;
        public static final String KEY_SPELL_TARGET = "spell_target";
        public static final String KEY_ROLL_SPELL_CHANCE = "roll_spell_chance";

        private record Stacking(Optional<String> effect, int stacks, int durationTicks, int maxStacks,
                                float perStack) {
        }

        private record Casting(Optional<String> spell, Optional<SpellTuning> spellTuning,
                               SummonCastTarget spellTarget, boolean rollSpellChance) {
        }

        private static final MapCodec<Stacking> STACKING_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf(KEY_EFFECT).forGetter(Stacking::effect),
                        Codec.INT.optionalFieldOf(KEY_STACKS, 1).forGetter(Stacking::stacks),
                        Codec.INT.optionalFieldOf(KEY_DURATION_TICKS, AbilityStacks.DEFAULT_DURATION_TICKS)
                                .forGetter(Stacking::durationTicks),
                        Codec.INT.optionalFieldOf(KEY_MAX_STACKS, AbilityStacks.DEFAULT_MAX_STACKS)
                                .forGetter(Stacking::maxStacks),
                        Codec.FLOAT.optionalFieldOf(KEY_PER_STACK, DEFAULT_PER_STACK)
                                .forGetter(Stacking::perStack)
                ).apply(instance, Stacking::new));

        private static final MapCodec<Casting> CASTING_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf(KEY_SPELL).forGetter(Casting::spell),
                        SpellTuning.CODEC.optionalFieldOf(KEY_SPELL_TUNING).forGetter(Casting::spellTuning),
                        SummonCastTarget.CODEC.optionalFieldOf(KEY_SPELL_TARGET, SummonCastTarget.AUTO)
                                .forGetter(Casting::spellTarget),
                        Codec.BOOL.optionalFieldOf(KEY_ROLL_SPELL_CHANCE, true)
                                .forGetter(Casting::rollSpellChance)
                ).apply(instance, Casting::new));

        public static final Codec<Action> CODEC = Customs.wrap(Action.class, RecordCodecBuilder.create(instance -> instance.group(
                ActionType.CODEC.fieldOf(KEY_TYPE).forGetter(Action::type),
                Codec.FLOAT.optionalFieldOf(KEY_DAMAGE_RATIO, FULL_DAMAGE).forGetter(Action::damageRatio),
                DamageSpec.CODEC.optionalFieldOf(KEY_DAMAGE_SPEC).forGetter(Action::damageSpec),
                Codec.BOOL.optionalFieldOf(KEY_AT_FOCUS, false).forGetter(Action::atFocus),
                Codec.FLOAT.optionalFieldOf(KEY_HEAL_RATIO, 0.0F).forGetter(Action::healRatio),
                Codec.STRING.optionalFieldOf(KEY_SOUND).forGetter(Action::sound),
                Codec.STRING.optionalFieldOf(KEY_PARTICLE).forGetter(Action::particle),
                AffixEffects.Projectile.CODEC.optionalFieldOf(KEY_PROJECTILE).forGetter(Action::projectile),
                STACKING_CODEC.forGetter(action -> new Stacking(action.effect(), action.stacks(),
                        action.durationTicks(), action.maxStacks(), action.perStack())),
                CASTING_CODEC.forGetter(action -> new Casting(action.spell(), action.spellTuning(),
                        action.spellTarget(), action.rollSpellChance())),
                BeamLook.CODEC.optionalFieldOf(BeamLook.KEY).forGetter(Action::beam),
                ParticleShape.CODEC.optionalFieldOf(ParticleShape.KEY).forGetter(Action::particleShape)
        ).apply(instance, (type, ratio, spec, focus, heal, sound, particle, projectile, stacking, casting,
                           beam, shape) ->
                new Action(type, ratio, spec, focus, stacking.effect(), stacking.stacks(),
                        stacking.durationTicks(), stacking.maxStacks(), stacking.perStack(), heal,
                        sound, particle, projectile, casting.spell(), casting.spellTuning(),
                        casting.spellTarget(), casting.rollSpellChance(), beam, shape))));

        public static final Action SAMPLE = new Action(ActionType.BEAM, FULL_DAMAGE, Optional.empty(),
                false, Optional.empty(), 1, AbilityStacks.DEFAULT_DURATION_TICKS,
                AbilityStacks.DEFAULT_MAX_STACKS, DEFAULT_PER_STACK, 0.0F, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                SummonCastTarget.AUTO, true, Optional.empty(), Optional.empty());

        public Action {
            damageSpec = damageSpec == null ? Optional.empty() : damageSpec;
            spellTarget = spellTarget == null ? SummonCastTarget.AUTO : spellTarget;
            beam = beam == null ? Optional.empty() : beam;
            particleShape = particleShape == null ? Optional.empty() : particleShape;
        }
    }

    public record Step(int at, int every, Optional<Integer> until, float chance, Action action) {

        public static final float ALWAYS = 1.0F;
        public static final int EVERY_TICK = 1;
        public static final String KEY_CHANCE = "chance";
        public static final String KEY_UNTIL = "until";

        public static final Codec<Step> CODEC = Customs.wrap(Step.class, RecordCodecBuilder.create(instance -> instance.group(
                AT_CODEC.optionalFieldOf("at", AT_WINDUP).forGetter(Step::at),
                Codec.INT.optionalFieldOf("every", 0).forGetter(Step::every),
                AT_CODEC.optionalFieldOf(KEY_UNTIL).forGetter(Step::until),
                Codec.FLOAT.optionalFieldOf(KEY_CHANCE, ALWAYS).forGetter(Step::chance),
                Action.CODEC.fieldOf("action").forGetter(Step::action)
        ).apply(instance, Step::new)));

        public static final Step SAMPLE = new Step(AT_WINDUP, 0, Optional.empty(), ALWAYS,
                Action.SAMPLE);

        public Step {
            until = until == null ? Optional.empty() : until;
        }

        public boolean rolls(RandomSource random) {
            return chance >= ALWAYS || random.nextFloat() < chance;
        }

        public boolean firesAt(int elapsed, AbilityConfig.Entry tuned) {
            int start = tickOf(at, tuned);
            int interval = every > 0 ? every : until.isPresent() ? EVERY_TICK : 0;
            if (interval <= 0) {
                return elapsed == start;
            }
            if (until.isPresent() && elapsed > tickOf(until.get(), tuned)) {
                return false;
            }
            return elapsed >= start && (elapsed - start) % interval == 0;
        }

        private static int tickOf(int marker, AbilityConfig.Entry tuned) {
            return switch (marker) {
                case AT_WINDUP -> tuned.windup();
                case AT_END -> Math.max(AT_START, tuned.duration() - 1);
                default -> marker;
            };
        }
    }

    public static final Codec<ScriptedAbility> CODEC = Customs.wrap(ScriptedAbility.class, RecordCodecBuilder.create(instance -> instance.group(
            AbilityKind.CODEC.optionalFieldOf(KEY_KIND, AbilityKind.ACTIVE)
                    .forGetter(ScriptedAbility::kind),
            PassiveTrigger.CODEC.optionalFieldOf(KEY_ON, PassiveTrigger.HIT)
                    .forGetter(ScriptedAbility::on),
            Codec.FLOAT.optionalFieldOf(KEY_CHANCE, ALWAYS).forGetter(ScriptedAbility::chance),
            Codec.STRING.listOf().optionalFieldOf("used_by", List.of())
                    .forGetter(ScriptedAbility::usedBy),
            AbilityFocus.CODEC.optionalFieldOf("focus", AbilityFocus.TARGET)
                    .forGetter(ScriptedAbility::focus),
            AbilityTuning.CODEC.optionalFieldOf("tuning", AbilityTuning.NONE)
                    .forGetter(ScriptedAbility::tuning),
            Step.CODEC.listOf().optionalFieldOf("steps", List.of()).forGetter(ScriptedAbility::steps),
            Codec.STRING.optionalFieldOf(KEY_DESCRIPTION).forGetter(ScriptedAbility::description)
    ).apply(instance, ScriptedAbility::new)));

    public static final ScriptedAbility SAMPLE = new ScriptedAbility(AbilityKind.ACTIVE,
            PassiveTrigger.HIT, ALWAYS, List.of(), AbilityFocus.TARGET, AbilityTuning.NONE,
            List.of(Step.SAMPLE), Optional.empty());

    public boolean passive() {
        return kind == AbilityKind.PASSIVE;
    }

    public ScriptedAbility {
        kind = kind == null ? AbilityKind.ACTIVE : kind;
        on = on == null ? PassiveTrigger.HIT : on;
        usedBy = usedBy == null ? List.of() : List.copyOf(usedBy);
        focus = focus == null ? AbilityFocus.TARGET : focus;
        tuning = tuning == null ? AbilityTuning.NONE : tuning;
        steps = steps == null ? List.of() : List.copyOf(steps);
        description = description == null ? Optional.empty() : description.filter(text -> !text.isBlank());
    }

    public AbilityConfig.Entry defaults() {
        return tuning.over(new AbilityConfig.Entry());
    }

    public Vec3 focusPoint(AbilityHost host, AbilityConfig.Entry tuned) {
        return focus.pointFor(host, tuned.reach(SummonTuning.get().aimRange));
    }

    public void run(ServerLevel level, AbilityHost host, int elapsed, float damage,
                    AbilityConfig.Entry tuned) {
        Player owner = host.owner();
        if (owner == null) {
            return;
        }
        if (elapsed == AT_START) {
            Customs.apply(CustomScopes.SCRIPTED_ABILITY, this, owner, host.body(), host.target());
        }
        Customs.ticked(CustomScopes.SCRIPTED_ABILITY, this, owner, host.body());
        for (int index = 0; index < steps.size(); index++) {
            Step step = steps.get(index);
            if (!step.firesAt(elapsed, tuned) || !step.rolls(level.getRandom())
                    || !Customs.allows(CustomScopes.ABILITY_STEP, step, owner, host.body(), host.target())) {
                continue;
            }
            Customs.apply(CustomScopes.ABILITY_STEP, step, owner, host.body(), host.target());
            Action action = step.action();
            JsonObject stepPatch = tuned.stepSpec(index);
            if (action.damageSpec().isEmpty() && stepPatch == null) {
                perform(level, host, owner, action, elapsed, damage, tuned);
                continue;
            }
            AbilityConfig.Entry stepTuned = tuned.copy();
            action.damageSpec().ifPresent(stepTuned.damage::useSpec);
            stepTuned.damage.mergeSpec(stepPatch);
            perform(level, host, owner, action, elapsed,
                    stepTuned.damageFrom(owner, host.baseDamage()), stepTuned);
        }
    }

    private void perform(ServerLevel level, AbilityHost host, Player owner, Action action,
                         int elapsed, float damage, AbilityConfig.Entry tuned) {
        if (!Customs.allows(CustomScopes.ABILITY_ACTION, action, owner, host.body(), host.target())) {
            return;
        }
        Customs.apply(CustomScopes.ABILITY_ACTION, action, owner, host.body(), host.target());
        float amount = damage * action.damageRatio();
        Vec3 focusPoint = focusPoint(host, tuned);
        Vec3 centre = action.atFocus() ? focusPoint : host.origin();
        if (action.type() != ActionType.SOUND) {
            action.sound().ifPresent(id -> sound(level, host, id, tuned));
        }
        Vec3 particleCentre = particleCentre(host, action, focusPoint);
        if (action.type() != ActionType.PARTICLES) {
            action.particle().ifPresent(id -> particles(level, host, particleCentre, id, action, elapsed,
                    tuned));
        }
        switch (action.type()) {
            case BEAM -> beam(level, host, owner, focusPoint, amount, tuned, action.beam());
            case BURST -> {
                for (LivingEntity victim : victimsAround(level, owner, centre, tuned)) {
                    AbilityStrike.hit(host.body(), owner, victim, amount, tuned);
                }
            }
            case PROJECTILE -> action.projectile().ifPresent(projectile ->
                    ProjectileLaunches.spawnFrom(level, owner, host.body(), host.target(),
                            projectile, amount));
            case DASH_STRIKE -> dashStrike(host, owner, amount, tuned);
            case PULL -> {
                for (LivingEntity victim : victimsAround(level, owner, centre, tuned)) {
                    pull(centre, victim, tuned);
                    if (action.damageRatio() > 0.0F && amount > 0.0F) {
                        AbilityStrike.hit(host.body(), owner, victim, amount, tuned);
                    }
                }
            }
            case APPLY_STACKS -> {
                String effect = action.effect().orElse(tuned.damage.applies);
                for (LivingEntity victim : victimsOf(level, host, owner, centre, tuned)) {
                    AbilityStacks.apply(victim, effect, action.stacks(), action.durationTicks(),
                            action.maxStacks());
                }
            }
            case CONSUME_STACKS -> {
                String effect = action.effect().orElse(tuned.damage.applies);
                for (LivingEntity victim : victimsOf(level, host, owner, centre, tuned)) {
                    int consumed = AbilityStacks.consume(victim, effect);
                    if (consumed > 0) {
                        AbilityStrike.hit(host.body(), owner, victim,
                                amount * action.perStack() * consumed, tuned);
                    }
                }
            }
            case HEAL_OWNER -> owner.heal(damage * action.healRatio());
            case RISE -> rise(host, elapsed, tuned);
            case SOUND -> action.sound().ifPresent(id -> sound(level, host, id, tuned));
            case PARTICLES -> action.particle().ifPresent(id -> particles(level, host, particleCentre, id,
                    action, elapsed, tuned));
            case CAST_SPELL -> action.spell().ifPresent(id -> castSpell(level, host, owner, id,
                    action, amount));
        }
    }

    private static void beam(ServerLevel level, AbilityHost host, Player owner, Vec3 focusPoint,
                             float amount, AbilityConfig.Entry tuned, Optional<BeamLook> look) {
        Vec3 from = look.map(chosen -> chosen.start(host.body(), host.origin())).orElse(host.origin());
        Vec3 delta = focusPoint.subtract(from);
        double length = Math.min(delta.length(), tuned.reach(delta.length()));
        if (length < MIN_LENGTH) {
            return;
        }
        Vec3 direction = delta.normalize();
        Vec3 end = from.add(direction.scale(length));
        if (look.isEmpty() && host.drawsOwnBeam()) {
            host.showBeam(end);
        } else {
            BeamCast.show(level, host.body(), from, end, look.orElse(BeamLook.DEFAULT));
        }
        double stepLength = SummonTuning.get().beamStep;
        Set<LivingEntity> struck = new LinkedHashSet<>();
        for (double travelled = 0.0D; travelled < length; travelled += stepLength) {
            Vec3 at = from.add(direction.scale(travelled));
            AABB box = new AABB(at, at).inflate(SummonTuning.get().beamHitPadding);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (struck.contains(victim) || !AffixTargets.canHit(owner, victim)) {
                    continue;
                }
                AbilityStrike.hit(host.body(), owner, victim, amount, tuned);
                struck.add(victim);
                if (struck.size() >= tuned.targets()) {
                    return;
                }
            }
        }
    }

    private static List<LivingEntity> victimsAround(ServerLevel level, Player owner, Vec3 centre,
                                                    AbilityConfig.Entry tuned) {
        double radius = tuned.area(BURST_FLOOR);
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(centre, centre).inflate(radius))) {
            if (AffixTargets.canHit(owner, victim)
                    && victim.getBoundingBox().getCenter().distanceToSqr(centre) <= radius * radius) {
                found.add(victim);
                if (found.size() >= Math.max(tuned.targets(), tuned.hits())
                        && tuned.shape.maxTargets > 1) {
                    break;
                }
            }
        }
        return found;
    }

    private static List<LivingEntity> victimsOf(ServerLevel level, AbilityHost host, Player owner,
                                                Vec3 centre, AbilityConfig.Entry tuned) {
        if (tuned.shape.radius > 0.0D) {
            return victimsAround(level, owner, centre, tuned);
        }
        LivingEntity target = host.target();
        return target != null && target.isAlive() && AffixTargets.canHit(owner, target)
                ? List.of(target)
                : List.of();
    }

    private static void dashStrike(AbilityHost host, Player owner, float amount,
                                   AbilityConfig.Entry tuned) {
        LivingEntity target = host.target();
        if (target == null || !target.isAlive() || !AffixTargets.canHit(owner, target)) {
            return;
        }
        Vec3 away = host.origin().subtract(target.position());
        Vec3 flat = new Vec3(away.x, 0.0D, away.z);
        Vec3 offset = flat.lengthSqr() < MIN_LENGTH ? Vec3.ZERO : flat.normalize().scale(DASH_STANDOFF);
        Vec3 spot = target.position().add(offset).add(0.0D, DASH_LIFT, 0.0D);
        host.holdAt(spot);
        AbilityStrike.hit(host.body(), owner, target, amount, tuned);
    }

    private static void pull(Vec3 centre, LivingEntity victim, AbilityConfig.Entry tuned) {
        Vec3 toward = centre.subtract(victim.position());
        if (toward.lengthSqr() < MIN_LENGTH) {
            return;
        }
        Vec3 drag = toward.normalize().scale(tuned.shape.pull);
        victim.push(drag.x, tuned.shape.lift, drag.z);
        victim.hurtMarked = true;
    }

    private static void rise(AbilityHost host, int elapsed, AbilityConfig.Entry tuned) {
        int windup = Math.max(1, tuned.windup());
        double progress = Math.min(1.0D, elapsed / (double) windup);
        host.holdAt(host.castStart().add(0.0D, tuned.shape.rise * progress, 0.0D));
    }

    private static void sound(ServerLevel level, AbilityHost host, String id,
                              AbilityConfig.Entry tuned) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {
            return;
        }
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.getOptional(key)
                .orElseGet(() -> SoundEvent.createVariableRangeEvent(key));
        level.playSound(null, BlockPos.containing(host.body().position()), event,
                SoundSource.PLAYERS, tuned.fx.volume, tuned.fx.pitch);
    }

    private Vec3 particleCentre(AbilityHost host, Action action, Vec3 focusPoint) {
        if (!action.atFocus()) {
            return host.body().position();
        }
        LivingEntity target = host.target();
        return focus == AbilityFocus.TARGET && target != null && target.isAlive()
                ? target.position()
                : focusPoint;
    }

    private static void particles(ServerLevel level, AbilityHost host, Vec3 centre, String id,
                                  Action action, int elapsed, AbilityConfig.Entry tuned) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        ParticleType<?> type = key == null
                ? null
                : BuiltInRegistries.PARTICLE_TYPE.getOptional(key).orElse(null);
        if (!(type instanceof ParticleOptions options)) {
            return;
        }
        double progress = elapsed / (double) Math.max(1, tuned.duration() - 1);
        action.particleShape().orElse(ParticleShape.DEFAULT).draw(level, options, centre, host.origin(),
                tuned.area(ParticleShape.FROM_TUNING), Math.max(1, tuned.fx.particles), tuned.fx.spread,
                progress);
    }

    private static void castSpell(ServerLevel level, AbilityHost host, Player owner, String id,
                                  Action action, float amount) {
        ResourceLocation spellId = ResourceLocation.tryParse(id);
        if (spellId == null || !SpellEngineCompat.isLoaded()) {
            return;
        }
        float damage = action.damageRatio() > 0.0F && amount > 0.0F
                ? amount
                : SpellEngineCompat.INHERIT_DAMAGE;
        SpellTuning tuning = action.spellTuning().orElse(SpellTuning.NONE);
        if (action.rollSpellChance() && level.getRandom().nextFloat()
                >= tuning.chanceFrom(SpellEngineCompat.triggerChance(spellId, level))) {
            return;
        }
        LivingEntity target = host.target();
        Entity body = host.body();
        SummonCastTarget mode = action.spellTarget();
        if (body instanceof LivingEntity caster) {
            SpellEngineCompat.castSpellAs(caster, owner, spellId, target, damage, tuning, mode);
            return;
        }
        if (mode == SummonCastTarget.HIT && target == null) {
            return;
        }
        boolean onOwner = mode == SummonCastTarget.SELF || mode == SummonCastTarget.OWNER;
        if (!onOwner && SpellEngineCompat.deliversProjectile(spellId, level)) {
            SpellEngineCompat.shootSpell(owner, spellId, target, damage, tuning,
                    launched -> SpellShotPlacement.expectFrom(launched, owner, target, body));
            return;
        }
        CastSpell.Target cast = onOwner
                ? CastSpell.Target.SELF
                : mode == SummonCastTarget.AIM
                        ? CastSpell.Target.AIM
                        : (target == null ? CastSpell.Target.SELF : CastSpell.Target.HIT);
        SpellEngineCompat.castSpell(owner, spellId, cast, target, damage, tuning);
    }

    private static int phaseOf(String phase) {
        return switch (phase == null ? "" : phase.toLowerCase(Locale.ROOT)) {
            case PHASE_START -> AT_START;
            case PHASE_END -> AT_END;
            default -> AT_WINDUP;
        };
    }

    private static String phaseName(int at) {
        return at == AT_END ? PHASE_END : PHASE_WINDUP;
    }
}
