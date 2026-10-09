package net.pixeldreamstudios.combat_styles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pixeldreamstudios.combat_styles.affix.CombatEffectDealer;
import net.pixeldreamstudios.combat_styles.affix.AffixTrails;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.attribute.ModAttributes;
import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.config.OptionSources;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.config.SpellCompatState;
import net.pixeldreamstudios.combat_styles.config.StatusEffectConfig;
import net.pixeldreamstudios.combat_styles.config.SummonTuning;
import net.pixeldreamstudios.combat_styles.custom.CustomChoices;
import net.pixeldreamstudios.combat_styles.entity.AbilityChoices;
import net.pixeldreamstudios.combat_styles.entity.ModEntities;
import net.pixeldreamstudios.combat_styles.entity.RaiseDead;
import net.pixeldreamstudios.combat_styles.equipment.EnchantmentChoices;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentProviders;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.equipment.MissTracker;
import net.pixeldreamstudios.combat_styles.item.ModItems;
import net.pixeldreamstudios.combat_styles.item.EquipmentItem;
import net.pixeldreamstudios.combat_styles.loot.RandomStyleFunction;

public final class CombatStyles {

    public static final String MOD_ID = "combat_styles";
    public static final String BETTER_COMBAT_ID = "bettercombat";
    public static final String PLAYER_ANIMATOR_ID = "playeranimator";
    public static final Logger LOGGER = LoggerFactory.getLogger("Combat Styles");
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private CombatStyles() {
    }

    private static void registerEquipment() {
        EquipmentProviders.register(stack ->
                stack.getItem() instanceof EquipmentItem equipment ? equipment.definition() : null);
        EquipmentProviders.register(stack -> {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId == null) {
                return null;
            }
            EquipmentDefinition definition = EquipmentRegistry.get(itemId.toString());
            return definition == null || definition.broadlyMatched() ? null : definition;
        });
        EquipmentProviders.register(EquipmentRegistry::matching);
    }

    public static void init() {
        CombatStylesConfig.load();
        SpellCompatState.load();
        StatusEffectConfig.load();
        AbilityConfig.load();
        OptionSources.register(new AbilityChoices());
        OptionSources.register(new CustomChoices());
        OptionSources.register(new EnchantmentChoices());
        RaiseDead.register();
        SummonTuning.load();
        ModComponents.register();
        ModAttributes.register();
        ModItems.register();
        registerEquipment();
        ModEntities.register();
        RandomStyleFunction.register();
        ModEvents.register();
        AffixTrails.register();
        MissTracker.setHandler(CombatEffectDealer::applyMissEquipment);
        SpellEngineCompat.init();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isPlayerAnimatorLoaded() {
        return Platform.isModLoaded(PLAYER_ANIMATOR_ID);
    }
}
