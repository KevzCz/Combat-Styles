package net.pixeldreamstudios.combat_styles.loot;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;

import java.util.List;

public final class BookProvider {

    public interface Books {

        Item bookFor(StyleRarity rarity);

        boolean isBook(Item item);

        void setContents(ItemStack book, List<StyleEntry> entries);

        StyleSequence contentsOf(ItemStack book);
    }

    private static Books books;

    private BookProvider() {
    }

    public static void set(Books provider) {
        books = provider;
    }

    public static boolean available() {
        return books != null;
    }

    public static Item bookFor(StyleRarity rarity) {
        return books == null ? null : books.bookFor(rarity);
    }

    public static boolean isBook(Item item) {
        return books != null && books.isBook(item);
    }

    public static void setContents(ItemStack book, List<StyleEntry> entries) {
        if (books != null) {
            books.setContents(book, entries);
        }
    }

    public static StyleSequence contentsOf(ItemStack book) {
        return books == null ? StyleSequence.EMPTY : books.contentsOf(book);
    }
}
