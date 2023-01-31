package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.features.misc.SlotLocking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
	void disconnect(Screen screen, CallbackInfo ci) {
		UnofficialMonumentaModClient.onDisconnect();
	}
	
	@Inject(method = "handleInputEvents", at = @At("HEAD"), cancellable = true)
	void umm$stopInputs(CallbackInfo ci) {
		SlotLocking.getInstance().onInputEvent(ci);
	}

}
