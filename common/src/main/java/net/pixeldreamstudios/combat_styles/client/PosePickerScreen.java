package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PosePickerScreen extends AnimationPickerScreen {

    public static final String POSE_PREFIX = "pose_";
    public static final String NONE = "";

    private static final String KEY_TITLE = "screen.combat_styles.pose_picker";
    private static final String KEY_SEARCH = "screen.combat_styles.search_poses";
    private static final String KEY_COUNT = "screen.combat_styles.pose_count";
    private static final String KEY_EMPTY = "screen.combat_styles.no_poses";
    private static final String KEY_NONE = "screen.combat_styles.pose_none";

    public PosePickerScreen(Screen parent, Consumer<String> onPick) {
        super(parent, onPick, Component.translatable(KEY_TITLE));
    }

    public static List<String> poses() {
        List<String> found = new ArrayList<>();
        found.add(NONE);
        for (String animation : AnimationCatalogue.all()) {
            if (isPose(animation)) {
                found.add(animation);
            }
        }
        return List.copyOf(found);
    }

    public static boolean isPose(String animation) {
        if (animation == null || animation.isBlank()) {
            return false;
        }
        int colon = animation.indexOf(':');
        String path = colon < 0 ? animation : animation.substring(colon + 1);
        return path.startsWith(POSE_PREFIX);
    }

    public static Component label(String pose) {
        return pose == null || pose.isBlank()
                ? Component.translatable(KEY_NONE)
                : Component.literal(StyleText.prettify(shortName(pose)));
    }

    @Override
    protected String rowText(String row) {
        return label(row).getString();
    }

    static String shortName(String pose) {
        int colon = pose.indexOf(':');
        String path = colon < 0 ? pose : pose.substring(colon + 1);
        return path.startsWith(POSE_PREFIX) ? path.substring(POSE_PREFIX.length()) : path;
    }

    @Override
    protected List<String> choices() {
        return poses();
    }

    @Override
    protected Component searchHint() {
        return Component.translatable(KEY_SEARCH);
    }

    @Override
    protected Component headerNote() {
        return Component.translatable(KEY_COUNT, visible().size(), poses().size());
    }

    @Override
    protected Component emptyLabel() {
        return Component.translatable(KEY_EMPTY);
    }

}
