package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.List;

public enum SlashAnchor implements StringRepresentable {

    WEAPON("weapon"),
    HITBOX("hitbox"),
    PLAYER("player"),
    TARGET("target");

    private static final String KEY_PREFIX = "anchor.combat_styles.";

    public static final List<SlashAnchor> ALL = List.of(values());

    public static final Codec<SlashAnchor> CODEC =
            StringRepresentable.fromEnum(SlashAnchor::values);

    private final String name;

    SlashAnchor(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public SlashAnchor next() {
        return ALL.get((ordinal() + 1) % ALL.size());
    }

    public boolean followsHitbox() {
        return this == HITBOX || this == TARGET;
    }

    public boolean worldAligned() {
        return this == PLAYER || this == TARGET;
    }

    public boolean tracksTarget() {
        return this == TARGET;
    }

    public String translationKey() {
        return KEY_PREFIX + name;
    }
}
