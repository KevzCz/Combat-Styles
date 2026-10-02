package net.pixeldreamstudios.combat_styles.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EquipmentTagLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "tags/equipment";

    public EquipmentTagLoader() {
        super(CombatStyles.GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, List<ResourceLocation>> byRarity = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            if (!file.getValue().isJsonObject()) {
                continue;
            }
            JsonObject root = file.getValue().getAsJsonObject();
            JsonArray values = root.getAsJsonArray("values");
            if (values == null) {
                continue;
            }
            String rarity = file.getKey().getPath();
            List<ResourceLocation> items =
                    byRarity.computeIfAbsent(rarity, missing -> new ArrayList<>());
            for (JsonElement value : values) {
                ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                if (id == null) {
                    CombatStyles.LOGGER.warn("Equipment tag {} has an unreadable id {}",
                            file.getKey(), value);
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
}
