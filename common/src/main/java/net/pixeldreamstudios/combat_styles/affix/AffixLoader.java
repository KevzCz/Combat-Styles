package net.pixeldreamstudios.combat_styles.affix;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.LinkedHashMap;
import java.util.Map;

public class AffixLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "style_affixes";

    public AffixLoader() {
        super(CombatStyles.GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, StyleAffix> loaded = new LinkedHashMap<>();
        int rejected = 0;

        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            StyleAffix affix = StyleAffix.CODEC
                    .parse(JsonOps.INSTANCE, file.getValue())
                    .resultOrPartial(error ->
                            CombatStyles.LOGGER.error("Invalid style affix {}: {}", file.getKey(), error))
                    .orElse(null);
            if (affix == null) {
                rejected++;
                continue;
            }
            StyleAffix previous = loaded.put(affix.serializedId(), affix);
            if (previous != null) {
                CombatStyles.LOGGER.warn("Style affix {} from {} overrides an earlier definition",
                        affix.serializedId(), file.getKey());
            }
        }

        AffixRegistry.accept(loaded);
        CustomAffixConfig.reload();
        CombatStyles.LOGGER.info("Loaded {} style affixes ({} rejected)",
                loaded.size(), rejected);
    }
}
