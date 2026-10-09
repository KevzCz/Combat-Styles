package net.pixeldreamstudios.combat_styles.client;

import net.bettercombat.api.WeaponAttributes;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class SoundPickerScreen extends PickerScreen<SoundPickerScreen.Row> {

    public record Row(String soundId, String label, boolean header, boolean isDefault) {
    }

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 232;
    private static final int ROW_HEIGHT = 13;
    private static final int BUTTON = 70;
    private static final int NAME_MARGIN = 70;

    private static final String KEY_TITLE = "screen.combat_styles.pick_sound";
    private static final String KEY_SEARCH = "screen.combat_styles.search_sounds";
    private static final String KEY_MATCHING = "screen.combat_styles.sounds_matching";
    private static final String KEY_OTHER = "screen.combat_styles.sounds_other";
    private static final String KEY_REGISTRY = "screen.combat_styles.sounds_registry";
    private static final String KEY_DEFAULT_SOUND = "screen.combat_styles.sound_default";
    private static final String KEY_NO_SOUND = "screen.combat_styles.sound_none";
    private static final String KEY_CANCEL = "gui.cancel";
    private static final String KEY_CONFIRM = "screen.combat_styles.confirm";

    private final StylePage page;
    private final Consumer<String> onChosen;
    private String chosen;

    public SoundPickerScreen(Screen parent, StylePage page, String currentSoundId,
                             Consumer<String> onChosen) {
        super(Component.translatable(KEY_TITLE), parent);
        this.page = page;
        this.chosen = currentSoundId;
        this.onChosen = onChosen;
    }

    @Override
    protected int panelWidth() {
        return fittedWidth(PANEL_WIDTH);
    }

    @Override
    protected int rowWidth(Row row) {
        return font.width(row.label()) + (row.header() ? TEXT_INSET * 2 : NAME_MARGIN);
    }

    @Override
    protected int panelHeight() {
        return PANEL_HEIGHT;
    }

    @Override
    protected int rowHeight() {
        return ROW_HEIGHT;
    }

    @Override
    protected boolean rebuildsOnSearch() {
        return true;
    }

    @Override
    protected boolean searchable() {
        return true;
    }

    @Override
    protected boolean closeOnPick() {
        return false;
    }

    @Override
    protected boolean selectable(Row row) {
        return !row.header();
    }

    @Override
    protected Component searchHint() {
        return Component.translatable(KEY_SEARCH);
    }

    @Override
    protected String searchTextOf(Row row) {
        return row.soundId();
    }

    @Override
    protected List<Row> choices() {
        return rowsFor("");
    }

    @Override
    protected List<Row> rowsFor(String needle) {
        List<Row> rows = new ArrayList<>();
        String defaultId = page == null ? null : page.swingSoundId();
        Set<String> matchingIds = new LinkedHashSet<>();

        if (accepts(AttackOverride.SILENT_SOUND, needle)
                || Component.translatable(KEY_NO_SOUND).getString()
                        .toLowerCase(Locale.ROOT).contains(needle)) {
            rows.add(new Row(AttackOverride.SILENT_SOUND,
                    Component.translatable(KEY_NO_SOUND).getString(), false, false));
        }

        List<WeaponAttributes.Sound> matching = page == null
                ? List.of()
                : StyleRegistry.swingSoundsForCategory(page.category());

        List<Row> matchingRows = new ArrayList<>();
        for (WeaponAttributes.Sound sound : matching) {
            matchingIds.add(sound.id());
            if (accepts(sound.id(), needle)) {
                matchingRows.add(new Row(sound.id(), StyleText.soundName(sound.id()), false,
                        Objects.equals(sound.id(), defaultId)));
            }
        }

        List<Row> otherRows = new ArrayList<>();
        for (WeaponAttributes.Sound sound : StyleRegistry.swingSounds()) {
            if (matchingIds.contains(sound.id())) {
                continue;
            }
            if (accepts(sound.id(), needle)) {
                otherRows.add(new Row(sound.id(), StyleText.soundName(sound.id()), false, false));
            }
        }

        Set<String> known = new LinkedHashSet<>(matchingIds);
        for (Row row : otherRows) {
            known.add(row.soundId());
        }

        List<Row> registryRows = new ArrayList<>();
        for (ResourceLocation id : BuiltInRegistries.SOUND_EVENT.keySet()) {
            String soundId = id.toString();
            if (known.contains(soundId) || !accepts(soundId, needle)) {
                continue;
            }
            registryRows.add(new Row(soundId, StyleText.soundName(soundId), false, false));
        }
        registryRows.sort(Comparator.comparing(Row::soundId));

        if (!matchingRows.isEmpty()) {
            rows.add(new Row(null, Component.translatable(KEY_MATCHING).getString(), true, false));
            rows.addAll(matchingRows);
        }
        if (!otherRows.isEmpty()) {
            rows.add(new Row(null, Component.translatable(KEY_OTHER).getString(), true, false));
            rows.addAll(otherRows);
        }
        if (!registryRows.isEmpty()) {
            rows.add(new Row(null, Component.translatable(KEY_REGISTRY).getString(), true, false));
            rows.addAll(registryRows);
        }
        return rows;
    }

    private boolean accepts(String soundId, String needle) {
        if (needle == null || needle.isEmpty()) {
            return true;
        }
        return soundId.toLowerCase(Locale.ROOT).contains(needle)
                || StyleText.soundName(soundId).toLowerCase(Locale.ROOT).contains(needle);
    }

    @Override
    protected void onPicked(Row row) {
        chosen = row.soundId();
        preview(row.soundId());
    }

    private void preview(String soundId) {
        if (soundId == null || minecraft == null || AttackOverride.isSilent(soundId)) {
            return;
        }
        ResourceLocation location = ResourceLocation.tryParse(soundId);
        if (location == null) {
            return;
        }
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(location);
        if (event != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0F));
        }
    }

    @Override
    protected void addFooterButtons(int buttonY) {
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CANCEL),
                        button -> minecraft.setScreen(parent))
                .bounds(contentLeft(), buttonY, BUTTON, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CONFIRM),
                        button -> confirm())
                .bounds(contentRight() - BUTTON, buttonY,
                        BUTTON, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private void confirm() {
        onChosen.accept(Objects.equals(chosen, page == null ? null : page.swingSoundId())
                ? null : chosen);
        minecraft.setScreen(parent);
    }

    @Override
    protected void renderRow(GuiGraphics graphics, Row row, int index,
                             int x, int y, int width, boolean hovered) {
        if (row.header()) {
            graphics.drawString(font, row.label(), x + TEXT_INSET, y + 3,
                    ScreenTheme.HEADING, false);
            return;
        }
        boolean selected = Objects.equals(row.soundId(), chosen);
        ScreenDraw.rowBackground(graphics, x + 1, y, width - 2, ROW_HEIGHT - 1,
                index, hovered, selected);
        ScreenDraw.drawTrimmed(graphics, font, row.label(),
                x + TEXT_INSET, y + 3, width - NAME_MARGIN, selected ? ScreenTheme.GOOD : ScreenTheme.TEXT);
        if (row.isDefault()) {
            Component tag = Component.translatable(KEY_DEFAULT_SOUND);
            graphics.drawString(font, tag, x + width - font.width(tag) - 6, y + 3,
                    ScreenTheme.MUTED, false);
        }
    }
}
