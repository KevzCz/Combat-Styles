package net.pixeldreamstudios.combat_styles.equipment;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class AccessoryCompat {

    public static final String TRINKETS_MOD_ID = "trinkets";
    public static final String CURIOS_MOD_ID = "curios";
    public static final String SLOT = "ring";

    private record Snapshot(int tick, List<ItemStack> stacks) {
    }

    private static final Map<Player, Snapshot> EQUIPPED =
            Collections.synchronizedMap(new WeakHashMap<>());

    private AccessoryCompat() {
    }

    public static List<ItemStack> equippedThisTick(Player player) {
        if (player == null) {
            return List.of();
        }
        Snapshot cached = EQUIPPED.get(player);
        if (cached != null && cached.tick() == player.tickCount) {
            return cached.stacks();
        }
        List<ItemStack> stacks = List.copyOf(equipped(player));
        EQUIPPED.put(player, new Snapshot(player.tickCount, stacks));
        return stacks;
    }

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerItem(Item item) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<ItemStack> equipped(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<ItemStack> inSlot(Player player, String slot) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<String> slotIds(Level level) {
        throw new AssertionError();
    }

}
