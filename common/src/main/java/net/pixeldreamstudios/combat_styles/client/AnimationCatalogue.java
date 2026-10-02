package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class AnimationCatalogue {

    private static final String FOLDER = "player_animations";
    private static final String EXTENSION = ".json";

    private static List<String> cached = List.of();

    private AnimationCatalogue() {
    }

    public static List<String> all() {
        if (cached.isEmpty()) {
            reload();
        }
        return cached;
    }

    public static void invalidate() {
        cached = List.of();
    }

    public static int reload() {
        TreeSet<String> found = new TreeSet<>(StyleRegistry.animations());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            cached = List.copyOf(found);
            return cached.size();
        }
        ResourceManager resources = minecraft.getResourceManager();
        if (resources == null) {
            cached = List.copyOf(found);
            return cached.size();
        }
        try {
            for (ResourceLocation path : resources
                    .listResources(FOLDER, id -> id.getPath().endsWith(EXTENSION)).keySet()) {
                String name = path.getPath();
                int slash = name.lastIndexOf('/');
                if (slash >= 0) {
                    name = name.substring(slash + 1);
                }
                name = name.substring(0, name.length() - EXTENSION.length());
                if (!name.isBlank()) {
                    found.add(path.getNamespace() + ":" + name);
                }
            }
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.warn("Failed to scan player animations", throwable);
        }
        cached = List.copyOf(new ArrayList<>(found));
        return cached.size();
    }
}
