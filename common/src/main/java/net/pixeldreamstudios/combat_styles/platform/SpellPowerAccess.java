package net.pixeldreamstudios.combat_styles.platform;

import dev.architectury.platform.Platform;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class SpellPowerAccess {

    public static final String MOD_ID = "spell_power";
    private static final String PREFIX = MOD_ID + ":";

    private SpellPowerAccess() {
    }

    public static boolean isLoaded() {
        return Platform.isModLoaded(MOD_ID);
    }

    public static double valueOf(Player player, String source) {
        if (player == null || source == null || source.isEmpty()) {
            return 0.0D;
        }
        if (source.startsWith(PREFIX)) {
            return spellPower(player, source.substring(PREFIX.length()));
        }
        ResourceLocation id = ResourceLocation.tryParse(source.contains(":") ? source : PREFIX + source);
        if (id != null && !BuiltInRegistries.ATTRIBUTE.containsKey(id) && isLoaded()) {
            Double power = SpellSchoolLookup.powerOf(player, source, PREFIX);
            if (power != null) {
                return power;
            }
        }
        return attributeValue(player, id);
    }

    public static List<String> schoolIds() {
        return isLoaded() ? SpellSchoolLookup.ids() : List.of();
    }

    private static double attributeValue(Player player, ResourceLocation id) {
        if (id == null) {
            return 0.0D;
        }
        Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(id).orElse(null);
        if (attribute == null || player.getAttribute(attribute) == null) {
            return 0.0D;
        }
        return player.getAttributeValue(attribute);
    }

    public static double valueOf(Player player, List<String> sources, boolean strongest) {
        if (player == null || sources == null || sources.isEmpty()) {
            return 0.0D;
        }
        double total = 0.0D;
        for (String source : sources) {
            double value = valueOf(player, source);
            total = strongest ? Math.max(total, value) : total + value;
        }
        return total;
    }

    public static String strongestOf(Player player, List<String> sources) {
        String best = null;
        double bestValue = 0.0D;
        for (String source : sources) {
            double value = valueOf(player, source);
            if (value > bestValue) {
                bestValue = value;
                best = source;
            }
        }
        return best;
    }

    public static ResourceLocation damageTypeOf(String school) {
        if (school == null || school.isEmpty()) {
            return null;
        }
        if (isLoaded()) {
            ResourceLocation owned = SpellSchoolLookup.damageTypeOf(school, PREFIX);
            if (owned != null) {
                return owned;
            }
        }
        return ResourceLocation.tryParse(school.contains(":") ? school : PREFIX + school);
    }

    public static double spellPower(Player player, String school) {
        return attributeValue(player, ResourceLocation.tryParse(PREFIX + school));
    }
}
