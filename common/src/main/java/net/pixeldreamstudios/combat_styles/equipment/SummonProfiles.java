package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.effect.EffectLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SummonProfiles {

    private record Cached(long tick, SummonProfile.Resolved resolved) {
    }

    private static final Map<UUID, Cached> RESOLVED = new ConcurrentHashMap<>();

    private SummonProfiles() {
    }

    public static void forget(UUID playerId) {
        RESOLVED.remove(playerId);
    }

    private static List<TriggeredEffect> liveEffects(Player player, EquipmentDefinition definition) {
        List<TriggeredEffect> live = new ArrayList<>();
        for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
            if (player == null
                    || EquipmentRuntime.liveAtRest(player, definition, named.getKey(), named.getValue())) {
                live.add(named.getValue());
            }
        }
        return live;
    }

    public static SummonProfile profileOf(Player player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
        if (definition == null) {
            return null;
        }
        for (TriggeredEffect entry : liveEffects(player, definition)) {
            SummonProfile profile = entry.effect().summonProfile().orElse(null);
            if (profile != null) {
                return profile;
            }
        }
        return null;
    }

    public static SummonProfile heldProfileOf(Player player) {
        if (player == null) {
            return null;
        }
        SummonProfile main = profileOf(player, player.getMainHandItem());
        return main != null ? main : profileOf(player, player.getOffhandItem());
    }

    public static SummonProfile wornProfileOf(Player player) {
        if (player == null) {
            return null;
        }
        for (ItemStack worn : WornItems.of(player)) {
            SummonProfile profile = profileOf(player, worn);
            if (profile != null) {
                return profile;
            }
        }
        return null;
    }

    public static SummonProfile profileFor(Player player) {
        SummonProfile held = heldProfileOf(player);
        return held != null ? held : wornProfileOf(player);
    }

    public static SummonProfile replacingProfileOf(Player player) {
        SummonProfile held = heldProfileOf(player);
        if (held != null && held.replacesSummons()) {
            return held;
        }
        SummonProfile worn = wornProfileOf(player);
        return worn != null && worn.replacesSummons() ? worn : null;
    }

    public static SummonReplacement replacementFor(Player player, String summonedId) {
        if (player == null || summonedId == null) {
            return replacementFor(player);
        }
        SummonReplacement blanket = null;
        for (ItemStack worn : WornItems.of(player)) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(worn);
            if (definition == null) {
                continue;
            }
            for (TriggeredEffect entry : liveEffects(player, definition)) {
                SummonReplacement replacement =
                        entry.effect().summonReplacement().orElse(null);
                if (replacement == null || !replacement.replaces(summonedId)) {
                    continue;
                }
                if (!replacement.replacedSummonedEntities().isEmpty()) {
                    return replacement;
                }
                if (blanket == null) {
                    blanket = replacement;
                }
            }
        }
        return blanket;
    }

    public static SummonReplacement replacementFor(Player player) {
        if (player == null) {
            return null;
        }
        for (ItemStack worn : WornItems.of(player)) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(worn);
            if (definition == null) {
                continue;
            }
            for (TriggeredEffect entry : liveEffects(player, definition)) {
                SummonReplacement replacement =
                        entry.effect().summonReplacement().orElse(null);
                if (replacement != null) {
                    return replacement;
                }
            }
        }
        return null;
    }
    public static boolean replacesSummons(Player player) {
        return replacementFor(player) != null;
    }

    public static final ProfileCondition.ConditionContext CONTEXT =
            new ProfileCondition.ConditionContext() {
                @Override
                public boolean hasEffect(Player player, String effectId) {
                    Holder<MobEffect> holder = EffectLookup.of(effectId);
                    return holder != null && player.hasEffect(holder);
                }

                @Override
                public boolean holding(Player player, String itemId) {
                    return SummonProfiles.holding(player, itemId);
                }

                @Override
                public boolean wearing(Player player, String itemId) {
                    return SummonProfiles.wearing(player, itemId);
                }
            };

    public static SummonProfile.Resolved resolvedFor(Player player) {
        if (player == null) {
            return null;
        }
        long now = player.level().getGameTime();
        Cached cached = RESOLVED.get(player.getUUID());
        if (cached != null && cached.tick() == now) {
            return cached.resolved();
        }
        SummonProfile profile = profileFor(player);
        SummonProfile.Resolved fresh = profile == null
                ? null
                : profile.resolve(player, CONTEXT);
        RESOLVED.put(player.getUUID(), new Cached(now, fresh));
        return fresh;
    }

    public static boolean holding(Player player, String itemId) {
        return matches(player.getMainHandItem(), itemId)
                || matches(player.getOffhandItem(), itemId);
    }

    public static boolean wearing(Player player, String itemId) {
        for (ItemStack worn : WornItems.of(player)) {
            if (matches(worn, itemId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(ItemStack stack, String itemId) {
        if (stack == null || stack.isEmpty() || itemId == null || itemId.isBlank()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.toString().equals(itemId);
    }
}
