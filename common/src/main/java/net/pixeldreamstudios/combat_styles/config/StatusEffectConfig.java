package net.pixeldreamstudios.combat_styles.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StatusEffectConfig {

    public static final String FOLDER = "status_effects";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String EXTENSION = ".json";
    private static final Map<String, Entry> DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, Entry> LOADED = new LinkedHashMap<>();
    private static boolean loaded;

    public static final class Entry {
        public int durationTicks = 200;
        public int maxStacks = 5;
        public float perStack = 0.25F;
        public boolean enabled = true;

        Entry() {
        }

        Entry(int durationTicks, int maxStacks, float perStack) {
            this.durationTicks = durationTicks;
            this.maxStacks = maxStacks;
            this.perStack = perStack;
        }

        Entry copy() {
            Entry copy = new Entry(durationTicks, maxStacks, perStack);
            copy.enabled = enabled;
            return copy;
        }
    }

    public static void registerDefault(String id, int durationTicks, int maxStacks, float perStack) {
        if (id == null || id.isBlank()) {
            return;
        }
        DEFAULTS.put(id, new Entry(durationTicks, maxStacks, perStack));
        if (loaded && !LOADED.containsKey(id)) {
            Path directory = directory();
            if (ensure(directory)) {
                loadEntry(directory, id, DEFAULTS.get(id).copy());
            }
        }
    }

    private StatusEffectConfig() {
    }

    public static Path directory() {
        return ConfigPaths.of(FOLDER);
    }

    public static Entry get(String id) {
        return LOADED.computeIfAbsent(id, key -> new Entry());
    }

    public static Map<String, Entry> all() {
        return Map.copyOf(LOADED);
    }

    public static void load() {
        LOADED.clear();
        loaded = true;
        Path directory = directory();
        if (!ensure(directory)) {
            DEFAULTS.forEach((id, entry) -> LOADED.put(id, entry.copy()));
            return;
        }
        for (Map.Entry<String, Entry> known : DEFAULTS.entrySet()) {
            loadEntry(directory, known.getKey(), known.getValue().copy());
        }
    }

    private static void loadEntry(Path directory, String id, Entry fallback) {
        LOADED.put(id, fallback);
        Path file = directory.resolve(id + EXTENSION);
        if (Files.exists(file)) {
            read(file, id);
        } else {
            write(file, fallback);
        }
    }

    private static boolean ensure(Path directory) {
        try {
            Files.createDirectories(directory);
            return true;
        } catch (IOException failure) {
            CombatStyles.LOGGER.warn("Could not create {}", directory, failure);
            return false;
        }
    }

    private static void read(Path file, String id) {
        try (Reader reader = Files.newBufferedReader(file)) {
            Entry parsed = GSON.fromJson(reader, Entry.class);
            if (parsed != null) {
                LOADED.put(id, parsed);
            }
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
        if (!ensure(directory)) {
            return;
        }
        for (Map.Entry<String, Entry> entry : LOADED.entrySet()) {
            write(directory.resolve(entry.getKey() + EXTENSION), entry.getValue());
        }
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
            Entry parsed = parse(member.getValue());
            if (parsed != null) {
                received.put(member.getKey(), parsed);
            }
        }
        LOADED.clear();
        LOADED.putAll(received);
    }

    public static boolean applyEntry(String id, JsonElement json) {
        Entry parsed = parse(json);
        if (id == null || !LOADED.containsKey(id) || parsed == null) {
            return false;
        }
        LOADED.put(id, parsed);
        Path directory = directory();
        if (!ensure(directory)) {
            return false;
        }
        write(directory.resolve(id + EXTENSION), parsed);
        return true;
    }

    private static Entry parse(JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            return null;
        }
        try {
            return GSON.fromJson(json, Entry.class);
        } catch (RuntimeException failure) {
            CombatStyles.LOGGER.warn("Rejected malformed status effect entry", failure);
            return null;
        }
    }
}
