package net.pixeldreamstudios.combat_styles.client;

import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.equipment.BurstSequence;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.equipment.SpellLockState;
import net.pixeldreamstudios.combat_styles.loot.BookRolls;
import net.pixeldreamstudios.combat_styles.loot.WeaponRolls;
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
        ClientStyleData.clear();
        DefaultStyles.clearSync();
        CustomStyles.clearSync();
        CustomAttacks.clearSync();
        EquipmentRegistry.clearSync();
        ClientSummons.clear();
        AffixRegistry.clearSync();
        AttackOverrides.clearSync();
        BookRolls.DEFINITIONS.clearSync();
        WeaponRolls.DEFINITIONS.clearSync();
        BurstSequence.clearClient();
    }
}
