package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.hud.Hud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Shadow
	private int scaledWidth;

	@Shadow
	private int scaledHeight;

	@Unique
	private final Hud hud = Hud.INSTANCE;

	@Unique
	private float tickDelta;

	@Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V",
		at = @At(value = "HEAD"))
	void render_head(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		this.tickDelta = tickDelta;
		hud.updateScreenSize(scaledWidth, scaledHeight);
	}

	@Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusEffectOverlay(Lnet/minecraft/client/util/math/MatrixStack;)V", shift = At.Shift.BEFORE))
	void renderSkills_beforeStatusEffects(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		if (!hud.abilties.renderInFrontOfChat()) { // TODO test if necessary / adapt alpha test stuff
			hud.abilties.renderAbsolute(matrices, tickDelta);
		}
	}

	@Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Scoreboard;getObjectiveForSlot(I)Lnet/minecraft/scoreboard/ScoreboardObjective;", shift = At.Shift.BEFORE),
		slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/util/math/MatrixStack;I)V")))
	void renderSkills_afterChat(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		if (hud.abilties.renderInFrontOfChat()) {
			hud.abilties.renderAbsolute(matrices, tickDelta);
		}
	}

	@Inject(method = "renderExperienceBar(Lnet/minecraft/client/util/math/MatrixStack;I)V",
		at = @At(value = "HEAD"), cancellable = true)
	void renderExperienceBar(MatrixStack matrices, int x, CallbackInfo ci) {
		if (UnofficialMonumentaModClient.options.hud_enabled) {
//			hud.experience.renderAbsolute(matrices, tickDelta, scaledWidth, scaledHeight);
			ci.cancel();
		}
	}

	@Inject(method = "renderStatusBars(Lnet/minecraft/client/util/math/MatrixStack;)V",
		at = @At(value = "HEAD"), cancellable = true)
	void renderStatusBars(MatrixStack matrices, CallbackInfo ci) {
		if (UnofficialMonumentaModClient.options.hud_enabled) {
			hud.health.renderAbsolute(matrices, tickDelta);
//			hud.hunger.renderAbsolute(matrices, tickDelta);
//			hud.air.renderAbsolute(matrices, tickDelta);
			// armor is useless in Monumenta, so no HUD element for that // TODO make one anyway? even in vanilla it doesn't really matter though
			ci.cancel();
		}
	}

	// TODO renderHeldItemTooltip

	/**
	 * If configured, do not show ability messages
	 * TODO the messages here are translated, so only work for English. Maybe just check for the ability name in the message? (assuming that one isn't translated as well...)
	 * or maybe send something to the server to make it not display these any more?
	 */
	@Inject(method = "setOverlayMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
	void setOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
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
