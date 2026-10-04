package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.AttributeBonus;
import net.pixeldreamstudios.combat_styles.equipment.StackAttributes;

import java.util.Map;

public record SummonStats(Map<String, AttributeBonus> bonuses) {

    private static final String MODIFIER_PREFIX = "summon/";
    private static final String ID_SEPARATOR = "_";

    public static final SummonStats NONE = new SummonStats(Map.of());

    public static final Codec<SummonStats> CODEC = Codec.unboundedMap(Codec.STRING, AttributeBonus.CODEC)
            .xmap(SummonStats::new, SummonStats::bonuses);

    public SummonStats {
        bonuses = bonuses == null ? Map.of() : Map.copyOf(bonuses);
    }

    public boolean isEmpty() {
        return bonuses.isEmpty();
    }

    public void applyTo(LivingEntity entity) {
        if (bonuses.isEmpty()) {
            return;
        }
        for (Map.Entry<String, AttributeBonus> entry : bonuses.entrySet()) {
            Holder<Attribute> attribute = StackAttributes.holderOf(entry.getKey());
            AttributeInstance instance = attribute == null ? null : entity.getAttribute(attribute);
            ResourceLocation key = ResourceLocation.tryParse(entry.getKey());
            if (instance == null || key == null) {
                continue;
            }
            ResourceLocation modifierId = CombatStyles.id(MODIFIER_PREFIX + key.getNamespace()
                    + ID_SEPARATOR + key.getPath());
            instance.removeModifier(modifierId);
            instance.addPermanentModifier(new AttributeModifier(modifierId,
                    entry.getValue().amount(), entry.getValue().operation()));
        }
        entity.setHealth(entity.getMaxHealth());
    }
}
