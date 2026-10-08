package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;
import java.util.Optional;

public final class EnchantLevels {

    public static final String SOURCE_PREFIX = "enchantment";
    public static final char SLOT_MARK = '@';
    public static final char SEPARATOR = ':';
    public static final String SLOT_MAIN_HAND = "mainhand";
    public static final String SLOT_OFF_HAND = "offhand";
    public static final String SLOT_HIGHEST = "highest";
    public static final String SLOT_SELF = "self";
    private static final int NONE = 0;

    public record Range(Optional<Integer> min, Optional<Integer> max) {

        public static final Range ANY = new Range(Optional.empty(), Optional.empty());

        public static final Codec<Range> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("min").forGetter(Range::min),
                Codec.INT.optionalFieldOf("max").forGetter(Range::max)
        ).apply(instance, Range::new));

        public Range {
            min = min == null ? Optional.empty() : min;
            max = max == null ? Optional.empty() : max;
        }

        public boolean accepts(int level) {
            return level >= min.orElse(1) && max.map(bound -> level <= bound).orElse(true);
        }
    }

    private EnchantLevels() {
    }

    public static int levelOf(ItemStack stack, String enchantment) {
        if (stack == null || stack.isEmpty() || enchantment == null || enchantment.isBlank()) {
            return NONE;
        }
        ResourceLocation wanted = ResourceLocation.tryParse(enchantment.trim());
        if (wanted == null) {
            return NONE;
        }
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(wanted)) {
                return entry.getIntValue();
            }
        }
        return NONE;
    }

    public static boolean matches(ItemStack stack, Map<String, Range> wanted, boolean any) {
        if (wanted.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Range> entry : wanted.entrySet()) {
            boolean met = entry.getValue().accepts(levelOf(stack, entry.getKey()));
            if (any && met) {
                return true;
            }
            if (!any && !met) {
                return false;
            }
        }
        return !any;
    }

    public static boolean isSource(String source) {
        return source != null && source.startsWith(SOURCE_PREFIX)
                && source.length() > SOURCE_PREFIX.length()
                && (source.charAt(SOURCE_PREFIX.length()) == SEPARATOR
                        || source.charAt(SOURCE_PREFIX.length()) == SLOT_MARK);
    }

    public static String enchantmentOf(String source) {
        if (!isSource(source)) {
            return null;
        }
        String rest = source.substring(SOURCE_PREFIX.length());
        if (rest.charAt(0) == SLOT_MARK) {
            int split = rest.indexOf(SEPARATOR);
            return split < 0 ? null : rest.substring(split + 1);
        }
        return rest.substring(1);
    }

    public static int sourceLevel(Player player, String source) {
        if (player == null || !isSource(source)) {
            return NONE;
        }
        String rest = source.substring(SOURCE_PREFIX.length());
        String slot = SLOT_MAIN_HAND;
        if (rest.charAt(0) == SLOT_MARK) {
            int split = rest.indexOf(SEPARATOR);
            if (split < 0) {
                return NONE;
            }
            slot = rest.substring(1, split);
            rest = rest.substring(split);
        }
        String enchantment = rest.substring(1);
        return switch (slot) {
            case SLOT_OFF_HAND -> levelOf(player.getOffhandItem(), enchantment);
            case SLOT_HIGHEST -> highest(player, enchantment);
            case SLOT_SELF -> levelOf(OwnerScope.stack(), enchantment);
            default -> levelOf(player.getMainHandItem(), enchantment);
        };
    }

    private static int highest(Player player, String enchantment) {
        int best = NONE;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            best = Math.max(best, levelOf(player.getItemBySlot(slot), enchantment));
        }
        return best;
    }
}
