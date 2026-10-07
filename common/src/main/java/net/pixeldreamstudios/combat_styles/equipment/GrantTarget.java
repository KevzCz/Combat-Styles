package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum GrantTarget implements StringRepresentable {
    SUMMONS,
    PETS,
    BOTH;

    public static final Codec<GrantTarget> CODEC = StringRepresentable.fromEnum(GrantTarget::values);

    public boolean accepts(boolean summon) {
        return this == BOTH || (summon ? this == SUMMONS : this == PETS);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
