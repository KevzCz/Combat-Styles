package net.pixeldreamstudios.combat_styles.style;
import net.bettercombat.api.WeaponAttributes;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.api.event.CombatStylesEvents;
import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRuntime;
import net.pixeldreamstudios.combat_styles.equipment.BurstSequence;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.equipment.ChargeMirror;
import net.pixeldreamstudios.combat_styles.equipment.ClientPass;
import net.pixeldreamstudios.combat_styles.equipment.StyleSubstitution;
import net.pixeldreamstudios.combat_styles.equipment.SubstitutionScope;
import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

public final class SequenceService {
    private SequenceService() {
    }

    public static StyleSequence sequenceOf(ItemStack stack) {
        return sequenceOf(stack, SwingHolder.currentId());
    }

    public static StyleSequence sequenceOf(ItemStack stack, UUID holder) {
        if (stack == null || stack.isEmpty()) {
            return StyleSequence.EMPTY;
        }
        return substitutedSequence(stack, holder, baseSequenceOf(stack, holder));
    }

    private static List<StyleEntry> replaceAll(Player player,
                                               EquipmentDefinition substituting,
                                               List<StyleEntry> entries,
                                               List<StyleEntry> replacement) {
        StyleSubstitution substitution =
                substituting.substitutingEffect(player).substitution().orElse(null);
        int lead = EquipmentRuntime.gatedSlotOf(player, substituting, entries.size());
        if (substitution != null && substitution.appliesAfterSlot()) {
            lead = Math.min(entries.size(), lead + 1);
        }
        if (lead <= 0) {
            return replacement;
        }
        if (lead >= entries.size()) {
            return concat(entries, replacement);
        }
        return concat(entries.subList(0, lead), replacement);
    }

