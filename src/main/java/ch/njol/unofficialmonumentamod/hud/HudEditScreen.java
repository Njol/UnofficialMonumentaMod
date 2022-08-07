package ch.njol.unofficialmonumentamod.hud;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {

	private final Screen parent;

	private final Hud hud = Hud.INSTANCE;

	public HudEditScreen(Screen parent) {
		super(Text.of(UnofficialMonumentaModClient.MOD_IDENTIFIER + " HUD Edit Screen"));
		this.parent = parent;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return hud.mouseClicked(this, mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		return hud.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return hud.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void removed() {
		hud.removed();
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		assert client != null;

		client.inGameHud.setOverlayMessage(Text.of("Messages appear here!"), false);

		hud.renderTooltip(this, matrices, mouseX, mouseY);

		matrices.push();
		matrices.translate(0, 0, 1000);
		client.textRenderer.drawWithShadow(matrices, "Reorder elements by holding ctrl and then dragging them around. ESC to close.", 5, 5, 0xffffffff);
		matrices.pop();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (UnofficialMonumentaModClient.openHudEditScreenKeybinding.matchesKey(keyCode, scanCode)) {
			close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void close() {
		assert client != null;
		client.setScreen(parent);
		client.inGameHud.setOverlayMessage(null, false);
	}

}
