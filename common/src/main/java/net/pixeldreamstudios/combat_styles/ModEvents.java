package net.pixeldreamstudios.combat_styles;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.LootEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import net.pixeldreamstudios.combat_styles.damage.DamageBonuses;
import net.pixeldreamstudios.combat_styles.network.ConfigEditHandler;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.network.SyncCooldownsPayload;
import net.pixeldreamstudios.combat_styles.affix.AffixDamageDealer;
import net.pixeldreamstudios.combat_styles.affix.AffixLoader;
import net.pixeldreamstudios.combat_styles.affix.AffixMechanics;
import net.pixeldreamstudios.combat_styles.affix.ProjectileUpkeep;
import net.pixeldreamstudios.combat_styles.attribute.AttackAttributes;
import net.pixeldreamstudios.combat_styles.command.CombatStylesCommand;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.entity.SummonHooks;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentHurt;
import net.pixeldreamstudios.combat_styles.equipment.EffectWindows;
import net.pixeldreamstudios.combat_styles.equipment.Gates;
import net.pixeldreamstudios.combat_styles.equipment.SpellLockState;
import net.pixeldreamstudios.combat_styles.equipment.StateEdges;
import net.pixeldreamstudios.combat_styles.equipment.PlayerTags;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.equipment.BurstSequence;
import net.pixeldreamstudios.combat_styles.equipment.ClientSwing;
import net.pixeldreamstudios.combat_styles.equipment.CombatState;
import net.pixeldreamstudios.combat_styles.equipment.ComboControl;
import net.pixeldreamstudios.combat_styles.equipment.DashGlide;
import net.pixeldreamstudios.combat_styles.equipment.DodgeState;
import net.pixeldreamstudios.combat_styles.equipment.IFrameState;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentAttributes;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentLoader;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRuntime;
import net.pixeldreamstudios.combat_styles.equipment.SubstitutionScope;
import net.pixeldreamstudios.combat_styles.equipment.SwingDrag;
import net.pixeldreamstudios.combat_styles.equipment.SwingHaste;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfiles;
import net.pixeldreamstudios.combat_styles.equipment.WornItems;
import net.pixeldreamstudios.combat_styles.api.CombatScheduler;
import net.pixeldreamstudios.combat_styles.equipment.MissTracker;
import net.pixeldreamstudios.combat_styles.equipment.PendingGrants;
import net.pixeldreamstudios.combat_styles.item.ModItems;
import net.pixeldreamstudios.combat_styles.loot.BookRollLoader;
import net.pixeldreamstudios.combat_styles.loot.EquipmentTagLoader;
import net.pixeldreamstudios.combat_styles.loot.RandomStyleFunction;
import net.pixeldreamstudios.combat_styles.loot.StyleLoot;
import net.pixeldreamstudios.combat_styles.loot.WeaponRollLoader;
import net.pixeldreamstudios.combat_styles.style.DeclaredClaims;
import net.pixeldreamstudios.combat_styles.style.CustomAttackLoader;
import net.pixeldreamstudios.combat_styles.style.CustomStyleLoader;
import net.pixeldreamstudios.combat_styles.style.DefaultStyleLoader;
import net.pixeldreamstudios.combat_styles.style.MovesetProjection;
import net.pixeldreamstudios.combat_styles.style.SwingWindow;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentUse;
import net.pixeldreamstudios.combat_styles.equipment.Cooldowns;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentProviders;
import net.pixeldreamstudios.combat_styles.equipment.PendingExtraHit;
import net.pixeldreamstudios.combat_styles.style.StyleIndexReloader;
import net.pixeldreamstudios.combat_styles.style.StyleConfigs;

public final class ModEvents {

    private static final int COOLDOWN_SYNC_INTERVAL = 5;

