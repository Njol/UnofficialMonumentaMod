package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.features.misc.SlotLocking;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Inject(method = "handleInputEvents", at = @At("HEAD"), cancellable = true)
	void umm$stopInputs(CallbackInfo ci) {
		SlotLocking.getInstance().onInputEvent(ci);
	}

}
