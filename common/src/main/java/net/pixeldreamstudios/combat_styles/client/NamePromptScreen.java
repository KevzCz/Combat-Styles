package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;
import java.util.function.Function;

public class NamePromptScreen extends PanelScreen {

    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 96;
    private static final int TITLE_Y = 10;
    private static final int FIELD_Y = 28;
    private static final int FIELD_HEIGHT = 14;
    private static final int ERROR_Y = 48;
    private static final int BUTTON_WIDTH = 74;
    private static final int NAME_MAX = 64;

    private static final String KEY_HINT = "screen.combat_styles.name_prompt_hint";
    private static final String KEY_CREATE = "screen.combat_styles.name_prompt_create";
    private static final String KEY_CANCEL = "gui.cancel";

    private final Function<String, Optional<Component>> onSubmit;
    private EditBox nameBox;
    private String typed = "";
    private Component error;

    public NamePromptScreen(Screen parent, Component title,
                            Function<String, Optional<Component>> onSubmit) {
        super(title, parent);
        this.onSubmit = onSubmit;
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
        nameBox = new FieldBox(font, contentLeft(), top + FIELD_Y, contentWidth(), FIELD_HEIGHT,
                Component.translatable(KEY_HINT));
        nameBox.setHint(Component.translatable(KEY_HINT).withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setMaxLength(NAME_MAX);
        nameBox.setValue(typed);
        nameBox.setResponder(value -> {
            typed = value;
            error = null;
        });
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);

        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CREATE), button -> submit())
                .bounds(contentLeft(), footerTop(), BUTTON_WIDTH, ScreenTheme.BUTTON_HEIGHT).build());
        addRenderableWidget(PanelButton.of(Component.translatable(KEY_CANCEL),
                        button -> minecraft.setScreen(parent))
                .bounds(contentRight() - BUTTON_WIDTH, footerTop(), BUTTON_WIDTH,
                        ScreenTheme.BUTTON_HEIGHT).build());
    }

    private void submit() {
        error = onSubmit.apply(typed.trim()).orElse(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics);
        drawTitle(graphics, title, top + TITLE_Y);
        if (error != null) {
            ScreenDraw.drawTrimmed(graphics, font, error.getString(),
                    contentLeft(), top + ERROR_Y, contentWidth(), ScreenTheme.BAD);
        }
        drawWidgets(graphics, mouseX, mouseY, partialTick);
    }
}
