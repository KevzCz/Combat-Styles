package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.loot.WeaponRoll;
import net.pixeldreamstudios.combat_styles.loot.WeaponRolls;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.style.StyleText;

public class WeaponRollListScreen extends DefinitionListScreen<WeaponRoll> {

    private static final String KEY_TITLE = "screen.combat_styles.weapon_roll_list";
    private static final String KEY_SUMMARY = "screen.combat_styles.weapon_roll_summary";
    private static final String KEY_EDITOR = "screen.combat_styles.weapon_roll_editor";
    public WeaponRollListScreen(Screen parent, boolean creative) {
        super(Component.translatable(KEY_TITLE), parent, creative, WeaponRolls.DEFINITIONS,
                EffectSchema.ROOT_WEAPON_ROLL, EditConfigPayload.Kind.WEAPON_ROLL,
                EditConfigPayload.Kind.WEAPON_ROLL_CLEAR, WeaponRoll.SAMPLE,
                Component.translatable(KEY_EDITOR));
    }

    @Override
    protected String describe(WeaponRoll value) {
        return StyleText.prettify(value.mode()) + " · " + Component.translatable(KEY_SUMMARY,
                StyleText.formatDecimal(value.weight()), value.styles().size()).getString();
    }

    @Override
    protected int tint(WeaponRoll value) {
        return ScreenTheme.ACCENT;
    }
}
