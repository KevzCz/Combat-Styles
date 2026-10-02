package net.pixeldreamstudios.combat_styles.platform;

import net.bettercombat.api.AttributesContainer;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.mixin.WeaponRegistryAccessor;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WeaponRegistryAccess {

    private static final ThreadLocal<Integer> BYPASS = ThreadLocal.withInitial(() -> 0);

    private WeaponRegistryAccess() {
    }

    public static Map<ResourceLocation, WeaponAttributes> registrations() {
        try {
            Map<ResourceLocation, WeaponAttributes> registrations =
                    WeaponRegistryAccessor.combat_styles$registrations();
            return registrations == null ? Map.of() : registrations;
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.error("Unable to read the BetterCombat weapon registry", throwable);
            return Map.of();
        }
    }

    public static Map<ResourceLocation, WeaponAttributes> indexable() {
        Map<ResourceLocation, WeaponAttributes> indexable =
                new LinkedHashMap<>(registrations());
        for (Map.Entry<ResourceLocation, AttributesContainer> entry : containers().entrySet()) {
            WeaponAttributes attributes = entry.getValue() == null
                    ? null
                    : entry.getValue().attributes();
            if (attributes != null) {
                indexable.putIfAbsent(entry.getKey(), attributes);
            }
        }
        CombatStyles.LOGGER.info("Indexable sources: {} registrations + {} containers = {} entries",
                registrations().size(), containers().size(), indexable.size());
        return indexable;
    }

    public static Map<ResourceLocation, AttributesContainer> containers() {
        try {
            Map<ResourceLocation, AttributesContainer> containers =
                    WeaponRegistryAccessor.combat_styles$containers();
            return containers == null ? Map.of() : containers;
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.error("Unable to read the BetterCombat container registry", throwable);
            return Map.of();
        }
    }

    public static boolean bypassSequence() {
        return BYPASS.get() > 0;
    }

    public static void pushBypass() {
        BYPASS.set(BYPASS.get() + 1);
    }

    public static void popBypass() {
        int depth = BYPASS.get() - 1;
        if (depth <= 0) {
            BYPASS.remove();
        } else {
            BYPASS.set(depth);
        }
    }

    public static WeaponAttributes rawAttributesFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        pushBypass();
        try {
            return WeaponRegistry.getAttributes(stack);
        } finally {
            popBypass();
        }
    }
}
