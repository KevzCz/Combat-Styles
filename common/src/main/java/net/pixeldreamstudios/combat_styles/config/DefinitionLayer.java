package net.pixeldreamstudios.combat_styles.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class DefinitionLayer<T> {

    private static final String EXTENSION = ".json";
    private static final String PATH_SEPARATOR = "/";

    private final String label;
    private final String folder;
    private final Codec<T> codec;
    private final Runnable onChange;

    private final Map<String, T> data = new ConcurrentHashMap<>();
    private final Map<String, T> config = new ConcurrentHashMap<>();
    private final Map<String, T> synced = new ConcurrentHashMap<>();
    private final Set<String> syncedConfig = ConcurrentHashMap.newKeySet();
    private final Set<String> syncedData = ConcurrentHashMap.newKeySet();

    public DefinitionLayer(String label, String folder, Codec<T> codec, Runnable onChange) {
        this.label = label;
        this.folder = folder;
        this.codec = codec;
        this.onChange = onChange;
    }

    public Codec<T> codec() {
        return codec;
    }

    public Path directory() {
        return ConfigPaths.of(folder);
    }

    public void acceptData(Map<String, T> loaded) {
        data.clear();
        if (loaded != null) {
            data.putAll(loaded);
        }
        onChange.run();
    }

    public int reloadConfig() {
        config.clear();
        Path directory = directory();
        if (Files.isDirectory(directory)) {
            List<Path> files;
            try (Stream<Path> walk = Files.walk(directory)) {
                files = walk.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(EXTENSION))
                        .toList();
            } catch (IOException exception) {
                CombatStyles.LOGGER.error("Unable to list {}", directory, exception);
                files = List.of();
            }
            for (Path file : files) {
                String id = idFor(directory.relativize(file));
                if (id == null) {
                    CombatStyles.LOGGER.warn("Skipped {}: put {} files in a namespace folder", file, label);
                    continue;
                }
                read(file).ifPresent(value -> config.put(id, value));
            }
        }
        onChange.run();
        return config.size();
    }

    private static String idFor(Path relative) {
        if (relative.getNameCount() < 2) {
            return null;
        }
        StringBuilder path = new StringBuilder();
        for (int i = 1; i < relative.getNameCount(); i++) {
            if (!path.isEmpty()) {
                path.append(PATH_SEPARATOR);
            }
            path.append(relative.getName(i));
        }
        String raw = path.substring(0, path.length() - EXTENSION.length());
        ResourceLocation id = ResourceLocation.tryBuild(relative.getName(0).toString(), raw);
        return id == null ? null : id.toString();
    }

    private Optional<T> read(Path file) {
        try {
            JsonElement json = JsonParser.parseString(Files.readString(file));
            return codec.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> CombatStyles.LOGGER.error("Invalid {} {}: {}",
                            label, file, error));
        } catch (IOException | RuntimeException exception) {
            CombatStyles.LOGGER.error("Unable to read {}", file, exception);
            return Optional.empty();
        }
    }

    private Path fileFor(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return null;
        }
        return directory().resolve(location.getNamespace())
                .resolve(location.getPath() + EXTENSION);
    }

    public boolean save(String id, T value) {
        Path file = id == null || value == null ? null : fileFor(id);
        if (file == null) {
            return false;
        }
        boolean written = codec.encodeStart(JsonOps.INSTANCE, value)
                .resultOrPartial(error -> CombatStyles.LOGGER.error("Unable to encode {} {}: {}",
                        label, id, error))
                .map(json -> {
                    try {
                        Files.createDirectories(file.getParent());
                        Files.writeString(file, new GsonBuilder().setPrettyPrinting()
                                .create().toJson(json));
                        return Boolean.TRUE;
                    } catch (IOException exception) {
                        CombatStyles.LOGGER.error("Unable to write {}", file, exception);
                        return Boolean.FALSE;
                    }
                })
                .orElse(Boolean.FALSE);
        if (written) {
            config.put(id, value);
            onChange.run();
        }
        return written;
    }

    public boolean delete(String id) {
        Path file = id == null ? null : fileFor(id);
        if (file == null || !config.containsKey(id)) {
            return false;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to delete {}", file, exception);
            return false;
        }
        config.remove(id);
        onChange.run();
        return true;
    }

    public Map<String, T> authoritative() {
        Map<String, T> merged = new TreeMap<>(data);
        merged.putAll(config);
        return merged;
    }

    public Map<String, T> view() {
        Map<String, T> merged = new TreeMap<>(synced);
        merged.putAll(authoritative());
        return merged;
    }

    public Set<String> configIds() {
        return Set.copyOf(config.keySet());
    }

    public Set<String> dataIds() {
        return Set.copyOf(data.keySet());
    }

    public boolean isConfigOwned(String id) {
        return id != null && (config.containsKey(id) || syncedConfig.contains(id));
    }

    public boolean hasDatapackVersion(String id) {
        return id != null && (data.containsKey(id) || syncedData.contains(id));
    }

    public boolean isOverridden(String id) {
        return isConfigOwned(id) && hasDatapackVersion(id);
    }

    public void acceptSync(Map<String, T> merged, Set<String> fromConfig, Set<String> fromData) {
        synced.clear();
        synced.putAll(merged);
        syncedConfig.clear();
        syncedConfig.addAll(fromConfig);
        syncedData.clear();
        syncedData.addAll(fromData);
    }

    public void acceptLocal(String id, T value) {
        if (id != null && value != null) {
            synced.put(id, value);
            syncedConfig.add(id);
        }
    }

    public void removeLocal(String id) {
        if (id == null) {
            return;
        }
        syncedConfig.remove(id);
        if (!hasDatapackVersion(id)) {
            synced.remove(id);
        }
    }

    public void clearSync() {
        synced.clear();
        syncedConfig.clear();
        syncedData.clear();
    }

    public void forget() {
        data.clear();
        config.clear();
        onChange.run();
    }

    public Map<String, String> encodedAuthoritative() {
        Map<String, String> encoded = new LinkedHashMap<>();
        authoritative().forEach((id, value) -> codec.encodeStart(JsonOps.INSTANCE, value)
                .result().ifPresent(json -> encoded.put(id, json.toString())));
        return encoded;
    }

    public void acceptEncodedSync(Map<String, String> encoded, Set<String> fromConfig,
                                  Set<String> fromData) {
        Map<String, T> decoded = new LinkedHashMap<>();
        encoded.forEach((id, raw) -> {
            try {
                codec.parse(JsonOps.INSTANCE, JsonParser.parseString(raw)).result()
                        .ifPresent(value -> decoded.put(id, value));
            } catch (RuntimeException exception) {
                CombatStyles.LOGGER.warn("Unable to read synced {} {}", label, id, exception);
            }
        });
        acceptSync(decoded, fromConfig, fromData);
    }
}
