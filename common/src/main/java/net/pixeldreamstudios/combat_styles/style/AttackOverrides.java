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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class AttackOverrides {
    private static final String EXTENSION = ".json";
    private static final String UNSOURCED = "unsourced";

    private static final Map<String, AttackOverride> OVERRIDES = new HashMap<>();
    private static final Map<String, AttackOverride> SYNCED = new ConcurrentHashMap<>();
    private static final Set<String> GENERATED = ConcurrentHashMap.newKeySet();
    private static volatile boolean remote;

    private AttackOverrides() {
    }

    public static Path directory() {
        return ConfigPaths.of(ConfigPaths.ATTACKS);
    }

    public static void generateMissing(Iterable<StylePage> pages) {
        Path directory = directory();
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to create {}", directory, exception);
            return;
        }

        int written = 0;
        for (StylePage page : pages) {
            if (!GENERATED.add(page.styleId())) {
                continue;
            }
            Path file = fileFor(directory, page);
            if (Files.exists(file)) {
                continue;
            }
            try {
                Files.createDirectories(file.getParent());
            } catch (IOException exception) {
                CombatStyles.LOGGER.error("Unable to create {}", file.getParent(), exception);
                continue;
            }
            if (write(file, AttackOverride.from(page))) {
                written++;
            }
        }
        if (written > 0) {
            CombatStyles.LOGGER.info("Generated {} attack override files in {}", written, directory);
        }
    }

    private static String styleIdForFile(String stem) {
        for (StylePage page : StyleRegistry.all()) {
            String id = page.styleId();
            String encoded = sanitize(id);
            if (encoded.equals(stem)) {
                return id;
            }
            if (stem.equals(namespaceOf(page) + "_" + encoded)) {
                return id;
            }
        }
        return stem;
    }

    private static String fileNameOf(String styleId) {
        return styleId.replaceAll("[^A-Za-z0-9._-]", "_") + EXTENSION;
    }

    private static String namespaceOf(StylePage page) {
        ResourceLocation source = page == null ? null : page.sourceWeapon();
        String namespace = source == null ? null : source.getNamespace();
        return namespace == null || namespace.isBlank()
                ? UNSOURCED
                : sanitize(namespace);
    }

    private static String sanitize(String raw) {
        return raw.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static Path fileFor(Path directory, StylePage page) {
        return directory.resolve(namespaceOf(page)).resolve(fileNameOf(page.styleId()));
    }

    public static void regenerateAll(Iterable<StylePage> pages) {
        Path directory = directory();
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to create {}", directory, exception);
            return;
        }
        for (StylePage page : pages) {
            Path file = fileFor(directory, page);
            try {
                Files.createDirectories(file.getParent());
            } catch (IOException exception) {
                CombatStyles.LOGGER.error("Unable to create {}", file.getParent(), exception);
                continue;
            }
            write(file, AttackOverride.from(page));
        }
    }

    private static boolean write(Path file, AttackOverride override) {
        return AttackOverride.CODEC.encodeStart(JsonOps.INSTANCE, override)
                .resultOrPartial(error -> CombatStyles.LOGGER.error("Encode failed: {}", error))
                .map(json -> {
                    try {
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

    public static int reload() {
        OVERRIDES.clear();
        GENERATED.clear();
        Path directory = directory();
        if (!Files.isDirectory(directory)) {
            return 0;
        }

        try (Stream<Path> files = Files.walk(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(EXTENSION)).forEach(path -> {
                String fileName = path.getFileName().toString();
                String stem = fileName.substring(0, fileName.length() - EXTENSION.length());
                load(path, styleIdForFile(stem));
            });
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to list {}", directory, exception);
        }
        return OVERRIDES.size();
    }

    private static void load(Path path, String styleId) {
        try {
            JsonElement json = JsonParser.parseString(Files.readString(path));
            AttackOverride.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error ->
                            CombatStyles.LOGGER.error("Invalid override {}: {}", styleId, error))
                    .ifPresent(override -> OVERRIDES.put(styleId, override));
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to read {}", path, exception);
        }
    }

    public static boolean save(String styleId, AttackOverride override) {
        if (styleId == null || styleId.isBlank() || override == null) {
            return false;
        }
        Path directory = directory();
        StylePage page = StyleRegistry.get(styleId);
        Path file = page == null
                ? directory.resolve(UNSOURCED).resolve(fileNameOf(styleId))
                : fileFor(directory, page);
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Unable to create {}", file.getParent(), exception);
            return false;
        }
        if (!write(file, override)) {
            return false;
        }
        OVERRIDES.put(styleId, override);
        StyleRegistry.rebuild();
        return true;
    }

    public static boolean isEditable(String styleId) {
        if (styleId == null || styleId.isBlank()) {
            return false;
        }
        if (CustomAttacks.isDatapackOwned(styleId)) {
            return false;
        }
        return !CustomStyles.isDatapackOwned(styleId);
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
