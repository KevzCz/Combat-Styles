package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

public final class OwnerScope {

    private static final ThreadLocal<ItemStack> CURRENT = new ThreadLocal<>();

    private OwnerScope() {
    }

    public static ItemStack stack() {
        ItemStack stack = CURRENT.get();
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public static boolean test(ItemStack owner, BooleanSupplier check) {
        ItemStack previous = CURRENT.get();
        CURRENT.set(owner);
        try {
            return check.getAsBoolean();
        } finally {
            restore(previous);
        }
    }

    public static void run(ItemStack owner, Runnable action) {
        ItemStack previous = CURRENT.get();
        CURRENT.set(owner);
        try {
            action.run();
        } finally {
            restore(previous);
        }
    }

    private static void restore(ItemStack previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
