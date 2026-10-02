package net.pixeldreamstudios.combat_styles.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.config.EquipmentDropConfig;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EquipmentDrops {

    private static final Map<String, List<ResourceLocation>> BY_RARITY = new LinkedHashMap<>();

    private EquipmentDrops() {
    }

    public static void accept(Map<String, List<ResourceLocation>> drops) {
        BY_RARITY.clear();
        if (drops != null) {
            BY_RARITY.putAll(drops);
        }
    }

    public static boolean isEmpty() {
        return BY_RARITY.isEmpty();
    }

    public static List<ItemStack> roll(ResourceLocation table, Entity entity,
                                       ServerLevel level, Vec3 origin, RandomSource random) {
        if (random == null || BY_RARITY.isEmpty()) {
            return List.of();
        }
        String tableId = table == null ? "" : table.toString();
        Set<String> here = structuresAt(level, entity, origin);
        List<ItemStack> dropped = new ArrayList<>();
        for (String rarity : EquipmentDropConfig.matching(table, entity, here)) {
            EquipmentDropConfig.Rarity entry = EquipmentDropConfig.rarity(rarity);
            if (entry == null || !BY_RARITY.containsKey(rarity)
                    || random.nextDouble() >= entry.chance) {
                continue;
            }
            ItemStack stack = rollOne(rarity, tableId, random);
            if (!stack.isEmpty()) {
                dropped.add(stack);
            }
        }
        return dropped;
    }

    private static Set<String> structuresAt(ServerLevel level, Entity entity, Vec3 origin) {
        Vec3 where = origin != null ? origin : (entity == null ? null : entity.position());
        if (level == null || where == null) {
            return Set.of();
        }
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Set<String> found = new HashSet<>();
        for (Structure structure : level.structureManager()
                .getAllStructuresAt(BlockPos.containing(where)).keySet()) {
            ResourceLocation id = registry.getKey(structure);
            if (id == null) {
                continue;
            }
            found.add(id.toString().toLowerCase(Locale.ROOT));
            registry.getResourceKey(structure)
                    .flatMap(registry::getHolder)
                    .ifPresent(holder -> holder.tags().forEach(tag ->
                            found.add("#" + tag.location().toString().toLowerCase(Locale.ROOT))));
        }
        return found;
    }

    private static ItemStack rollOne(String rarity, String tableId, RandomSource random) {
        List<ResourceLocation> candidates = BY_RARITY.get(rarity);
        if (candidates == null || candidates.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation picked = pickItem(rarity, tableId, candidates, random);
        if (picked == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(picked).orElse(null);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        if (BookProvider.isBook(item)) {
            BookRolls.fill(stack, StyleRarity.byKey(rarity), random);
        }
        return stack;
    }

    private static ResourceLocation pickItem(String rarity, String tableId,
                                             List<ResourceLocation> candidates,
                                             RandomSource random) {
        int total = 0;
        for (ResourceLocation candidate : candidates) {
            total += EquipmentDropConfig.itemWeight(rarity, candidate, tableId);
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (ResourceLocation candidate : candidates) {
            roll -= EquipmentDropConfig.itemWeight(rarity, candidate, tableId);
            if (roll < 0) {
                return candidate;
            }
        }
        return null;
    }

    public static List<ResourceLocation> poolOf(String rarity) {
        List<ResourceLocation> pool = BY_RARITY.get(rarity);
        return pool == null ? List.of() : new ArrayList<>(pool);
    }
}
