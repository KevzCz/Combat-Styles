package net.pixeldreamstudios.combat_styles.client;

import dev.architectury.platform.Mod;
import dev.architectury.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.config.OptionSources;
import net.pixeldreamstudios.combat_styles.equipment.EnchantLevels;
import net.pixeldreamstudios.combat_styles.equipment.EnchantmentChoices;
import net.pixeldreamstudios.combat_styles.equipment.UseScope;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class ScalingSources {

    public record Category(Component name, List<String> sources) {
    }

    private static final String VANILLA = ResourceLocation.DEFAULT_NAMESPACE;
    private static final String KEY_VANILLA = "screen.combat_styles.source_category.vanilla";
    private static final String KEY_SPELL_POWER = "screen.combat_styles.source_category.spell_power";
    private static final String KEY_ENCHANTMENTS = "screen.combat_styles.source_category.enchantments";
    private static final String KEY_USE = "screen.combat_styles.source_category.use";
    private static final String KEY_MOD = "screen.combat_styles.source_category.mod";
    private static final List<String> USE_SOURCES = List.of(UseScope.CHARGE, UseScope.HELD_SECONDS,
            UseScope.HELD_TICKS);

    private ScalingSources() {
    }

    public static List<Category> categories() {
        List<Category> categories = new ArrayList<>();
        Set<String> schools = new LinkedHashSet<>(SpellPowerAccess.schoolIds());
        List<String> vanilla = new ArrayList<>();
        Map<String, List<String>> modded = new TreeMap<>();
        for (String attribute : EffectChoices.attributeIds()) {
            if (schools.contains(attribute)) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(attribute);
            String namespace = id == null ? VANILLA : id.getNamespace();
            if (VANILLA.equals(namespace)) {
                vanilla.add(attribute);
            } else {
                modded.computeIfAbsent(namespace, ignored -> new ArrayList<>()).add(attribute);
            }
        }
        add(categories, Component.translatable(KEY_VANILLA), vanilla);
        add(categories, Component.translatable(KEY_SPELL_POWER), List.copyOf(schools));
        List<String> enchantments = new ArrayList<>();
        for (String enchantment : OptionSources.values(EnchantmentChoices.SELECTOR)) {
            enchantments.add(EnchantLevels.SOURCE_PREFIX + EnchantLevels.SEPARATOR + enchantment);
        }
        add(categories, Component.translatable(KEY_ENCHANTMENTS), enchantments);
        add(categories, Component.translatable(KEY_USE), USE_SOURCES);
        for (Map.Entry<String, List<String>> mod : modded.entrySet()) {
            add(categories, Component.translatable(KEY_MOD, modName(mod.getKey())), mod.getValue());
        }
        return categories;
    }

    public static List<String> ids() {
        Map<String, Boolean> ids = new LinkedHashMap<>();
        for (Category category : categories()) {
            for (String source : category.sources()) {
                ids.put(source, Boolean.TRUE);
            }
        }
        return List.copyOf(ids.keySet());
    }

    private static void add(List<Category> categories, Component name, List<String> sources) {
        if (!sources.isEmpty()) {
            categories.add(new Category(name, List.copyOf(sources)));
        }
    }

    private static String modName(String namespace) {
        return Platform.getOptionalMod(namespace).map(Mod::getName)
                .orElseGet(() -> StyleText.prettify(namespace));
    }
}
