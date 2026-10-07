package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum SummonCastTarget implements StringRepresentable {

    AUTO,
    HIT,
    SELF,
    OWNER,
    AIM;

    public static final Codec<SummonCastTarget> CODEC =
            StringRepresentable.fromEnum(SummonCastTarget::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
