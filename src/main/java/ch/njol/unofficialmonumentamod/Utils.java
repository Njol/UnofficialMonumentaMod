package ch.njol.unofficialmonumentamod;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

public abstract class Utils {

	private Utils() {
	}

	/**
	 * Gets the plain display name of an items. This is used by Monumenta to distinguish items.
	 *
	 * @param itemStack An item stack
	 * @return The plain display name of the item, i.e. the value of NBT node plain.display.Name.
	 */
	public static String getPlainDisplayName(ItemStack itemStack) {
		return itemStack.getNbt() == null ? null : itemStack.getNbt().getCompound("plain").getCompound("display").getString("Name");
	}

	public static float smoothStep(float f) {
		if (f <= 0) {
			return 0;
		}
		if (f >= 1) {
			return 1;
		}
		return f * f * (3 - 2 * f);
	}

	public static float ease(float currentValue, float oldValue, float speedFactor, float minSpeed) {
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

	public static int clamp(int lowerBound, int value, int upperBound) {
		return Math.max(lowerBound, Math.min(value, upperBound));
	}

	public static float clamp(float lowerBound, float value, float upperBound) {
		return Math.max(lowerBound, Math.min(value, upperBound));
	}

	public static List<Text> getTooltip(ItemStack stack) {
		return stack.getTooltip(MinecraftClient.getInstance().player, TooltipContext.BASIC);
	}

	public static void drawFilledPolygon(DrawContext drawContext, int originX, int originY, float radius, int sides, int color) {
		drawPartialFilledPolygon(drawContext, originX, originY, radius, sides, color, 1.0);
	}

	public static void drawPartialFilledPolygon(DrawContext drawContext, int originX, int originY, float radius, int sides, int color, double percentage) {
		MatrixStack matrices = drawContext.getMatrices();

		//percentage from 0.00 to 1.00
		float a = (float)(color >> 24 & 0xFF) / 255.0f;
		float r = (float)(color >> 16 & 0xFF) / 255.0f;
		float g = (float)(color >> 8 & 0xFF) / 255.0f;
		float b = (float)(color & 0xFF) / 255.0f;
		Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();

		bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
		bufferBuilder.vertex(positionMatrix, originX, originY, 0.0f).color(a, r, g, b).next();

		//very optimised (trust)
		for (int i = 0; i <= (sides * percentage); i++) {
			double angle = ((Math.PI * 2) * i / sides) + Math.toRadians(180);
			bufferBuilder.vertex(positionMatrix, (float) (originX + Math.sin(angle) * radius), (float) (originY + Math.cos(angle) * radius), 0.0f).color(a, r, g, b).next();
		}
		BufferBuilder.BuiltBuffer built = bufferBuilder.end();

		BufferRenderer.drawWithGlobalProgram(built);
		RenderSystem.disableBlend();
	}

	public static void drawHollowPolygon(DrawContext drawContext, int originX, int originY, int borderWidth, float radius, int sides, int color) {
		drawPartialHollowPolygon(drawContext, originX, originY, borderWidth, radius, sides, color, 1.0);
	}

	private static void drawPartialPartPolygon(DrawContext drawContext, int originX, int originY, int borderWidth, float radius, int sides, double percentage, float r, float g, float b, float a) {
		MatrixStack matrices = drawContext.getMatrices();

		Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

		//very optimised (trust)
		for (int i = 0; i <= (sides * percentage); i++) {
			double angle = ((Math.PI * 2) * i / sides) + Math.toRadians(180);
			bufferBuilder.vertex(positionMatrix, (float) (originX + Math.sin(angle) * (radius - borderWidth)), (float) (originY + Math.cos(angle) * (radius - borderWidth)), 0.0f).color(a, r, g, b).next();
			bufferBuilder.vertex(positionMatrix, (float) (originX + Math.sin(angle) * radius), (float) (originY + Math.cos(angle) * radius), 0.0f).color(a, r, g, b).next();
		}

		BufferBuilder.BuiltBuffer built = bufferBuilder.end();
		BufferRenderer.drawWithGlobalProgram(built);
		RenderSystem.disableBlend();
	}

	public static void drawPartialHollowPolygon(DrawContext drawContext, int originX, int originY, int borderWidth, float radius, int sides, int color, double percentage) {
		float a = (float)(color >> 24 & 0xFF) / 255.0f;
		float r = (float)(color >> 16 & 0xFF) / 255.0f;
		float g = (float)(color >> 8 & 0xFF) / 255.0f;
		float b = (float)(color & 0xFF) / 255.0f;

		drawPartialPartPolygon(drawContext, originX, originY, borderWidth, radius, sides, percentage, r, g, b, a);
		drawPartialPartPolygon(drawContext, originX, originY, borderWidth, radius + ((float) borderWidth / 2), sides*2, percentage, r, g, b, a);
		drawPartialPartPolygon(drawContext, originX, originY, borderWidth, radius - ((float) borderWidth / 2), sides*2, percentage, r, g, b, a);
	}
	
	public static class Lerp {
		//copied from https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/src/main/java/io/github/moulberry/notenoughupdates/core/util/lerp/LerpingFloat.java under the LGPL 3.0 license
		
		private int timeSpent;
		private long lastMillis;
		private final int targetTime;
		
		private float targetValue;
		private float lerpValue;
		
		public Lerp(float initValue, int targetTime) {
			this.targetValue = this.lerpValue = initValue;
			this.targetTime = targetTime;
		}
		
		public Lerp(int initValue) {
			this(initValue, 200);
		}
		
		public void tick() {
			int lastTimeSpent = timeSpent;
			this.timeSpent += (int) (System.currentTimeMillis() - lastMillis);
			
			float lastDistPercentToTarget = lastTimeSpent / (float) targetTime;
			float distPercentToTarget = timeSpent / (float) targetTime;
			float fac = (1 - lastDistPercentToTarget) / lastDistPercentToTarget;
			
			float startValue = lerpValue - (targetValue - lerpValue) / fac;
			
			float dist = targetValue - startValue;
			if (dist == 0) return;
			
			float oldLerpValue = lerpValue;
			if (distPercentToTarget >= 1) {
				lerpValue = targetValue;
			} else {
				lerpValue = startValue + dist * distPercentToTarget;
			}
			
			if (lerpValue == oldLerpValue) {
				timeSpent = lastTimeSpent;
			} else {
				this.lastMillis = System.currentTimeMillis();
			}
		}
		
		public void resetTimer() {
			this.timeSpent = 0;
			this.lastMillis = System.currentTimeMillis();
		}
		
		public void setTarget(float targetValue) {
			this.targetValue = targetValue;
		}
		
		public void setValue(float value) {
			this.targetValue = this.lerpValue = value;
		}
		
		public float getValue() {
			return lerpValue;
		}
		
		public float getTarget() {
			return targetValue;
		}
	}

}
