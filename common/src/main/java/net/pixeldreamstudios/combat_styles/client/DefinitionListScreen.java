package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.DefinitionLayer;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public abstract class DefinitionListScreen<T> extends ContentListScreen<DefinitionListScreen.Named<T>> {

    public record Named<T>(String id, T value) {
    }

    private static final String NAMESPACE_SEPARATOR = ":";
    private static final String KEY_NEEDS_ID = "screen.combat_styles.definition_needs_id";
    private static final String KEY_BAD_ID = "screen.combat_styles.affix_bad_id";
    private static final String KEY_EXISTS = "screen.combat_styles.definition_exists";
    private static final String KEY_SAVE_FAILED = "screen.combat_styles.affix_save_failed";
    private static final String KEY_NEW_TITLE = "screen.combat_styles.definition_new";

    private final DefinitionLayer<T> layer;
    private final String schemaRoot;
    private final EditConfigPayload.Kind saveKind;
    private final EditConfigPayload.Kind clearKind;
    private final T sample;
    private final Component editorTitle;

    protected DefinitionListScreen(Component title, Screen parent, boolean creative,
                                   DefinitionLayer<T> layer, String schemaRoot,
                                   EditConfigPayload.Kind saveKind, EditConfigPayload.Kind clearKind,
                                   T sample, Component editorTitle) {
        super(title, parent, creative);
        this.editorTitle = editorTitle;
        this.layer = layer;
        this.schemaRoot = schemaRoot;
        this.saveKind = saveKind;
        this.clearKind = clearKind;
        this.sample = sample;
    }

    protected abstract String describe(T value);

    protected abstract int tint(T value);

    protected void afterChange() {
    }

    @Override
    protected List<Named<T>> entries() {
        List<Named<T>> entries = new ArrayList<>();
        for (Map.Entry<String, T> entry : layer.view().entrySet()) {
            entries.add(new Named<>(entry.getKey(), entry.getValue()));
        }
        return entries;
    }

    @Override
    protected String idOf(Named<T> entry) {
        return entry.id();
    }

    @Override
    protected String subtitleOf(Named<T> entry) {
        return describe(entry.value());
    }

    @Override
    protected int tintOf(Named<T> entry) {
        return tint(entry.value());
    }

    @Override
    protected Source sourceOf(Named<T> entry) {
        if (layer.isOverridden(entry.id())) {
            return Source.OVERRIDDEN;
        }
        return layer.isConfigOwned(entry.id()) ? Source.CONFIG : Source.DATAPACK;
    }

    @Override
    protected void openEditor(Named<T> existing) {
        if (existing == null) {
            minecraft.setScreen(new NamePromptScreen(this,
                    Component.translatable(KEY_NEW_TITLE, editorTitle), this::createNamed));
            return;
        }
        edit(existing.id(), existing.value());
    }

    private Optional<Component> createNamed(String raw) {
        String name = raw.toLowerCase(Locale.ROOT);
        if (name.isEmpty()) {
            return Optional.of(Component.translatable(KEY_NEEDS_ID));
        }
        ResourceLocation id = name.contains(NAMESPACE_SEPARATOR)
                ? ResourceLocation.tryParse(name)
                : ResourceLocation.tryBuild(CombatStyles.MOD_ID, name);
        if (id == null) {
            return Optional.of(Component.translatable(KEY_BAD_ID));
        }
        if (layer.view().containsKey(id.toString())) {
            return Optional.of(Component.translatable(KEY_EXISTS));
        }
        edit(id.toString(), sample);
        return Optional.empty();
    }

    private void edit(String id, T current) {
        minecraft.setScreen(new EffectTreeScreen(this, editorTitle, Component.literal(id),
                layer.codec(), current,
                applied -> applied.ifPresent(value -> save(id, value)))
                .withSchema(schemaRoot));
    }

    private void save(String id, T value) {
        if (!ConfigEdits.permitted()) {
            showError(ConfigEdits.operatorOnly());
            return;
        }
        if (!ConfigEdits.save(saveKind, id, layer.codec(), value)) {
            showError(Component.translatable(KEY_SAVE_FAILED));
            return;
        }
        layer.acceptLocal(id, value);
        afterChange();
        refresh();
    }

    @Override
    protected boolean remove(Named<T> entry) {
        if (!ConfigEdits.remove(clearKind, entry.id())) {
            return false;
        }
        layer.removeLocal(entry.id());
        afterChange();
        return true;
    }
}
