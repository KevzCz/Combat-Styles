package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.entity.AbilityPick;
import net.pixeldreamstudios.combat_styles.entity.SummonAim;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record SummonCadence(Optional<AbilityPick> abilityPick,
                            Optional<Integer> recastTicks,
                            Optional<SummonAim> aim,
                            List<SummonPassive> passives,
                            List<String> carriedAbilities,
                            List<Optional<AbilityTuning>> activeTunings) {

    public static final String KEY_ABILITY_PICK = "ability_pick";
    public static final String KEY_RECAST_TICKS = "recast_ticks";
    public static final String KEY_AIM = "aim";
    public static final String KEY_PASSIVES = "passives";
    public static final int NEVER_RECASTS = 0;

    public static final SummonCadence NONE = new SummonCadence(Optional.empty(),
            Optional.empty(), Optional.empty(), List.of(), List.of(), List.of());

    public static final MapCodec<SummonCadence> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    SummonActives.CODEC.optionalFieldOf(SummonActives.KEY)
                            .forGetter(SummonCadence::actives),
                    AbilityPick.CODEC.optionalFieldOf(KEY_ABILITY_PICK)
                            .forGetter(cadence -> Optional.empty()),
                    Codec.INT.optionalFieldOf(KEY_RECAST_TICKS)
                            .forGetter(cadence -> Optional.empty()),
                    SummonAim.CODEC.optionalFieldOf(KEY_AIM).forGetter(cadence -> Optional.empty()),
                    SummonPassive.CODEC.listOf().optionalFieldOf(KEY_PASSIVES, List.of())
                            .forGetter(SummonCadence::passives)
            ).apply(instance, SummonCadence::decoded));

    public SummonCadence {
        abilityPick = abilityPick == null ? Optional.empty() : abilityPick;
        recastTicks = recastTicks == null ? Optional.empty() : recastTicks.map(ticks ->
                Math.max(NEVER_RECASTS, ticks));
        aim = aim == null ? Optional.empty() : aim;
        passives = passives == null ? List.of() : List.copyOf(passives);
        carriedAbilities = carriedAbilities == null ? List.of() : List.copyOf(carriedAbilities);
        activeTunings = activeTunings == null ? List.of() : List.copyOf(activeTunings);
    }

    private static SummonCadence decoded(Optional<SummonActives> actives,
                                         Optional<AbilityPick> legacyPick,
                                         Optional<Integer> legacyRecast,
                                         Optional<SummonAim> legacyAim,
                                         List<SummonPassive> passives) {
        SummonActives block = actives.orElse(null);
        return new SummonCadence(
                block == null || block.pick().isEmpty() ? legacyPick : block.pick(),
                block == null || block.recastTicks().isEmpty() ? legacyRecast : block.recastTicks(),
                block == null || block.aim().isEmpty() ? legacyAim : block.aim(),
                passives,
                block == null ? List.of() : block.abilities().stream().map(AbilityUse::ability).toList(),
                block == null ? List.of() : block.abilities().stream().map(AbilityUse::tuning).toList());
    }

    private Optional<SummonActives> actives() {
        SummonActives block = new SummonActives(uses(), abilityPick, recastTicks, aim);
        return block.isEmpty() ? Optional.empty() : Optional.of(block);
    }

    public SummonCadence carrying(List<String> abilities) {
        return new SummonCadence(abilityPick, recastTicks, aim, passives, abilities, activeTunings);
    }

    public List<AbilityUse> uses() {
        List<AbilityUse> uses = new ArrayList<>();
        for (int index = 0; index < carriedAbilities.size(); index++) {
            uses.add(new AbilityUse(carriedAbilities.get(index), tuningAt(index)));
        }
        return uses;
    }

    public Optional<AbilityTuning> tuningAt(int index) {
        return index >= 0 && index < activeTunings.size() ? activeTunings.get(index) : Optional.empty();
    }

    public List<String> abilitiesOr(List<String> fallback) {
        return carriedAbilities.isEmpty() ? fallback : carriedAbilities;
    }

    public boolean isEmpty() {
        return abilityPick.isEmpty() && recastTicks.isEmpty() && aim.isEmpty() && passives.isEmpty();
    }
}
