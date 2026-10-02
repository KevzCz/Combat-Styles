package net.pixeldreamstudios.combat_styles.equipment;

import com.google.gson.GsonBuilder;
import com.mojang.serialization.JsonOps;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.ConfigPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EquipmentConfig {

    private static final String EXTENSION = ".json";

    private EquipmentConfig() {
    }

    public static Path directory() {
        return ConfigPaths.of(EquipmentLoader.DIRECTORY);
    }

    public static Path fileFor(String id) {
        return directory().resolve(id.replace(':', '_') + EXTENSION);
    }

    public static boolean save(EquipmentDefinition definition) {
        if (definition == null || definition.id() == null || definition.id().isBlank()) {
            return false;
        }
        Path directory = directory();
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Failed to create equipment folder", exception);
            return false;
        }
        return EquipmentDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .resultOrPartial(error -> CombatStyles.LOGGER.error(
                        "Failed to encode equipment {}: {}", definition.id(), error))
                .map(json -> {
                    try {
                        Files.writeString(fileFor(definition.id()),
                                new GsonBuilder().setPrettyPrinting().create().toJson(json));
                        return true;
                    } catch (IOException exception) {
                        CombatStyles.LOGGER.error("Failed to write equipment {}",
                                definition.id(), exception);
                        return false;
                    }
                })
                .orElse(false);
    }
}
