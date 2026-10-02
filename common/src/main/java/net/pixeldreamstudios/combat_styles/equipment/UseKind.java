package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum UseKind implements StringRepresentable {

    PRESS,
    CHARGE,
    CHANNEL;

    public static final Codec<UseKind> CODEC = Codec.STRING.xmap(
            UseKind::byName, UseKind::serializedName);

    public static UseKind byName(String name) {
        if (name == null || name.isBlank()) {
            return PRESS;
        }
        String wanted = name.trim().toUpperCase(Locale.ROOT);
        for (UseKind kind : values()) {
            if (kind.name().equals(wanted)) {
                return kind;
            }
        }
        return PRESS;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return serializedName();
    }

    public boolean holds() {
        return this != PRESS;
    }

    public boolean repeats() {
        return this == CHANNEL;
    }

    public boolean completesOnRelease() {
        return this == CHARGE;
    }

    public UsePhase completion() {
        return switch (this) {
            case PRESS -> UsePhase.START;
            case CHARGE -> UsePhase.RELEASE;
            case CHANNEL -> UsePhase.CHANNEL;
        };
    }

    public boolean emits(UsePhase phase) {
        if (phase == UsePhase.ANY) {
            return true;
        }
        return switch (this) {
            case PRESS -> phase == UsePhase.START;
            case CHARGE -> phase == UsePhase.START || phase == UsePhase.RELEASE;
            case CHANNEL -> phase == UsePhase.START || phase == UsePhase.CHANNEL
                    || phase == UsePhase.RELEASE;
        };
    }
}
