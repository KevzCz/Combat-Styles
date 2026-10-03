package net.pixeldreamstudios.combat_styles.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.loot.BookProvider;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;

import java.util.List;

public final class StyleBooks implements BookProvider.Books {

    @Override
    public Item bookFor(StyleRarity rarity) {
        return ModItems.bookFor(rarity);
    }

    @Override
    public boolean isBook(Item item) {
        return item instanceof StyleBookItem;
    }

    @Override
    public void setContents(ItemStack book, List<StyleEntry> entries) {
        StyleBookItem.setContents(book, entries);
    }

    @Override
    public StyleSequence contentsOf(ItemStack book) {
        return StyleBookItem.contentsOf(book);
    }
}
