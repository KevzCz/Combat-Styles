package net.pixeldreamstudios.combat_styles.item;

import dev.architectury.registry.CreativeTabOutput;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(CombatStyles.MOD_ID, Registries.ITEM);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(CombatStyles.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<Item> STYLE_PAGE =
            ITEMS.register("style_page", () -> new StylePageItem(new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> STYLE_EXTRACTOR =
            ITEMS.register("style_extractor",
                    () -> new StyleExtractorItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("general", () ->
            CreativeTabRegistry.create(
                    Component.translatable("itemGroup.combat_styles.general"),
                    () -> new ItemStack(STYLE_PAGE.get())));

    private ModItems() {
    }

    public static void register() {
        ITEMS.register();
        TABS.register();
        CreativeTabRegistry.modify(TAB, ModItems::fillTab);
    }

    private static void fillTab(FeatureFlagSet flags, CreativeTabOutput output,
                                boolean canUseGameMasterBlocks) {
        output.accept(new ItemStack(STYLE_EXTRACTOR.get()));
        for (StylePage page : StyleRegistry.all()) {
            output.accept(StylePageItem.of(page.styleId()));
        }
    }
}
