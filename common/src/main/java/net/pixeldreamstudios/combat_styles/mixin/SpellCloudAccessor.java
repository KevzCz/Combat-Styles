package net.pixeldreamstudios.combat_styles.mixin;

import net.spell_engine.internals.SpellExecution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.spell_engine.entity.SpellCloud", remap = false)
public interface SpellCloudAccessor {

    @Accessor(value = "context", remap = false)
    SpellExecution.ImpactContext combat_styles$context();
}
