package net.pixeldreamstudios.combat_styles.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class AbilityConfig {

    public static final String FOLDER = "abilities";

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();

    private static final String EXTENSION = ".json";
    private static final String GAZED = "combat_styles_content:gazed";
    private static final double SPELL_POWER_PER_POINT = 0.02D;
    private static final Map<String, Entry> LOADED = new LinkedHashMap<>();
    private static final Map<String, Entry> DECLARED = new LinkedHashMap<>();

    public static final class Damage {
        public float baseDamage;
        public float weaponDamageRatio;
        public double perPoint;
        public String scalesWith = SpellCompatState.damageSource();
        public String school = SpellCompatState.damageSource();
        public int hits = 1;
        public String applies;
        public int appliesStacks = 1;
        public float appliesChance = 1.0F;
        public float lifesteal;
        public boolean detonates;

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
            return made;
        }
    }

    public static final class Timing {
        public int durationTicks;
        public int windupTicks;

        Timing copy() {
            Timing made = new Timing();
            made.durationTicks = durationTicks;
            made.windupTicks = windupTicks;
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
            made.stamp = 0;
            return made;
        }

        public int fingerprint() {
            if (stamp == 0) {
                stamp = Objects.hash(damage.baseDamage, damage.weaponDamageRatio,
                        damage.perPoint, damage.scalesWith, damage.school, damage.hits,
                        damage.applies, damage.appliesStacks, damage.appliesChance,
                        damage.lifesteal, damage.detonates, timing.durationTicks,
                        timing.windupTicks, shape.radius, shape.range, shape.rise,
                        shape.pull, shape.lift, shape.maxTargets, fx.particles,
                        fx.spread, fx.volume, fx.pitch, extra);
            }
            return stamp;
        }

        private transient int stamp;

        public Entry sane() {
            stamp = 0;
            damage.baseDamage = Math.max(0.0F, damage.baseDamage);
            damage.weaponDamageRatio = Math.max(0.0F, damage.weaponDamageRatio);
            damage.perPoint = Math.max(0.0D, damage.perPoint);
            damage.hits = Math.max(1, damage.hits);
            damage.appliesStacks = Math.max(1, damage.appliesStacks);
            damage.appliesChance = Math.clamp(damage.appliesChance, 0.0F, 1.0F);
            damage.lifesteal = Math.max(0.0F, damage.lifesteal);
            timing.durationTicks = Math.max(0, timing.durationTicks);
            timing.windupTicks = Math.max(0, timing.windupTicks);
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
            List<DamageSpec.ScalingEntry> scaling = damage.perPoint > 0.0D
                    ? List.of(new DamageSpec.ScalingEntry(damage.scalesWith,
                            (float) damage.perPoint, Optional.empty(),
                            DamageSpec.ScalingKind.MULTIPLIER,
                            DamageSpec.ScalingEntry.DEFAULT_BASELINE))
                    : List.of();
            return new DamageSpec(damage.baseDamage, damage.weaponDamageRatio,
                    DamageSpec.NO_HEALTH_RATIO, DamageSpec.NO_HEALTH_RATIO,
                    DamageSpec.NO_HEALTH_RATIO,
                    Optional.empty(), Optional.ofNullable(damage.school),
                    scaling, DamageSpec.ScalingMode.SUM, List.of(),
                    Optional.empty(), Optional.empty(), DamageSpec.FULL_MULTIPLIER);
        }
    }

    private AbilityConfig() {
    }

    public static Path directory() {
        return ConfigPaths.of(FOLDER);
    }

    public static Entry get(String id) {
        return LOADED.computeIfAbsent(key(id), missing -> new Entry());
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

    private static Entry define(float baseDamage, float weaponRatio, int hits,
                                int durationTicks, int windupTicks,
                                double radius, double range, double rise, int maxTargets,
                                int particles, double spread, float volume, float pitch) {
        Entry entry = new Entry();
        entry.damage.baseDamage = baseDamage;
        entry.damage.weaponDamageRatio = weaponRatio;
        entry.damage.perPoint = SPELL_POWER_PER_POINT;
        entry.damage.hits = hits;
        entry.timing.durationTicks = durationTicks;
        entry.timing.windupTicks = windupTicks;
        entry.shape.radius = radius;
        entry.shape.range = range;
        entry.shape.rise = rise;
        entry.shape.maxTargets = maxTargets;
        entry.fx.particles = particles;
        entry.fx.spread = spread;
        entry.fx.volume = volume;
        entry.fx.pitch = pitch;
        return entry.sane();
    }

    private static void defaults() {
        LOADED.clear();
        LOADED.putAll(DECLARED);

        LOADED.put("twin_lance", define(2.0F, 0.5F, 2, 10, 2,
                0.0D, 14.0D, 0.0D, 1, 4, 0.1D, 0.35F, 1.8F));

        LOADED.put("furrow", define(4.0F, 1.0F, 1, 30, 4,
                1.1D, 9.0D, 0.0D, 4, 2, 0.2D, 0.35F, 0.6F));

        LOADED.put("wide_cut", define(3.0F, 1.0F, 1, 16, 2,
                1.0D, 7.0D, 0.0D, 5, 0, 0.0D, 0.35F, 0.7F));

        LOADED.put("blink_watch", define(3.5F, 1.0F, 1, 18, 8,
                0.0D, 3.0D, 0.0D, 1, 12, 0.3D, 0.35F, 1.5F));

Entry tether = define(1.5F, 0.35F, 1, 40, 8,
                0.0D, 0.0D, 0.0D, 1, 3, 0.2D, 0.35F, 1.2F);
        tether.damage.lifesteal = 0.35F;
        LOADED.put("tether", tether);

        Entry collapse = define(6.0F, 1.0F, 1, 24, 18,
                4.0D, 0.0D, -0.35D, 8, 1, 0.15D, 0.6F, 1.4F);
        collapse.damage.applies = GAZED;
        LOADED.put("collapse", collapse);

        Entry eclipse = define(7.0F, 1.0F, 1, 26, 18,
                4.5D, 0.0D, 0.9D, 8, 12, 0.7D, 0.55F, 0.75F);
        eclipse.damage.applies = GAZED;
        eclipse.shape.pull = 0.35D;
        eclipse.shape.lift = 0.12D;
        LOADED.put("eclipse", eclipse);

        Entry supernova = define(10.0F, 1.0F, 1, 30, 22,
                6.0D, 0.0D, 1.1D, 12, 24, 1.2D, 0.8F, 1.15F);
        supernova.damage.detonates = true;
        LOADED.put("supernova", supernova);
    }

    public static void define(String ability, Entry defaults) {
        if (ability == null || defaults == null) {
            return;
        }
        Entry sane = defaults.sane();
        DECLARED.put(key(ability), sane);
        LOADED.putIfAbsent(key(ability), sane);
    }

    public static Entry declared(String ability) {
        return DECLARED.get(key(ability));
    }

    public static void load() {
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
        entry.damage.scalesWith = SpellCompatState.damageSource();
        entry.damage.school = SpellCompatState.damageSource();
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
        return parsed.sane();
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
