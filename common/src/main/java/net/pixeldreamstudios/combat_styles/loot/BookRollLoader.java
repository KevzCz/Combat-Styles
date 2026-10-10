package net.pixeldreamstudios.combat_styles.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BookRollLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "style_books";
    private static final FileToIdConverter FILES = FileToIdConverter.json(DIRECTORY);
    private static final String BUNDLED_ROOT = "/data/";
    private static final String PATH_SEPARATOR = "/";

    public BookRollLoader() {
        super(CombatStyles.GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, BookRoll> loaded = new LinkedHashMap<>();
        int rejected = 0;
        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            JsonElement json = overriddenDefault(file.getKey(), resourceManager).orElse(file.getValue());
            BookRoll roll = BookRoll.CODEC
                    .parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error ->
                            CombatStyles.LOGGER.error("Invalid style book {}: {}", file.getKey(), error))
                    .orElse(null);
            if (roll == null) {
                rejected++;
                continue;
            }
            if (roll.pool().isEmpty() && roll.categories().isEmpty()) {
                CombatStyles.LOGGER.warn("Style book {} has no pool and no categories", file.getKey());
            }
            loaded.put(file.getKey().toString(), roll);
        }
        BookRolls.DEFINITIONS.acceptData(loaded);
        if (rejected > 0) {
            CombatStyles.LOGGER.warn("Rejected {} invalid style book definitions", rejected);
        }
        CombatStyles.LOGGER.info("Loaded {} style book definitions", loaded.size());
    }

    private static Optional<JsonElement> overriddenDefault(ResourceLocation id, ResourceManager resourceManager) {
        if (!CombatStyles.MOD_ID.equals(id.getNamespace())) {
            return Optional.empty();
        }
        ResourceLocation file = FILES.idToFile(id);
        List<Resource> stack = resourceManager.getResourceStack(file);
        if (stack.size() < 2) {
            return Optional.empty();
        }
        JsonElement bundled = bundled(file);
        if (bundled == null) {
            return Optional.empty();
        }
        for (int index = stack.size() - 1; index >= 0; index--) {
            JsonElement candidate = read(stack.get(index));
            if (candidate != null && !candidate.equals(bundled)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static JsonElement bundled(ResourceLocation file) {
        String path = BUNDLED_ROOT + file.getNamespace() + PATH_SEPARATOR + file.getPath();
        try (InputStream stream = BookRollLoader.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader);
            }
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    private static JsonElement read(Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader);
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }
}
