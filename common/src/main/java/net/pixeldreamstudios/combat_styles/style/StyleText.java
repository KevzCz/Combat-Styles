package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.AffixText;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

public final class StyleText {
    private static final ChatFormatting NEUTRAL = ChatFormatting.GRAY;
    private static final ChatFormatting GOOD = ChatFormatting.GREEN;
    private static final ChatFormatting BAD = ChatFormatting.RED;
    private static final String SPELL_NAME_KEY = "spell.%s.%s.name";

    private StyleText() {
    }

    public static Component spellName(String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            return Component.literal(prettify(raw));
        }
        return Component.translatableWithFallback(
                String.format(Locale.ROOT, SPELL_NAME_KEY, id.getNamespace(), id.getPath()),
                prettify(id.getPath()));
    }

    public static Component registryName(String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            return Component.literal(prettify(raw));
        }
        Component named = fromRegistries(id);
        return named == null ? Component.literal(prettify(id.getPath())) : named;
    }

    public static Component choiceName(String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        String trimmed = raw.trim();
        StylePage page = StyleRegistry.get(trimmed);
        if (page != null) {
            return name(page);
        }
        StyleAffix affix = AffixRegistry.get(trimmed);
        if (affix != null) {
            return AffixText.name(affix);
        }
        return registryName(trimmed);
    }

    private static Component fromRegistries(ResourceLocation id) {
        var attribute = BuiltInRegistries.ATTRIBUTE.getOptional(id).orElse(null);
        if (attribute != null) {
            return Component.translatable(attribute.getDescriptionId());
        }
        var effect = BuiltInRegistries.MOB_EFFECT.get(id);
        if (effect != null) {
            return effect.getDisplayName();
        }
        var entity = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (entity != null) {
            return Component.translatable(entity.getDescriptionId());
        }
        var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        return item == null ? null : Component.translatable(item.getDescriptionId());
    }

    public static Component name(StylePage page) {
        CustomStyle custom = CustomStyles.get(page.styleId());
        if (custom != null) {
            StyleEntry entry = custom.entry();
            if (entry.name().isPresent()) {
                return Component.literal(entry.name().get())
                        .withStyle(page.rarity().color());
            }
            if (entry.nameKey().isPresent()) {
                return Component.translatable(entry.nameKey().get())
                        .withStyle(page.rarity().color());
            }
        }
        String animationKey = animationKeyOf(page);
        if (animationKey != null) {
            return Component.translatableWithFallback(animationKey,
                            Component.translatableWithFallback(page.translationKey(),
                                    fallbackName(page)).getString())
                    .withStyle(page.rarity().color());
        }
        return Component.translatableWithFallback(page.translationKey(), fallbackName(page))
                .withStyle(page.rarity().color());
    }

    public static Component affixedName(StyleEntry entry, StylePage page) {
        Component base = name(entry, page);
        if (entry == null || entry.name().isPresent() || entry.nameKey().isPresent()) {
            return base;
        }
        List<StyleAffix> affixes = entry.resolveAffixes().affixes();
        if (affixes.isEmpty()) {
            return base;
        }
        MutableComponent named = Component.empty();
        for (StyleAffix affix : affixes) {
            named.append(AffixText.name(affix).copy().withStyle(affix.rarity().color()))
                    .append(Component.literal(" "));
        }
        return named.append(base);
    }

    public static Component name(StyleEntry entry, StylePage page) {
        if (entry != null && entry.name().isPresent()) {
            return Component.literal(entry.name().get())
                    .withStyle(page == null ? NEUTRAL : page.rarity().color());
        }
        if (entry != null && entry.nameKey().isPresent()) {
            return Component.translatable(entry.nameKey().get())
                    .withStyle(page == null ? NEUTRAL : page.rarity().color());
        }
        return page == null ? Component.empty() : name(page);
    }

    private static String animationKeyOf(StylePage page) {
        String animation = page.animation();
        if (animation == null || !animation.contains(":")) {
            return null;
        }
        return StyleIds.STYLE_NAME_PREFIX + animation.replace(':', '.');
    }

    public static Component categoryName(String category) {
        return Component.translatableWithFallback(StyleIds.CATEGORY_PREFIX + category, prettify(category));
    }

    public static Component schoolName(String school) {
        if (school == null || school.isBlank()) {
            return Component.translatableWithFallback(
                    StyleIds.SCHOOL_PREFIX + "physical", "Physical");
        }
        String key = school.contains(":")
                ? school.substring(school.indexOf(0x3A) + 1)
                : school;
        key = key.toLowerCase(Locale.ROOT);
        return Component.translatableWithFallback(StyleIds.SCHOOL_PREFIX + key, prettify(key));
    }

    public static Component scalingSourceName(String source) {
        if (source == null || source.isBlank()) {
            return Component.empty();
        }
        String qualified = source.contains(":")
                ? source
                : SpellPowerAccess.MOD_ID + ":" + source;
        return registryName(qualified);
    }

    public static Component hitboxName(WeaponAttributes.HitBoxShape hitbox) {
        String key = hitbox == null ? "unknown" : hitbox.name().toLowerCase(Locale.ROOT);
        return Component.translatableWithFallback(StyleIds.HITBOX_PREFIX + key,
                hitbox == null ? "Unknown" : prettify(key));
    }

    public static Component rarityName(StyleRarity rarity) {
        return Component.translatableWithFallback(rarity.translationKey(), prettify(rarity.key()))
                .withStyle(rarity.color());
    }

    public static Component conditionName(WeaponAttributes.Condition condition) {
        if (condition == null) {
            return Component.empty();
        }
        String key = condition.name().toLowerCase(Locale.ROOT);
        return Component.translatableWithFallback(StyleIds.CONDITION_PREFIX + key, prettify(key));
    }

    public static MutableComponent multiplier(String labelKey, double value, boolean higherIsBetter) {
        ChatFormatting color = colorFor(value, higherIsBetter);
        return Component.translatable(labelKey)
                .withStyle(NEUTRAL)
                .append(Component.literal(" " + formatMultiplier(value)).withStyle(color));
    }

    public static MutableComponent raw(String labelKey, String value) {
        return Component.translatable(labelKey)
                .withStyle(NEUTRAL)
                .append(Component.literal(" " + value).withStyle(ChatFormatting.WHITE));
    }

    public static MutableComponent attributeLine(String attributeId, AttackAttribute value,
                                                 ChatFormatting label) {
        if (value.set()) {
            return Component.translatable(KEY_ATTRIBUTE_SET,
                            registryName(attributeId).copy().withStyle(label),
                            formatDecimal(value.amount()))
                    .withStyle(ChatFormatting.AQUA);
        }
        boolean percent = value.operation() != AttributeModifier.Operation.ADD_VALUE;
        double shown = percent ? value.amount() * 100.0D : value.amount();
        String text = (shown > 0.0D ? "+" : "") + formatDecimal(shown) + (percent ? "%" : "");
        return Component.empty()
                .append(registryName(attributeId).copy().withStyle(label))
                .append(Component.literal(" " + text).withStyle(
                        shown > 0.0D ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private static final String KEY_ATTRIBUTE_SET = "tooltip.combat_styles.attribute_set";

    public static String formatMultiplier(double value) {
        return String.format(Locale.ROOT, "%.2fx", value);
    }

    public static String formatCompact(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    public static String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static String formatDegrees(double value) {
        return String.format(Locale.ROOT, "%.0f°", value);
    }

    private static ChatFormatting colorFor(double value, boolean higherIsBetter) {
        if (Math.abs(value - 1.0D) < 0.001D) {
            return NEUTRAL;
        }
        boolean above = value > 1.0D;
        return above == higherIsBetter ? GOOD : BAD;
    }

    public static ChatFormatting upswingColor(double upswing, double baseline) {
        if (Math.abs(upswing - baseline) < 0.001D) {
            return NEUTRAL;
        }
        return upswing < baseline ? GOOD : BAD;
    }

    private static String fallbackName(StylePage page) {
        String animation = page.animation();
        if (animation == null || animation.isBlank()) {
            return prettify(page.category()) + " Attack " + (page.attackIndex() + 1);
        }
        return AnimationNames.readable(lastSegment(animation));
    }

    public static String soundName(String soundId) {
        if (soundId == null || soundId.isBlank()) {
            return "";
        }
        String path = lastSegment(soundId);
        path = path.replaceAll("_\\d+$", "").replaceAll("_+", "_").replaceAll("^_|_$", "");
        return prettify(path);
    }

    private static String lastSegment(String value) {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        String path = parsed == null ? value : parsed.getPath();
        int slash = path.lastIndexOf('/');
        return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : path;
    }

    public static String sentenceCase(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        char first = raw.charAt(0);
        if (Character.isUpperCase(first)) {
            return raw;
        }
        return Character.toUpperCase(first) + raw.substring(1);
    }

    public static String prettify(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        int colon = raw.lastIndexOf(':');
        String withoutNamespace = colon >= 0 && colon < raw.length() - 1
                ? raw.substring(colon + 1)
                : raw;
        String[] words = withoutNamespace.replace('.', '_').split("_");
        StringBuilder builder = new StringBuilder(raw.length());
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
    }
}
