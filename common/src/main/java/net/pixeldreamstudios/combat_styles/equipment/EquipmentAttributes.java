package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.attribute.AttackAttributes;
import net.pixeldreamstudios.combat_styles.style.AttackAttribute;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EquipmentAttributes {
    private static final String MODIFIER_PATH = "equipment";
    private static final Map<UUID, Map<Key, Bonus>> ACTIVE = new ConcurrentHashMap<>();

    private record Key(String attributeId, AttributeModifier.Operation operation, boolean set) {
    }

    private static final String SET_SUFFIX = "/set";

    private record Bonus(AttackAttribute attribute, String ownerId) {
    }

    private EquipmentAttributes() {
    }

    public static void forget(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    public static void sync(Player player) {
        if (player == null) {
            return;
        }
        Map<Key, Bonus> desired = desiredFor(player);
        Map<Key, Bonus> current = ACTIVE.get(player.getUUID());

        reanchorSets(player, desired);
        if (desired.equals(current == null ? Map.of() : current)) {
            return;
        }

        if (current != null) {
            for (Key key : current.keySet()) {
                if (!desired.containsKey(key)) {
                    detach(player, key);
                }
            }
        }
        for (Map.Entry<Key, Bonus> entry : desired.entrySet()) {
            attach(player, entry.getKey(), entry.getValue());
        }

        if (desired.isEmpty()) {
            ACTIVE.remove(player.getUUID());
        } else {
            ACTIVE.put(player.getUUID(), desired);
        }
    }

    public static void clear(Player player) {
        if (player == null) {
            return;
        }
        Map<Key, Bonus> current = ACTIVE.remove(player.getUUID());
        if (current == null) {
            return;
        }
        for (Key key : current.keySet()) {
            detach(player, key);
        }
    }

    private static void reanchorSets(Player player, Map<Key, Bonus> desired) {
        for (Map.Entry<Key, Bonus> entry : desired.entrySet()) {
            Bonus bonus = entry.getValue();
            if (!bonus.attribute().set()) {
                continue;
            }
            AttributeInstance instance =
                    AttackAttributes.instanceOf(player, entry.getKey().attributeId());
            if (instance != null) {
                attachSet(player, entry.getKey(), bonus, instance);
            }
        }
    }

    public static Map<String, AttributeBonus> declaredFor(ItemStack stack) {
        EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
        if (definition == null) {
            return Map.of();
        }
        Map<String, AttributeBonus> found = new LinkedHashMap<>();
        for (Map.Entry<String, AttributeBonus> entry
                : definition.passiveAttributes().entrySet()) {
            AttributeBonus bonus = entry.getValue();
            if (bonus != null && bonus.amount() != 0.0D && !bonus.set()
                    && bonus.usesAccessorySlot()) {
                found.merge(entry.getKey(), bonus, (a, b) -> b.stacks()
                        ? b.withAmount(a.amount() + b.amount())
                        : a);
            }
        }
        return found;
    }

    private static boolean placed(Player player, ItemStack stack, AttributeBonus bonus,
                                  boolean inAccessorySlot) {
        EquipmentSlot slot = bonus.vanillaSlot();
        if (slot != null) {
            return player.getItemBySlot(slot) == stack;
        }
        return !AccessoryCompat.isLoaded() || inAccessorySlot;
    }

    private static void add(Map<Key, Bonus> merged, String attributeId, AttributeBonus declared,
                            String ownerId) {
        AttackAttribute bonus = declared.asAttackAttribute();
        Key key = new Key(attributeId, bonus.operation(), bonus.set());
        merged.merge(key, new Bonus(bonus, ownerId), (a, b) ->
                b.attribute().stacks()
                        ? new Bonus(new AttackAttribute(
                                a.attribute().amount() + b.attribute().amount(),
                                a.attribute().operation(), true, a.attribute().set()),
                                a.ownerId())
                        : a);
    }

    private static boolean containsStack(List<ItemStack> stacks, ItemStack stack) {
        for (ItemStack candidate : stacks) {
            if (candidate == stack) {
                return true;
            }
        }
        return false;
    }

    private static Map<Key, Bonus> desiredFor(Player player) {
        if (EquipmentRegistry.isEmpty() || EquipmentProviders.isEmpty()) {
            return Map.of();
        }
        Map<Key, Bonus> merged = new LinkedHashMap<>();
        List<ItemStack> accessories = AccessoryCompat.isLoaded()
                ? AccessoryCompat.equipped(player)
                : List.of();
        for (WornEquipment worn : WornEquipment.of(player)) {
            ItemStack stack = worn.stack();
            EquipmentDefinition definition = worn.definition();
            boolean inAccessorySlot = containsStack(accessories, stack);
            for (Map.Entry<String, AttributeBonus> entry
                    : definition.passiveAttributes().entrySet()) {
                AttributeBonus declared = entry.getValue();
                if (declared == null || declared.amount() == 0.0D
                        || !declared.set() || !declared.usesAccessorySlot()) {
                    continue;
                }
                add(merged, entry.getKey(), declared, definition.id());
            }
            for (Map.Entry<String, AttributeBonus> entry
                    : definition.conditionalAttributes(player, worn.copy()).entrySet()) {
                AttributeBonus declared = entry.getValue();
                if (declared == null || declared.amount() == 0.0D
                        || !placed(player, stack, declared, inAccessorySlot)) {
                    continue;
                }
                add(merged, entry.getKey(), declared, definition.id());
            }
        }
        return merged;
    }

    private static void attachSet(Player player, Key key, Bonus bonus,
                                  AttributeInstance instance) {
        ResourceLocation id = modifierIdFor(key);
        double without = valueWithout(instance, id);
        double target = Mth.lerp(
                EquipmentRuntime.rechargeProgress(player, bonus.ownerId()),
                without, bonus.attribute().amount());

        AttributeModifier wanted = null;
        if (Math.abs(without) < EPSILON) {
            if (Math.abs(target) >= EPSILON) {
                wanted = new AttributeModifier(id, target, AttributeModifier.Operation.ADD_VALUE);
            }
        } else {
            double factor = target / without - 1.0D;
            if (Math.abs(factor) >= EPSILON) {
                wanted = new AttributeModifier(id, factor,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            }
        }

        AttributeModifier current = instance.getModifier(id);
        if (wanted == null) {
            if (current != null) {
                instance.removeModifier(id);
            }
            return;
        }
        if (current != null && current.operation() == wanted.operation()
                && Math.abs(current.amount() - wanted.amount()) < EPSILON) {
            return;
        }
        instance.removeModifier(id);
        instance.addTransientModifier(wanted);
    }

    private static double valueWithout(AttributeInstance instance, ResourceLocation excluded) {
        double added = instance.getBaseValue();
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (!modifier.id().equals(excluded)
                    && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                added += modifier.amount();
            }
        }
        double total = added;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (!modifier.id().equals(excluded)
                    && modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                total += added * modifier.amount();
            }
        }
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (!modifier.id().equals(excluded)
                    && modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                total *= 1.0D + modifier.amount();
            }
        }
        return instance.getAttribute().value().sanitizeValue(total);
    }

    private static final double EPSILON = 1.0E-6D;

    private static void attach(Player player, Key key, Bonus bonus) {
        AttributeInstance instance = AttackAttributes.instanceOf(player, key.attributeId());
        if (instance != null && bonus.attribute().set()) {
            attachSet(player, key, bonus, instance);
            return;
        }
        if (instance == null) {
            return;
        }
        ResourceLocation id = modifierIdFor(key);
        instance.removeModifier(id);
        instance.addTransientModifier(
                new AttributeModifier(id, bonus.attribute().amount(), key.operation()));
    }

    private static void detach(Player player, Key key) {
        AttributeInstance instance = AttackAttributes.instanceOf(player, key.attributeId());
        if (instance != null) {
            instance.removeModifier(modifierIdFor(key));
        }
    }

    private static ResourceLocation modifierIdFor(Key key) {
        return CombatStyles.id(MODIFIER_PATH + "/"
                + key.attributeId().replace(':', '.').replace('/', '.')
                + "/" + key.operation().getSerializedName()
                + (key.set() ? SET_SUFFIX : ""));
    }
}
