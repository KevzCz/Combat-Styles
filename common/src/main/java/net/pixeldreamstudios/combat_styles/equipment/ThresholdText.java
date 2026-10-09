package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;

public final class ThresholdText {

    private static final String KEY_AT_LEAST = "tooltip.combat_styles.threshold_at_least";
    private static final String KEY_AT_MOST = "tooltip.combat_styles.threshold_at_most";
    private static final String KEY_BETWEEN = "tooltip.combat_styles.threshold_between";

    private ThresholdText() {
    }

    public static List<Component> parts(StateGate state) {
        List<Component> parts = new ArrayList<>();
        if (state == null) {
            return parts;
        }
        for (Threshold threshold : state.thresholds()) {
            Component source = StyleText.scalingSourceName(threshold.source());
            if (threshold.atLeast().isPresent() && threshold.atMost().isPresent()) {
                parts.add(Component.translatable(KEY_BETWEEN, source,
                        StyleText.formatDecimal(threshold.atLeast().get()),
                        StyleText.formatDecimal(threshold.atMost().get())));
            } else if (threshold.atLeast().isPresent()) {
                parts.add(Component.translatable(KEY_AT_LEAST, source,
                        StyleText.formatDecimal(threshold.atLeast().get())));
            } else if (threshold.atMost().isPresent()) {
                parts.add(Component.translatable(KEY_AT_MOST, source,
                        StyleText.formatDecimal(threshold.atMost().get())));
            }
        }
        return parts;
    }
}
