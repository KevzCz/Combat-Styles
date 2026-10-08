package net.pixeldreamstudios.combat_styles.custom;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.entity.ScriptedAbility;
import net.pixeldreamstudios.combat_styles.entity.SummonCompanion;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentEffect;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentTrigger;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;
import net.pixeldreamstudios.combat_styles.equipment.TriggeredEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CustomScopes {

    private static final String NEST_SEPARATOR = ".";
    private static final String WORD_SEPARATOR = "_";

    public static final String PROJECTILE = of(AffixEffects.Projectile.class);
    public static final String ON_HIT = of(AffixEffects.OnHit.class);
    public static final String STYLE_AFFIX = of(StyleAffix.class);
    public static final String SUMMON = of(AffixEffects.Summon.class);
    public static final String SUMMON_COMPANION = of(SummonCompanion.class);
    public static final String SUMMON_PROFILE = of(SummonProfile.class);
    public static final String SCRIPTED_ABILITY = of(ScriptedAbility.class);
    public static final String ABILITY_STEP = of(ScriptedAbility.Step.class);
    public static final String ABILITY_ACTION = of(ScriptedAbility.Action.class);
    public static final String TRIGGERED_EFFECT = of(TriggeredEffect.class);
    public static final String EQUIPMENT_EFFECT = of(EquipmentEffect.class);
    public static final String TRIGGER = of(EquipmentTrigger.class);
    public static final String DAMAGE_SPEC = of(DamageSpec.class);

    private CustomScopes() {
    }

    public static String of(Class<?> type) {
        List<String> names = new ArrayList<>();
        for (Class<?> at = type; at != null; at = at.getEnclosingClass()) {
            names.addFirst(snake(at.getSimpleName()));
        }
        return String.join(NEST_SEPARATOR, names);
    }

    private static String snake(String name) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < name.length(); index++) {
            char letter = name.charAt(index);
            if (Character.isUpperCase(letter) && index > 0) {
                out.append(WORD_SEPARATOR);
            }
            out.append(Character.toLowerCase(letter));
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }
}
