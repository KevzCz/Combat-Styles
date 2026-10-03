package net.pixeldreamstudios.combat_styles.style;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.bettercombat.api.WeaponAttributes;

import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.ConfigPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class AttackOverrides {
    private static final String EXTENSION = ".json";
    private static final String UNSOURCED = "unsourced";
    private static final String DATAPACK_FOLDER = "datapack";
    private static final String CONFIG_FOLDER = "config";
    private static final String ALL_NAMESPACES = "all";
    private static final String NAMESPACE_SEPARATOR = ":";
    private static final String UNSAFE_CHARACTERS = "[^A-Za-z0-9._-]";
    private static final String SAFE_REPLACEMENT = "_";
    private static final int OWNED_DEPTH = 3;
    private static final DateTimeFormatter BACKUP_STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static final Map<String, AttackOverride> OVERRIDES = new HashMap<>();
    private static final Map<String, Path> FILES = new HashMap<>();
    private static final Map<String, AttackOverride> SYNCED = new ConcurrentHashMap<>();
    private static volatile boolean remote;

    public record Refresh(int moved, List<String> lines, Path backup) {
    }

    private AttackOverrides() {
    }

    public static Path directory() {
        return ConfigPaths.of(ConfigPaths.ATTACKS);
    }

    public static int reload() {
        OVERRIDES.clear();
        FILES.clear();
        Path directory = directory();
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        adoptUnsourced(directory);

        List<Path> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(EXTENSION))
                    .forEach(found::add);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to list {}", directory, exception);
        }
        found.sort(Comparator.comparing((Path path) -> isOwnerFolder(directory.relativize(path)))
                .thenComparing(Path::toString));
        for (Path path : found) {
            load(path, idFor(directory.relativize(path)));
        }
        return OVERRIDES.size();
    }

    private static boolean isOwnerFolder(Path relative) {
        if (relative.getNameCount() != OWNED_DEPTH) {
            return false;
        }
        String first = relative.getName(0).toString();
        return DATAPACK_FOLDER.equals(first) || CONFIG_FOLDER.equals(first);
    }

    private static String idFor(Path relative) {
        String fileName = relative.getFileName().toString();
        String stem = fileName.substring(0, fileName.length() - EXTENSION.length());
        if (isOwnerFolder(relative)) {
            return relative.getName(1) + NAMESPACE_SEPARATOR + stem;
        }
        return styleIdForFile(stem);
    }

    private static void adoptUnsourced(Path directory) {
        Path unsourced = directory.resolve(UNSOURCED);
        if (!Files.isDirectory(unsourced)) {
            return;
        }
        Map<String, String> customByStem = new LinkedHashMap<>();
        for (CustomAttack attack : CustomAttacks.all()) {
            customByStem.put(sanitize(attack.id()), attack.id());
        }
        for (CustomStyle style : CustomStyles.all()) {
            customByStem.put(sanitize(style.id()), style.id());
        }
        List<Path> candidates;
        try (Stream<Path> files = Files.list(unsourced)) {
            candidates = files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(EXTENSION))
                    .toList();
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to list {}", unsourced, exception);
            return;
        }
        for (Path file : candidates) {
            String fileName = file.getFileName().toString();
            String styleId = customByStem.get(fileName.substring(0,
                    fileName.length() - EXTENSION.length()));
            if (styleId == null) {
                continue;
            }
            Path target = ownerFile(styleId);
            if (target == null || Files.exists(target)) {
                continue;
            }
            try {
                Files.createDirectories(target.getParent());
                Files.move(file, target);
                CombatStyles.LOGGER.info("Moved {} to {}", file, target);
            } catch (IOException exception) {
                CombatStyles.LOGGER.error("Unable to move {} to {}", file, target, exception);
            }
        }
    }

    private static String styleIdForFile(String stem) {
        for (StylePage page : StyleRegistry.all()) {
            String id = page.styleId();
            String encoded = sanitize(id);
            if (encoded.equals(stem)) {
                return id;
            }
            if (stem.equals(namespaceOf(page) + SAFE_REPLACEMENT + encoded)) {
                return id;
            }
        }
        return stem;
    }

    private static String namespaceOf(StylePage page) {
        ResourceLocation source = page == null ? null : page.sourceWeapon();
        String namespace = source == null ? null : source.getNamespace();
        return namespace == null || namespace.isBlank()
                ? UNSOURCED
                : sanitize(namespace);
    }

    private static String sanitize(String raw) {
        return raw.replaceAll(UNSAFE_CHARACTERS, SAFE_REPLACEMENT);
    }

    private static boolean isCustom(String styleId) {
        return CustomAttacks.contains(styleId) || CustomStyles.contains(styleId);
    }

    private static boolean hasDatapackSource(String styleId) {
        return CustomAttacks.hasDatapackVersion(styleId) || CustomStyles.hasDatapackVersion(styleId);
    }

    private static Path ownerFile(String styleId) {
        if (styleId == null || styleId.isBlank()) {
            return null;
        }
        Path directory = directory();
        int split = styleId.indexOf(NAMESPACE_SEPARATOR);
        if (isCustom(styleId) && split > 0) {
            String folder = hasDatapackSource(styleId) ? DATAPACK_FOLDER : CONFIG_FOLDER;
            return directory.resolve(folder)
                    .resolve(sanitize(styleId.substring(0, split)))
                    .resolve(sanitize(styleId.substring(split + 1)) + EXTENSION);
        }
        StylePage page = StyleRegistry.get(styleId);
        String folder = page == null ? UNSOURCED : namespaceOf(page);
        return directory.resolve(folder).resolve(sanitize(styleId) + EXTENSION);
    }

    private static String ownerNamespace(String styleId, Path file) {
        Path relative = directory().relativize(file);
        if (isOwnerFolder(relative)) {
            return relative.getName(1).toString();
        }
        String first = relative.getName(0).toString();
        if (relative.getNameCount() > 1 && !UNSOURCED.equals(first)) {
            return first;
        }
        int split = styleId.indexOf(NAMESPACE_SEPARATOR);
        return split > 0 ? styleId.substring(0, split) : UNSOURCED;
    }

    private static boolean write(Path file, AttackOverride override) {
        return AttackOverride.CODEC.encodeStart(JsonOps.INSTANCE, override)
                .resultOrPartial(error -> CombatStyles.LOGGER.error("Encode failed: {}", error))
                .map(json -> {
                    try {
                        Files.createDirectories(file.getParent());
                        Files.writeString(file, new GsonBuilder().setPrettyPrinting()
                                .create().toJson(json));
                        return Boolean.TRUE;
                    } catch (IOException exception) {
                        CombatStyles.LOGGER.error("Unable to write {}", file, exception);
                        return Boolean.FALSE;
                    }
                })
                .orElse(Boolean.FALSE);
    }

    private static void load(Path path, String styleId) {
        try {
            JsonElement json = JsonParser.parseString(Files.readString(path));
            AttackOverride.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error ->
                            CombatStyles.LOGGER.error("Invalid override {}: {}", styleId, error))
                    .ifPresent(override -> {
                        OVERRIDES.put(styleId, override);
                        FILES.put(styleId, path);
                    });
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to read {}", path, exception);
        }
    }

    public static boolean save(String styleId, AttackOverride override) {
        if (styleId == null || styleId.isBlank() || override == null) {
            return false;
        }
        StylePage base = StyleRegistry.raw(styleId);
        AttackOverride trimmed = base == null
                ? override
                : override.withoutMatching(AttackOverride.from(base));
        if (trimmed.isBlank()) {
            clear(styleId);
            return true;
        }
        Path target = ownerFile(styleId);
        if (target == null || !write(target, trimmed)) {
            return false;
        }
        Path previous = FILES.put(styleId, target);
        if (previous != null && !previous.equals(target)) {
            delete(previous);
        }
        OVERRIDES.put(styleId, trimmed);
        StyleRegistry.rebuild();
        return true;
    }

    public static boolean clear(String styleId) {
        Path file = styleId == null ? null : FILES.remove(styleId);
        OVERRIDES.remove(styleId);
        boolean removed = file != null && delete(file);
        StyleRegistry.rebuild();
        return removed;
    }

    public static boolean has(String styleId) {
        return styleId != null && get(styleId) != null;
    }

    private static boolean delete(Path file) {
        try {
            return Files.deleteIfExists(file);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to delete {}", file, exception);
            return false;
        }
    }

    public static Refresh refresh(String namespace) {
        boolean everything = namespace == null || namespace.isBlank()
                || ALL_NAMESPACES.equalsIgnoreCase(namespace);
        Path directory = directory();
        Path backup = ConfigPaths.of(ConfigPaths.ATTACKS_BACKUP)
                .resolve(LocalDateTime.now().format(BACKUP_STAMP));
        List<String> lines = new ArrayList<>();
        int moved = 0;
        for (Map.Entry<String, Path> entry : new LinkedHashMap<>(FILES).entrySet()) {
            String styleId = entry.getKey();
            Path file = entry.getValue();
            if (!everything && !namespace.equalsIgnoreCase(ownerNamespace(styleId, file))) {
                continue;
            }
            StylePage base = StyleRegistry.raw(styleId);
            AttackOverride override = OVERRIDES.get(styleId);
            if (override != null && base != null) {
                List<String> differences = override.differencesFrom(AttackOverride.from(base));
                if (!differences.isEmpty()) {
                    lines.add(styleId + ": " + String.join(", ", differences));
                }
            }
            Path target = backup.resolve(directory.relativize(file));
            try {
                Files.createDirectories(target.getParent());
                Files.move(file, target);
                moved++;
            } catch (IOException exception) {
                CombatStyles.LOGGER.error("Unable to back up {}", file, exception);
            }
        }
        reload();
        StyleRegistry.rebuild();
        return new Refresh(moved, lines, backup);
    }

    public static Set<String> namespaces() {
        Set<String> namespaces = new TreeSet<>();
        FILES.forEach((styleId, file) -> namespaces.add(ownerNamespace(styleId, file)));
        return namespaces;
    }

    public static boolean isEditable(String styleId) {
        return styleId != null && !styleId.isBlank();
    }

    public static void acceptSync(Map<String, AttackOverride> synced) {
        SYNCED.clear();
        SYNCED.putAll(synced);
        remote = true;
        StyleRegistry.rebuild();
    }

    public static void acceptLocal(String styleId, AttackOverride override) {
        if (styleId == null || override == null) {
            return;
        }
        (remote ? SYNCED : OVERRIDES).put(styleId, override);
        StyleRegistry.rebuild();
    }

    public static void clearSync() {
        SYNCED.clear();
        remote = false;
        StyleRegistry.rebuild();
    }

    public static Map<String, AttackOverride> snapshot() {
        return Map.copyOf(OVERRIDES);
    }

    public static AttackOverride get(String styleId) {
        return remote ? SYNCED.get(styleId) : OVERRIDES.get(styleId);
    }

    public static boolean isDisabled(String styleId) {
        AttackOverride override = OVERRIDES.get(styleId);
        return override != null && override.isDisabled();
    }

    public static StylePage apply(StylePage page) {
        return applyTo(page, OVERRIDES.get(page.styleId()));
    }

    public static StylePage applyTo(StylePage page, AttackOverride override) {
        return applyTo(page, override, false);
    }

    public static StylePage scaleWith(StylePage page, AttackOverride augment) {
        return applyTo(page, augment, true);
    }

    public static StylePage applyTo(StylePage page, AttackOverride override, boolean scaling) {
        if (override == null) {
            return page;
        }

        WeaponAttributes.Attack source = page.attack();
        WeaponAttributes.HitBoxShape hitbox = override.hitbox()
                .map(AttackOverrides::hitbox)
                .orElse(source.hitbox());

        WeaponAttributes.Attack patched = new WeaponAttributes.Attack(
                source.conditions(),
                hitbox == null ? source.hitbox() : hitbox,
                scaling
                        ? source.damageMultiplier() * override.damageMultiplier().orElse(1.0D)
                        : override.damageMultiplier().orElse(source.damageMultiplier()),
                override.movementSpeedMultiplier().orElse(source.movementSpeedMultiplier()),
                scaling
                        ? source.rangeMultiplier() * override.rangeMultiplier().orElse(1.0F)
                        : override.rangeMultiplier().orElse(source.rangeMultiplier()),
                override.angle().orElse(source.angle()),
                override.upswing().orElse(source.upswing()),
                override.animation().orElse(source.animation()),
                swingSound(override, source),
                source.impactSound(),
                source.trailParticles());

        StyleRarity rarity = override.rarity()
                .map(AttackOverrides::rarity)
                .orElse(page.rarity());

        return new StylePage(page.styleId(), page.category(), page.attackIndex(),
                patched, page.sourceWeapon(), rarity == null ? page.rarity() : rarity);
    }

    private static WeaponAttributes.Sound swingSound(AttackOverride override,
                                                     WeaponAttributes.Attack source) {
        if (override.swingSound().isEmpty()) {
            return source.swingSound();
        }
        String id = override.swingSound().get();
        if (id.isBlank() || AttackOverride.isSilent(id)) {
            return null;
        }
        return new WeaponAttributes.Sound(id);
    }

    private static WeaponAttributes.HitBoxShape hitbox(String name) {
        try {
            return WeaponAttributes.HitBoxShape.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            CombatStyles.LOGGER.warn("Unknown hitbox shape {}", name);
            return null;
        }
    }

    private static StyleRarity rarity(String name) {
        try {
            return StyleRarity.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            CombatStyles.LOGGER.warn("Unknown rarity {}", name);
            return null;
        }
    }
}
