package ch.njol.unofficialmonumentamod.mixins.screen;

import ch.njol.unofficialmonumentamod.features.calculator.Calculator;
import ch.njol.unofficialmonumentamod.features.calculator.CalculatorWidget;
import ch.njol.unofficialmonumentamod.features.misc.SlotLocking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractParentElement {
	@Shadow protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement);

	@Unique
	void initializeWidget() {
		//initialize calculator widget if it should be added.
		if (Calculator.INSTANCE.shouldRender()) {
			CalculatorWidget calculator = new CalculatorWidget((Screen) (Object) this);
			calculator.init(CalculatorWidget.getMode());
			Calculator.lastWidgetInitialized = calculator;
			addDrawableChild(calculator);
		}
	}

	@Inject(at = @At("HEAD"), method = "close")
	void onClose(CallbackInfo ci) {
		//remove calculator from opened screen.
		if (Calculator.lastWidgetInitialized != null) {
			Calculator.lastWidgetInitialized.onParentClosed();
			Calculator.lastWidgetInitialized = null;
		}
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
		initializeWidget();
	}
	@Inject(at = @At("TAIL"), method = "resize")
	void onResize(MinecraftClient client, int width, int height, CallbackInfo ci) {
		initializeWidget();
	}
}
