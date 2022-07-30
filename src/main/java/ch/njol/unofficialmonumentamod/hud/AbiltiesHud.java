package ch.njol.unofficialmonumentamod.hud;

import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.options.Options;
import ch.njol.unofficialmonumentamod.options.Options.Position;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AbiltiesHud extends HudElement {

	private static final Identifier COOLDOWN_OVERLAY = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/abilities/cooldown_overlay.png");
	private static final Identifier COOLDOWN_FLASH = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/abilities/off_cooldown.png");
	private static final Identifier UNKNOWN_ABILITY_ICON = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/abilities/unknown_ability.png");
	private static final Identifier UNKNOWN_CLASS_BORDER = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/abilities/unknown_border.png");

	private String draggedAbility = null;

	public AbiltiesHud(Hud hud) {
		super(hud);
	}

	public boolean renderInFrontOfChat() {
		return UnofficialMonumentaModClient.options.abilitiesDisplay_inFrontOfChat
			       && !(client.currentScreen instanceof ChatScreen);
	}

	protected boolean isEnabled() {
		return UnofficialMonumentaModClient.options.abilitiesDisplay_enabled && !UnofficialMonumentaModClient.abilityHandler.abilityData.isEmpty();
	}

	private int getTotalSize() {
		int numAbilities = UnofficialMonumentaModClient.abilityHandler.abilityData.size();
		return UnofficialMonumentaModClient.options.abilitiesDisplay_iconSize * numAbilities + UnofficialMonumentaModClient.options.abilitiesDisplay_iconGap * (numAbilities - 1);
	}

	@Override
	protected int getWidth() {
		return UnofficialMonumentaModClient.options.abilitiesDisplay_horizontal ? getTotalSize() : UnofficialMonumentaModClient.options.abilitiesDisplay_iconSize;
	}

	@Override
	protected int getHeight() {
		return UnofficialMonumentaModClient.options.abilitiesDisplay_horizontal ? UnofficialMonumentaModClient.options.abilitiesDisplay_iconSize : getTotalSize();
	}

	@Override
	protected int getZOffset() {
		return renderInFrontOfChat() ? 100 : 0;
	}

	@Override
	protected Position getPosition() {
		return UnofficialMonumentaModClient.options.abilitiesDisplay_position;
	}

	@Override
	protected void render(MatrixStack matrices, float tickDelta) {
		if (client.options.hudHidden || client.player == null || client.player.isSpectator()) {
			return;
		}
		Options options = UnofficialMonumentaModClient.options;

		AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
		List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
		if (abilityInfos.isEmpty()) {
			return;
		}

		// NB: this code is partially duplicated in ChatScreenMixin!

		if (options.abilitiesDisplay_condenseOnlyOnCooldown) {
			abilityInfos = abilityInfos.stream().filter(this::isAbilityVisible).collect(Collectors.toList());
		}

		int iconSize = options.abilitiesDisplay_iconSize;
		int iconGap = options.abilitiesDisplay_iconGap;

		boolean horizontal = options.abilitiesDisplay_horizontal;

		int totalSize = getTotalSize();

		boolean ascendingRenderOrder = options.abilitiesDisplay_ascendingRenderOrder;
		int textColor = 0xFF000000 | options.abilitiesDisplay_textColorRaw;

		float silenceCooldownFraction = abilityHandler.initialSilenceDuration <= 0 || abilityHandler.silenceDuration <= 0 ? 0 : 1f * abilityHandler.silenceDuration / abilityHandler.initialSilenceDuration;

		// multiple passes to render multiple layers.
		// layer 0: textures
		// layer 1: numbers
		for (int layer = 0; layer < 2; layer++) {

			int x = 0;
			int y = 0;
			if (!ascendingRenderOrder) {
				if (horizontal) {
					x += totalSize - iconSize;
				} else {
					y += totalSize - iconSize;
				}
			}

			for (int i = 0; i < abilityInfos.size(); i++) {
				AbilityHandler.AbilityInfo abilityInfo = abilityInfos.get(ascendingRenderOrder ? i : abilityInfos.size() - 1 - i);

				if (isAbilityVisible(abilityInfo)) {
					// some settings are affected by called methods, so set them anew for each ability to render
					RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
					RenderSystem.enableRescaleNormal();
					RenderSystem.enableBlend();
					RenderSystem.defaultBlendFunc();
					RenderSystem.enableAlphaTest();

					if (layer == 0) {

						float animTicks = abilityInfo.offCooldownAnimationTicks + tickDelta;
						float animLength = 2; // half of length actually
						float scaledIconSize = iconSize * (options.abilitiesDisplay_offCooldownResize ? 1 + 0.08f * Utils.smoothStep(1 - Math.abs(animTicks - animLength) / animLength) : 1);
						float scaledX = x - (scaledIconSize - iconSize) / 2;
						float scaledY = y - (scaledIconSize - iconSize) / 2;

						bindTextureOrDefault(getAbilityFileIdentifier(abilityInfo.className, abilityInfo.name), UNKNOWN_ABILITY_ICON);
						drawTextureSmooth(matrices, scaledX, scaledY, scaledIconSize, scaledIconSize);

						// silenceCooldownFraction is >= 0 so this is also >= 0
						float cooldownFraction = abilityInfo.initialCooldown <= 0 ? 0 : Math.min(Math.max((abilityInfo.remainingCooldown - tickDelta) / abilityInfo.initialCooldown, silenceCooldownFraction), 1);
						if (cooldownFraction > 0) {
							// cooldown overlay is a series of 16 images, starting will full cooldown at the top, and successive shorter cooldowns below.
							final int numCooldownTextures = 16;
							int cooldownTextureIndex = (int) Math.floor((1 - cooldownFraction) * numCooldownTextures);
							client.getTextureManager().bindTexture(COOLDOWN_OVERLAY);
							drawTextureSmooth(matrices,
								scaledX, scaledY, scaledIconSize, scaledIconSize,
								0, 1, 1f * cooldownTextureIndex / numCooldownTextures, 1f * (cooldownTextureIndex + 1) / numCooldownTextures);
						}
						if (options.abilitiesDisplay_offCooldownFlashIntensity > 0 && animTicks < 8) {
							this.client.getTextureManager().bindTexture(COOLDOWN_FLASH);
							RenderSystem.color4f(1, 1, 1, options.abilitiesDisplay_offCooldownFlashIntensity * (1 - animTicks / 8f));
							drawTextureSmooth(matrices, scaledX, scaledY, scaledIconSize, scaledIconSize);
							RenderSystem.color4f(1, 1, 1, 1);
						}

						bindTextureOrDefault(getBorderFileIdentifier(abilityInfo.className), UNKNOWN_CLASS_BORDER);
						drawTextureSmooth(matrices, scaledX, scaledY, scaledIconSize, scaledIconSize);

					} else {

						if ((abilityInfo.remainingCooldown > 0 || abilityHandler.silenceDuration > 0) && options.abilitiesDisplay_showCooldownAsText) {
							String cooldownString = "" + (int) Math.ceil(Math.max(Math.max(abilityInfo.remainingCooldown, abilityHandler.silenceDuration), 0) / 20f);
							drawOutlinedText(matrices, cooldownString,
								x + iconSize - options.abilitiesDisplay_textOffset - this.client.textRenderer.getWidth(cooldownString),
								y + iconSize - options.abilitiesDisplay_textOffset - this.client.textRenderer.fontHeight,
								textColor);
						}

						if (abilityInfo.maxCharges > 1) {
							drawOutlinedText(matrices, "" + abilityInfo.charges, x + options.abilitiesDisplay_textOffset, y + options.abilitiesDisplay_textOffset, textColor);
						}

					}
				}

				if (horizontal) {
					x += (ascendingRenderOrder ? 1 : -1) * (iconSize + iconGap);
				} else {
					y += (ascendingRenderOrder ? 1 : -1) * (iconSize + iconGap);
				}

			}
		}
	}

	private static final Map<String, Identifier> abilityIdentifiers = new HashMap<>();

	// TODO use Sprites instead for animation and a neat isPixelTransparent() method
	private static Identifier getAbilityFileIdentifier(String className, String name) {
		return abilityIdentifiers.computeIfAbsent((className == null ? "unknown" : className) + "/" + name, key -> new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER,
			"textures/abilities/" + key.replaceAll("[^a-zA-Z0-9/._-]", "_").toLowerCase(Locale.ROOT) + ".png"));
	}

	private static final Map<String, Identifier> borderIdentifiers = new HashMap<>();

	private static Identifier getBorderFileIdentifier(String className) {
		return borderIdentifiers.computeIfAbsent(className == null ? "unknown" : className, key -> new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER,
			"textures/abilities/" + key.replaceAll("[^a-zA-Z0-9/._-]", "_").toLowerCase(Locale.ROOT) + "/border.png"));
	}

	public boolean isAbilityVisible(AbilityHandler.AbilityInfo abilityInfo) {
		// abilities are visible with showOnlyOnCooldown IFF they are on cooldown or don't have a cooldown (and should have stacks instead)
		return !UnofficialMonumentaModClient.options.abilitiesDisplay_showOnlyOnCooldown
			       || draggedAbility != null
			       || abilityInfo.remainingCooldown > 0
			       || abilityInfo.maxCharges > 0 && (abilityInfo.initialCooldown <= 0 || UnofficialMonumentaModClient.options.abilitiesDisplay_alwaysShowAbilitiesWithCharges);
	}

	@Override
	Hud.ClickResult mouseClicked(double mouseX, double mouseY, int button) {
		AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
		List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
		if (abilityInfos.isEmpty()) {
			return Hud.ClickResult.NONE;
		}
		if (UnofficialMonumentaModClient.options.abilitiesDisplay_condenseOnlyOnCooldown) {
			abilityInfos = abilityInfos.stream().filter(this::isAbilityVisible).collect(Collectors.toList());
		}

		int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY, true);
		if (index < 0) {
			return Hud.ClickResult.NONE;
		}
		if (Screen.hasControlDown()) {
			return super.mouseClicked(mouseX, mouseY, button);
		} else {
			draggedAbility = abilityInfos.get(index).getOrderId();
		}
		return Hud.ClickResult.DRAG;
	}

	private int getClosestAbilityIndex(List<AbilityHandler.AbilityInfo> abilityInfos, double mouseX, double mouseY, boolean initialClick) {

		int x = 0;
		int y = 0;

		Options options = UnofficialMonumentaModClient.options;
		int iconSize = options.abilitiesDisplay_iconSize;
		int iconGap = options.abilitiesDisplay_iconGap;
		boolean horizontal = options.abilitiesDisplay_horizontal;

		int closestAbilityIndex;
		if (horizontal) {
			closestAbilityIndex = (int) Math.floor((mouseX - x + iconGap / 2.0) / (iconSize + iconGap));
		} else {
			closestAbilityIndex = (int) Math.floor((mouseY - y + iconGap / 2.0) / (iconSize + iconGap));
		}
		closestAbilityIndex = Math.max(0, Math.min(closestAbilityIndex, abilityInfos.size() - 1));
		if (initialClick) {
			// on first click make sure we're sufficiently close to the ability icon
			final double clickableFraction = 1; // textures are actually smaller than the whole icon size, so only make a part clickable
			double abiCenterX = (horizontal ? x + closestAbilityIndex * (iconSize + iconGap) : x) + iconSize / 2.0;
			double abiCenterY = (horizontal ? y : y + closestAbilityIndex * (iconSize + iconGap)) + iconSize / 2.0;
			if (Math.abs(abiCenterX - mouseX) > iconSize / 2.0 * clickableFraction
				    || Math.abs(abiCenterY - mouseY) > iconSize / 2.0 * clickableFraction) {
				return -1;
			}
		}
		return closestAbilityIndex;
	}

	public void renderTooltip(Screen screen, MatrixStack matrices, int mouseX, int mouseY) {
		if (!UnofficialMonumentaModClient.options.abilitiesDisplay_enabled
			    || !UnofficialMonumentaModClient.options.abilitiesDisplay_tooltips
			    || dragging
			    || draggedAbility != null) {
			return;
		}
		AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
		List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
		if (abilityInfos.isEmpty()) {
			return;
		}
		if (UnofficialMonumentaModClient.options.abilitiesDisplay_condenseOnlyOnCooldown) {
			abilityInfos = abilityInfos.stream().filter(this::isAbilityVisible).collect(Collectors.toList());
		}

		int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY, true);
		if (index < 0) {
			return;
		}

		AbilityHandler.AbilityInfo abilityInfo = abilityInfos.get(index);
		screen.renderTooltip(matrices, Text.of(abilityInfo.name), mouseX, mouseY);
		// TODO also display ability description
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (dragging) {
			return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		} else if (draggedAbility != null) {
			AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
			List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
			if (abilityInfos.isEmpty()) {
				return false;
			}
			int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY, false);
			if (index < 0) {
				return false;
			}
			String abilityAtCurrentPos = abilityInfos.get(index).getOrderId();
			if (abilityAtCurrentPos.equals(draggedAbility)) {
				return false;
			}
			List<String> order = new ArrayList<>(UnofficialMonumentaModClient.options.abilitiesDisplay_order);
			int currentAbiOrderIndex = order.indexOf(abilityAtCurrentPos);
			if (currentAbiOrderIndex < 0) // shouldn't happen
			{
				return false;
			}
			order.remove(draggedAbility);
			order.add(currentAbiOrderIndex, draggedAbility);
			UnofficialMonumentaModClient.options.abilitiesDisplay_order = order;
			abilityHandler.sortAbilities();
			return true;
		}
		return false;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragging) {
			super.mouseReleased(mouseX, mouseY, button);
		} else if (draggedAbility != null) {
			draggedAbility = null;
			UnofficialMonumentaModClient.saveConfig();
			return true;
		}
		return false;
	}

	public void removed() {
		super.removed();
		draggedAbility = null;
	}
}
