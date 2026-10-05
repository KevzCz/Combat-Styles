package net.pixeldreamstudios.combat_styles.config;

import dev.architectury.platform.Platform;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.nio.file.Path;

public final class ConfigPaths {

    public static final String ATTACKS = "attacks";
    public static final String ATTACKS_BACKUP = "attacks_backup";
    public static final String CUSTOM_ATTACKS = "custom_attacks";
    public static final String CUSTOM_STYLES = "custom_styles";
    public static final String DEFAULT_STYLES = "default_styles";
    public static final String LEGACY_STYLES = "styles";
    public static final String STYLE_BOOKS = "style_books";
    public static final String WEAPON_ROLLS = "weapon_rolls";
    public static final String AFFIXES = "custom_affixes";
    public static final String STYLE_ID_LEDGER = "style_ids.json";

    private ConfigPaths() {
    }

    public static Path root() {
        return Platform.getConfigFolder().resolve(CombatStyles.MOD_ID);
    }

    public static Path of(String folder) {
        return root().resolve(folder);
    }
}
