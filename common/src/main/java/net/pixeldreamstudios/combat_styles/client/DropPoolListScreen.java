package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.loot.DropPool;
import net.pixeldreamstudios.combat_styles.loot.DropPools;
import net.pixeldreamstudios.combat_styles.loot.EquipmentDrops;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;

public class DropPoolListScreen extends DefinitionListScreen<DropPool> {

    private static final String KEY_TITLE = "screen.combat_styles.drop_pool_list";
    private static final String KEY_SUMMARY = "screen.combat_styles.drop_pool_summary";
    private static final String KEY_EDITOR = "screen.combat_styles.drop_pool_editor";
    private static final double PERCENT = 100.0D;

    public DropPoolListScreen(Screen parent, boolean creative) {
        super(Component.translatable(KEY_TITLE), parent, creative, DropPools.DEFINITIONS,
                EffectSchema.ROOT_DROP_POOL, EditConfigPayload.Kind.DROP_POOL,
                EditConfigPayload.Kind.DROP_POOL_CLEAR, DropPool.EMPTY,
                Component.translatable(KEY_EDITOR));
    }

    @Override
    protected boolean canCreate() {
        return false;
    }

    @Override
    protected List<Named<DropPool>> entries() {
        List<Named<DropPool>> entries = new ArrayList<>();
        for (StyleRarity rarity : StyleRarity.values()) {
            entries.add(new Named<>(DropPools.idFor(rarity), DropPools.stored(rarity)));
        }
        return entries;
    }

    @Override
    protected DropPool forEditing(String id, DropPool value) {
        StyleRarity rarity = DropPools.rarityOf(id);
        return rarity == null ? value : DropPools.withTagged(value, EquipmentDrops.tagged(rarity.key()));
    }

    @Override
    protected DropPool beforeSave(String id, DropPool value) {
        StyleRarity rarity = DropPools.rarityOf(id);
        return rarity == null ? value
                : DropPools.withoutDefaultTagged(value, EquipmentDrops.tagged(rarity.key()));
    }

    @Override
    protected String describe(DropPool value) {
        return Component.translatable(KEY_SUMMARY, StyleText.formatDecimal(value.chance() * PERCENT),
                value.entries().size()).getString();
    }

    @Override
    protected int tint(DropPool value) {
        return ScreenTheme.ACCENT;
    }
}
