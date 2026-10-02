package net.pixeldreamstudios.combat_styles.style;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultStyles {

    private static final Map<ResourceLocation, StyleSequence> REGISTERED = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, StyleSequence> DATAPACK = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, StyleSequence> CONFIG = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Integer> SLOT_LIMITS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Integer> CONFIG_SLOT_LIMITS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> APPENDING = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceLocation, StyleSequence> SYNCED = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Integer> SYNCED_SLOT_LIMITS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> SYNCED_APPENDING = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> LOCKED_ITEMS = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> SYNCED_LOCKED = ConcurrentHashMap.newKeySet();

    private DefaultStyles() {
    }

    public static void register(ResourceLocation itemId, StyleSequence sequence) {
        if (itemId == null || sequence == null || sequence.isEmpty()) {
            return;
        }
        REGISTERED.put(itemId, sequence);
    }

    public static void acceptDatapack(Map<ResourceLocation, StyleSequence> loaded,
                                      Map<ResourceLocation, Integer> slotLimits,
                                      Set<ResourceLocation> appending) {
        DATAPACK.clear();
        DATAPACK.putAll(loaded);
        SLOT_LIMITS.clear();
        SLOT_LIMITS.putAll(slotLimits);
        APPENDING.removeIf(id -> !CONFIG.containsKey(id));
        APPENDING.addAll(appending);
    }

    public static void acceptConfig(Map<ResourceLocation, StyleSequence> loaded,
                                    Map<ResourceLocation, Integer> slotLimits,
                                    Set<ResourceLocation> appending) {
        CONFIG.clear();
        CONFIG.putAll(loaded);
        CONFIG_SLOT_LIMITS.clear();
        CONFIG_SLOT_LIMITS.putAll(slotLimits);
        APPENDING.addAll(appending);
    }

    public static Map<ResourceLocation, StyleSequence> snapshotSequences() {
        Map<ResourceLocation, StyleSequence> merged = new LinkedHashMap<>(REGISTERED);
        merged.putAll(DATAPACK);
        merged.putAll(CONFIG);
        return merged;
    }

    public static Map<ResourceLocation, Integer> snapshotSlotLimits() {
        Map<ResourceLocation, Integer> merged = new LinkedHashMap<>(SLOT_LIMITS);
        merged.putAll(CONFIG_SLOT_LIMITS);
        return merged;
    }

    private static final Set<ResourceLocation> STICKY = new HashSet<>();
    private static final Set<ResourceLocation> SYNCED_STICKY = ConcurrentHashMap.newKeySet();

    public static void acceptSticky(Set<ResourceLocation> sticky) {
        STICKY.clear();
        STICKY.addAll(sticky);
    }

    public static void acceptSyncSticky(Set<ResourceLocation> sticky) {
        SYNCED_STICKY.clear();
        SYNCED_STICKY.addAll(sticky);
    }

    public static Set<ResourceLocation> snapshotSticky() {
        return Set.copyOf(STICKY);
    }

    public static boolean keepsThroughRolls(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && (STICKY.contains(itemId) || SYNCED_STICKY.contains(itemId));
    }

    private static final Map<ResourceLocation, List<SlotTuning>> SLOT_TUNINGS = new HashMap<>();

    public static void acceptSlotTunings(Map<ResourceLocation, List<SlotTuning>> tunings) {
        SLOT_TUNINGS.clear();
        SLOT_TUNINGS.putAll(tunings);
    }

    public static List<SlotTuning> slotTuningsFor(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId == null ? List.of() : SLOT_TUNINGS.getOrDefault(itemId, List.of());
    }

    public static Set<ResourceLocation> snapshotTuning() {
        return Set.copyOf(TUNING);
    }

    public static Set<ResourceLocation> snapshotAppending() {
        return Set.copyOf(APPENDING);
    }

    private static final Set<ResourceLocation> SYNCED_TUNING = ConcurrentHashMap.newKeySet();

    public static void acceptSyncTuning(Set<ResourceLocation> tuning) {
        SYNCED_TUNING.clear();
        SYNCED_TUNING.addAll(tuning);
    }

    public static void acceptSync(Map<ResourceLocation, StyleSequence> sequences,
                                  Map<ResourceLocation, Integer> slotLimits,
                                  Set<ResourceLocation> appending) {
        SYNCED.clear();
        SYNCED.putAll(sequences);
        SYNCED_SLOT_LIMITS.clear();
        SYNCED_SLOT_LIMITS.putAll(slotLimits);
        SYNCED_APPENDING.clear();
        SYNCED_APPENDING.addAll(appending);
    }

    public static void clearSync() {
        SYNCED.clear();
        SYNCED_SLOT_LIMITS.clear();
        SYNCED_APPENDING.clear();
        SYNCED_STICKY.clear();
        SYNCED_TUNING.clear();
        SYNCED_LOCKED.clear();
    }

    public static Set<ResourceLocation> snapshotLocked() {
        return Set.copyOf(LOCKED_ITEMS);
    }

    public static void acceptSyncLocked(Set<ResourceLocation> locked) {
        SYNCED_LOCKED.clear();
        SYNCED_LOCKED.addAll(locked);
    }

    public static void acceptLocked(Set<ResourceLocation> locked) {
        LOCKED_ITEMS.clear();
        if (locked != null) {
            LOCKED_ITEMS.addAll(locked);
        }
    }

    public static boolean isLockedByDefault(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && (LOCKED_ITEMS.contains(itemId) || SYNCED_LOCKED.contains(itemId));
    }

    public static void registerSlotLimit(ResourceLocation itemId, int slots) {
        if (itemId != null && slots > 0) {
            SLOT_LIMITS.put(itemId, slots);
        }
    }

    public static int slotLimitFor(ItemStack stack) {
        int configured = CombatStylesConfig.get().maxAttacksPerSequence();
        if (stack == null || stack.isEmpty()) {
            return configured;
        }
        Integer override = stack.get(ModComponents.SLOT_LIMIT.get());
        if (override != null && override > 0) {
            return override;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return configured;
        }
        Integer fromConfig = CONFIG_SLOT_LIMITS.get(itemId);
        if (fromConfig != null) {
            return fromConfig;
        }
        Integer fromData = SLOT_LIMITS.get(itemId);
        if (fromData != null) {
            return fromData;
        }
        Integer fromSync = SYNCED_SLOT_LIMITS.get(itemId);
        return fromSync == null ? configured : fromSync;
    }

    public static boolean grantsAttacks(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (CarriedStyles.isCarrier(stack)) {
            return false;
        }
        StyleSequence rolled = stack.get(ModComponents.ROLLED_SEQUENCE.get());
        if (rolled != null && !rolled.isEmpty()) {
            return true;
        }
        return !forItem(BuiltInRegistries.ITEM.getKey(stack.getItem())).isEmpty();
    }

    public static StyleSequence forItem(ResourceLocation itemId) {
        if (itemId == null) {
            return StyleSequence.EMPTY;
        }
        StyleSequence fromConfig = CONFIG.get(itemId);
        if (fromConfig != null) {
            return fromConfig;
        }
        StyleSequence fromPack = DATAPACK.get(itemId);
        if (fromPack != null) {
            return fromPack;
        }
        StyleSequence fromCode = REGISTERED.get(itemId);
        if (fromCode != null) {
            return fromCode;
        }
        StyleSequence fromSync = SYNCED.get(itemId);
        return fromSync == null ? StyleSequence.EMPTY : fromSync;
    }

    private static final Set<ResourceLocation> TUNING = new HashSet<>();

    public static void acceptTuning(Set<ResourceLocation> tuning) {
        TUNING.clear();
        TUNING.addAll(tuning);
    }

    private static boolean tunes(ResourceLocation itemId) {
        return itemId != null && (TUNING.contains(itemId) || SYNCED_TUNING.contains(itemId));
    }

    public static StyleSequence forStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return StyleSequence.EMPTY;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        StyleSequence defined = forItem(itemId);
        if (tunes(itemId)) {
            return tuned(stack, defined);
        }
        boolean appends = itemId != null
                && (APPENDING.contains(itemId) || SYNCED_APPENDING.contains(itemId));
        if (!defined.isEmpty() && !appends) {
            return defined;
        }

        List<StyleEntry> combined = new ArrayList<>();
        for (StylePage native_ : StyleRegistry.nodesOf(
                WeaponRegistryAccess.rawAttributesFor(stack))) {
            combined.add(StyleEntry.of(native_.styleId()));
        }
        for (StyleEntry entry : defined.entries()) {
            if (combined.size() >= slotLimitFor(stack)) {
                break;
            }
            combined.add(entry);
        }
        return combined.isEmpty()
                ? defined
                : new StyleSequence(defined.name(), combined);
    }

    private static StyleSequence tuned(ItemStack stack, StyleSequence defined) {
        List<StylePage> natives = StyleRegistry.nodesOf(
                WeaponRegistryAccess.rawAttributesFor(stack));
        if (natives.isEmpty()) {
            return defined;
        }
        List<StyleEntry> overlays = defined.entries();
        List<StyleEntry> result = new ArrayList<>(natives.size());
        for (int index = 0; index < natives.size(); index++) {
            StyleEntry base = StyleEntry.of(natives.get(index).styleId());
            if (index >= overlays.size()) {
                result.add(base);
                continue;
            }
            StyleEntry overlay = overlays.get(index);
            StyleEntry merged = base;
            if (!overlay.affixIds().isEmpty()) {
                merged = merged.withAffixes(overlay.affixIds());
            }
            if (overlay.attackOverride().isPresent()) {
                merged = merged.withAttackOverride(overlay.attackOverride().get());
            }
            if (!overlay.affixTuning().isEmpty()) {
                merged = merged.withAffixTuning(overlay.affixTuning());
            }
            result.add(merged);
        }

        List<SlotTuning> slotTunings = slotTuningsFor(stack);
        if (!slotTunings.isEmpty()) {
            for (int index = 0; index < result.size(); index++) {
                for (SlotTuning tuning : slotTunings) {
                    if (tuning.matches(index, result.size())) {
                        result.set(index, tuning.applyTo(result.get(index)));
                    }
                }
            }
        }
        return new StyleSequence(defined.name(), result);
    }

    public static Set<String> nativeStyleIdsOf(ItemStack stack) {
        Set<String> ids = new LinkedHashSet<>();
        for (StyleEntry entry : forStack(stack).entries()) {
            ids.add(entry.styleId());
            StylePage page = StyleRegistry.get(entry.styleId());
            if (page != null) {
                ids.add(page.styleId());
            }
        }
        return ids;
    }

    public static Map<ResourceLocation, StyleSequence> all() {
        Map<ResourceLocation, StyleSequence> merged = new LinkedHashMap<>(REGISTERED);
        merged.putAll(DATAPACK);
        merged.putAll(CONFIG);
        return Map.copyOf(merged);
    }
}
