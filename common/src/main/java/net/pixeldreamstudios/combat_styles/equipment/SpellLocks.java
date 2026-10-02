package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record SpellLocks(List<String> grant, List<String> disable, List<String> disableSchools,
                         Mode disableMode, List<SpellTune> tune) {

    public enum Mode implements StringRepresentable {
        BLOCK,
        REMOVE;

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final SpellLocks NONE = new SpellLocks(List.of(), List.of(), List.of(), Mode.BLOCK,
            List.of());

    public static final SpellLocks SAMPLE = new SpellLocks(List.of("wizards:frostbolt"),
            List.of("wizards:fireball"), List.of("fire"), Mode.BLOCK,
            List.of(new SpellTune("wizards:frostbolt", SpellTuning.NONE)));

    public static final Codec<SpellLocks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("grant", List.of()).forGetter(SpellLocks::grant),
            Codec.STRING.listOf().optionalFieldOf("disable", List.of()).forGetter(SpellLocks::disable),
            Codec.STRING.listOf().optionalFieldOf("disable_schools", List.of())
                    .forGetter(SpellLocks::disableSchools),
            Mode.CODEC.optionalFieldOf("disable_mode", Mode.BLOCK).forGetter(SpellLocks::disableMode),
            SpellTune.CODEC.listOf().optionalFieldOf("tune", List.of())
                    .forGetter(SpellLocks::tune)
    ).apply(instance, SpellLocks::new));

    public SpellLocks {
        grant = grant == null ? List.of() : List.copyOf(grant);
        disable = disable == null ? List.of() : List.copyOf(disable);
        disableSchools = disableSchools == null ? List.of() : List.copyOf(disableSchools);
        disableMode = disableMode == null ? Mode.BLOCK : disableMode;
        tune = tune == null ? List.of() : List.copyOf(tune);
    }

    public boolean isEmpty() {
        return grant.isEmpty() && disable.isEmpty() && disableSchools.isEmpty() && tune.isEmpty();
    }

    public boolean disables() {
        return !disable.isEmpty() || !disableSchools.isEmpty();
    }

    public SpellLocks mergedWith(SpellLocks other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return other;
        }
        return new SpellLocks(union(grant, other.grant()), union(disable, other.disable()),
                union(disableSchools, other.disableSchools()),
                disables() ? disableMode : other.disableMode(), mergedTune(other.tune()));
    }

    private List<SpellTune> mergedTune(List<SpellTune> other) {
        List<SpellTune> merged = new ArrayList<>(tune);
        merged.addAll(other);
        return merged;
    }

    private static List<String> union(List<String> first, List<String> second) {
        List<String> merged = new ArrayList<>(first);
        for (String entry : second) {
            if (!merged.contains(entry)) {
                merged.add(entry);
            }
        }
        return List.copyOf(merged);
    }
}
