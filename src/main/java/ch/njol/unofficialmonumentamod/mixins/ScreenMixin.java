package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.misc.Calculator;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin implements ParentElement {
    @Shadow protected abstract <T extends Element> T addChild(T child);

    /**
     *  Used to render the calculator injection
     */
    @Inject(at=@At("HEAD"), method = "render")
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Calculator.renderer.render(matrices, mouseX, mouseY, delta);
    }

    /**
     *  Used to remove the calculator injection
     */
    @Inject(at=@At("HEAD"), method = "onClose")
    private void onClose(CallbackInfo ci) {
        Calculator.renderer.onClose();
    }

    /**
     *  Injects the calculator
     */
    @Inject(at=@At("TAIL"), method = "init()V")
    private void onInit(CallbackInfo ci) {
        if (Calculator.renderer.shouldRender()) {
            Calculator.renderer.init();

            this.addChild(Calculator.renderer.changeMode);
            for (TextFieldWidget widget: Calculator.renderer.children) {
                this.addChild(widget);
            }
        }
    }
}
