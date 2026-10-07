package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.List;

public record MobFilter(List<String> entities, List<String> entityTags,
                        List<String> exemptEntities, List<String> exemptTags) {

    public static final String KEY_ENTITIES = "entities";
    public static final String KEY_ENTITY_TAGS = "entity_tags";
    public static final String KEY_EXEMPT_ENTITIES = "exempt_entities";
    public static final String KEY_EXEMPT_TAGS = "exempt_tags";
    private static final String TAG_MARKER = "#";

    public static final MobFilter ANY = new MobFilter(List.of(), List.of(), List.of(), List.of());

    public static final MapCodec<MobFilter> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf(KEY_ENTITIES, List.of()).forGetter(MobFilter::entities),
            Codec.STRING.listOf().optionalFieldOf(KEY_ENTITY_TAGS, List.of()).forGetter(MobFilter::entityTags),
            Codec.STRING.listOf().optionalFieldOf(KEY_EXEMPT_ENTITIES, List.of())
                    .forGetter(MobFilter::exemptEntities),
            Codec.STRING.listOf().optionalFieldOf(KEY_EXEMPT_TAGS, List.of()).forGetter(MobFilter::exemptTags)
    ).apply(instance, MobFilter::new));

    public MobFilter {
        entities = entities == null ? List.of() : List.copyOf(entities);
        entityTags = entityTags == null ? List.of() : List.copyOf(entityTags);
        exemptEntities = exemptEntities == null ? List.of() : List.copyOf(exemptEntities);
        exemptTags = exemptTags == null ? List.of() : List.copyOf(exemptTags);
    }

    public boolean accepts(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        if (exemptEntities.contains(id) || inAny(type, exemptTags)) {
            return false;
        }
        if (entities.isEmpty() && entityTags.isEmpty()) {
            return true;
        }
        return entities.contains(id) || inAny(type, entityTags);
    }

    private static boolean inAny(EntityType<?> type, List<String> tags) {
        for (String raw : tags) {
            String bare = raw.startsWith(TAG_MARKER) ? raw.substring(1) : raw;
            ResourceLocation id = ResourceLocation.tryParse(bare);
            if (id != null && type.is(TagKey.create(Registries.ENTITY_TYPE, id))) {
                return true;
            }
        }
        return false;
    }
}
