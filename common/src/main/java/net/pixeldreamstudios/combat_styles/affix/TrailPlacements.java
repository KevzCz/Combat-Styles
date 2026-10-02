package net.pixeldreamstudios.combat_styles.affix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.architectury.platform.Platform;
import net.bettercombat.api.fx.ParticlePlacement;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TrailPlacements {

    private static final String FOLDER = "bettercombat";
    private static final String FILE = "weapon_trails.json";
    private static final String ANIMATION_BASED = "animation_based";
    private static final String ROLL_SET = "roll_set";
    private static final String LOCAL_YAW = "local_yaw";
    private static final String PITCH_ADDITION = "pitch_addition";
    private static final String PARTICLE_TYPE = "particle_type";

    private static final Map<String, ParticlePlacement> PLACEMENTS = new HashMap<>();
    private static final Map<String, List<ParticlePlacement>> SEQUENCES = new HashMap<>();
    private static boolean loaded;

    private TrailPlacements() {
    }

    public static void reload() {
        PLACEMENTS.clear();
        SEQUENCES.clear();
        loaded = true;

        Path file = Platform.getConfigFolder().resolve(FOLDER).resolve(FILE);
        if (!Files.isRegularFile(file)) {
            CombatStyles.LOGGER.debug("No weapon trail config at {}", file);
            return;
        }

        try {
            JsonElement root = JsonParser.parseString(Files.readString(file));
            if (!root.isJsonObject()) {
                return;
            }
            JsonElement animations = root.getAsJsonObject().get(ANIMATION_BASED);
            if (animations == null || !animations.isJsonObject()) {
                return;
            }
            for (Map.Entry<String, JsonElement> entry : animations.getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonArray() || entry.getValue().getAsJsonArray().isEmpty()) {
                    continue;
                }
                List<ParticlePlacement> sequence = new ArrayList<>();
                for (JsonElement element : entry.getValue().getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        sequence.add(placementOf(element.getAsJsonObject()));
                    }
                }
                if (sequence.isEmpty()) {
                    continue;
                }
                SEQUENCES.put(entry.getKey(), List.copyOf(sequence));
                PLACEMENTS.put(entry.getKey(), sequence.getFirst());
            }
            CombatStyles.LOGGER.info("Loaded {} trail placements from {}", PLACEMENTS.size(), file);
        } catch (IOException | RuntimeException exception) {
            CombatStyles.LOGGER.error("Unable to read {}", file, exception);
        }
    }

    private static ParticlePlacement placementOf(JsonObject json) {
        return new ParticlePlacement(
                json.has(PARTICLE_TYPE) ? json.get(PARTICLE_TYPE).getAsString() : "",
                0.0F, 0.0F, 0.0F,
                json.has(LOCAL_YAW) ? json.get(LOCAL_YAW).getAsFloat() : 0.0F,
                json.has(PITCH_ADDITION) ? json.get(PITCH_ADDITION).getAsFloat() : 0.0F,
                json.has(ROLL_SET) ? json.get(ROLL_SET).getAsFloat() : 0.0F);
    }

    public static ParticlePlacement get(String animation) {
        if (!loaded) {
            reload();
        }
        return animation == null ? null : PLACEMENTS.get(animation);
    }

    public static List<ParticlePlacement> sequence(String animation) {
        if (!loaded) {
            reload();
        }
        if (animation == null) {
            return List.of();
        }
        return SEQUENCES.getOrDefault(animation, List.of());
    }

    public static boolean isStab(String animation) {
        ParticlePlacement placement = get(animation);
        return placement != null && placement.particle_type().contains("stab");
    }
}
