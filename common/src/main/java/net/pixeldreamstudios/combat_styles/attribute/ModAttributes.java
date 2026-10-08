package net.pixeldreamstudios.combat_styles.attribute;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public final class ModAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(CombatStyles.MOD_ID, Registries.ATTRIBUTE);

    public static final String BUDGET_MULTIPLIER_ID = "attack_budget_multiplier";
    public static final String BUDGET_FLAT_ID = "attack_budget_flat";
    public static final double MULTIPLIER_DEFAULT = 100.0D;
    public static final double FLAT_DEFAULT = 0.0D;
    public static final String MELEE_KNOCKBACK_ID = "melee_knockback";
    public static final double MELEE_KNOCKBACK_DEFAULT = 1.0D;
    private static final double MELEE_KNOCKBACK_MAX = 10.0D;

    public static final RegistrySupplier<Attribute> BUDGET_MULTIPLIER =
            ATTRIBUTES.register(BUDGET_MULTIPLIER_ID, () -> new RangedAttribute(
                    "attribute.name." + CombatStyles.MOD_ID + "." + BUDGET_MULTIPLIER_ID,
                    MULTIPLIER_DEFAULT, 0.0D, 10000.0D).setSyncable(true));

    public static final RegistrySupplier<Attribute> BUDGET_FLAT =
            ATTRIBUTES.register(BUDGET_FLAT_ID, () -> new RangedAttribute(
                    "attribute.name." + CombatStyles.MOD_ID + "." + BUDGET_FLAT_ID,
                    FLAT_DEFAULT, -1024.0D, 1024.0D).setSyncable(true));

    public static final RegistrySupplier<Attribute> MELEE_KNOCKBACK =
            ATTRIBUTES.register(MELEE_KNOCKBACK_ID, () -> new RangedAttribute(
                    "attribute.name." + CombatStyles.MOD_ID + "." + MELEE_KNOCKBACK_ID,
                    MELEE_KNOCKBACK_DEFAULT, 0.0D, MELEE_KNOCKBACK_MAX).setSyncable(true));

    private ModAttributes() {
    }

    public static void register() {
        ATTRIBUTES.register();
    }

    public static void appendTo(AttributeSupplier.Builder builder) {
        if (builder == null) {
            return;
        }
        addTo(builder, BUDGET_MULTIPLIER_ID, MULTIPLIER_DEFAULT);
        addTo(builder, BUDGET_FLAT_ID, FLAT_DEFAULT);
        addTo(builder, MELEE_KNOCKBACK_ID, MELEE_KNOCKBACK_DEFAULT);
    }

    private static void addTo(AttributeSupplier.Builder builder, String path, double base) {
        BuiltInRegistries.ATTRIBUTE
                .getHolder(ResourceKey.create(Registries.ATTRIBUTE, CombatStyles.id(path)))
                .ifPresent(holder -> builder.add(holder, base));
    }
}
