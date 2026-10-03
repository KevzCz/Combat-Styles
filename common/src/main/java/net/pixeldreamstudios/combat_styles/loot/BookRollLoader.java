package net.pixeldreamstudios.combat_styles.loot;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.LinkedHashMap;
import java.util.Map;

public class BookRollLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "style_books";

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
            BookRoll roll = BookRoll.CODEC
                    .parse(JsonOps.INSTANCE, file.getValue())
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
}
