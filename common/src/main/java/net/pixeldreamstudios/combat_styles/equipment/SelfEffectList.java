package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;

import java.util.List;

public record SelfEffectList(List<AffixEffects.SelfEffect> selfEffects) {

    public static final SelfEffectList EMPTY = new SelfEffectList(List.of());

    public static final Codec<SelfEffectList> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    AffixEffects.SelfEffect.CODEC.listOf()
                            .optionalFieldOf("self_effects", List.of())
                            .forGetter(SelfEffectList::selfEffects)
            ).apply(instance, SelfEffectList::new));

    public SelfEffectList {
        selfEffects = selfEffects == null ? List.of() : List.copyOf(selfEffects);
    }
}
