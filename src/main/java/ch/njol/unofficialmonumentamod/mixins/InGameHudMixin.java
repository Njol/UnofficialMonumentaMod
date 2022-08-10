package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.hud.Hud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Shadow
	private int scaledWidth;

	@Shadow
	private int scaledHeight;

	@Shadow
	private @Nullable Text overlayMessage;
	@Shadow
	@Final
	private MinecraftClient client;
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
		if (!hud.abilities.renderInFrontOfChat()) {
			hud.abilities.renderAbsolute(matrices, tickDelta);
		}
	}

	@Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Scoreboard;getObjectiveForSlot(I)Lnet/minecraft/scoreboard/ScoreboardObjective;", shift = At.Shift.BEFORE),
		slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/util/math/MatrixStack;I)V")))
	void renderSkills_afterChat(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		if (hud.abilities.renderInFrontOfChat()) {
			hud.abilities.renderAbsolute(matrices, tickDelta);
		}
	}

	// TODO XP bar? at least to be able to move it around?
//	@Inject(method = "renderExperienceBar(Lnet/minecraft/client/util/math/MatrixStack;I)V",
//		at = @At(value = "HEAD"), cancellable = true)
//	void renderExperienceBar(MatrixStack matrices, int x, CallbackInfo ci) {
//		if (UnofficialMonumentaModClient.options.hud_enabled) {
//			hud.experience.renderAbsolute(matrices, tickDelta, scaledWidth, scaledHeight);
//			ci.cancel();
//		}
//	}

	// TODO mount jump bar? (in vanilla, it replaces the xp bar)

	@Inject(method = "renderStatusBars(Lnet/minecraft/client/util/math/MatrixStack;)V",
		at = @At(value = "HEAD"), cancellable = true)
	void renderStatusBars(MatrixStack matrices, CallbackInfo ci) {
		if (UnofficialMonumentaModClient.options.hud_enabled
			    && UnofficialMonumentaModClient.options.hud_statusBarsEnabled) {
			hud.health.renderAbsolute(matrices, tickDelta);
			hud.hunger.renderAbsolute(matrices, tickDelta);
			hud.breath.renderAbsolute(matrices, tickDelta);
			// armor is useless in Monumenta, so no HUD element for that // TODO make one anyway? even in vanilla it doesn't really matter though
			ci.cancel();
		}
	}

	@Inject(method = "renderMountHealth(Lnet/minecraft/client/util/math/MatrixStack;)V",
		at = @At(value = "HEAD"), cancellable = true)
	void renderMountHealth(MatrixStack matrices, CallbackInfo ci) {
		if (UnofficialMonumentaModClient.options.hud_enabled
			    && UnofficialMonumentaModClient.options.hud_mountHealthEnabled) {
			hud.mountHealthBar.renderAbsolute(matrices, tickDelta);
			ci.cancel();
		}
	}

	@Inject(method = "renderHeldItemTooltip(Lnet/minecraft/client/util/math/MatrixStack;)V",
		at = @At(value = "HEAD"))
	void renderHeldItemTooltip_head(MatrixStack matrices, CallbackInfo ci) {
		if (UnofficialMonumentaModClient.options.hud_enabled
			    && UnofficialMonumentaModClient.options.hud_moveHeldItemTooltip) {
			Rectangle position = hud.heldItemTooltip.getDimension();
			// NB: Minecraft will write the text at the following coordinates, so need to subtract that from the translation
			int x = (this.scaledWidth - position.width) / 2;
			int y = this.scaledHeight - 59;
			if (!this.client.interactionManager.hasStatusBars()) {
				y += 14;
			}
			matrices.push();
			matrices.translate(position.x - x, position.y - y, 0);
		}
	}

	@Inject(method = "renderHeldItemTooltip(Lnet/minecraft/client/util/math/MatrixStack;)V",
		at = @At(value = "RETURN"))
	void renderHeldItemTooltip_return(MatrixStack matrices, CallbackInfo ci) {
		if (UnofficialMonumentaModClient.options.hud_enabled
			    && UnofficialMonumentaModClient.options.hud_moveHeldItemTooltip) {
			matrices.pop();
		}
	}

	@Redirect(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V"),
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;overlayRemaining:I", ordinal = 0),
			to = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;titleTotalTicks:I", ordinal = 0)))
	void render_overlayMessage_translate(MatrixStack instance, double x, double y, double z) {
		if (!UnofficialMonumentaModClient.options.hud_enabled
			    || !UnofficialMonumentaModClient.options.hud_moveOverlayMessage) {
			instance.translate(x, y, z);
			return;
		}
		Rectangle position = hud.overlayMessage.getDimension();
		// NB: Minecraft will write the text at (-textWidth/2, -4), so need to subtract that from the translation
		instance.translate(position.x + position.width / 2f, position.y + 4, z);
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
