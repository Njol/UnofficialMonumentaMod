package ch.njol.unofficialmonumentamod.hud;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.mixins.InGameHudAccessor;
import ch.njol.unofficialmonumentamod.options.Options;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class OverlayMessage extends HudElement {

	public static String EDIT_SAMPLE_MESSAGE = "Messages appear here!";

	public OverlayMessage(Hud hud) {
		super(hud);
	}

	@Override
	protected boolean isEnabled() {
		return UnofficialMonumentaModClient.options.hud_enabled && UnofficialMonumentaModClient.options.hud_moveOverlayMessage;
	}

	@Override
	protected boolean isVisible() {
		return false; // only "visible" while editing, but the text is always rendered by (modified) vanilla code
	}

	@Override
	protected int getWidth() {
		Text overlayMessage = ((InGameHudAccessor) client.inGameHud).getOverlayMessage();
		return overlayMessage == null ? 0 : client.textRenderer.getWidth(overlayMessage);
	}

	@Override
	protected int getHeight() {
		return client.textRenderer.fontHeight;
	}

	@Override
	protected Options.Position getPosition() {
		return UnofficialMonumentaModClient.options.hud_overlayMessagePosition;
	}

	@Override
	protected int getZOffset() {
		return 0;
	}

	@Override
	protected void render(MatrixStack matrices, float tickDelta) {
		// nothing to do - rendered by (modified) vanilla code
	}

}
