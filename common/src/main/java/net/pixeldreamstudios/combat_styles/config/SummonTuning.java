package net.pixeldreamstudios.combat_styles.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class SummonTuning {

    public static final String FILE = "summons.json";

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();

    private static final Map<String, Section<?>> SECTIONS = new LinkedHashMap<>();

    private static Values values = new Values();
    private static boolean loaded;

    public static final class Values {
        public double beamStep = 0.35D;
        public double beamHitPadding = 0.45D;
        public float minimumDamage = 0.05F;
        public int trailInterval = 4;
        public float cueVolume = 0.35F;
        public double reacquireRadius = 10.0D;
        public double sweepArcDegrees = 140.0D;
        public double furrowProbeDepth = 3.0D;
        public double blinkOvershoot = 4.0D;
        public double rosterRadius = 24.0D;
        public boolean recallStrays = true;
        public int maxCommandedPerOrder = 16;
        public double followStartDistance = 6.0D;
        public double followStopDistance = 2.5D;
        public double teleportDistance = 12.0D;
        public double huntRange = 24.0D;
    }

    private static final class Section<T> {
        private final Class<T> type;
        private T value;

        private Section(Class<T> type) {
            this.type = type;
        }

        private void read(JsonObject root, String key) {
            JsonElement own = root.get(key);
            T parsed = GSON.fromJson(own != null && own.isJsonObject() ? own : root, type);
            value = parsed != null ? parsed : GSON.fromJson(new JsonObject(), type);
        }
    }

    private SummonTuning() {
    }

    public static Values get() {
        return values;
    }

    public static <T> Supplier<T> register(String key, Class<T> type) {
        Section<T> section = new Section<>(type);
        SECTIONS.put(key, section);
        if (loaded) {
            load();
        } else {
            section.read(new JsonObject(), key);
        }
        return () -> section.value;
    }

    private static Path file() {
        return ConfigPaths.of(FILE);
    }

    public static void load() {
        loaded = true;
        Path path = file();
        JsonObject root = new JsonObject();
        boolean complete = Files.exists(path);
        if (complete) {
            try (Reader reader = Files.newBufferedReader(path)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (parsed != null && parsed.isJsonObject()) {
                    root = parsed.getAsJsonObject();
                }
            } catch (IOException | RuntimeException failure) {
                CombatStyles.LOGGER.warn("Could not read {}", path, failure);
            }
        }
        complete &= readRoot(root);
        if (!complete) {
            save();
        }
    }

    private static boolean readRoot(JsonObject root) {
        boolean complete = true;
        Values parsed = GSON.fromJson(root, Values.class);
        values = parsed != null ? parsed : new Values();
        for (String key : GSON.toJsonTree(new Values()).getAsJsonObject().keySet()) {
            complete &= root.has(key);
        }
        for (Map.Entry<String, Section<?>> section : SECTIONS.entrySet()) {
            complete &= root.has(section.getKey());
            section.getValue().read(root, section.getKey());
        }
        return complete;
    }

    public static JsonObject toJson() {
        JsonObject root = GSON.toJsonTree(values).getAsJsonObject();
        for (Map.Entry<String, Section<?>> section : SECTIONS.entrySet()) {
            root.add(section.getKey(), GSON.toJsonTree(section.getValue().value));
        }
        return root;
    }

    public static void accept(JsonElement json) {
        if (json != null && json.isJsonObject()) {
            readRoot(json.getAsJsonObject());
        }
    }

    public static boolean applyEdit(JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            return false;
        }
        try {
            readRoot(json.getAsJsonObject());
        } catch (RuntimeException failure) {
            CombatStyles.LOGGER.warn("Rejected malformed summon tuning edit", failure);
            return false;
        }
        save();
        return true;
    }

    public static void save() {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException failure) {
            CombatStyles.LOGGER.warn("Could not create {}", path.getParent(), failure);
            return;
        }
        JsonObject root = toJson();
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(root, writer);
        } catch (IOException failure) {
            CombatStyles.LOGGER.warn("Could not write {}", path, failure);
        }
    }
}
