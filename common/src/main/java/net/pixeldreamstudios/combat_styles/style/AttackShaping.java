package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.affix.AffixDamageDealer;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.equipment.SequenceSlots;
import net.pixeldreamstudios.combat_styles.equipment.SubstitutionScope;

import java.util.ArrayList;
import java.util.List;

public final class AttackShaping {

    private AttackShaping() {
    }

    public static AttackBehaviour behaviourFor(Player player, WeaponAttributes.Attack attack) {
        if (player == null || attack == null) {
            return AttackBehaviour.NONE;
        }
        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) {
            return AttackBehaviour.NONE;
        }
        StyleSequence sequence = SequenceService.sequenceOf(weapon, player.getUUID());
        List<StyleEntry> entries = sequence.entries();
        if (entries.isEmpty()) {
            return behaviourOf(SequenceService.attackOverridesOf(weapon)
                    .get(String.valueOf(nativeSlotOf(weapon, attack))));
        }
        int index = SequenceSlots.slotOf(entries, attack, SequenceSlots.swungSlot(player));
        if (index < 0) {
            return AttackBehaviour.NONE;
        }
        StyleEntry entry = entries.get(index);
        StylePage page = StyleRegistry.get(entry.styleId());
        if (page == null) {
            return AttackBehaviour.NONE;
        }
        AttackOverride direct = entry.attackOverride().orElse(null);
        if (direct != null) {
            page = AttackOverrides.applyTo(page, direct);
        }
        return withAffixes(player, withDeclared(behaviourOf(mergedFor(weapon, entry, index, page)),
                entry), entry, page);
    }

    private static AttackBehaviour withAffixes(Player player, AttackBehaviour behaviour,
                                               StyleEntry entry, StylePage page) {
        List<StyleAffix> affixes = AffixDamageDealer
                .affixesForCurrentAttack(player, player.getMainHandItem())
                .restrictedTo(page).affixes();
        if (affixes.isEmpty()) {
            affixes = entry.resolveAffixes().restrictedTo(page).affixes();
        }
        if (affixes.isEmpty()) {
            return behaviour;
        }
        AttackBehaviour merged = behaviour;
        for (StyleAffix affix : affixes) {
            AttackBehaviour granted = affix.behaviour().orElse(null);
            if (granted != null && !granted.isEmpty()) {
                merged = merged.mergedWith(granted, MergeMode.ADD);
            }
        }
        return merged;
    }

    private static int nativeSlotOf(ItemStack weapon, WeaponAttributes.Attack attack) {
        WeaponAttributes attributes = SequenceService.nativeAttributes(weapon);
        if (attributes == null || attributes.attacks() == null) {
            return 0;
        }
        WeaponAttributes.Attack[] attacks = attributes.attacks();
        for (int index = 0; index < attacks.length; index++) {
            if (SubstitutionScope.sameAttack(attacks[index], attack)) {
                return index;
            }
        }
        return 0;
    }

    public static AttackOverride overrideFor(Player player, WeaponAttributes.Attack attack) {
        if (player == null || attack == null) {
            return null;
        }
        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) {
            return null;
        }
        List<StyleEntry> entries = SequenceService.sequenceOf(weapon, player.getUUID()).entries();
        int index = SequenceSlots.slotOf(entries, attack, SequenceSlots.swungSlot(player));
        if (index < 0) {
            return null;
        }
        StyleEntry entry = entries.get(index);
        StylePage page = StyleRegistry.get(entry.styleId());
        return page == null ? null : mergedFor(weapon, entry, index, page);
    }

    private static AttackOverride mergedFor(ItemStack weapon, StyleEntry entry, int index,
                                            StylePage page) {
        AttackOverride direct = entry.attackOverride().orElse(null);
        AttackOverride fromItem = SequenceService.attackOverridesOf(weapon)
                .get(String.valueOf(index));
        AttackOverride merged = direct == null
                ? fromItem
                : (fromItem == null ? direct : direct.overriddenBy(fromItem));
        AttackOverride fromStyle = AttackOverrides.get(page.styleId());
        if (fromStyle == null) {
            return merged;
        }
        return merged == null ? fromStyle : fromStyle.overriddenBy(merged);
    }

    private static AttackBehaviour behaviourOf(AttackOverride override) {
        return override == null ? AttackBehaviour.NONE : override.behaviourOrDefault();
    }

    private static AttackBehaviour withDeclared(AttackBehaviour behaviour, StyleEntry entry) {
        CustomAttack attack = declaredAttackFor(entry);
        if (attack == null || attack.attack().slashes().isEmpty()) {
            return behaviour;
        }
        List<SlashPlacement> combined = new ArrayList<>(attack.attack().slashes());
        combined.addAll(behaviour.slashes());
        return behaviour.withSlashes(combined);
    }

    private static CustomAttack declaredAttackFor(StyleEntry entry) {
        CustomAttack direct = CustomAttacks.get(entry.styleId());
        if (direct != null) {
            return direct;
        }
        if (entry.customStyleId().isEmpty()) {
            return null;
        }
        CustomStyle style = CustomStyles.get(entry.customStyleId().get());
        return style == null ? null : CustomAttacks.get(style.entry().styleId());
    }
}
