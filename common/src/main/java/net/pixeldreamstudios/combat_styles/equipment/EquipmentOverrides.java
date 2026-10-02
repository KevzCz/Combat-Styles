package net.pixeldreamstudios.combat_styles.equipment;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.ConfigPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class EquipmentOverrides {

    private static final String EXTENSION = ".json";
    private static final String FOLDER = "equipment_overrides";

    private static final Map<String, EquipmentDefinition> OVERRIDES = new LinkedHashMap<>();

    private EquipmentOverrides() {
    }

    public static Path directory() {
        return ConfigPaths.of(FOLDER);
    }

    public static Map<String, EquipmentDefinition> all() {
        return Map.copyOf(OVERRIDES);
    }

    public static EquipmentDefinition get(String id) {
        return OVERRIDES.get(id);
    }

    public static boolean has(String id) {
        return OVERRIDES.containsKey(id);
    }

    public static void forgetAll() {
        OVERRIDES.clear();
    }

    public static int reload() {
        OVERRIDES.clear();
        Path directory = directory();
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(EXTENSION))
                    .forEach(EquipmentOverrides::load);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to list {}", directory, exception);
        }
        return OVERRIDES.size();
    }

    private static void load(Path path) {
        try {
            JsonElement json = JsonParser.parseString(Files.readString(path));
            EquipmentDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> CombatStyles.LOGGER.error(
                            "Failed to parse equipment override {}: {}", path, error))
                    .ifPresent(definition -> OVERRIDES.put(definition.id(), definition));
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Failed to read equipment override {}", path, exception);
        }
    }

    public static boolean save(EquipmentDefinition definition) {
        if (definition == null || definition.id() == null || definition.id().isBlank()) {
            return false;
        }
        Path directory = directory();
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Failed to create equipment override folder", exception);
            return false;
        }
        String name = definition.id().replace(':', '_') + EXTENSION;
        return EquipmentDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .resultOrPartial(error -> CombatStyles.LOGGER.error(
                        "Failed to encode equipment override {}: {}", definition.id(), error))
                .map(json -> {
                    try {
                        Files.writeString(directory.resolve(name),
                                new GsonBuilder().setPrettyPrinting().create().toJson(json));
                        OVERRIDES.put(definition.id(), definition);
                        return true;
                    } catch (IOException exception) {
                        CombatStyles.LOGGER.error("Failed to write equipment override {}",
                                definition.id(), exception);
                        return false;
                    }
                })
                .orElse(false);
    }

    public static boolean clear(String id) {
        if (id == null || !OVERRIDES.containsKey(id)) {
            return false;
        }
        Path file = directory().resolve(id.replace(':', '_') + EXTENSION);
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Failed to delete equipment override {}", id, exception);
            return false;
        }
        OVERRIDES.remove(id);
        return true;
    }

    public static Map<String, EquipmentDefinition> applyTo(
            Map<String, EquipmentDefinition> loaded) {
        if (OVERRIDES.isEmpty()) {
            return loaded;
        }
        Map<String, EquipmentDefinition> merged = new LinkedHashMap<>(loaded);
        merged.putAll(OVERRIDES);
        return merged;
    }
}
