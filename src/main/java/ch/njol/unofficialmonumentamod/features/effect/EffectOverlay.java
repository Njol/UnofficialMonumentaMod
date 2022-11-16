package ch.njol.unofficialmonumentamod.features.effect;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class EffectOverlay extends HudElement {

	private static final int PADDING_VERTICAL = 5;
	private static final int PADDING_HORIZONTAL = 5;

	private static final ArrayList<Effect> dummyEffects = new ArrayList<>();

	static {
		dummyEffects.add(new Effect(
			"Active effects",
			0,
			360000
		));
		dummyEffects.add(
			new Effect(
				"are shown here.",
				0,
				240000
			)
		);
		dummyEffects.add(
			new Effect(
				"Bad Effect",
				-20,
				120000
			)
		);
		dummyEffects.add(
			new Effect(
				"A very long effect that will mostly likely be trimmed to fit",
				10,
				60000,
				true
			)
		);
	}

	private final ArrayList<Effect> effects = new ArrayList<>();
	private long lastUpdate = 0;

	public void update() {
		if (client.getNetworkHandler() == null) {
			return;
		}
		effects.clear();
		Collection<PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();

		for (PlayerListEntry entry : entries) {
			Effect effect = Effect.from(entry);
			if (effect != null) {
				effects.add(effect);
			}
		}

		lastUpdate = System.currentTimeMillis();
	}

	public ArrayList<Effect> getCumulativeEffects() {
		final ArrayList<Effect> cumulativeEffects = new ArrayList<>();
		effectLoop:
		for (Effect effect : effects) {
			for (Effect cumulativeEffect : cumulativeEffects) {
				if (Objects.equals(cumulativeEffect.name, effect.name)
					    && cumulativeEffect.isPercentage == effect.isPercentage) {
					cumulativeEffect.effectPower += effect.effectPower;
					if (effect.effectTime < cumulativeEffect.effectTime) {
						cumulativeEffect.effectTime = effect.effectTime;
					}
					continue effectLoop;
				}
			}
			cumulativeEffects.add(effect.clone());
		}
		return cumulativeEffects;
	}

	public void tick() {
		if (lastUpdate + 1000 < System.currentTimeMillis()) {
			// update every second
			update();
			return;
		}

		for (Effect effect : effects) {
			effect.tick();
		}
	}

	@Override
	protected void render(MatrixStack matrices, float tickDelta) {
		ArrayList<Effect> visibleEffects = isInEditMode() ? dummyEffects : UnofficialMonumentaModClient.options.effect_compress ? getCumulativeEffects() : effects;
		TextRenderer textRenderer = client.textRenderer;

		int height = getHeight();
		int width = getWidth();
		int currentY = PADDING_VERTICAL;

		DrawableHelper.fill(matrices, 0, 0, width, height, client.options.getTextBackgroundColor(0.3f));

		boolean textAlightRight = UnofficialMonumentaModClient.options.effect_textAlightRight;
		for (Effect effect : visibleEffects) {
			Text text = effect.toText(tickDelta, textAlightRight);
			textRenderer.draw(matrices, text, textAlightRight ? width - PADDING_HORIZONTAL - textRenderer.getWidth(text) : PADDING_HORIZONTAL, currentY, 0xFFFFFFFF);
			currentY += textRenderer.fontHeight + 2;
		}
	}

	@Override
	protected boolean isEnabled() {
		return UnofficialMonumentaModClient.options.effect_enabled;
	}

	@Override
	protected boolean isVisible() {
		return !this.effects.isEmpty();
	}

	@Override
	protected int getWidth() {
		return UnofficialMonumentaModClient.options.effect_width + (2 * PADDING_HORIZONTAL);
	}

	@Override
	protected int getHeight() {
		ArrayList<Effect> effects = isInEditMode() ? dummyEffects : UnofficialMonumentaModClient.options.effect_compress ? getCumulativeEffects() : this.effects;

		return effects.size() * (client.textRenderer.fontHeight + 2) - 2 + (2 * PADDING_VERTICAL);
	}

	@Override
	protected ElementPosition getPosition() {
		return UnofficialMonumentaModClient.options.effect_position;
	}

	@Override
	protected int getZOffset() {
		return 0;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragging) {
			UnofficialMonumentaModClient.saveConfig();
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

}
