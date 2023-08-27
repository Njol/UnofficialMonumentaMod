package ch.njol.unofficialmonumentamod.features.effects;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.mixins.PlayerListHudAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
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
		effects.clear();
		for (PlayerListEntry entry : getCategory("Custom Effects")) {
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
		//clear effects that well... don't affect and aren't 0 power effects.
		cumulativeEffects.removeIf((effect) -> effect.effectPower == 0 && !effect.isNonStackableEffect);
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
	protected void render(DrawContext drawContext, float tickDelta) {
		ArrayList<Effect> visibleEffects = isInEditMode() ? dummyEffects : UnofficialMonumentaModClient.options.effect_compress ? getCumulativeEffects() : effects;
		TextRenderer textRenderer = client.textRenderer;

		int height = getHeight();
		int width = getWidth();
		int currentY = PADDING_VERTICAL;

		drawContext.fill(0, 0, width, height, client.options.getTextBackgroundColor(UnofficialMonumentaModClient.options.overlay_opacity));

		boolean textAlightRight = UnofficialMonumentaModClient.options.effect_textAlightRight;
		for (Effect effect : visibleEffects) {
			Text text = effect.toText(tickDelta, textAlightRight);
			drawContext.drawTextWithShadow(textRenderer, text, textAlightRight ? width - PADDING_HORIZONTAL - textRenderer.getWidth(text) : PADDING_HORIZONTAL, currentY, 0xFFFFFFFF);
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

	private static List<PlayerListEntry> getCategory(String categoryName) {
		if (MinecraftClient.getInstance().player == null) {
			return List.of();
		}
		String key = categoryName.trim().toLowerCase();
		//from gold to empty entry

		//get tablist entries
		ClientPlayNetworkHandler clientPlayNetworkHandler = MinecraftClient.getInstance().player.networkHandler;
		List<PlayerListEntry> list = clientPlayNetworkHandler.getListedPlayerListEntries().stream().sorted(((PlayerListHudAccessor) MinecraftClient.getInstance().inGameHud.getPlayerListHud()).getOrdering()).limit(80L).toList();


		List<PlayerListEntry> categoryEntries = new ArrayList<>();
		boolean addEntries = false;

		for (PlayerListEntry entry: list) {
			if (entry.getDisplayName() == null) continue;

			if (entry.getDisplayName().getString().trim().toLowerCase().equals(key)) {
				addEntries = true;
			} else if (entry.getDisplayName().getString().trim().equalsIgnoreCase("") && addEntries) {
				break;
			} else if (addEntries) {
				categoryEntries.add(entry);
			}
		}

		return categoryEntries;
	}
}
