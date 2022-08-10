package ch.njol.unofficialmonumentamod.hud;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.mixins.InGameHudAccessor;
import ch.njol.unofficialmonumentamod.options.Options;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

public class HeldItemTooltip extends HudElement {

	public static final Text EDIT_SAMPLE_MESSAGE = Text.of("Held Item's Name");

	public HeldItemTooltip(Hud hud) {
		super(hud);
	}

	@Override
	protected boolean isEnabled() {
		return UnofficialMonumentaModClient.options.hud_enabled && UnofficialMonumentaModClient.options.hud_moveHeldItemTooltip
			       && client.options.heldItemTooltips && this.client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR; // these two are vanilla check
	}

	@Override
	protected boolean isVisible() {
		return false; // only "visible" while editing, but the text is always rendered by (modified) vanilla code
	}

	@Override
	protected int getWidth() {
		ItemStack currentStack = ((InGameHudAccessor) client.inGameHud).getCurrentStack();
		if (currentStack == null || currentStack.isEmpty()) {
			return 0;
		}
		MutableText mutableText = new LiteralText("").append(currentStack.getName()).formatted(currentStack.getRarity().formatting);
		if (currentStack.hasCustomName()) {
			mutableText.formatted(Formatting.ITALIC);
		}
		return client.textRenderer.getWidth(mutableText);
	}

	@Override
	protected int getHeight() {
		return client.textRenderer.fontHeight;
	}

	@Override
	protected Options.Position getPosition() {
		return UnofficialMonumentaModClient.options.hud_heldItemTooltipPosition;
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
