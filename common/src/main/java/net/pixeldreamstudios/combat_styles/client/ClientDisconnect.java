package net.pixeldreamstudios.combat_styles.client;
import net.pixeldreamstudios.combat_styles.entity.ScriptedAbilities;

import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.equipment.BurstSequence;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.equipment.SpellLockState;
import net.pixeldreamstudios.combat_styles.loot.BookRolls;
import net.pixeldreamstudios.combat_styles.skill.SkillEquipmentStacks;
import net.pixeldreamstudios.combat_styles.skill.SkillGrants;
import net.pixeldreamstudios.combat_styles.loot.WeaponRolls;
import net.pixeldreamstudios.combat_styles.damage.CritGroups;
import net.pixeldreamstudios.combat_styles.loot.DropPools;
import net.pixeldreamstudios.combat_styles.loot.EquipmentDrops;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.CustomAttacks;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.DefaultStyles;

public final class ClientDisconnect {

    private ClientDisconnect() {
    }

    public static void clearAll() {
        ClientConfigSync.restoreLocal();
        SpellLockState.clearMirror();
        SkillGrants.clearMirror();
        SkillEquipmentStacks.clear();
        ClientStyleData.clear();
        DefaultStyles.clearSync();
        CustomStyles.clearSync();
        CustomAttacks.clearSync();
        EquipmentRegistry.clearSync();
        ClientSummons.clear();
        ClientBeams.clear();
        AffixRegistry.clearSync();
        AttackOverrides.clearSync();
        BookRolls.DEFINITIONS.clearSync();
        WeaponRolls.DEFINITIONS.clearSync();
        CritGroups.DEFINITIONS.clearSync();
        DropPools.DEFINITIONS.clearSync();
        EquipmentDrops.clearSync();
        ScriptedAbilities.DEFINITIONS.clearSync();
        ScriptedAbilities.rebuildFromSync();
        BurstSequence.clearClient();
    }
}
