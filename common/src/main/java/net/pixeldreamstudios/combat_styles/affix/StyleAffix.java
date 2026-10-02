package net.pixeldreamstudios.combat_styles.affix;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.api.AffixInfo;
import net.pixeldreamstudios.combat_styles.damage.DamageBonus;
import net.pixeldreamstudios.combat_styles.style.AttackBehaviour;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record StyleAffix(ResourceLocation id,
                         int weight,
                         StyleRarity rarity,
                         Optional<TrailColor> trail,
                         AffixEffects.OnHit onHit,
                         AffixEffects.OnHit onSwing,
                         AppliesTo appliesTo,
                         double costModifier,
                         double damageMultiplier,
                         Optional<AttackBehaviour> behaviour,
                         List<DamageBonus> damageBonus) implements AffixInfo {

    public static final double DEFAULT_COST_MODIFIER = 1.0D;
    private static final int DEFAULT_WEIGHT = 10;

    public StyleAffix {
        damageBonus = damageBonus == null ? List.of() : List.copyOf(damageBonus);
    }

    public static final Codec<StyleAffix> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(StyleAffix::id),
            Codec.INT.optionalFieldOf("weight", DEFAULT_WEIGHT).forGetter(StyleAffix::weight),
            StyleRarity.CODEC.optionalFieldOf("rarity", StyleRarity.UNCOMMON).forGetter(StyleAffix::rarity),
            TrailColor.CODEC.optionalFieldOf("trail").forGetter(StyleAffix::trail),
            AffixEffects.OnHit.CODEC.optionalFieldOf("on_hit", AffixEffects.OnHit.EMPTY).forGetter(StyleAffix::onHit),
            AffixEffects.OnHit.CODEC.optionalFieldOf("on_swing", AffixEffects.OnHit.EMPTY).forGetter(StyleAffix::onSwing),
            AppliesTo.CODEC.optionalFieldOf("applies_to", AppliesTo.ANY).forGetter(StyleAffix::appliesTo),
            Codec.DOUBLE.optionalFieldOf("cost_modifier", DEFAULT_COST_MODIFIER).forGetter(StyleAffix::costModifier),
            Codec.DOUBLE.optionalFieldOf("damage_multiplier", 1.0D).forGetter(StyleAffix::damageMultiplier),
            AttackBehaviour.CODEC.optionalFieldOf("behaviour").forGetter(StyleAffix::behaviour),
            DamageBonus.LIST_CODEC.optionalFieldOf(DamageBonus.KEY, List.of())
                    .forGetter(StyleAffix::damageBonus)
    ).apply(instance, StyleAffix::new));

    public static final StreamCodec<ByteBuf, StyleAffix> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(
                    raw -> CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(raw))
                            .result().orElse(null),
                    affix -> CODEC.encodeStart(JsonOps.INSTANCE, affix)
                            .result().map(JsonElement::toString).orElse(""));

    public static final StreamCodec<ByteBuf, Map<String, StyleAffix>> MAP_STREAM_CODEC =
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, STREAM_CODEC);

    public record TrailColor(String color, boolean glows) {

        public static final Codec<TrailColor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("color").forGetter(TrailColor::color),
                Codec.BOOL.optionalFieldOf("glows", true).forGetter(TrailColor::glows)
        ).apply(instance, TrailColor::new));

        public long rgba() {
            String hex = color.startsWith("#") ? color.substring(1) : color;
            try {
                long value = Long.parseLong(hex, 16);
                return hex.length() <= 6 ? (value << 8) | 0xFFL : value;
            } catch (NumberFormatException ignored) {
                return 0xFFFFFFFFL;
            }
        }

        public int argb() {
            long rgba = rgba();
            return (int) (0xFF000000L | (rgba >> 8));
        }
    }

    public record AppliesTo(List<String> hitboxes, List<String> categories, List<String> animations) {

        public static final AppliesTo ANY = new AppliesTo(List.of(), List.of(), List.of());

        public static final Codec<AppliesTo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("hitbox", List.of()).forGetter(AppliesTo::hitboxes),
                Codec.STRING.listOf().optionalFieldOf("categories", List.of()).forGetter(AppliesTo::categories),
                Codec.STRING.listOf().optionalFieldOf("animations", List.of()).forGetter(AppliesTo::animations)
        ).apply(instance, AppliesTo::new));

        public boolean matches(StylePage page) {
            if (page == null) {
                return false;
            }
            if (!categories.isEmpty() && !categories.contains(page.category())) {
                return false;
            }
            if (!animations.isEmpty()) {
                String animation = page.animation();
                boolean matched = false;
                for (String allowed : animations) {
                    if (animation != null && animation.contains(allowed)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return false;
                }
            }
            if (hitboxes.isEmpty()) {
                return true;
            }
            WeaponAttributes.HitBoxShape hitbox = page.hitbox();
            if (hitbox == null) {
                return false;
            }
            for (String allowed : hitboxes) {
                if (allowed.equalsIgnoreCase(hitbox.name())) {
                    return true;
                }
            }
            return false;
        }
    }

    public String translationKey() {
        return "affix." + id.getNamespace() + "." + id.getPath();
    }

    public String nameKey() {
        return translationKey() + ".name";
    }

    public String descriptionKey() {
        return translationKey() + ".desc";
    }

    public String fallbackName() {
        String path = id.getPath().replace('_', ' ');
        return path.isEmpty() ? "" : Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    public boolean canApplyTo(StylePage page) {
        return appliesTo.matches(page);
    }

    public boolean convertsDamage() {
        return onHit.allDamage().stream().anyMatch(AffixEffects.Damage::converts);
    }

    @Override
    public boolean firesOnSwing() {
        return !onSwing.isEmpty();
    }

    @Override
    public boolean firesOnHit() {
        return !onHit.isEmpty();
    }

    @Override
    public boolean spawnsWave() {
        return onSwing.slashWave().isPresent() || onHit.slashWave().isPresent();
    }

    public String serializedId() {
        return id.toString().toLowerCase(Locale.ROOT);
    }
}
