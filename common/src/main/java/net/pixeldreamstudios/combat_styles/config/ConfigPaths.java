package net.pixeldreamstudios.combat_styles.config;

import dev.architectury.platform.Platform;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.nio.file.Path;

public final class ConfigPaths {

    public static final String ATTACKS = "attacks";
    public static final String STYLES = "styles";
    public static final String AFFIXES = "custom_affixes";

    private ConfigPaths() {
    }

    public static Path root() {
        return Platform.getConfigFolder().resolve(CombatStyles.MOD_ID);
    }

    public static Path of(String folder) {
        return root().resolve(folder);
    }
}
