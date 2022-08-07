package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.hud.Hud;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

	@Unique
	private final Hud hud = Hud.INSTANCE;

	protected ChatScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "mouseClicked(DDI)Z", at = @At("RETURN"), cancellable = true)
	void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			return;
		}
		cir.setReturnValue(hud.mouseClicked(this, mouseX, mouseY, button));
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (hud.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (hud.mouseReleased(mouseX, mouseY, button)) {
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Inject(method = "removed()V", at = @At("HEAD"))
	public void removed(CallbackInfo ci) {
		hud.removed();
	}

	@Redirect(method = "render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;getText(DD)Lnet/minecraft/text/Style;"))
	public Style render(ChatHud instance, double x, double y, MatrixStack matrices, int mouseX, int mouseY, float delta) {
		Style style = instance.getText(x, y);
		if (style != null && style.getHoverEvent() != null) {
			return style;
		}
		hud.renderTooltip(this, matrices, mouseX, mouseY);
		return style;
	}

}
