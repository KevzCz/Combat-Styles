package net.pixeldreamstudios.combat_styles.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

import net.pixeldreamstudios.combat_styles.loot.EquipmentDrops;
import net.pixeldreamstudios.combat_styles.loot.WeaponRolls;

@Mixin(LootTable.class)
public abstract class LootTableRollMixin {

    @Inject(
            method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN"),
            require = 1)
    private void combat_styles$rollGeneratedLoot(LootParams params,
                                                 CallbackInfoReturnable<ObjectArrayList<ItemStack>> callback) {
        combat_styles$roll(params, callback.getReturnValue());
    }

    @Inject(
            method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;J)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN"),
            require = 1)
    private void combat_styles$rollSeededLoot(LootParams params, long seed,
                                              CallbackInfoReturnable<ObjectArrayList<ItemStack>> callback) {
        combat_styles$roll(params, callback.getReturnValue());
    }

    private void combat_styles$roll(LootParams params, ObjectArrayList<ItemStack> items) {
        if (items == null) {
            return;
        }
        if (!items.isEmpty() && !WeaponRolls.isEmpty()) {
            for (ItemStack stack : items) {
                WeaponRolls.apply(stack, null, params.getLevel().getRandom());
            }
        }
        if (EquipmentDrops.isEmpty()) {
            return;
        }
        items.addAll(EquipmentDrops.roll(combat_styles$tableId(),
                params.getOptionalParameter(LootContextParams.THIS_ENTITY),
                params.getLevel(),
                params.getOptionalParameter(LootContextParams.ORIGIN),
                params.getLevel().getRandom()));
    }

    @Shadow
    @Final
    private Optional<ResourceLocation> randomSequence;

    private ResourceLocation combat_styles$tableId() {
        return randomSequence == null ? null : randomSequence.orElse(null);
    }
}
