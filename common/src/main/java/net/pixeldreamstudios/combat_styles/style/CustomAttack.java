package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record CustomAttack(String id, DeclaredAttack attack,
                           Optional<String> category,
                           Optional<StyleRarity> rarity,
                           Optional<Double> costOverride,
                           Optional<String> nameKey) {

    public static final String DEFAULT_CATEGORY = "custom";

    public static final Codec<CustomAttack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CustomAttack::id),
            DeclaredAttack.CODEC.fieldOf("attack").forGetter(CustomAttack::attack),
            Codec.STRING.optionalFieldOf("category").forGetter(CustomAttack::category),
            StyleRarity.CODEC.optionalFieldOf("rarity").forGetter(CustomAttack::rarity),
            Codec.DOUBLE.optionalFieldOf("cost").forGetter(CustomAttack::costOverride),
            Codec.STRING.optionalFieldOf("name_key").forGetter(CustomAttack::nameKey)
    ).apply(instance, CustomAttack::new));

    public static final StreamCodec<ByteBuf, CustomAttack> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC);

    public static final StreamCodec<ByteBuf, Map<String, CustomAttack>> MAP_STREAM_CODEC =
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, STREAM_CODEC);

    public String categoryOrDefault() {
        return category.orElse(DEFAULT_CATEGORY);
    }

    public StyleRarity rarityOrDefault() {
        return rarity.orElse(StyleRarity.COMMON);
    }

    public String translationKey() {
        return nameKey.orElseGet(() -> "style." + id.replace(':', '.'));
    }
}
