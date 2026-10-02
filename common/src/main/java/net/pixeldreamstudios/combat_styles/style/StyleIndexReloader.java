package net.pixeldreamstudios.combat_styles.style;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import net.pixeldreamstudios.combat_styles.DataIntegrity;
import net.pixeldreamstudios.combat_styles.config.EquipmentDropConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class StyleIndexReloader implements PreparableReloadListener {

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier,
                                          ResourceManager manager,
                                          ProfilerFiller prepareProfiler,
                                          ProfilerFiller applyProfiler,
                                          Executor prepareExecutor,
                                          Executor applyExecutor) {
        return CompletableFuture.completedFuture((Void) null)
                .thenCompose(barrier::wait)
                .thenRunAsync(() -> {
                    AttackOverrides.reload();
                    CustomAttackConfig.reload();
                    CustomStyleConfig.reload();
                    EquipmentDropConfig.load();
                    StyleRegistry.rebuild();
                    DefaultStyleConfig.reload();
                    DataIntegrity.verify();
                }, applyExecutor);
    }
}
