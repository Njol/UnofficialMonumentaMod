package ch.njol.unofficialmonumentamod.hud;

import ch.njol.unofficialmonumentamod.ModSpriteAtlasHolder;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.options.Options;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.util.function.Function;

public class MountHealthBar extends HudElement {

	private static final int HEIGHT = 32;
	private static final int MARGIN = 16;

	private static Identifier BACKGROUND, OVERLAY, HEALTH_BAR;

	private static final DecimalFormat SINGLE_DIGIT = new DecimalFormat("0.0");
	private static final DecimalFormat OPTIONAL_SINGLE_DIGIT = new DecimalFormat("0.#");

	public MountHealthBar(Hud hud) {
		super(hud);
	}

	public static void registerSprites(Function<String, Identifier> register) {
		OVERLAY = register.apply("mount_health/overlay");
		BACKGROUND = register.apply("mount_health/background");
		HEALTH_BAR = register.apply("mount_health/health");
	}

	@Override
	protected boolean isEnabled() {
		return UnofficialMonumentaModClient.options.hud_enabled && UnofficialMonumentaModClient.options.hud_statusBarsEnabled;
	}

	@Override
	protected boolean isVisible() {
		return getMount() != null;
	}

	@Override
	protected int getWidth() {
		Sprite barSprite = ModSpriteAtlasHolder.HUD_ATLAS.getSprite(HEALTH_BAR);
		return 2 * MARGIN + (int) (1.0 * barSprite.getWidth() * HEIGHT / barSprite.getHeight());
	}

	@Override
	protected int getHeight() {
		return HEIGHT;
	}

	@Override
	protected Options.Position getPosition() {
		return UnofficialMonumentaModClient.options.hud_mountHealthBarPosition;
	}

	@Override
	protected int getZOffset() {
		return 1;
	}

	private LivingEntity getMount() {
		if (this.client.getCameraEntity() instanceof PlayerEntity player
			    && player.getVehicle() instanceof LivingEntity mount) {
			return mount;
		}
		return null;
	}

	private float easedHealth = -1;

	@Override
	protected void render(MatrixStack matrices, float tickDelta) {

		LivingEntity mount = getMount();
		if (mount == null && !isInEditMode()) {
			easedHealth = -1;
			return;
		}

		int width = getWidth();
		int healthWidth = width - 2 * MARGIN;

		if (UnofficialMonumentaModClient.options.hud_mountHealthMirror) {
			matrices.push();
			matrices.translate(width, 0, 0);
			matrices.scale(-1, 1, 1);
			RenderSystem.disableCull();
		}

		drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BACKGROUND), 0, 0, width, HEIGHT);

		float maxHealth = Math.max(1.0f, mount != null ? mount.getMaxHealth() : 20);
		float health = Utils.clamp(0, mount != null ? mount.getHealth() : 20, maxHealth);
		float lastFrameDuration = client.getLastFrameDuration() / 20;
		easedHealth = easedHealth <= 0 ? health : Utils.ease(health, easedHealth, 6 * lastFrameDuration, maxHealth / 3 * lastFrameDuration);

		drawPartialSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(HEALTH_BAR),
			MARGIN, 0, healthWidth, HEIGHT,
			0, 0, easedHealth / maxHealth, 1);

		drawSprite(matrices, ModSpriteAtlasHolder.HUD_ATLAS.getSprite(OVERLAY), 0, 0, width, HEIGHT);

		if (UnofficialMonumentaModClient.options.hud_mountHealthMirror) {
			matrices.pop();
			RenderSystem.enableCull();
		}

		if (UnofficialMonumentaModClient.options.hud_mountHealthText) {
			String fullText = OPTIONAL_SINGLE_DIGIT.format(health);
			drawOutlinedText(matrices, fullText, width / 2 - client.textRenderer.getWidth(fullText) / 2, HEIGHT / 2 - client.textRenderer.fontHeight / 2 + UnofficialMonumentaModClient.options.hud_mountHealthTextOffset, 0xFFFFFFFF);
		}
	}

	@Override
	protected boolean isClickable(double mouseX, double mouseY) {
		return !isPixelTransparent(ModSpriteAtlasHolder.HUD_ATLAS.getSprite(BACKGROUND), mouseX, mouseY)
			       || !isPixelTransparent(ModSpriteAtlasHolder.HUD_ATLAS.getSprite(OVERLAY), mouseX, mouseY);
	}

}
