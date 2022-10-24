package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.misc.Calculator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.Selectable;
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
    @Shadow abstract protected <T extends Element & Selectable> T addSelectableChild(T child);

    @Inject(at=@At("HEAD"), method = "render")
    private void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Calculator.tick();
        Calculator.INSTANCE.render(matrices, mouseX, mouseY, delta);
    }

    @Inject(at=@At("HEAD"), method = "close")
    private void onClose(CallbackInfo ci) {
        Calculator.INSTANCE.onClose();
    }

    @Inject(at=@At("TAIL"), method = "init(Lnet/minecraft/client/MinecraftClient;II)V")
    private void onInit(MinecraftClient client, int width, int height, CallbackInfo ci) {
        if (Calculator.INSTANCE.shouldRender()) {
            Calculator.INSTANCE.init();

            this.addSelectableChild(Calculator.INSTANCE.changeMode);
            for (TextFieldWidget widget: Calculator.INSTANCE.children) {
                this.addSelectableChild(widget);
            }
        }
    }
}
