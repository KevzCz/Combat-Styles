package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.item.ItemStack;

public record UseEvent(ItemStack weapon, UseKind kind, int heldTicks, UsePhase phase) {

    public UseEvent {
        weapon = weapon == null ? ItemStack.EMPTY : weapon;
        kind = kind == null ? UseKind.PRESS : kind;
        phase = phase == null ? UsePhase.START : phase;
    }

    public UsePhase completion() {
        return kind.completion();
    }
}
