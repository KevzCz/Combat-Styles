package net.pixeldreamstudios.combat_styles.progression;

import java.util.Map;
import java.util.function.Supplier;

public final class ClientLearnedView {

    private static volatile Supplier<Map<String, Integer>> source = Map::of;

    private ClientLearnedView() {
    }

    public static void bind(Supplier<Map<String, Integer>> supplier) {
        if (supplier != null) {
            source = supplier;
        }
    }

    public static Map<String, Integer> copies() {
        Map<String, Integer> copies = source.get();
        return copies == null ? Map.of() : copies;
    }
}
