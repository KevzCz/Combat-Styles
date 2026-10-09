package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

import net.pixeldreamstudios.combat_styles.loot.EquipmentDrops;
import net.pixeldreamstudios.combat_styles.loot.WeaponRolls;

@Mixin(LootTable.class)
public abstract class LootTableRollMixin {

    private static final String RAW =
            "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V";

    @Shadow
    @Final
    private Optional<ResourceLocation> randomSequence;

    @ModifyVariable(method = RAW, at = @At("HEAD"), argsOnly = true, require = 1)
    private Consumer<ItemStack> combat_styles$rollWeapons(Consumer<ItemStack> output, LootContext context) {
        if (!combat_styles$outermost(context) || WeaponRolls.isEmpty()) {
            return output;
        }
        return stack -> {
            if (!EquipmentDrops.isAuthored(stack)) {
                WeaponRolls.apply(stack, null, context.getRandom());
            }
            output.accept(stack);
        };
    }

    @Inject(method = RAW, at = @At("RETURN"), require = 1)
    private void combat_styles$addEquipmentDrops(LootContext context, Consumer<ItemStack> output,
                                                 CallbackInfo callback) {
        if (!combat_styles$outermost(context)) {
            return;
        }
        for (EquipmentDrops.Drop drop : EquipmentDrops.roll(combat_styles$tableId(context),
                context.getParamOrNull(LootContextParams.THIS_ENTITY),
                context.getLevel(),
                context.getParamOrNull(LootContextParams.ORIGIN),
                context.getRandom())) {
            if (drop.roll()) {
                output.accept(drop.stack());
            } else {
                EquipmentDrops.deliverAuthored(drop.stack(), () -> output.accept(drop.stack()));
            }
        }
    }

    private static boolean combat_styles$outermost(LootContext context) {
        return context != null
                && ((LootContextAccessor) context).combat_styles$visitedElements().isEmpty();
    }

    private ResourceLocation combat_styles$tableId(LootContext context) {
        if (randomSequence != null && randomSequence.isPresent()) {
            return randomSequence.get();
        }
        MinecraftServer server = context.getLevel().getServer();
        return server == null ? null : server.reloadableRegistries().get()
                .registryOrThrow(Registries.LOOT_TABLE)
                .getKey((LootTable) (Object) this);
    }
}
