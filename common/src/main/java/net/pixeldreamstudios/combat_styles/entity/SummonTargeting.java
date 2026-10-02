package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum SummonTargeting {

    ALL,
    SINGLE,
    RANDOM,
    ANCHORED,
    TEMPORARY,
    NEAREST;

    public static final Codec<SummonTargeting> CODEC = Codec.STRING.xmap(
            SummonTargeting::byName, SummonTargeting::key);

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SummonTargeting byName(String name) {
        if (name == null || name.isBlank()) {
            return ALL;
        }
        String bare = name.trim();
        int colon = bare.indexOf(':');
        if (colon >= 0) {
            bare = bare.substring(colon + 1);
        }
        try {
            return valueOf(bare.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ALL;
        }
    }

    public boolean accepts(ConfigurableSummon summon) {
        if (!summon.stance().canBeOrdered()) {
            return false;
        }
        return switch (this) {
            case ANCHORED -> !summon.temporary();
            case TEMPORARY -> summon.temporary();
            default -> true;
        };
    }

    public static List<String> keys() {
        List<String> found = new ArrayList<>();
        for (SummonTargeting mode : values()) {
            found.add(mode.key());
        }
        return List.copyOf(found);
    }

    public boolean commandsOne() {
        return this == SINGLE || this == RANDOM || this == NEAREST;
    }
}
