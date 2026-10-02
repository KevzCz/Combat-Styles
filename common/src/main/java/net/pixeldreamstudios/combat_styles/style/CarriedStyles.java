package net.pixeldreamstudios.combat_styles.style;

import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.component.ModComponents;

public final class CarriedStyles {

    private CarriedStyles() {
    }

    public static StyleSequence of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return StyleSequence.EMPTY;
        }
        StyleSequence carried = stack.get(ModComponents.CARRIED_STYLES.get());
        return carried == null ? StyleSequence.EMPTY : carried;
    }

    public static void store(ItemStack stack, StyleSequence sequence) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (sequence == null || sequence.isEmpty()) {
            stack.remove(ModComponents.CARRIED_STYLES.get());
        } else {
            stack.set(ModComponents.CARRIED_STYLES.get(), sequence);
        }
    }

    public static boolean has(ItemStack stack) {
        return !of(stack).isEmpty();
    }

    public static boolean isCarrier(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof StyleCarrier;
    }
}
