package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.features.misc.managers.MessageNotifier;
import ch.njol.unofficialmonumentamod.hud.strike.ChestCountOverlay;
import ch.njol.unofficialmonumentamod.hud.AbilitiesHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Unique
	private final AbilitiesHud abilitiesHud = AbilitiesHud.INSTANCE;

	@Inject(method = "setOverlayMessage", at = @At("TAIL"))
	public void onActionbar(Text message, boolean tinted, CallbackInfo ci) {
		ChestCountOverlay.INSTANCE.onActionbarReceived(message);
	}

	@Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusEffectOverlay(Lnet/minecraft/client/util/math/MatrixStack;)V", shift = At.Shift.BEFORE))
	void renderSkills_beforeStatusEffects(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		if (!abilitiesHud.renderInFrontOfChat()) {
			UnofficialMonumentaModClient.effectOverlay.renderAbsolute(matrices, tickDelta);
			ChestCountOverlay.INSTANCE.renderAbsolute(matrices, tickDelta);
			MessageNotifier.getInstance().renderAbsolute(matrices, tickDelta);
			abilitiesHud.renderAbsolute(matrices, tickDelta);
		}
	}

	@Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Scoreboard;getObjectiveForSlot(I)Lnet/minecraft/scoreboard/ScoreboardObjective;", shift = At.Shift.BEFORE),
		slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/util/math/MatrixStack;III)V")))
	void renderSkills_afterChat(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		if (abilitiesHud.renderInFrontOfChat()) {
			UnofficialMonumentaModClient.effectOverlay.renderAbsolute(matrices, tickDelta);
			ChestCountOverlay.INSTANCE.renderAbsolute(matrices, tickDelta);
			MessageNotifier.getInstance().renderAbsolute(matrices, tickDelta);
			abilitiesHud.renderAbsolute(matrices, tickDelta);
		}
	}

	/**
	 * If configured, do not show ability messages
	 * TODO The messages here are translated, so only work for English. Maybe just check for the ability name in the message? (assuming that one isn't translated as well...)
	 * TODO Or maybe make this an option server-side? That would probably be the best option. Maybe even make the client mod send this request.
	 */
	@Inject(method = "setOverlayMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
	void setOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
		if (message == null) {
			return;
		}
		synchronized (UnofficialMonumentaModClient.abilityHandler) {
			if (!UnofficialMonumentaModClient.options.abilitiesDisplay_hideAbilityRelatedMessages
				    || !UnofficialMonumentaModClient.options.abilitiesDisplay_enabled
				    || UnofficialMonumentaModClient.abilityHandler.abilityData.isEmpty()) {
				return;
			}
			String m = message.getString();
			if (StringUtils.startsWithIgnoreCase(m, "You are silenced")
				    || StringUtils.startsWithIgnoreCase(m, "All your cooldowns have been reset")
				    || StringUtils.startsWithIgnoreCase(m, "Cloak stacks:")
				    || StringUtils.startsWithIgnoreCase(m, "Rage:")
				    || StringUtils.startsWithIgnoreCase(m, "Holy energy radiates from your hands")
				    || StringUtils.startsWithIgnoreCase(m, "The light from your hands fades")) {
				ci.cancel();
				return;
			}
			for (AbilityHandler.AbilityInfo abilityInfo : UnofficialMonumentaModClient.abilityHandler.abilityData) {
				if (StringUtils.startsWithIgnoreCase(m, abilityInfo.name + " is now off cooldown")
					    || StringUtils.startsWithIgnoreCase(m, abilityInfo.name + " has been activated")
					    || StringUtils.startsWithIgnoreCase(m, abilityInfo.name + " stacks")
					    || StringUtils.startsWithIgnoreCase(m, abilityInfo.name + " charges")) {
					ci.cancel();
					return;
				}
			}
		}
	}

}
