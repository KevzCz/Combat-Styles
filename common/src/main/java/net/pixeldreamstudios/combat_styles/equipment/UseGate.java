package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.mojang.datafixers.util.Either;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record UseGate(boolean enabled, UsePhase phase,
                      Optional<Integer> heldAbove, Optional<Integer> heldBelow) {

    public static final UseGate ANY = new UseGate(false, UsePhase.ANY,
            Optional.empty(), Optional.empty());

    public static final MapCodec<UseGate> EXPANDED = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("enabled", Boolean.TRUE)
                            .forGetter(UseGate::enabled),
                    UsePhase.CODEC.optionalFieldOf("phase", UsePhase.ANY)
                            .forGetter(UseGate::phase),
                    Codec.INT.optionalFieldOf("held_above").forGetter(UseGate::heldAbove),
                    Codec.INT.optionalFieldOf("held_below").forGetter(UseGate::heldBelow)
            ).apply(instance, UseGate::new));

    public static final Codec<UseGate> CODEC = Codec.either(EXPANDED.codec(), Codec.BOOL)
            .xmap(either -> either.map(value -> value, UseGate::of),
                    Either::left);

    public static UseGate of(boolean enabled) {
        return new UseGate(enabled, UsePhase.ANY, Optional.empty(), Optional.empty());
    }

    public UseGate {
        phase = phase == null ? UsePhase.ANY : phase;
    }

    public boolean isEmpty() {
        return !enabled && phase == UsePhase.ANY
                && heldAbove.isEmpty() && heldBelow.isEmpty();
    }

    public boolean instant() {
        return phase == UsePhase.START || phase == UsePhase.RELEASE;
    }

    public boolean heldNow(Player player, EquipmentDefinition definition) {
        if (isEmpty()) {
            return true;
        }
        if (instant() || player == null || !player.isUsingItem() || definition == null) {
            return false;
        }
        ItemStack using = player.getUseItem();
        EquipmentDefinition used = EquipmentProviders.definitionFor(using);
        boolean own = used != null && used.id().equals(definition.id());
        if (!own && !EquipmentUse.declaresUse(using)) {
            return false;
        }
        int held = player.getTicksUsingItem();
        return heldAbove.map(bound -> held > bound).orElse(true)
                && heldBelow.map(bound -> held < bound).orElse(true);
    }

    public String rejects(EquipmentContext context) {
        if (isEmpty()) {
            return null;
        }
        UseEvent used = context.used();
        if (used == null) {
            return "on_use (this is not a use event)";
        }
        if (phase == UsePhase.ANY) {
            if (used.phase() != used.completion()) {
                return "on_use (waiting for " + used.completion().serializedName() + ")";
            }
        } else if (phase != used.phase()) {
            return "on_use phase (" + used.phase().serializedName() + ")";
        }
        if (heldAbove.isPresent() && used.heldTicks() <= heldAbove.get()) {
            return "held_above (held " + used.heldTicks() + ")";
        }
        if (heldBelow.isPresent() && used.heldTicks() >= heldBelow.get()) {
            return "held_below (held " + used.heldTicks() + ")";
        }
        return null;
    }
}
