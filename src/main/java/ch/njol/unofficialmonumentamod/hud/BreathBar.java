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
import net.minecraft.tag.FluidTags;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class BreathBar extends HudElement {

	private static final int HEIGHT = 32;
	private static final int MARGIN = 16;

	private static Identifier BACKGROUND, BREATH, WATER_BREATHING, OVERLAY;

	public BreathBar(Hud hud) {
		super(hud);
	}

	public static void registerSprites(Function<String, Identifier> register) {
		BACKGROUND = register.apply("breath/background");
		BREATH = register.apply("breath/breath");
		WATER_BREATHING = register.apply("breath/water_breathing");
		OVERLAY = register.apply("breath/overlay");
	}

	@Override
	protected boolean isEnabled() {
		return UnofficialMonumentaModClient.options.hud_enabled && UnofficialMonumentaModClient.options.hud_statusBarsEnabled;
	}

	@Override
	protected boolean isVisible() {
		PlayerEntity player = getCameraPlayer();
		return player != null && (player.getAir() < player.getMaxAir()
			                          || player.isSubmergedIn(FluidTags.WATER) && (!UnofficialMonumentaModClient.options.hud_hideBreathWithWaterBreathing || !player.hasStatusEffect(StatusEffects.WATER_BREATHING)));
	}

	@Override
	protected int getWidth() {
		Sprite barSprite = ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BREATH);
		return 2 * MARGIN + (int) (1.0 * barSprite.getWidth() * HEIGHT / barSprite.getHeight());
	}

	@Override
	protected int getHeight() {
		return HEIGHT;
	}

	@Override
	protected Options.Position getPosition() {
		return UnofficialMonumentaModClient.options.hud_breathBarPosition;
	}

	@Override
	protected int getZOffset() {
		return 1;
	}

	private PlayerEntity getCameraPlayer() {
		return !(this.client.getCameraEntity() instanceof PlayerEntity) ? null : (PlayerEntity) this.client.getCameraEntity();
	}

	private float easedAir = -1;

	@Override
	protected void render(MatrixStack matrices, float tickDelta) {

		PlayerEntity player = getCameraPlayer();
		if (player == null) {
			return;
		}

		int width = getWidth();
		int barWidth = width - 2 * MARGIN;

		if (UnofficialMonumentaModClient.options.hud_breathMirror) {
			matrices.push();
			matrices.translate(width, 0, 0);
			matrices.scale(-1, 1, 1);
			RenderSystem.disableCull();
		}

		drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BACKGROUND), 0, 0, width, HEIGHT);

		int air = Utils.clamp(0, player.getAir(), player.getMaxAir());
		float lastFrameDuration = client.getLastFrameDuration() / 20;
		easedAir = easedAir < 0 ? air : Utils.ease(air, easedAir, 6 * lastFrameDuration, 6 * lastFrameDuration);

		boolean hasWaterBreathing = player.hasStatusEffect(StatusEffects.WATER_BREATHING);
		drawPartialSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(hasWaterBreathing ? WATER_BREATHING : BREATH),
			MARGIN, 0, barWidth, HEIGHT,
			0, 0, 1f * air / player.getMaxAir(), 1);

		drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(OVERLAY),
			0, 0, width, HEIGHT);

		if (UnofficialMonumentaModClient.options.hud_breathMirror) {
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
