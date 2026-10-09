package net.pixeldreamstudios.combat_styles.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EquipmentDrops {

    public record Drop(ItemStack stack, boolean roll) {
    }

    private static final Map<String, List<String>> TAGGED = new LinkedHashMap<>();
    private static final Map<String, List<String>> SYNCED_TAGGED = new LinkedHashMap<>();
    private static final Set<ItemStack> AUTHORED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final double NEVER = 0.0D;

    private EquipmentDrops() {
    }

    public static void accept(Map<String, List<ResourceLocation>> drops) {
        TAGGED.clear();
        if (drops != null) {
            drops.forEach((rarity, ids) -> TAGGED.put(rarity,
                    ids.stream().map(ResourceLocation::toString).toList()));
        }
    }

    public static Map<String, List<String>> taggedForSync() {
        return Map.copyOf(TAGGED);
    }

    public static void acceptSync(Map<String, List<String>> synced) {
        SYNCED_TAGGED.clear();
        if (synced != null) {
            SYNCED_TAGGED.putAll(synced);
        }
    }

    public static void clearSync() {
        SYNCED_TAGGED.clear();
    }

    public static void forget() {
        TAGGED.clear();
    }

    public static List<String> tagged(String rarity) {
        List<String> found = TAGGED.isEmpty() ? SYNCED_TAGGED.get(rarity) : TAGGED.get(rarity);
        return found == null ? List.of() : found;
    }

    public static boolean isAuthored(ItemStack stack) {
        return AUTHORED.contains(stack);
    }

    public static void deliverAuthored(ItemStack stack, Runnable delivery) {
        AUTHORED.add(stack);
        try {
            delivery.run();
        } finally {
            AUTHORED.remove(stack);
        }
    }

    public static List<Drop> roll(ResourceLocation table, Entity entity,
                                  ServerLevel level, Vec3 origin, RandomSource random) {
        if (random == null || level == null) {
            return List.of();
        }
        String tableId = table == null ? "" : table.toString();
        Set<String> here = structuresAt(level, entity, origin);
        List<Drop> dropped = new ArrayList<>();
        for (StyleRarity rarity : StyleRarity.values()) {
            DropPool pool = DropPools.effective(rarity);
            if (pool.chance() <= NEVER || pool.entries().isEmpty()
                    || !pool.matches(tableId, entity, here) || random.nextDouble() >= pool.chance()) {
                continue;
            }
            DropEntry entry = pick(pool.entries(), tableId, random);
            if (entry == null) {
                continue;
            }
            ItemStack stack = entry.create(level.registryAccess());
            if (stack.isEmpty()) {
                continue;
            }
            if (BookProvider.isBook(stack.getItem()) && !entry.isInstance()) {
                BookRolls.fill(stack, rarity, random);
            }
            dropped.add(new Drop(stack, entry.roll()));
        }
        return dropped;
    }

    private static DropEntry pick(List<DropEntry> entries, String tableId, RandomSource random) {
        int total = 0;
        for (DropEntry entry : entries) {
            total += entry.weightFor(tableId);
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (DropEntry entry : entries) {
            roll -= entry.weightFor(tableId);
            if (roll < 0) {
                return entry;
            }
        }
        return null;
    }

    private static Set<String> structuresAt(ServerLevel level, Entity entity, Vec3 origin) {
        Vec3 where = origin != null ? origin : (entity == null ? null : entity.position());
        if (where == null) {
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
                            found.add(DropPool.TAG_MARKER + tag.location().toString().toLowerCase(Locale.ROOT))));
        }
        return found;
    }
}
