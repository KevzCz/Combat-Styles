package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

import java.util.Arrays;
import java.util.List;

public final class SequenceSlots {

    private static final ThreadLocal<Integer> PREVIEW_SLOT =
            ThreadLocal.withInitial(() -> ClientSwing.NO_SLOT);

    private SequenceSlots() {
    }

    public static void beginPreview(int slot) {
        PREVIEW_SLOT.set(slot);
    }

    public static void endPreview() {
        PREVIEW_SLOT.remove();
    }

    public static int slotFor(int comboCount, int size) {
        return ComboCycle.slotFor(comboCount, size);
    }

    public static int sequenceComboOf(Player player, int comboCount) {
        int combo = comboCount;
        if (player != null && PlayerAttackHelper.isDualWielding(player)) {
            boolean offHand = swungWithOffHand(player, combo);
            combo = (offHand && combo > 0 ? combo - 1 : combo) / 2;
        }
        int anchor = SubstitutionAnchor.of(player, combo);
        if (anchor != SubstitutionAnchor.NONE) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "anchor — combo " + comboCount
                        + " dual " + combo + " minus anchor " + anchor
                        + " = " + (combo - anchor));
            }
        }
        return combo - anchor;
    }

    public static int indexOfAttack(List<StyleEntry> entries,
                                    WeaponAttributes.Attack attack) {
        if (entries == null || attack == null) {
            return ClientSwing.NO_SLOT;
        }
        for (int index = 0; index < entries.size(); index++) {
            StylePage page = pageOf(entries.get(index));
            if (page != null && SubstitutionScope.sameAttack(page.attack(), attack)) {
                return index;
            }
        }
        return ClientSwing.NO_SLOT;
    }

    public static int slotOf(List<StyleEntry> entries, WeaponAttributes.Attack attack, int hint) {
        if (entries != null && attack != null && hint >= 0 && hint < entries.size()) {
            StylePage page = pageOf(entries.get(hint));
            if (page != null && SubstitutionScope.sameAttack(page.attack(), attack)) {
                return hint;
            }
        }
        return indexOfAttack(entries, attack);
    }

    public static int swungSlot(Player player) {
        if (player == null) {
            return ClientSwing.NO_SLOT;
        }
        int previewed = PREVIEW_SLOT.get();
        if (previewed != ClientSwing.NO_SLOT) {
            return previewed;
        }
        return player.level().isClientSide()
                ? ChargeMirror.swungSlot()
                : CombatState.swungSlot(player);
    }

    private static StylePage pageOf(StyleEntry entry) {
        if (entry == null) {
            return null;
        }
        StylePage page = StyleRegistry.get(entry.styleId());
        if (page == null || entry.attackOverride().isEmpty()) {
            return page;
        }
        return AttackOverrides.applyTo(page, entry.attackOverride().get());
    }

    public static int liveSlot(Player player, ItemStack weapon, int comboCount, int size) {
        int combo = sequenceComboOf(player, comboCount);
        return liveSlotOf(liveSlots(player, weapon, comboCount, size), combo, size);
    }

    public static int liveSlotOf(int[] live, int sequenceCombo, int size) {
        return ComboCycle.slotOf(live, sequenceCombo, size);
    }

    public static int[] liveSlots(Player player, ItemStack weapon, int comboCount, int size) {
        return liveSlots(player, weapon, size, swungWithOffHand(player, comboCount));
    }

    public static int[] liveSlotsFor(Player player, ItemStack weapon, int size) {
        boolean offHand = player != null && weapon != null
                && weapon == player.getOffhandItem()
                && PlayerAttackHelper.isDualWielding(player);
        return liveSlots(player, weapon, size, offHand);
    }

    public static int[] liveSlots(Player player, ItemStack weapon, int size, boolean offHand) {
        int[] all = ComboCycle.all(size);
        WeaponAttributes.Attack[] attacks = composedAttacks(weapon);
        if (player == null || attacks == null || attacks.length != size) {
            return all;
        }
        int[] live = new int[size];
        int count = 0;
        for (int index = 0; index < size; index++) {
            WeaponAttributes.Attack attack = attacks[index];
            if (attack == null || AttackConditions.passes(attack.conditions(), player, offHand)) {
                live[count++] = index;
            }
        }
        return count == 0 ? all : Arrays.copyOf(live, count);
    }

    public static int positionIn(int[] live, int slot) {
        return ComboCycle.positionOf(live, slot);
    }

    private static WeaponAttributes.Attack[] composedAttacks(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty() || WeaponRegistryAccess.bypassSequence()) {
            return null;
        }
        WeaponAttributes attributes = WeaponRegistry.getAttributes(weapon);
        return attributes == null ? null : attributes.attacks();
    }

    public static boolean swungWithOffHand(Player player, int comboCount) {
        if (player == null || !PlayerAttackHelper.isDualWielding(player)) {
            return false;
        }
        if (!player.level().isClientSide()) {
            AttackHand swung = CombatState.swungHand(player);
            if (swung != null) {
                return swung.isOffHand();
            }
        }
        return PlayerAttackHelper.shouldAttackWithOffHand(player, comboCount);
    }
}
