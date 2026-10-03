package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.List;

public class AffixManagerScreen extends ContentListScreen<StyleAffix> {

    private static final String KEY_TITLE = "screen.combat_styles.affix_manager";
    private static final String KEY_SEARCH = "screen.combat_styles.search_affixes";
    private static final String KEY_WEIGHT = "screen.combat_styles.affix_manager_weight";

    private final Runnable onChanged;

    public AffixManagerScreen(Screen parent, boolean creative, Runnable onChanged) {
        super(Component.translatable(KEY_TITLE), parent, creative);
        this.onChanged = onChanged;
    }

    @Override
    protected Component searchHint() {
        return Component.translatable(KEY_SEARCH);
    }

    @Override
    protected List<StyleAffix> entries() {
        return AffixRegistry.all();
    }

    @Override
    protected String idOf(StyleAffix entry) {
        return entry.serializedId();
    }

    @Override
    protected String subtitleOf(StyleAffix entry) {
        return StyleText.rarityName(entry.rarity()).getString() + " · "
                + Component.translatable(KEY_WEIGHT, entry.weight()).getString();
    }

    @Override
    protected int tintOf(StyleAffix entry) {
        Integer tint = entry.rarity().color().getColor();
        return tint == null ? ScreenTheme.MUTED : tint;
    }

    @Override
    protected Source sourceOf(StyleAffix entry) {
        String id = entry.serializedId();
        if (AffixRegistry.isOverridden(id)) {
            return Source.OVERRIDDEN;
        }
        return AffixRegistry.isConfigOwned(id) ? Source.CONFIG : Source.DATAPACK;
    }

    @Override
    protected void openEditor(StyleAffix existing) {
        minecraft.setScreen(new AffixBuilderScreen(this, existing, saved -> {
            refresh();
            onChanged.run();
        }));
    }

    @Override
    protected boolean remove(StyleAffix entry) {
        if (!ConfigEdits.remove(EditConfigPayload.Kind.AFFIX_DELETE, entry.serializedId())) {
            return false;
        }
        AffixRegistry.removeLocal(entry.serializedId());
        onChanged.run();
        return true;
    }
}
