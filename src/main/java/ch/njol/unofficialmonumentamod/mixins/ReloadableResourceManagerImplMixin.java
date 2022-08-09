package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.misc.managers.ItemNameSpoofer;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourceReloadListener;
import net.minecraft.resource.ResourceReloadMonitor;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableResourceManagerImpl.class)
public class ReloadableResourceManagerImplMixin {

    /**
     *  Loads the reloadable resources of the mod.
     */
    @Inject(at = @At("HEAD"), method = "beginReloadInner")
    private void reloadResources(Executor prepareExecutor, Executor applyExecutor, List<ResourceReloadListener> listeners, CompletableFuture<Unit> initialStage, CallbackInfoReturnable<ResourceReloadMonitor> cir) {
        UnofficialMonumentaModClient.LOGGER.info("Loading resources.");
        UnofficialMonumentaModClient.locations.load();
        ItemNameSpoofer.load();
        UnofficialMonumentaModClient.LOGGER.info("Finished Loading resources");
    }
}
