package net.pixeldreamstudios.combat_styles.style;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DefaultStyleLoader extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "default_styles";

    public record Sequence(String mode, int slots, List<StyleEntry> entries) {

        public static final String MODE_APPEND = "append";
        public static final String MODE_REPLACE = "replace";

        public static final Sequence NONE = new Sequence(MODE_APPEND, 0, List.of());

        public static final MapCodec<Sequence> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("mode", MODE_APPEND).forGetter(Sequence::mode),
                Codec.INT.optionalFieldOf("max_slots", 0).forGetter(Sequence::slots),
                StyleEntry.CODEC.listOf().optionalFieldOf("entries", List.of())
                        .forGetter(Sequence::entries)
        ).apply(instance, Sequence::new));

        public static final Codec<Sequence> CODEC = MAP_CODEC.codec();

        public Sequence {
            entries = entries == null ? List.of() : List.copyOf(entries);
        }

        public boolean replaces() {
            return MODE_REPLACE.equalsIgnoreCase(mode);
        }

        public boolean isEmpty() {
            return entries.isEmpty() && slots <= 0;
        }
    }

    public record Applies(List<String> items, List<String> tags,
                          List<String> categories, boolean any) {

        public static final Applies NONE = new Applies(List.of(), List.of(), List.of(), false);

        public static final MapCodec<Applies> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("items", List.of()).forGetter(Applies::items),
                Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(Applies::tags),
                Codec.STRING.listOf().optionalFieldOf("categories", List.of())
                        .forGetter(Applies::categories),
                Codec.BOOL.optionalFieldOf("any", false).forGetter(Applies::any)
        ).apply(instance, Applies::new));

        public static final Codec<Applies> CODEC = MAP_CODEC.codec();

        public boolean isEmpty() {
            return items.isEmpty() && tags.isEmpty() && categories.isEmpty() && !any;
        }

        public Applies {
            items = items == null ? List.of() : List.copyOf(items);
            tags = tags == null ? List.of() : List.copyOf(tags);
            categories = categories == null ? List.of() : List.copyOf(categories);
        }
    }

    public record Definition(Applies applies, Sequence sequence, List<SlotTuning> tune,
                             int priority, boolean locked, boolean keepThroughRolls,
                             Optional<String> nameKey) {

        public static final Codec<Definition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Applies.CODEC.optionalFieldOf("applies_to", Applies.NONE).forGetter(Definition::applies),
                Sequence.CODEC.optionalFieldOf("sequence", Sequence.NONE).forGetter(Definition::sequence),
                SlotTuning.CODEC.listOf().optionalFieldOf("tune", List.of()).forGetter(Definition::tune),
                Codec.INT.optionalFieldOf("priority", 0).forGetter(Definition::priority),
                Codec.BOOL.optionalFieldOf("locked", false).forGetter(Definition::locked),
                Codec.BOOL.optionalFieldOf("keep_through_rolls", false)
                        .forGetter(Definition::keepThroughRolls),
                Codec.STRING.optionalFieldOf("name_key").forGetter(Definition::nameKey)
        ).apply(instance, Definition::new));

        public Definition {
            tune = tune == null ? List.of() : List.copyOf(tune);
        }

        public List<String> items() {
            return applies.items();
        }

        public List<String> tags() {
            return applies.tags();
        }

        public List<String> categories() {
            return applies.categories();
        }

        public boolean any() {
            return applies.any();
        }

        public List<StyleEntry> entries() {
            return sequence.entries();
        }

        public int slots() {
            return sequence.slots();
        }

        public String name() {
            return "";
        }

        public List<SlotTuning> tunings() {
            return tune;
        }

        public boolean appends() {
            return !sequence.replaces() && !tunes();
        }

        public boolean tunes() {
            return !tune.isEmpty();
        }
    }

    public DefaultStyleLoader() {
        super(CombatStyles.GSON, DIRECTORY);
    }

    private static final Map<ResourceLocation, JsonElement> LAST_LOADED = new LinkedHashMap<>();

    public static void reapply() {
        if (LAST_LOADED.isEmpty()) {
            return;
        }
        applyTo(new LinkedHashMap<>(LAST_LOADED));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        LAST_LOADED.clear();
        LAST_LOADED.putAll(object);
        applyTo(object);
    }

    private static void applyTo(Map<ResourceLocation, JsonElement> object) {
        Map<ResourceLocation, StyleSequence> loaded = new LinkedHashMap<>();
        Map<ResourceLocation, Integer> slotLimits = new LinkedHashMap<>();
        Set<ResourceLocation> appending = new HashSet<>();
        Set<ResourceLocation> tuning = new HashSet<>();
        Set<ResourceLocation> sticky = new HashSet<>();
        Map<ResourceLocation, List<SlotTuning>> slotTunings = new HashMap<>();

        Map<ResourceLocation, Integer> priorities = new HashMap<>();
        Set<ResourceLocation> locked = new HashSet<>();
        int rejected = 0;
        int unresolved = 0;

        List<Map.Entry<ResourceLocation, Definition>> parsed = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> file : object.entrySet()) {
            Definition definition = Definition.CODEC
                    .parse(JsonOps.INSTANCE, file.getValue())
                    .resultOrPartial(error ->
                            CombatStyles.LOGGER.error("Invalid default style {}: {}", file.getKey(), error))
                    .orElse(null);
            if (definition == null) {
                rejected++;
                continue;
            }
            if (definition.entries().isEmpty() && definition.slots() <= 0) {
                CombatStyles.LOGGER.warn("Default style {} has no entries and no slot override",
                        file.getKey());
                continue;
            }
            parsed.add(Map.entry(file.getKey(), definition));
        }

        parsed.sort(Comparator.comparingInt(
                (Map.Entry<ResourceLocation, Definition> entry) -> entry.getValue().priority())
                .reversed());

        for (Map.Entry<ResourceLocation, Definition> file : parsed) {
            Definition definition = file.getValue();

            List<StyleEntry> resolvedEntries = new ArrayList<>(definition.entries().size());
            for (StyleEntry entry : definition.entries()) {
                if (entry.customStyleId().isEmpty()
                        && StyleRegistry.get(entry.styleId()) == null) {
                    unresolved++;
                    continue;
                }
                resolvedEntries.add(entry);
            }

            String name = definition.nameKey()
                    .map(key -> Component.translatable(key).getString())
                    .orElse(definition.name());

            StyleSequence sequence = resolvedEntries.isEmpty()
                    ? null
                    : new StyleSequence(name, resolvedEntries);

            for (ResourceLocation itemId : targets(definition)) {
                int priority = definition.priority();
                Integer existing = priorities.get(itemId);
                if (existing != null && existing > priority) {
                    continue;
                }
                priorities.put(itemId, priority);

                if (sequence != null) {
                    loaded.put(itemId, sequence);
                    if (definition.appends()) {
                        appending.add(itemId);
                    } else {
                        appending.remove(itemId);
                    }
                    if (definition.tunes()) {
                        tuning.add(itemId);
                    } else {
                        tuning.remove(itemId);
                    }
                    if (!definition.tunings().isEmpty()) {
                        slotTunings.put(itemId, definition.tunings());
                    }
                    if (definition.keepThroughRolls()) {
                        sticky.add(itemId);
                    } else {
                        sticky.remove(itemId);
                    }
                }
                if (definition.slots() > 0) {
                    slotLimits.put(itemId, definition.slots());
                }
                if (definition.locked()) {
                    locked.add(itemId);
                }
            }
        }

        DefaultStyles.acceptDatapack(loaded, slotLimits, appending);
        DefaultStyles.acceptTuning(tuning);
        DefaultStyles.acceptSticky(sticky);
        DefaultStyles.acceptSlotTunings(slotTunings);
        DefaultStyles.acceptLocked(locked);
        CombatStyles.LOGGER.info(
                "Loaded {} default combat style definitions ({} slot overrides, {} rejected, "
                        + "{} unresolved entries)",
                loaded.size(), slotLimits.size(), rejected, unresolved);
    }

    public static List<ResourceLocation> targetsOf(Definition definition) {
        return targets(definition);
    }

    private static List<ResourceLocation> targets(Definition definition) {
        List<ResourceLocation> targets = new ArrayList<>();
        if (definition.any()) {
            targets.addAll(WeaponRegistryAccess.registrations().keySet());
        }
        for (String raw : definition.items()) {
            ResourceLocation itemId = ResourceLocation.tryParse(raw);
            if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId)) {
                targets.add(itemId);
            }
        }
        for (String raw : definition.tags()) {
            ResourceLocation tagId = ResourceLocation.tryParse(raw);
            if (tagId == null) {
                continue;
            }
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
            for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(holder.value());
                if (itemId != null) {
                    targets.add(itemId);
                }
            }
        }
        for (String category : definition.categories()) {
            for (Map.Entry<ResourceLocation, WeaponAttributes> entry
                    : WeaponRegistryAccess.registrations().entrySet()) {
                WeaponAttributes attributes = entry.getValue();
                if (attributes != null && category.equalsIgnoreCase(attributes.category())) {
                    targets.add(entry.getKey());
                }
            }
        }
        return targets;
    }
}
