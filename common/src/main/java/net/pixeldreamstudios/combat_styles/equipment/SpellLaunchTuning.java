package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SpellLaunchTuning(int extraLaunches, int launchDelay, float velocityAdd,
                                float scaleAdd, float meteorRadiusAdd) {

    public static final int SPELL_DELAY = 0;

    public static final SpellLaunchTuning NONE =
            new SpellLaunchTuning(0, SPELL_DELAY, 0.0F, 0.0F, 0.0F);

    public static final Codec<SpellLaunchTuning> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("extra_launches", 0)
                            .forGetter(SpellLaunchTuning::extraLaunches),
                    Codec.intRange(0, Integer.MAX_VALUE)
                            .optionalFieldOf("launch_delay", SPELL_DELAY)
                            .forGetter(SpellLaunchTuning::launchDelay),
                    Codec.FLOAT.optionalFieldOf("velocity_add", 0.0F)
                            .forGetter(SpellLaunchTuning::velocityAdd),
                    Codec.FLOAT.optionalFieldOf("scale_add", 0.0F)
                            .forGetter(SpellLaunchTuning::scaleAdd),
                    Codec.FLOAT.optionalFieldOf("meteor_radius_add", 0.0F)
                            .forGetter(SpellLaunchTuning::meteorRadiusAdd)
            ).apply(instance, SpellLaunchTuning::new));

    public boolean isEmpty() {
        return equals(NONE);
    }
}
