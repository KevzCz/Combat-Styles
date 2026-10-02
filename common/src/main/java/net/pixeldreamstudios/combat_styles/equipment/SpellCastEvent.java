package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public record SpellCastEvent(ResourceLocation spell, String school, String castType,
                             CastPhase phase, float progress, LivingEntity target,
                             boolean triggered) {

    public SpellCastEvent {
        school = school == null ? "" : school;
        castType = castType == null ? "" : castType;
        phase = phase == null ? CastPhase.CAST : phase;
    }
}
