package net.pixeldreamstudios.combat_styles.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.item.StylePageItem;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.util.List;

public class RandomStyleFunction extends LootItemConditionalFunction {

    public static final DeferredRegister<LootItemFunctionType<?>> FUNCTIONS =
            DeferredRegister.create(CombatStyles.MOD_ID, Registries.LOOT_FUNCTION_TYPE);

    public static final MapCodec<RandomStyleFunction> CODEC =
            RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
                    .apply(instance, RandomStyleFunction::new));

    public static final RegistrySupplier<LootItemFunctionType<RandomStyleFunction>> TYPE =
            FUNCTIONS.register("random_style", () -> new LootItemFunctionType<>(CODEC));

    protected RandomStyleFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    public static void register() {
        FUNCTIONS.register();
    }

    public static LootItemConditionalFunction.Builder<?> builder() {
        return simpleBuilder(RandomStyleFunction::new);
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return TYPE.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (!BookRolls.isEmpty()) {
            StyleRarity rarity = StyleLoot.weightedRarity(context.getRandom());
            ItemStack book = BookRolls.create(rarity, context.getRandom());
            if (!BookProvider.contentsOf(book).isEmpty()) {
                return book;
            }
        }

        StylePage page = StyleLoot.weightedPick(context.getRandom());
        if (page == null) {
            return stack;
        }
        return StylePageItem.of(page.styleId(),
                StyleLoot.rollAffixes(page, context.getRandom()));
    }
}
