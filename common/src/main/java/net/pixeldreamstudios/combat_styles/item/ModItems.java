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
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

import java.util.EnumMap;
import java.util.Map;

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

    private static final String BOOK_PREFIX = "style_book_";
    private static final int BOOK_STACK = 16;
    private static final Map<StyleRarity, RegistrySupplier<Item>> BOOKS =
            new EnumMap<>(StyleRarity.class);

    static {
        for (StyleRarity rarity : StyleRarity.values()) {
            BOOKS.put(rarity, ITEMS.register(BOOK_PREFIX + rarity.key(),
                    () -> new StyleBookItem(new Item.Properties()
                            .stacksTo(BOOK_STACK)
                            .rarity(rarity.itemRarity()), rarity)));
        }
    }

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("general", () ->
            CreativeTabRegistry.create(
                    Component.translatable("itemGroup.combat_styles.general"),
                    () -> new ItemStack(STYLE_PAGE.get())));

    private ModItems() {
    }

    public static Item bookFor(StyleRarity rarity) {
        RegistrySupplier<Item> book = BOOKS.get(rarity == null ? StyleRarity.COMMON : rarity);
        return book == null ? BOOKS.get(StyleRarity.COMMON).get() : book.get();
    }

    public static void register() {
        ITEMS.register();
        TABS.register();
        CreativeTabRegistry.modify(TAB, ModItems::fillTab);
    }

    private static void fillTab(FeatureFlagSet flags, CreativeTabOutput output,
                                boolean canUseGameMasterBlocks) {
        output.accept(new ItemStack(STYLE_EXTRACTOR.get()));
        for (StyleRarity rarity : StyleRarity.values()) {
            output.accept(new ItemStack(bookFor(rarity)));
        }
        for (StylePage page : StyleRegistry.all()) {
            output.accept(StylePageItem.of(page.styleId()));
        }
    }
}
