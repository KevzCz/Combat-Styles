package net.pixeldreamstudios.combat_styles.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class ConfigMigration {

    private static final String EXTENSION = ".json";
    private static final String KEY_ID = "id";
    private static final String KEY_STYLE = "style";
    private static final String KEY_ATTACK = "attack";

    private ConfigMigration() {
    }

    public static void run() {
        Path legacyStyles = ConfigPaths.of(ConfigPaths.LEGACY_STYLES);
        for (Path file : jsonFilesIn(legacyStyles)) {
            JsonObject json = read(file);
            boolean custom = json != null && json.has(KEY_ID) && json.has(KEY_STYLE);
            move(file, ConfigPaths.of(custom ? ConfigPaths.CUSTOM_STYLES : ConfigPaths.DEFAULT_STYLES));
        }
        Path attacks = ConfigPaths.of(ConfigPaths.ATTACKS);
        for (Path file : jsonFilesIn(attacks)) {
            JsonObject json = read(file);
            if (json != null && json.has(KEY_ID) && json.has(KEY_ATTACK)) {
                move(file, ConfigPaths.of(ConfigPaths.CUSTOM_ATTACKS));
            }
        }
    }

    public static boolean move(Path file, Path targetDirectory) {
        Path target = targetDirectory.resolve(file.getFileName());
        try {
            Files.createDirectories(targetDirectory);
            if (Files.exists(target)) {
                CombatStyles.LOGGER.warn("Kept {} where it is: {} already exists", file, target);
                return false;
            }
            Files.move(file, target);
            CombatStyles.LOGGER.info("Moved {} to {}", file, target);
            return true;
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to move {} to {}", file, target, exception);
            return false;
        }
    }

    private static List<Path> jsonFilesIn(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(EXTENSION))
                    .toList();
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to list {}", directory, exception);
            return List.of();
        }
    }

    private static JsonObject read(Path file) {
        try {
            JsonElement json = JsonParser.parseString(Files.readString(file));
            return json.isJsonObject() ? json.getAsJsonObject() : null;
        } catch (IOException | RuntimeException exception) {
            CombatStyles.LOGGER.warn("Unable to read {}", file, exception);
            return null;
        }
    }
}
