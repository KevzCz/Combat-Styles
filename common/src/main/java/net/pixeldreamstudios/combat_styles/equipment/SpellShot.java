package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SpellShot(String spell, SpellTuning tuning) {

    public static final SpellShot NONE = new SpellShot("", SpellTuning.NONE);

    public static final Codec<SpellShot> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.STRING.optionalFieldOf("spell", "").forGetter(SpellShot::spell),
                    SpellTuning.CODEC.optionalFieldOf("tuning", SpellTuning.NONE)
                            .forGetter(SpellShot::tuning)
            ).apply(instance, SpellShot::new));

    public SpellShot {
        spell = spell == null ? "" : spell;
        tuning = tuning == null ? SpellTuning.NONE : tuning;
    }

    public boolean isEmpty() {
        return spell.isBlank();
    }
}
