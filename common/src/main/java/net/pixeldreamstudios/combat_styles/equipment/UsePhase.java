package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum UsePhase implements StringRepresentable {

    ANY,
    EVERY,
    START,
    CHANNEL,
    RELEASE;

    public static final Codec<UsePhase> CODEC = Codec.STRING.xmap(
            UsePhase::byName, UsePhase::serializedName);

    public static UsePhase byName(String name) {
        if (name == null || name.isBlank()) {
            return ANY;
        }
        String wanted = name.trim().toUpperCase(Locale.ROOT);
        for (UsePhase phase : values()) {
            if (phase.name().equals(wanted)) {
                return phase;
            }
        }
        return ANY;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return serializedName();
    }

    public boolean accepts(UsePhase actual) {
        return this == ANY || this == EVERY || this == actual;
    }
}
