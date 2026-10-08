package net.pixeldreamstudios.combat_styles.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EquipmentDropConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "equipment_drops.json";
    public static final int DEFAULT_ITEM_WEIGHT = 10;
    private static final String CHEST_PATTERN = "chests/";
    private static final String ENTITY_PATTERN = "entities/";
    private static final double BASE_CHANCE = 0.05D;
    private static final double RARITY_FALLOFF = 0.6D;

    public static final class Item {
        public int weight = DEFAULT_ITEM_WEIGHT;
        public Map<String, Integer> tableBonus = new LinkedHashMap<>();

        public int weightFor(String tableId) {
            int total = weight;
            for (Map.Entry<String, Integer> bonus : tableBonus.entrySet()) {
                String where = bonus.getKey();
                if (where.equalsIgnoreCase(tableId)
                        || (!where.isEmpty() && tableId.contains(where))) {
                    total += bonus.getValue();
                }
            }
            return Math.max(0, total);
        }
    }

    public static final class Rarity {
        public List<String> tables = new ArrayList<>();
        public List<String> tablePatterns = new ArrayList<>();
        public List<String> entities = new ArrayList<>();
        public List<String> entityTags = new ArrayList<>();
        public List<String> structures = new ArrayList<>();
        public List<String> structureTags = new ArrayList<>();
        public double chance = 0.0D;
        public Map<String, Item> items = new LinkedHashMap<>();

        public boolean matches(String tableId, Entity entity, Set<String> structuresHere) {
            for (String table : tables) {
                if (table.equalsIgnoreCase(tableId)) {
                    return true;
                }
            }
            for (String pattern : tablePatterns) {
                if (!pattern.isEmpty() && tableId.contains(pattern)) {
                    return true;
                }
            }
            return matchesEntity(entity) || matchesStructure(structuresHere);
        }

        private boolean matchesStructure(Set<String> structuresHere) {
            if (structuresHere == null || structuresHere.isEmpty()) {
                return false;
            }
            for (String named : structures) {
                if (structuresHere.contains(named.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            for (String tag : structureTags) {
                String needle = (tag.startsWith("#") ? tag.substring(1) : tag)
                        .toLowerCase(Locale.ROOT);
                if (structuresHere.contains("#" + needle)) {
                    return true;
                }
            }
            return false;
        }

        private boolean matchesEntity(Entity entity) {
            if (entity == null) {
                return false;
            }
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            for (String named : entities) {
                if (named.equalsIgnoreCase(id)) {
                    return true;
                }
            }
            for (String tag : entityTags) {
                ResourceLocation parsed = ResourceLocation.tryParse(
                        tag.startsWith("#") ? tag.substring(1) : tag);
                if (parsed != null && entity.getType().is(
                        TagKey.create(Registries.ENTITY_TYPE, parsed))) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final class Entry {
        public Map<String, Rarity> rarities = new LinkedHashMap<>();
    }

    private static Entry loaded = defaults();

    private EquipmentDropConfig() {
    }

    public static void load() {
        Path file = ConfigPaths.root().resolve(FILE);
        if (!Files.exists(file)) {
            loaded = defaults();
            write(file, loaded);
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Entry parsed = GSON.fromJson(reader, Entry.class);
            loaded = parsed == null || parsed.rarities == null || parsed.rarities.isEmpty()
                    ? defaults()
                    : parsed;
        } catch (IOException | RuntimeException failure) {
            CombatStyles.LOGGER.warn("Could not read {}", file, failure);
            loaded = defaults();
        }
        for (StyleRarity rarity : StyleRarity.values()) {
            loaded.rarities.putIfAbsent(key(rarity), new Rarity());
        }
    }

    public static void save() {
        write(ConfigPaths.root().resolve(FILE), loaded);
    }

    public static void restoreDefaults() {
        loaded = defaults();
    }

    public static Map<String, Rarity> rarities() {
        return loaded.rarities == null ? Map.of() : loaded.rarities;
    }

    public static Rarity rarity(String name) {
        return rarities().get(name);
    }

    public static Item item(String rarityName, String item) {
        Rarity rarity = rarity(rarityName);
        if (rarity == null || rarity.items == null) {
            return null;
        }
        return rarity.items.computeIfAbsent(item, missing -> new Item());
    }

    public static int itemWeight(String rarityName, ResourceLocation item, String tableId) {
        Item entry = item(rarityName, item.toString());
        return entry == null ? DEFAULT_ITEM_WEIGHT : entry.weightFor(tableId);
    }

    public static List<String> matching(ResourceLocation table, Entity entity,
                                        Set<String> structuresHere) {
        String id = table == null ? "" : table.toString();
        List<String> found = new ArrayList<>();
        for (Map.Entry<String, Rarity> entry : rarities().entrySet()) {
            if (entry.getValue().chance > 0.0D && entry.getValue().matches(id, entity, structuresHere)) {
                found.add(entry.getKey());
            }
        }
        return found;
    }

    private static String key(StyleRarity rarity) {
        return rarity.name().toLowerCase(Locale.ROOT);
    }

    private static Entry defaults() {
        Entry entry = new Entry();
        double chance = BASE_CHANCE;
        for (StyleRarity rarity : StyleRarity.values()) {
            Rarity value = new Rarity();
            value.tablePatterns.add(CHEST_PATTERN);
            value.tablePatterns.add(ENTITY_PATTERN);
            value.chance = chance;
            entry.rarities.put(key(rarity), value);
            chance *= RARITY_FALLOFF;
        }
        return entry;
    }

    private static void write(Path file, Entry entry) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(entry, writer);
            }
        } catch (IOException failure) {
            CombatStyles.LOGGER.warn("Could not write {}", file, failure);
        }
    }

    public static JsonObject toJson() {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Rarity> entry : rarities().entrySet()) {
            json.add(entry.getKey(), GSON.toJsonTree(entry.getValue()));
        }
        return json;
    }

    public static void accept(JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            return;
        }
        Entry received = new Entry();
        for (Map.Entry<String, JsonElement> member : json.getAsJsonObject().entrySet()) {
            Rarity parsed = parse(member.getValue());
            if (parsed != null) {
                received.rarities.put(member.getKey(), parsed);
            }
        }
        if (!received.rarities.isEmpty()) {
            loaded = received;
        }
    }

    public static boolean applyRarity(String name, JsonElement json) {
        Rarity parsed = parse(json);
        if (name == null || parsed == null || !rarities().containsKey(name)) {
            return false;
        }
        loaded.rarities.put(name, parsed);
        save();
        return true;
    }

    private static Rarity parse(JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            return null;
        }
        try {
            return GSON.fromJson(json, Rarity.class);
        } catch (RuntimeException failure) {
            CombatStyles.LOGGER.warn("Rejected malformed equipment drop rarity", failure);
            return null;
        }
    }
}
