package net.pixeldreamstudios.combat_styles.item;

import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import net.pixeldreamstudios.combat_styles.equipment.AccessoryCompat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class AddonItems {

    private static final int SINGLE = 1;
    private static final Map<String, List<RegistrySupplier<? extends Item>>> BY_NAMESPACE =
            new LinkedHashMap<>();

    private AddonItems() {
    }

    public static <T extends Item> RegistrySupplier<T> item(String namespace, String id,
                                                             Supplier<T> factory) {
        RegistrySupplier<T> registered = RegistrarManager.get(namespace).get(Registries.ITEM)
                .register(ResourceLocation.fromNamespaceAndPath(namespace, id), factory);
        BY_NAMESPACE.computeIfAbsent(namespace, key -> new ArrayList<>()).add(registered);
        return registered;
    }

    public static RegistrySupplier<EquipmentItem> equipment(String namespace, String id) {
        String definitionId = ResourceLocation.fromNamespaceAndPath(namespace, id).toString();
        RegistrySupplier<EquipmentItem> registered = item(namespace, id,
                () -> new EquipmentItem(new Item.Properties().stacksTo(SINGLE), definitionId));
        registered.listen(AccessoryCompat::registerItem);
        return registered;
    }

    public static List<RegistrySupplier<? extends Item>> of(String namespace) {
        List<RegistrySupplier<? extends Item>> found = BY_NAMESPACE.get(namespace);
        return found == null ? List.of() : List.copyOf(found);
    }
}
