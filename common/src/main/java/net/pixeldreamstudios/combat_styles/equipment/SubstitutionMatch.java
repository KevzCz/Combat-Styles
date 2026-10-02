package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

import java.util.List;

public final class SubstitutionMatch {

    private SubstitutionMatch() {
    }

    public static boolean swung(Player player, EquipmentEffect effect) {
        List<String> styles = stylesOf(effect);
        if (styles.isEmpty()) {
            return true;
        }
        WeaponAttributes.Attack swung = CombatState.swungAttack(player);
        if (swung == null) {
            return false;
        }
        return namesAny(styles, swung);
    }

    public static boolean provides(EquipmentDefinition definition,
                                   WeaponAttributes.Attack swung) {
        for (TriggeredEffect triggered : definition.triggered()) {
            if (namesAny(stylesOf(triggered.effect()), swung)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> stylesOf(EquipmentEffect effect) {
        StyleSubstitution substitution = effect.substitution().orElse(null);
        if (substitution != null && !substitution.isEmpty()) {
            return substitution.styles();
        }
        return effect.substituteStyle().map(List::of).orElse(List.of());
    }

    public static boolean namesStyle(String styleId, WeaponAttributes.Attack swung) {
        return namesAny(List.of(styleId), swung);
    }

    private static boolean namesAny(List<String> styles, WeaponAttributes.Attack swung) {
        for (String styleId : styles) {
            StyleEntry entry = SequenceService.entryFor(styleId);
            StylePage page = entry == null ? null : pageOf(entry);
            if (page != null && SubstitutionScope.sameAttack(page.attack(), swung)) {
                return true;
            }
        }
        return false;
    }

    private static StylePage pageOf(StyleEntry entry) {
        StylePage page = StyleRegistry.get(entry.styleId());
        if (page == null || entry.attackOverride().isEmpty()) {
            return page;
        }
        return AttackOverrides.applyTo(page, entry.attackOverride().get());
    }
}
