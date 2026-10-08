package net.pixeldreamstudios.combat_styles.custom;

import net.pixeldreamstudios.combat_styles.config.ChoiceSource;
import net.pixeldreamstudios.combat_styles.config.OptionField;

import java.util.List;

public final class CustomChoices implements ChoiceSource {

    @Override
    public String selector() {
        return CustomUse.KEY_HOOK;
    }

    @Override
    public List<String> values() {
        return CustomRegistry.ids();
    }

    @Override
    public List<OptionField> options(String value) {
        return CustomRegistry.optionsFor(value);
    }
}
