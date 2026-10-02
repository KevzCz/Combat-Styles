package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SpellChainTuning(int size, int triggers, int increment) {

    public static final SpellChainTuning NONE = new SpellChainTuning(0, 0, 0);

    public static final Codec<SpellChainTuning> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.optionalFieldOf("size", 0).forGetter(SpellChainTuning::size),
                    Codec.INT.optionalFieldOf("triggers", 0).forGetter(SpellChainTuning::triggers),
                    Codec.INT.optionalFieldOf("increment", 0)
                            .forGetter(SpellChainTuning::increment)
            ).apply(instance, SpellChainTuning::new));

    public boolean isEmpty() {
        return equals(NONE);
    }
}
