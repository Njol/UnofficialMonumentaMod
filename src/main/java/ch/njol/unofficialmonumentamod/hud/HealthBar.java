package ch.njol.unofficialmonumentamod.hud;

import ch.njol.unofficialmonumentamod.ModSpriteAtlasHolder;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.options.Options;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.util.function.Function;

public class HealthBar extends HudElement {

	private static final int HEIGHT = 32;
	private static final int MARGIN = 16;

	private static Identifier BACKGROUND, OVERLAY;
	private static Identifier HEALTH_LEFT, HEALTH, HEALTH_RIGHT, HIT_HEALTH, HIT_HEALTH_RIGHT;
	private static Identifier ABSORPTION_LEFT, ABSORPTION, ABSORPTION_RIGHT;
	private static Identifier POISON_LEFT, POISON, POISON_RIGHT, HIT_POISON, HIT_POISON_RIGHT;
	private static Identifier REGENERATION;
	private static Identifier WITHER;

	private static final DecimalFormat SINGLE_DIGIT = new DecimalFormat("0.0");
	private static final DecimalFormat OPTIONAL_SINGLE_DIGIT = new DecimalFormat("0.#");

	public HealthBar(Hud hud) {
		super(hud);
	}

	public static void registerSprites(Function<String, Identifier> register) {
		BACKGROUND = register.apply("health/background");
		OVERLAY = register.apply("health/overlay");
		HEALTH_LEFT = register.apply("health/health_left");
		HEALTH = register.apply("health/health");
		HEALTH_RIGHT = register.apply("health/health_right");
		HIT_HEALTH = register.apply("health/health_damage");
		HIT_HEALTH_RIGHT = register.apply("health/health_damage_right");
		POISON_LEFT = register.apply("health/poison_left");
		POISON = register.apply("health/poison");
		POISON_RIGHT = register.apply("health/poison_right");
		HIT_POISON = register.apply("health/poison_damage");
		HIT_POISON_RIGHT = register.apply("health/poison_damage_right");
		ABSORPTION_LEFT = register.apply("health/absorption_left");
		ABSORPTION = register.apply("health/absorption");
		ABSORPTION_RIGHT = register.apply("health/absorption_right");
		REGENERATION = register.apply("health/regeneration");
		WITHER = register.apply("health/wither");
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
		Sprite healthSprite = ModSpriteAtlasHolder.HUD_ATLAS.getSprite(HEALTH);
		return 2 * MARGIN + (int) (1.0 * healthSprite.getWidth() * HEIGHT / healthSprite.getHeight());
	}

	@Override
	protected int getHeight() {
		return HEIGHT;
	}

	@Override
	protected Options.Position getPosition() {
		return UnofficialMonumentaModClient.options.hud_healthBarPosition;
	}

	@Override
	protected int getZOffset() {
		return 1;
	}

	private PlayerEntity getCameraPlayer() {
		return !(this.client.getCameraEntity() instanceof PlayerEntity) ? null : (PlayerEntity) this.client.getCameraEntity();
	}

	private float easedHealth = -1;
	private float easedAbsorption = -1;
	private float regenProgress = 0;

