package net.pixeldreamstudios.combat_styles.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.util.ArrayList;
import java.util.List;

final class SpellSchoolLookup {

    private static final String NAMESPACE_SEPARATOR = ":";

    private SpellSchoolLookup() {
    }

    static SpellSchool find(String raw, String defaultNamespace) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String id = raw.contains(NAMESPACE_SEPARATOR) ? raw : defaultNamespace + raw;
        for (SpellSchool school : SpellSchools.all()) {
            if (school != null && school.id != null && school.id.toString().equals(id)) {
                return school;
            }
        }
        return null;
    }

    static ResourceLocation damageTypeOf(String raw, String defaultNamespace) {
        SpellSchool school = find(raw, defaultNamespace);
        return school == null || school.damageType == null ? null : school.damageType.location();
    }

    static Double powerOf(Player player, String raw, String defaultNamespace) {
        SpellSchool school = find(raw, defaultNamespace);
        return school == null ? null : SpellPower.getSpellPower(school, player).baseValue();
    }

    static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (SpellSchool school : SpellSchools.all()) {
            if (school != null && school.id != null) {
                ids.add(school.id.toString());
            }
        }
        ids.sort(String::compareTo);
        return List.copyOf(ids);
    }
}
