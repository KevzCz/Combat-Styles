package net.pixeldreamstudios.combat_styles.config;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class AbilityConfig {

    public static final String FOLDER = "abilities";

    private static final Set<String> LEGACY_DAMAGE_FIELDS = Set.of("baseDamage",
            "weaponDamageRatio", "perPoint", "scalesWith", "school", "damageType");

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .addSerializationExclusionStrategy(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes field) {
                    return field.getDeclaringClass() == Damage.class
                            && LEGACY_DAMAGE_FIELDS.contains(field.getName());
                }

                @Override
                public boolean shouldSkipClass(Class<?> type) {
                    return false;
                }
            })
            .setPrettyPrinting()
            .create();

    private static final String EXTENSION = ".json";
    private static final Map<String, Entry> LOADED = new LinkedHashMap<>();
    private static final Map<String, Entry> DECLARED = new LinkedHashMap<>();
    private static final Map<String, Entry> DATA = new LinkedHashMap<>();
    private static boolean loadedOnce;

    public static final class Damage {
        private static final String SPEC_BASE = "base";
        private static final String SPEC_OF_HIT = "of_hit";
        private static final String SPEC_TYPE = "type";
        private static final String SPEC_SCHOOL = "school";
        private static final String SPEC_SCALING = "scaling";

        public float baseDamage;
        public float weaponDamageRatio;
        public double perPoint;
        public String scalesWith;
        public String school;
        public int hits = 1;
        public String applies;
        public int appliesStacks = 1;
        public float appliesChance = 1.0F;
        public float lifesteal;
        public boolean detonates;
        public JsonObject damageSpec;
        public String damageType;

        Damage copy() {
            Damage made = new Damage();
            made.baseDamage = baseDamage;
            made.weaponDamageRatio = weaponDamageRatio;
            made.perPoint = perPoint;
            made.scalesWith = scalesWith;
            made.school = school;
            made.hits = hits;
            made.applies = applies;
            made.appliesStacks = appliesStacks;
            made.appliesChance = appliesChance;
            made.lifesteal = lifesteal;
            made.detonates = detonates;
            made.damageSpec = damageSpec == null ? null : damageSpec.deepCopy();
            made.damageType = damageType;
            return made;
        }

        public void useSpec(DamageSpec spec) {
            damageSpec = encoded(spec);
        }

        public void mergeSpec(DamageSpec patch) {
            mergeSpec(encoded(patch));
        }

        public void mergeSpec(JsonObject patch) {
            if (patch == null) {
                return;
            }
            if (damageSpec == null) {
                adoptSpec();
            }
            for (Map.Entry<String, JsonElement> entry : patch.entrySet()) {
                damageSpec.add(entry.getKey(), entry.getValue().deepCopy());
            }
        }

        public static JsonObject specPatch(DamageSpec patch) {
            return encoded(patch);
        }

        public static DamageSpec scaledSpec(float base, float ofHit, double perPoint) {
            Damage made = new Damage();
            made.baseDamage = base;
            made.weaponDamageRatio = ofHit;
            made.perPoint = perPoint;
            return made.legacySpec();
        }

        public DamageSpec legacySpec() {
            String source = scalesWith == null ? SpellCompatState.damageSource() : scalesWith;
            List<DamageSpec.ScalingEntry> scaling = perPoint > 0.0D
                    ? List.of(new DamageSpec.ScalingEntry(source, (float) perPoint, Optional.empty(),
                            DamageSpec.ScalingKind.MULTIPLIER, DamageSpec.ScalingEntry.DEFAULT_BASELINE))
                    : List.of();
            return new DamageSpec(baseDamage, weaponDamageRatio,
                    DamageSpec.NO_HEALTH_RATIO, DamageSpec.NO_HEALTH_RATIO, DamageSpec.NO_HEALTH_RATIO,
                    Optional.ofNullable(damageType == null || damageType.isBlank() ? null : damageType),
                    Optional.of(school == null ? SpellCompatState.damageSource() : school),
                    scaling, DamageSpec.ScalingMode.SUM, List.of(),
                    Optional.empty(), Optional.empty(), DamageSpec.FULL_MULTIPLIER);
        }

        public void adoptSpec() {
            if (damageSpec == null) {
                damageSpec = encoded(legacySpec());
            } else {
                JsonObject legacy = encoded(legacySpec());
                if (baseDamage > 0.0F) {
                    damageSpec.add(SPEC_BASE, legacy.get(SPEC_BASE));
                }
                if (weaponDamageRatio > 0.0F) {
                    damageSpec.add(SPEC_OF_HIT, legacy.get(SPEC_OF_HIT));
                }
                if (perPoint > 0.0D) {
                    damageSpec.add(SPEC_SCALING, legacy.get(SPEC_SCALING));
                }
                if (damageType != null && !damageType.isBlank()) {
                    damageSpec.add(SPEC_TYPE, legacy.get(SPEC_TYPE));
                }
                if (school != null && !damageSpec.has(SPEC_SCHOOL)) {
                    damageSpec.add(SPEC_SCHOOL, legacy.get(SPEC_SCHOOL));
                }
            }
            baseDamage = 0.0F;
            weaponDamageRatio = 0.0F;
            perPoint = 0.0D;
            scalesWith = null;
            school = null;
            damageType = null;
        }

        private static JsonObject encoded(DamageSpec spec) {
            return DamageSpec.CODEC.encodeStart(JsonOps.INSTANCE, spec).result()
                    .filter(JsonElement::isJsonObject)
                    .map(JsonElement::getAsJsonObject)
                    .orElseGet(JsonObject::new);
        }
    }

    public static final class Timing {
        public int durationTicks;
        public int windupTicks;
        public int cooldownTicks;

        Timing copy() {
            Timing made = new Timing();
            made.durationTicks = durationTicks;
            made.windupTicks = windupTicks;
            made.cooldownTicks = cooldownTicks;
            return made;
        }
    }

    public static final class Shape {
        public double radius;
        public double range;
        public double rise;
        public double pull;
        public double lift;
        public int maxTargets = 1;

        Shape copy() {
            Shape made = new Shape();
            made.radius = radius;
            made.range = range;
            made.rise = rise;
            made.pull = pull;
            made.lift = lift;
            made.maxTargets = maxTargets;
            return made;
        }
    }

    public static final class Presentation {
        public int particles;
        public double spread;
        public float volume;
        public float pitch;

        Presentation copy() {
            Presentation made = new Presentation();
            made.particles = particles;
            made.spread = spread;
            made.volume = volume;
            made.pitch = pitch;
            return made;
        }
    }

    public static final class Entry {
        public Damage damage = new Damage();
        public Timing timing = new Timing();
        public Shape shape = new Shape();
        public Presentation fx = new Presentation();
        public Map<String, Double> extra = new LinkedHashMap<>();
        public JsonObject options;
        public transient Map<Integer, JsonObject> stepSpecs;

        public void mergeStepSpec(int step, DamageSpec patch) {
            if (stepSpecs == null) {
                stepSpecs = new LinkedHashMap<>();
            }
            JsonObject merged = stepSpecs.computeIfAbsent(step, ignored -> new JsonObject());
            for (Map.Entry<String, JsonElement> entry : Damage.specPatch(patch).entrySet()) {
                merged.add(entry.getKey(), entry.getValue().deepCopy());
            }
        }

        public JsonObject stepSpec(int step) {
            return stepSpecs == null ? null : stepSpecs.get(step);
        }

        public <T> void useOptions(Codec<T> codec, T value) {
            options = codec.encodeStart(JsonOps.INSTANCE, value).result()
                    .filter(JsonElement::isJsonObject)
                    .map(JsonElement::getAsJsonObject)
                    .orElse(null);
        }

        public void mergeOptions(JsonObject patch) {
            if (patch == null) {
                return;
            }
            if (options == null) {
                options = new JsonObject();
            }
            for (Map.Entry<String, JsonElement> entry : patch.entrySet()) {
                options.add(entry.getKey(), entry.getValue().deepCopy());
            }
        }

        public <T> T options(Codec<T> codec, T fallback) {
            return options == null
                    ? fallback
                    : codec.parse(JsonOps.INSTANCE, options).result().orElse(fallback);
        }

        public Map<String, Double> options() {
            return extra == null ? Map.of() : Map.copyOf(extra);
        }

        public double extra(String option, double fallback) {
            Double found = extra == null ? null : extra.get(option);
            return found == null ? fallback : found;
        }

        public int extraInt(String option, int fallback) {
            return (int) extra(option, fallback);
        }

        public float extraFloat(String option, float fallback) {
            return (float) extra(option, fallback);
        }

        public boolean extraFlag(String option, boolean fallback) {
            return extra(option, fallback ? 1.0D : 0.0D) > 0.0D;
        }

        public boolean declares(String option) {
            return extra != null && extra.containsKey(option);
        }

        public Entry with(String option, double value) {
            if (extra == null) {
                extra = new LinkedHashMap<>();
            }
            extra.put(option, value);
            stamp = 0;
            return this;
        }

        public boolean marks() {
            return damage != null && damage.applies != null && !damage.applies.isBlank();
        }

        public boolean detonates() {
            return damage != null && damage.detonates;
        }

        public boolean heals() {
            return damage != null && damage.lifesteal > 0.0F;
        }

        public int hits() {
            return damage == null ? 1 : Math.max(1, damage.hits);
        }

        public int cooldown() {
            return timing == null ? 0 : Math.max(0, timing.cooldownTicks);
        }

        public String damageType() {
            return asDamageSpec().damageType().filter(type -> !type.isBlank()).orElse(null);
        }

        public String school() {
            return asDamageSpec().school().filter(named -> !named.isBlank()).orElse(null);
        }

        public int windup() {
            return timing == null ? 0 : Math.max(0, timing.windupTicks);
        }

        public int duration() {
            return timing == null ? 0 : Math.max(0, timing.durationTicks);
        }

        public int targets() {
            return shape == null ? 1 : Math.max(1, shape.maxTargets);
        }

        public double reach(double fallback) {
            return shape == null || shape.range <= 0.0D ? fallback : shape.range;
        }

        public double area(double fallback) {
            return shape == null || shape.radius <= 0.0D ? fallback : shape.radius;
        }

        public Entry copy() {
            Entry made = new Entry();
            made.damage = damage == null ? new Damage() : damage.copy();
            made.timing = timing == null ? new Timing() : timing.copy();
            made.shape = shape == null ? new Shape() : shape.copy();
            made.fx = fx == null ? new Presentation() : fx.copy();
            made.extra = extra == null ? new LinkedHashMap<>() : new LinkedHashMap<>(extra);
            made.options = options == null ? null : options.deepCopy();
            if (stepSpecs != null) {
                made.stepSpecs = new LinkedHashMap<>();
                for (Map.Entry<Integer, JsonObject> entry : stepSpecs.entrySet()) {
                    made.stepSpecs.put(entry.getKey(), entry.getValue().deepCopy());
                }
            }
            made.stamp = 0;
            return made;
        }

        public int fingerprint() {
            if (stamp == 0) {
                stamp = Objects.hash(damage.baseDamage, damage.weaponDamageRatio,
                        damage.perPoint, damage.scalesWith, damage.school, damage.hits,
                        damage.applies, damage.appliesStacks, damage.appliesChance,
                        damage.lifesteal, damage.detonates, damage.damageSpec,
                        damage.damageType, timing.durationTicks,
                        timing.windupTicks, timing.cooldownTicks, shape.radius, shape.range, shape.rise,
                        shape.pull, shape.lift, shape.maxTargets, fx.particles,
                        fx.spread, fx.volume, fx.pitch, extra, options);
            }
            return stamp;
        }

        private transient int stamp;
        private transient DamageSpec parsedSpec;
        private transient JsonObject parsedSpecSource;

        public Entry sane() {
            stamp = 0;
            damage.baseDamage = Math.max(0.0F, damage.baseDamage);
            damage.weaponDamageRatio = Math.max(0.0F, damage.weaponDamageRatio);
            damage.perPoint = Math.max(0.0D, damage.perPoint);
            damage.adoptSpec();
            damage.hits = Math.max(1, damage.hits);
            damage.appliesStacks = Math.max(1, damage.appliesStacks);
            damage.appliesChance = Math.clamp(damage.appliesChance, 0.0F, 1.0F);
            damage.lifesteal = Math.max(0.0F, damage.lifesteal);
            timing.durationTicks = Math.max(0, timing.durationTicks);
            timing.windupTicks = Math.max(0, timing.windupTicks);
            timing.cooldownTicks = Math.max(0, timing.cooldownTicks);
            if (timing.durationTicks > 0 && timing.windupTicks > timing.durationTicks) {
                timing.windupTicks = timing.durationTicks;
            }
            shape.radius = Math.max(0.0D, shape.radius);
            shape.range = Math.max(0.0D, shape.range);
            shape.maxTargets = Math.max(1, shape.maxTargets);
            fx.particles = Math.max(0, fx.particles);
            fx.spread = Math.max(0.0D, fx.spread);
            fx.volume = Math.max(0.0F, fx.volume);
            fx.pitch = Math.max(0.0F, fx.pitch);
            return this;
        }

        public float damageFrom(Player player, float weaponDamage) {
            return asDamageSpec().amountFor(player, weaponDamage);
        }

        public DamageSpec asDamageSpec() {
            if (damage.damageSpec != null) {
                if (parsedSpec == null || parsedSpecSource != damage.damageSpec) {
                    parsedSpec = DamageSpec.CODEC.parse(JsonOps.INSTANCE, damage.damageSpec)
                            .resultOrPartial(error -> CombatStyles.LOGGER.warn(
                                    "Invalid ability damage_spec: {}", error))
                            .orElse(null);
                    parsedSpecSource = damage.damageSpec;
                }
                if (parsedSpec != null) {
                    return parsedSpec;
                }
            }
            return damage.legacySpec();
        }
    }

    private AbilityConfig() {
    }

    public static Path directory() {
        return ConfigPaths.of(FOLDER);
    }

    public static Entry get(String id) {
        String key = key(id);
        Entry loaded = LOADED.get(key);
        if (loaded != null) {
            return loaded;
        }
        Entry data = DATA.get(key);
        return data != null ? data : LOADED.computeIfAbsent(key, missing -> new Entry());
    }

    public static void defineData(String ability, Entry defaults) {
        if (ability == null || defaults == null) {
            return;
        }
        DATA.put(key(ability), defaults.sane());
    }

    public static void forgetData(String ability) {
        DATA.remove(key(ability));
    }

    public static Map<String, Entry> all() {
        return Map.copyOf(LOADED);
    }

    public static String key(String id) {
        if (id == null) {
            return "";
        }
        String bare = id.trim();
        int colon = bare.indexOf(':');
        return (colon < 0 ? bare : bare.substring(colon + 1)).toLowerCase(Locale.ROOT);
    }

    public static <T> Map<String, T> keyed(Map<String, T> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, T> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, T> entry : source.entrySet()) {
            normalized.put(key(entry.getKey()), entry.getValue());
        }
        return Map.copyOf(normalized);
    }

    private static void defaults() {
        LOADED.clear();
        LOADED.putAll(DECLARED);
    }

    public static void define(String ability, Entry defaults) {
        if (ability == null || defaults == null) {
            return;
        }
        Entry sane = defaults.sane();
        String key = key(ability);
        DECLARED.put(key, sane);
        if (!loadedOnce) {
            LOADED.putIfAbsent(key, sane);
            return;
        }
        if (LOADED.containsKey(key)) {
            return;
        }
        LOADED.put(key, sane.copy());
        Path directory = directory();
        try {
            Files.createDirectories(directory);
        } catch (IOException failure) {
            CombatStyles.LOGGER.warn("Could not create {}", directory, failure);
            return;
        }
        Path file = directory.resolve(key + EXTENSION);
        if (Files.exists(file)) {
            read(file, key);
        } else {
            write(file, LOADED.get(key));
        }
    }

    public static Entry declared(String ability) {
        return DECLARED.get(key(ability));
    }

    public static void load() {
        loadedOnce = true;
        defaults();

        Path directory = directory();
        try {
            Files.createDirectories(directory);
        } catch (IOException failure) {
            CombatStyles.LOGGER.warn("Could not create {}", directory, failure);
            return;
        }

        for (Map.Entry<String, Entry> known : Map.copyOf(LOADED).entrySet()) {
            Path file = directory.resolve(known.getKey() + EXTENSION);
            if (Files.exists(file)) {
                read(file, known.getKey());
                if (SpellCompatState.compatChanged()) {
                    retuneDamageSource(known.getKey());
                    write(file, LOADED.get(known.getKey()));
                }
            } else {
                write(file, known.getValue());
            }
        }
    }

    private static void retuneDamageSource(String id) {
        Entry entry = LOADED.get(id);
        if (entry == null || entry.damage == null) {
            return;
        }
        String source = SpellCompatState.damageSource();
        DamageSpec spec = entry.asDamageSpec();
        List<DamageSpec.ScalingEntry> scaling = new ArrayList<>();
        for (DamageSpec.ScalingEntry scale : spec.scaling()) {
            scaling.add(new DamageSpec.ScalingEntry(source, scale.perPoint(), scale.cap(),
                    scale.mode(), scale.baseline()));
        }
        entry.damage.useSpec(new DamageSpec(spec.base(), spec.ofHit(), spec.ofTargetMaxHealth(),
                spec.ofTargetMissingHealth(), spec.ofSelfMissingHealth(), spec.damageType(),
                Optional.of(source), scaling, spec.scalingMode(), spec.proportional(),
                spec.minimum(), spec.maximum(), spec.multiplier()));
    }

    private static void read(Path file, String id) {
        try (Reader reader = Files.newBufferedReader(file)) {
            Entry parsed = GSON.fromJson(reader, Entry.class);
            if (parsed == null) {
                return;
            }
            LOADED.put(id, completed(parsed, LOADED.get(id)));
        } catch (IOException | RuntimeException failure) {
            CombatStyles.LOGGER.warn("Could not read {}", file, failure);
        }
    }

    private static void write(Path file, Entry entry) {
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(entry, writer);
        } catch (IOException failure) {
            CombatStyles.LOGGER.warn("Could not write {}", file, failure);
        }
    }

    public static void save() {
        Path directory = directory();
        try {
            Files.createDirectories(directory);
        } catch (IOException failure) {
            CombatStyles.LOGGER.warn("Could not create {}", directory, failure);
            return;
        }
        for (Map.Entry<String, Entry> entry : LOADED.entrySet()) {
            write(directory.resolve(entry.getKey() + EXTENSION), entry.getValue());
        }
    }

    private static Entry completed(Entry parsed, Entry known) {
        parsed.damage = parsed.damage == null
                ? (known == null ? new Damage() : known.damage) : parsed.damage;
        parsed.timing = parsed.timing == null
                ? (known == null ? new Timing() : known.timing) : parsed.timing;
        parsed.shape = parsed.shape == null
                ? (known == null ? new Shape() : known.shape) : parsed.shape;
        parsed.fx = parsed.fx == null
                ? (known == null ? new Presentation() : known.fx) : parsed.fx;
        if (parsed.extra == null) {
            parsed.extra = known == null ? new LinkedHashMap<>() : known.extra;
        }
        if (parsed.options == null && known != null && known.options != null) {
            parsed.options = known.options.deepCopy();
        }
        return parsed.sane();
    }

    public static JsonObject entryJson(String id) {
        JsonElement tree = GSON.toJsonTree(get(id));
        return tree != null && tree.isJsonObject() ? tree.getAsJsonObject() : new JsonObject();
    }

    public static JsonObject toJson() {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Entry> entry : LOADED.entrySet()) {
            json.add(entry.getKey(), GSON.toJsonTree(entry.getValue()));
        }
        return json;
    }

    public static void accept(JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            return;
        }
        Map<String, Entry> received = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> member : json.getAsJsonObject().entrySet()) {
            Entry parsed = parse(member.getValue(), LOADED.get(member.getKey()));
            if (parsed != null) {
                received.put(member.getKey(), parsed);
            }
        }
        LOADED.clear();
        LOADED.putAll(received);
    }

    public static boolean applyEntry(String id, JsonElement json) {
        Entry parsed = id == null ? null : parse(json, LOADED.get(id));
        if (parsed == null || !LOADED.containsKey(id)) {
            return false;
        }
        LOADED.put(id, parsed);
        Path directory = directory();
        try {
            Files.createDirectories(directory);
        } catch (IOException failure) {
            CombatStyles.LOGGER.warn("Could not create {}", directory, failure);
            return false;
        }
        write(directory.resolve(id + EXTENSION), parsed);
        return true;
    }

    private static Entry parse(JsonElement json, Entry known) {
        if (json == null || !json.isJsonObject()) {
            return null;
        }
        try {
            Entry parsed = GSON.fromJson(json, Entry.class);
            return parsed == null ? null : completed(parsed, known);
        } catch (RuntimeException failure) {
            CombatStyles.LOGGER.warn("Rejected malformed ability entry", failure);
            return null;
        }
    }
}
