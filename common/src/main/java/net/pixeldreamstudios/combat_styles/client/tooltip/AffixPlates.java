package net.pixeldreamstudios.combat_styles.client.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import net.pixeldreamstudios.combat_styles.equipment.AffixPlateData;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class AffixPlates {

    private static final String MARKER = "combat_styles:plate/";
    private static final int MAX_KEYS = 256;

    private static final Map<Component, AffixPlateData.Plate> PLATES = new IdentityHashMap<>();
    private static final Map<String, AffixPlateData.Plate> BY_KEY = new HashMap<>();

    private AffixPlates() {
    }

    public static void beginTooltip() {
        PLATES.clear();
        if (BY_KEY.size() > MAX_KEYS) {
            BY_KEY.clear();
        }
    }

    public static void attach(MutableComponent line, Component label, int fillColor) {
        AffixPlateData.Plate plate = new AffixPlateData.Plate(label, fillColor);
        PLATES.put(line, plate);
        String key = MARKER + label.getString() + "/" + fillColor;
        BY_KEY.put(key, plate);
        line.withStyle(style -> style.withInsertion(key));
    }

    public static AffixPlateData.Plate of(Component line) {
        AffixPlateData.Plate direct = PLATES.get(line);
        return direct != null ? direct : fromStyle(line.getStyle());
    }

    public static AffixPlateData.Plate of(FormattedCharSequence text) {
        AtomicReference<AffixPlateData.Plate> found = new AtomicReference<>();
        text.accept((index, style, codePoint) -> {
            AffixPlateData.Plate plate = fromStyle(style);
            if (plate != null) {
                found.set(plate);
                return false;
            }
            return true;
        });
        return found.get();
    }

    private static AffixPlateData.Plate fromStyle(Style style) {
        if (style == null || style.getInsertion() == null) {
            return null;
        }
        return BY_KEY.get(style.getInsertion());
    }
}
