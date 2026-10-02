package net.pixeldreamstudios.combat_styles.loot;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.style.StyleEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record WeaponRoll(List<String> items, List<String> categories, List<String> tables,
                         List<String> tags,
                         float chance, int minSlots, int maxSlots,
                         float affixChance, int maxAffixes,
                         List<StyleEntry> styles, Map<String, String> names,
                         Optional<Integer> slotLimit, String mode,
                         List<Integer> slots, List<String> affixes,
                         Optional<String> name) {

    public static final String MODE_APPEND = "append";
    public static final String MODE_REPLACE = "replace";
    public static final String MODE_SLOT = "slot";
    public static final String MODE_AFFIX = "affix";

    private static final List<String> MODES =
            List.of(MODE_APPEND, MODE_REPLACE, MODE_SLOT, MODE_AFFIX);

    private static final Codec<String> MODE_CODEC = Codec.STRING.validate(mode ->
            MODES.stream().anyMatch(known -> known.equalsIgnoreCase(mode))
                    ? DataResult.success(mode)
                    : DataResult.error(() -> "Unknown weapon roll mode '" + mode
                            + "', expected one of " + MODES));

    private static final Codec<StyleEntry> STYLE_ENTRY_CODEC = Codec.either(
                    Codec.STRING, StyleEntry.CODEC)
            .xmap(either -> either.map(StyleEntry::of, entry -> entry),
                    entry -> entry.isPlain()
                            ? Either.left(entry.styleId())
                            : Either.right(entry));

    private static final float DEFAULT_CHANCE = 0.05F;
    private static final int DEFAULT_MIN_SLOTS = 1;
    private static final int DEFAULT_MAX_SLOTS = 3;
    private static final float DEFAULT_AFFIX_CHANCE = 0.25F;
    private static final int DEFAULT_MAX_AFFIXES = 1;

    public boolean appends() {
        return MODE_APPEND.equalsIgnoreCase(mode);
    }

    public boolean replaces() {
        return MODE_REPLACE.equalsIgnoreCase(mode);
    }

    public boolean rewritesSlots() {
        return MODE_SLOT.equalsIgnoreCase(mode);
    }

    public boolean affixesOnly() {
        return MODE_AFFIX.equalsIgnoreCase(mode);
    }

    private record Targeting(List<String> items, List<String> categories, List<String> tables,
                             List<String> tags, float chance, int minSlots, int maxSlots,
                             float affixChance, int maxAffixes) {
    }

    private record Content(List<StyleEntry> styles, Map<String, String> names,
                           Optional<Integer> slotLimit, String mode,
                           List<Integer> slots, List<String> affixes,
                           Optional<String> name) {
    }

    private static final MapCodec<Targeting> TARGETING_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("items", List.of()).forGetter(Targeting::items),
            Codec.STRING.listOf().optionalFieldOf("categories", List.of())
                    .forGetter(Targeting::categories),
            Codec.STRING.listOf().optionalFieldOf("tables", List.of()).forGetter(Targeting::tables),
            Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(Targeting::tags),
            Codec.FLOAT.optionalFieldOf("chance", DEFAULT_CHANCE).forGetter(Targeting::chance),
            Codec.INT.optionalFieldOf("min_slots", DEFAULT_MIN_SLOTS).forGetter(Targeting::minSlots),
            Codec.INT.optionalFieldOf("max_slots", DEFAULT_MAX_SLOTS).forGetter(Targeting::maxSlots),
            Codec.FLOAT.optionalFieldOf("affix_chance", DEFAULT_AFFIX_CHANCE)
                    .forGetter(Targeting::affixChance),
            Codec.INT.optionalFieldOf("max_affixes", DEFAULT_MAX_AFFIXES)
                    .forGetter(Targeting::maxAffixes)
    ).apply(instance, Targeting::new));

    private static final MapCodec<Content> CONTENT_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            STYLE_ENTRY_CODEC.listOf().optionalFieldOf("styles", List.of())
                    .forGetter(Content::styles),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("names", Map.of()).forGetter(Content::names),
            Codec.INT.optionalFieldOf("slot_limit").forGetter(Content::slotLimit),
            MODE_CODEC.optionalFieldOf("mode", MODE_APPEND).forGetter(Content::mode),
            Codec.either(Codec.INT, Codec.INT.listOf())
                    .xmap(either -> either.map(List::of, list -> list),
                            list -> list.size() == 1
                                    ? Either.left(list.getFirst())
                                    : Either.right(list))
                    .optionalFieldOf("slots", List.of()).forGetter(Content::slots),
            Codec.STRING.listOf().optionalFieldOf("affixes", List.of())
                    .forGetter(Content::affixes),
            Codec.STRING.optionalFieldOf("name").forGetter(Content::name)
    ).apply(instance, Content::new));

    public static final Codec<WeaponRoll> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TARGETING_CODEC.forGetter(roll -> new Targeting(roll.items(), roll.categories(),
                    roll.tables(), roll.tags(), roll.chance(), roll.minSlots(), roll.maxSlots(),
                    roll.affixChance(), roll.maxAffixes())),
            CONTENT_CODEC.forGetter(roll -> new Content(roll.styles(), roll.names(),
                    roll.slotLimit(), roll.mode(), roll.slots(), roll.affixes(), roll.name()))
    ).apply(instance, (targeting, content) -> new WeaponRoll(
            targeting.items(), targeting.categories(), targeting.tables(), targeting.tags(),
            targeting.chance(),
            targeting.minSlots(), targeting.maxSlots(), targeting.affixChance(),
            targeting.maxAffixes(), content.styles(), content.names(), content.slotLimit(),
            content.mode(), content.slots(), content.affixes(), content.name())));
}
