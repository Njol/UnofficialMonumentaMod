package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.options.Options;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Mixin(InGameHud.class)
public class InGameHudMixin extends DrawableHelper {

	private static final Identifier COOLDOWN_OVERLAY = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/abilities/cooldown_overlay.png");
	private static final Identifier UNKNOWN_ABILITY_ICON = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/abilities/unknown_ability.png");
	private static final Identifier UNKNOWN_CLASS_BORDER = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/abilities/unknown_border.png");

	@Shadow
	@Final
	private MinecraftClient client;

	@Shadow
	private int scaledWidth;

	@Shadow
	private int scaledHeight;

	@Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusEffectOverlay(Lnet/minecraft/client/util/math/MatrixStack;)V", shift = At.Shift.BEFORE))
	void renderSkills(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		if (this.client.options.hudHidden)
			return;

		Options options = UnofficialMonumentaModClient.options;
		if (!options.abilitiesDisplay_enabled)
			return;

		AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
		List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
		if (abilityInfos.isEmpty())
			return;

		int iconSize = options.abilitiesDisplay_iconSize;
		int iconGap = options.abilitiesDisplay_iconGap;

		boolean horizontal = options.abilitiesDisplay_horizontal;
		float align = options.abilitiesDisplay_align;

		int totalSize = iconSize * abilityInfos.size() + iconGap * (abilityInfos.size() - 1);

		boolean ascendingRenderOrder = options.abilitiesDisplay_ascendingRenderOrder;

		float silenceCooldownFraction = abilityHandler.initialSilenceDuration <= 0 ? 0 : 1f * abilityHandler.silenceDuration / abilityHandler.initialSilenceDuration;

		// multiple passes to render multiple layers.
		// layer 0: textures
		// layer 1: numbers
		for (int layer = 0; layer < 2; layer++) {

			int x = Math.round(this.scaledWidth * options.abilitiesDisplay_offsetXRelative) + options.abilitiesDisplay_offsetXAbsolute;
			int y = Math.round(this.scaledHeight * options.abilitiesDisplay_offsetYRelative) + options.abilitiesDisplay_offsetYAbsolute;
			if (horizontal)
				x -= align * totalSize;
			else
				y -= align * totalSize;
			if (!ascendingRenderOrder) {
				if (horizontal)
					x += totalSize - iconSize;
				else
					y += totalSize - iconSize;
			}

			for (int i = 0; i < abilityInfos.size(); i++) {
				AbilityHandler.AbilityInfo abilityInfo = abilityInfos.get(ascendingRenderOrder ? i : abilityInfos.size() - 1 - i);

				float cooldownFraction = Math.min(Math.max((abilityInfo.remainingCooldown - tickDelta) / abilityInfo.initialCooldown, silenceCooldownFraction), 1);
				boolean visible = !options.abilitiesDisplay_showOnlyOnCooldown || cooldownFraction > 0;
				if (visible) {
					// some settings are affected by called methods, so set them anew for each ability to render
					RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
					RenderSystem.enableRescaleNormal();
					RenderSystem.enableBlend();
					RenderSystem.defaultBlendFunc();
					RenderSystem.enableAlphaTest();

					if (layer == 0) {

						bindTextureOrDefault(getAbilityFileIdentifier(abilityInfo.className, abilityInfo.name), UNKNOWN_ABILITY_ICON);
						drawTexture(matrices, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);

						if (cooldownFraction > 0) {
							// cooldown overlay is a series of 16 images, starting will full cooldown at the top, and successive shorter cooldowns below.
							final int numCooldownTextures = 16;
							int cooldownTextureIndex = (int) Math.floor((1 - cooldownFraction) * numCooldownTextures);
							this.client.getTextureManager().bindTexture(COOLDOWN_OVERLAY);
							drawTexture(matrices, x, y, 0, cooldownTextureIndex * iconSize, iconSize, iconSize, iconSize, iconSize * numCooldownTextures);
						}

						bindTextureOrDefault(getBorderFileIdentifier(abilityInfo.className), UNKNOWN_CLASS_BORDER);
						drawTexture(matrices, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);

					} else {

						int textColor = getTextColor(abilityInfo.className);

						if (cooldownFraction > 0 && options.abilitiesDisplay_showCooldownAsText) {
							String cooldownString = "" + (int) Math.ceil(Math.max(abilityInfo.remainingCooldown, abilityHandler.silenceDuration) / 20f);
							drawText(cooldownString,
									x + iconSize - options.abilitiesDisplay_textOffset - this.client.textRenderer.getWidth(cooldownString),
									y + iconSize - options.abilitiesDisplay_textOffset - this.client.textRenderer.fontHeight,
									textColor);
						}

						if (abilityInfo.maxCharges > 1) {
							drawText("" + abilityInfo.charges, x + options.abilitiesDisplay_textOffset, y + options.abilitiesDisplay_textOffset, textColor);
						}

					}
				}

				if (horizontal)
					x += (ascendingRenderOrder ? 1 : -1) * (iconSize + iconGap);
				else
					y += (ascendingRenderOrder ? 1 : -1) * (iconSize + iconGap);

			}
		}
	}

