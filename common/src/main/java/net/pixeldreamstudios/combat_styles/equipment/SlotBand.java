package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum SlotBand implements StringRepresentable {

    OPENING("opening", "tooltip.combat_styles.equip_band_opening"),
    MIDDLE("middle", "tooltip.combat_styles.equip_band_middle"),
    CLOSING("closing", "tooltip.combat_styles.equip_band_closing");

    public static final Codec<SlotBand> CODEC = StringRepresentable.fromEnum(SlotBand::values);

    private final String key;
    private final String translationKey;

    SlotBand(String key, String translationKey) {
        this.key = key;
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public boolean matches(int slotIndex, int sequenceSize) {
        if (sequenceSize <= 0 || slotIndex < 0 || slotIndex >= sequenceSize) {
            return false;
        }
        int span = Math.max(1, sequenceSize / 3);
        return switch (this) {
            case OPENING -> slotIndex < span;
            case CLOSING -> slotIndex >= sequenceSize - span;
            case MIDDLE -> sequenceSize <= 2
                    ? slotIndex == sequenceSize / 2
                    : slotIndex >= span && slotIndex < sequenceSize - span;
        };
    }

    @Override
    public String getSerializedName() {
        return key;
    }
}
