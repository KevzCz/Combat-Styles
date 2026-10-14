package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record SpellLocks(List<String> grant, List<String> disable, List<String> disableSchools,
                         Mode disableMode, List<SpellTune> tune, Map<String, String> replace) {

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
            List.of(), Map.of());

    public static final SpellLocks SAMPLE = new SpellLocks(List.of("wizards:frostbolt"),
            List.of("wizards:fireball"), List.of("fire"), Mode.BLOCK,
            List.of(new SpellTune("wizards:frostbolt", SpellTuning.NONE)), Map.of());

    public static final Codec<SpellLocks> CODEC = Customs.wrap(SpellLocks.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("grant", List.of()).forGetter(SpellLocks::grant),
            Codec.STRING.listOf().optionalFieldOf("disable", List.of()).forGetter(SpellLocks::disable),
            Codec.STRING.listOf().optionalFieldOf("disable_schools", List.of())
                    .forGetter(SpellLocks::disableSchools),
            Mode.CODEC.optionalFieldOf("disable_mode", Mode.BLOCK).forGetter(SpellLocks::disableMode),
            SpellTune.CODEC.listOf().optionalFieldOf("tune", List.of())
                    .forGetter(SpellLocks::tune),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("replace", Map.of())
                    .forGetter(SpellLocks::replace)
    ).apply(instance, SpellLocks::new)));

    public SpellLocks {
        grant = grant == null ? List.of() : List.copyOf(grant);
        disable = disable == null ? List.of() : List.copyOf(disable);
        disableSchools = disableSchools == null ? List.of() : List.copyOf(disableSchools);
        disableMode = disableMode == null ? Mode.BLOCK : disableMode;
        tune = tune == null ? List.of() : List.copyOf(tune);
        replace = replace == null ? Map.of() : Map.copyOf(replace);
    }

    public boolean isEmpty() {
        return grant.isEmpty() && disable.isEmpty() && disableSchools.isEmpty() && tune.isEmpty()
                && replace.isEmpty();
    }

    public boolean disables() {
        return !disable.isEmpty() || !disableSchools.isEmpty() || !replace.isEmpty();
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
                disables() ? disableMode : other.disableMode(), mergedTune(other.tune()),
                mergedReplace(other.replace()));
    }

    private Map<String, String> mergedReplace(Map<String, String> other) {
        Map<String, String> merged = new LinkedHashMap<>(replace);
        other.forEach(merged::putIfAbsent);
        return merged;
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
