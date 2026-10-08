package net.pixeldreamstudios.combat_styles.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record AffixTuning(Optional<Double> costModifier,
                          Optional<Double> damageMultiplier,
                          Optional<Double> chance,
                          Optional<Integer> cooldown,
                          Optional<StyleAffix.TrailColor> trail,
                          Optional<Integer> weight,
                          Optional<StyleRarity> rarity,
                          Optional<StyleAffix.AppliesTo> appliesTo,
                          Optional<AffixEffects.OnHit> onHit,
                          Optional<AffixEffects.OnHit> onSwing) {

    public static final AffixTuning NONE = new AffixTuning(Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    public static final Codec<AffixTuning> CODEC = Customs.wrap(AffixTuning.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("cost_modifier").forGetter(AffixTuning::costModifier),
            Codec.DOUBLE.optionalFieldOf("damage_multiplier").forGetter(AffixTuning::damageMultiplier),
            Codec.DOUBLE.optionalFieldOf("chance").forGetter(AffixTuning::chance),
            Codec.INT.optionalFieldOf("cooldown").forGetter(AffixTuning::cooldown),
            StyleAffix.TrailColor.CODEC.optionalFieldOf("trail").forGetter(AffixTuning::trail),
            Codec.INT.optionalFieldOf("weight").forGetter(AffixTuning::weight),
            StyleRarity.CODEC.optionalFieldOf("rarity").forGetter(AffixTuning::rarity),
            StyleAffix.AppliesTo.CODEC.optionalFieldOf("applies_to").forGetter(AffixTuning::appliesTo),
            AffixEffects.OnHit.CODEC.optionalFieldOf("on_hit").forGetter(AffixTuning::onHit),
            AffixEffects.OnHit.CODEC.optionalFieldOf("on_swing").forGetter(AffixTuning::onSwing)
    ).apply(instance, AffixTuning::new)));

    public static final Codec<Map<String, AffixTuning>> MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, CODEC);

    public static final StreamCodec<ByteBuf, AffixTuning> STREAM_CODEC =
            ByteBufCodecs.fromCodec(CODEC);

    public static final StreamCodec<ByteBuf, Map<String, AffixTuning>> MAP_STREAM_CODEC =
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, STREAM_CODEC);

    public boolean isEmpty() {
        return costModifier.isEmpty() && damageMultiplier.isEmpty() && chance.isEmpty()
                && cooldown.isEmpty() && trail.isEmpty() && weight.isEmpty()
                && rarity.isEmpty() && appliesTo.isEmpty()
                && onHit.isEmpty() && onSwing.isEmpty();
    }

    public AffixTuning mergedUnder(AffixTuning weaker) {
        if (weaker == null || weaker.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return weaker;
        }
        return new AffixTuning(
                costModifier.or(weaker::costModifier),
                damageMultiplier.or(weaker::damageMultiplier),
                chance.or(weaker::chance),
                cooldown.or(weaker::cooldown),
                trail.or(weaker::trail),
                weight.or(weaker::weight),
                rarity.or(weaker::rarity),
                appliesTo.or(weaker::appliesTo),
                onHit.or(weaker::onHit),
                onSwing.or(weaker::onSwing));
    }

    public StyleAffix applyTo(StyleAffix affix) {
        if (affix == null || isEmpty()) {
            return affix;
        }
        AffixEffects.OnHit hit = onHit.orElseGet(
                () -> AffixEffects.tune(affix.onHit(), chance, cooldown));
        AffixEffects.OnHit swing = onSwing.orElseGet(
                () -> AffixEffects.tune(affix.onSwing(), chance, cooldown));
        return Customs.copy(affix, new StyleAffix(
                affix.id(),
                weight.orElse(affix.weight()),
                rarity.orElse(affix.rarity()),
                trail.isPresent() ? trail : affix.trail(),
                hit,
                swing,
                appliesTo.orElse(affix.appliesTo()),
                costModifier.orElse(affix.costModifier()),
                damageMultiplier.orElse(affix.damageMultiplier()),
                affix.behaviour(),
                affix.damageBonus()));
    }
}
