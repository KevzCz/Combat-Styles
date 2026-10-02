package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum SlashMode implements StringRepresentable {
    PARTICLE,
    ATTACK,
    BOTH;

    public static final Codec<SlashMode> CODEC = StringRepresentable.fromEnum(SlashMode::values);

    public boolean spawnsParticle() {
        return this != ATTACK;
    }

    public boolean dealsDamage() {
        return this != PARTICLE;
    }

    public SlashMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
