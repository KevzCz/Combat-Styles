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

    public record Sourced(EquipmentDefinition definition, int copy, String effectKey,
                          SummonProfile profile) {

        public String key() {
            return EffectKeys.ownerOf(definition.id(), copy) + KEY_SEPARATOR + effectKey;
        }
    }

    private record Cached(long tick, SummonProfile.Resolved resolved) {
    }

    private static final String KEY_SEPARATOR = "|";

    private static final Map<UUID, Cached> RESOLVED = new ConcurrentHashMap<>();
    private static final Map<String, Cached> RESOLVED_BY_KEY = new ConcurrentHashMap<>();
    private static final Map<String, SummonProfile> INLINE = new ConcurrentHashMap<>();
    private static final String INLINE_PREFIX = "inline:";

    private SummonProfiles() {
    }

    public static void forget(UUID playerId) {
        RESOLVED.remove(playerId);
        String prefix = playerId + KEY_SEPARATOR;
        RESOLVED_BY_KEY.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private static List<Map.Entry<String, TriggeredEffect>> liveEffects(Player player,
                                                                        EquipmentDefinition definition,
                                                                        int copy) {
        List<Map.Entry<String, TriggeredEffect>> live = new ArrayList<>();
        for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
            if (player == null
                    || EquipmentRuntime.liveAtRest(player, definition, copy, named.getKey(),
                            named.getValue())) {
                live.add(named);
            }
        }
        return live;
    }

    public static List<Sourced> sourcedOf(Player player, ItemStack stack) {
        List<Sourced> found = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return found;
        }
        EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
        if (definition == null) {
            return found;
        }
        return sourcedOf(player, definition, player == null ? 0
                : WornEquipment.copyOf(player, stack));
    }

    private static List<Sourced> sourcedOf(Player player, EquipmentDefinition definition,
                                           int copy) {
        List<Sourced> found = new ArrayList<>();
        for (Map.Entry<String, TriggeredEffect> entry : liveEffects(player, definition, copy)) {
            entry.getValue().effect().summonProfile().ifPresent(profile ->
                    found.add(new Sourced(definition, copy, entry.getKey(), profile)));
        }
        return found;
    }

    public static List<AbilityGrant> liveGrants(Player player) {
        List<AbilityGrant> found = new ArrayList<>();
        if (player == null) {
            return found;
        }
        for (WornEquipment worn : WornEquipment.of(player)) {
            for (Map.Entry<String, TriggeredEffect> entry : liveEffects(player, worn.definition(), worn.copy())) {
                entry.getValue().effect().grantAbilities().ifPresent(found::add);
            }
        }
        return found;
    }

    public static List<Sourced> liveProfiles(Player player) {
        List<Sourced> found = new ArrayList<>();
        if (player == null) {
            return found;
        }
        for (WornEquipment worn : WornEquipment.of(player)) {
            found.addAll(sourcedOf(player, worn.definition(), worn.copy()));
        }
        return found;
    }

    public static SummonProfile profileOf(Player player, ItemStack stack) {
        List<Sourced> found = sourcedOf(player, stack);
        return found.isEmpty() ? null : found.getFirst().profile();
    }

    public static SummonProfile heldProfileOf(Player player) {
        if (player == null) {
            return null;
        }
        SummonProfile main = activeProfileOf(player, player.getMainHandItem());
        return main != null ? main : activeProfileOf(player, player.getOffhandItem());
    }

    private static SummonProfile activeProfileOf(Player player, ItemStack stack) {
        return WornItems.includes(player, stack) ? profileOf(player, stack) : null;
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

    public static SummonProfile profileFor(Player player, String entityId) {
        for (Sourced sourced : liveProfiles(player)) {
            if (sourced.profile().entity().map(id -> id.equals(entityId)).orElse(false)) {
                return sourced.profile();
            }
        }
        return null;
    }

    public static SummonProfile replacingProfileOf(Player player) {
        for (Sourced sourced : liveProfiles(player)) {
            if (sourced.profile().replacesSummons()) {
                return sourced.profile();
            }
        }
        return null;
    }

    public static SummonProfile replacementFor(Player player, String summonedId) {
        SummonProfile blanket = null;
        for (Sourced sourced : liveProfiles(player)) {
            SummonProfile profile = sourced.profile();
            if (summonedId == null || !profile.replaces(summonedId)) {
                continue;
            }
            if (!profile.replacedSummonedEntities().isEmpty()) {
                return profile;
            }
            if (blanket == null) {
                blanket = profile;
            }
        }
        return blanket;
    }

    public static boolean replacesSummons(Player player) {
        return replacingProfileOf(player) != null;
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

    public static String keyOf(Player player, SummonProfile profile) {
        if (player == null || profile == null) {
            return null;
        }
        for (Sourced sourced : liveProfiles(player)) {
            if (sourced.profile() == profile) {
                return sourced.key();
            }
        }
        String inline = INLINE_PREFIX + Integer.toHexString(profile.hashCode());
        INLINE.put(inline, profile);
        return inline;
    }

    public static SummonProfile.Resolved resolvedFor(Player player, String key, String entityId) {
        if (player == null) {
            return null;
        }
        String cacheKey = player.getUUID() + KEY_SEPARATOR + key + KEY_SEPARATOR + entityId;
        long now = player.level().getGameTime();
        Cached cached = RESOLVED_BY_KEY.get(cacheKey);
        if (cached != null && cached.tick() == now) {
            return cached.resolved();
        }
        SummonProfile profile = key == null ? null : INLINE.get(key);
        if (profile == null && key != null) {
            for (Sourced sourced : liveProfiles(player)) {
                if (sourced.key().equals(key)) {
                    profile = sourced.profile();
                    break;
                }
            }
        }
        if (profile == null && entityId != null) {
            profile = profileFor(player, entityId);
        }
        if (profile == null) {
            profile = replacingProfileOf(player);
        }
        SummonProfile.Resolved fresh = profile == null ? null : profile.resolve(player, CONTEXT);
        RESOLVED_BY_KEY.put(cacheKey, new Cached(now, fresh));
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
