package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class WornItems {

    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private record Cached(int tick, List<ItemStack> worn) {
    }

    private record Key(UUID player, boolean client) {
    }

    private static final Map<Key, Cached> CACHE = new ConcurrentHashMap<>();

    private WornItems() {
    }

    public static void forget(UUID playerId) {
        CACHE.keySet().removeIf(key -> key.player().equals(playerId));
    }

    public static List<ItemStack> of(LivingEntity player) {
        if (player == null) {
            return List.of();
        }
        Key key = new Key(player.getUUID(), player.level().isClientSide());
        Cached cached = CACHE.get(key);
        if (cached != null && cached.tick() == player.tickCount) {
            return cached.worn();
        }
        List<ItemStack> resolved = resolve(player);
        CACHE.put(key, new Cached(player.tickCount, resolved));
        return resolved;
    }

    private static List<ItemStack> resolve(LivingEntity player) {
        if (player == null) {
            return List.of();
        }
        List<ItemStack> worn = new ArrayList<>();
        WeaponRegistryAccess.pushBypass();
        try {
            for (EquipmentSlot slot : ARMOR) {
                addIfActive(worn, player.getItemBySlot(slot), false);
            }
            addIfActive(worn, player.getMainHandItem(), true);
            addIfActive(worn, player.getOffhandItem(), true);
            if (AccessoryCompat.isLoaded() && player instanceof Player holder) {
                for (ItemStack accessory : AccessoryCompat.equipped(holder)) {
                    addIfActive(worn, accessory, false);
                }
            }
        } finally {
            WeaponRegistryAccess.popBypass();
        }
        return worn;
    }

    private static void addIfActive(List<ItemStack> worn, ItemStack stack, boolean held) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
        if (definition == null || definition.activeIn().accepts(held)) {
            worn.add(stack);
        }
    }

    public static boolean includes(LivingEntity player, ItemStack stack) {
        for (ItemStack worn : of(player)) {
            if (worn == stack) {
                return true;
            }
        }
        return false;
    }
}
