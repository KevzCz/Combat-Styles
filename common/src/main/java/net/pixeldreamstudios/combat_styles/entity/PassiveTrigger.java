package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum PassiveTrigger implements StringRepresentable {

    HIT,
    HURT,
    KILL;

    public static final Codec<PassiveTrigger> CODEC = StringRepresentable.fromEnum(PassiveTrigger::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
