package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.affix.DamageTypeMatch;

import java.util.List;
import java.util.Optional;

public record BlockGate(boolean enabled, List<String> damageTypes,
                        Optional<Boolean> fullyBlocked) {

    public static final BlockGate ANY = new BlockGate(false, List.of(), Optional.empty());

    public static final Codec<BlockGate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", Boolean.TRUE).forGetter(BlockGate::enabled),
            Codec.STRING.listOf().optionalFieldOf("taken_damage_type", List.of())
                    .forGetter(BlockGate::damageTypes),
            Codec.BOOL.optionalFieldOf("fully_blocked").forGetter(BlockGate::fullyBlocked)
    ).apply(instance, BlockGate::new));

    public BlockGate {
        damageTypes = damageTypes == null ? List.of() : List.copyOf(damageTypes);
    }

    public boolean isEmpty() {
        return !enabled && damageTypes.isEmpty() && fullyBlocked.isEmpty();
    }

    public String rejects(EquipmentContext context) {
        if (isEmpty()) {
            return null;
        }
        BlockEvent blocked = context.blocked();
        if (blocked == null) {
            return "shield_block (this is not a block event)";
        }
        if (!DamageTypeMatch.matches(damageTypes, blocked.source())) {
            return "taken_damage_type";
        }
        if (fullyBlocked.isPresent() && fullyBlocked.get() != blocked.fullyBlocked()) {
            return "fully_blocked";
        }
        return null;
    }
}
