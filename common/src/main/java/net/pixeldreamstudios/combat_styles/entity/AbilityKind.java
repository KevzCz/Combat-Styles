package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum AbilityKind implements StringRepresentable {

    ACTIVE,
    PASSIVE;

    public static final Codec<AbilityKind> CODEC = StringRepresentable.fromEnum(AbilityKind::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
