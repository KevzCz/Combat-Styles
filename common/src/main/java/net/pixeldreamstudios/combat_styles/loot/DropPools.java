package net.pixeldreamstudios.combat_styles.loot;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.ConfigPaths;
import net.pixeldreamstudios.combat_styles.config.DefinitionLayer;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DropPools {

    private static final String LABEL = "drop pool";
    private static final char NAMESPACE_SEPARATOR = ':';
    private static final String CHEST_PATTERN = "chests/";
    private static final String ENTITY_PATTERN = "entities/";
    private static final double BASE_CHANCE = 0.05D;
    private static final double RARITY_FALLOFF = 0.6D;

    public static final DefinitionLayer<DropPool> DEFINITIONS = new DefinitionLayer<>(LABEL,
            ConfigPaths.EQUIPMENT_DROPS, DropPool.CODEC, DropPools::changed);

    private DropPools() {
    }

    private static void changed() {
    }

    public static String idFor(StyleRarity rarity) {
        return CombatStyles.id(rarity.key()).toString();
    }

    public static StyleRarity rarityOf(String id) {
        return id == null ? null : exact(id.substring(id.indexOf(NAMESPACE_SEPARATOR) + 1));
    }

    private static StyleRarity exact(String key) {
        for (StyleRarity rarity : StyleRarity.values()) {
            if (rarity.key().equalsIgnoreCase(key)) {
                return rarity;
            }
        }
        return null;
    }

    public static DropPool stored(StyleRarity rarity) {
        DropPool found = DEFINITIONS.view().get(idFor(rarity));
        return found == null ? defaultPool(rarity) : found;
    }

    public static DropPool effective(StyleRarity rarity) {
        return withTagged(stored(rarity), EquipmentDrops.tagged(rarity.key()));
    }

    public static DropPool defaultPool(StyleRarity rarity) {
        double chance = BASE_CHANCE * Math.pow(RARITY_FALLOFF, rarity.ordinal());
        return new DropPool(chance, List.of(), List.of(CHEST_PATTERN, ENTITY_PATTERN), List.of(),
                List.of(), List.of(), List.of(), List.of());
    }

    public static DropPool withTagged(DropPool pool, List<String> tagged) {
        Set<String> listed = new HashSet<>();
        for (DropEntry entry : pool.entries()) {
            if (!entry.isInstance()) {
                listed.add(entry.itemId());
            }
        }
        List<DropEntry> entries = new ArrayList<>(pool.entries());
        for (String id : tagged) {
            if (listed.add(id)) {
                entries.add(DropEntry.of(id));
            }
        }
        return pool.withEntries(entries);
    }

    public static DropPool withoutDefaultTagged(DropPool pool, List<String> tagged) {
        Set<String> fromTags = Set.copyOf(tagged);
        List<DropEntry> kept = new ArrayList<>();
        for (DropEntry entry : pool.entries()) {
            if (!(entry.isPlain() && fromTags.contains(entry.itemId()))) {
                kept.add(entry);
            }
        }
        return pool.withEntries(kept);
    }
}
