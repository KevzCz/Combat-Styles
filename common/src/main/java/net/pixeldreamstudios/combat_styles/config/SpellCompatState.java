package net.pixeldreamstudios.combat_styles.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SpellCompatState {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "compat.json";

    public static final String MAGIC_SOURCE = "minecraft:magic";
    public static final String ARCANE_SOURCE = SpellPowerAccess.MOD_ID + ":arcane";

    private static boolean changed;

    public static final class Marker {
        public boolean generatedWithSpellPower;
    }

    private SpellCompatState() {
    }

    public static void load() {
        boolean present = SpellPowerAccess.isLoaded();
        Path file = ConfigPaths.root().resolve(FILE);
        Marker marker = read(file);
        changed = marker == null || marker.generatedWithSpellPower != present;
        if (changed) {
            Marker updated = new Marker();
            updated.generatedWithSpellPower = present;
            write(file, updated);
        }
    }

    public static boolean compatChanged() {
        return changed;
    }

    public static String damageSource() {
        return SpellPowerAccess.isLoaded() ? ARCANE_SOURCE : MAGIC_SOURCE;
    }

    private static Marker read(Path file) {
        if (!Files.exists(file)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            return GSON.fromJson(reader, Marker.class);
        } catch (IOException | RuntimeException failure) {
            CombatStyles.LOGGER.warn("Could not read {}", file, failure);
            return null;
        }
    }

    private static void write(Path file, Marker marker) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(marker, writer);
            }
        } catch (IOException failure) {
            CombatStyles.LOGGER.warn("Could not write {}", file, failure);
        }
    }
}
