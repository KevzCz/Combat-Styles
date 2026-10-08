package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum SwingHands implements StringRepresentable {

    OWN_HAND,
    ANY;

    public static final String KEY = "swings";
    private static final String TOOLTIP_PREFIX = "tooltip.combat_styles.swings.";

    public static final Codec<SwingHands> CODEC = StringRepresentable.fromEnum(SwingHands::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String tooltipKey() {
        return TOOLTIP_PREFIX + getSerializedName();
    }
}
