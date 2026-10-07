package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.entity.AbilityPick;
import net.pixeldreamstudios.combat_styles.entity.SummonAim;

import java.util.List;
import java.util.Optional;

public record SummonActives(List<AbilityUse> abilities, Optional<AbilityPick> pick,
                            Optional<Integer> recastTicks, Optional<SummonAim> aim) {

    public static final String KEY = "actives";
    public static final String KEY_ABILITIES = "abilities";
    public static final String KEY_PICK = "pick";

    public static final Codec<SummonActives> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AbilityUse.CODEC.listOf().optionalFieldOf(KEY_ABILITIES, List.of())
                    .forGetter(SummonActives::abilities),
            AbilityPick.CODEC.optionalFieldOf(KEY_PICK).forGetter(SummonActives::pick),
            Codec.INT.optionalFieldOf(SummonCadence.KEY_RECAST_TICKS)
                    .forGetter(SummonActives::recastTicks),
            SummonAim.CODEC.optionalFieldOf(SummonCadence.KEY_AIM).forGetter(SummonActives::aim)
    ).apply(instance, SummonActives::new));

    public static final SummonActives SAMPLE = new SummonActives(List.of(AbilityUse.SAMPLE),
            Optional.of(AbilityPick.RANDOM), Optional.empty(), Optional.empty());

    public SummonActives {
        abilities = abilities == null ? List.of() : List.copyOf(abilities);
        pick = pick == null ? Optional.empty() : pick;
        recastTicks = recastTicks == null ? Optional.empty() : recastTicks;
        aim = aim == null ? Optional.empty() : aim;
    }

    public boolean isEmpty() {
        return abilities.isEmpty() && pick.isEmpty() && recastTicks.isEmpty() && aim.isEmpty();
    }
}
