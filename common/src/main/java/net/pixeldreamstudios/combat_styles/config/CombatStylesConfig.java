package net.pixeldreamstudios.combat_styles.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.architectury.platform.Platform;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CombatStylesConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = CombatStyles.MOD_ID + ".json";
    private static final int MIN_ATTACKS = 1;
    private static final int MAX_ATTACKS = 32;

    private static CombatStylesConfig instance = new CombatStylesConfig();
    private static volatile boolean remote;

    private int maxAttacksPerSequence = 10;
    private boolean lootEnabled = true;
    private double mobDropChance = 0.15D;
    private double chestLootChance = 0.35D;
    private boolean requireLearnedStyles = true;
    private int maxAffixesPerSlot = 2;
    private int maxLootAffixes = 2;
    private double extraAffixChance = 0.12D;
    private double extraAffixFalloff = 0.4D;
    private boolean showLockedStyles = true;
    private boolean enforceStyleBudget = true;
    private double affixChance = 0.25D;
    private boolean affixesEnabled = true;
    private boolean animationPreviewEnabled = true;
    private double animationPreviewSpeed = 1.0D;
    private boolean debugLogging = false;
    private int motionDragWindowBonus = 30;
    private List<String> dragGrantingMotions = new ArrayList<>(List.of("dash", "blink", "vault"));
    private boolean sneakSuppressesMotion = true;

    private boolean categoryAffinityEnabled = true;
    private boolean preserveArc = true;
    private double relatedCost = 1.15D;
    private double relatedDamage = 0.95D;
    private double relatedRange = 0.95D;
    private double relatedUpswing = 1.05D;
    private double alienCost = 1.40D;
    private double alienDamage = 0.85D;
    private double alienRange = 0.85D;
    private double alienUpswing = 1.15D;
    private Map<String, List<String>> categoryRelations = defaultRelations();

    private double swingEffectDelay = 0.0D;
    private double slashWaveScale = 1.5D;
    private double thrustWaveScale = 2.0D;
    private boolean weaponRollsEnabled = true;
    private boolean rollCraftedWeapons = false;
    private boolean namedEntriesLocked = true;
    private boolean showAffinityInTooltip = true;
    private boolean alwaysExpandTooltips = false;
    private boolean tooltipGemBullets = true;
    private boolean tooltipIcons = true;
    private int hitHistoryTimeout = 60;
    private boolean cooldownHudEnabled = true;
    private String cooldownHudAnchor = "top_left";
    private int cooldownHudOffsetX = 8;
    private int cooldownHudOffsetY = 8;
    private boolean cooldownHudVertical = true;
    private boolean cooldownHudFlipped = false;
    private boolean cooldownHudHideWhenReady = false;
    private String cooldownHudDensity = ConfigOptions.HUD_DENSITY_DEFAULT;
    private boolean gameplayLootEnabled = false;
    private List<String> lootTableBlocklist = new ArrayList<>();
    private int weightCommon = 12;
    private int weightUncommon = 6;
    private int weightRare = 3;
    private int weightEpic = 2;
    private int weightLegendary = 1;
    private int weightMythic = 1;
    private boolean etchOncePerWeapon = true;
    private boolean etchRequiresCreative = false;
    private int maxMovesetsPerItem = 0;
    private boolean enforceMinimumAttacks = true;
    private double budgetPerStyle = 1.5D;
    private double budgetStyleSoftCap = 40.0D;
    private boolean budgetStyleLinear = false;
    private double budgetStyleMax = 0.0D;
    private boolean countCustomStyles = true;

    private static Map<String, List<String>> defaultRelations() {
        Map<String, List<String>> relations = new LinkedHashMap<>();
        relations.put("sword", List.of("dagger", "katana", "rapier", "cutlass"));
        relations.put("greatsword", List.of("claymore", "greataxe", "greathammer"));
        relations.put("spear", List.of("trident", "halberd", "glaive"));
        relations.put("axe", List.of("greataxe", "doubleaxe", "greathammer"));
        return relations;
    }

    public static CombatStylesConfig get() {
        return instance;
    }

    public static void load() {
        remote = false;
        Path path = configPath();
        if (!Files.exists(path)) {
            instance = new CombatStylesConfig();
            save();
            return;
        }
        instance = readLocal();
    }

    private static CombatStylesConfig readLocal() {
        Path path = configPath();
        if (!Files.exists(path)) {
            return new CombatStylesConfig();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            CombatStylesConfig loaded = GSON.fromJson(reader, CombatStylesConfig.class);
            CombatStylesConfig read = loaded == null ? new CombatStylesConfig() : loaded;
            read.clamp();
            return read;
        } catch (IOException | RuntimeException exception) {
            CombatStyles.LOGGER.error("Failed to read {}, falling back to defaults", FILE_NAME, exception);
            return new CombatStylesConfig();
        }
    }

    public static JsonElement rulesJson() {
        CombatStylesConfig rules = copyOf(instance);
        rules.copyPreferencesFrom(new CombatStylesConfig());
        return GSON.toJsonTree(rules);
    }

    public static void acceptServer(JsonElement json) {
        CombatStylesConfig received = parse(json);
        if (received == null) {
            return;
        }
        received.copyPreferencesFrom(instance);
        instance = received;
        remote = true;
    }

    public static boolean applyEdit(JsonElement json) {
        CombatStylesConfig received = parse(json);
        if (received == null) {
            return false;
        }
        received.copyPreferencesFrom(instance);
        instance = received;
        save();
        return true;
    }

    private static CombatStylesConfig parse(JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            return null;
        }
        try {
            CombatStylesConfig parsed = GSON.fromJson(json, CombatStylesConfig.class);
            if (parsed != null) {
                parsed.clamp();
            }
            return parsed;
        } catch (RuntimeException exception) {
            CombatStyles.LOGGER.error("Rejected malformed {} from the network", FILE_NAME, exception);
            return null;
        }
    }

    private static CombatStylesConfig copyOf(CombatStylesConfig source) {
        CombatStylesConfig copy = GSON.fromJson(GSON.toJsonTree(source), CombatStylesConfig.class);
        return copy == null ? new CombatStylesConfig() : copy;
    }

    private void copyPreferencesFrom(CombatStylesConfig other) {
        showAffinityInTooltip = other.showAffinityInTooltip;
        alwaysExpandTooltips = other.alwaysExpandTooltips;
        tooltipGemBullets = other.tooltipGemBullets;
        tooltipIcons = other.tooltipIcons;
        cooldownHudEnabled = other.cooldownHudEnabled;
        cooldownHudAnchor = other.cooldownHudAnchor;
        cooldownHudOffsetX = other.cooldownHudOffsetX;
        cooldownHudOffsetY = other.cooldownHudOffsetY;
        cooldownHudVertical = other.cooldownHudVertical;
        cooldownHudFlipped = other.cooldownHudFlipped;
        cooldownHudHideWhenReady = other.cooldownHudHideWhenReady;
        cooldownHudDensity = other.cooldownHudDensity;
        animationPreviewEnabled = other.animationPreviewEnabled;
        animationPreviewSpeed = other.animationPreviewSpeed;
        debugLogging = other.debugLogging;
    }

    public static void save() {
        Path path = configPath();
        CombatStylesConfig written = instance;
        if (remote) {
            written = readLocal();
            written.copyPreferencesFrom(instance);
        }
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(written, writer);
            }
        } catch (IOException exception) {
            CombatStyles.LOGGER.error("Failed to write {}", FILE_NAME, exception);
        }
    }

    private static Path configPath() {
        return Platform.getConfigFolder().resolve(CombatStyles.MOD_ID).resolve(FILE_NAME);
    }

    private void clamp() {
        maxAttacksPerSequence = Math.clamp(maxAttacksPerSequence, MIN_ATTACKS, MAX_ATTACKS);
        mobDropChance = Math.clamp(mobDropChance, 0.0D, 1.0D);
        chestLootChance = Math.clamp(chestLootChance, 0.0D, 1.0D);
        animationPreviewSpeed = Math.clamp(animationPreviewSpeed, 0.1D, 4.0D);
        affixChance = Math.clamp(affixChance, 0.0D, 1.0D);
    }

    public boolean categoryAffinityEnabled() {
        return categoryAffinityEnabled;
    }

    public double swingEffectDelay() {
        return swingEffectDelay;
    }

    public double slashWaveScale() {
        return slashWaveScale;
    }

    public double thrustWaveScale() {
        return thrustWaveScale;
    }

    public boolean weaponRollsEnabled() {
        return weaponRollsEnabled;
    }

    public boolean rollCraftedWeapons() {
        return rollCraftedWeapons;
    }

    public boolean namedEntriesLocked() {
        return namedEntriesLocked;
    }

    public boolean showAffinityInTooltip() {
        return showAffinityInTooltip;
    }

    public boolean alwaysExpandTooltips() {
        return alwaysExpandTooltips;
    }

    public boolean tooltipGemBullets() {
        return tooltipGemBullets;
    }

    public boolean tooltipIcons() {
        return tooltipIcons;
    }

    public int hitHistoryTimeout() {
        return hitHistoryTimeout;
    }

    public boolean cooldownHudEnabled() {
        return cooldownHudEnabled;
    }

    public String cooldownHudAnchor() {
        return cooldownHudAnchor;
    }

    public int cooldownHudOffsetX() {
        return cooldownHudOffsetX;
    }

    public int cooldownHudOffsetY() {
        return cooldownHudOffsetY;
    }

    public boolean cooldownHudVertical() {
        return cooldownHudVertical;
    }

    public boolean cooldownHudFlipped() {
        return cooldownHudFlipped;
    }

    public boolean cooldownHudHideWhenReady() {
        return cooldownHudHideWhenReady;
    }

    public String cooldownHudDensity() {
        return cooldownHudDensity;
    }

    public void placeCooldownHud(String anchor, int offsetX, int offsetY) {
        this.cooldownHudAnchor = anchor;
        this.cooldownHudOffsetX = offsetX;
        this.cooldownHudOffsetY = offsetY;
    }

    public boolean gameplayLootEnabled() {
        return gameplayLootEnabled;
    }

    public boolean etchOncePerWeapon() {
        return etchOncePerWeapon;
    }

    public boolean etchRequiresCreative() {
        return etchRequiresCreative;
    }

    public int maxMovesetsPerItem() {
        return maxMovesetsPerItem;
    }

    public boolean enforceMinimumAttacks() {
        return enforceMinimumAttacks;
    }

    public double budgetPerStyle() {
        return budgetPerStyle;
    }

    public double budgetStyleSoftCap() {
        return budgetStyleSoftCap;
    }

    public boolean budgetStyleLinear() {
        return budgetStyleLinear;
    }

    public double budgetStyleMax() {
        return budgetStyleMax;
    }

    public boolean countCustomStyles() {
        return countCustomStyles;
    }

    public List<String> lootTableBlocklist() {
        return lootTableBlocklist == null ? List.of() : lootTableBlocklist;
    }

    public int rarityWeight(String rarityName) {
        return switch (rarityName == null ? "" : rarityName.toLowerCase(Locale.ROOT)) {
            case "uncommon" -> Math.max(0, weightUncommon);
            case "rare" -> Math.max(0, weightRare);
            case "epic" -> Math.max(0, weightEpic);
            case "legendary" -> Math.max(0, weightLegendary);
            case "mythic" -> Math.max(0, weightMythic);
            default -> Math.max(0, weightCommon);
        };
    }

    public boolean preserveArc() {
        return preserveArc;
    }

    public record AffinityTier(double cost, double damage, double range, double upswing) {

        public static final AffinityTier NEUTRAL = new AffinityTier(1.0D, 1.0D, 1.0D, 1.0D);
    }

    public AffinityTier relatedMultipliers() {
        return new AffinityTier(relatedCost, relatedDamage, relatedRange, relatedUpswing);
    }

    public AffinityTier alienMultipliers() {
        return new AffinityTier(alienCost, alienDamage, alienRange, alienUpswing);
    }

    public Map<String, Set<String>> categoryRelations() {
        Map<String, Set<String>> resolved = new LinkedHashMap<>();
        if (categoryRelations == null) {
            return resolved;
        }
        for (Map.Entry<String, List<String>> entry : categoryRelations.entrySet()) {
            Set<String> related = new LinkedHashSet<>();
            for (String value : entry.getValue()) {
                related.add(value.toLowerCase(Locale.ROOT));
            }
            resolved.put(entry.getKey().toLowerCase(Locale.ROOT), related);
        }
        return resolved;
    }

    public int maxAttacksPerSequence() {
        return maxAttacksPerSequence;
    }

    public boolean lootEnabled() {
        return lootEnabled;
    }

    public double mobDropChance() {
        return mobDropChance;
    }

    public double chestLootChance() {
        return chestLootChance;
    }

    public int maxAffixesPerSlot() {
        return Math.max(1, maxAffixesPerSlot);
    }

    public int maxLootAffixes() {
        return Math.max(1, maxLootAffixes);
    }

    public double extraAffixChance() {
        return Math.clamp(extraAffixChance, 0.0D, 1.0D);
    }

    public double extraAffixFalloff() {
        return Math.clamp(extraAffixFalloff, 0.0D, 1.0D);
    }

    public boolean requireLearnedStyles() {
        return requireLearnedStyles;
    }

    public boolean showLockedStyles() {
        return showLockedStyles;
    }

    public boolean enforceStyleBudget() {
        return enforceStyleBudget;
    }

    public double affixChance() {
        return affixesEnabled ? affixChance : 0.0D;
    }

    public boolean affixesEnabled() {
        return affixesEnabled;
    }

    public boolean animationPreviewEnabled() {
        return animationPreviewEnabled;
    }

    public double animationPreviewSpeed() {
        return animationPreviewSpeed;
    }

    public boolean debugLogging() {
        return debugLogging;
    }

    public int motionDragWindowBonus() {
        return motionDragWindowBonus;
    }

    public List<String> dragGrantingMotions() {
        return dragGrantingMotions;
    }

    public boolean sneakSuppressesMotion() {
        return sneakSuppressesMotion;
    }
    public List<ConfigOptions.Option> options() {
        List<ConfigOptions.Option> options = new ArrayList<>();
        options.add(ConfigOptions.bool("categoryAffinityEnabled", "Category Affinity Enabled",
                () -> categoryAffinityEnabled, value -> categoryAffinityEnabled = value));
        options.add(ConfigOptions.bool("weaponRollsEnabled", "Weapon Rolls Enabled",
                () -> weaponRollsEnabled, value -> weaponRollsEnabled = value)
                .describedAs("Every weapon found in loot gets one weighted weapon roll."));
        options.add(ConfigOptions.bool("rollCraftedWeapons", "Roll Crafted Weapons",
                () -> rollCraftedWeapons, value -> rollCraftedWeapons = value)
                .describedAs("Crafted and smithed weapons also get a weapon roll."));
        options.add(ConfigOptions.bool("namedEntriesLocked", "Named Entries Locked",
                () -> namedEntriesLocked, value -> namedEntriesLocked = value));
        options.add(ConfigOptions.bool("showAffinityInTooltip", "Show Affinity In Tooltip",
                () -> showAffinityInTooltip, value -> showAffinityInTooltip = value));
        options.add(ConfigOptions.bool("gameplayLootEnabled", "Gameplay Loot Enabled",
                () -> gameplayLootEnabled, value -> gameplayLootEnabled = value)
                .describedAs("Also drop from gameplay/ tables: fishing, gifts, bartering, sniffer digs."));
        options.add(ConfigOptions.bool("etchOncePerWeapon", "Etch Once Per Weapon",
                () -> etchOncePerWeapon, value -> etchOncePerWeapon = value));
        options.add(ConfigOptions.bool("etchRequiresCreative", "Etch Requires Creative",
                () -> etchRequiresCreative, value -> etchRequiresCreative = value));
        options.add(ConfigOptions.bool("enforceMinimumAttacks", "Enforce Minimum Attacks",
                () -> enforceMinimumAttacks, value -> enforceMinimumAttacks = value));
        options.add(ConfigOptions.bool("budgetStyleLinear", "Budget Style Linear",
                () -> budgetStyleLinear, value -> budgetStyleLinear = value));
        options.add(ConfigOptions.bool("countCustomStyles", "Count Custom Styles",
                () -> countCustomStyles, value -> countCustomStyles = value));
        options.add(ConfigOptions.bool("preserveArc", "Preserve Arc",
                () -> preserveArc, value -> preserveArc = value));
        options.add(ConfigOptions.bool("lootEnabled", "Loot Enabled",
                () -> lootEnabled, value -> lootEnabled = value)
                .describedAs("Master switch for style page drops."));
        ConfigOptions.addList(options, "Loot", "lootTableBlocklist", lootTableBlocklist,
                "Table that never drops pages. Trailing * matches a prefix. Blank removes it.");
        options.add(ConfigOptions.bool("requireLearnedStyles", "Require Learned Styles",
                () -> requireLearnedStyles, value -> requireLearnedStyles = value));
        options.add(ConfigOptions.bool("showLockedStyles", "Show Locked Styles",
                () -> showLockedStyles, value -> showLockedStyles = value));
        options.add(ConfigOptions.bool("enforceStyleBudget", "Enforce Style Budget",
                () -> enforceStyleBudget, value -> enforceStyleBudget = value));
        options.add(ConfigOptions.bool("affixesEnabled", "Affixes Enabled",
                () -> affixesEnabled, value -> affixesEnabled = value));
        options.add(ConfigOptions.bool("animationPreviewEnabled", "Animation Preview Enabled",
                () -> animationPreviewEnabled, value -> animationPreviewEnabled = value));
        options.add(ConfigOptions.bool("debugLogging", "Debug Logging",
                () -> debugLogging, value -> debugLogging = value));
        ConfigOptions.addList(options, ConfigOptions.VISUALS, "dragGrantingMotions",
                dragGrantingMotions,
                "Motion effects that grant a drag window: dash, blink, or vault.",
                ConfigOptions.SUGGEST_DRAG_MOTIONS);
        options.add(ConfigOptions.integer("motionDragWindowBonus", "Motion Drag Window Bonus",
                () -> motionDragWindowBonus, value -> motionDragWindowBonus = value));
        options.add(ConfigOptions.bool("sneakSuppressesMotion", "Sneak Suppresses Motion",
                () -> sneakSuppressesMotion, value -> sneakSuppressesMotion = value));
        options.add(ConfigOptions.integer("hitHistoryTimeout", "Hit History Timeout",
                () -> hitHistoryTimeout, value -> hitHistoryTimeout = value));
        options.add(ConfigOptions.bool("alwaysExpandTooltips", "Always Expand Tooltips",
                () -> alwaysExpandTooltips, value -> alwaysExpandTooltips = value));
        options.add(ConfigOptions.bool("tooltipGemBullets", "Tooltip Gem Bullets",
                        () -> tooltipGemBullets, value -> tooltipGemBullets = value)
                .describedAs("Gem icons in front of tooltip lines. Off uses a plain indent."));
        options.add(ConfigOptions.bool("tooltipIcons", "Tooltip Trigger Icons",
                        () -> tooltipIcons, value -> tooltipIcons = value)
                .describedAs("Icons on trigger headers such as When you swing. Off shows text only."));
        options.add(ConfigOptions.bool("cooldownHudEnabled", "Cooldown HUD",
                () -> cooldownHudEnabled, value -> cooldownHudEnabled = value));
        options.add(ConfigOptions.bool("cooldownHudVertical", "Cooldown HUD Vertical",
                () -> cooldownHudVertical, value -> cooldownHudVertical = value));
        options.add(ConfigOptions.bool("cooldownHudFlipped", "Cooldown HUD Bars On Left",
                () -> cooldownHudFlipped, value -> cooldownHudFlipped = value));
        options.add(ConfigOptions.bool("cooldownHudHideWhenReady", "Cooldown HUD Hide When Ready",
                () -> cooldownHudHideWhenReady, value -> cooldownHudHideWhenReady = value));
        options.add(ConfigOptions.text("cooldownHudDensity", "Cooldown HUD Density",
                        () -> cooldownHudDensity, value -> cooldownHudDensity = value)
                .suggesting(ConfigOptions.KIND_HUD_DENSITY));
        options.add(ConfigOptions.integer("cooldownHudOffsetX", "Cooldown HUD Offset X",
                () -> cooldownHudOffsetX, value -> cooldownHudOffsetX = value));
        options.add(ConfigOptions.integer("cooldownHudOffsetY", "Cooldown HUD Offset Y",
                () -> cooldownHudOffsetY, value -> cooldownHudOffsetY = value));
        options.add(ConfigOptions.integer("maxMovesetsPerItem", "Max Movesets Per Item",
                () -> maxMovesetsPerItem, value -> maxMovesetsPerItem = value));
        options.add(ConfigOptions.integer("maxAttacksPerSequence", "Max Attacks Per Sequence",
                () -> maxAttacksPerSequence, value -> maxAttacksPerSequence = value));
        options.add(ConfigOptions.integer("maxAffixesPerSlot", "Max Affixes Per Slot",
                () -> maxAffixesPerSlot, value -> maxAffixesPerSlot = value));
        options.add(ConfigOptions.integer("maxLootAffixes", "Max Loot Affixes",
                () -> maxLootAffixes, value -> maxLootAffixes = value)
                .describedAs("Most affixes a dropped page can carry."));
        options.add(ConfigOptions.decimal("swingEffectDelay", "Swing Effect Delay",
                () -> swingEffectDelay, value -> swingEffectDelay = value));
        options.add(ConfigOptions.decimal("slashWaveScale", "Slash Wave Scale",
                () -> slashWaveScale, value -> slashWaveScale = value));
        options.add(ConfigOptions.decimal("thrustWaveScale", "Thrust Wave Scale",
                () -> thrustWaveScale, value -> thrustWaveScale = value));
        options.add(ConfigOptions.decimal("budgetPerStyle", "Budget Per Style",
                () -> budgetPerStyle, value -> budgetPerStyle = value));
        options.add(ConfigOptions.decimal("budgetStyleSoftCap", "Budget Style Soft Cap",
                () -> budgetStyleSoftCap, value -> budgetStyleSoftCap = value));
        options.add(ConfigOptions.decimal("budgetStyleMax", "Budget Style Max",
                () -> budgetStyleMax, value -> budgetStyleMax = value));
        options.add(ConfigOptions.decimal("mobDropChance", "Mob Drop Chance",
                () -> mobDropChance, value -> mobDropChance = value)
                .describedAs("0 to 1. Page drop odds from entities/ tables."));
        options.add(ConfigOptions.decimal("chestLootChance", "Chest Loot Chance",
                () -> chestLootChance, value -> chestLootChance = value)
                .describedAs("0 to 1. Page drop odds from chests/ tables."));
        options.add(ConfigOptions.decimal("extraAffixChance", "Extra Affix Chance",
                () -> extraAffixChance, value -> extraAffixChance = value));
        options.add(ConfigOptions.decimal("extraAffixFalloff", "Extra Affix Falloff",
                () -> extraAffixFalloff, value -> extraAffixFalloff = value));
        options.add(ConfigOptions.decimal("affixChance", "Affix Chance",
                () -> affixChance, value -> affixChance = value));
        options.add(ConfigOptions.decimal("animationPreviewSpeed", "Animation Preview Speed",
                () -> animationPreviewSpeed, value -> animationPreviewSpeed = value));
        return options;
    }
}
