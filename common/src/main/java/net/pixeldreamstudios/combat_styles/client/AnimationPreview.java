package net.pixeldreamstudios.combat_styles.client;

import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.equipment.PoseTuning;

public final class AnimationPreview {

    private final ModifierLayer<KeyframeAnimationPlayer> layer = new ModifierLayer<>();
    private String currentAnimation;
    private PoseTuning tuning = PoseTuning.NONE;
    private boolean available = true;
    private float age;

    public ModifierLayer<KeyframeAnimationPlayer> layer() {
        return layer;
    }

    public float age() {
        return age;
    }

    public boolean isAvailable() {
        return available && CombatStyles.isPlayerAnimatorLoaded();
    }

    public boolean isPlaying() {
        KeyframeAnimationPlayer player = layer.getAnimation();
        return player != null && player.isActive();
    }

    public String currentAnimation() {
        return currentAnimation;
    }

    public void select(String animationId) {
        if (animationId == null || animationId.isBlank()) {
            stop();
            currentAnimation = null;
            return;
        }
        if (animationId.equals(currentAnimation)) {
            return;
        }
        currentAnimation = animationId;
        play();
    }

    public void select(String animationId, PoseTuning poseTuning) {
        PoseTuning chosen = poseTuning == null ? PoseTuning.NONE : poseTuning;
        boolean sameAnimation = animationId != null && animationId.equals(currentAnimation);
        if (sameAnimation && chosen.equals(tuning)) {
            return;
        }
        tuning = chosen;
        if (animationId == null || animationId.isBlank()) {
            stop();
            currentAnimation = null;
            return;
        }
        currentAnimation = animationId;
        play();
    }

    public PoseTuning tuning() {
        return tuning;
    }

    public void play() {
        if (!isAvailable() || currentAnimation == null) {
            return;
        }
        KeyframeAnimation animation = resolve(currentAnimation);
        if (animation == null) {
            available = false;
            layer.setAnimation(null);
            return;
        }
        layer.setAnimation(new KeyframeAnimationPlayer(PoseTuner.tuned(animation, tuning)));
        double speed = CombatStylesConfig.get().animationPreviewSpeed();
        if (Math.abs(speed - 1.0D) > 0.001D) {
            layer.addModifierLast(new SpeedModifier((float) speed));
        }
    }

    public void stop() {
        layer.setAnimation(null);
    }

    public void tick() {
        age++;
        if (!isAvailable()) {
            return;
        }
        layer.tick();
        KeyframeAnimationPlayer player = layer.getAnimation();
        if (player != null && !player.isActive()) {
            play();
        }
    }

    private KeyframeAnimation resolve(String animationId) {
        ResourceLocation parsed = ResourceLocation.tryParse(animationId);
        if (parsed == null) {
            return null;
        }
        KeyframeAnimation direct = lookup(parsed);
        if (direct != null) {
            return direct;
        }
        int slash = parsed.getPath().lastIndexOf('/');
        if (slash >= 0 && slash < parsed.getPath().length() - 1) {
            return lookup(ResourceLocation.fromNamespaceAndPath(
                    parsed.getNamespace(), parsed.getPath().substring(slash + 1)));
        }
        return null;
    }

    private KeyframeAnimation lookup(ResourceLocation location) {
        try {
            return PlayerAnimationRegistry.getAnimation(location) instanceof KeyframeAnimation animation
                    ? animation
                    : null;
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.debug("Animation preview lookup failed for {}", location, throwable);
            return null;
        }
    }
}
