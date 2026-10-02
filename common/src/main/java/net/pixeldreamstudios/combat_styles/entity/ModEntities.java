package net.pixeldreamstudios.combat_styles.entity;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.pixeldreamstudios.combat_styles.CombatStyles;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(CombatStyles.MOD_ID, Registries.ENTITY_TYPE);

    private static final float WIDTH = 2.0F;
    private static final float HEIGHT = 1.0F;
    private static final int TRACKING_RANGE = 6;
    private static final int UPDATE_INTERVAL = 1;

    public static final RegistrySupplier<EntityType<SlashWaveEntity>> SLASH_WAVE =
            ENTITIES.register("slash_wave", () -> EntityType.Builder
                    .<SlashWaveEntity>of(SlashWaveEntity::new, MobCategory.MISC)
                    .sized(WIDTH, HEIGHT)
                    .clientTrackingRange(TRACKING_RANGE)
                    .updateInterval(UPDATE_INTERVAL)
                    .build(CombatStyles.MOD_ID + ":slash_wave"));

    private ModEntities() {
    }

    public static void register() {
        ENTITIES.register();
    }
}
