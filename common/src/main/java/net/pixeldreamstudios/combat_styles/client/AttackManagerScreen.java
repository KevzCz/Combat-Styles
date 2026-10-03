package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.style.CustomAttack;
import net.pixeldreamstudios.combat_styles.style.CustomAttacks;

import java.util.List;
import java.util.function.Consumer;

public class AttackManagerScreen extends ContentListScreen<CustomAttack> {

    private static final String KEY_TITLE = "screen.combat_styles.attack_manager";
    private static final String KEY_SEARCH = "screen.combat_styles.search_attacks";

    private final String defaultCategory;
    private final Consumer<CustomAttack> onSaved;
    private final Consumer<String> onDeleted;

    public AttackManagerScreen(Screen parent, String defaultCategory, boolean creative,
                               Consumer<CustomAttack> onSaved, Consumer<String> onDeleted) {
        super(Component.translatable(KEY_TITLE), parent, creative);
        this.defaultCategory = defaultCategory;
        this.onSaved = onSaved;
        this.onDeleted = onDeleted;
    }

    @Override
    protected Component searchHint() {
        return Component.translatable(KEY_SEARCH);
    }

    @Override
    protected List<CustomAttack> entries() {
        return CustomAttacks.all();
    }

    @Override
    protected String idOf(CustomAttack entry) {
        return entry.id();
    }

    @Override
    protected String subtitleOf(CustomAttack entry) {
        return entry.attack().animation();
    }

    @Override
    protected int tintOf(CustomAttack entry) {
        Integer tint = entry.rarityOrDefault().color().getColor();
        return tint == null ? ScreenTheme.MUTED : tint;
    }

    @Override
    protected Source sourceOf(CustomAttack entry) {
        if (CustomAttacks.isOverridden(entry.id())) {
            return Source.OVERRIDDEN;
        }
        return CustomAttacks.isConfigOwned(entry.id()) ? Source.CONFIG : Source.DATAPACK;
    }

    @Override
    protected void openEditor(CustomAttack existing) {
        minecraft.setScreen(new AttackBuilderScreen(this, defaultCategory, created -> {
            onSaved.accept(created);
            refresh();
        }, existing));
    }

    @Override
    protected boolean remove(CustomAttack entry) {
        if (!ConfigEdits.remove(EditConfigPayload.Kind.ATTACK_DELETE, entry.id())) {
            return false;
        }
        onDeleted.accept(entry.id());
        return true;
    }
}
