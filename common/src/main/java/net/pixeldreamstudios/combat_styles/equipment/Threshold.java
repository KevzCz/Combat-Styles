package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;

import java.util.Optional;

public record Threshold(String source, Optional<Float> atLeast, Optional<Float> atMost) {

    public static final String KEY = "thresholds";
    public static final String KEY_SOURCE = "source";
    public static final String KEY_AT_LEAST = "at_least";
    public static final String KEY_AT_MOST = "at_most";
    public static final String SAMPLE_SOURCE = "use:charge";
    public static final float SAMPLE_AT_LEAST = 0.8F;
    public static final Threshold SAMPLE = new Threshold(SAMPLE_SOURCE, Optional.of(SAMPLE_AT_LEAST),
            Optional.empty());

    public static final Codec<Threshold> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf(KEY_SOURCE).forGetter(Threshold::source),
            Codec.FLOAT.optionalFieldOf(KEY_AT_LEAST).forGetter(Threshold::atLeast),
            Codec.FLOAT.optionalFieldOf(KEY_AT_MOST).forGetter(Threshold::atMost)
    ).apply(instance, Threshold::new));

    public Threshold {
        source = source == null ? "" : source.trim();
        atLeast = atLeast == null ? Optional.empty() : atLeast;
        atMost = atMost == null ? Optional.empty() : atMost;
    }

    public boolean met(Player player) {
        double value = SpellPowerAccess.valueOf(player, source);
        return atLeast.map(bound -> value >= bound).orElse(true)
                && atMost.map(bound -> value <= bound).orElse(true);
    }
}