    private ModEvents() {
    }

    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                CombatStylesCommand.register(dispatcher));

        ReloadListenerRegistry.register(PackType.SERVER_DATA, new AffixLoader(),
                CombatStyles.id(AffixLoader.DIRECTORY));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new CustomAttackLoader(),
                CombatStyles.id(CustomAttackLoader.DIRECTORY));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new CustomStyleLoader(),
                CombatStyles.id(CustomStyleLoader.DIRECTORY));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new EquipmentLoader(),
                CombatStyles.id(EquipmentLoader.DIRECTORY));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new DefaultStyleLoader(),
                CombatStyles.id(DefaultStyleLoader.DIRECTORY));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new WeaponRollLoader(),
                CombatStyles.id(WeaponRollLoader.DIRECTORY));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new BookRollLoader(),
                CombatStyles.id(BookRollLoader.DIRECTORY));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new EquipmentTagLoader(),
                CombatStyles.id("equipment_tags"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new StyleIndexReloader(),
                CombatStyles.id("style_index"));

        LifecycleEvent.SERVER_STOPPED.register(server -> ServerDataReset.clear());

        LifecycleEvent.SERVER_STARTED.register(server -> {
            StyleConfigs.reloadAll();
            DataIntegrity.verify();
        });

        PlayerEvent.PLAYER_QUIT.register(player -> {
            AffixDamageDealer.forgetHistory(player.getUUID());
            CombatState.forget(player.getUUID());
            CombatScheduler.forget(player.getUUID());
            ClientSwing.forget(player.getUUID());
            ComboControl.forget(player.getUUID());
            PendingGrants.forget(player.getUUID());
            SummonHooks.forget(player.getUUID());
            SummonProfiles.forget(player.getUUID());
            AttackAttributes.forget(player.getUUID());
            AffixMechanics.forget(player.getUUID());
            EquipmentRuntime.forget(player.getUUID());
            SubstitutionScope.forget(player.getUUID());
            EquipmentHurt.forget(player.getUUID());
            EffectWindows.forget(player.getUUID());
            EquipmentAttributes.forget(player.getUUID());
            DodgeState.forget(player.getUUID());
            IFrameState.forget(player.getUUID());
            DashGlide.forget(player.getUUID());
            SwingHaste.forget(player.getUUID());
            SwingDrag.forgetSweep(player.getUUID());
            WornItems.forget(player.getUUID());
            DeclaredClaims.forget(player.getUUID());
            SpellLockState.forget(player.getUUID());
            StateEdges.forget(player.getUUID());
            PlayerTags.forget(player.getUUID());
            EquipmentUse.forget(player.getUUID());
            SwingWindow.forget(player.getUUID());
            PendingExtraHit.forget(player.getUUID());
            NetworkManager.forgetCooldownSync(player.getUUID());
            DamageBonuses.forget(player.getUUID());
            CombatDebug.forget(player.getUUID());
            BurstSequence.forget(player.getUUID());
        });

        TickEvent.SERVER_POST.register(server -> {
            EquipmentProviders.advance();
            ConfigEditHandler.flush(server);
            long gameTime = server.overworld().getGameTime();
            AffixMechanics.tickExpiries(gameTime);
            AffixDamageDealer.tickPending(gameTime);
            CombatScheduler.tick(gameTime, id -> server.getPlayerList().getPlayer(id));
            ProjectileUpkeep.tick();
            EquipmentRuntime.tickCooldowns(gameTime);
            MissTracker.tick(gameTime);
            DodgeState.tick(gameTime);
            IFrameState.tick(gameTime);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                DashGlide.tick(player);
                EquipmentRuntime.tickHolds(player);
                Gates.tick(player);
                StateEdges.tick(player);
                MovesetProjection.project(player);
                EquipmentAttributes.sync(player);
                SummonHooks.keepAlive(player);
                if (SpellEngineCompat.isLoaded()) {
                    SpellLockState.update(player);
                }
                boolean windows = Gates.consumeChanged(player);
                boolean combat = CombatState.consumeChanged(player);
                boolean tags = PlayerTags.consumeChanged(player);
                boolean cooldowns = Cooldowns.consumeChanged(player);
                if (windows || combat || tags || cooldowns
                        || gameTime % COOLDOWN_SYNC_INTERVAL == 0L) {
                    NetworkManager.syncCooldowns(player, SyncCooldownsPayload.NO_PROC);
                }
            }
        });

        LootEvent.MODIFY_LOOT_TABLE.register((key, context, builtin) -> {
            if (!builtin || key == null || !StyleLoot.isEligible(key.location())) {
                return;
            }
            float chance = (float) StyleLoot.chanceFor(key.location());
            context.addPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .when(LootItemRandomChanceCondition.randomChance(chance))
                    .add(LootItem.lootTableItem(ModItems.STYLE_PAGE.get())
                            .apply(RandomStyleFunction.builder())));
        });
    }
}
