package net.pixeldreamstudios.combat_styles.style;

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

public final class CustomAttackConfig {

    private static final String EXTENSION = ".json";

    private static final Map<String, CustomAttack> LOADED = new LinkedHashMap<>();

    private CustomAttackConfig() {
    }

    public static Path directory() {
        return ConfigPaths.of(ConfigPaths.ATTACKS);
    }

    public static Map<String, CustomAttack> loaded() {
        return Map.copyOf(LOADED);
    }

    public static void forgetAll() {
        LOADED.clear();
    }

    public static int reload() {
        LOADED.clear();
        Path directory = directory();
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(path -> path.toString().endsWith(EXTENSION)).forEach(path -> {
                try {
                    JsonElement json = JsonParser.parseString(Files.readString(path));
                    CustomAttack.CODEC.parse(JsonOps.INSTANCE, json)
                            .resultOrPartial(error -> CombatStyles.LOGGER.error(
                                    "Invalid custom attack {}: {}", path.getFileName(), error))
                            .ifPresent(style -> LOADED.put(style.id(), style));
                } catch (IOException exception) {
                    CombatStyles.LOGGER.error("Failed to read custom attack {}",
                            path.getFileName(), exception);
                }
            });
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Failed to list custom attacks", exception);
        }
        if (!LOADED.isEmpty()) {
            CombatStyles.LOGGER.info("Loaded {} custom attacks from config", LOADED.size());
        }
        return LOADED.size();
    }

    public static boolean isFromConfig(String styleId) {
        return LOADED.containsKey(styleId);
    }

    public static boolean delete(String styleId) {
        if (styleId == null || !LOADED.containsKey(styleId)) {
            return false;
        }
        Path file = directory().resolve(styleId.replace(':', '_') + EXTENSION);
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Failed to delete custom attack {}", styleId, exception);
            return false;
        }
        LOADED.remove(styleId);
        return true;
    }

    public static boolean save(CustomAttack style) {
        if (style == null) {
            return false;
        }
        Path directory = directory();
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Failed to create custom attack folder", exception);
            return false;
        }
        String name = style.id().replace(':', '_') + EXTENSION;
        return CustomAttack.CODEC.encodeStart(JsonOps.INSTANCE, style)
                .resultOrPartial(error -> CombatStyles.LOGGER.error(
                        "Failed to encode custom attack {}: {}", style.id(), error))
                .map(json -> {
                    try {
                        Files.writeString(directory.resolve(name),
                                new GsonBuilder().setPrettyPrinting().create().toJson(json));
                        LOADED.put(style.id(), style);
                        return true;
                    } catch (IOException exception) {
                        CombatStyles.LOGGER.error("Failed to write custom attack {}",
                                style.id(), exception);
                        return false;
                    }
                })
                .orElse(false);
    }
}
