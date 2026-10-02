package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.equipment.EquipmentContext;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRuntime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NativeAugments {
    private NativeAugments() {
    }

    public static WeaponAttributes apply(WeaponAttributes base, ItemStack stack) {
        if (base == null || base.attacks() == null || base.attacks().length == 0) {
            return base;
        }
        Player player = SwingHolder.current() instanceof Player holder ? holder : null;
        Map<String, AttackOverride> perItem = SequenceService.attackOverridesOf(stack);
        if (player == null && perItem.isEmpty()) {
            return base;
        }

        WeaponAttributes.Attack[] attacks = base.attacks();
        List<WeaponAttributes.Attack> composed = new ArrayList<>(attacks.length);
        boolean changed = false;

        for (int index = 0; index < attacks.length; index++) {
            WeaponAttributes.Attack attack = attacks[index];
            AttackOverride augment = player == null
                    ? null
                    : augmentFor(player, index, attacks.length, attack);
            AttackOverride fromItem = perItem.get(String.valueOf(index));
            if (augment == null && fromItem == null) {
                composed.add(attack);
                continue;
            }
            StylePage page = new StylePage("", base.category(), index, attack, null,
                    StyleRarity.COMMON);

            if (fromItem != null) {
                page = AttackOverrides.applyTo(page, fromItem);
            }
            if (augment != null) {
                page = AttackOverrides.scaleWith(page, augment);
            }
            composed.add(page.attack());
            changed = true;
        }
        if (!changed) {
            return base;
        }

        return new WeaponAttributes(
                base.attackRange(),
                base.rangeBonus(),
                base.pose(),
                base.offHandPose(),
                base.two_handed(),
                base.category(),
                composed.toArray(new WeaponAttributes.Attack[0]),
                base.trailAppearance());
    }

    private static AttackOverride augmentFor(Player player, int index, int size,
                                             WeaponAttributes.Attack attack) {
        EquipmentContext context = new EquipmentContext(player, null, attack, index, size,
                EquipmentRuntime.swingCount(player));
        AttackOverride merged = null;
        Set<String> defining = new HashSet<>();
        for (EquipmentRuntime.Match equipment : EquipmentRuntime.matching(player, context)) {
            if (equipment.effect().substituteStyle().isPresent() && !defining.add(equipment.id())) {
                continue;
            }
            AttackOverride augment = equipment.effect().augment().orElse(null);
            if (augment == null || !EquipmentRuntime.rewriteAllowed(player, equipment)) {
                continue;
            }
            merged = merged == null ? augment : EquipmentComposer.mergeAugments(merged, augment);
        }
        return merged;
    }
}
