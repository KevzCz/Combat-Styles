package net.pixeldreamstudios.combat_styles.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

public class WeaponRollFunction extends LootItemConditionalFunction {

    public static final MapCodec<WeaponRollFunction> CODEC =
            RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
                    .apply(instance, WeaponRollFunction::new));

    public static final RegistrySupplier<LootItemFunctionType<WeaponRollFunction>> TYPE =
            RandomStyleFunction.FUNCTIONS.register("weapon_roll",
                    () -> new LootItemFunctionType<>(CODEC));

    protected WeaponRollFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    public static LootItemConditionalFunction.Builder<?> builder() {
        return simpleBuilder(WeaponRollFunction::new);
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return TYPE.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        WeaponRolls.apply(stack, null, context.getRandom());
        return stack;
    }
}
