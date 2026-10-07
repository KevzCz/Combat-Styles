package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.server.level.ServerLevel;

import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.config.OptionField;
import net.pixeldreamstudios.combat_styles.config.OptionSources;

import java.util.List;

public interface SummonAbility {

    String TUNE_DAMAGE = "damage";
    String TUNE_OPTIONS = OptionSources.KEY_OPTIONS;
    List<String> DAMAGE_ONLY = List.of(TUNE_DAMAGE);

    AbilityKind kind();

    void run(ServerLevel level, AbilityHost host, int elapsed, float damage, AbilityConfig.Entry tuned);

    default List<String> usedBy() {
        return List.of();
    }

    default List<String> tunable() {
        return DAMAGE_ONLY;
    }

    default List<OptionField> options() {
        return List.of();
    }

    default boolean scripted() {
        return false;
    }
}
