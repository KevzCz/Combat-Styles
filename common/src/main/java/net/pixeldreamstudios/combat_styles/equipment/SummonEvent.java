package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public record SummonEvent(Kind kind, Entity summon, LivingEntity other) {

    public enum Kind {
        SPAWN,
        DEATH,
        KILL
    }

    public String summonId() {
        if (summon == null) {
            return "";
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(summon.getType());
        return id == null ? "" : id.toString();
    }
}
