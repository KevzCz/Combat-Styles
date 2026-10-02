package net.pixeldreamstudios.combat_styles.api;

import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

public interface AffixInfo {

    ResourceLocation id();

    String serializedId();

    StyleRarity rarity();

    int weight();

    double costModifier();

    double damageMultiplier();

    String nameKey();

    String descriptionKey();

    String fallbackName();

    boolean canApplyTo(StylePage page);

    boolean convertsDamage();

    boolean firesOnSwing();

    boolean firesOnHit();

    boolean spawnsWave();
}
