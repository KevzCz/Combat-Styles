package net.pixeldreamstudios.combat_styles.affix;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.List;

public enum SpawnAnchor implements StringRepresentable {

    PLAYER("player"),
    EYES("eyes"),
    FEET("feet"),
    TARGET("target"),
    MAIN_HAND("main_hand"),
    OFF_HAND("off_hand");

    public static final List<SpawnAnchor> ALL = List.of(values());

    public static final Codec<SpawnAnchor> CODEC =
            StringRepresentable.fromEnum(SpawnAnchor::values);

    private final String name;

    SpawnAnchor(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public SpawnAnchor next() {
        return ALL.get((ordinal() + 1) % ALL.size());
    }

    public boolean usesTarget() {
        return this == TARGET;
    }

    public boolean isHand() {
        return this == MAIN_HAND || this == OFF_HAND;
    }
}