	@Override
	protected void render(MatrixStack matrices, float tickDelta) {

		PlayerEntity player = getCameraPlayer();
		if (player == null) {
			return;
		}

		int width = getWidth();
		int healthWidth = width - 2 * MARGIN;

		if (UnofficialMonumentaModClient.options.hud_healthMirror) {
			matrices.push();
			matrices.translate(width, 0, 0);
			matrices.scale(-1, 1, 1);
			RenderSystem.disableCull();
		}

		drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BACKGROUND), 0, 0, width, HEIGHT);

		float maxHealth = Math.max(1.0f, player.getMaxHealth());
		float health = Utils.clamp(0, player.getHealth(), maxHealth);
		float absorption = Math.max(0, player.getAbsorptionAmount());

		float lastFrameDuration = client.getLastFrameDuration() / 20;
		easedHealth = easedHealth <= 0 ? health : Math.min(Utils.ease(health, easedHealth, 6 * lastFrameDuration, maxHealth / 3 * lastFrameDuration), maxHealth);
		easedAbsorption = Utils.ease(absorption, easedAbsorption, 6 * lastFrameDuration, maxHealth / 3 * lastFrameDuration);

		if (health > 0 || easedHealth > 0) {
			boolean poisoned = player.hasStatusEffect(StatusEffects.POISON);
			drawPartialSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(poisoned ? POISON : HEALTH), MARGIN, 0, healthWidth, HEIGHT, 0, 0, Math.min(easedHealth, health) / maxHealth, 1);
			if (easedHealth > health) {
				drawPartialSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(poisoned ? HIT_POISON : HIT_HEALTH), MARGIN, 0, healthWidth, HEIGHT, health / maxHealth, 0, easedHealth / maxHealth, 1);
				drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(poisoned ? HIT_POISON_RIGHT : HIT_HEALTH_RIGHT), healthWidth * (easedHealth / maxHealth), 0, 2 * MARGIN, HEIGHT);
			}
			drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(poisoned ? POISON_LEFT : HEALTH_LEFT), 0, 0, 2 * MARGIN, HEIGHT);
			drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(poisoned ? POISON_RIGHT : HEALTH_RIGHT), healthWidth * (Math.min(easedHealth, health) / maxHealth), 0, 2 * MARGIN, HEIGHT);
		}

		StatusEffectInstance regeneration = player.getStatusEffect(StatusEffects.REGENERATION);
		if (regeneration != null) {
			regenProgress += UnofficialMonumentaModClient.options.hud_regenerationSpeed * lastFrameDuration * (regeneration.getAmplifier() + 1);
			if (regenProgress > healthWidth) {
				regenProgress -= healthWidth;
			}
			Sprite regenSprite = ModSpriteAtlasHolder.HUD_ATLAS.getSprite(REGENERATION);
			drawPartialSprite(matrices, regenSprite, MARGIN + regenProgress, 0, healthWidth, HEIGHT, 0, 0, Math.min(easedHealth, health) / maxHealth - regenProgress / healthWidth, 1);
			drawPartialSprite(matrices, regenSprite, MARGIN + regenProgress - healthWidth, 0, healthWidth, HEIGHT, 1 - regenProgress / healthWidth, 0, Math.min(1, Math.min(easedHealth, health) / maxHealth + 1 - regenProgress / healthWidth), 1);
		}

		if (easedAbsorption > 0) {
			drawPartialSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(ABSORPTION), MARGIN, 0, healthWidth, HEIGHT, 0, 0, Math.min(1.0f, easedAbsorption / maxHealth), 1);
			drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(ABSORPTION_LEFT), 0, 0, 2 * MARGIN, HEIGHT);
			drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(ABSORPTION_RIGHT), healthWidth * Math.min(1.0f, easedAbsorption / maxHealth), 0, 2 * MARGIN, HEIGHT);
		}

		drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(OVERLAY), 0, 0, width, HEIGHT);

		if (player.hasStatusEffect(StatusEffects.WITHER)) {
			drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(WITHER), 0, 0, width, HEIGHT);
		}

		if (UnofficialMonumentaModClient.options.hud_healthMirror) {
			matrices.pop();
			RenderSystem.enableCull();
		}

		if (UnofficialMonumentaModClient.options.hud_healthText) {
			// TODO text options: no text at all, no max health, absorption added to current health, current as % (with or without absorption), maybe even move max health to the side
			String fullText = OPTIONAL_SINGLE_DIGIT.format(health) + (absorption <= 0 ? "" : " + " + OPTIONAL_SINGLE_DIGIT.format(absorption));
			drawOutlinedText(matrices, fullText, width / 2 - client.textRenderer.getWidth(fullText) / 2, HEIGHT / 2 - client.textRenderer.fontHeight / 2 + UnofficialMonumentaModClient.options.hud_healthTextOffset, 0xFFFFFFFF);
		}
	}

	@Override
	protected boolean isClickable(double mouseX, double mouseY) {
		return !isPixelTransparent(ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BACKGROUND), mouseX, mouseY)
			       || !isPixelTransparent(ModSpriteAtlasHolder.HUD_ATLAS.getSprite(OVERLAY), mouseX, mouseY);
	}

}
