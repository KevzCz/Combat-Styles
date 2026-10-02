package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;

public final class SpellMatch {

    private SpellMatch() {
    }

    public static boolean names(List<String> spells, ResourceLocation spell) {
        if (spells.isEmpty()) {
            return true;
        }
        if (spell == null) {
            return false;
        }
        String full = spell.toString();
        for (String entry : spells) {
            if (entry.equals(full) || entry.equals(spell.getPath())) {
                return true;
            }
        }
        return false;
    }

    public static boolean inSchool(List<String> schools, String school) {
        if (schools.isEmpty()) {
            return true;
        }
        return school != null && schools.stream()
                .anyMatch(wanted -> SpellLockState.schoolKey(wanted).equals(school));
    }

    public static boolean ofType(List<String> castTypes, String castType) {
        if (castTypes.isEmpty()) {
            return true;
        }
        String actual = castType == null ? "" : castType.toLowerCase(Locale.ROOT);
        return castTypes.stream()
                .anyMatch(wanted -> wanted.trim().toLowerCase(Locale.ROOT).equals(actual));
    }
}
