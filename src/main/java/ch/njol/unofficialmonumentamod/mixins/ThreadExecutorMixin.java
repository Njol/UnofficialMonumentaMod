package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Objects;

@Mixin(ThreadExecutor.class)
public abstract class ThreadExecutorMixin<R extends Runnable> {

    /**
     *  Silences errors that happen when a player leaves monumenta or changes shard.
     */
    @Inject(method = "executeTask", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Lorg/slf4j/Marker;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void silenceTeamFatal(R task, CallbackInfo ci, Exception exception) {
        if (UnofficialMonumentaModClient.options.silenceTeamErrors) {
            if ((Objects.equals(exception.getMessage(), "Player is either on another team or not on any team. Cannot remove from team 'players'.") || Objects.equals(exception.getMessage(), "Cannot invoke \"net.minecraft.scoreboard.Team.getName()\" because \"team\" is null")))
                ci.cancel();
        }
    }
}
