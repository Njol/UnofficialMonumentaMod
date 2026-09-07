package ch.njol.unofficialmonumentamod.mixins.screen;

import ch.njol.unofficialmonumentamod.core.gui.InventoryWidget;
import ch.njol.unofficialmonumentamod.features.misc.SlotLocking;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
	@Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
	void umm$onSlotClicked(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
		HandledScreen<?> $this = (HandledScreen<?>) (Object) this;
		
		if (SlotLocking.getInstance().onSlotClicked($this, slot, slotId, button, actionType)) {
			ci.cancel();
		}
	}
	
	@Inject(method = "render", at = @At("TAIL"))
	void umm$onRender(DrawContext drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		SlotLocking.getInstance().tickRender(drawContext, mouseX, mouseY);
	}

	//allows the pass-through of mouseReleased events to inventory widgets.
	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	void umm$handledScreen$onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		HandledScreen<?> $this = (HandledScreen<?>) (Object) this;

		if($this.getFocused() instanceof InventoryWidget) {
			if ($this.getFocused().mouseReleased(mouseX, mouseY, button)) {
				cir.setReturnValue(true);
			}
		}
	}

	//allows the pass-through of mouseDragged events to inventory widgets.
	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	void umm$handledScreen$onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
		HandledScreen<?> $this = (HandledScreen<?>) (Object) this;

		if($this.getFocused() instanceof InventoryWidget) {
			if ($this.getFocused().mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
				cir.setReturnValue(true);
			}
		}
	}

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V",
					shift = Shift.AFTER
			),
			locals = LocalCapture.CAPTURE_FAILSOFT
	)
	private void umm$afterDrawnSlot(DrawContext drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci, int i, int j, int k, Slot slot) {
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		HandledScreen<?> $this = (HandledScreen<?>) (Object) this;
		SlotLocking.getInstance().drawSlot($this, drawContext, slot);
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
	}
}
