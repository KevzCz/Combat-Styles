package net.pixeldreamstudios.combat_styles.affix;

import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.damage.ScaledNumbers;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.entity.RaiseRules;
import net.pixeldreamstudios.combat_styles.entity.SummonEquipment;
import net.pixeldreamstudios.combat_styles.entity.SummonStats;
import net.pixeldreamstudios.combat_styles.entity.SummonAim;
import net.pixeldreamstudios.combat_styles.entity.SummonTargeting;
import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;
import net.pixeldreamstudios.combat_styles.equipment.SummonCadence;
import net.pixeldreamstudios.combat_styles.equipment.StateGate;
import net.pixeldreamstudios.combat_styles.equipment.SpellShot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class AffixEffects {
    public static OnHit tune(OnHit onHit, Optional<Double> chance, Optional<Integer> cooldown) {
        if (onHit == null || onHit.isEmpty() || (chance.isEmpty() && cooldown.isEmpty())) {
            return onHit;
        }

        Optional<Ignite> ignite = onHit.ignite();
        Optional<Lifesteal> lifesteal = onHit.lifesteal();
        List<StatusEffect> statusEffects = onHit.statusEffects();
        Mechanics mechanics = onHit.mechanics();

        if (chance.isPresent()) {
            float value = chance.get().floatValue();
            ignite = ignite.map(existing ->
                    new Ignite(existing.seconds(), value));
            lifesteal = lifesteal.map(existing ->
                    new Lifesteal(existing.ratio(), value, existing.damageSpec()));
            List<StatusEffect> tuned = new ArrayList<>(statusEffects.size());
            for (StatusEffect effect : statusEffects) {
                tuned.add(new StatusEffect(effect.effect(), effect.duration(),
                        effect.amplifier(), value, effect.stacks(), effect.maxAmplifier()));
            }
            statusEffects = List.copyOf(tuned);
        }

        if (cooldown.isPresent()) {
            mechanics = mechanics.withCooldown(new Cooldown(cooldown.get()));
        }

        OnHit tuned = Customs.copy(onHit, new OnHit(onHit.damage(), onHit.damages(), statusEffects, ignite, lifesteal,
                onHit.knockback(), onHit.splash(), onHit.selfEffects(), onHit.sound(),
                onHit.particles(), onHit.projectiles(), onHit.castSpells(), onHit.slashWave(),
                mechanics, onHit.freezeTicks(), onHit.delayTicks(), onHit.echo()));
        return ScaledNumbers.carry(onHit, tuned, path ->
                (chance.isPresent() && TUNED_CHANCE.matcher(path).matches())
                        || (cooldown.isPresent() && path.startsWith(TUNED_COOLDOWN)));
    }

    private static final Pattern TUNED_CHANCE =
            Pattern.compile("(ignite|lifesteal|effects/\\d+)/chance");
    private static final String TUNED_COOLDOWN = "mechanics/cooldown";

    private AffixEffects() {
    }

    public enum DamageMode {
        ADDITIVE,
        AMPLIFY,
        CONVERT;

        public static final Codec<DamageMode> CODEC = Codec.STRING.xmap(
                raw -> {
                    try {
                        return valueOf(raw.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        return ADDITIVE;
                    }
                },
                mode -> mode.name().toLowerCase(Locale.ROOT));
    }

    public interface Typed {
        boolean inherit();

        Optional<String> damageType();

        Optional<String> school();
    }

    public interface Delayed<T extends Delayed<T>> {
        int delayTicks();

        T withoutDelay();

        boolean isEmpty();
    }

    public record Scaling(List<String> sources, float perPoint, Optional<Float> cap,
                          boolean strongest) {
        private static final Codec<List<String>> SOURCE_CODEC = Codec.either(
                        Codec.STRING, Codec.STRING.listOf())
                .xmap(either -> either.map(List::of, list -> list),
                        list -> list.size() == 1
                                ? Either.left(list.getFirst())
                                : Either.right(list));

        public static final Codec<Scaling> CODEC = Customs.wrap(Scaling.class, RecordCodecBuilder.create(instance -> instance.group(
                SOURCE_CODEC.fieldOf("source").forGetter(Scaling::sources),
                Codec.FLOAT.optionalFieldOf("per_point", 0.0F).forGetter(Scaling::perPoint),
                Codec.FLOAT.optionalFieldOf("cap").forGetter(Scaling::cap),
                Codec.BOOL.optionalFieldOf("strongest", true).forGetter(Scaling::strongest)
        ).apply(instance, Scaling::new)));

        public float apply(float base, double sourceValue) {
            float bonus = (float) (perPoint * sourceValue);
            return base + cap.map(limit -> Math.min(bonus, limit)).orElse(bonus);
        }
    }

    public record Damage(DamageMode mode, String damageType, Optional<String> school,
                         float ratio, float flat, boolean ignoresArmour,
                         Optional<Scaling> scaling, float multiplier) {
        public static final float NO_MULTIPLIER = 1.0F;
        public static final String KEY_MULTIPLIER = "multiplier";
        public static final String KEY_RATIO = "ratio";

        public static final Codec<Damage> CODEC = Customs.wrap(Damage.class, RecordCodecBuilder.create(instance -> instance.group(
                DamageMode.CODEC.optionalFieldOf("mode", DamageMode.ADDITIVE).forGetter(Damage::mode),
                Codec.STRING.optionalFieldOf("type", "").forGetter(Damage::damageType),
                Codec.STRING.optionalFieldOf("school").forGetter(Damage::school),
                Codec.FLOAT.optionalFieldOf(KEY_RATIO, 0.0F).forGetter(Damage::ratio),
                Codec.FLOAT.optionalFieldOf("flat", 0.0F).forGetter(Damage::flat),
                Codec.BOOL.optionalFieldOf("ignores_armour", false).forGetter(Damage::ignoresArmour),
                Scaling.CODEC.optionalFieldOf("scaling").forGetter(Damage::scaling),
                Codec.FLOAT.optionalFieldOf(KEY_MULTIPLIER, NO_MULTIPLIER).forGetter(Damage::multiplier)
        ).apply(instance, Damage::new)));

        public Damage(DamageMode mode, String damageType, Optional<String> school, float ratio,
                      float flat, boolean ignoresArmour, Optional<Scaling> scaling) {
            this(mode, damageType, school, ratio, flat, ignoresArmour, scaling, NO_MULTIPLIER);
        }

        public boolean converts() {
            return mode == DamageMode.CONVERT;
        }

        public boolean amplifies() {
            return mode == DamageMode.AMPLIFY;
        }
    }

    public record StatusEffect(String effect, int duration, int amplifier, float chance,
                               boolean stacks, Optional<Integer> maxAmplifier) {
        public static final Codec<StatusEffect> CODEC = Customs.wrap(StatusEffect.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("effect").forGetter(StatusEffect::effect),
                Codec.INT.optionalFieldOf("duration", 60).forGetter(StatusEffect::duration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(StatusEffect::amplifier),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(StatusEffect::chance),
                Codec.BOOL.optionalFieldOf("stacks", false).forGetter(StatusEffect::stacks),
                Codec.INT.optionalFieldOf("max_amplifier").forGetter(StatusEffect::maxAmplifier)
        ).apply(instance, StatusEffect::new)));
    }

    public record Ignite(int seconds, float chance) {
        public static final Codec<Ignite> CODEC = Customs.wrap(Ignite.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("seconds", 4).forGetter(Ignite::seconds),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Ignite::chance)
        ).apply(instance, Ignite::new)));
    }

    public record Lifesteal(float ratio, float chance, Optional<DamageSpec> damageSpec) {
        public static final Codec<Lifesteal> CODEC = Customs.wrap(Lifesteal.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("ratio", 0.15F).forGetter(Lifesteal::ratio),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Lifesteal::chance),
                DamageSpec.CODEC.optionalFieldOf("damage_spec").forGetter(Lifesteal::damageSpec)
        ).apply(instance, Lifesteal::new)));
    }

    public record Knockback(float strength, boolean pull) {
        public static final Codec<Knockback> CODEC = Customs.wrap(Knockback.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("strength", 0.5F).forGetter(Knockback::strength),
                Codec.BOOL.optionalFieldOf("pull", false).forGetter(Knockback::pull)
        ).apply(instance, Knockback::new)));
    }

    public record Splash(float radius, float ratio, boolean hitsAttacker,
                         boolean inherit, Optional<String> damageType,
                         Optional<String> school,
                         Optional<DamageSpec> damageSpec) implements Typed {
        public static final Codec<Splash> CODEC = Customs.wrap(Splash.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("radius", 3.0F).forGetter(Splash::radius),
                Codec.FLOAT.optionalFieldOf("ratio", 0.4F).forGetter(Splash::ratio),
                Codec.BOOL.optionalFieldOf("hits_attacker", false).forGetter(Splash::hitsAttacker),
                Codec.BOOL.optionalFieldOf("inherit", true).forGetter(Splash::inherit),
                Codec.STRING.optionalFieldOf("damage_type").forGetter(Splash::damageType),
                Codec.STRING.optionalFieldOf("school").forGetter(Splash::school),
                DamageSpec.CODEC.optionalFieldOf("damage_spec").forGetter(Splash::damageSpec)
        ).apply(instance, Splash::new)));
    }

    public record SelfEffect(String effect, int duration, int amplifier,
                             boolean stacks, Optional<Integer> maxAmplifier) {
        public static final Codec<SelfEffect> CODEC = Customs.wrap(SelfEffect.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("effect").forGetter(SelfEffect::effect),
                Codec.INT.optionalFieldOf("duration", 60).forGetter(SelfEffect::duration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(SelfEffect::amplifier),
                Codec.BOOL.optionalFieldOf("stacks", false).forGetter(SelfEffect::stacks),
                Codec.INT.optionalFieldOf("max_amplifier").forGetter(SelfEffect::maxAmplifier)
        ).apply(instance, SelfEffect::new)));
    }

    public record Sound(String id, float volume, float pitch) {
        public static final Codec<Sound> CODEC = Customs.wrap(Sound.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Sound::id),
                Codec.FLOAT.optionalFieldOf("volume", 1.0F).forGetter(Sound::volume),
                Codec.FLOAT.optionalFieldOf("pitch", 1.0F).forGetter(Sound::pitch)
        ).apply(instance, Sound::new)));
    }

    public record Particles(String type, int count, float spread, float speed) {
        public static final Codec<Particles> CODEC = Customs.wrap(Particles.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(Particles::type),
                Codec.INT.optionalFieldOf("count", 12).forGetter(Particles::count),
                Codec.FLOAT.optionalFieldOf("spread", 0.4F).forGetter(Particles::spread),
                Codec.FLOAT.optionalFieldOf("speed", 0.05F).forGetter(Particles::speed)
        ).apply(instance, Particles::new)));
    }

    public record WaveSpeed(float start, float trough, float end, int stallTicks, int recoverTicks) {
        public static final WaveSpeed FLAT = new WaveSpeed(1.0F, 1.0F, 1.0F, 0, 0);

        public static final Codec<WaveSpeed> CODEC = Customs.wrap(WaveSpeed.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("start", 0.2F).forGetter(WaveSpeed::start),
                Codec.FLOAT.optionalFieldOf("trough", 0.01F).forGetter(WaveSpeed::trough),
                Codec.FLOAT.optionalFieldOf("end", 1.0F).forGetter(WaveSpeed::end),
                Codec.INT.optionalFieldOf("stall_ticks", 3).forGetter(WaveSpeed::stallTicks),
                Codec.INT.optionalFieldOf("recover_ticks", 6).forGetter(WaveSpeed::recoverTicks)
        ).apply(instance, WaveSpeed::new)));

        public boolean isFlat() {
            return stallTicks <= 0 && recoverTicks <= 0;
        }

        public float multiplierAt(int age) {
            if (isFlat()) {
                return end;
            }
            if (age < stallTicks) {
                float t = stallTicks <= 1 ? 1.0F : age / (float) stallTicks;
                return start + (trough - start) * smooth(t);
            }
            int into = age - stallTicks;
            if (into >= recoverTicks) {
                return end;
            }
            float t = recoverTicks <= 0 ? 1.0F : into / (float) recoverTicks;
            return trough + (end - trough) * smooth(t);
        }

        private static float smooth(float t) {
            float clamped = Math.clamp(t, 0.0F, 1.0F);
            return clamped * clamped * (3.0F - 2.0F * clamped);
        }
    }

    public record SlashWave(float range, float radius, float ratio, float speed,
                            float chance, boolean followsSwing,
                            boolean inherit,
                            Optional<DamageSpec> damageSpec,
                            boolean pierces,
                            Optional<String> texture, int frames,
                            float scale, Optional<TrailColour> colour, int delayTicks,
                            Optional<WaveSpeed> speedCurve) implements Typed {
        public static final int DEFAULT_FRAMES = 10;

        private record Shape(float range, float radius, float ratio, float speed,
                             float chance, boolean followsSwing, boolean inherit, int delayTicks,
                             Optional<WaveSpeed> speedCurve) {
        }

        private record Visual(Optional<DamageSpec> damageSpec,
                              boolean pierces,
                              Optional<String> texture, int frames,
                              float scale, Optional<TrailColour> colour) {
        }

        private static final MapCodec<Shape> SHAPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("range", 12.0F).forGetter(Shape::range),
                Codec.FLOAT.optionalFieldOf("radius", 1.2F).forGetter(Shape::radius),
                Codec.FLOAT.optionalFieldOf("ratio", 0.5F).forGetter(Shape::ratio),
                Codec.FLOAT.optionalFieldOf("speed", 0.8F).forGetter(Shape::speed),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Shape::chance),
                Codec.BOOL.optionalFieldOf("follows_swing", true).forGetter(Shape::followsSwing),
                Codec.BOOL.optionalFieldOf("inherit", true).forGetter(Shape::inherit),
                Codec.INT.optionalFieldOf("delay_ticks", 0).forGetter(Shape::delayTicks),
                WaveSpeed.CODEC.optionalFieldOf("speed_curve").forGetter(Shape::speedCurve)
        ).apply(instance, Shape::new));

        private static final MapCodec<Visual> VISUAL_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                DamageSpec.CODEC.optionalFieldOf("damage_spec").forGetter(Visual::damageSpec),
                Codec.BOOL.optionalFieldOf("pierces", true).forGetter(Visual::pierces),
                Codec.STRING.optionalFieldOf("texture").forGetter(Visual::texture),
                Codec.INT.optionalFieldOf("frames", DEFAULT_FRAMES).forGetter(Visual::frames),
                Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(Visual::scale),
                TrailColour.CODEC.optionalFieldOf("colour").forGetter(Visual::colour)
        ).apply(instance, Visual::new));

        public static final Codec<SlashWave> CODEC = Customs.wrap(SlashWave.class, RecordCodecBuilder.create(instance -> instance.group(
                SHAPE_CODEC.forGetter(wave -> new Shape(wave.range(), wave.radius(), wave.ratio(),
                        wave.speed(), wave.chance(), wave.followsSwing(), wave.inherit(),
                        wave.delayTicks(), wave.speedCurve())),
                VISUAL_CODEC.forGetter(wave -> new Visual(wave.damageSpec(),
                        wave.pierces(), wave.texture(), wave.frames(),
                        wave.scale(), wave.colour()))
        ).apply(instance, (shape, visual) -> new SlashWave(
                shape.range(), shape.radius(), shape.ratio(), shape.speed(), shape.chance(),
                shape.followsSwing(), shape.inherit(), visual.damageSpec(),
                visual.pierces(), visual.texture(), visual.frames(),
                visual.scale(), visual.colour(), shape.delayTicks(), shape.speedCurve()))));

        public Optional<String> damageType() {
            return damageSpec.flatMap(DamageSpec::damageType);
        }

        public Optional<String> school() {
            return damageSpec.flatMap(DamageSpec::school);
        }
    }

    public record TrailColour(String colour, boolean glows) {
        private static final String KEY_COLOR = "color";

        public static final Codec<TrailColour> CODEC = Customs.wrap(TrailColour.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf(KEY_COLOR).forGetter(TrailColour::colour),
                Codec.BOOL.optionalFieldOf("glows", true).forGetter(TrailColour::glows)
        ).apply(instance, TrailColour::new)));

        public int argb() {
            String hex = colour.startsWith("#") ? colour.substring(1) : colour;
            try {
                long value = Long.parseLong(hex, 16);
                return hex.length() <= 6
                        ? (int) (0xFF000000L | value)
                        : (int) value;
            } catch (NumberFormatException ignored) {
                return 0xFFFFFFFF;
            }
        }
    }

    public record Projectile(String entity, int count, float speed, float spread,
                             float damage, boolean fromTarget, float chance,
                             Optional<DamageSpec> damageSpec, int lifetimeTicks,
                             boolean removeOnLanding, ProjectileSpawn spawn,
                             SpellShot spellProjectile, String name, boolean ignoreIframes) {
        public static final int NO_LIFETIME = 0;

        public static final Codec<Projectile> CODEC = Customs.wrap(Projectile.class, LibraryRef.wrap(RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("entity", "").forGetter(Projectile::entity),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Projectile::count),
                Codec.FLOAT.optionalFieldOf("speed", 1.5F).forGetter(Projectile::speed),
                Codec.FLOAT.optionalFieldOf("spread", 0.0F).forGetter(Projectile::spread),
                Codec.FLOAT.optionalFieldOf("damage", 0.0F).forGetter(Projectile::damage),
                Codec.BOOL.optionalFieldOf("from_target", false).forGetter(Projectile::fromTarget),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Projectile::chance),
                DamageSpec.CODEC.optionalFieldOf("damage_spec").forGetter(Projectile::damageSpec),
                Codec.INT.optionalFieldOf("lifetime_ticks", NO_LIFETIME)
                        .forGetter(Projectile::lifetimeTicks),
                Codec.BOOL.optionalFieldOf("remove_on_landing", false)
                        .forGetter(Projectile::removeOnLanding),
                ProjectileSpawn.CODEC.optionalFieldOf("spawn", ProjectileSpawn.DEFAULT)
                        .forGetter(Projectile::spawn),
                SpellShot.CODEC.optionalFieldOf("spell_projectile", SpellShot.NONE)
                        .forGetter(Projectile::spellProjectile),
                Codec.STRING.optionalFieldOf("name", "").forGetter(Projectile::name),
                Codec.BOOL.optionalFieldOf("ignore_iframes", false)
                        .forGetter(Projectile::ignoreIframes)
        ).apply(instance, Projectile::new)), LibraryRef.PROJECTILE_KEY));

        public Projectile {
            spellProjectile = spellProjectile == null ? SpellShot.NONE : spellProjectile;
            name = name == null ? "" : name;
            spawn = spawn == null ? ProjectileSpawn.DEFAULT : spawn;
            if (fromTarget && spawn.anchor() == SpawnAnchor.EYES) {
                spawn = new ProjectileSpawn(SpawnAnchor.TARGET, spawn.forward(), spawn.up(),
                        spawn.right(), spawn.yaw(), spawn.pitch(), spawn.mode(), spawn.arc(),
                        spawn.inheritMotion(), spawn.aimAtTarget(), spawn.velocity());
            }
        }

        public boolean hasLifetime() {
            return lifetimeTicks > NO_LIFETIME;
        }
    }

    public record Dash(float forward, float upward, boolean towardTarget, float chance,
                       float onConnectRatio, float onConnectKnockback, int iframes) {
        public static final Codec<Dash> CODEC = Customs.wrap(Dash.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("forward", 0.6F).forGetter(Dash::forward),
                Codec.FLOAT.optionalFieldOf("upward", 0.0F).forGetter(Dash::upward),
                Codec.BOOL.optionalFieldOf("toward_target", false).forGetter(Dash::towardTarget),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Dash::chance),
                Codec.FLOAT.optionalFieldOf("on_connect_ratio", 0.0F).forGetter(Dash::onConnectRatio),
                Codec.FLOAT.optionalFieldOf("on_connect_knockback", 0.0F)
                        .forGetter(Dash::onConnectKnockback),
                Codec.INT.optionalFieldOf("iframes", 0).forGetter(Dash::iframes)
        ).apply(instance, Dash::new)));
    }

    public record Execute(float healthThreshold, float bonusRatio, boolean instantKill,
                          boolean repeatOnKill, Optional<DamageSpec> damageSpec) {
        public static final Codec<Execute> CODEC = Customs.wrap(Execute.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("health_threshold", 0.2F).forGetter(Execute::healthThreshold),
                Codec.FLOAT.optionalFieldOf("bonus_ratio", 1.0F).forGetter(Execute::bonusRatio),
                Codec.BOOL.optionalFieldOf("instant_kill", false).forGetter(Execute::instantKill),
                Codec.BOOL.optionalFieldOf("repeat_on_kill", false).forGetter(Execute::repeatOnKill),
                DamageSpec.CODEC.optionalFieldOf("damage_spec").forGetter(Execute::damageSpec)
        ).apply(instance, Execute::new)));
    }

    public record Chain(int maxTargets, float range, float falloff, float ratio,
                        boolean inherit, Optional<String> damageType,
                        Optional<String> school,
                        Optional<DamageSpec> damageSpec) implements Typed {
        public static final Codec<Chain> CODEC = Customs.wrap(Chain.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("max_targets", 3).forGetter(Chain::maxTargets),
                Codec.FLOAT.optionalFieldOf("range", 5.0F).forGetter(Chain::range),
                Codec.FLOAT.optionalFieldOf("falloff", 0.65F).forGetter(Chain::falloff),
                Codec.FLOAT.optionalFieldOf("ratio", 0.5F).forGetter(Chain::ratio),
                Codec.BOOL.optionalFieldOf("inherit", true).forGetter(Chain::inherit),
                Codec.STRING.optionalFieldOf("damage_type").forGetter(Chain::damageType),
                Codec.STRING.optionalFieldOf("school").forGetter(Chain::school),
                DamageSpec.CODEC.optionalFieldOf("damage_spec").forGetter(Chain::damageSpec)
        ).apply(instance, Chain::new)));
    }

    public record Momentum(float perHitRatio, int maxStacks, int decayTicks) {
        public static final Codec<Momentum> CODEC = Customs.wrap(Momentum.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("per_hit_ratio", 0.12F).forGetter(Momentum::perHitRatio),
                Codec.INT.optionalFieldOf("max_stacks", 5).forGetter(Momentum::maxStacks),
                Codec.INT.optionalFieldOf("decay_ticks", 60).forGetter(Momentum::decayTicks)
        ).apply(instance, Momentum::new)));
    }

    public record Requires(int minAttacks, List<String> styles, List<String> categories,
                           List<String> weaponCategories, int hitsLanded,
                           Optional<Boolean> sameTarget,
                           Optional<Boolean> sameStyle, String onFailure,
                           float perPreceding, float perPrecedingCap) {
        public static final String FAILURE_PLAIN = "plain";
        public static final String FAILURE_SKIP = "skip";

        public static final Requires NONE = new Requires(0, List.of(), List.of(), List.of(), 0,
                Optional.empty(), Optional.empty(), FAILURE_PLAIN, 0.0F, 0.0F);

        public static final Codec<Requires> CODEC = Customs.wrap(Requires.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("min_attacks", 0).forGetter(Requires::minAttacks),
                Codec.STRING.listOf().optionalFieldOf("styles", List.of()).forGetter(Requires::styles),
                Codec.STRING.listOf().optionalFieldOf("categories", List.of())
                        .forGetter(Requires::categories),
                Codec.STRING.listOf().optionalFieldOf("weapon_categories", List.of())
                        .forGetter(Requires::weaponCategories),
                Codec.INT.optionalFieldOf("hits_landed", 0).forGetter(Requires::hitsLanded),
                Codec.BOOL.optionalFieldOf("same_target").forGetter(Requires::sameTarget),
                Codec.BOOL.optionalFieldOf("same_style").forGetter(Requires::sameStyle),
                Codec.STRING.optionalFieldOf("on_failure", FAILURE_PLAIN).forGetter(Requires::onFailure),
                Codec.FLOAT.optionalFieldOf("per_preceding", 0.0F).forGetter(Requires::perPreceding),
                Codec.FLOAT.optionalFieldOf("per_preceding_cap", 0.0F)
                        .forGetter(Requires::perPrecedingCap)
        ).apply(instance, Requires::new)));

        public float scaleFor(int preceding) {
            if (perPreceding <= 0.0F || preceding <= 0) {
                return 1.0F;
            }
            float bonus = perPreceding * preceding;
            if (perPrecedingCap > 0.0F) {
                bonus = Math.min(bonus, perPrecedingCap);
            }
            return 1.0F + bonus;
        }

        public boolean isEmpty() {
            return minAttacks <= 0 && styles.isEmpty() && categories.isEmpty()
                    && weaponCategories.isEmpty() && hitsLanded <= 0
                    && sameTarget.isEmpty() && sameStyle.isEmpty();
        }
    }

    public record SlotRule(List<Integer> slots, Optional<Boolean> first, Optional<Boolean> last,
                           Optional<String> afterStyle, Optional<String> afterCategory,
                           Requires requires) {
        public static final SlotRule ANY = new SlotRule(List.of(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Requires.NONE);

        public static final Codec<SlotRule> CODEC = Customs.wrap(SlotRule.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.either(Codec.INT, Codec.INT.listOf())
                        .xmap(either -> either.map(List::of, list -> list),
                                list -> list.size() == 1
                                        ? Either.left(list.getFirst())
                                        : Either.right(list))
                        .optionalFieldOf("slot", List.of()).forGetter(SlotRule::slots),
                Codec.BOOL.optionalFieldOf("first").forGetter(SlotRule::first),
                Codec.BOOL.optionalFieldOf("last").forGetter(SlotRule::last),
                Codec.STRING.optionalFieldOf("after_style").forGetter(SlotRule::afterStyle),
                Codec.STRING.optionalFieldOf("after_category").forGetter(SlotRule::afterCategory),
                Requires.CODEC.optionalFieldOf("requires", Requires.NONE).forGetter(SlotRule::requires)
        ).apply(instance, SlotRule::new)));

        public boolean isEmpty() {
            return slots.isEmpty() && first.isEmpty() && last.isEmpty()
                    && afterStyle.isEmpty() && afterCategory.isEmpty() && requires.isEmpty();
        }
    }

    public record Condition(Optional<Float> selfHealthBelow, Optional<Float> selfHealthAbove,
                            Optional<Float> targetHealthBelow, Optional<Float> targetHealthAbove,
                            StateGate state, SlotRule slot, float multiplier,
                            boolean refundSlot,
                            Optional<Boolean> onMiss, List<String> damageType,
                            List<String> takenDamageType, List<String> targetEffects,
                            Optional<Boolean> targetBurning, Optional<String> crit,
                            Optional<String> critMissed) {
        public static final String KEY_TARGET_EFFECTS = "target_effects";
        public static final String KEY_TARGET_BURNING = "target_burning";
        public static final String KEY_CRIT = "crit";
        public static final String KEY_CRIT_MISSED = "crit_missed";

        public boolean needsTarget() {
            return targetHealthBelow.isPresent() || targetHealthAbove.isPresent()
                    || state.needsTarget() || !targetEffects.isEmpty() || targetBurning.isPresent();
        }

        public static final Condition NONE = new Condition(Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), StateGate.ANY, SlotRule.ANY, 1.0F,
                false, Optional.empty(), List.of(), List.of(), List.of(), Optional.empty(),
                Optional.empty(), Optional.empty());

        public static final Codec<Condition> CODEC = Customs.wrap(Condition.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("self_health_below").forGetter(Condition::selfHealthBelow),
                Codec.FLOAT.optionalFieldOf("self_health_above").forGetter(Condition::selfHealthAbove),
                Codec.FLOAT.optionalFieldOf("target_health_below").forGetter(Condition::targetHealthBelow),
                Codec.FLOAT.optionalFieldOf("target_health_above").forGetter(Condition::targetHealthAbove),
                StateGate.CODEC.forGetter(Condition::state),
                SlotRule.CODEC.optionalFieldOf("slot", SlotRule.ANY).forGetter(Condition::slot),
                Codec.FLOAT.optionalFieldOf("multiplier", 1.0F).forGetter(Condition::multiplier),
                Codec.BOOL.optionalFieldOf("refund_slot", false).forGetter(Condition::refundSlot),
                Codec.BOOL.optionalFieldOf("on_miss").forGetter(Condition::onMiss),
                Codec.STRING.listOf().optionalFieldOf("damage_type", List.of())
                        .forGetter(Condition::damageType),
                Codec.STRING.listOf().optionalFieldOf("taken_damage_type", List.of())
                        .forGetter(Condition::takenDamageType),
                Codec.STRING.listOf().optionalFieldOf(KEY_TARGET_EFFECTS, List.of())
                        .forGetter(Condition::targetEffects),
                Codec.BOOL.optionalFieldOf(KEY_TARGET_BURNING).forGetter(Condition::targetBurning),
                Codec.STRING.optionalFieldOf(KEY_CRIT).forGetter(Condition::crit),
                Codec.STRING.optionalFieldOf(KEY_CRIT_MISSED).forGetter(Condition::critMissed)
        ).apply(instance, Condition::new)));

        public Condition {
            state = state == null ? StateGate.ANY : state;
            targetEffects = targetEffects == null ? List.of() : List.copyOf(targetEffects);
            targetBurning = targetBurning == null ? Optional.empty() : targetBurning;
            crit = crit == null ? Optional.empty() : crit;
            critMissed = critMissed == null ? Optional.empty() : critMissed;
        }

        public boolean isEmpty() {
            return selfHealthBelow.isEmpty() && selfHealthAbove.isEmpty()
                    && targetHealthBelow.isEmpty() && targetHealthAbove.isEmpty()
                    && state.isEmpty() && slot.isEmpty() && !refundSlot
                    && onMiss.isEmpty() && damageType.isEmpty()
                    && takenDamageType.isEmpty() && targetEffects.isEmpty()
                    && targetBurning.isEmpty() && crit.isEmpty() && critMissed.isEmpty();
        }
    }

    public record Heal(float flat, float ratio, float chance, boolean overheal) {
        public static final Codec<Heal> CODEC = Customs.wrap(Heal.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("flat", 0.0F).forGetter(Heal::flat),
                Codec.FLOAT.optionalFieldOf("ratio", 0.0F).forGetter(Heal::ratio),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Heal::chance),
                Codec.BOOL.optionalFieldOf("overheal", false).forGetter(Heal::overheal)
        ).apply(instance, Heal::new)));
    }

    public record Disarm(int durabilityDamage, float slownessSeconds, float chance) {
        public static final Codec<Disarm> CODEC = Customs.wrap(Disarm.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("durability_damage", 0).forGetter(Disarm::durabilityDamage),
                Codec.FLOAT.optionalFieldOf("slowness_seconds", 0.0F).forGetter(Disarm::slownessSeconds),
                Codec.FLOAT.optionalFieldOf("chance", 0.25F).forGetter(Disarm::chance)
        ).apply(instance, Disarm::new)));
    }

    public record Vault(float radius, float strength, boolean upward, float chance) {
        public static final Codec<Vault> CODEC = Customs.wrap(Vault.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("radius", 3.0F).forGetter(Vault::radius),
                Codec.FLOAT.optionalFieldOf("strength", 0.5F).forGetter(Vault::strength),
                Codec.BOOL.optionalFieldOf("upward", true).forGetter(Vault::upward),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Vault::chance)
        ).apply(instance, Vault::new)));
    }

    public record AttributeBonus(String attribute, double amount, String operation,
                                 int duration, int maxStacks, boolean onTarget) {
        static Codec<String> attributeIdCodec() {
            return ATTRIBUTE_ID_CODEC;
        }

        private static final Codec<String> ATTRIBUTE_ID_CODEC = Codec.STRING.validate(id -> {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null) {
                return DataResult.error(() -> "Malformed attribute id: " + id);
            }
            if (!BuiltInRegistries.ATTRIBUTE.containsKey(parsed)) {
                return DataResult.error(() -> "Unknown attribute: " + id
                        + " (1.21.1 ids are prefixed, e.g. minecraft:generic.attack_speed)");
            }
            return DataResult.success(id);
        });

        public static final Codec<AttributeBonus> CODEC = Customs.wrap(AttributeBonus.class, RecordCodecBuilder.create(instance -> instance.group(
                ATTRIBUTE_ID_CODEC.fieldOf("attribute").forGetter(AttributeBonus::attribute),
                Codec.DOUBLE.fieldOf("amount").forGetter(AttributeBonus::amount),
                Codec.STRING.optionalFieldOf("operation", "add_value").forGetter(AttributeBonus::operation),
                Codec.INT.optionalFieldOf("duration", 60).forGetter(AttributeBonus::duration),
                Codec.INT.optionalFieldOf("max_stacks", 1).forGetter(AttributeBonus::maxStacks),
                Codec.BOOL.optionalFieldOf("on_target", false).forGetter(AttributeBonus::onTarget)
        ).apply(instance, AttributeBonus::new)));
    }

    public record AttackAttributeBonus(String attribute, double amount, String operation,
                                 boolean stacks) {
        public static final Codec<AttackAttributeBonus> CODEC = Customs.wrap(AttackAttributeBonus.class, RecordCodecBuilder.create(instance -> instance.group(
                AttributeBonus.attributeIdCodec().fieldOf("attribute").forGetter(AttackAttributeBonus::attribute),
                Codec.DOUBLE.fieldOf("amount").forGetter(AttackAttributeBonus::amount),
                Codec.STRING.optionalFieldOf("operation", "add_value").forGetter(AttackAttributeBonus::operation),
                Codec.BOOL.optionalFieldOf("stacks", true).forGetter(AttackAttributeBonus::stacks)
        ).apply(instance, AttackAttributeBonus::new)));
    }

    public record Cooldown(int ticks, boolean shared) {
        public static final Codec<Cooldown> CODEC = Customs.wrap(Cooldown.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("ticks").forGetter(Cooldown::ticks),
                Codec.BOOL.optionalFieldOf("shared", Boolean.FALSE).forGetter(Cooldown::shared)
        ).apply(instance, Cooldown::new)));

        public Cooldown(int ticks) {
            this(ticks, false);
        }
    }

    public record Resource(float health, int hunger, int experienceLevels, boolean cancelIfShort) {
        public static final Codec<Resource> CODEC = Customs.wrap(Resource.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("health", 0.0F).forGetter(Resource::health),
                Codec.INT.optionalFieldOf("hunger", 0).forGetter(Resource::hunger),
                Codec.INT.optionalFieldOf("experience_levels", 0).forGetter(Resource::experienceLevels),
                Codec.BOOL.optionalFieldOf("cancel_if_short", true).forGetter(Resource::cancelIfShort)
        ).apply(instance, Resource::new)));
    }

    public record Summon(String entity, int count, int lifespanTicks, float radius,
                         boolean inheritTarget, float damageRatio,
                         Optional<DamageSpec> damageSpec,
                         Optional<String> ability, float chance,
                         Optional<ProjectileSpawn> spawn, Optional<Integer> maxActive,
                         SummonEquipment equipment, SummonStats attributes,
                         boolean spectral, boolean keepGear, boolean ownDamage) {
        public static final String VICTIM = "@victim";

        public static final Codec<Summon> CODEC = Customs.wrap(Summon.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("entity").forGetter(Summon::entity),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Summon::count),
                Codec.INT.optionalFieldOf("lifespan", 200).forGetter(Summon::lifespanTicks),
                Codec.FLOAT.optionalFieldOf("radius", 2.0F).forGetter(Summon::radius),
                Codec.BOOL.optionalFieldOf("inherit_target", true).forGetter(Summon::inheritTarget),
                Codec.FLOAT.optionalFieldOf("damage_ratio", 0.0F).forGetter(Summon::damageRatio),
                DamageSpec.CODEC.optionalFieldOf("damage_spec").forGetter(Summon::damageSpec),
                Codec.STRING.optionalFieldOf("ability").forGetter(Summon::ability),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Summon::chance),
                ProjectileSpawn.CODEC.optionalFieldOf("spawn").forGetter(Summon::spawn),
                Codec.INT.optionalFieldOf("max_active").forGetter(Summon::maxActive),
                SummonEquipment.CODEC.optionalFieldOf("equipment", SummonEquipment.NONE)
                        .forGetter(Summon::equipment),
                SummonStats.CODEC.optionalFieldOf("attributes", SummonStats.NONE)
                        .forGetter(Summon::attributes),
                Codec.BOOL.optionalFieldOf("spectral", Boolean.FALSE).forGetter(Summon::spectral),
                Codec.BOOL.optionalFieldOf("keep_gear", Boolean.FALSE).forGetter(Summon::keepGear),
                Codec.BOOL.optionalFieldOf("own_damage", Boolean.FALSE).forGetter(Summon::ownDamage)
        ).apply(instance, Summon::new)));

        public Summon {
            equipment = equipment == null ? SummonEquipment.NONE : equipment;
            attributes = attributes == null ? SummonStats.NONE : attributes;
        }

        public boolean raisesVictim() {
            return VICTIM.equals(entity);
        }

        public Summon asEntity(String id) {
            return Customs.copy(this, new Summon(id, count, lifespanTicks, radius, inheritTarget, damageRatio,
                    damageSpec, ability, chance, spawn, maxActive, equipment, attributes,
                    spectral, keepGear, ownDamage));
        }
    }

    public record Command(String command, boolean asServer) {
        public static final Codec<Command> CODEC = Customs.wrap(Command.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("command").forGetter(Command::command),
                Codec.BOOL.optionalFieldOf("as_server", true).forGetter(Command::asServer)
        ).apply(instance, Command::new)));
    }

    public record Blink(float distance, boolean behindTarget, int iframes) {
        public static final Codec<Blink> CODEC = Customs.wrap(Blink.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("distance", 3.0F).forGetter(Blink::distance),
                Codec.BOOL.optionalFieldOf("behind_target", true).forGetter(Blink::behindTarget),
                Codec.INT.optionalFieldOf("iframes", 0).forGetter(Blink::iframes)
        ).apply(instance, Blink::new)));
    }

    public record Shield(float amount, int duration, float dodgeChance, int dodgeDuration) {
        public static final Codec<Shield> CODEC = Customs.wrap(Shield.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("amount", 4.0F).forGetter(Shield::amount),
                Codec.INT.optionalFieldOf("duration", 100).forGetter(Shield::duration),
                Codec.FLOAT.optionalFieldOf("dodge_chance", 0.0F).forGetter(Shield::dodgeChance),
                Codec.INT.optionalFieldOf("dodge_duration", 0).forGetter(Shield::dodgeDuration)
        ).apply(instance, Shield::new)));
    }

    public record Cleave(float radius, float ratio, int maxTargets,
                         boolean inherit, Optional<String> damageType,
                         Optional<String> school,
                         Optional<DamageSpec> damageSpec) implements Typed {
        public static final Codec<Cleave> CODEC = Customs.wrap(Cleave.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("radius", 3.0F).forGetter(Cleave::radius),
                Codec.FLOAT.optionalFieldOf("ratio", 0.5F).forGetter(Cleave::ratio),
                Codec.INT.optionalFieldOf("max_targets", 3).forGetter(Cleave::maxTargets),
                Codec.BOOL.optionalFieldOf("inherit", true).forGetter(Cleave::inherit),
                Codec.STRING.optionalFieldOf("damage_type").forGetter(Cleave::damageType),
                Codec.STRING.optionalFieldOf("school").forGetter(Cleave::school),
                DamageSpec.CODEC.optionalFieldOf("damage_spec").forGetter(Cleave::damageSpec)
        ).apply(instance, Cleave::new)));
    }

    public record Motion(Optional<Dash> dash,
                         Optional<Blink> blink,
                         int delayTicks) implements Delayed<Motion> {
        public static final Motion NONE =
                new Motion(Optional.empty(), Optional.empty(), 0);

        public static final Codec<Motion> CODEC = Customs.wrap(Motion.class, RecordCodecBuilder.create(instance -> instance.group(
                Dash.CODEC.optionalFieldOf("dash").forGetter(Motion::dash),
                Blink.CODEC.optionalFieldOf("blink").forGetter(Motion::blink),
                Codec.INT.optionalFieldOf("delay_ticks", 0).forGetter(Motion::delayTicks)
        ).apply(instance, Motion::new)));

        @Override
        public Motion withoutDelay() {
            return delayTicks == 0 ? this : new Motion(dash, blink, 0);
        }

        public boolean isEmpty() {
            return dash.isEmpty() && blink.isEmpty();
        }
    }

    public record Offense(Optional<Execute> execute,
                          Optional<Chain> chain,
                          Optional<Cleave> cleave,
                          Optional<Momentum> momentum,
                          int delayTicks) implements Delayed<Offense> {
        public static final Offense NONE = new Offense(Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), 0);

        public static final Codec<Offense> CODEC = Customs.wrap(Offense.class, RecordCodecBuilder.create(instance -> instance.group(
                Execute.CODEC.optionalFieldOf("execute").forGetter(Offense::execute),
                Chain.CODEC.optionalFieldOf("chain").forGetter(Offense::chain),
                Cleave.CODEC.optionalFieldOf("cleave").forGetter(Offense::cleave),
                Momentum.CODEC.optionalFieldOf("momentum").forGetter(Offense::momentum),
                Codec.INT.optionalFieldOf("delay_ticks", 0).forGetter(Offense::delayTicks)
        ).apply(instance, Offense::new)));

        @Override
        public Offense withoutDelay() {
            return delayTicks == 0 ? this : new Offense(execute, chain, cleave, momentum, 0);
        }

        public boolean isEmpty() {
            return execute.isEmpty() && chain.isEmpty() && cleave.isEmpty() && momentum.isEmpty();
        }
    }

    public record Sustain(Optional<Heal> heal,
                          Optional<Shield> shield,
                          int delayTicks) implements Delayed<Sustain> {
        public static final Sustain NONE = new Sustain(Optional.empty(), Optional.empty(), 0);

        public static final Codec<Sustain> CODEC = Customs.wrap(Sustain.class, RecordCodecBuilder.create(instance -> instance.group(
                Heal.CODEC.optionalFieldOf("heal").forGetter(Sustain::heal),
                Shield.CODEC.optionalFieldOf("shield").forGetter(Sustain::shield),
                Codec.INT.optionalFieldOf("delay_ticks", 0).forGetter(Sustain::delayTicks)
        ).apply(instance, Sustain::new)));

        @Override
        public Sustain withoutDelay() {
            return delayTicks == 0 ? this : new Sustain(heal, shield, 0);
        }

        public boolean isEmpty() {
            return heal.isEmpty() && shield.isEmpty();
        }
    }

    public record Hasten(float ratio, int swings, float chance, float thisSwing) {

        public static final Codec<Hasten> CODEC = Customs.wrap(Hasten.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("ratio", 0.5F).forGetter(Hasten::ratio),
                Codec.INT.optionalFieldOf("swings", 1).forGetter(Hasten::swings),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Hasten::chance),
                Codec.FLOAT.optionalFieldOf("this_swing", 1.0F).forGetter(Hasten::thisSwing)
        ).apply(instance, Hasten::new)));
    }

    public record Control(Optional<Disarm> disarm,
                          List<AttributeBonus> attributes,
                          List<AttackAttributeBonus> attackAttributes,
                          Optional<Hasten> hasten,
                          Optional<Vault> vault,
                          int delayTicks) implements Delayed<Control> {
        public static final Control NONE =
                new Control(Optional.empty(), List.of(), List.of(), Optional.empty(),
                        Optional.empty(), 0);

        public static final Codec<Control> CODEC = Customs.wrap(Control.class, RecordCodecBuilder.create(instance -> instance.group(
                Disarm.CODEC.optionalFieldOf("disarm").forGetter(Control::disarm),
                AttributeBonus.CODEC.listOf().optionalFieldOf("attributes", List.of())
                        .forGetter(Control::attributes),
                AttackAttributeBonus.CODEC.listOf().optionalFieldOf("attack_attributes", List.of())
                        .forGetter(Control::attackAttributes),
                Hasten.CODEC.optionalFieldOf("hasten").forGetter(Control::hasten),
                Vault.CODEC.optionalFieldOf("vault").forGetter(Control::vault),
                Codec.INT.optionalFieldOf("delay_ticks", 0).forGetter(Control::delayTicks)
        ).apply(instance, Control::new)));

        @Override
        public Control withoutDelay() {
            return delayTicks == 0 ? this
                    : new Control(disarm, attributes, attackAttributes, hasten, vault, 0);
        }

        public boolean isEmpty() {
            return disarm.isEmpty() && attributes.isEmpty() && attackAttributes.isEmpty()
                    && hasten.isEmpty() && vault.isEmpty();
        }
    }

    public record Quicken(float ratio, float chance) {

        public static final Codec<Quicken> CODEC = Customs.wrap(Quicken.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("ratio", 0.1F).forGetter(Quicken::ratio),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Quicken::chance)
        ).apply(instance, Quicken::new)));
    }

    public record Order(String ability,
                        float damageRatio,
                        float chance,
                        SummonTargeting targets,
                        Optional<AbilityTuning> tuning,
                        int delayTicks,
                        Optional<SummonAim> aim,
                        Optional<Quicken> quicken) implements Delayed<Order> {
        public static final Order NONE = new Order("", 0.0F, 1.0F,
                SummonTargeting.ALL, Optional.empty(), 0, Optional.empty(), Optional.empty());

        public static final Codec<Order> CODEC = Customs.wrap(Order.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("ability", "").forGetter(Order::ability),
                Codec.FLOAT.optionalFieldOf("damage_ratio", 0.0F).forGetter(Order::damageRatio),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Order::chance),
                SummonTargeting.CODEC.optionalFieldOf("targets", SummonTargeting.ALL)
                        .forGetter(Order::targets),
                AbilityTuning.CODEC.optionalFieldOf("tuning").forGetter(Order::tuning),
                Codec.INT.optionalFieldOf("delay_ticks", 0).forGetter(Order::delayTicks),
                SummonAim.CODEC.optionalFieldOf(SummonCadence.KEY_AIM).forGetter(Order::aim),
                Quicken.CODEC.optionalFieldOf("quicken").forGetter(Order::quicken)
        ).apply(instance, Order::new)));

        @Override
        public Order withoutDelay() {
            return delayTicks == 0
                    ? this
                    : new Order(ability, damageRatio, chance, targets, tuning, 0, aim, quicken);
        }

        public boolean isEmpty() {
            return ability.isBlank() && quicken.isEmpty();
        }

        public boolean ordersAbility() {
            return !ability.isBlank();
        }
    }

    public record Spawn(List<Summon> summons,
                        List<Command> commands,
                        int delayTicks,
                        Optional<RaiseRules> raiseDead) implements Delayed<Spawn> {
        public static final Spawn NONE = new Spawn(List.of(), List.of(), 0, Optional.empty());

        public static final Codec<Spawn> CODEC = Customs.wrap(Spawn.class, RecordCodecBuilder.create(instance -> instance.group(
                Summon.CODEC.listOf().optionalFieldOf("summons", List.of()).forGetter(Spawn::summons),
                Command.CODEC.listOf().optionalFieldOf("commands", List.of()).forGetter(Spawn::commands),
                Codec.INT.optionalFieldOf("delay_ticks", 0).forGetter(Spawn::delayTicks),
                RaiseRules.CODEC.optionalFieldOf(RaiseRules.KEY).forGetter(Spawn::raiseDead)
        ).apply(instance, Spawn::new)));

        public Spawn {
            raiseDead = raiseDead == null ? Optional.empty() : raiseDead;
        }

        @Override
        public Spawn withoutDelay() {
            return delayTicks == 0 ? this : new Spawn(summons, commands, 0, raiseDead);
        }

        public boolean isEmpty() {
            return summons.isEmpty() && commands.isEmpty() && raiseDead.isEmpty();
        }
    }

    public record Mechanics(Motion motion,
                            Offense offense,
                            Sustain sustain,
                            Control control,
                            Spawn spawn,
                            Order order,
                            Optional<Cooldown> cooldown,
                            Optional<Resource> cost,
                            Condition condition,
                            int maxTargetsPerSwing) {
        public static final int UNLIMITED_TARGETS = 0;

        public static final Mechanics NONE = new Mechanics(Motion.NONE, Offense.NONE,
                Sustain.NONE, Control.NONE, Spawn.NONE, Order.NONE,
                Optional.empty(), Optional.empty(), Condition.NONE, UNLIMITED_TARGETS);

        public boolean capsTargets() {
            return maxTargetsPerSwing > UNLIMITED_TARGETS;
        }

        public static final Codec<Mechanics> CODEC = Customs.wrap(Mechanics.class, RecordCodecBuilder.create(instance -> instance.group(
                Motion.CODEC.optionalFieldOf("motion", Motion.NONE).forGetter(Mechanics::motion),
                Offense.CODEC.optionalFieldOf("offense", Offense.NONE).forGetter(Mechanics::offense),
                Sustain.CODEC.optionalFieldOf("sustain", Sustain.NONE).forGetter(Mechanics::sustain),
                Control.CODEC.optionalFieldOf("control", Control.NONE).forGetter(Mechanics::control),
                Spawn.CODEC.optionalFieldOf("spawn", Spawn.NONE).forGetter(Mechanics::spawn),
                Order.CODEC.optionalFieldOf("order", Order.NONE).forGetter(Mechanics::order),
                Cooldown.CODEC.optionalFieldOf("cooldown").forGetter(Mechanics::cooldown),
                Resource.CODEC.optionalFieldOf("cost").forGetter(Mechanics::cost),
                Condition.CODEC.optionalFieldOf("condition", Condition.NONE).forGetter(Mechanics::condition),
                Codec.INT.optionalFieldOf("max_targets_per_swing", UNLIMITED_TARGETS)
                        .forGetter(Mechanics::maxTargetsPerSwing)
        ).apply(instance, Mechanics::new)));

        public Optional<Dash> dash() {
            return motion.dash();
        }

        public Optional<Blink> blink() {
            return motion.blink();
        }

        public Optional<Vault> vault() {
            return control.vault();
        }

        public Optional<Execute> execute() {
            return offense.execute();
        }

        public Optional<Chain> chain() {
            return offense.chain();
        }

        public Optional<Cleave> cleave() {
            return offense.cleave();
        }

        public Optional<Momentum> momentum() {
            return offense.momentum();
        }

        public Optional<Heal> heal() {
            return sustain.heal();
        }

        public Optional<Shield> shield() {
            return sustain.shield();
        }

        public Optional<Disarm> disarm() {
            return control.disarm();
        }

        public List<AttributeBonus> attributes() {
            return control.attributes();
        }

        public List<AttackAttributeBonus> attackAttributes() {
            return control.attackAttributes();
        }

        public List<Summon> summons() {
            return spawn.summons();
        }

        public Optional<RaiseRules> raiseDead() {
            return spawn.raiseDead();
        }

        public List<Command> commands() {
            return spawn.commands();
        }

        public Mechanics withCooldown(Cooldown value) {
            return new Mechanics(motion, offense, sustain, control, spawn, order,
                    Optional.ofNullable(value), cost, condition, maxTargetsPerSwing);
        }

        public boolean isEmpty() {
            return motion.isEmpty() && offense.isEmpty() && sustain.isEmpty()
                    && control.isEmpty() && spawn.isEmpty() && order.isEmpty();
        }
    }

    public record Echo(int times, int delayTicks, float ratio,
                       Optional<DamageSpec> damageSpec) {

        private static final int SAMPLE_ECHO_DELAY = 10;

        public static final Echo SAMPLE = new Echo(1, SAMPLE_ECHO_DELAY, 1.0F, Optional.empty());

        public static final Codec<Echo> CODEC = Customs.wrap(Echo.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("times", 1).forGetter(Echo::times),
                Codec.INT.optionalFieldOf("delay_ticks", 10).forGetter(Echo::delayTicks),
                Codec.FLOAT.optionalFieldOf("ratio", 1.0F).forGetter(Echo::ratio),
                DamageSpec.CODEC.optionalFieldOf("damage_spec").forGetter(Echo::damageSpec)
        ).apply(instance, Echo::new)));
    }

    public record OnHit(Optional<Damage> damage,
                        List<Damage> damages,
                        List<StatusEffect> statusEffects,
                        Optional<Ignite> ignite,
                        Optional<Lifesteal> lifesteal,
                        Optional<Knockback> knockback,
                        Optional<Splash> splash,
                        List<SelfEffect> selfEffects,
                        Optional<Sound> sound,
                        List<Particles> particles,
                        List<Projectile> projectiles,
                        List<CastSpell> castSpells,
                        Optional<SlashWave> slashWave,
                        Mechanics mechanics,
                        float freezeTicks,
                        int delayTicks,
                        Optional<Echo> echo) {
        private static final String ECHO_KEY = "echo";

        public static final OnHit EMPTY = new OnHit(Optional.empty(), List.of(), List.of(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(), Optional.empty(), List.of(), List.of(), List.of(), Optional.empty(),
                Mechanics.NONE, 0.0F, 0, Optional.empty());

        public List<Damage> allDamage() {
            if (damages.isEmpty()) {
                return damage.map(List::of).orElseGet(List::of);
            }
            if (damage.isEmpty()) {
                return damages;
            }
            List<Damage> all = new ArrayList<>(damages.size() + 1);
            all.add(damage.get());
            all.addAll(damages);
            return List.copyOf(all);
        }

        public OnHit withEcho(Optional<Echo> value) {
            return ScaledNumbers.carry(this, Customs.copy(this, new OnHit(damage, damages, statusEffects,
                    ignite, lifesteal, knockback, splash, selfEffects, sound, particles, projectiles,
                    castSpells, slashWave, mechanics, freezeTicks, delayTicks, value)),
                    path -> path.startsWith(ECHO_KEY));
        }

        public Optional<Dash> dash() {
            return mechanics.dash();
        }

        public Optional<Execute> execute() {
            return mechanics.execute();
        }

        public Optional<Chain> chain() {
            return mechanics.chain();
        }

        public Optional<Momentum> momentum() {
            return mechanics.momentum();
        }

        public Optional<Heal> heal() {
            return mechanics.heal();
        }

        public Optional<Disarm> disarm() {
            return mechanics.disarm();
        }

        public Optional<Vault> vault() {
            return mechanics.vault();
        }

        public Optional<Blink> blink() {
            return mechanics.blink();
        }

        public Optional<Shield> shield() {
            return mechanics.shield();
        }

        public Optional<Cleave> cleave() {
            return mechanics.cleave();
        }

        public Condition condition() {
            return mechanics.condition();
        }

        public List<AttributeBonus> attributes() {
            return mechanics.attributes();
        }

        public List<AttackAttributeBonus> attackAttributes() {
            return mechanics.attackAttributes();
        }

        public Optional<Cooldown> cooldown() {
            return mechanics.cooldown();
        }

        public Optional<Resource> cost() {
            return mechanics.cost();
        }

        public List<Summon> summons() {
            return mechanics.summons();
        }

        public Optional<RaiseRules> raiseDead() {
            return mechanics.raiseDead();
        }

        public List<Command> commands() {
            return mechanics.commands();
        }

        private record Impact(Optional<Damage> damage, List<Damage> damages,
                              List<StatusEffect> statusEffects,
                              Optional<Ignite> ignite, Optional<Lifesteal> lifesteal,
                              Optional<Knockback> knockback, Optional<Splash> splash,
                              float freezeTicks) {
        }

        private record Presentation(List<SelfEffect> selfEffects, Optional<Sound> sound,
                                    List<Particles> particles, List<Projectile> projectiles,
                                    List<CastSpell> castSpells,
                                    Optional<SlashWave> slashWave, Mechanics mechanics,
                                    int delayTicks, Optional<Echo> echo) {
        }

        private static final MapCodec<Impact> IMPACT_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Damage.CODEC.optionalFieldOf("damage").forGetter(Impact::damage),
                Damage.CODEC.listOf().optionalFieldOf("damages", List.of()).forGetter(Impact::damages),
                StatusEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Impact::statusEffects),
                Ignite.CODEC.optionalFieldOf("ignite").forGetter(Impact::ignite),
                Lifesteal.CODEC.optionalFieldOf("lifesteal").forGetter(Impact::lifesteal),
                Knockback.CODEC.optionalFieldOf("knockback").forGetter(Impact::knockback),
                Splash.CODEC.optionalFieldOf("splash").forGetter(Impact::splash),
                Codec.FLOAT.optionalFieldOf("freeze_ticks", 0.0F).forGetter(Impact::freezeTicks)
        ).apply(instance, Impact::new));

        private static final MapCodec<Presentation> PRESENTATION_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                SelfEffect.CODEC.listOf().optionalFieldOf("self_effects", List.of()).forGetter(Presentation::selfEffects),
                Sound.CODEC.optionalFieldOf("sound").forGetter(Presentation::sound),
                Particles.CODEC.listOf().optionalFieldOf("particles", List.of()).forGetter(Presentation::particles),
                Projectile.CODEC.listOf().optionalFieldOf("projectiles", List.of()).forGetter(Presentation::projectiles),
                CastSpell.CODEC.listOf().optionalFieldOf("cast_spells", List.of()).forGetter(Presentation::castSpells),
                SlashWave.CODEC.optionalFieldOf("slash_wave").forGetter(Presentation::slashWave),
                Mechanics.CODEC.optionalFieldOf("mechanics", Mechanics.NONE).forGetter(Presentation::mechanics),
                Codec.INT.optionalFieldOf("delay_ticks", 0).forGetter(Presentation::delayTicks),
                Echo.CODEC.optionalFieldOf(ECHO_KEY).forGetter(Presentation::echo)
        ).apply(instance, Presentation::new));

        public static final Codec<OnHit> CODEC = Customs.wrap(OnHit.class, ScaledNumbers.wrap(RecordCodecBuilder.create(instance -> instance.group(
                IMPACT_CODEC.forGetter(hit -> new Impact(hit.damage(), hit.damages(),
                        hit.statusEffects(), hit.ignite(), hit.lifesteal(), hit.knockback(),
                        hit.splash(), hit.freezeTicks())),
                PRESENTATION_CODEC.forGetter(hit -> new Presentation(hit.selfEffects(), hit.sound(),
                        hit.particles(), hit.projectiles(), hit.castSpells(), hit.slashWave(),
                        hit.mechanics(),
                        hit.delayTicks(), hit.echo()))
        ).apply(instance, (impact, presentation) -> new OnHit(
                impact.damage(), impact.damages(), impact.statusEffects(), impact.ignite(),
                impact.lifesteal(), impact.knockback(), impact.splash(),
                presentation.selfEffects(), presentation.sound(), presentation.particles(),
                presentation.projectiles(), presentation.castSpells(), presentation.slashWave(),
                presentation.mechanics(),
                impact.freezeTicks(), presentation.delayTicks(), presentation.echo())))));

        public boolean isEmpty() {
            return damage.isEmpty() && damages.isEmpty() && statusEffects.isEmpty() && ignite.isEmpty()
                    && lifesteal.isEmpty() && knockback.isEmpty() && splash.isEmpty()
                    && selfEffects.isEmpty() && sound.isEmpty() && particles.isEmpty()
                    && projectiles.isEmpty() && castSpells.isEmpty() && slashWave.isEmpty() && mechanics.isEmpty()
                    && freezeTicks <= 0.0F;
        }
    }
}
