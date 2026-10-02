package net.pixeldreamstudios.combat_styles.client.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.style.SequenceService;

public final class SequenceMark {

    private static final String GEM = " ◈";

    private SequenceMark() {
    }

    public static boolean marks(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !SequenceService.isSupported(stack)) {
            return false;
        }
        if (!SequenceService.attackOverridesOf(stack).isEmpty()) {
            return true;
        }
        if (!SequenceService.rolledSequenceOf(stack).isEmpty()) {
            return true;
        }
        return SequenceService.hasSequence(stack) && !SequenceService.usesDefault(stack);
    }

    public static Component decorate(Component name, ItemStack stack) {
        if (!marks(stack)) {
            return name;
        }
        MutableComponent decorated = name.copy();
        return decorated.append(Component.literal(GEM)
                .withStyle(ChatFormatting.AQUA));
    }
}
