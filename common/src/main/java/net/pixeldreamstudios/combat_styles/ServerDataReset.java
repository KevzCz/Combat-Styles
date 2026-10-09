package net.pixeldreamstudios.combat_styles;
import net.pixeldreamstudios.combat_styles.entity.AbilityHosts;
import net.pixeldreamstudios.combat_styles.entity.ScriptedAbilities;

import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.CustomAffixConfig;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentOverrides;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.style.CustomAttackConfig;
import net.pixeldreamstudios.combat_styles.style.CustomAttacks;
import net.pixeldreamstudios.combat_styles.style.CustomStyleConfig;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.loot.BookRolls;
import net.pixeldreamstudios.combat_styles.loot.WeaponRolls;
import net.pixeldreamstudios.combat_styles.damage.CritGroups;
import net.pixeldreamstudios.combat_styles.loot.DropPools;
import net.pixeldreamstudios.combat_styles.style.DefaultStyles;

import java.util.Map;

public final class ServerDataReset {

    private ServerDataReset() {
    }

    public static void clear() {
        DefaultStyles.clear();
        BookRolls.DEFINITIONS.forget();
        WeaponRolls.DEFINITIONS.forget();
        CritGroups.DEFINITIONS.forget();
        DropPools.DEFINITIONS.forget();
        ScriptedAbilities.DEFINITIONS.forget();
        AbilityHosts.clear();
        CustomStyles.accept(Map.of());
        CustomStyleConfig.forgetAll();
        CustomAttacks.accept(Map.of());
        CustomAttackConfig.forgetAll();
        CustomAffixConfig.forgetAll();
        AffixRegistry.accept(Map.of());
        AffixRegistry.acceptCustom(Map.of());
        EquipmentOverrides.forgetAll();
        EquipmentRegistry.accept(Map.of());
        EquipmentRegistry.acceptData(Map.of());
    }
}
