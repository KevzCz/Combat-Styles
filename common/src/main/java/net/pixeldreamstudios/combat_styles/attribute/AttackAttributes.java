package net.pixeldreamstudios.combat_styles.attribute;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.PendingExtraHit;
import net.pixeldreamstudios.combat_styles.style.AttackAttribute;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AttackAttributes {

    private static final String MODIFIER_PATH = "attack_attribute";

    private static final String RATIO_PATH = "attack_ratio";
    private static final double NEUTRAL_RATIO = 1.0D;

    private record Armed(AttackOverride override, double ratio, long tick) {
    }

    public static final int NO_SLOT = -1;

    private static final Map<UUID, Map<Integer, Armed>> PENDING = new ConcurrentHashMap<>();

    private AttackAttributes() {
    }

    public static List<Applied> apply(LivingEntity entity, AttackOverride override) {
        return apply(entity, override, 0);
    }

    public static List<Applied> apply(LivingEntity entity, AttackOverride override, int scope) {
        if (entity == null || override == null || override.attributes().isEmpty()) {
            return List.of();
        }
        List<Applied> applied = new ArrayList<>(override.attributes().size());
        for (Map.Entry<String, AttackAttribute> bonus : override.attributes().entrySet()) {
            if (bonus.getValue() == null || bonus.getValue().amount() == 0.0D) {
                continue;
            }
            AttributeInstance instance = instanceOf(entity, bonus.getKey());
            if (instance == null) {
                continue;
            }
            ResourceLocation id = modifierIdFor(bonus.getKey(), scope);
            if (instance.getModifier(id) != null) {
                continue;
            }
            instance.addTransientModifier(new AttributeModifier(id,
                    bonus.getValue().amount(), bonus.getValue().operation()));
            applied.add(new Applied(instance, id));
        }
        return applied;
    }

    public static void arm(LivingEntity entity, AttackOverride override) {
        arm(entity, override, NEUTRAL_RATIO);
    }

    public static void arm(LivingEntity entity, AttackOverride override, double ratio) {
        arm(entity, override, ratio, NO_SLOT);
    }

    public static void arm(LivingEntity entity, AttackOverride override, double ratio,
                           int slot) {
        if (entity == null || override == null) {
            return;
        }
        boolean scales = Math.abs(ratio - NEUTRAL_RATIO) > 1.0E-6D;
        if (override.attributes().isEmpty() && !scales) {
            return;
        }
        PENDING.computeIfAbsent(entity.getUUID(), key -> new ConcurrentHashMap<>())
                .put(slot, new Armed(override, ratio, entity.level().getGameTime()));
    }

    public static void disarm(LivingEntity entity) {
        disarm(entity, NO_SLOT);
    }

    public static void disarm(LivingEntity entity, int slot) {
        if (entity == null) {
            return;
        }
        Map<Integer, Armed> armed = PENDING.get(entity.getUUID());
        if (armed != null) {
            armed.remove(slot);
        }
    }

    private static final Map<UUID, Integer> DEPTH = new ConcurrentHashMap<>();

    public static List<Applied> openScope(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return List.of();
        }
        Map<Integer, Armed> pending = PENDING.get(entity.getUUID());
        if (pending == null || pending.isEmpty()) {
            return List.of();
        }
        int slot = PendingExtraHit.index();
        Armed armed = pending.remove(slot);
        if (armed == null && slot != NO_SLOT) {
            armed = pending.remove(NO_SLOT);
        }
        if (armed == null) {
            return List.of();
        }
        AttackOverride override = armed.override();
        int scope = DEPTH.merge(entity.getUUID(), 1, Integer::sum);
        if (scope <= 1) {
            clearStale(entity, override, scope);
        }
        List<Applied> applied = new ArrayList<>(apply(entity, override, scope));
        Applied scaled = applyRatio(entity, armed.ratio(), scope);
        if (scaled != null) {
            applied.add(scaled);
        }
        return applied;
    }

    private static Applied applyRatio(LivingEntity entity, double ratio, int scope) {
        if (Math.abs(ratio - NEUTRAL_RATIO) <= 1.0E-6D) {
            return null;
        }
        AttributeInstance instance = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (instance == null) {
            return null;
        }
        ResourceLocation id = CombatStyles.id(RATIO_PATH + "." + scope);
        instance.removeModifier(id);
        instance.addTransientModifier(new AttributeModifier(id, ratio - NEUTRAL_RATIO,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return new Applied(instance, id);
    }

    private static void clearStale(LivingEntity entity, AttackOverride override, int scope) {
        for (String attributeId : override.attributes().keySet()) {
            AttributeInstance instance = instanceOf(entity, attributeId);
            if (instance != null) {
                instance.removeModifier(modifierIdFor(attributeId, scope));
            }
        }
    }

    public static void closeScope(LivingEntity entity, List<Applied> applied) {
        if (entity != null && entity.level().isClientSide()) {
            return;
        }
        remove(applied);
        if (entity == null) {
            return;
        }
        DEPTH.computeIfPresent(entity.getUUID(),
                (key, depth) -> depth <= 1 ? null : depth - 1);
    }

    public static void forget(UUID playerId) {
        PENDING.remove(playerId);
        DEPTH.remove(playerId);
    }

    public static void remove(List<Applied> applied) {
        for (Applied entry : applied) {
            entry.instance().removeModifier(entry.id());
        }
    }

    public static AttributeInstance instanceOf(LivingEntity entity, String attributeId) {
        ResourceLocation id = ResourceLocation.tryParse(attributeId);
        if (id == null) {
            return null;
        }
        Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE
                .getHolder(ResourceKey.create(Registries.ATTRIBUTE, id))
                .orElse(null);
        return attribute == null ? null : entity.getAttribute(attribute);
    }

    public static ResourceLocation modifierIdFor(String attributeId) {
        return modifierIdFor(attributeId, 0);
    }

    public static ResourceLocation modifierIdFor(String attributeId, int scope) {
        String path = MODIFIER_PATH + "/"
                + attributeId.replace(':', '.').replace('/', '.');
        return CombatStyles.id(scope <= 0 ? path : path + "." + scope);
    }

    public record Applied(AttributeInstance instance, ResourceLocation id) {
    }
}
