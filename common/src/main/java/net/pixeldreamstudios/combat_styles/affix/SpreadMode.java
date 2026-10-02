package net.pixeldreamstudios.combat_styles.affix;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.List;

public enum SpreadMode implements StringRepresentable {

    RANDOM("random"),
    FAN("fan"),
    RING("ring"),
    LINE("line");

    public static final List<SpreadMode> ALL = List.of(values());

    public static final Codec<SpreadMode> CODEC =
            StringRepresentable.fromEnum(SpreadMode::values);

    private final String name;

    SpreadMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public SpreadMode next() {
        return ALL.get((ordinal() + 1) % ALL.size());
    }

    public boolean scattersDirection() {
        return this == RANDOM;
    }

    public boolean spacesEvenly() {
        return this == FAN || this == RING;
    }

    public boolean offsetsOrigin() {
        return this == LINE;
    }
}
