package net.pixeldreamstudios.combat_styles.equipment;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import net.pixeldreamstudios.combat_styles.affix.AffixGrants;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.style.InnateAffixes;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.entity.SummonHooks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EquipmentLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "equipment";

    public EquipmentLoader() {
        super(CombatStyles.GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, EquipmentDefinition> loaded = new LinkedHashMap<>();
        int rejected = 0;

        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            EquipmentDefinition definition = EquipmentDefinition.CODEC
                    .parse(JsonOps.INSTANCE, file.getValue())
                    .resultOrPartial(error ->
                            CombatStyles.LOGGER.error("Invalid equipment {}: {}", file.getKey(), error))
                    .orElse(null);
            if (definition == null) {
                rejected++;
                continue;
            }
            if (definition.id().isBlank()) {
                CombatStyles.LOGGER.error("Equipment {} has a blank id", file.getKey());
                rejected++;
                continue;
            }
            if (definition.effects().isEmpty()) {
                CombatStyles.LOGGER.error("Equipment {} declares no effects", definition.id());
                rejected++;
                continue;
            }
            boolean usable = true;
            for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
                String where = definition.id() + "." + named.getKey();
                EquipmentEffect effect = named.getValue().effect();
                if (effect.isEmpty()) {
                    CombatStyles.LOGGER.error("Equipment {} declares no effect the loader "
                            + "recognises; check EquipmentEffect.isEmpty covers every block", where);
                    usable = false;
                    continue;
                }
                for (String affixId : effect.grantAffixes()) {
                    if (AffixRegistry.get(AffixGrants.affixOf(affixId)) == null) {
                        CombatStyles.LOGGER.warn("{} grants unknown affix {}", where, affixId);
                    }
                }
                for (String affixId : effect.tuneAffixes().keySet()) {
                    if (AffixRegistry.get(affixId) == null) {
                        CombatStyles.LOGGER.warn("{} tunes unknown affix {}", where, affixId);
                    }
                }
            }
            if (!usable) {
                rejected++;
                continue;
            }
            EquipmentDefinition previous = loaded.put(definition.id(), definition);
            if (previous != null) {
                CombatStyles.LOGGER.warn("Equipment {} from {} overrides an earlier definition",
                        definition.id(), file.getKey());
            }
        }

        EquipmentOverrides.reload();
        EquipmentRegistry.acceptData(loaded);
        loaded = new LinkedHashMap<>(EquipmentOverrides.applyTo(loaded));
        EquipmentRegistry.accept(loaded);

        Map<String, List<InnateAffixes.Slot>> innate = new LinkedHashMap<>();
        for (EquipmentDefinition definition : loaded.values()) {
            if (!definition.innateAffixes().isEmpty()) {
                innate.put(definition.id(), definition.innateAffixes());
            }
        }
        InnateAffixes.acceptEquipment(innate);

        SummonHooks.reload();
        CombatStyles.LOGGER.info("Loaded {} equipment definitions ({} rejected)", loaded.size(), rejected);
    }
}
