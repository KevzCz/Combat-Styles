package net.pixeldreamstudios.combat_styles.compat;

import dev.architectury.platform.Platform;
import net.pixeldreamstudios.combat_styles.compat.editor.EditorLists;

public final class SkillTreeEditorCompat {

    public static final String MOD_ID = "skill_tree_editor";

    private SkillTreeEditorCompat() {
    }

    public static void init() {
        if (Platform.isModLoaded(MOD_ID)) {
            EditorLists.register();
        }
    }
}
