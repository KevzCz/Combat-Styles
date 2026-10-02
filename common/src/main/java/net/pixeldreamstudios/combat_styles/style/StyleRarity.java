package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Rarity;

public enum StyleRarity implements StringRepresentable {
    COMMON("common", ChatFormatting.GRAY, Rarity.COMMON, 0.0D),
    UNCOMMON("uncommon", ChatFormatting.GREEN, Rarity.UNCOMMON, 0.12D),
    RARE("rare", ChatFormatting.AQUA, Rarity.RARE, 0.30D),
    EPIC("epic", ChatFormatting.LIGHT_PURPLE, Rarity.EPIC, 0.55D),
    LEGENDARY("legendary", ChatFormatting.GOLD, Rarity.EPIC, 0.75D),
    MYTHIC("mythic", ChatFormatting.RED, Rarity.EPIC, 0.90D);

    public static final Codec<StyleRarity> CODEC = StringRepresentable.fromEnum(StyleRarity::values);

    private final String key;
    private final ChatFormatting color;
    private final Rarity itemRarity;
    private final double threshold;

    StyleRarity(String key, ChatFormatting color, Rarity itemRarity, double threshold) {
        this.key = key;
        this.color = color;
        this.itemRarity = itemRarity;
        this.threshold = threshold;
    }

    public String translationKey() {
        return StyleIds.RARITY_PREFIX + key;
    }

    public ChatFormatting color() {
        return color;
    }

    public Rarity itemRarity() {
        return itemRarity;
    }

    public String key() {
        return key;
    }

    @Override
    public String getSerializedName() {
        return key;
    }

    public static StyleRarity byKey(String key) {
        for (StyleRarity candidate : values()) {
            if (candidate.key.equalsIgnoreCase(key)) {
                return candidate;
            }
        }
        return COMMON;
    }

    public static StyleRarity fromDeviation(double deviation) {
        StyleRarity result = COMMON;
        for (StyleRarity candidate : values()) {
            if (deviation >= candidate.threshold) {
                result = candidate;
            }
        }
        return result;
    }
}
