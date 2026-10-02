package net.pixeldreamstudios.combat_styles.style;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.component.ModComponents;

import java.util.Map;
import java.util.UUID;

public final class MovesetProjection {
    private static final EquipmentSlot[] HANDS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private MovesetProjection() {
    }

    public static void project(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID holder = player.getUUID();
        for (EquipmentSlot hand : HANDS) {
            project(player.getItemBySlot(hand), holder);
        }
    }

    public static void project(ItemStack stack, UUID holder) {
        if (stack == null || stack.isEmpty() || holder == null) {
            return;
        }
        Map<UUID, StyleSequence> movesets = stack.get(ModComponents.MOVESETS.get());

        StyleSequence owned = movesets == null ? null : movesets.get(holder);
        StyleSequence shown = stack.get(ModComponents.SEQUENCE.get());
        if (owned == null || owned.isEmpty()) {
            if (shown != null && !shown.isEmpty()) {
                stack.remove(ModComponents.SEQUENCE.get());
            }
            return;
        }
        if (!owned.equals(shown)) {
            stack.set(ModComponents.SEQUENCE.get(), owned);
        }
    }
}
