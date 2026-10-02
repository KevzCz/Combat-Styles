package net.pixeldreamstudios.combat_styles.style;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;

import java.util.UUID;

public final class SwingHolder {

    private static final ThreadLocal<LivingEntity> CURRENT = new ThreadLocal<>();

    private SwingHolder() {
    }

    public static void push(LivingEntity player) {
        if (player != null) {
            CURRENT.set(player);
        }
    }

    public static void pop() {
        CURRENT.remove();
    }

    public static LivingEntity current() {
        return CURRENT.get();
    }

    public static ItemStack currentWeapon() {
        LivingEntity player = CURRENT.get();
        if (player == null) {
            return ItemStack.EMPTY;
        }
        WeaponRegistryAccess.pushBypass();
        try {
            return player.getMainHandItem();
        } finally {
            WeaponRegistryAccess.popBypass();
        }
    }

    public static UUID currentId() {
        LivingEntity player = CURRENT.get();
        return player == null ? null : player.getUUID();
    }
}
