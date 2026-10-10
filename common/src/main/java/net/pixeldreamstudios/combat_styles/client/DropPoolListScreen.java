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
import java.util.Map;

public class DropPoolListScreen extends DefinitionListScreen<DropPool> {

    private static final String KEY_TITLE = "screen.combat_styles.drop_pool_list";
    private static final String KEY_SUMMARY = "screen.combat_styles.drop_pool_summary";
    private static final String KEY_EDITOR = "screen.combat_styles.drop_pool_editor";
    private static final double PERCENT = 100.0D;

    public DropPoolListScreen(Screen parent, boolean creative) {
        super(Component.translatable(KEY_TITLE), parent, creative, DropPools.DEFINITIONS,
                EffectSchema.ROOT_DROP_POOL, EditConfigPayload.Kind.DROP_POOL,
                EditConfigPayload.Kind.DROP_POOL_CLEAR, DropPools.newPool(),
                Component.translatable(KEY_EDITOR));
    }

    @Override
    protected List<Named<DropPool>> entries() {
        List<Named<DropPool>> entries = new ArrayList<>();
        for (StyleRarity rarity : StyleRarity.values()) {
            entries.add(new Named<>(DropPools.idFor(rarity), DropPools.stored(rarity)));
        }
        for (Map.Entry<String, DropPool> custom : DropPools.DEFINITIONS.view().entrySet()) {
            if (!DropPools.isBuiltIn(custom.getKey())) {
                entries.add(new Named<>(custom.getKey(), custom.getValue()));
            }
        }
        return entries;
    }

    @Override
    protected DropPool forEditing(String id, DropPool value) {
        return DropPools.withTagged(value, EquipmentDrops.tagged(DropPools.tagKey(id)));
    }

    @Override
    protected DropPool beforeSave(String id, DropPool value) {
        return DropPools.withoutDefaultTagged(value, EquipmentDrops.tagged(DropPools.tagKey(id)));
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
