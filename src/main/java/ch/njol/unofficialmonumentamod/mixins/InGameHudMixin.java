package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.shard.ShardData;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import ch.njol.unofficialmonumentamod.hud.strike.ChestCountOverlay;
import ch.njol.unofficialmonumentamod.hud.AbilitiesHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.MessageType;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Unique
	private final AbilitiesHud abiltiesHud = AbilitiesHud.INSTANCE;

	@Inject(method = "setOverlayMessage", at = @At("TAIL"))
	public void onActionbar(Text message, boolean tinted, CallbackInfo ci) {
		ChestCountOverlay.INSTANCE.onActionbarReceived(message);
	}

	@Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusEffectOverlay(Lnet/minecraft/client/util/math/MatrixStack;)V", shift = At.Shift.BEFORE))
	void renderSkills_beforeStatusEffects(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		if (!abiltiesHud.renderInFrontOfChat()) {
			UnofficialMonumentaModClient.effectOverlay.renderAbsolute(matrices, tickDelta);
			ChestCountOverlay.INSTANCE.renderAbsolute(matrices, tickDelta);
			abiltiesHud.renderAbsolute(matrices, tickDelta);
		}
	}

	@Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Scoreboard;getObjectiveForSlot(I)Lnet/minecraft/scoreboard/ScoreboardObjective;", shift = At.Shift.BEFORE),
		slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/util/math/MatrixStack;I)V")))
	void renderSkills_afterChat(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		if (abiltiesHud.renderInFrontOfChat()) {
			UnofficialMonumentaModClient.effectOverlay.renderAbsolute(matrices, tickDelta);
			ChestCountOverlay.INSTANCE.renderAbsolute(matrices, tickDelta);
			abiltiesHud.renderAbsolute(matrices, tickDelta);
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

	@Unique
	private final String NPCNamePattern = "\\[\\w*] ";

	@Inject(method = "addChatMessage", at = @At("TAIL"))
	private void umm$newChatMessageListener(MessageType type, Text message, UUID sender, CallbackInfo ci) {
		if (!Locations.getShortShard().equals("isles") || !UnofficialMonumentaModClient.options.enableDelveRecognition) {
			return;	//stop it on triggering either on another shard (outside dev environment) than a possible one or if the feature is disabled
		}

		String npcText = message.getString();
		String npcMessage = npcText.replaceFirst(NPCNamePattern, "");
		if (npcMessage.equals(npcText)) {
			return;//if it's not given by an NPC, it should not be used.
		}

		for (Map.Entry<String, ShardData.Shard> entry: ShardData.getShards().entrySet()) {
			if (!entry.getValue().canBeDelveBounty) {
				continue;//skip if shard cannot be a delve bounty
			}

			TranslatableText translatedText = new TranslatableText("unofficial-monumenta-mod.delvebounty." + entry.getKey().toLowerCase());

			if (Objects.equals(npcMessage, translatedText.getString())) {
				MutableText text = new TranslatableText("unofficial-monumenta-mod.delvebounty.newBountyMessage")
						.setStyle(Style.EMPTY.withColor(Formatting.GOLD))
						.append(new LiteralText(entry.getValue().officialName).setStyle(Style.EMPTY.withColor(Formatting.RED)));
				MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
				break;
			}
		}
	}

}
