package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SpellTune(String spell, SpellTuning tuning) {

    public static final Codec<SpellTune> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("spell").forGetter(SpellTune::spell),
            SpellTuning.MAP_CODEC.forGetter(SpellTune::tuning)
    ).apply(instance, SpellTune::new));

    public SpellTune {
        spell = spell == null ? "" : spell;
        tuning = tuning == null ? SpellTuning.NONE : tuning;
    }
}
