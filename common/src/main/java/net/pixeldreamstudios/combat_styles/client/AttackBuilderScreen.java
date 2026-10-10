package net.pixeldreamstudios.combat_styles.client;

import net.bettercombat.api.fx.ParticlePlacement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.WeaponCategories;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.style.CustomAttack;
import net.pixeldreamstudios.combat_styles.style.DeclaredAttack;
import net.pixeldreamstudios.combat_styles.style.SlashAnchor;
import net.pixeldreamstudios.combat_styles.style.SlashMode;
import net.pixeldreamstudios.combat_styles.style.SlashPlacement;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class AttackBuilderScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 218;
    private static final int PADDING = ScreenTheme.PADDING;
    private static final int HEADER = 70;
    private static final int FOOTER = 40;
    private static final int ROW_HEIGHT = 18;
    private static final int BOX_HEIGHT = 14;
    private static final int BUTTON_WIDTH = 74;
    private static final int NAME_MAX = 48;
    private static final int LABEL_WIDTH = 54;
    private static final int PICK_WIDTH = 44;

    private static final String KEY_TITLE = "screen.combat_styles.attack_builder";
    private static final String KEY_SHAPE = "screen.combat_styles.section_identity";
    private static final String KEY_TITLE_EDIT = "screen.combat_styles.attack_editor_title";
    private static final String KEY_HINT = "screen.combat_styles.attack_builder_hint";
    private static final String KEY_NAME = "screen.combat_styles.builder_name";
    private static final String KEY_ANIMATION = "screen.combat_styles.builder_animation";
    private static final String KEY_PICK = "screen.combat_styles.builder_pick";
    private static final String KEY_HITBOX = "tooltip.combat_styles.hitbox";
    private static final String KEY_CATEGORY = "tooltip.combat_styles.category";
    private static final String KEY_RARITY = "screen.combat_styles.field_rarity";
    private static final String KEY_SOUND = "tooltip.combat_styles.sound";
    private static final String KEY_STYLE_PAGE = "screen.combat_styles.field_style_page";
    private static final String KEY_STYLE_PAGE_HINT = "hint.combat_styles.style_page";
    private static final String KEY_NATURAL = "screen.combat_styles.style_page_natural";
    private static final String KEY_NOT_NATURAL = "screen.combat_styles.style_page_hidden";
    private static final String KEY_SOUND_NONE = "screen.combat_styles.builder_sound_none";
    private static final String KEY_SAVE = "screen.combat_styles.builder_save";
    private static final String KEY_UPDATE = "screen.combat_styles.builder_update";
    private static final String KEY_CLOSE = "gui.back";
    private static final String KEY_INVALID = "screen.combat_styles.builder_invalid";
    private static final String KEY_SLASHES_BUTTON = "screen.combat_styles.slashes_button";
    private static final String DRAFT_ANIMATION = "minecraft:none";

    private static final String[] HITBOXES = {
            "horizontal_plane", "vertical_plane", "forward_box"};

    private static final int KEY_ENTER = 257;

    private final Consumer<CustomAttack> onCreated;
    private final List<String> categories;
    private final Scrollbar scrollbar = new Scrollbar(ScreenTheme.TRACK, ScreenTheme.ACCENT);
    private EditBox nameBox;
    private EditBox animationBox;
    private String nameText = "";
    private String animationText = "";
    private int hitbox;
    private int rarity = StyleRarity.COMMON.ordinal();
    private int category;
    private final CustomAttack editing;
    private EditBox valueBox;
    private int editingRow = -1;
    private final List<SlashPlacement> extraSlashes = new ArrayList<>();
    private double baseDamage = 1.0D;
    private double baseAngle = 90.0D;
    private double baseUpswing = 0.5D;
    private float baseRange = 1.0F;
    private float baseMovement = 1.0F;
    private String swingSound = "";
    private boolean natural = true;
    private String error;

    public AttackBuilderScreen(Screen parent, String defaultCategory,
                               Consumer<CustomAttack> onCreated) {
        this(parent, defaultCategory, onCreated, null);
    }

    public AttackBuilderScreen(Screen parent, String defaultCategory,
                               Consumer<CustomAttack> onCreated, CustomAttack existing) {
        super(Component.translatable(KEY_TITLE), parent);
        this.onCreated = onCreated;
        this.editing = existing;

        categories = new ArrayList<>(WeaponCategories.allNames());
        category = defaultCategory == null
                ? 0
                : Math.max(0, categories.indexOf(defaultCategory));

        if (existing != null) {
            DeclaredAttack source = existing.attack();
            nameText = prettify(existing.id().contains(":")
                    ? existing.id().substring(existing.id().indexOf(':') + 1)
                    : existing.id());
            animationText = source.animation();
            hitbox = Math.max(0, indexOf(HITBOXES, source.hitbox()));
            rarity = existing.rarityOrDefault().ordinal();
            natural = existing.stylePage().orElse(Boolean.TRUE);
            category = Math.max(0, categories.indexOf(existing.categoryOrDefault()));
            baseDamage = source.damageMultiplier();
            baseAngle = source.angle();
            baseUpswing = source.upswing();
            baseRange = source.rangeMultiplier();
            baseMovement = source.movementSpeedMultiplier();
            swingSound = source.swingSound().orElse("");
            source.trail().ifPresent(list -> {
                for (ParticlePlacement placement : list) {
                    extraSlashes.add(new SlashPlacement(placement.particle_type(),
                            placement.x_addition(), placement.y_addition(),
                            placement.z_addition(), placement.local_yaw(),
                            placement.pitch_addition(), placement.roll_set(), 0,
                            SlashMode.PARTICLE, Optional.empty(), SlashAnchor.WEAPON));
                }
            });
            extraSlashes.addAll(source.slashes());
        }
    }

    private static int indexOf(String[] values, String needle) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(needle)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected int panelWidth() {
        return PANEL_WIDTH;
    }

    @Override
    protected int panelHeight() {
        return PANEL_HEIGHT;
    }

    @Override
    protected void layout() {

        nameBox = new FieldBox(font, left + PADDING + LABEL_WIDTH, top + 24,
                PANEL_WIDTH - PADDING * 2 - LABEL_WIDTH, BOX_HEIGHT,
                Component.translatable(KEY_NAME));
        nameBox.setMaxLength(NAME_MAX);
        nameBox.setHint(Component.translatable(KEY_NAME).withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(nameText);
        nameBox.setResponder(value -> nameText = value);
        addRenderableWidget(nameBox);

        animationBox = new FieldBox(font, left + PADDING + LABEL_WIDTH, top + 42,
                PANEL_WIDTH - PADDING * 2 - LABEL_WIDTH - PICK_WIDTH - 4, BOX_HEIGHT,
                Component.translatable(KEY_ANIMATION));
        animationBox.setMaxLength(NAME_MAX * 2);
        animationBox.setHint(Component.translatable(KEY_ANIMATION)
                .withStyle(ChatFormatting.DARK_GRAY));
        animationBox.setValue(animationText);
        animationBox.setResponder(value -> animationText = value);
        addRenderableWidget(animationBox);

        valueBox = new FieldBox(font, left + PADDING, top + 24, 60, 12, Component.empty());
        valueBox.setMaxLength(12);
        valueBox.setVisible(false);
        addRenderableWidget(valueBox);

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_PICK), button ->
                        minecraft.setScreen(new AnimationPickerScreen(this, picked -> {
                            animationText = picked;
                            animationBox.setValue(picked);
                        })))
                .bounds(left + PANEL_WIDTH - PADDING - PICK_WIDTH, top + 42,
                        PICK_WIDTH, BOX_HEIGHT)
                .build());

        int buttonY = top + PANEL_HEIGHT - ScreenTheme.BUTTON_HEIGHT - 6;
        addRenderableWidget(PanelButton.of(slashesLabel(),
                        button -> minecraft.setScreen(new SlashListScreen(this, draftPage(),
                                extraSlashes,
                                applied -> {
                                    extraSlashes.clear();
                                    extraSlashes.addAll(applied);
                                })))
                .bounds(left + PADDING, buttonY, BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CLOSE),
                        button -> minecraft.setScreen(parent))
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH * 2 - 6, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(
                        Component.translatable(editing == null ? KEY_SAVE : KEY_UPDATE),
                        button -> save())
                .bounds(left + PANEL_WIDTH - PADDING - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
    }

    private Optional<String> soundOf() {
        return swingSound == null || swingSound.isBlank()
                ? Optional.empty()
                : Optional.of(swingSound);
    }

    private void openCategoryPicker() {
        minecraft.setScreen(new ChoicePickerScreen(this, Component.translatable(KEY_CATEGORY),
                categories, categories.isEmpty() ? null : categories.get(category), picked -> {
                    Optional<String> chosen = WeaponCategories.normalize(picked);
                    if (chosen.isEmpty()) {
                        return;
                    }
                    if (!categories.contains(chosen.get())) {
                        categories.add(chosen.get());
                    }
                    category = categories.indexOf(chosen.get());
                }).creatingNew(WeaponCategories::normalize));
    }

    private String soundLabel() {
        return swingSound == null || swingSound.isBlank()
                ? Component.translatable(KEY_SOUND_NONE).getString()
                : StyleText.soundName(swingSound);
    }

    private StylePage draftPage() {
        DeclaredAttack draft = new DeclaredAttack(
                animationText.isBlank() ? DRAFT_ANIMATION : animationText,
                HITBOXES[hitbox], baseDamage, baseAngle, baseUpswing,
                baseRange, baseMovement, soundOf(), Optional.empty());
        return new StylePage("", categories.isEmpty() ? "" : categories.get(category), 0,
                draft.toAttack(), null, StyleRarity.values()[rarity]);
    }

    private Component slashesLabel() {
        return Component.translatable(KEY_SLASHES_BUTTON, extraSlashes.size());
    }

    private String idFor(String name) {
        String slug = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        slug = slug.replaceAll("^_+", "").replaceAll("_+$", "");
        return slug.isBlank() ? "" : CombatStyles.MOD_ID + ":" + slug;
    }

    private void save() {
        String id = editing != null ? editing.id() : idFor(nameText);
        String animation = animationText.trim();
        if (id.isBlank() || animation.isBlank() || !animation.contains(":")) {
            error = Component.translatable(KEY_INVALID).getString();
            return;
        }
        DeclaredAttack attack = new DeclaredAttack(animation, HITBOXES[hitbox],
                baseDamage, baseAngle, baseUpswing, baseRange, baseMovement,
                soundOf(), trailOf(), delayedSlashes());
        CustomAttack created = new CustomAttack(id, attack,
                categories.isEmpty() ? Optional.empty() : Optional.of(categories.get(category)),
                Optional.of(StyleRarity.values()[rarity]),
                editing == null ? Optional.empty() : editing.costOverride(),
                editing == null ? Optional.empty() : editing.nameKey(),
                natural ? Optional.empty() : Optional.of(Boolean.FALSE));
        onCreated.accept(created);
        minecraft.setScreen(parent);
    }

    private List<SlashPlacement> delayedSlashes() {
        return extraSlashes.stream()
                .filter(slash -> slash.delayTicks() > 0 || slash.mode() != SlashMode.PARTICLE)
                .toList();
    }

    private Optional<List<ParticlePlacement>> trailOf() {
        List<ParticlePlacement> placements = new ArrayList<>();
        for (SlashPlacement slash : extraSlashes) {
            if (slash.delayTicks() <= 0 && slash.mode() == SlashMode.PARTICLE) {
                placements.add(slash.toBetterCombat(SlashPlacement.FALLBACK));
            }
        }
        return placements.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(placements));
    }

    protected int listTop() {
        return top + HEADER;
    }

    protected int listBottom() {
        return top + PANEL_HEIGHT - FOOTER - 6;
    }

    protected int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_HEIGHT);
    }

    protected int maxScroll() {
        return 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, width, height, ScreenTheme.SHADE);
        ScreenDraw.panel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(font,
                Component.translatable(editing == null ? KEY_TITLE : KEY_TITLE_EDIT),
                left + PADDING, top + 10, ScreenTheme.HEADING, false);
        graphics.drawString(font, Component.translatable(KEY_NAME),
                left + PADDING, top + 27, ScreenTheme.MUTED, false);
        graphics.drawString(font, Component.translatable(KEY_ANIMATION),
                left + PADDING, top + 45, ScreenTheme.MUTED, false);
        ScreenDraw.sectionHeader(graphics, font, Component.translatable(KEY_SHAPE),
                left + PADDING, top + 60, PANEL_WIDTH - PADDING * 2);

        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        ScreenDraw.sunken(graphics, x - 2, listTop() - 2, listWidth + 2,
                listBottom() - listTop() + 4);
        graphics.enableScissor(x - 2, listTop() - 2, x + listWidth, listBottom() + 2);
        int y = listTop() - scroll;

        y = cycleRow(graphics, KEY_HITBOX, prettify(HITBOXES[hitbox]), x, y, listWidth,
                mouseX, mouseY);
        y = cycleRow(graphics, KEY_CATEGORY,
                categories.isEmpty() ? "-" : prettify(categories.get(category)),
                x, y, listWidth, mouseX, mouseY);
        y = cycleRow(graphics, KEY_RARITY,
                Component.translatable(StyleRarity.values()[rarity].translationKey()).getString(),
                x, y, listWidth, mouseX, mouseY);
        y = cycleRow(graphics, KEY_SOUND, soundLabel(), x, y, listWidth, mouseX, mouseY);
        boolean pageHovered = ScreenDraw.isWithin(mouseX, mouseY, x, y, listWidth, ROW_HEIGHT);
        y = cycleRow(graphics, KEY_STYLE_PAGE,
                Component.translatable(natural ? KEY_NATURAL : KEY_NOT_NATURAL).getString(),
                x, y, listWidth, mouseX, mouseY);

        graphics.disableScissor();

        int noticeY = top + PANEL_HEIGHT - FOOTER + 2;
        Component notice = error != null ? Component.literal(error)
                : Component.translatable(pageHovered ? KEY_STYLE_PAGE_HINT : KEY_HINT);
        graphics.drawString(font, notice, x, noticeY,
                error == null ? ScreenTheme.DIM : ScreenTheme.BAD, false);
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }

    private int cycleRow(GuiGraphics graphics, String labelKey, String value,
                         int x, int y, int listWidth, int mouseX, int mouseY) {
        return ScreenDraw.valueRow(graphics, font, Component.translatable(labelKey), Component.literal(value),
                x, y, listWidth, ROW_HEIGHT, mouseX, mouseY);
    }

    private static String prettify(String raw) {
        String spaced = raw.replace('_', ' ');
        return spaced.isEmpty() ? spaced
                : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        error = null;
        if (valueBox.isVisible() && valueBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(valueBox);
            return true;
        }
        if (nameBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(nameBox);
            return true;
        }
        if (animationBox.mouseClicked(mouseX, mouseY, button)) {
            setFocused(animationBox);
            return true;
        }

        if (scrollbar.isOverTrack(mouseX, mouseY)) {
            scroll = scrollbar.beginDrag(mouseX, mouseY, scroll);
            return true;
        }
        if (mouseY < listTop() || mouseY >= listBottom()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int x = left + PADDING;
        int listWidth = PANEL_WIDTH - PADDING * 2;
        int y = listTop() - scroll;

        if (ScreenDraw.isWithin(mouseX, mouseY, x, y, listWidth, ROW_HEIGHT)) {
            hitbox = (hitbox + 1) % HITBOXES.length;
            return true;
        }
        y += ROW_HEIGHT;
        if (ScreenDraw.isWithin(mouseX, mouseY, x, y, listWidth, ROW_HEIGHT)) {
            openCategoryPicker();
            return true;
        }
        y += ROW_HEIGHT;
        if (ScreenDraw.isWithin(mouseX, mouseY, x, y, listWidth, ROW_HEIGHT)) {
            rarity = (rarity + 1) % StyleRarity.values().length;
            return true;
        }
        y += ROW_HEIGHT;
        if (ScreenDraw.isWithin(mouseX, mouseY, x, y, listWidth, ROW_HEIGHT)) {
            minecraft.setScreen(new SoundPickerScreen(this, draftPage(), swingSound,
                    picked -> swingSound = picked));
            return true;
        }
        y += ROW_HEIGHT;
        if (ScreenDraw.isWithin(mouseX, mouseY, x, y, listWidth, ROW_HEIGHT)) {
            natural = !natural;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (scrollbar.isDragging()) {
            scroll = scrollbar.drag(mouseY, scroll);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrollbar.endDrag();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.clamp(scroll - (int) (scrollY * ROW_HEIGHT), 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KEY_ESCAPE) {
            minecraft.setScreen(parent);
            return true;
        }
        if (keyCode == KEY_ENTER && !nameBox.isFocused() && !animationBox.isFocused()) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (editingRow >= 0) {
            return valueBox.charTyped(character, modifiers);
        }
        return super.charTyped(character, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
