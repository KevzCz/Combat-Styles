package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.List;
import java.util.Optional;

public record ItemGate(Optional<ItemMatch> mainHand, Optional<ItemMatch> offHand,
                       Optional<ItemMatch> head, Optional<ItemMatch> chest,
                       Optional<ItemMatch> legs, Optional<ItemMatch> feet,
                       List<AccessoryMatch> accessories, Optional<ItemMatch> self) {

    public static final String MAIN_HAND = "main_hand";
    public static final String OFF_HAND = "off_hand";
    public static final String HEAD = "head";
    public static final String CHEST = "chest";
    public static final String LEGS = "legs";
    public static final String FEET = "feet";
    public static final String ACCESSORIES = "accessories";
    public static final String SELF = "self";
    public static final List<String> SLOTS = List.of(MAIN_HAND, OFF_HAND, HEAD, CHEST, LEGS, FEET);

    private static final String REASON_PREFIX = "items.";

    public record Slotted(String key, EquipmentSlot slot, ItemMatch match) {
    }

    public static final ItemGate NONE = new ItemGate(Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(),
            Optional.empty());

    public static final Codec<ItemGate> CODEC = Customs.wrap(ItemGate.class, RecordCodecBuilder.create(instance -> instance.group(
            ItemMatch.CODEC.optionalFieldOf(MAIN_HAND).forGetter(ItemGate::mainHand),
            ItemMatch.CODEC.optionalFieldOf(OFF_HAND).forGetter(ItemGate::offHand),
            ItemMatch.CODEC.optionalFieldOf(HEAD).forGetter(ItemGate::head),
            ItemMatch.CODEC.optionalFieldOf(CHEST).forGetter(ItemGate::chest),
            ItemMatch.CODEC.optionalFieldOf(LEGS).forGetter(ItemGate::legs),
            ItemMatch.CODEC.optionalFieldOf(FEET).forGetter(ItemGate::feet),
            AccessoryMatch.CODEC.listOf().optionalFieldOf(ACCESSORIES, List.of())
                    .forGetter(ItemGate::accessories),
            ItemMatch.CODEC.optionalFieldOf(SELF).forGetter(ItemGate::self)
    ).apply(instance, ItemGate::new)));

    public ItemGate {
        accessories = accessories == null ? List.of() : List.copyOf(accessories);
        self = self == null ? Optional.empty() : self;
    }

    public List<Slotted> slotted() {
        return List.of(
                new Slotted(MAIN_HAND, EquipmentSlot.MAINHAND, mainHand.orElse(ItemMatch.NONE)),
                new Slotted(OFF_HAND, EquipmentSlot.OFFHAND, offHand.orElse(ItemMatch.NONE)),
                new Slotted(HEAD, EquipmentSlot.HEAD, head.orElse(ItemMatch.NONE)),
                new Slotted(CHEST, EquipmentSlot.CHEST, chest.orElse(ItemMatch.NONE)),
                new Slotted(LEGS, EquipmentSlot.LEGS, legs.orElse(ItemMatch.NONE)),
                new Slotted(FEET, EquipmentSlot.FEET, feet.orElse(ItemMatch.NONE)));
    }

    public boolean isEmpty() {
        return slotted().stream().allMatch(slot -> slot.match().isEmpty())
                && accessories.stream().allMatch(AccessoryMatch::isEmpty)
                && self.map(ItemMatch::isEmpty).orElse(true);
    }

    public String rejects(Player player) {
        for (Slotted slot : slotted()) {
            if (!slot.match().isEmpty() && !slot.match().test(player.getItemBySlot(slot.slot()))) {
                return REASON_PREFIX + slot.key();
            }
        }
        if (self.isPresent() && !self.get().isEmpty() && !self.get().test(OwnerScope.stack())) {
            return REASON_PREFIX + SELF;
        }
        for (AccessoryMatch accessory : accessories) {
            if (!accessory.isEmpty()
                    && !accessory.match().test(accessory.stacks(player))) {
                return REASON_PREFIX + ACCESSORIES + "." + accessory.slot();
            }
        }
        return null;
    }
}
