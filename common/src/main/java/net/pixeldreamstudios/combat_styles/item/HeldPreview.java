package net.pixeldreamstudios.combat_styles.item;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class HeldPreview {

    private static final ThreadLocal<ItemStack> SHOWN = new ThreadLocal<>();

    private HeldPreview() {
    }

    public static <T> T showing(ItemStack stack, Supplier<T> body) {
        ItemStack previous = SHOWN.get();
        SHOWN.set(stack);
        try {
            return body.get();
        } finally {
            if (previous == null) {
                SHOWN.remove();
            } else {
                SHOWN.set(previous);
            }
        }
    }

    public static double valueOf(Player player, Holder<Attribute> attribute) {
        AttributeInstance live = player.getAttribute(attribute);
        if (live == null) {
            return 0.0D;
        }
        ItemStack shown = SHOWN.get();
        ItemStack held = player.getMainHandItem();
        if (shown == null || shown.isEmpty() || shown == held) {
            return live.getValue();
        }
        List<AttributeModifier> shownModifiers = mainHandModifiers(shown, attribute);
        if (shownModifiers.isEmpty()) {
            return live.getValue();
        }
        AttributeInstance asHeld = new AttributeInstance(attribute, changed -> {
        });
        asHeld.replaceFrom(live);
        for (AttributeModifier modifier : mainHandModifiers(held, attribute)) {
            asHeld.removeModifier(modifier.id());
        }
        for (AttributeModifier modifier : shownModifiers) {
            asHeld.removeModifier(modifier.id());
            asHeld.addTransientModifier(modifier);
        }
        return asHeld.getValue();
    }

    private static List<AttributeModifier> mainHandModifiers(ItemStack stack, Holder<Attribute> attribute) {
        List<AttributeModifier> found = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return found;
        }
        stack.forEachModifier(EquipmentSlot.MAINHAND, (owner, modifier) -> {
            if (owner.equals(attribute)) {
                found.add(modifier);
            }
        });
        return found;
    }
}
