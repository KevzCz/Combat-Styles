package net.pixeldreamstudios.combat_styles.equipment.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.pixeldreamstudios.combat_styles.equipment.AccessoryCompat;

import java.util.List;

public final class AccessoryCompatImpl {

    private AccessoryCompatImpl() {
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(AccessoryCompat.TRINKETS_MOD_ID);
    }

    public static void registerItem(Item item) {
        if (!isLoaded()) {
            return;
        }
        TrinketsInternal.registerItem(item);
    }

    public static List<ItemStack> inSlot(Player player, String slot) {
        if (!isLoaded() || player == null) {
            return List.of();
        }
        return TrinketsInternal.inSlot(player, slot);
    }

    public static List<String> slotIds(Level level) {
        if (!isLoaded() || level == null) {
            return List.of();
        }
        return TrinketsInternal.slotIds(level);
    }

    public static List<ItemStack> equipped(Player player) {
        if (!isLoaded()) {
            return List.of();
        }
        return TrinketsInternal.equipped(player);
    }
}
