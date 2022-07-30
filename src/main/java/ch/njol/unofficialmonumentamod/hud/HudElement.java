package ch.njol.unofficialmonumentamod.hud;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.options.Options.Position;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;

public abstract class HudElement {

	private static final Logger LOGGER = LogManager.getLogger();

	protected final Hud hud;
	protected final MinecraftClient client;

	public HudElement(Hud hud) {
		this.hud = hud;
		client = MinecraftClient.getInstance();
	}

	public void bindTextureOrDefault(Identifier identifier, Identifier defaultIdentifier) {
		client.getTextureManager().bindTexture(identifier);
		AbstractTexture texture = client.getTextureManager().getTexture(identifier);
		if (texture == null || texture == MissingSprite.getMissingSpriteTexture()) {
			client.getTextureManager().bindTexture(defaultIdentifier);
		}
	}

	public void renderAbsolute(MatrixStack matrices, float tickDelta) {
		if (!isEnabled()) {
			return;
		}
		Rectangle dimension = getDimension();
		matrices.push();
		matrices.translate(dimension.x, dimension.y, getZOffset());
		try {
			render(matrices, tickDelta);
		} catch (Exception e) {
			LOGGER.error("Error rendering " + getClass().getCanonicalName(), e);
		} finally {
			matrices.pop();
		}
	}

	public Rectangle getDimension() {
		Position position = getPosition();
		int width = getWidth();
		int height = getHeight();
		int x = Math.round(hud.scaledWidth * position.offsetXRelative + position.offsetXAbsolute - position.alignX * width);
		int y = Math.round(hud.scaledHeight * position.offsetYRelative + position.offsetYAbsolute - position.alignY * height);
		return new Rectangle(x, y, width, height);
	}

	protected abstract boolean isEnabled();

	protected abstract int getWidth();

	protected abstract int getHeight();

	protected abstract Position getPosition();

	protected abstract int getZOffset();

	protected abstract void render(MatrixStack matrices, float tickDelta);

	// helper methods

	/**
	 * Draws text with a full black border around it
	 */
	public void drawOutlinedText(MatrixStack matrices, String text, int x, int y, int color) {
		matrices.push();
		matrices.translate(0, 0, 1);
		client.textRenderer.draw(matrices, text, x - 1, y, 0);
		client.textRenderer.draw(matrices, text, x, y - 1, 0);
		client.textRenderer.draw(matrices, text, x + 1, y, 0);
		client.textRenderer.draw(matrices, text, x, y + 1, 0);
		matrices.translate(0, 0, 0.03f);
		client.textRenderer.draw(matrices, text, x, y, color);
		matrices.pop();
	}

	public static void drawTextureSmooth(MatrixStack matrices, float x, float y, float width, float height) {
		drawTexturedQuadSmooth(matrices.peek().getModel(), x, x + width, y, y + height, 0, 0, 1, 0, 1);
	}

	public static void drawTextureSmooth(MatrixStack matrices, float x, float y, float width, float height, float u0, float u1, float v0, float v1) {
		drawTexturedQuadSmooth(matrices.peek().getModel(), x, x + width, y, y + height, 0, u0, u1, v0, v1);
	}

	public static void drawTexturedQuadSmooth(Matrix4f matrices, float x0, float x1, float y0, float y1, float z, float u0, float u1, float v0, float v1) {
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
		bufferBuilder.vertex(matrices, x0, y1, z).texture(u0, v1).next();
		bufferBuilder.vertex(matrices, x1, y1, z).texture(u1, v1).next();
		bufferBuilder.vertex(matrices, x1, y0, z).texture(u1, v0).next();
		bufferBuilder.vertex(matrices, x0, y0, z).texture(u0, v0).next();
		bufferBuilder.end();
		RenderSystem.enableAlphaTest();
		BufferRenderer.draw(bufferBuilder);
	}

