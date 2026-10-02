package net.pixeldreamstudios.combat_styles.loot;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WeaponRollLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "weapon_rolls";

    public WeaponRollLoader() {
        super(CombatStyles.GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<WeaponRoll> loaded = new ArrayList<>();
        int rejected = 0;
        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            WeaponRoll roll = WeaponRoll.CODEC
                    .parse(JsonOps.INSTANCE, file.getValue())
                    .resultOrPartial(error ->
                            CombatStyles.LOGGER.error("Invalid weapon roll {}: {}", file.getKey(), error))
                    .orElse(null);
            if (roll == null) {
                rejected++;
                continue;
            }
            if (roll.chance() <= 0.0F) {
                CombatStyles.LOGGER.warn("Weapon roll {} has chance <= 0 and will never fire",
                        file.getKey());
            }
            if (roll.items().isEmpty() && roll.categories().isEmpty() && roll.tables().isEmpty()) {
                CombatStyles.LOGGER.warn("Weapon roll {} has no filters and matches every weapon",
                        file.getKey());
            }
            if ((roll.rewritesSlots() || roll.affixesOnly()) && roll.slots().isEmpty()) {
                CombatStyles.LOGGER.info("Weapon roll {} targets a random slot ({} mode)",
                        file.getKey(), roll.mode());
            }
            loaded.add(roll);
        }
        WeaponRolls.accept(loaded);
        CombatStyles.LOGGER.info("Loaded {} weapon roll definitions ({} rejected)",
                loaded.size(), rejected);
    }
}
