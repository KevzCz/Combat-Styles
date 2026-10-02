package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.component.ModComponents;

import net.pixeldreamstudios.combat_styles.api.EquipmentInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EquipmentProviders {

    private static final List<EquipmentInfo> PROVIDERS = new CopyOnWriteArrayList<>();

    private record Resolved(long registry, long tick, EquipmentDefinition definition) {
    }

    private static final Map<ItemStack, Resolved> RESOLVED =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static volatile long tick;

    private EquipmentProviders() {
    }

    public static void register(EquipmentInfo provider) {
        if (provider != null && !PROVIDERS.contains(provider)) {
            PROVIDERS.add(provider);
            tick++;
        }
    }

    public static void advance() {
        tick++;
    }

    public static EquipmentDefinition definitionFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        EquipmentDefinition own = stack.get(ModComponents.STACK_EQUIPMENT.get());
        if (own != null) {
            return own;
        }
        long registry = EquipmentRegistry.generation();
        long now = tick;
        Resolved cached = RESOLVED.get(stack);
        if (cached != null && cached.registry() == registry && cached.tick() == now) {
            return cached.definition();
        }
        EquipmentDefinition resolved = resolve(stack);
        RESOLVED.put(stack, new Resolved(registry, now, resolved));
        return resolved;
    }

    private static EquipmentDefinition resolve(ItemStack stack) {
        for (EquipmentInfo provider : PROVIDERS) {
            EquipmentDefinition definition = provider.definitionFor(stack);
            if (definition != null) {
                return definition;
            }
        }
        return null;
    }

    public static boolean isEmpty() {
        return PROVIDERS.isEmpty();
    }
}
