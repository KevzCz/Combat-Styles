package net.pixeldreamstudios.combat_styles.item;

import net.minecraft.world.entity.player.Player;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class TooltipContextAccess {

    private static BooleanSupplier expanded = () -> false;
    private static BooleanSupplier detailed = () -> false;
    private static Supplier<Player> viewer = () -> null;

    private TooltipContextAccess() {
    }

    public static void bind(BooleanSupplier expandedCheck, Supplier<Player> viewerLookup) {
        if (expandedCheck != null) {
            expanded = expandedCheck;
        }
        if (viewerLookup != null) {
            viewer = viewerLookup;
        }
    }

    public static void bindDetailed(BooleanSupplier detailedCheck) {
        if (detailedCheck != null) {
            detailed = detailedCheck;
        }
    }

    public static boolean expanded() {
        return expanded.getAsBoolean();
    }

    public static boolean detailed() {
        return detailed.getAsBoolean();
    }

    public static Player viewer() {
        return viewer.get();
    }
}
