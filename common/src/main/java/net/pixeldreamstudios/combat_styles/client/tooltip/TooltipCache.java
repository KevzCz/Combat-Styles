package net.pixeldreamstudios.combat_styles.client.tooltip;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

public final class TooltipCache {

    private Object key;
    private List<Component> lines;

    public List<Component> get(Object cacheKey, Supplier<List<Component>> builder) {
        if (lines == null || key == null || !key.equals(cacheKey)) {
            key = cacheKey;
            lines = List.copyOf(builder.get());
        }
        return lines;
    }

    public void invalidate() {
        key = null;
        lines = null;
    }
}
