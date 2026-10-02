package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Locale;

public enum InheritMode implements StringRepresentable {

    OWN,
    PARENT,
    HIGHEST,
    LOWEST;

    private static final String KEY_PREFIX = "inherit.combat_styles.";

    public static final List<InheritMode> ALL = List.of(values());

    public static final Codec<InheritMode> CODEC =
            StringRepresentable.fromEnum(InheritMode::values);

    public InheritMode next() {
        return ALL.get((ordinal() + 1) % ALL.size());
    }

    public boolean inherits() {
        return this != OWN;
    }

    public boolean compares() {
        return this == HIGHEST || this == LOWEST;
    }

    public String translationKey() {
        return KEY_PREFIX + getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
