package net.pixeldreamstudios.combat_styles.equipment.neoforge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import net.pixeldreamstudios.combat_styles.equipment.AccessoryCompat;

import java.util.List;

public final class AccessoryCompatImpl {

    private AccessoryCompatImpl() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(AccessoryCompat.CURIOS_MOD_ID);
    }

    public static void registerItem(Item item) {
        if (!isLoaded()) {
            return;
        }
        CuriosInternal.registerItem(item);
    }

    public static List<ItemStack> inSlot(Player player, String slot) {
        if (!isLoaded() || player == null) {
            return List.of();
        }
        return CuriosInternal.inSlot(player, slot);
    }

    public static List<String> slotIds(Level level) {
        if (!isLoaded() || level == null) {
            return List.of();
        }
        return CuriosInternal.slotIds(level);
    }

    public static List<ItemStack> equipped(Player player) {
        if (!isLoaded()) {
            return List.of();
        }
        return CuriosInternal.equipped(player);
    }
}
