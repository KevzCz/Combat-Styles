package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.damage.CritGroups;
import net.pixeldreamstudios.combat_styles.damage.ScaledNumber;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.style.StyleText;

public class CritGroupListScreen extends DefinitionListScreen<ScaledNumber.Crit> {

    private static final String KEY_TITLE = "screen.combat_styles.crit_group_list";
    private static final String KEY_SUMMARY = "screen.combat_styles.crit_group_summary";
    private static final String KEY_SUMMARY_ADD = "screen.combat_styles.crit_group_summary_add";
    private static final String KEY_REACTS = "screen.combat_styles.crit_group_reacts";
    private static final String KEY_EDITOR = "screen.combat_styles.crit_group_editor";
    private static final float PERCENT = 100.0F;

    public CritGroupListScreen(Screen parent, boolean creative) {
        super(Component.translatable(KEY_TITLE), parent, creative, CritGroups.DEFINITIONS,
                EffectSchema.ROOT_CRIT, EditConfigPayload.Kind.CRIT_GROUP,
                EditConfigPayload.Kind.CRIT_GROUP_CLEAR, ScaledNumber.Crit.SAMPLE,
                Component.translatable(KEY_EDITOR));
    }

    @Override
    protected String describe(ScaledNumber.Crit value) {
        boolean adds = value.modeOrDefault() == ScaledNumber.Crit.Mode.ADD_VALUE;
        String summary = Component.translatable(adds ? KEY_SUMMARY_ADD : KEY_SUMMARY,
                StyleText.formatDecimal(value.chance().map(ScaledNumber::base)
                        .orElse(ScaledNumber.Crit.NO_CHANCE) * PERCENT),
                StyleText.formatDecimal(value.value().map(ScaledNumber::base)
                        .orElse(adds ? ScaledNumber.Crit.NO_ADDITION
                                : ScaledNumber.Crit.DEFAULT_MULTIPLIER))).getString();
        return value.onCrit().isPresent()
                ? summary + Component.translatable(KEY_REACTS).getString()
                : summary;
    }

    @Override
    protected int tint(ScaledNumber.Crit value) {
        return ScreenTheme.ACCENT;
    }
}
