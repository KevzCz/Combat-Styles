package net.pixeldreamstudios.combat_styles.style;
import net.pixeldreamstudios.combat_styles.entity.ScriptedAbilities;

import net.pixeldreamstudios.combat_styles.affix.CustomAffixConfig;
import net.pixeldreamstudios.combat_styles.affix.TrailPlacements;
import net.pixeldreamstudios.combat_styles.config.ConfigMigration;
import net.pixeldreamstudios.combat_styles.config.EquipmentDropConfig;
import net.pixeldreamstudios.combat_styles.loot.BookRolls;
import net.pixeldreamstudios.combat_styles.loot.WeaponRolls;

public final class StyleConfigs {

    private StyleConfigs() {
    }

    public static int reloadAll() {
        ConfigMigration.run();
        TrailPlacements.reload();
        CustomAttackConfig.reload();
        CustomStyleConfig.reload();
        CustomAffixConfig.reload();
        EquipmentDropConfig.load();
        BookRolls.DEFINITIONS.reloadConfig();
        WeaponRolls.DEFINITIONS.reloadConfig();
        ScriptedAbilities.DEFINITIONS.reloadConfig();
        int overrides = AttackOverrides.reload();
        StyleRegistry.rebuild();
        DefaultStyleConfig.reload();
        return overrides;
    }
}
