package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

import net.pixeldreamstudios.combat_styles.config.ConfigOptions;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public final class DropSuggestions {

    public static final String LOOT_TABLES = "loot_tables";
    public static final String TABLE_PATTERNS = "table_patterns";
    public static final String ENTITIES = "entities";
    public static final String ENTITY_TAGS = "entity_tags";
    public static final String STRUCTURES = "structures";
    public static final String STRUCTURE_TAGS = "structure_tags";
    public static final String DRAG_MOTIONS = "drag_motions";
    public static final String HUD_DENSITY = ConfigOptions.KIND_HUD_DENSITY;
    public static final String SCHOOLS = ConfigOptions.SUGGEST_SCHOOLS;
    public static final String SCALING_SOURCES = ConfigOptions.SUGGEST_SCALING_SOURCES;

    private static final List<String> MOTIONS = List.of("dash", "blink", "vault");

    private static final List<String> PATTERNS = List.of(
            "chests/", "entities/", "gameplay/", "blocks/", "archaeology/",
            "chests/village/", "shearing/", "spawners/");

    private DropSuggestions() {
    }

    public static List<String> of(String kind) {
        return switch (kind) {
            case LOOT_TABLES -> lootTables();
            case TABLE_PATTERNS -> PATTERNS;
            case ENTITIES -> entities();
            case ENTITY_TAGS -> entityTags();
            case STRUCTURES -> structures();
            case STRUCTURE_TAGS -> structureTags();
            case DRAG_MOTIONS -> MOTIONS;
            case HUD_DENSITY -> HudDensity.keys();
            case SCHOOLS -> SpellPowerAccess.schoolIds();
            case SCALING_SOURCES -> scalingSources();
            default -> List.of();
        };
    }

    private static List<String> scalingSources() {
        TreeSet<String> found = new TreeSet<>(SpellPowerAccess.schoolIds());
        for (ResourceLocation id : BuiltInRegistries.ATTRIBUTE.keySet()) {
            found.add(id.toString());
        }
        return List.copyOf(found);
    }

    private static List<String> lootTables() {
        TreeSet<String> found = new TreeSet<>();
        for (ResourceKey<LootTable> key : BuiltInLootTables.all()) {
            found.add(key.location().toString());
        }
        return List.copyOf(found);
    }

    private static List<String> entities() {
        TreeSet<String> found = new TreeSet<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id != null) {
                found.add(id.toString());
            }
        }
        return List.copyOf(found);
    }

    private static List<String> entityTags() {
        TreeSet<String> found = new TreeSet<>();
        BuiltInRegistries.ENTITY_TYPE.getTagNames()
                .forEach(tag -> found.add("#" + tag.location()));
        return List.copyOf(found);
    }

    private static List<String> structures() {
        Registry<Structure> registry = structureRegistry();
        if (registry == null) {
            return List.of();
        }
        TreeSet<String> found = new TreeSet<>();
        for (ResourceLocation id : registry.keySet()) {
            found.add(id.toString());
        }
        return List.copyOf(found);
    }

    private static List<String> structureTags() {
        Registry<Structure> registry = structureRegistry();
        if (registry == null) {
            return List.of();
        }
        TreeSet<String> found = new TreeSet<>();
        registry.getTagNames().forEach(tag -> found.add("#" + tag.location()));
        return List.copyOf(found);
    }

    private static Registry<Structure> structureRegistry() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getConnection() == null) {
            return null;
        }
        RegistryAccess access = minecraft.getConnection().registryAccess();
        return access.registry(Registries.STRUCTURE).orElse(null);
    }

    public static List<String> matching(String kind, String typed, List<String> already,
                                        int limit) {
        String needle = typed == null ? "" : typed.trim().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String candidate : of(kind)) {
            if (out.size() >= limit) {
                break;
            }
            if (already.contains(candidate)) {
                continue;
            }
            if (needle.isEmpty() || candidate.toLowerCase(Locale.ROOT).contains(needle)) {
                out.add(candidate);
            }
        }
        return out;
    }
}
