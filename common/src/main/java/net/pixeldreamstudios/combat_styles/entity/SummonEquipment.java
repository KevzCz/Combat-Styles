package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public record SummonEquipment(Optional<String> mainhand, Optional<String> offhand,
                              Optional<String> head, Optional<String> chest,
                              Optional<String> legs, Optional<String> feet,
                              Optional<String> body) {

    private static final float NEVER_DROPS = 0.0F;

    public static final SummonEquipment NONE = new SummonEquipment(Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());

    public static final Codec<SummonEquipment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("mainhand").forGetter(SummonEquipment::mainhand),
            Codec.STRING.optionalFieldOf("offhand").forGetter(SummonEquipment::offhand),
            Codec.STRING.optionalFieldOf("head").forGetter(SummonEquipment::head),
            Codec.STRING.optionalFieldOf("chest").forGetter(SummonEquipment::chest),
            Codec.STRING.optionalFieldOf("legs").forGetter(SummonEquipment::legs),
            Codec.STRING.optionalFieldOf("feet").forGetter(SummonEquipment::feet),
            Codec.STRING.optionalFieldOf("body").forGetter(SummonEquipment::body)
    ).apply(instance, SummonEquipment::new));

    public boolean isEmpty() {
        return slots().isEmpty();
    }

    public Map<EquipmentSlot, String> slots() {
        Map<EquipmentSlot, String> slots = new EnumMap<>(EquipmentSlot.class);
        mainhand.ifPresent(id -> slots.put(EquipmentSlot.MAINHAND, id));
        offhand.ifPresent(id -> slots.put(EquipmentSlot.OFFHAND, id));
        head.ifPresent(id -> slots.put(EquipmentSlot.HEAD, id));
        chest.ifPresent(id -> slots.put(EquipmentSlot.CHEST, id));
        legs.ifPresent(id -> slots.put(EquipmentSlot.LEGS, id));
        feet.ifPresent(id -> slots.put(EquipmentSlot.FEET, id));
        body.ifPresent(id -> slots.put(EquipmentSlot.BODY, id));
        return slots;
    }

    public void applyTo(LivingEntity entity) {
        for (Map.Entry<EquipmentSlot, String> slot : slots().entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(slot.getValue());
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                continue;
            }
            equip(entity, slot.getKey(), new ItemStack(BuiltInRegistries.ITEM.get(id)));
        }
    }

    public static void applyStacks(LivingEntity entity, Map<EquipmentSlot, ItemStack> stacks) {
        for (Map.Entry<EquipmentSlot, ItemStack> slot : stacks.entrySet()) {
            if (!slot.getValue().isEmpty()) {
                equip(entity, slot.getKey(), slot.getValue().copy());
            }
        }
    }

    private static void equip(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        entity.setItemSlot(slot, stack);
        if (entity instanceof Mob mob) {
            mob.setDropChance(slot, NEVER_DROPS);
        }
    }

    public static Map<EquipmentSlot, ItemStack> snapshotOf(LivingEntity entity) {
        Map<EquipmentSlot, ItemStack> held = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                held.put(slot, stack.copy());
            }
        }
        return held;
    }
}
