package net.pixeldreamstudios.combat_styles.compat.editor;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.style.CustomStyle;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.WeaponCategories;
import net.pixeldreamstudios.skill_tree_editor.api.SkillTreeEditorApi;

import java.util.Set;
import java.util.TreeSet;

public final class EditorLists {

    private static final String STYLES = "styles";
    private static final String EQUIPMENT = "equipment";
    private static final String AFFIXES = "affixes";
    private static final String WEAPON_CATEGORIES = "weapon_categories";

    private EditorLists() {
    }

    public static void register() {
        SkillTreeEditorApi.registerIdList(CombatStyles.id(STYLES), EditorLists::styles);
        SkillTreeEditorApi.registerIdList(CombatStyles.id(EQUIPMENT),
                () -> EquipmentRegistry.all().stream().map(EquipmentDefinition::id).toList());
        SkillTreeEditorApi.registerIdList(CombatStyles.id(AFFIXES),
                () -> AffixRegistry.all().stream().map(StyleAffix::serializedId).toList());
        SkillTreeEditorApi.registerIdList(CombatStyles.id(WEAPON_CATEGORIES), WeaponCategories::allNames);
    }

    private static Set<String> styles() {
        Set<String> ids = new TreeSet<>();
        StyleRegistry.distinct().stream().map(StylePage::styleId).forEach(ids::add);
        CustomStyles.all().stream().map(CustomStyle::id).forEach(ids::add);
        return ids;
    }
}
