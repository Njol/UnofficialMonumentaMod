package ch.njol.unofficialmonumentamod.hud;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public class Hud {

	public enum ClickResult {
		NONE, HANDLED, DRAG;
	}

	public static Hud INSTANCE = new Hud();

	public int scaledWidth;
	public int scaledHeight;

	public final AbiltiesHud abilities = new AbiltiesHud(this);
	public final HealthBar health = new HealthBar(this);
	public final HungerBar hunger = new HungerBar(this);
	public final BreathBar breath = new BreathBar(this);
	public final MountHealthBar mountHealthBar = new MountHealthBar(this);

	// moved vanilla elements
	public final OverlayMessage overlayMessage = new OverlayMessage(this);
	public final HeldItemTooltip heldItemTooltip = new HeldItemTooltip(this);

	private final HudElement[] allElements = {abilities, health, hunger, breath, mountHealthBar,
		overlayMessage, heldItemTooltip};

	private HudElement draggedElement = null;

	private Hud() {
	}

	public void updateScreenSize(int scaledWidth, int scaledHeight) {
		this.scaledWidth = scaledWidth;
		this.scaledHeight = scaledHeight;
	}

	public boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
		for (HudElement element : allElements) {
			if (!element.isEnabled() || (!element.isVisible() && !(screen instanceof HudEditScreen))) {
				continue;
			}
			Rectangle dimension = element.getDimension();
			if (!dimension.contains(mouseX, mouseY) || !element.isClickable(mouseX - dimension.x, mouseY - dimension.y)) {
				continue;
			}
			ClickResult result = element.mouseClicked(mouseX - dimension.x, mouseY - dimension.y, button);
			switch (result) {
				case NONE:
					continue;
				case HANDLED:
					return true;
				case DRAG:
					draggedElement = element;
					return true;
			}
		}
		return false;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (draggedElement != null) {
			return draggedElement.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		}
		return false;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (draggedElement != null) {
			draggedElement.mouseReleased(mouseX, mouseY, button);
			draggedElement = null;
			return true;
		}
		return false;
	}

	public void removed() {
		draggedElement = null;
		for (HudElement element : allElements) {
			element.removed();
		}
	}

	public void renderTooltip(Screen screen, MatrixStack matrices, int mouseX, int mouseY) {
		for (HudElement element : allElements) {
			if (!element.isEnabled() || (!element.isVisible() && !(screen instanceof HudEditScreen))) {
				continue;
			}
			Rectangle dimension = element.getDimension();
			if (!dimension.contains(mouseX, mouseY) || !element.isClickable(mouseX - dimension.x, mouseY - dimension.y)) {
				continue;
			}
			element.renderTooltip(screen, matrices, mouseX, mouseY);
			return;
		}
	}

}
