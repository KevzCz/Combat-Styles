package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum MergeMode implements StringRepresentable {

    REPLACE,
    ADD,
    MULTIPLY;

    public static final Codec<MergeMode> CODEC = StringRepresentable.fromEnum(MergeMode::values);

    public MergeMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public boolean stacks() {
        return this != REPLACE;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
