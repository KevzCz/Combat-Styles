package net.pixeldreamstudios.combat_styles.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.affix.CombatEffectDealer;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.entity.SummonOwner;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentActivation;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRuntime;
import net.pixeldreamstudios.combat_styles.equipment.TriggeredEffect;
import net.pixeldreamstudios.combat_styles.equipment.WornEquipment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DamageBonuses {

    private static final String DEBUG = "damage_bonus";

    private record Cached(long tick, long registry, List<DamageBonus> bonuses) {
    }

    private static final Map<UUID, Cached> EQUIPMENT = new ConcurrentHashMap<>();

    private DamageBonuses() {
    }

    public static void forget(UUID playerId) {
        EQUIPMENT.remove(playerId);
    }

    public static float apply(LivingEntity target, DamageSource source, float amount) {
        if (target == null || source == null || amount <= 0.0F
                || target.level().isClientSide()) {
            return amount;
        }
        Player owner = ownerOf(source);
        if (owner == null || owner == target) {
            return amount;
        }
        List<DamageBonus> equipment = equipmentFor(owner);
        List<DamageBonus> affixes = CombatEffectDealer.attackBonuses(owner, source);
        if (equipment.isEmpty() && affixes.isEmpty()) {
            return amount;
        }
        List<DamageBonus> all = new ArrayList<>(equipment.size() + affixes.size());
        all.addAll(equipment);
        all.addAll(affixes);
        float boosted = DamageBonus.apply(amount, all, source);
        if (boosted != amount && CombatDebug.enabled(owner)) {
            CombatDebug.fired(owner, DEBUG, source.getMsgId() + " " + amount + " -> " + boosted);
        }
        return boosted;
    }

    private static Player ownerOf(DamageSource source) {
        Entity causing = source.getEntity();
        if (causing instanceof Player player) {
            return player;
        }
        Player summoner = SummonOwner.ownerPlayer(causing);
        if (summoner != null) {
            return summoner;
        }
        Entity direct = source.getDirectEntity();
        Player shooter = EquipmentActivation.ownerOf(direct);
        return shooter != null ? shooter : SummonOwner.ownerPlayer(direct);
    }

    private static List<DamageBonus> equipmentFor(Player player) {
        if (EquipmentRegistry.isEmpty()) {
            return List.of();
        }
        long tick = player.level().getGameTime();
        long registry = EquipmentRegistry.generation();
        Cached cached = EQUIPMENT.get(player.getUUID());
        if (cached != null && cached.tick() == tick && cached.registry() == registry) {
            return cached.bonuses();
        }
        List<DamageBonus> found = new ArrayList<>();
        for (WornEquipment worn : WornEquipment.of(player)) {
            EquipmentDefinition definition = worn.definition();
            for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
                TriggeredEffect entry = named.getValue();
                if (!entry.effect().damageBonus().isEmpty()
                        && EquipmentRuntime.liveAtRest(player, definition, worn.copy(),
                                named.getKey(), entry)) {
                    found.addAll(entry.effect().damageBonus());
                }
            }
        }
        List<DamageBonus> bonuses = List.copyOf(found);
        EQUIPMENT.put(player.getUUID(), new Cached(tick, registry, bonuses));
        return bonuses;
    }
}
