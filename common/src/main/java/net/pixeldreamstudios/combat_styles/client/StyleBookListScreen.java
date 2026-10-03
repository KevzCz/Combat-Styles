package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.loot.BookRoll;
import net.pixeldreamstudios.combat_styles.loot.BookRolls;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.style.StyleText;

public class StyleBookListScreen extends DefinitionListScreen<BookRoll> {

    private static final String KEY_TITLE = "screen.combat_styles.style_book_list";
    private static final String KEY_SUMMARY = "screen.combat_styles.style_book_summary";
    private static final String KEY_EDITOR = "screen.combat_styles.style_book_editor";

    public StyleBookListScreen(Screen parent, boolean creative) {
        super(Component.translatable(KEY_TITLE), parent, creative, BookRolls.DEFINITIONS,
                EffectSchema.ROOT_STYLE_BOOK, EditConfigPayload.Kind.STYLE_BOOK,
                EditConfigPayload.Kind.STYLE_BOOK_CLEAR, BookRoll.SAMPLE,
                Component.translatable(KEY_EDITOR));
    }

    @Override
    protected String describe(BookRoll value) {
        return StyleText.rarityName(value.rarity()).getString() + " · "
                + Component.translatable(KEY_SUMMARY, value.pool().size(), value.minAttacks(),
                value.maxAttacks()).getString();
    }

    @Override
    protected int tint(BookRoll value) {
        Integer tint = value.rarity().color().getColor();
        return tint == null ? ScreenTheme.MUTED : tint;
    }
}
