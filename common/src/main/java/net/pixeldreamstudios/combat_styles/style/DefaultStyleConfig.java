package net.pixeldreamstudios.combat_styles.style;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.ConfigPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class DefaultStyleConfig {

    private static final String EXTENSION = ".json";

    private DefaultStyleConfig() {
    }

    public static Path directory() {
        return ConfigPaths.of(ConfigPaths.STYLES);
    }

    public static int reload() {
        Map<ResourceLocation, StyleSequence> loaded = new LinkedHashMap<>();
        Map<ResourceLocation, Integer> slotLimits = new LinkedHashMap<>();
        Set<ResourceLocation> appending = new HashSet<>();

        Path directory = directory();
        if (!Files.isDirectory(directory)) {
            DefaultStyles.acceptConfig(loaded, slotLimits, appending);
            return 0;
        }

        try (Stream<Path> files = Files.list(directory)) {
            files.filter(path -> path.toString().endsWith(EXTENSION))
                    .forEach(path -> load(path, loaded, slotLimits, appending));
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to list {}", directory, exception);
        }

        DefaultStyles.acceptConfig(loaded, slotLimits, appending);
        return loaded.size();
    }

    private static void load(Path path,
                             Map<ResourceLocation, StyleSequence> loaded,
                             Map<ResourceLocation, Integer> slotLimits,
                             Set<ResourceLocation> appending) {
        try {
            JsonElement json = JsonParser.parseString(Files.readString(path));
            DefaultStyleLoader.Definition definition = DefaultStyleLoader.Definition.CODEC
                    .parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error ->
                            CombatStyles.LOGGER.error("Invalid default style {}: {}", path, error))
                    .orElse(null);
            if (definition == null) {
                return;
            }

            StyleSequence sequence = definition.entries().isEmpty()
                    ? null
                    : new StyleSequence(definition.name(), definition.entries());

            for (ResourceLocation itemId : DefaultStyleLoader.targetsOf(definition)) {
                if (sequence != null) {
                    loaded.put(itemId, sequence);
                    if (definition.appends()) {
                        appending.add(itemId);
                    }
                }
                if (definition.slots() > 0) {
                    slotLimits.put(itemId, definition.slots());
                }
            }
        } catch (IOException | RuntimeException exception) {
            CombatStyles.LOGGER.error("Unable to read {}", path, exception);
        }
    }

    public static Path export(ResourceLocation itemId, StyleSequence sequence, int slots) {
        return export(itemId, sequence, slots, List.of(), false, false);
    }

    public static Path export(ResourceLocation itemId, StyleSequence sequence, int slots,
                              List<SlotTuning> tune, boolean locked, boolean keepThroughRolls) {
        Path directory = directory();
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to create {}", directory, exception);
            return null;
        }

        List<String> items = new ArrayList<>();
        items.add(itemId.toString());
        DefaultStyleLoader.Applies applies = new DefaultStyleLoader.Applies(
                items, List.of(), List.of(), false);
        DefaultStyleLoader.Sequence composed = new DefaultStyleLoader.Sequence(
                DefaultStyleLoader.Sequence.MODE_REPLACE, slots, sequence.entries());
        DefaultStyleLoader.Definition definition = new DefaultStyleLoader.Definition(
                applies, composed, tune == null ? List.of() : tune, 0, locked,
                keepThroughRolls, Optional.empty());

        Path file = directory.resolve(itemId.getPath() + EXTENSION);
        return DefaultStyleLoader.Definition.CODEC.encodeStart(JsonOps.INSTANCE, definition)
                .resultOrPartial(error -> CombatStyles.LOGGER.error("Encode failed: {}", error))
                .map(json -> {
                    try {
                        Files.writeString(file, new GsonBuilder().setPrettyPrinting()
                                .create().toJson(json));
                        return file;
                    } catch (IOException exception) {
                        CombatStyles.LOGGER.error("Unable to write {}", file, exception);
                        return null;
                    }
                })
                .orElse(null);
    }

    public static ResourceLocation itemIdOf(ItemStack stack) {
        return stack == null || stack.isEmpty()
                ? null
                : BuiltInRegistries.ITEM.getKey(stack.getItem());
    }
}
