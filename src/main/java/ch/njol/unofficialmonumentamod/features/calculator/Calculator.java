package ch.njol.unofficialmonumentamod.features.calculator;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.shard.ShardData;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import ch.njol.unofficialmonumentamod.mixins.screen.ScreenAccessor;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;

public class Calculator extends DrawableHelper {
	public static final Calculator INSTANCE = new Calculator();

	public static CalculatorWidget lastWidgetInitialized = null;

	public static void registerListeners() {
		ShardData.ShardChangedEventCallback.EVENT.register((currentShard, previousShard) -> {
			CalculatorWidget.setState((currentShard.shortShard.equals("plots") || !UnofficialMonumentaModClient.options.enableKeybindOutsidePlots) ? CalculatorWidget.CalculatorState.OPEN : CalculatorWidget.CalculatorState.CLOSED);
		});
	}

	public boolean shouldRender() {
		return mightRender()
				&& CalculatorWidget.getState() == CalculatorWidget.CalculatorState.OPEN;
	}

	public boolean mightRender() {
		return UnofficialMonumentaModClient.options.showCalculator
				&& (Objects.equals(Locations.getShortShard(), "plots") || UnofficialMonumentaModClient.options.enableKeybindOutsidePlots)
				&& (MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen ||
				MinecraftClient.getInstance().currentScreen instanceof ShulkerBoxScreen);
	}

	public boolean keyTyped(int keyCode, int scanCode, int modifiers) {
		if (mightRender()
				&& UnofficialMonumentaModClient.toggleCalculatorKeyBinding.matchesKey(keyCode, scanCode)
				&& modifiers == 0) {
			//invert current state;
			CalculatorWidget.setState(CalculatorWidget.getState() == CalculatorWidget.CalculatorState.OPEN ? CalculatorWidget.CalculatorState.CLOSED : CalculatorWidget.CalculatorState.OPEN);
			if (MinecraftClient.getInstance().currentScreen == null) {
				return false;
			}
			if (CalculatorWidget.getState() == CalculatorWidget.CalculatorState.OPEN) {
				//open
				if (lastWidgetInitialized != null) {
					return false;
				}

				CalculatorWidget widget = new CalculatorWidget(MinecraftClient.getInstance().currentScreen);
				widget.init(CalculatorWidget.getMode());
				((ScreenAccessor) MinecraftClient.getInstance().currentScreen).doAddDrawableChild(widget);
				lastWidgetInitialized = widget;
			} else {
				//close
				((ScreenAccessor) MinecraftClient.getInstance().currentScreen).doRemove(lastWidgetInitialized);
				lastWidgetInitialized = null;
			}
			return true;
		}
		return false;
	}
}
