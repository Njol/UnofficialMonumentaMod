package ch.njol.unofficialmonumentamod.hud;

import ch.njol.unofficialmonumentamod.ModSpriteAtlasHolder;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.options.Options;
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
	private static final int PIXELS_PER_SIZE = 64;

	private static Identifier BACKGROUND_LEFT, BACKGROUND_MID, BACKGROUND_RIGHT;
	private static Identifier HEALTH_LEFT, HEALTH_MID, HEALTH_RIGHT, HIT_HEALTH_MID;
	private static Identifier ABSORPTION_LEFT, ABSORPTION_MID, ABSORPTION_RIGHT;
	private static Identifier POISON_LEFT, POISON_MID, POISON_RIGHT;
	private static Identifier REGENERATION;
	private static Identifier WITHER_LEFT, WITHER_RIGHT;

	private static final DecimalFormat SINGLE_DIGIT = new DecimalFormat("#.0");
	private static final DecimalFormat OPTIONAL_SINGLE_DIGIT = new DecimalFormat("#.#");

	public HealthBar(Hud hud) {
		super(hud);
	}

	public static void registerSprites(Function<String, Identifier> register) {
		BACKGROUND_LEFT = register.apply("background_left");
		BACKGROUND_MID = register.apply("background_mid");
		BACKGROUND_RIGHT = register.apply("background_right");
		HEALTH_LEFT = register.apply("healthbar_left");
		HEALTH_MID = register.apply("healthbar_mid");
		HEALTH_RIGHT = register.apply("healthbar_right");
		HIT_HEALTH_MID = register.apply("healthbar_damage_mid");
		POISON_LEFT = register.apply("poison_left");
		POISON_MID = register.apply("poison_mid");
		POISON_RIGHT = register.apply("poison_right");
		ABSORPTION_LEFT = register.apply("absorption_left");
		ABSORPTION_MID = register.apply("absorption_mid");
		ABSORPTION_RIGHT = register.apply("absorption_right");
		REGENERATION = register.apply("regeneration");
		WITHER_LEFT = register.apply("wither_left");
		WITHER_RIGHT = register.apply("wither_right");
	}

	@Override
	protected boolean isEnabled() {
		return UnofficialMonumentaModClient.options.hud_enabled;
	}

	@Override
	protected int getWidth() {
		return 2 * MARGIN + UnofficialMonumentaModClient.options.hud_heathBarSize * PIXELS_PER_SIZE;
	}

	@Override
	protected int getHeight() {
		return HEIGHT;
	}

	@Override
	protected Options.Position getPosition() {
		return UnofficialMonumentaModClient.options.hud_heathBarPosition;
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

	private static float ease(float currentValue, float oldValue, float speedFactor, float minSpeed) {
		if (Math.abs(currentValue - oldValue) <= minSpeed) {
			return currentValue;
		} else {
			float speed = (currentValue - oldValue) * speedFactor;
			if (speed > 0 && speed < minSpeed) {
				speed = minSpeed;
			} else if (speed < 0 && speed > -minSpeed) {
				speed = -minSpeed;
			}
			return oldValue + speed;
		}
	}

	@Override
	protected void render(MatrixStack matrices, float tickDelta) {

		PlayerEntity player = getCameraPlayer();
		if (player == null) {
			return;
		}

		int width = getWidth();
		int size = UnofficialMonumentaModClient.options.hud_heathBarSize;

		client.getTextureManager().bindTexture(ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BACKGROUND_LEFT).getAtlas().getId());

		drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BACKGROUND_LEFT), 0, 0, PIXELS_PER_SIZE + MARGIN, HEIGHT);
		drawRepeatingSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BACKGROUND_MID), MARGIN + PIXELS_PER_SIZE, 0, PIXELS_PER_SIZE, HEIGHT, size - 2, 1);
		drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BACKGROUND_RIGHT), MARGIN + PIXELS_PER_SIZE * (size - 1), 0, PIXELS_PER_SIZE + MARGIN, HEIGHT);

		float health = player.getHealth();
		float absorption = player.getAbsorptionAmount();
		float maxHealth = Math.max(1.0f, player.getMaxHealth());

		float lastFrameDuration = client.getLastFrameDuration() / 20;
		easedHealth = ease(health, easedHealth, 6 * lastFrameDuration, maxHealth / 3 * lastFrameDuration);
		easedAbsorption = ease(absorption, easedAbsorption, 6 * lastFrameDuration, maxHealth / 3 * lastFrameDuration);

		boolean poisoned = player.hasStatusEffect(StatusEffects.POISON);
		drawRepeatingSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(poisoned ? POISON_MID : HEALTH_MID), MARGIN, 0, PIXELS_PER_SIZE, HEIGHT, size * Math.min(1.0f, Math.min(easedHealth, health) / maxHealth), 1);
		if (easedHealth > health) {
			drawRepeatingSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(HIT_HEALTH_MID), MARGIN + health / maxHealth * PIXELS_PER_SIZE * size, 0, PIXELS_PER_SIZE, HEIGHT, size * Math.min(1.0f, (easedHealth - health) / maxHealth), 1);
		}
		drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(poisoned ? POISON_LEFT : HEALTH_LEFT), 0, 0, 2 * MARGIN, HEIGHT);
		drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(poisoned ? POISON_RIGHT : HEALTH_RIGHT), PIXELS_PER_SIZE * size * Math.min(1.0f, easedHealth / maxHealth), 0, 2 * MARGIN, HEIGHT);

		StatusEffectInstance regeneration = player.getStatusEffect(StatusEffects.REGENERATION);
		if (regeneration != null) {
			drawRepeatingSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(REGENERATION), MARGIN, 0, PIXELS_PER_SIZE, HEIGHT, size * Math.min(1.0f, easedHealth / maxHealth), 1);
		}

		if (easedAbsorption > 0) {
			drawRepeatingSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(ABSORPTION_MID), MARGIN, 0, PIXELS_PER_SIZE, HEIGHT, size * Math.min(1.0f, easedAbsorption / maxHealth), 1);
			drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(ABSORPTION_LEFT), 0, 0, 2 * MARGIN, HEIGHT);
			drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(ABSORPTION_RIGHT), PIXELS_PER_SIZE * size * Math.min(1.0f, easedAbsorption / maxHealth), 0, 2 * MARGIN, HEIGHT);
		}

		if (player.hasStatusEffect(StatusEffects.WITHER)) {
			drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(WITHER_LEFT), 0, 0, PIXELS_PER_SIZE + MARGIN, HEIGHT);
			drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(WITHER_RIGHT), MARGIN + PIXELS_PER_SIZE * (size - 1), 0, PIXELS_PER_SIZE + MARGIN, HEIGHT);
		}

		// TODO text options: no text at all, no max health, absorption added to current health, current as % (with or without absorption)
		String fullText = SINGLE_DIGIT.format(health) + " / " + OPTIONAL_SINGLE_DIGIT.format(maxHealth) + (absorption <= 0 ? "" : " + " + SINGLE_DIGIT.format(absorption));
		drawOutlinedText(matrices, fullText, width / 2 - client.textRenderer.getWidth(fullText) / 2, HEIGHT / 2 - client.textRenderer.fontHeight / 2, 0xFFFFFFFF);

	}

	@Override
	Hud.ClickResult mouseClicked(double mouseX, double mouseY, int button) {
		// TODO limit click area
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
