package net.pixeldreamstudios.combat_styles.affix;

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

public final class CustomAffixConfig {

    private static final String EXTENSION = ".json";

    private static final Map<String, StyleAffix> LOADED = new LinkedHashMap<>();

    private CustomAffixConfig() {
    }

    public static Path directory() {
        return ConfigPaths.of(ConfigPaths.AFFIXES);
    }

    public static Map<String, StyleAffix> loaded() {
        return Map.copyOf(LOADED);
    }

    public static boolean isFromConfig(String affixId) {
        return LOADED.containsKey(affixId);
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
            files.filter(path -> path.toString().endsWith(EXTENSION)).forEach(CustomAffixConfig::load);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to list {}", directory, exception);
        }
        if (!LOADED.isEmpty()) {
            CombatStyles.LOGGER.info("Loaded {} custom affixes from config", LOADED.size());
        }
        return LOADED.size();
    }

    private static void load(Path file) {
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file));
            StyleAffix affix = StyleAffix.CODEC.parse(JsonOps.INSTANCE, parsed)
                    .resultOrPartial(error -> CombatStyles.LOGGER.error(
                            "Invalid custom affix {}: {}", file.getFileName(), error))
                    .orElse(null);
            if (affix != null) {
                LOADED.put(affix.serializedId(), affix);
            }
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to read {}", file, exception);
        }
    }

    public static boolean delete(String affixId) {
        if (affixId == null || !LOADED.containsKey(affixId)) {
            return false;
        }
        Path file = directory().resolve(affixId.replace(':', '_') + EXTENSION);
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Failed to delete custom affix {}", affixId, exception);
            return false;
        }
        LOADED.remove(affixId);
        AffixRegistry.acceptCustom(loaded());
        return true;
    }

    public static boolean save(StyleAffix affix) {
        if (affix == null) {
            return false;
        }
        Path directory = directory();
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Failed to create custom affix folder", exception);
            return false;
        }
        String name = affix.serializedId().replace(':', '_') + EXTENSION;
        return StyleAffix.CODEC.encodeStart(JsonOps.INSTANCE, affix)
                .resultOrPartial(error -> CombatStyles.LOGGER.error(
                        "Failed to encode custom affix {}: {}", affix.serializedId(), error))
                .map(json -> {
                    try {
                        Files.writeString(directory.resolve(name),
                                new GsonBuilder().setPrettyPrinting().create().toJson(json));
                        LOADED.put(affix.serializedId(), affix);
                        AffixRegistry.acceptCustom(loaded());
                        return true;
                    } catch (IOException exception) {
                        CombatStyles.LOGGER.error("Failed to write custom affix {}",
                                affix.serializedId(), exception);
                        return false;
                    }
                })
                .orElse(false);
    }
}