	public static void drawSprite(MatrixStack matrices, Sprite sprite, float x, float y, float width, float height) {
		sprite.getAtlas().bindTexture();
		drawTexturedQuadSmooth(matrices.peek().getModel(), x, x + width, y, y + height, 0, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV());
	}

	// need to draw lots of quads for this as the sprite texture is in an atlas and thus wrapping UV isn't possible
	public static void drawRepeatingSprite(MatrixStack matrices, Sprite sprite, float x, float y, float width, float height, float xRepetitions, float yRepetitions) {
		if (xRepetitions * yRepetitions > 1000) {
			throw new IllegalArgumentException("Too many sprite repetitions requested! (" + xRepetitions + ", " + yRepetitions + ")");
		}
		MinecraftClient.getInstance().getTextureManager().bindTexture(sprite.getAtlas().getId());
		for (int xrep = 0; xrep < xRepetitions; xrep++) {
			for (int yrep = 0; yrep < yRepetitions; yrep++) {
				float xfactor = Math.min(1.0f, xRepetitions - xrep);
				float yfactor = Math.min(1.0f, yRepetitions - yrep);
				drawTexturedQuadSmooth(matrices.peek().getModel(),
					x + width * xrep, x + width * (xrep + xfactor), y + height * yrep, y + height * (yrep + yfactor), 0,
					sprite.getMinU(), sprite.getMinU() + (sprite.getMaxU() - sprite.getMinU()) * xfactor, sprite.getMinV(), sprite.getMinV() + (sprite.getMaxV() - sprite.getMinV()) * yfactor);
			}
		}
	}

	protected boolean dragging = false;

	private double dragX;
	private double dragY;

	Hud.ClickResult mouseClicked(double mouseX, double mouseY, int button) {
		if (Screen.hasControlDown()) {
			dragging = true;
			dragX = mouseX;
			dragY = mouseY;
			return Hud.ClickResult.DRAG;
		} else {
			return Hud.ClickResult.NONE;
		}
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (dragging) {
			Rectangle dimension = getDimension();
			Position position = getPosition();

			double newX = mouseX - dragX;
			double newY = mouseY - dragY;

			// Offsets to the sides and centers of the screen. The smallest offset of each direction will be used as anchor point.
			double left = newX;
			double horizontalMiddle = Math.abs(newX + dimension.width / 2.0 - hud.scaledWidth / 2.0);
			double right = hud.scaledWidth - (newX + dimension.width);
			double top = newY;
			double verticalMiddle = Math.abs(newY + dimension.height / 2.0 - hud.scaledHeight / 2.0);
			double bottom = hud.scaledHeight - (newY + dimension.height);

			position.offsetXRelative = left < horizontalMiddle && left < right ? 0 : horizontalMiddle < right ? 0.5f : 1;
			position.offsetYRelative = top < verticalMiddle && top < bottom ? 0 : verticalMiddle < bottom ? 0.5f : 1;
			position.alignX = position.offsetXRelative;
			position.alignY = position.offsetYRelative;
			position.offsetXAbsolute = (int) Math.round(newX - hud.scaledWidth * position.offsetXRelative + position.alignX * dimension.width);
			position.offsetYAbsolute = (int) Math.round(newY - hud.scaledHeight * position.offsetYRelative + position.alignY * dimension.height);

			// snap to center
			if (!Screen.hasAltDown()) {
				if (position.offsetXRelative == 0.5f && Math.abs(position.offsetXAbsolute) < 10) {
					position.offsetXAbsolute = 0;
				}
				if (position.offsetYRelative == 0.5f && Math.abs(position.offsetYAbsolute) < 10) {
					position.offsetYAbsolute = 0;
				}
			}
			return true;
		}
		return false;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragging) {
			dragging = false;
			UnofficialMonumentaModClient.saveConfig();
			return true;
		}
		return false;
	}

	public void removed() {
		dragging = false;
	}

	public void renderTooltip(Screen screen, MatrixStack matrices, int mouseX, int mouseY) {

	}

}
