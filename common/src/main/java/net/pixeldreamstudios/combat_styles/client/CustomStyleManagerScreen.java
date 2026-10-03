package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.style.CustomStyle;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.List;

public class CustomStyleManagerScreen extends ContentListScreen<CustomStyle> {

    private static final String KEY_TITLE = "screen.combat_styles.custom_style_manager";
    private static final String KEY_SEARCH = "screen.combat_styles.search_custom_styles";
    private static final String SUBTITLE_SEPARATOR = " · ";

    private final Runnable onChanged;

    public CustomStyleManagerScreen(Screen parent, boolean creative, Runnable onChanged) {
        super(Component.translatable(KEY_TITLE), parent, creative);
        this.onChanged = onChanged;
    }

    @Override
    protected Component searchHint() {
        return Component.translatable(KEY_SEARCH);
    }

    @Override
    protected List<CustomStyle> entries() {
        return CustomStyles.all();
    }

    @Override
    protected String idOf(CustomStyle entry) {
        return entry.id();
    }

    @Override
    protected String subtitleOf(CustomStyle entry) {
        StylePage base = StyleRegistry.get(entry.entry().styleId());
        String baseName = base == null ? entry.entry().styleId() : StyleText.name(base).getString();
        return StyleText.prettify(entry.group()) + SUBTITLE_SEPARATOR + baseName;
    }

    @Override
    protected int tintOf(CustomStyle entry) {
        StylePage page = StyleRegistry.get(entry.id());
        StyleRarity rarity = entry.rarity().orElse(page == null ? StyleRarity.COMMON : page.rarity());
        Integer tint = rarity.color().getColor();
        return tint == null ? ScreenTheme.MUTED : tint;
    }

    @Override
    protected Source sourceOf(CustomStyle entry) {
        if (CustomStyles.isOverridden(entry.id())) {
            return Source.OVERRIDDEN;
        }
        return CustomStyles.isConfigOwned(entry.id()) ? Source.CONFIG : Source.DATAPACK;
    }

    @Override
    protected void openEditor(CustomStyle existing) {
        minecraft.setScreen(new CustomStyleEditorScreen(this, existing, saved -> {
            refresh();
            onChanged.run();
        }));
    }

    @Override
    protected boolean remove(CustomStyle entry) {
        if (!ConfigEdits.remove(EditConfigPayload.Kind.CUSTOM_STYLE_DELETE, entry.id())) {
            return false;
        }
        CustomStyles.removeLocal(entry.id());
        StyleRegistry.rebuild();
        onChanged.run();
        return true;
    }
}
