package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record CustomStyle(String id, String group, StyleEntry entry,
                          Optional<String> category, Optional<StyleRarity> rarity,
                          Optional<Double> costOverride,
                          Optional<Boolean> stylePage) {

    public static final String DEFAULT_GROUP = "custom";

    public static final Codec<CustomStyle> CODEC = Customs.wrap(CustomStyle.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CustomStyle::id),
            Codec.STRING.optionalFieldOf("group", DEFAULT_GROUP).forGetter(CustomStyle::group),
            StyleEntry.CODEC.fieldOf("style").forGetter(CustomStyle::entry),
            Codec.STRING.optionalFieldOf("category").forGetter(CustomStyle::category),
            StyleRarity.CODEC.optionalFieldOf("rarity").forGetter(CustomStyle::rarity),
            Codec.DOUBLE.optionalFieldOf("cost").forGetter(CustomStyle::costOverride),
            Codec.BOOL.optionalFieldOf(StylePool.KEY_STYLE_PAGE).forGetter(CustomStyle::stylePage)
    ).apply(instance, CustomStyle::new)));

    public static final StreamCodec<ByteBuf, CustomStyle> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public CustomStyle decode(ByteBuf buffer) {
            String id = ByteBufCodecs.STRING_UTF8.decode(buffer);
            String group = ByteBufCodecs.STRING_UTF8.decode(buffer);
            StyleEntry entry = StyleEntry.STREAM_CODEC.decode(buffer);
            Optional<String> category = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buffer);
            Optional<String> rarityKey = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buffer);
            Optional<Double> cost = ByteBufCodecs.optional(ByteBufCodecs.DOUBLE).decode(buffer);
            Optional<Boolean> stylePage = ByteBufCodecs.optional(ByteBufCodecs.BOOL).decode(buffer);
            return new CustomStyle(id, group, entry, category,
                    rarityKey.map(CustomStyle::rarityOf), cost, stylePage);
        }

        @Override
        public void encode(ByteBuf buffer, CustomStyle style) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, style.id());
            ByteBufCodecs.STRING_UTF8.encode(buffer, style.group());
            StyleEntry.STREAM_CODEC.encode(buffer, style.entry());
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buffer, style.category());
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8)
                    .encode(buffer, style.rarity().map(StyleRarity::key));
            ByteBufCodecs.optional(ByteBufCodecs.DOUBLE).encode(buffer, style.costOverride());
            ByteBufCodecs.optional(ByteBufCodecs.BOOL).encode(buffer, style.stylePage());
        }
    };

    public static final StreamCodec<ByteBuf, Map<String, CustomStyle>> MAP_STREAM_CODEC =
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, STREAM_CODEC);

    private static StyleRarity rarityOf(String key) {
        for (StyleRarity rarity : StyleRarity.values()) {
            if (rarity.key().equalsIgnoreCase(key)) {
                return rarity;
            }
        }
        return StyleRarity.COMMON;
    }

    public StyleEntry mergedWith(StyleEntry override) {
        if (override == null) {
            return entry;
        }

        List<String> affixIds = override.affixIds().isEmpty()
                ? entry.affixIds()
                : override.affixIds();

        Map<String, AffixTuning> tuning = new LinkedHashMap<>(entry.affixTuning());
        tuning.putAll(override.affixTuning());

        String styleId = override.styleId().isBlank() ? entry.styleId() : override.styleId();

        return new StyleEntry(
                styleId,
                override.soundId().or(entry::soundId),
                new ArrayList<>(affixIds),
                override.attackOverride().or(entry::attackOverride),
                override.name().or(entry::name),
                override.nameKey().or(entry::nameKey),
                tuning,
                Optional.of(id));
    }

    public StyleEntry asEntry() {
        return new StyleEntry(entry.styleId(), entry.soundId(), entry.affixIds(),
                entry.attackOverride(), entry.name(), entry.nameKey(), entry.affixTuning(),
                Optional.of(id));
    }
}
