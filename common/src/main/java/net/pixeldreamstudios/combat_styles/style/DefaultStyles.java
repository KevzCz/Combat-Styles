package net.pixeldreamstudios.combat_styles.style;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class DefaultStyles {

    private static final Map<ResourceLocation, StyleSequence> REGISTERED = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Integer> REGISTERED_SLOT_LIMITS = new ConcurrentHashMap<>();

    private static volatile DefaultStyleSet datapack = DefaultStyleSet.EMPTY;
    private static volatile DefaultStyleSet config = DefaultStyleSet.EMPTY;

    private static final Map<ResourceLocation, StyleSequence> SYNCED = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Integer> SYNCED_SLOT_LIMITS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> SYNCED_APPENDING = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> SYNCED_TUNING = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> SYNCED_STICKY = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> SYNCED_LOCKED = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> SYNCED_CONFIG_ITEMS = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceLocation, List<SlotTuning>> SYNCED_SLOT_TUNINGS =
            new ConcurrentHashMap<>();

    private DefaultStyles() {
    }

    public static void register(ResourceLocation itemId, StyleSequence sequence) {
        if (itemId == null || sequence == null || sequence.isEmpty()) {
            return;
        }
        REGISTERED.put(itemId, sequence);
    }

    public static void registerSlotLimit(ResourceLocation itemId, int slots) {
        if (itemId != null && slots > 0) {
            REGISTERED_SLOT_LIMITS.put(itemId, slots);
        }
    }

    public static void acceptDatapack(DefaultStyleSet loaded) {
        datapack = loaded == null ? DefaultStyleSet.EMPTY : loaded;
    }

    public static void acceptConfig(DefaultStyleSet loaded) {
        config = loaded == null ? DefaultStyleSet.EMPTY : loaded;
    }

    public static void clear() {
        datapack = DefaultStyleSet.EMPTY;
        config = DefaultStyleSet.EMPTY;
    }

    private static DefaultStyleSet layerFor(ResourceLocation itemId) {
        return config.owns(itemId) ? config : datapack;
    }

    private static boolean flagged(ResourceLocation itemId,
                                   Function<DefaultStyleSet, Set<ResourceLocation>> flags,
                                   Set<ResourceLocation> synced) {
        return itemId != null
                && (flags.apply(layerFor(itemId)).contains(itemId) || synced.contains(itemId));
    }

    private static Set<ResourceLocation> effective(
            Function<DefaultStyleSet, Set<ResourceLocation>> flags) {
        Set<ResourceLocation> merged = new HashSet<>();
        for (ResourceLocation itemId : flags.apply(datapack)) {
            if (!config.owns(itemId)) {
                merged.add(itemId);
            }
        }
        merged.addAll(flags.apply(config));
        return merged;
    }

    public static Map<ResourceLocation, StyleSequence> snapshotSequences() {
        Map<ResourceLocation, StyleSequence> merged = new LinkedHashMap<>(REGISTERED);
        merged.putAll(datapack.sequences());
        merged.putAll(config.sequences());
        return merged;
    }

    public static Map<ResourceLocation, Integer> snapshotSlotLimits() {
        Map<ResourceLocation, Integer> merged = new LinkedHashMap<>(REGISTERED_SLOT_LIMITS);
        merged.putAll(datapack.slotLimits());
        merged.putAll(config.slotLimits());
        return merged;
    }

    public static Set<ResourceLocation> snapshotAppending() {
        return Set.copyOf(effective(DefaultStyleSet::appending));
    }

    public static Set<ResourceLocation> snapshotTuning() {
        return Set.copyOf(effective(DefaultStyleSet::tuning));
    }

    public static Set<ResourceLocation> snapshotSticky() {
        return Set.copyOf(effective(DefaultStyleSet::sticky));
    }

    public static Set<ResourceLocation> snapshotLocked() {
        return Set.copyOf(effective(DefaultStyleSet::locked));
    }

    public static Map<ResourceLocation, List<SlotTuning>> snapshotSlotTunings() {
        Map<ResourceLocation, List<SlotTuning>> merged = new LinkedHashMap<>();
        datapack.slotTunings().forEach((itemId, tunings) -> {
            if (!config.owns(itemId)) {
                merged.put(itemId, tunings);
            }
        });
        merged.putAll(config.slotTunings());
        return merged;
    }

    public static Set<ResourceLocation> snapshotConfigItems() {
        return Set.copyOf(config.targets());
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

    public static void acceptSyncTuning(Set<ResourceLocation> tuning) {
        SYNCED_TUNING.clear();
        SYNCED_TUNING.addAll(tuning);
    }

    public static void acceptSyncSticky(Set<ResourceLocation> sticky) {
        SYNCED_STICKY.clear();
        SYNCED_STICKY.addAll(sticky);
    }

    public static void acceptSyncLocked(Set<ResourceLocation> locked) {
        SYNCED_LOCKED.clear();
        SYNCED_LOCKED.addAll(locked);
    }

    public static void acceptSyncSlotTunings(Map<ResourceLocation, List<SlotTuning>> tunings) {
        SYNCED_SLOT_TUNINGS.clear();
        SYNCED_SLOT_TUNINGS.putAll(tunings);
    }

    public static void acceptSyncConfigItems(Set<ResourceLocation> items) {
        SYNCED_CONFIG_ITEMS.clear();
        SYNCED_CONFIG_ITEMS.addAll(items);
    }

    public static void clearSync() {
        SYNCED.clear();
        SYNCED_SLOT_LIMITS.clear();
        SYNCED_APPENDING.clear();
        SYNCED_STICKY.clear();
        SYNCED_TUNING.clear();
        SYNCED_LOCKED.clear();
        SYNCED_SLOT_TUNINGS.clear();
        SYNCED_CONFIG_ITEMS.clear();
    }

    private static ResourceLocation itemIdOf(ItemStack stack) {
        return stack == null || stack.isEmpty()
                ? null
                : BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    public static boolean hasConfigDefault(ItemStack stack) {
        ResourceLocation itemId = itemIdOf(stack);
        return itemId != null && (config.owns(itemId) || SYNCED_CONFIG_ITEMS.contains(itemId));
    }

    public static boolean keepsThroughRolls(ItemStack stack) {
        return flagged(itemIdOf(stack), DefaultStyleSet::sticky, SYNCED_STICKY);
    }

    public static boolean isLockedByDefault(ItemStack stack) {
        return flagged(itemIdOf(stack), DefaultStyleSet::locked, SYNCED_LOCKED);
    }

    public static List<SlotTuning> slotTuningsFor(ItemStack stack) {
        ResourceLocation itemId = itemIdOf(stack);
        if (itemId == null) {
            return List.of();
        }
        List<SlotTuning> local = layerFor(itemId).slotTunings().get(itemId);
        if (local != null) {
            return local;
        }
        return SYNCED_SLOT_TUNINGS.getOrDefault(itemId, List.of());
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

        ResourceLocation itemId = itemIdOf(stack);
        if (itemId == null) {
            return configured;
        }
        Integer fromConfig = config.slotLimits().get(itemId);
        if (fromConfig != null) {
            return fromConfig;
        }
        Integer fromData = datapack.slotLimits().get(itemId);
        if (fromData != null) {
            return fromData;
        }
        Integer fromCode = REGISTERED_SLOT_LIMITS.get(itemId);
        if (fromCode != null) {
            return fromCode;
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
        return !forItem(itemIdOf(stack)).isEmpty();
    }

    public static StyleSequence forItem(ResourceLocation itemId) {
        if (itemId == null) {
            return StyleSequence.EMPTY;
        }
        StyleSequence fromConfig = config.sequences().get(itemId);
        if (fromConfig != null) {
            return fromConfig;
        }
        StyleSequence fromPack = datapack.sequences().get(itemId);
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

    public static StyleSequence forStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return StyleSequence.EMPTY;
        }
        ResourceLocation itemId = itemIdOf(stack);
        StyleSequence defined = forItem(itemId);
        if (flagged(itemId, DefaultStyleSet::tuning, SYNCED_TUNING)) {
            return tuned(stack, defined);
        }
        boolean appends = flagged(itemId, DefaultStyleSet::appending, SYNCED_APPENDING);
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
        return Map.copyOf(snapshotSequences());
    }
}
