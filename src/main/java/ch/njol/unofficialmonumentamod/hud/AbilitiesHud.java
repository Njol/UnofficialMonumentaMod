package ch.njol.unofficialmonumentamod.hud;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.ModSpriteAtlasHolder;
import ch.njol.minecraft.uiframework.hud.Hud;
import ch.njol.minecraft.uiframework.hud.HudElement;
import ch.njol.unofficialmonumentamod.AbilityHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.options.Options;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class AbilitiesHud extends HudElement {

	private static ModSpriteAtlasHolder atlas;

	private static Identifier COOLDOWN_OVERLAY;
	private static Identifier COOLDOWN_FLASH;
	private static Identifier UNKNOWN_ABILITY_ICON;
	private static Identifier UNKNOWN_CLASS_BORDER;

	private String draggedAbility = null;

	public static AbilitiesHud INSTANCE = new AbilitiesHud();

	private AbilitiesHud() {
	}

	public static void registerSprites() {
		if (atlas == null) {
			atlas = ModSpriteAtlasHolder.createAtlas(UnofficialMonumentaModClient.MOD_IDENTIFIER, "abilities");
		} else {
			atlas.clearSprites();
		}
		COOLDOWN_OVERLAY = atlas.registerSprite("cooldown_overlay");
		COOLDOWN_FLASH = atlas.registerSprite("off_cooldown");
		UNKNOWN_ABILITY_ICON = atlas.registerSprite("unknown_ability");
		UNKNOWN_CLASS_BORDER = atlas.registerSprite("unknown_border");
		List<Identifier> foundIcons = MinecraftClient.getInstance().getResourceManager().findResources("textures/abilities", path -> true)
			                              .keySet().stream()
			                              .filter(id -> id.getNamespace().equals(UnofficialMonumentaModClient.MOD_IDENTIFIER)).toList();
		for (Identifier foundIcon : foundIcons) {
			if (foundIcon == COOLDOWN_OVERLAY || foundIcon == COOLDOWN_FLASH || foundIcon == UNKNOWN_ABILITY_ICON || foundIcon == UNKNOWN_CLASS_BORDER) {
				continue;
			}
			atlas.registerSprite(foundIcon.getPath().substring("textures/abilities/".length(), foundIcon.getPath().length() - ".png".length()));
		}
	}

	public boolean renderInFrontOfChat() {
		return UnofficialMonumentaModClient.options.abilitiesDisplay_inFrontOfChat
			       && !(client.currentScreen instanceof ChatScreen);
	}

	protected boolean isEnabled() {
		return UnofficialMonumentaModClient.options.abilitiesDisplay_enabled && !UnofficialMonumentaModClient.abilityHandler.abilityData.isEmpty();
	}

	@Override
	protected boolean isVisible() {
		return true;
	}

	private int getTotalSize() {
		int numAbilities = (int) UnofficialMonumentaModClient.abilityHandler.abilityData.stream().filter(a -> isAbilityVisible(a, true)).count();
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
	protected ElementPosition getPosition() {
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

		abilityInfos = abilityInfos.stream().filter(a -> isAbilityVisible(a, true)).collect(Collectors.toList());

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

				if (isAbilityVisible(abilityInfo, false)) {
					// some settings are affected by called methods, so set them anew for each ability to render
					RenderSystem.setShader(GameRenderer::getPositionTexProgram);
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
					RenderSystem.enableBlend();
					RenderSystem.defaultBlendFunc();

					if (layer == 0) {

						float animTicks = abilityInfo.offCooldownAnimationTicks + tickDelta;
						float animLength = 2; // half of length actually
						float scaledIconSize = iconSize * (options.abilitiesDisplay_offCooldownResize ? 1 + 0.08f * Utils.smoothStep(1 - Math.abs(animTicks - animLength) / animLength) : 1);
						float scaledX = x - (scaledIconSize - iconSize) / 2;
						float scaledY = y - (scaledIconSize - iconSize) / 2;

						float durationFraction = abilityInfo.initialDuration > 0 && abilityInfo.remainingDuration > 0 ? (abilityInfo.remainingDuration - tickDelta) / abilityInfo.initialDuration : 0;
						if (durationFraction > 0 && options.abilitiesDisplay_durationRenderMode == AbilityHandler.DurationRenderMode.CIRCLE) {
							Utils.drawPartialHollowPolygon(
								matrices,
								(int) scaledX + (iconSize / 2),
								(int) scaledY + (iconSize / 2),
								4,
								((float) iconSize / 2),
								360,
								durationFraction > 0.10 ? 0x00FF00FF : 0xFF0000FF,//If above 10% then green else red
								durationFraction
							);
							//as RenderSystem settings are changed during the circle drawing, need to re-set them.
							RenderSystem.setShader(GameRenderer::getPositionTexProgram);
							RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
							RenderSystem.enableBlend();
							RenderSystem.defaultBlendFunc();
						}

						drawSprite(matrices, getAbilityIcon(abilityInfo), scaledX, scaledY, scaledIconSize, scaledIconSize);


						// silenceCooldownFraction is >= 0 so this is also >= 0
						float cooldownFraction = abilityInfo.initialCooldown <= 0 ? 0 : Math.min(Math.max((abilityInfo.remainingCooldown - tickDelta) / abilityInfo.initialCooldown, silenceCooldownFraction), 1);
						if (cooldownFraction > 0) {
							Sprite cooldownOverlay = atlas.getSprite(COOLDOWN_OVERLAY);
							float yOffset = (cooldownOverlay.getContents().getWidth() - cooldownOverlay.getContents().getHeight()) / 2f;
							drawPartialSprite(matrices, cooldownOverlay, scaledX, scaledY + yOffset, scaledIconSize, scaledIconSize - 2 * yOffset, 0, 1 - cooldownFraction, 1, 1);
						}
						if (options.abilitiesDisplay_offCooldownFlashIntensity > 0 && animTicks < 8) {
							RenderSystem.setShaderColor(1, 1, 1, options.abilitiesDisplay_offCooldownFlashIntensity * (1 - animTicks / 8f));
							drawSprite(matrices, atlas.getSprite(COOLDOWN_FLASH), scaledX, scaledY, scaledIconSize, scaledIconSize);
							RenderSystem.setShaderColor(1, 1, 1, 1);
						}

						drawSprite(matrices, getSpriteOrDefault(getBorderFileIdentifier(abilityInfo.className, abilityHandler.silenceDuration > 0), UNKNOWN_CLASS_BORDER), scaledX, scaledY, scaledIconSize, scaledIconSize);

						//bar looks better on top of the border, that's why we're checking again here
						if (durationFraction > 0 && options.abilitiesDisplay_durationRenderMode == AbilityHandler.DurationRenderMode.BAR) {
							drawDurationBar(matrices, scaledX, scaledY, durationFraction, abilityInfo.className);
						}
					} else {

						if ((abilityInfo.remainingCooldown > 0 || abilityHandler.silenceDuration > 0) && options.abilitiesDisplay_showCooldownAsText) {
							String cooldownString = "" + (int) Math.ceil(Math.max(Math.max(abilityInfo.remainingCooldown, abilityHandler.silenceDuration), 0) / 20f);
							drawOutlinedText(matrices, cooldownString,
								x + iconSize - options.abilitiesDisplay_textOffset - this.client.textRenderer.getWidth(cooldownString),
								y + iconSize - options.abilitiesDisplay_textOffset - this.client.textRenderer.fontHeight,
								textColor);
						}

						if (abilityInfo.maxCharges > 1 || abilityInfo.maxCharges == 1 && abilityInfo.initialCooldown <= 0) {
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

	private void drawDurationBar(MatrixStack matrices, float originX, float originY, float fraction, String className) {

		Options options = UnofficialMonumentaModClient.options;
		int iconSize = options.abilitiesDisplay_iconSize;
		float barHeight = 8 * iconSize / 32f;

		matrices.push();

		boolean horizontal = options.abilitiesDisplay_durationBar_side == Options.DurationBarSideMode.FOLLOW ? options.abilitiesDisplay_horizontal
			                     : options.abilitiesDisplay_durationBar_side == Options.DurationBarSideMode.HORIZONTAL;
		matrices.translate(originX + iconSize / 2f, originY + iconSize / 2f, 0);
		if (!horizontal) {
			matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(90));
		}
		matrices.translate(iconSize / 32f * options.abilitiesDisplay_durationBar_offsetX, iconSize / 32f * options.abilitiesDisplay_durationBar_offsetY - barHeight / 2f, 0);

		Sprite backgroundSprite = getClassDuration(className, "background");
		Sprite barSprite = getClassDuration(className, "full");
		Sprite overlaySprite = getClassDuration(className, "overlay");

		float backgroundWidth = iconSize;
		float barWidth = 1f * barSprite.getContents().getWidth() / backgroundSprite.getContents().getWidth() * backgroundWidth;
		float overlayWidth = 1f * overlaySprite.getContents().getWidth() / backgroundSprite.getContents().getWidth() * backgroundWidth;

		drawSprite(matrices, backgroundSprite, -backgroundWidth / 2, 0, backgroundWidth, barHeight);
		drawPartialSprite(matrices, barSprite, -barWidth / 2, 0, barWidth, barHeight, 0, 0, fraction, 1);
		drawSprite(matrices, overlaySprite, -overlayWidth / 2, 0, overlayWidth, barHeight);

		matrices.pop();
	}

	private static final Pattern IDENTIFIER_SANITATION_PATTERN = Pattern.compile("[^a-zA-Z0-9/._-]");

	private static String sanitizeForIdentifier(String string) {
		return IDENTIFIER_SANITATION_PATTERN.matcher(string).replaceAll("_").toLowerCase(Locale.ROOT);
	}

	private static final Map<String, Identifier> abilityIdentifiers = new HashMap<>();

	private Sprite getClassDuration(String className, String part) {
		String id = className + "/" + className + "_bar_" + part;
		Identifier baseIdentifier = abilityIdentifiers.computeIfAbsent(id, key -> new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, sanitizeForIdentifier(key)));
		Sprite sprite = atlas.getSprite(baseIdentifier);
		if (!sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId())) {
			return sprite;
		}

		Identifier e = abilityIdentifiers.computeIfAbsent("shaman/shaman_bar_" + part, key -> new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, sanitizeForIdentifier(key)));
		return atlas.getSprite(e);
	}

	private Sprite getAbilityIcon(AbilityHandler.AbilityInfo abilityInfo) {
		String id = (abilityInfo.className == null ? "unknown" : abilityInfo.className) + "/" + abilityInfo.name + (abilityInfo.mode == null ? "" : "_" + abilityInfo.mode);

		// for abilities with charges, use a special "_max" sprite when charges are full (and the sprite exists)
		if ((abilityInfo.maxCharges > 1 || abilityInfo.maxCharges == 1 && abilityInfo.initialCooldown <= 0) && abilityInfo.charges == abilityInfo.maxCharges) {
			Identifier maxIdentifier = abilityIdentifiers.computeIfAbsent(id + "_max", key -> new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, sanitizeForIdentifier(key)));
			Sprite sprite = atlas.getSprite(maxIdentifier);
			if (!sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId())) {
				return sprite;
			}
		}

		Identifier baseIdentifier = abilityIdentifiers.computeIfAbsent(id, key -> new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, sanitizeForIdentifier(key)));
		Sprite sprite = atlas.getSprite(baseIdentifier);
		if (!sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId())) {
			return sprite;
		}
		return atlas.getSprite(UNKNOWN_ABILITY_ICON);
	}

	private static final Map<String, Identifier> borderIdentifiers = new HashMap<>();

	private static Identifier getBorderFileIdentifier(String className, boolean silenced) {
		return borderIdentifiers.computeIfAbsent((className == null ? "unknown" : className) + (silenced ? "_silenced" : ""),
			key -> new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER,
				sanitizeForIdentifier(className == null ? "unknown" : className) + "/border" + (silenced ? "_silenced" : "")));
	}

	private Sprite getSpriteOrDefault(Identifier identifier, Identifier defaultIdentifier) {
		Sprite sprite = atlas.getSprite(identifier);
		if (sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId())) {
			return atlas.getSprite(defaultIdentifier);
		}
		return sprite;
	}

	public boolean isAbilityVisible(AbilityHandler.AbilityInfo abilityInfo, boolean forSpaceCalculation) {
		// Passive abilities are visible iff passives are enabled in the options
		if (abilityInfo.initialCooldown == 0 && abilityInfo.maxCharges == 0) {
			return UnofficialMonumentaModClient.options.abilitiesDisplay_showPassiveAbilities;
		}

		// Active abilities take up space even if hidden unless condenseOnlyOnCooldown is enabled
		if (forSpaceCalculation && !UnofficialMonumentaModClient.options.abilitiesDisplay_condenseOnlyOnCooldown) {
			return true;
		}

		// Active abilities are visible with showOnlyOnCooldown iff they are on cooldown or don't have a cooldown (and should have stacks instead)
		return !UnofficialMonumentaModClient.options.abilitiesDisplay_showOnlyOnCooldown
			       || draggedAbility != null
			       || isInEditMode()
			       || abilityInfo.remainingCooldown > 0
			       || abilityInfo.maxCharges > 0 && (abilityInfo.initialCooldown <= 0 || UnofficialMonumentaModClient.options.abilitiesDisplay_alwaysShowAbilitiesWithCharges);
	}

	@Override
	public Hud.ClickResult mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return Hud.ClickResult.NONE;
		}
		if (Screen.hasControlDown()) {
			startDragging(mouseX, mouseY);
		} else {
			AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
			List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
			if (abilityInfos.isEmpty()) {
				return Hud.ClickResult.NONE;
			}
			abilityInfos = abilityInfos.stream().filter(a -> isAbilityVisible(a, true)).collect(Collectors.toList());
			int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY);
			if (index < 0) {
				return Hud.ClickResult.NONE;
			}
			draggedAbility = abilityInfos.get(index).getOrderId();
		}
		return Hud.ClickResult.DRAG;
	}

	private int getClosestAbilityIndex(List<AbilityHandler.AbilityInfo> abilityInfos, double mouseX, double mouseY) {
		if (abilityInfos.isEmpty()) {
			return -1;
		}

		Options options = UnofficialMonumentaModClient.options;
		int iconSize = options.abilitiesDisplay_iconSize;
		int iconGap = options.abilitiesDisplay_iconGap;
		boolean horizontal = options.abilitiesDisplay_horizontal;

		int closestAbilityIndex;
		if (horizontal) {
			closestAbilityIndex = (int) Math.floor((mouseX + iconGap / 2.0) / (iconSize + iconGap));
		} else {
			closestAbilityIndex = (int) Math.floor((mouseY + iconGap / 2.0) / (iconSize + iconGap));
		}
		closestAbilityIndex = Utils.clamp(0, closestAbilityIndex, abilityInfos.size() - 1);

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
		abilityInfos = abilityInfos.stream().filter(a -> isAbilityVisible(a, true)).collect(Collectors.toList());

		int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY);
		if (index < 0) {
			return;
		}

		AbilityHandler.AbilityInfo abilityInfo = abilityInfos.get(index);

		// renderTooltip assumes that the coordinates passed in are absolute...
		Rectangle dimension = getDimension();
		matrices.push();
		matrices.translate(-dimension.x, -dimension.y, 0);
		screen.renderTooltip(matrices, Text.of(abilityInfo.name), mouseX + dimension.x, mouseY + dimension.y);
		matrices.pop();
		// TODO also display ability description?
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
			int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY);
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
			UnofficialMonumentaModClient.saveConfig();
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

	@Override
	protected boolean isClickable(double mouseX, double mouseY) {
		AbilityHandler abilityHandler = UnofficialMonumentaModClient.abilityHandler;
		List<AbilityHandler.AbilityInfo> abilityInfos = abilityHandler.abilityData;
		if (abilityInfos.isEmpty()) {
			return false;
		}
		abilityInfos = abilityInfos.stream().filter(a -> isAbilityVisible(a, true)).collect(Collectors.toList());

		int index = getClosestAbilityIndex(abilityInfos, mouseX, mouseY);
		if (index < 0) {
			return false;
		}
		AbilityHandler.AbilityInfo abilityInfo = abilityInfos.get(index);
		if (!isAbilityVisible(abilityInfo, false)) {
			return false;
		}
		Options options = UnofficialMonumentaModClient.options;
		int iconSize = options.abilitiesDisplay_iconSize;
		int iconOffset = iconSize + options.abilitiesDisplay_iconGap;
		if (options.abilitiesDisplay_horizontal) {
			mouseX -= index * iconOffset;
		} else {
			mouseY -= index * iconOffset;
		}
		return !isPixelTransparent(getAbilityIcon(abilityInfo), mouseX / iconSize, mouseY / iconSize)
			       || !isPixelTransparent(getSpriteOrDefault(getBorderFileIdentifier(abilityInfo.className, abilityHandler.silenceDuration > 0), UNKNOWN_CLASS_BORDER), mouseX / iconSize, mouseY / iconSize);
	}

}
