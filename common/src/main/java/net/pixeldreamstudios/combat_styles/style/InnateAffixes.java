package net.pixeldreamstudios.combat_styles.style;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InnateAffixes {

    public record Slot(Optional<Integer> slot, String affix) {

        private static final Codec<Slot> FULL = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("slot").forGetter(Slot::slot),
                Codec.STRING.fieldOf("affix").forGetter(Slot::affix)
        ).apply(instance, Slot::new));

        public static final Codec<Slot> CODEC = Codec.either(Codec.STRING, FULL)
                .xmap(either -> either.map(id -> new Slot(Optional.empty(), id), slot -> slot),
                        slot -> slot.slot().isPresent()
                                ? Either.right(slot)
                                : Either.left(slot.affix()));
    }

    private static final Map<String, List<String>> FROM_EQUIPMENT = new LinkedHashMap<>();

    private InnateAffixes() {
    }

    public static void acceptEquipment(Map<String, List<Slot>> grants) {
        FROM_EQUIPMENT.clear();
        if (grants == null) {
            return;
        }
        for (Map.Entry<String, List<Slot>> grant : grants.entrySet()) {
            if (grant.getValue() == null || grant.getValue().isEmpty()) {
                continue;
            }
            FROM_EQUIPMENT.put(grant.getKey(), resolve(grant.getValue()));
        }
    }

    private static List<String> resolve(List<Slot> slots) {
        List<String> resolved = new ArrayList<>();
        int next = 0;
        for (Slot slot : slots) {
            int index = slot.slot().orElse(next);
            while (resolved.size() <= index) {
                resolved.add(null);
            }
            resolved.set(index, slot.affix());
            next = index + 1;
        }
        return List.copyOf(resolved);
    }

    public static boolean isEmpty() {
        return FROM_EQUIPMENT.isEmpty();
    }

    public static String affixFor(ItemStack stack, int slot) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        List<String> slots = slotsFor(stack);
        if (slots == null || slot < 0 || slot >= slots.size()) {
            return null;
        }
        String affix = slots.get(slot);
        return affix == null || affix.isBlank() ? null : affix;
    }

    public static int slotCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        List<String> slots = slotsFor(stack);
        return slots == null ? 0 : slots.size();
    }

    private static List<String> slotsFor(ItemStack stack) {
        String id = itemId(stack);
        if (id == null) {
            return null;
        }
        return FROM_EQUIPMENT.get(id);
    }

    private static String itemId(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? null : id.toString();
    }
}
