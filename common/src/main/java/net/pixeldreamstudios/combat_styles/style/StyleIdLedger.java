package net.pixeldreamstudios.combat_styles.style;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.config.ConfigPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class StyleIdLedger {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SHAPE = new TypeToken<TreeMap<String, String>>() { }.getType();
    private static final int EXAMPLES = 5;

    private StyleIdLedger() {
    }

    public static void check() {
        Map<String, String> current = StyleRegistry.nativeSignatures();
        if (current.isEmpty()) {
            return;
        }
        Path file = ConfigPaths.of(ConfigPaths.STYLE_ID_LEDGER);
        Map<String, String> previous = read(file);
        List<String> changed = new ArrayList<>();
        int removed = 0;
        for (Map.Entry<String, String> entry : previous.entrySet()) {
            String now = current.get(entry.getKey());
            if (now == null) {
                removed++;
            } else if (!now.equals(entry.getValue())) {
                changed.add(entry.getKey());
            }
        }
        if (!changed.isEmpty()) {
            CombatStyles.LOGGER.warn("{} style ids now point at a different attack than last load, "
                            + "most likely because a weapon mod changed its own attack list: {}",
                    changed.size(), changed.subList(0, Math.min(EXAMPLES, changed.size())));
        }
        if (removed > 0 && CombatStylesConfig.get().debugLogging()) {
            CombatStyles.LOGGER.info("{} style ids from the last load no longer exist", removed);
        }
        TreeMap<String, String> snapshot = new TreeMap<>(current);
        if (!snapshot.equals(previous)) {
            write(file, snapshot);
        }
    }

    private static Map<String, String> read(Path file) {
        if (!Files.exists(file)) {
            return new TreeMap<>();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Map<String, String> parsed = GSON.fromJson(reader, SHAPE);
            return parsed == null ? new TreeMap<>() : new TreeMap<>(parsed);
        } catch (IOException | RuntimeException failure) {
            CombatStyles.LOGGER.warn("Could not read {}", file, failure);
            return new TreeMap<>();
        }
    }

    private static void write(Path file, Map<String, String> snapshot) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(snapshot, writer);
            }
        } catch (IOException failure) {
            CombatStyles.LOGGER.warn("Could not write {}", file, failure);
        }
    }
}
