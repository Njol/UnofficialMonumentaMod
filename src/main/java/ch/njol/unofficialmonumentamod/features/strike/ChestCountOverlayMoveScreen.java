package ch.njol.unofficialmonumentamod.features.strike;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.MoveScreen;
import ch.njol.unofficialmonumentamod.options.Options;
import java.awt.Point;
import net.minecraft.text.TranslatableText;
import org.lwjgl.glfw.GLFW;


public class ChestCountOverlayMoveScreen extends MoveScreen {

	public ChestCountOverlayMoveScreen() {
		super(new TranslatableText("unofficial-monumenta-mod.move.chestcount"));
	}

	private boolean draggingOverlay = false;

	private double dragX;
	private double dragY;

	@Override
	public void removed() {
		super.removed();
		ChestCountOverlay.shouldRenderDummy = false;
	}

	@Override
	protected void init() {
		super.init();
		ChestCountOverlay.shouldRenderDummy = true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		Point origin = getOverlayOrigin();


		if ((origin.x > mouseX && origin.x + ChestCountOverlay.width < mouseX) || (origin.x < mouseX && origin.x + ChestCountOverlay.width > mouseX)) {
			if ((origin.y > mouseY && origin.y + ChestCountOverlay.height < mouseY) || (origin.y < mouseY && origin.y + ChestCountOverlay.height > mouseY)) {
				//mouse in overlay

				if (button == 0) {
					//clicked
					synchronized (origin) {
						dragX = mouseX - origin.x;
						dragY = mouseY - origin.y;
						draggingOverlay = true;
					}
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	public void setToInBoundPos() {
		Options options = UnofficialMonumentaModClient.options;
		if (options.chestCount_offsetXRelative == 0.0f && options.chestCount_offsetXAbsolute < 0) {
			options.chestCount_offsetXAbsolute = 0;
		}
		if (options.chestCount_offsetXRelative == 1.0f && options.chestCount_offsetXAbsolute > 0) {
			options.chestCount_offsetXAbsolute = -ChestCountOverlay.width;
		}

		if (options.chestCount_offsetYRelative == 0.0f && options.chestCount_offsetYAbsolute < 0) {
			options.chestCount_offsetYAbsolute = 0;
		}
		if (options.chestCount_offsetYRelative == 1.0f && options.chestCount_offsetYAbsolute > 0) {
			options.chestCount_offsetYAbsolute = -ChestCountOverlay.height;
		}
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		Options options = UnofficialMonumentaModClient.options;
		if (draggingOverlay) {
			synchronized (options) {
				double newX = mouseX - dragX;
				double newY = mouseY - dragY;

				double left = newX;
				double horizontalMiddle = Math.abs(newX + ChestCountOverlay.width / 2.0 - width / 2.0);
				double right = width - (newX + ChestCountOverlay.width);
				double top = newY;
				double verticalMiddle = Math.abs(newY + ChestCountOverlay.height / 2.0 - height / 2.0);
				double bottom = height - (newY + ChestCountOverlay.height);

				options.chestCount_offsetXRelative = left < horizontalMiddle && left < right ? 0 : horizontalMiddle < right ? 0.5f : 1;
				options.chestCount_offsetYRelative = top < verticalMiddle && top < bottom ? 0 : verticalMiddle < bottom ? 0.5f : 1;
				options.chestCount_offsetXAbsolute = (int) Math.round(newX + (options.effect_offsetXRelative * ChestCountOverlay.width) - (this.width * options.chestCount_offsetXRelative));
				options.chestCount_offsetYAbsolute = (int) Math.round(newY + 0 - (this.height * options.chestCount_offsetYRelative));

				setToInBoundPos();

				//snap to original pos
				if (options.chestCount_offsetXRelative == 1.0f && options.chestCount_offsetXAbsolute > -64) {
					options.chestCount_offsetXAbsolute = -64;
				}
				if (options.chestCount_offsetYRelative == 0.0f && Math.abs(options.chestCount_offsetYAbsolute) < 10) {
					options.chestCount_offsetYAbsolute = 0;
				}

				UnofficialMonumentaModClient.saveConfig();
			}
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (draggingOverlay) {
			draggingOverlay = false;

			UnofficialMonumentaModClient.saveConfig();
		}

		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		Options def = UnofficialMonumentaModClient.dummyConfig;
		if (keyCode == GLFW.GLFW_KEY_R) {
			//pressed Reset key
			UnofficialMonumentaModClient.options.chestCount_offsetXRelative = def.chestCount_offsetXRelative;
			UnofficialMonumentaModClient.options.chestCount_offsetYRelative = def.chestCount_offsetYRelative;
			UnofficialMonumentaModClient.options.chestCount_offsetXAbsolute = def.chestCount_offsetXAbsolute;
			UnofficialMonumentaModClient.options.chestCount_offsetYAbsolute = def.chestCount_offsetYAbsolute;

			UnofficialMonumentaModClient.saveConfig();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	public Point getOverlayOrigin() {
		Options o = UnofficialMonumentaModClient.options;

		int x = Math.round(width * o.chestCount_offsetXRelative) + o.chestCount_offsetXAbsolute;
		int y = Math.round(height * o.chestCount_offsetYRelative) + o.chestCount_offsetYAbsolute;

		return new Point(x, y);
	}
}
