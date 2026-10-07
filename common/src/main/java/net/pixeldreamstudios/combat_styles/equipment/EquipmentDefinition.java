package net.pixeldreamstudios.combat_styles.equipment;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.style.InnateAffixes;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record EquipmentDefinition(String id,
                                  Map<String, TriggeredEffect> effects,
                                  Map<String, AttributeBonus> attributes,
                                  Optional<StyleRarity> rarity,
                                  List<InnateAffixes.Slot> innateAffixes,
                                  Map<String, EffectGroup> groups,
                                  Optional<String> pose,
                                  Optional<String> offHandPose,
                                  PoseTuning poseTuning,
                                  List<String> items,
                                  List<String> itemTags,
                                  List<String> weaponCategories,
                                  List<String> excludedItems,
                                  List<String> excludedTags,
                                  List<String> excludedCategories,
                                  UseSpec use,
                                  Optional<String> weaponCategory,
                                  ActiveIn activeIn) {

    private static final String WEAPON_CATEGORY = "weapon_category";

    private record Extras(Optional<String> weaponCategory, ActiveIn activeIn) {
    }

    private static final MapCodec<Extras> EXTRAS = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf(WEAPON_CATEGORY).forGetter(Extras::weaponCategory),
            ActiveIn.CODEC.optionalFieldOf(ActiveIn.KEY, ActiveIn.ANY).forGetter(Extras::activeIn)
    ).apply(instance, Extras::new));

    private static final MapCodec<EquipmentDefinition> CORE = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(EquipmentDefinition::id),
            Codec.unboundedMap(Codec.STRING, TriggeredEffect.CODEC)
                    .optionalFieldOf("effects", Map.of())
                    .forGetter(EquipmentDefinition::effects),
            Codec.unboundedMap(Codec.STRING, AttributeBonus.CODEC)
                    .optionalFieldOf("attributes", Map.of())
                    .forGetter(EquipmentDefinition::attributes),
            StyleRarity.CODEC.optionalFieldOf("rarity").forGetter(EquipmentDefinition::rarity),
            InnateAffixes.Slot.CODEC.listOf().optionalFieldOf("innate_affixes", List.of())
                    .forGetter(EquipmentDefinition::innateAffixes),
            Codec.unboundedMap(Codec.STRING, EffectGroup.CODEC)
                    .optionalFieldOf("groups", Map.of())
                    .forGetter(EquipmentDefinition::groups),
            Codec.STRING.optionalFieldOf("pose").forGetter(EquipmentDefinition::pose),
            Codec.STRING.optionalFieldOf("off_hand_pose")
                    .forGetter(EquipmentDefinition::offHandPose),
            PoseTuning.CODEC.optionalFieldOf("pose_tuning", PoseTuning.NONE)
                    .forGetter(EquipmentDefinition::poseTuning),
            Codec.STRING.listOf().optionalFieldOf("items", List.of())
                    .forGetter(EquipmentDefinition::items),
            Codec.STRING.listOf().optionalFieldOf("item_tags", List.of())
                    .forGetter(EquipmentDefinition::itemTags),
            Codec.STRING.listOf().optionalFieldOf("weapon_categories", List.of())
                    .forGetter(EquipmentDefinition::weaponCategories),
            Codec.STRING.listOf().optionalFieldOf("excluded_items", List.of())
                    .forGetter(EquipmentDefinition::excludedItems),
            Codec.STRING.listOf().optionalFieldOf("excluded_tags", List.of())
                    .forGetter(EquipmentDefinition::excludedTags),
            Codec.STRING.listOf().optionalFieldOf("excluded_categories", List.of())
                    .forGetter(EquipmentDefinition::excludedCategories),
            UseSpec.CODEC.optionalFieldOf("use", UseSpec.NONE)
                    .forGetter(EquipmentDefinition::use)
    ).apply(instance, EquipmentDefinition::new));

    public static final Codec<EquipmentDefinition> CODEC = Codec.mapPair(CORE, EXTRAS)
            .xmap(pair -> pair.getFirst().withWeaponCategory(pair.getSecond().weaponCategory())
                            .withActiveIn(pair.getSecond().activeIn()),
                    definition -> Pair.of(definition,
                            new Extras(definition.weaponCategory(), definition.activeIn())))
            .codec();

    public static final StreamCodec<ByteBuf, EquipmentDefinition> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(
                    raw -> CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(raw))
                            .result().orElse(null),
                    definition -> CODEC.encodeStart(JsonOps.INSTANCE, definition)
                            .result().map(JsonElement::toString).orElse(""));

    public static final StreamCodec<ByteBuf, Map<String, EquipmentDefinition>> MAP_STREAM_CODEC =
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, STREAM_CODEC);

    public EquipmentDefinition {
        effects = effects == null ? Map.of() : withEventPayloads(effects, groups);
        attributes = attributes == null ? Map.of() : new LinkedHashMap<>(attributes);
        innateAffixes = innateAffixes == null ? List.of() : List.copyOf(innateAffixes);
        groups = groups == null ? Map.of() : withTriggerChances(groups, effects);
        poseTuning = poseTuning == null ? PoseTuning.NONE : poseTuning;
        items = items == null ? List.of() : List.copyOf(items);
        itemTags = itemTags == null ? List.of() : List.copyOf(itemTags);
        weaponCategories = weaponCategories == null ? List.of() : List.copyOf(weaponCategories);
        excludedItems = excludedItems == null ? List.of() : List.copyOf(excludedItems);
        excludedTags = excludedTags == null ? List.of() : List.copyOf(excludedTags);
        use = use == null ? UseSpec.NONE : use;
        weaponCategory = weaponCategory == null
                ? Optional.empty()
                : weaponCategory.map(String::trim).filter(category -> !category.isEmpty());
        excludedCategories = excludedCategories == null
                ? List.of()
                : List.copyOf(excludedCategories);
        activeIn = activeIn == null ? ActiveIn.ANY : activeIn;
    }

    public EquipmentDefinition(String id, Map<String, TriggeredEffect> effects,
                               Map<String, AttributeBonus> attributes, Optional<StyleRarity> rarity,
                               List<InnateAffixes.Slot> innateAffixes, Map<String, EffectGroup> groups,
                               Optional<String> pose, Optional<String> offHandPose,
                               PoseTuning poseTuning, List<String> items, List<String> itemTags,
                               List<String> weaponCategories, List<String> excludedItems,
                               List<String> excludedTags, List<String> excludedCategories,
                               UseSpec use, Optional<String> weaponCategory) {
        this(id, effects, attributes, rarity, innateAffixes, groups, pose, offHandPose, poseTuning,
                items, itemTags, weaponCategories, excludedItems, excludedTags, excludedCategories,
                use, weaponCategory, ActiveIn.ANY);
    }

    public EquipmentDefinition(String id, Map<String, TriggeredEffect> effects,
                               Map<String, AttributeBonus> attributes, Optional<StyleRarity> rarity,
                               List<InnateAffixes.Slot> innateAffixes, Map<String, EffectGroup> groups,
                               Optional<String> pose, Optional<String> offHandPose,
                               PoseTuning poseTuning, List<String> items, List<String> itemTags,
                               List<String> weaponCategories, List<String> excludedItems,
                               List<String> excludedTags, List<String> excludedCategories,
                               UseSpec use) {
        this(id, effects, attributes, rarity, innateAffixes, groups, pose, offHandPose, poseTuning,
                items, itemTags, weaponCategories, excludedItems, excludedTags, excludedCategories,
                use, Optional.empty(), ActiveIn.ANY);
    }

    public EquipmentDefinition withWeaponCategory(Optional<String> category) {
        return new EquipmentDefinition(id, effects, attributes, rarity, innateAffixes, groups, pose,
                offHandPose, poseTuning, items, itemTags, weaponCategories, excludedItems,
                excludedTags, excludedCategories, use, category, activeIn);
    }

    public EquipmentDefinition withActiveIn(ActiveIn scope) {
        return new EquipmentDefinition(id, effects, attributes, rarity, innateAffixes, groups, pose,
                offHandPose, poseTuning, items, itemTags, weaponCategories, excludedItems,
                excludedTags, excludedCategories, use, weaponCategory, scope);
    }

    private static Map<String, TriggeredEffect> withEventPayloads(Map<String, TriggeredEffect> effects,
                                                                  Map<String, EffectGroup> groups) {
        Map<String, TriggeredEffect> migrated = new LinkedHashMap<>();
        for (Map.Entry<String, TriggeredEffect> named : effects.entrySet()) {
            EquipmentTrigger groupTrigger = null;
            if (groups != null) {
                for (EffectGroup group : groups.values()) {
                    if (group.holds(named.getKey())) {
                        groupTrigger = group.trigger();
                        break;
                    }
                }
            }
            migrated.put(named.getKey(), EventPayloads.migrated(named.getValue(), groupTrigger));
        }
        return migrated;
    }

    private static Map<String, EffectGroup> withTriggerChances(Map<String, EffectGroup> groups,
                                                               Map<String, TriggeredEffect> effects) {
        Map<String, EffectGroup> synced = new LinkedHashMap<>();
        for (Map.Entry<String, EffectGroup> named : groups.entrySet()) {
            EffectGroup group = named.getValue();
            List<EffectGroup.Entry> entries = new ArrayList<>(group.entries().size());
            boolean changed = false;
            for (EffectGroup.Entry entry : group.entries()) {
                TriggeredEffect effect = effects.get(entry.effect());
                Float chance = effect == null ? null : effect.trigger().chance().orElse(null);
                if (chance != null && chance != entry.chance()) {
                    entries.add(new EffectGroup.Entry(entry.effect(), chance));
                    changed = true;
                } else {
                    entries.add(entry);
                }
            }
            synced.put(named.getKey(), changed ? group.withEntries(entries) : group);
        }
        return synced;
    }

    public boolean definesUse() {
        return !use.isEmpty();
    }

    public boolean definesPose() {
        return pose.isPresent() || offHandPose.isPresent() || !poseTuning.isEmpty();
    }

    public boolean broadlyMatched() {
        return !items.isEmpty() || !itemTags.isEmpty() || !weaponCategories.isEmpty();
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !broadlyMatched()) {
            return false;
        }
        if (excluded(stack)) {
            return false;
        }
        if (!items.isEmpty() && WeaponMatch.listsItem(stack, items)) {
            return true;
        }
        return !(weaponCategories.isEmpty() && itemTags.isEmpty())
                && WeaponMatch.matches(stack, weaponCategories, itemTags);
    }

    private boolean excluded(ItemStack stack) {
        if (!excludedItems.isEmpty() && WeaponMatch.listsItem(stack, excludedItems)) {
            return true;
        }
        return !(excludedTags.isEmpty() && excludedCategories.isEmpty())
                && WeaponMatch.matches(stack, excludedCategories, excludedTags);
    }

    public EffectGroup groupOf(String effect) {
        for (EffectGroup group : groups.values()) {
            if (group.holds(effect)) {
                return group;
            }
        }
        return null;
    }

    public boolean isPermanent(String key, TriggeredEffect entry) {
        if (!entry.trigger().isAlways() || entry.trigger().rollsChance()) {
            return false;
        }
        EffectGroup group = groupOf(key);
        return group == null || (!group.gated() && !group.draws());
    }

    public boolean groupSubstitutes(EffectGroup group) {
        for (EffectGroup.Entry entry : group.entries()) {
            TriggeredEffect effect = effects.get(entry.effect());
            if (effect != null && substitutes(effect.effect())) {
                return true;
            }
        }
        return false;
    }

    public boolean groupAlwaysOn(EffectGroup group) {
        for (EffectGroup.Entry entry : group.entries()) {
            TriggeredEffect effect = effects.get(entry.effect());
            if (effect != null && effect.effect().hasAlwaysOnParts()) {
                return true;
            }
        }
        return false;
    }

    public boolean groupArmsOnSwing(EffectGroup group) {
        return group.armsOnSwing();
    }

    public Map<String, AttributeBonus> passiveAttributes() {
        Map<String, AttributeBonus> found = new LinkedHashMap<>(attributes);
        for (Map.Entry<String, TriggeredEffect> named : effects.entrySet()) {
            if (!isPermanent(named.getKey(), named.getValue())) {
                continue;
            }
            for (Map.Entry<String, AttributeBonus> bonus
                    : named.getValue().effect().attributes().entrySet()) {
                found.merge(bonus.getKey(), bonus.getValue(), (a, b) -> b.stacks()
                        ? b.withAmount(a.amount() + b.amount())
                        : a);
            }
        }
        return found;
    }

    public Map<String, AttributeBonus> conditionalAttributes(Player player) {
        return conditionalAttributes(player, 0);
    }

    public Map<String, AttributeBonus> conditionalAttributes(Player player, int copy) {
        Map<String, AttributeBonus> found = new LinkedHashMap<>();
        for (Map.Entry<String, TriggeredEffect> named : effects.entrySet()) {
            TriggeredEffect entry = named.getValue();
            if (isPermanent(named.getKey(), entry)) {
                continue;
            }
            if (!EquipmentRuntime.liveAtRest(player, this, copy, named.getKey(), entry)) {
                continue;
            }
            for (Map.Entry<String, AttributeBonus> bonus
                    : entry.effect().attributes().entrySet()) {
                found.merge(bonus.getKey(), bonus.getValue(), (a, b) -> b.stacks()
                        ? b.withAmount(a.amount() + b.amount())
                        : a);
            }
        }
        return found;
    }

    public List<TriggeredEffect> triggered() {
        return List.copyOf(effects.values());
    }

    public EquipmentEffect substitutingEffect(Player player) {
        String chosen = SubstitutionChoice.of(player, id);
        if (chosen != null) {
            TriggeredEffect picked = effects.get(chosen);
            if (picked != null && substitutes(picked.effect())) {
                return picked.effect();
            }
        }
        for (Map.Entry<String, TriggeredEffect> named : effects.entrySet()) {
            if (substitutes(named.getValue().effect()) && groupOf(named.getKey()) == null) {
                return named.getValue().effect();
            }
        }
        return EquipmentEffect.EMPTY;
    }

    public static boolean substitutes(EquipmentEffect effect) {
        StyleSubstitution substitution = effect.substitution().orElse(null);
        return (substitution != null && !substitution.isEmpty())
                || effect.substituteStyle().isPresent();
    }

    public EquipmentEffect effect() {
        EquipmentEffect merged = EquipmentEffect.EMPTY;
        for (TriggeredEffect entry : effects.values()) {
            if (entry.trigger().isAlways() && !entry.trigger().rollsChance()) {
                merged = merged.mergedWith(entry.effect());
            }
        }
        return merged;
    }

}
