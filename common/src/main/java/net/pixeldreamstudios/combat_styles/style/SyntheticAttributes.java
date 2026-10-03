package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.component.ModComponents;

import java.util.ArrayList;
import java.util.List;

public final class SyntheticAttributes {

    public static final String CATEGORY = "improvised";
    private static final double ATTACK_RANGE = 3.0D;
    private static final double RANGE_BONUS = 0.0D;

    private SyntheticAttributes() {
    }

    public static boolean isImprovised(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && Boolean.TRUE.equals(stack.get(ModComponents.IMPROVISED.get()));
    }

    public static void setImprovised(ItemStack stack, boolean improvised) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (improvised) {
            stack.set(ModComponents.IMPROVISED.get(), Boolean.TRUE);
        } else {
            stack.remove(ModComponents.IMPROVISED.get());
        }
    }

    public static WeaponAttributes forStack(ItemStack stack, WeaponAttributes existing) {
        if (stack == null || stack.isEmpty()) {
            return existing;
        }
        StyleSequence sequence = SequenceService.rawBaselineOf(stack);
        if (sequence.isEmpty()) {
            sequence = stack.get(ModComponents.SEQUENCE.get());
        }
        if (sequence == null || sequence.isEmpty()) {
            return existing;
        }
        if (!isImprovised(stack) && !DefaultStyles.grantsAttacks(stack)) {
            return existing;
        }
        List<StylePage> pages = sequence.resolve();
        List<WeaponAttributes.Attack> attacks = new ArrayList<>(pages.size());
        for (StylePage page : pages) {
            if (page != null && page.attack() != null) {
                attacks.add(page.attack());
            }
        }
        if (attacks.isEmpty()) {
            return existing;
        }
        return new WeaponAttributes(ATTACK_RANGE, RANGE_BONUS, null, null, Boolean.FALSE,
                WeaponCategories.categoryFor(stack, CATEGORY),
                attacks.toArray(new WeaponAttributes.Attack[0]), null);
    }
}
