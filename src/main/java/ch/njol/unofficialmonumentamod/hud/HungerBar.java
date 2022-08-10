package ch.njol.unofficialmonumentamod.hud;

import ch.njol.unofficialmonumentamod.ModSpriteAtlasHolder;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.options.Options;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Function;

public class HungerBar extends HudElement {

	private static final int HEIGHT = 32;
	private static final int MARGIN = 16;

	private static Identifier BACKGROUND, BAR_HUNGER, BAR_DECAY, BAR_SATURATION,
		OVERLAY, OVERLAY_DECAY, OVERLAY_SATURATION;
	private static NavigableMap<Integer, Identifier> LEVEL_OVERLAYS, LEVEL_OVERLAYS_DECAY;

	public HungerBar(Hud hud) {
		super(hud);
	}

	public static void registerSprites(Function<String, Identifier> register) {
		BACKGROUND = register.apply("hunger/background");
		BAR_HUNGER = register.apply("hunger/hunger");
		BAR_DECAY = register.apply("hunger/decay");
		BAR_SATURATION = register.apply("hunger/saturation");
		OVERLAY = register.apply("hunger/overlay");
		OVERLAY_DECAY = register.apply("hunger/overlay_decay");
		OVERLAY_SATURATION = register.apply("hunger/overlay_saturation");
		LEVEL_OVERLAYS = ModSpriteAtlasHolder.findLevelledSprites("hud", "hunger", "overlay_", register);
		LEVEL_OVERLAYS_DECAY = ModSpriteAtlasHolder.findLevelledSprites("hud", "hunger", "overlay_decay_", register);
	}

	@Override
	protected boolean isEnabled() {
		return UnofficialMonumentaModClient.options.hud_enabled && UnofficialMonumentaModClient.options.hud_statusBarsEnabled;
	}

	@Override
	protected boolean isVisible() {
		return true;
	}

	@Override
	protected int getWidth() {
		Sprite barSprite = ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BAR_HUNGER);
		return 2 * MARGIN + (int) (1.0 * barSprite.getWidth() * HEIGHT / barSprite.getHeight());
	}

	@Override
	protected int getHeight() {
		return HEIGHT;
	}

	@Override
	protected Options.Position getPosition() {
		return UnofficialMonumentaModClient.options.hud_hungerBarPosition;
	}

	@Override
	protected int getZOffset() {
		return 1;
	}

	private PlayerEntity getCameraPlayer() {
		return !(this.client.getCameraEntity() instanceof PlayerEntity) ? null : (PlayerEntity) this.client.getCameraEntity();
	}

	private float easedHunger = -1;
	private float easedSaturation = -1;


	@Override
	protected void render(MatrixStack matrices, float tickDelta) {

		PlayerEntity player = getCameraPlayer();
		if (player == null) {
			return;
		}

		int width = getWidth();
		int barWidth = width - 2 * MARGIN;

		if (UnofficialMonumentaModClient.options.hud_hungerMirror) {
			matrices.push();
			matrices.translate(width, 0, 0);
			matrices.scale(-1, 1, 1);
			RenderSystem.disableCull();
		}

		drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BACKGROUND), 0, 0, width, HEIGHT);

		int hunger = Utils.clamp(0, player.getHungerManager().getFoodLevel(), 20);
		float saturation = Utils.clamp(0, player.getHungerManager().getSaturationLevel(), 20);
		float lastFrameDuration = client.getLastFrameDuration() / 20;
		easedHunger = Utils.ease(hunger, easedHunger, 6 * lastFrameDuration, 6 * lastFrameDuration);
		easedSaturation = Utils.ease(saturation, easedSaturation, 6 * lastFrameDuration, 6 * lastFrameDuration);

		boolean hasDecay = player.hasStatusEffect(StatusEffects.HUNGER);
		drawPartialSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(hasDecay ? BAR_DECAY : BAR_HUNGER),
			MARGIN, 0, barWidth, HEIGHT,
			0, 0, easedHunger / 20, 1);
		drawPartialSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BAR_SATURATION),
			MARGIN, 0, barWidth, HEIGHT,
			0, 0, easedSaturation / 20, 1);

		drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(hasDecay ? OVERLAY_DECAY : OVERLAY), 0, 0, width, HEIGHT);

		Map.Entry<Integer, Identifier> levelOverlay = (hasDecay ? LEVEL_OVERLAYS_DECAY : LEVEL_OVERLAYS).floorEntry(hunger);
		if (levelOverlay != null) {
			drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(levelOverlay.getValue()), 0, 0, width, HEIGHT);
		}

		if (easedSaturation > 0) {
			drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(OVERLAY_SATURATION), 0, 0, width, HEIGHT);
		}

//		// TODO text options: no text at all, with saturation, current as % (with or without saturation)
//		String fullText = "" + hunger;
//		drawOutlinedText(matrices, fullText, width / 2 - client.textRenderer.getWidth(fullText) / 2, HEIGHT / 2 - client.textRenderer.fontHeight / 2 + UnofficialMonumentaModClient.options.hud_hungerTextOffset, 0xFFFFFFFF);

		if (UnofficialMonumentaModClient.options.hud_hungerMirror) {
			matrices.pop();
			RenderSystem.enableCull();
		}
	}

	@Override
	protected boolean isClickable(double mouseX, double mouseY) {
		return !isPixelTransparent(ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BACKGROUND), mouseX, mouseY)
			       || !isPixelTransparent(ModSpriteAtlasHolder.HUD_ATLAS.getSprite(OVERLAY), mouseX, mouseY);
	}

}