	private void bindTextureOrDefault(Identifier identifier, Identifier defaultIdentifier) {
		this.client.getTextureManager().bindTexture(identifier);
		AbstractTexture texture = this.client.getTextureManager().getTexture(identifier);
		if (texture == null || texture == MissingSprite.getMissingSpriteTexture()) {
			this.client.getTextureManager().bindTexture(defaultIdentifier);
		}
	}

	private static final Map<String, Identifier> abilityIdentifiers = new HashMap<>();

	private static Identifier getAbilityFileIdentifier(String className, String name) {
		return abilityIdentifiers.computeIfAbsent((className == null ? "unknown" : className) + "/" + name, key -> new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER,
				"textures/abilities/" + key.replaceAll("[^a-zA-Z0-9/._-]", "_").toLowerCase(Locale.ROOT) + ".png"));
	}

	private static final Map<String, Identifier> borderIdentifiers = new HashMap<>();

	private static Identifier getBorderFileIdentifier(String className) {
		return borderIdentifiers.computeIfAbsent(className == null ? "unknown" : className, key -> new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER,
				"textures/abilities/" + key.replaceAll("[^a-zA-Z0-9/._-]", "_").toLowerCase(Locale.ROOT) + "/border.png"));
	}

	/**
	 * Draws text with a full black border around it
	 */
	private void drawText(String text, int x, int y, int color) {
		MatrixStack matrixStack = new MatrixStack();
		matrixStack.translate(0, 0, getZOffset());
		VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
		this.client.textRenderer.draw(text, x - 1, y, 0, false, matrixStack.peek().getModel(), immediate, false, 0, 15728880);
		this.client.textRenderer.draw(text, x, y - 1, 0, false, matrixStack.peek().getModel(), immediate, false, 0, 15728880);
		this.client.textRenderer.draw(text, x + 1, y, 0, false, matrixStack.peek().getModel(), immediate, false, 0, 15728880);
		this.client.textRenderer.draw(text, x, y + 1, 0, false, matrixStack.peek().getModel(), immediate, false, 0, 15728880);
		matrixStack.translate(0, 0, 0.03f);
		this.client.textRenderer.draw(text, x, y, color, false, matrixStack.peek().getModel(), immediate, false, 0, 15728880);
		immediate.draw();
	}

	private static int getTextColor(String className) {
		if (className == null) {
			return 0xFFFFFF;
		}
		// TODO set colours
		switch (className) {
			case "Mage":
				return 0xca54d5;
			case "Rogue":
				return 0xdddddd; // good for readability of chat, looks gray though
			case "Warlock":
				return 0xeeeeee; // not too good
			case "Warrior":
				return 0xcccccc;
			case "Cleric":
				return 0xbbbbbb;
			case "Scout":
				return 0xFFFFFF;
			case "Alchemist":
				return 0xFFFFFF;
		}
		// TODO depths classes
		return 0xFFFFFF;
	}

	/**
	 * If configured, do not show ability messages
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
				|| StringUtils.startsWithIgnoreCase(m, "Rage:")) {
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
