package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum ActiveIn implements StringRepresentable {

    ANY,
    HELD,
    WORN;

    public static final String KEY = "active_in";
    private static final String TOOLTIP_PREFIX = "tooltip.combat_styles.active_in.";

    public static final Codec<ActiveIn> CODEC = StringRepresentable.fromEnum(ActiveIn::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean accepts(boolean held) {
        return switch (this) {
            case ANY -> true;
            case HELD -> held;
            case WORN -> !held;
        };
    }

    public String tooltipKey() {
        return TOOLTIP_PREFIX + getSerializedName();
    }
}
