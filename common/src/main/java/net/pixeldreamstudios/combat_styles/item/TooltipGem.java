package net.pixeldreamstudios.combat_styles.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum TooltipGem {
    SAPPHIRE(0xE000),
    AMBER(0xE001),
    AMETHYST(0xE002),
    EMERALD(0xE003),
    AQUAMARINE(0xE004),
    ROSE(0xE005),
    SILVER(0xE006),
    SLATE(0xE007),
    HOLLOW(0xE008),
    SHADOW(0xE009);

    public static final TooltipGem STAT = EMERALD;
    public static final TooltipGem MOVESET = AMBER;
    public static final TooltipGem SPELL = AMETHYST;
    public static final TooltipGem EFFECT = AQUAMARINE;
    public static final TooltipGem DAMAGE = ROSE;
    public static final TooltipGem SUMMON = SAPPHIRE;
    public static final TooltipGem NOTE = SLATE;
    public static final TooltipGem CUSTOM = SILVER;
    public static final TooltipGem MORE = HOLLOW;

    private static final ResourceLocation FONT =
            ResourceLocation.fromNamespaceAndPath(CombatStyles.MOD_ID, "icons");
    private static final int SPACER = 0xE010;
    private static final String GAP = " ";
    private static final String PLAIN_INDENT = "  ";
    private static final String LEAD = " ";
    private static final String DETAIL_MARK = CombatStyles.MOD_ID + ":detail";
    private static final String HEADER_MARK = CombatStyles.MOD_ID + ":header";

    private final int glyph;

    TooltipGem(int glyph) {
        this.glyph = glyph;
    }

    public MutableComponent icon() {
        return Component.literal(Character.toString(glyph))
                .withStyle(style -> style.withFont(FONT).withColor(ChatFormatting.WHITE));
    }

    public MutableComponent bullet(Component line) {
        if (!CombatStylesConfig.get().tooltipGemBullets()) {
            return Component.literal(PLAIN_INDENT).append(line);
        }
        return Component.literal(LEAD).append(icon()).append(GAP).append(line);
    }

    public static MutableComponent hang(Component line) {
        if (!CombatStylesConfig.get().tooltipGemBullets()) {
            return Component.literal(PLAIN_INDENT).append(line);
        }
        return Component.literal(LEAD).append(spacer()).append(GAP).append(line);
    }

    public static MutableComponent detail(Component line) {
        return Component.empty().withStyle(style -> style.withInsertion(DETAIL_MARK)).append(line);
    }

    public static boolean isDetail(Component line) {
        return line != null && DETAIL_MARK.equals(line.getStyle().getInsertion());
    }

    public static MutableComponent header(Component line) {
        return Component.empty().withStyle(style -> style.withInsertion(HEADER_MARK)).append(line);
    }

    public static boolean isHeader(Component line) {
        return line != null && HEADER_MARK.equals(line.getStyle().getInsertion());
    }

    public MutableComponent place(Component line) {
        if (isHeader(line)) {
            return line.copy();
        }
        return isDetail(line) ? hang(bullet(line)) : bullet(line);
    }

    public static MutableComponent hangPlace(Component line) {
        if (isHeader(line)) {
            return hang(line);
        }
        return isDetail(line) ? hang(hang(line)) : hang(line);
    }

    public static List<Component> indentDetails(List<Component> lines) {
        List<Component> shown = new ArrayList<>(lines.size());
        for (Component line : lines) {
            shown.add(isDetail(line) ? hang(line) : line);
        }
        return shown;
    }

    private static MutableComponent spacer() {
        return Component.literal(Character.toString(SPACER)).withStyle(style -> style.withFont(FONT));
    }

    public static final String NO_ICON = "none";

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static List<String> iconNames() {
        List<String> names = new ArrayList<>();
        for (TooltipGem gem : values()) {
            names.add(gem.serializedName());
        }
        names.add(NO_ICON);
        return names;
    }

    public static MutableComponent iconLine(String icon, Component body) {
        for (TooltipGem gem : values()) {
            if (gem.serializedName().equals(icon)) {
                return gem.bullet(body);
            }
        }
        return hang(body);
    }

    public static TooltipGem of(StyleRarity rarity) {
        if (rarity == null) {
            return SILVER;
        }
        return switch (rarity) {
            case COMMON -> SILVER;
            case UNCOMMON -> EMERALD;
            case RARE -> AQUAMARINE;
            case EPIC -> AMETHYST;
            case LEGENDARY -> AMBER;
            case MYTHIC -> ROSE;
        };
    }
}
