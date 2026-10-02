package net.pixeldreamstudios.combat_styles.compat;

import net.minecraft.core.Holder;
import net.spell_engine.api.spell.Spell;
import net.spell_power.api.SpellSchool;

import java.util.Set;

public final class TunedImpactFilter extends Spell.Modifier.ImpactFilter {

    private final Set<String> schools;
    private final Set<Spell.Impact.Action.Type> types;

    TunedImpactFilter(Set<String> schools, Set<Spell.Impact.Action.Type> types) {
        this.schools = Set.copyOf(schools);
        this.types = Set.copyOf(types);
    }

    public boolean accepts(Spell.Impact impact, Holder<Spell> spell) {
        if (impact == null) {
            return false;
        }
        if (!types.isEmpty() && (impact.action == null || !types.contains(impact.action.type))) {
            return false;
        }
        if (schools.isEmpty()) {
            return true;
        }
        SpellSchool school = impact.school != null
                ? impact.school
                : (spell == null ? null : spell.value().school);
        return school != null && school.id != null && schools.contains(school.id.getPath());
    }
}