    public static int originalSlotFor(ItemStack stack, UUID holder, int slot) {
        StyleSubstitution substitution = activeSubstitutionOf(stack, holder);
        if (substitution == null || !substitution.keepsOriginalSlots()) {
            Player player = SwingHolder.current() instanceof Player holding ? holding : null;
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "slotmap — slot " + slot + " no mapping ("
                        + (substitution == null ? "no active substitution"
                                : "mode " + substitution.mode()) + ")");
            }
            return StyleSubstitution.NO_ORIGINAL_SLOT;
        }
        int baseSize = baseSequenceOf(stack, holder).entries().size();
        Player player = SwingHolder.current() instanceof Player holding ? holding : null;
        int lead = substitution.replacesWholeSequence()
                ? EquipmentRuntime.gatedSlotOf(player,
                        SubstitutionScope.candidateFor(player, stack), baseSize)
                : 0;
        if (substitution.appliesAfterSlot()) {
            lead = Math.min(baseSize, lead + 1);
        }
        int mapped = substitution.originalSlotOf(slot, baseSize, lead);
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "slotmap — slot " + slot + " -> " + mapped
                    + " (mode " + substitution.mode() + " base " + baseSize
                    + " lead " + lead
                    + " added " + substitution.styles().size() + ")");
        }
        return mapped;
    }

    private static StyleSubstitution activeSubstitutionOf(ItemStack stack, UUID holder) {
        Player player = SwingHolder.current() instanceof Player holding ? holding : null;
        if (player == null || holder == null || !holder.equals(player.getUUID())) {
            return null;
        }
        EquipmentDefinition substituting = SubstitutionScope.candidateFor(player, stack);
        return substituting == null
                ? null
                : substituting.substitutingEffect(player).substitution().orElse(null);
    }

    private static StyleSequence baseSequenceOf(ItemStack stack, UUID holder) {
        Map<UUID, StyleSequence> movesets = stack.get(ModComponents.MOVESETS.get());
        if (movesets != null && !movesets.isEmpty()) {
            StyleSequence owned = holder == null ? null : movesets.get(holder);
            return owned == null || owned.isEmpty()
                    ? baselineOf(stack)
                    : CustomStyles.resolve(owned);
        }
        StyleSequence sequence = stack.get(ModComponents.SEQUENCE.get());
        if (sequence != null && !sequence.isEmpty()) {
            return CustomStyles.resolve(sequence);
        }
        return baselineOf(stack);
    }

    public static final String SUBSTITUTED = "combat_styles:substituted";

    public static StyleSequence substituteSequenceOf(ItemStack stack) {
        Player player = SwingHolder.current() instanceof Player holder ? holder : null;
        EquipmentDefinition substituting = SubstitutionScope.activeFor(player, stack);
        if (substituting == null) {
            return StyleSequence.EMPTY;
        }

        StyleSubstitution substitution = substituting.substitutingEffect(player).substitution().orElse(null);
        List<String> styleIds = substitution != null && !substitution.isEmpty()
                ? substitution.styles()
                : substituting.substitutingEffect(player).substituteStyle().map(List::of).orElse(List.of());
        if (styleIds.isEmpty()) {
            return StyleSequence.EMPTY;
        }

        List<StyleEntry> entries = new ArrayList<>(styleIds.size());
        for (String styleId : styleIds) {
            StyleEntry entry = tuned(entryFor(styleId), styleId, substitution);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries.isEmpty()
                ? StyleSequence.EMPTY
                : new StyleSequence(SUBSTITUTED, entries);
    }

    private static StyleEntry tuned(StyleEntry entry, String styleId,
                                    StyleSubstitution substitution) {
        if (entry == null || substitution == null || substitution.tuning().isEmpty()) {
            return entry;
        }
        AttackOverride override = substitution.tuning().get(styleId);
        if (override == null) {
            return entry;
        }
        return new StyleEntry(entry.styleId(), entry.soundId(), entry.affixIds(),
                Optional.of(override), entry.name(), entry.nameKey(),
                entry.affixTuning(), entry.customStyleId());
    }

    public static StyleEntry entryFor(String styleId) {
        if (styleId == null || styleId.isBlank()) {
            return null;
        }
        CustomStyle definition = CustomStyles.get(styleId);
        boolean custom = definition != null
                && StyleRegistry.contains(definition.entry().styleId());
        if (!custom && !StyleRegistry.contains(styleId)) {
            CombatStyles.LOGGER.warn("Substitution references unknown style {}", styleId);
            return null;
        }
        if (custom) {
            return definition.asEntry();
        }
        return new StyleEntry(styleId,
                Optional.empty(), List.of(), Optional.empty(),
                Optional.empty(), Optional.empty(), Map.of(),
                Optional.empty());
    }

    public static Map<UUID, StyleSequence> movesetsOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Map.of();
        }
        Map<UUID, StyleSequence> movesets = stack.get(ModComponents.MOVESETS.get());
        return movesets == null ? Map.of() : movesets;
    }

    public static void storeMoveset(ItemStack stack, UUID holder, StyleSequence sequence) {
        if (stack == null || stack.isEmpty() || holder == null) {
            return;
        }
        StyleSequence previous = sequenceOf(stack, holder);
        Map<UUID, StyleSequence> movesets = new LinkedHashMap<>(movesetsOf(stack));
        if (sequence == null || sequence.isEmpty()) {
            movesets.remove(holder);
        } else {
            movesets.put(holder, sequence);
        }
        int limit = CombatStylesConfig.get().maxMovesetsPerItem();
        if (limit > 0 && movesets.size() > limit) {
            Iterator<UUID> stale = movesets.keySet().iterator();
            while (movesets.size() > limit && stale.hasNext()) {
                UUID candidate = stale.next();
                if (!candidate.equals(holder)) {
                    stale.remove();
                }
            }
        }
        if (movesets.isEmpty()) {
            stack.remove(ModComponents.MOVESETS.get());
        } else {
            stack.set(ModComponents.MOVESETS.get(), movesets);
        }
        StyleSequence current = sequenceOf(stack, holder);
        if (!previous.equals(current)) {
            CombatStylesEvents.SEQUENCE_CHANGED.forEach(listener ->
                    listener.onSequenceChanged(stack, previous, current));
        }
    }

    public static StyleSequence baselineOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return StyleSequence.EMPTY;
        }
        if (CarriedStyles.isCarrier(stack)) {
            return StyleSequence.EMPTY;
        }
        StyleSequence rolled = stack.get(ModComponents.ROLLED_SEQUENCE.get());
        if (rolled != null && !rolled.isEmpty()) {
            return CustomStyles.resolve(rolled);
        }
        return CustomStyles.resolve(DefaultStyles.forStack(stack));
    }

    public static StyleSequence rawBaselineOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return StyleSequence.EMPTY;
        }
        if (CarriedStyles.isCarrier(stack)) {
            return StyleSequence.EMPTY;
        }
        StyleSequence rolled = stack.get(ModComponents.ROLLED_SEQUENCE.get());
        if (rolled != null && !rolled.isEmpty()) {
            return rolled;
        }
        return DefaultStyles.forStack(stack);
    }

    public static StyleSequence rolledSequenceOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return StyleSequence.EMPTY;
        }
        StyleSequence rolled = stack.get(ModComponents.ROLLED_SEQUENCE.get());
        return rolled == null ? StyleSequence.EMPTY : rolled;
    }

    public static void storeRolled(ItemStack stack, StyleSequence sequence) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (sequence == null || sequence.isEmpty()) {
            stack.remove(ModComponents.ROLLED_SEQUENCE.get());
        } else {
            stack.set(ModComponents.ROLLED_SEQUENCE.get(), sequence);
        }
    }

    public static StyleSequence customSequenceOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return StyleSequence.EMPTY;
        }
        StyleSequence sequence = stack.get(ModComponents.SEQUENCE.get());
        return sequence == null ? StyleSequence.EMPTY : sequence;
    }

    public static boolean usesDefault(ItemStack stack) {
        return customSequenceOf(stack).isEmpty() && !baselineOf(stack).isEmpty();
    }

    public static boolean isEtched(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(stack.get(ModComponents.ETCHED.get()));
    }

    public static void setEtched(ItemStack stack, boolean etched) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (etched) {
            stack.set(ModComponents.ETCHED.get(), Boolean.TRUE);
        } else {
            stack.remove(ModComponents.ETCHED.get());
        }
    }

    public static boolean isLocked(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(stack.get(ModComponents.LOCKED.get()))
                || DefaultStyles.isLockedByDefault(stack);
    }

    public static void setLocked(ItemStack stack, boolean locked) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (locked) {
            stack.set(ModComponents.LOCKED.get(), Boolean.TRUE);
        } else {
            stack.remove(ModComponents.LOCKED.get());
        }
    }

    public static boolean hasSequence(ItemStack stack) {
        return !sequenceOf(stack).isEmpty();
    }

    public static void store(ItemStack stack, StyleSequence sequence) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        StyleSequence previous = sequenceOf(stack);
        if (sequence == null || sequence.isEmpty()) {
            stack.remove(ModComponents.SEQUENCE.get());
        } else {
            stack.set(ModComponents.SEQUENCE.get(), sequence);
        }
        StyleSequence current = sequenceOf(stack);
        if (!previous.equals(current)) {
            CombatStylesEvents.SEQUENCE_CHANGED.forEach(listener ->
                    listener.onSequenceChanged(stack, previous, current));
        }
    }

    public static void clear(ItemStack stack) {
        store(stack, StyleSequence.EMPTY);
    }

    public static Map<String, AttackOverride> attackOverridesOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Map.of();
        }
        Map<String, AttackOverride> overrides = stack.get(ModComponents.ATTACK_OVERRIDES.get());
        return overrides == null ? Map.of() : overrides;
    }

    public static void setAttackOverride(ItemStack stack, String styleId, AttackOverride override) {
        if (stack == null || stack.isEmpty() || styleId == null) {
            return;
        }
        Map<String, AttackOverride> current = new LinkedHashMap<>(attackOverridesOf(stack));
        if (override == null) {
            current.remove(styleId);
        } else {
            current.put(styleId, override);
        }
        if (current.isEmpty()) {
            stack.remove(ModComponents.ATTACK_OVERRIDES.get());
        } else {
            stack.set(ModComponents.ATTACK_OVERRIDES.get(), Map.copyOf(current));
        }
    }

    public static void clearAttackOverrides(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            stack.remove(ModComponents.ATTACK_OVERRIDES.get());
        }
    }

    public static WeaponAttributes baseAttributes(ItemStack stack) {
        WeaponAttributes attributes = WeaponRegistryAccess.rawAttributesFor(stack);
        if (hasAttacks(attributes)) {
            return attributes;
        }
        return SyntheticAttributes.forStack(stack, attributes);
    }

    public static WeaponAttributes nativeAttributes(ItemStack stack) {
        WeaponAttributes attributes = WeaponRegistryAccess.rawAttributesFor(stack);
        return hasAttacks(attributes) ? attributes : null;
    }

    private static boolean hasAttacks(WeaponAttributes attributes) {
        return attributes != null && attributes.attacks() != null && attributes.attacks().length > 0;
    }

    public static boolean isSupported(ItemStack stack) {
        return hasAttacks(baseAttributes(stack));
    }

    private static StyleSequence substitutedSequence(ItemStack stack, UUID holder,
                                                     StyleSequence base) {
        Player player = SwingHolder.current() instanceof Player current ? current : null;
        if (player == null && holder != null) {
            Player local = ClientHolder.localPlayer();
            if (local != null && holder.equals(local.getUUID())) {
                player = local;
            }
        }
        if (player == null || holder == null || !holder.equals(player.getUUID())) {
            return base;
        }
        List<StyleEntry> burst = BurstSequence.remaining(player);
        if (!burst.isEmpty()) {
            return new StyleSequence(SUBSTITUTED, burst);
        }
        EquipmentDefinition substituting = SubstitutionScope.activeFor(player, stack);
        if (substituting == null) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "substitution — none active (suppressed "
                        + SubstitutionScope.suppressed() + ")");
            }
            return base;
        }
        StyleSubstitution substitution = substituting.substitutingEffect(player).substitution().orElse(null);
        List<String> styles = substitution != null && !substitution.isEmpty()
                ? substitution.styles()
                : substituting.substitutingEffect(player).substituteStyle().map(List::of).orElse(List.of());
        if (styles.isEmpty()) {
            return base;
        }
        int from = 0;
        if (player.level().isClientSide()) {
            ClientPass.trace(player, substituting.id(), "moveset from " + from
                    + " of " + styles.size() + " (progress " + ClientPass.progress()
                    + ", charged " + ChargeMirror.charged(substituting.id()) + ")");
        }
        List<StyleEntry> replacement = new ArrayList<>(styles.size());
        for (int index = from; index < styles.size(); index++) {
            StyleEntry entry = tuned(entryFor(styles.get(index)),
                    styles.get(index), substitution);
            if (entry != null) {
                replacement.add(entry);
            }
        }
        if (replacement.isEmpty()) {
            return base;
        }
        List<StyleEntry> entries = base == null ? List.of() : base.entries();
        String mode = substitution == null
                ? StyleSubstitution.MODE_REPLACE_ALL
                : substitution.mode();
        List<StyleEntry> merged = switch (mode) {
            case StyleSubstitution.MODE_APPEND -> concat(entries, replacement);
            case StyleSubstitution.MODE_PREPEND -> concat(replacement, entries);
            case StyleSubstitution.MODE_REPLACE_SLOT ->
                    replaceSlots(entries, replacement, substitution);
            default -> replaceAll(player, substituting, entries, replacement);
        };
        return merged.isEmpty() ? base : new StyleSequence(SUBSTITUTED, merged);
    }

    private static List<StyleEntry> concat(List<StyleEntry> first, List<StyleEntry> second) {
        List<StyleEntry> merged = new ArrayList<>(first.size() + second.size());
        merged.addAll(first);
        merged.addAll(second);
        return merged;
    }

    private static List<StyleEntry> replaceSlots(List<StyleEntry> entries,
                                                 List<StyleEntry> replacement,
                                                 StyleSubstitution substitution) {
        if (entries.isEmpty()) {
            return replacement;
        }
        List<StyleEntry> result = new ArrayList<>(entries);
        int taken = 0;
        for (int index = 0; index < result.size(); index++) {
            if (!substitution.targets(index, result.size())) {
                continue;
            }
            result.set(index, replacement.get(Math.min(taken, replacement.size() - 1)));
            taken++;
        }
        return result;
    }

    public static String categoryOf(ItemStack stack) {
        WeaponAttributes attributes = baseAttributes(stack);
        if (attributes != null) {
            return attributes.category();
        }
        return stack == null || stack.isEmpty()
                ? null
                : WeaponCategories.assigned(stack).orElse(null);
    }
}
