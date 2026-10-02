package net.pixeldreamstudios.combat_styles.style;

import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;

public final class SequenceLimits {

    private SequenceLimits() {
    }

    public static int minimumFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()
                || !CombatStylesConfig.get().enforceMinimumAttacks()
                || ignoresMinimum(stack)) {
            return 0;
        }
        StyleSequence baseline = SequenceService.baselineOf(stack);
        int natives = baseline.isEmpty()
                ? StyleRegistry.nodesOf(SequenceService.baseAttributes(stack)).size()
                : baseline.size();
        return Math.min(natives, maximumFor(stack));
    }

    public static boolean ignoresMinimum(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && Boolean.TRUE.equals(stack.get(ModComponents.IGNORE_MINIMUM.get()));
    }

    public static void setIgnoresMinimum(ItemStack stack, boolean ignore) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (ignore) {
            stack.set(ModComponents.IGNORE_MINIMUM.get(), Boolean.TRUE);
        } else {
            stack.remove(ModComponents.IGNORE_MINIMUM.get());
        }
    }

    public static int maximumFor(ItemStack stack) {
        return DefaultStyles.slotLimitFor(stack);
    }

    public static int grantedBy(ItemStack stack, StyleEntry entry) {
        if (stack == null || stack.isEmpty() || entry == null) {
            return 0;
        }
        return Math.max(countOf(SequenceService.rawBaselineOf(stack), entry),
                nativeGrantOf(stack, entry));
    }

    private static int nativeGrantOf(ItemStack stack, StyleEntry entry) {
        if (!entry.affixIds().isEmpty() || entry.customStyleId().isPresent()) {
            return 0;
        }
        StylePage resolved = StyleRegistry.get(entry.styleId());
        String styleId = resolved == null ? entry.styleId() : resolved.styleId();
        if (styleId == null || styleId.isBlank()) {
            return 0;
        }
        int count = 0;
        for (StylePage node : StyleRegistry.nodesOf(SequenceService.baseAttributes(stack))) {
            if (styleId.equals(node.styleId())) {
                count++;
            }
        }
        return Math.max(count, baselineGrantOf(stack, styleId));
    }

    private static int baselineGrantOf(ItemStack stack, String styleId) {
        int count = 0;
        for (StyleEntry existing : SequenceService.rawBaselineOf(stack).entries()) {
            if (existing.customStyleId().isPresent() || !existing.affixIds().isEmpty()) {
                continue;
            }
            StylePage resolved = StyleRegistry.get(existing.styleId());
            String existingId = resolved == null ? existing.styleId() : resolved.styleId();
            if (styleId.equals(existingId)) {
                count++;
            }
        }
        return count;
    }

    public static int countOf(StyleSequence sequence, StyleEntry entry) {
        if (sequence == null || entry == null) {
            return 0;
        }
        String key = entry.learnedKey();
        int count = 0;
        for (StyleEntry existing : sequence.entries()) {
            if (existing.learnedKey().equals(key)) {
                count++;
            }
        }
        return count;
    }

    public static int allowedCopies(ItemStack stack, StyleEntry entry, int owned) {
        return Math.max(owned, grantedBy(stack, entry));
    }
}
