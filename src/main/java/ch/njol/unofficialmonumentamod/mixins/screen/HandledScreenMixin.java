package ch.njol.unofficialmonumentamod.mixins.screen;

import ch.njol.unofficialmonumentamod.features.misc.SlotLocking;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
	@Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
	void umm$onSlotClicked(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
		HandledScreen $this = (HandledScreen) (Object) this;
		
		if (SlotLocking.getInstance().onSlotClicked($this, slot, slotId, button, actionType)) {
			ci.cancel();
		}
	}
	
	@Inject(method = "render", at = @At("TAIL"))
	void umm$onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		SlotLocking.getInstance().tickRender(matrices, mouseX, mouseY);
	}

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;isPointOverSlot(Lnet/minecraft/screen/slot/Slot;DD)Z",
					shift = Shift.BEFORE
			),
			locals = LocalCapture.CAPTURE_FAILSOFT
	)
	private void umm$afterDrawnSlot(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci, int i, int j, MatrixStack matrixStack, int k, Slot slot) {
		RenderSystem.disableDepthTest();
		HandledScreen $this = (HandledScreen) (Object) this;
		SlotLocking.getInstance().drawSlot($this, matrices, slot);
		RenderSystem.enableDepthTest();
	}
}
