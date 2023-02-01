package ch.njol.unofficialmonumentamod.mixins.screen;

import ch.njol.unofficialmonumentamod.features.calculator.Calculator;
import java.util.List;

import ch.njol.unofficialmonumentamod.features.misc.SlotLocking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractParentElement {
	@Shadow @Final private List<Selectable> selectables;

	@Shadow @Final private List<Element> children;

	@Inject(at = @At("HEAD"), method = "render")
	void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		//run injected calculator if exists.
		Calculator.tick();
		Calculator.INSTANCE.render(matrices, mouseX, mouseY, delta);
	}

	@Inject(at = @At("HEAD"), method = "close")
	void onClose(CallbackInfo ci) {
		//uninject the calculator from the current opened screen
		Calculator.INSTANCE.onClose();
	}

	@Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
	void onKeyTyped(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		Screen $this = (Screen) (Object) this;
		
		if (Calculator.INSTANCE.keyTyped(keyCode, scanCode, modifiers)) {
			cir.setReturnValue(true);
		}
		
		SlotLocking.getInstance().onKeyboardInput($this, keyCode, scanCode, modifiers, cir);
	}

	@Inject(at = @At("TAIL"), method = "init(Lnet/minecraft/client/MinecraftClient;II)V")
	void onInit(MinecraftClient client, int width, int height, CallbackInfo ci) {
		//"inject" the calculator
		if (Calculator.INSTANCE.shouldRender()) {
			Calculator.INSTANCE.init();

			//as addSelectableChild is broken when trying to shadow or invoke it, replace it by its content.
			this.selectables.add(Calculator.INSTANCE.changeMode);
			this.children.add(Calculator.INSTANCE.changeMode);
			for (TextFieldWidget widget : Calculator.INSTANCE.children) {
				this.selectables.add(widget);
				this.children.add(widget);
			}
		}
	}
}
