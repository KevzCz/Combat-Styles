package net.pixeldreamstudios.combat_styles.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EquipmentTagLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "tags/equipment";

    private static final String KEY_REPLACE = "replace";
    private static final String KEY_VALUES = "values";
    private static final FileToIdConverter FILES = FileToIdConverter.json(DIRECTORY);

    private record Layer(String rarity, int pack, String file, JsonObject root) {
    }

    public EquipmentTagLoader() {
        super(CombatStyles.GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<String> packOrder = resourceManager.listPacks().map(PackResources::packId).toList();
        List<Layer> layers = new ArrayList<>();
        for (Map.Entry<ResourceLocation, List<Resource>> stack
                : FILES.listMatchingResourceStacks(resourceManager).entrySet()) {
            String rarity = FILES.fileToId(stack.getKey()).getPath();
            for (Resource resource : stack.getValue()) {
                JsonObject root = read(resource, stack.getKey());
                if (root != null) {
                    layers.add(new Layer(rarity, packOrder.indexOf(resource.sourcePackId()),
                            stack.getKey().toString(), root));
                }
            }
        }
        layers.sort(Comparator.comparingInt(Layer::pack).thenComparing(Layer::file));

        Map<String, List<ResourceLocation>> byRarity = new LinkedHashMap<>();
        for (Layer layer : layers) {
            List<ResourceLocation> items =
                    byRarity.computeIfAbsent(layer.rarity(), missing -> new ArrayList<>());
            JsonElement replace = layer.root().get(KEY_REPLACE);
            if (replace != null && replace.isJsonPrimitive() && replace.getAsBoolean()) {
                items.clear();
            }
            JsonArray values = layer.root().getAsJsonArray(KEY_VALUES);
            if (values == null) {
                continue;
            }
            for (JsonElement value : values) {
                ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                if (id == null) {
                    CombatStyles.LOGGER.warn("Equipment tag {} has an unreadable id {}",
                            layer.file(), value);
                    continue;
                }
                if (!items.contains(id)) {
                    items.add(id);
                }
            }
        }
        EquipmentDrops.accept(byRarity);
        CombatStyles.LOGGER.info("Loaded equipment drop pools for {} rarities", byRarity.size());
    }

    private static JsonObject read(Resource resource, ResourceLocation file) {
        try (Reader reader = resource.openAsReader()) {
            JsonElement json = JsonParser.parseReader(reader);
            return json.isJsonObject() ? json.getAsJsonObject() : null;
        } catch (IOException | RuntimeException exception) {
            CombatStyles.LOGGER.warn("Unable to read equipment tag {} from {}", file,
                    resource.sourcePackId(), exception);
            return null;
        }
    }
}
