package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.mc.MonumentaModResourceReloader;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Final
	@Shadow private ReloadableResourceManagerImpl resourceManager;

	@Shadow
	@Nullable
	public ClientPlayerEntity player;


	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
	void disconnect(Screen screen, CallbackInfo ci) {
		UnofficialMonumentaModClient.onDisconnect();
	}


	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;addResourcePackProfilesToManager(Lnet/minecraft/resource/ResourcePackManager;)V"))
	void init(RunArgs args, CallbackInfo ci) {
		resourceManager.registerReloader(new MonumentaModResourceReloader());
	}

	@Inject(method = "reloadResources(Z)Ljava/util/concurrent/CompletableFuture;", at = @At("TAIL"))
	void postReloadedResources(boolean force, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		//will trigger on F3 + T, resource pack menu close, /reload command, etc...
		MonumentaModResourceReloader.onPostReload();
	}
}
