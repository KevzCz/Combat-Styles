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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class DefaultStyleConfig {

    private static final String EXTENSION = ".json";
    private static final char NAMESPACE_SEPARATOR = ':';
    private static final char FILE_SEPARATOR = '_';

    private DefaultStyleConfig() {
    }

    public static Path directory() {
        return ConfigPaths.of(ConfigPaths.DEFAULT_STYLES);
    }

    public static int reload() {
        List<DefaultStyleLoader.Definition> definitions = new ArrayList<>();
        Path directory = directory();
        if (Files.isDirectory(directory)) {
            try (Stream<Path> files = Files.list(directory)) {
                files.filter(path -> path.toString().endsWith(EXTENSION))
                        .forEach(path -> read(path).ifPresent(definitions::add));
            } catch (IOException exception) {
                CombatStyles.LOGGER.error("Unable to list {}", directory, exception);
            }
        }
        DefaultStyleSet compiled = DefaultStyleSet.compile(definitions);
        DefaultStyles.acceptConfig(compiled);
        return compiled.targets().size();
    }

    private static Optional<DefaultStyleLoader.Definition> read(Path path) {
        try {
            JsonElement json = JsonParser.parseString(Files.readString(path));
            return DefaultStyleLoader.Definition.CODEC
                    .parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error ->
                            CombatStyles.LOGGER.error("Invalid default style {}: {}", path, error));
        } catch (IOException | RuntimeException exception) {
            CombatStyles.LOGGER.error("Unable to read {}", path, exception);
            return Optional.empty();
        }
    }

    public static Path fileFor(ResourceLocation itemId) {
        return directory().resolve(itemId.toString().replace(NAMESPACE_SEPARATOR, FILE_SEPARATOR)
                + EXTENSION);
    }

    public static Path exportFrom(ItemStack stack, UUID holder) {
        ResourceLocation itemId = itemIdOf(stack);
        if (itemId == null || !SequenceService.isSupported(stack)) {
            return null;
        }
        StyleSequence sequence = SequenceService.sequenceOf(stack, holder);
        if (sequence.isEmpty()) {
            return null;
        }

        Map<String, AttackOverride> overrides = SequenceService.attackOverridesOf(stack);
        List<StyleEntry> entries = new ArrayList<>(sequence.entries());
        for (int i = 0; i < entries.size(); i++) {
            StyleEntry entry = entries.get(i);
            StylePage page = StyleRegistry.get(CustomStyles.resolve(entry).styleId());
            AttackOverride effective = page == null
                    ? null
                    : AttackOverrides.get(page.styleId());
            AttackOverride declared = entry.attackOverride().orElse(null);
            if (declared != null) {
                effective = effective == null ? declared : effective.overriddenBy(declared);
            }
            AttackOverride fromSlot = overrides.get(String.valueOf(i));
            if (fromSlot != null) {
                effective = effective == null ? fromSlot : effective.overriddenBy(fromSlot);
            }
            if (effective != null) {
                entries.set(i, entry.withAttackOverride(effective));
            }
        }

        return export(itemId, new StyleSequence(sequence.name(), entries),
                DefaultStyles.slotLimitFor(stack),
                DefaultStyles.slotTuningsFor(stack),
                SequenceService.isLocked(stack),
                DefaultStyles.keepsThroughRolls(stack));
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

        Path file = fileFor(itemId);
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

    public static boolean remove(ResourceLocation itemId) {
        if (itemId == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(fileFor(itemId));
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to remove the default style for {}", itemId, exception);
            return false;
        }
    }

    public static ResourceLocation itemIdOf(ItemStack stack) {
        return stack == null || stack.isEmpty()
                ? null
                : BuiltInRegistries.ITEM.getKey(stack.getItem());
    }
}
