package net.pixeldreamstudios.combat_styles.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public record DropPool(double chance, List<String> tables, List<String> tablePatterns,
                       List<String> entities, List<String> entityTags, List<String> structures,
                       List<String> structureTags, List<DropEntry> entries) {

    public static final String KEY_CHANCE = "chance";
    public static final String KEY_TABLES = "tables";
    public static final String KEY_TABLE_PATTERNS = "table_patterns";
    public static final String KEY_ENTITIES = "entities";
    public static final String KEY_ENTITY_TAGS = "entity_tags";
    public static final String KEY_STRUCTURES = "structures";
    public static final String KEY_STRUCTURE_TAGS = "structure_tags";
    public static final String KEY_ENTRIES = "entries";
    public static final String TAG_MARKER = "#";
    private static final double NEVER = 0.0D;

    public static final Codec<DropPool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf(KEY_CHANCE, NEVER).forGetter(DropPool::chance),
            Codec.STRING.listOf().optionalFieldOf(KEY_TABLES, List.of()).forGetter(DropPool::tables),
            Codec.STRING.listOf().optionalFieldOf(KEY_TABLE_PATTERNS, List.of())
                    .forGetter(DropPool::tablePatterns),
            Codec.STRING.listOf().optionalFieldOf(KEY_ENTITIES, List.of()).forGetter(DropPool::entities),
            Codec.STRING.listOf().optionalFieldOf(KEY_ENTITY_TAGS, List.of()).forGetter(DropPool::entityTags),
            Codec.STRING.listOf().optionalFieldOf(KEY_STRUCTURES, List.of()).forGetter(DropPool::structures),
            Codec.STRING.listOf().optionalFieldOf(KEY_STRUCTURE_TAGS, List.of())
                    .forGetter(DropPool::structureTags),
            DropEntry.CODEC.listOf().optionalFieldOf(KEY_ENTRIES, List.of()).forGetter(DropPool::entries)
    ).apply(instance, DropPool::new));

    public static final DropPool EMPTY = new DropPool(NEVER, List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of());

    public DropPool {
        tables = copy(tables);
        tablePatterns = copy(tablePatterns);
        entities = copy(entities);
        entityTags = copy(entityTags);
        structures = copy(structures);
        structureTags = copy(structureTags);
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public DropPool withEntries(List<DropEntry> replaced) {
        return new DropPool(chance, tables, tablePatterns, entities, entityTags, structures,
                structureTags, replaced);
    }

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
            String needle = (tag.startsWith(TAG_MARKER) ? tag.substring(1) : tag).toLowerCase(Locale.ROOT);
            if (structuresHere.contains(TAG_MARKER + needle)) {
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
                    tag.startsWith(TAG_MARKER) ? tag.substring(1) : tag);
            if (parsed != null && entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, parsed))) {
                return true;
            }
        }
        return false;
    }
}
