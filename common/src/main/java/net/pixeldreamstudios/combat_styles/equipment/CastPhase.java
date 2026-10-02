package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum CastPhase implements StringRepresentable {

    CASTING,
    CAST,
    CHANNEL;

    public static final Codec<CastPhase> CODEC = Codec.STRING.xmap(
            CastPhase::byName, CastPhase::serializedName);

    public static CastPhase byName(String name) {
        if (name == null || name.isBlank()) {
            return CASTING;
        }
        String wanted = name.trim().toUpperCase(Locale.ROOT);
        for (CastPhase phase : values()) {
            if (phase.name().equals(wanted)) {
                return phase;
            }
        }
        return CASTING;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return serializedName();
    }

    public boolean event() {
        return this != CASTING;
    }
}
