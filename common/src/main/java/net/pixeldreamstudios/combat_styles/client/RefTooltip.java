package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import net.pixeldreamstudios.combat_styles.item.TooltipGem;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.affix.AffixText;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.CustomStyle;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleText;
import net.pixeldreamstudios.combat_styles.style.StyleTooltip;

import java.util.ArrayList;
import java.util.List;

public final class RefTooltip {

    private static final String KEY_UNKNOWN = "screen.combat_styles.ref_unknown";
    private static final String KEY_TUNED = "screen.combat_styles.ref_tuned";
    private static final String KEY_TUNED_BY = "screen.combat_styles.ref_tuned_by";
    private static final String KEY_ENTITY = "screen.combat_styles.ref_entity";

    private RefTooltip() {
    }

    public static List<Component> forChoice(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        if (AffixRegistry.get(id) != null) {
            return forAffix(id, null);
        }
        if (StyleRegistry.get(id) != null) {
            return forStyle(id);
        }
        return null;
    }

    public static List<Component> forStyle(String styleId) {
        CustomStyle custom = styleId == null ? null : CustomStyles.get(styleId);
        if (custom != null) {
            return forCustomStyle(custom);
        }
        List<Component> lines = new ArrayList<>();
        StylePage page = styleId == null ? null : StyleRegistry.get(styleId);
        if (page == null) {
            return unknown(styleId);
        }
        lines.add(StyleText.name(page));
        StyleTooltip.appendStatGrid(page, lines);
        StyleTooltip.appendDetails(page, lines);
        AttackOverride override = AttackOverrides.get(page.styleId());
        if (override != null) {
            StyleTooltip.appendTiming(override, lines);
            StyleTooltip.appendAttributes(override, lines);
        }
        return lines;
    }

    private static List<Component> forCustomStyle(CustomStyle custom) {
        StyleEntry entry = custom.entry();
        StylePage page = StyleRegistry.get(entry.styleId());
        if (page == null) {
            return unknown(custom.id());
        }
        List<Component> lines = new ArrayList<>();
        lines.add(StyleText.name(entry, page));
        StyleTooltip.appendStatGrid(page, lines);
        StyleTooltip.appendDetails(page, lines);
        entry.attackOverride().ifPresent(override -> {
            StyleTooltip.appendTiming(override, lines);
            StyleTooltip.appendAttributes(override, lines);
        });
        for (String affix : entry.affixIds()) {
            lines.addAll(forAffix(affix, entry.affixTuning().get(affix),
                    StyleText.name(entry, page).getString()));
        }
        return lines;
    }

    public static List<Component> forAffix(String affixId, AffixTuning tuning) {
        return forAffix(affixId, tuning, null);
    }

    public static List<Component> forAffix(String affixId, AffixTuning tuning, String source) {
        StyleAffix affix = affixId == null ? null : AffixRegistry.get(affixId);
        if (affix == null) {
            return unknown(affixId);
        }
        List<Component> lines = new ArrayList<>();
        if (tuning == null || tuning.isEmpty()) {
            lines.add(AffixText.name(affix));
            AffixText.appendEffects(affix, lines);
            return TooltipGem.indentDetails(lines);
        }
        StyleAffix tuned = tuning.applyTo(affix);
        lines.add(AffixText.name(tuned).copy()
                .append(Component.literal(" "))
                .append(Component.translatable(source == null ? KEY_TUNED : KEY_TUNED_BY, source)
                        .withStyle(ChatFormatting.DARK_AQUA)));
        AffixText.appendTuned(affix, tuned, lines, true);
        return TooltipGem.indentDetails(lines);
    }

    public static List<Component> forSpell(String spellId) {
        ResourceLocation id = spellId == null ? null : ResourceLocation.tryParse(spellId);
        Minecraft client = Minecraft.getInstance();
        if (id == null || client == null || client.level == null
                || !SpellEngineCompat.isLoaded()) {
            return null;
        }
        List<String> described = SpellEngineCompat.describeSpell(id, client.level);
        if (described.isEmpty()) {
            return unknown(spellId);
        }
        List<Component> lines = new ArrayList<>();
        lines.add(StyleText.spellName(spellId).copy().withStyle(ChatFormatting.GOLD));
        for (String line : described) {
            lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    public static List<Component> forEntity(String entityId) {
        ResourceLocation id = entityId == null ? null : ResourceLocation.tryParse(entityId);
        EntityType<?> type = id == null
                ? null
                : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) {
            return unknown(entityId);
        }
        List<Component> lines = new ArrayList<>();
        lines.add(type.getDescription().copy().withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable(KEY_ENTITY, entityId)
                .withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    public static List<Component> unknown(String id) {
        return List.of(Component.translatable(KEY_UNKNOWN,
                id == null || id.isBlank() ? "-" : id).withStyle(ChatFormatting.RED));
    }
}
