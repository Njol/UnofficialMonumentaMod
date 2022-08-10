package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
	void disconnect(Screen screen, CallbackInfo ci) {
		UnofficialMonumentaModClient.onDisconnect();
	}

	@Inject(method = "<init>(Lnet/minecraft/client/RunArgs;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourcePackManager;createResourcePacks()Ljava/util/List;", shift = At.Shift.AFTER))
	void construct_setupAtlases(RunArgs args, CallbackInfo ci) {
//		AbiltiesHud.reloadIcons();
//		ModSpriteAtlasHolder.registerSprites((MinecraftClient) (Object) this);
	}

	@Inject(method = "reloadResources(Z)Ljava/util/concurrent/CompletableFuture;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourcePackManager;createResourcePacks()Ljava/util/List;", shift = At.Shift.AFTER))
	void reloadResources(boolean force, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
//		AbiltiesHud.reloadIcons();
	}

}
