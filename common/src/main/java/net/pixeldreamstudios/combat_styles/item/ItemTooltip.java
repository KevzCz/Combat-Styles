package net.pixeldreamstudios.combat_styles.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class ItemTooltip {

    private static final String KEY_EXPAND = "tooltip.combat_styles.expand_hint";
    private static final String KEY_DETAIL_MARK = "tooltip.combat_styles.detail_mark";

    private ItemTooltip() {
    }

    public static boolean detailed() {
        return TooltipContextAccess.detailed();
    }

    public static Component mark() {
        return Component.translatable(KEY_DETAIL_MARK).withStyle(ChatFormatting.DARK_GRAY);
    }

    public static Component marked(Component line) {
        return line.copy().append(Component.literal(" "))
                .append(mark());
    }

    public static void expandable(List<Component> tooltip,
                                  Consumer<List<Component>> details,
                                  Consumer<List<Component>> collapsed) {
        if (TooltipContextAccess.expanded()) {
            details.accept(tooltip);
            return;
        }
        if (collapsed != null) {
            collapsed.accept(tooltip);
            return;
        }
        tooltip.add(Component.empty());
        tooltip.add(hint());
    }

    public static void expandable(List<Component> tooltip, Consumer<List<Component>> details) {
        expandable(tooltip, details, null);
    }

    public static Component hint() {
        return Component.translatable(KEY_EXPAND).withStyle(ChatFormatting.DARK_GRAY);
    }

    public static Component hint(ChatFormatting style) {
        return Component.translatable(KEY_EXPAND).withStyle(style);
    }
}
