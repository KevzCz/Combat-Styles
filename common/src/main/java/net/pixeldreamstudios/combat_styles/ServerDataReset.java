package net.pixeldreamstudios.combat_styles;

import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.CustomAffixConfig;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentOverrides;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.style.CustomAttackConfig;
import net.pixeldreamstudios.combat_styles.style.CustomAttacks;
import net.pixeldreamstudios.combat_styles.style.CustomStyleConfig;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.DefaultStyles;

import java.util.Map;
import java.util.Set;

public final class ServerDataReset {

    private ServerDataReset() {
    }

    public static void clear() {
        DefaultStyles.acceptDatapack(Map.of(), Map.of(), Set.of());
        DefaultStyles.acceptConfig(Map.of(), Map.of(), Set.of());
        DefaultStyles.acceptSticky(Set.of());
        DefaultStyles.acceptSlotTunings(Map.of());
        DefaultStyles.acceptTuning(Set.of());
        DefaultStyles.acceptLocked(Set.of());
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
