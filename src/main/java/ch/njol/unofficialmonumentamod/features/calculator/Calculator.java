package ch.njol.unofficialmonumentamod.features.calculator;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import ch.njol.unofficialmonumentamod.mixins.screen.HandledScreenAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import static java.lang.Integer.parseInt;

public class Calculator extends DrawableHelper {
	public static final Calculator INSTANCE = new Calculator();

	private static final MinecraftClient mc = MinecraftClient.getInstance();

	private static CalculatorMode mode = CalculatorMode.NORMAL;
	private static CalculatorState state = CalculatorState.OPEN;

	private static final ArrayList<Integer> values = new ArrayList<>();

	public static String output;

	private static void switchMode() {
		mode = (mode == CalculatorMode.NORMAL ? CalculatorMode.EXCHANGE : mode == CalculatorMode.EXCHANGE ? CalculatorMode.REVERSE_EXCHANGE : CalculatorMode.NORMAL);
	}

	public synchronized static String logic() {
		int HyperValue = (int) Math.floor((values.get(1) * values.get(0)) / 64.0);
		int CompressedValue = (values.get(1) * values.get(0)) % 64;

		return HyperValue + "H* " + CompressedValue + "C*";
	}

	public synchronized static String exchangeLogic() {
		if (values.size() < 3) {
			return "0H* 0C*";
		}
		int rate1 = values.get(0);//in C*1
		int rate2 = values.get(1);//in C*2

		float exchange_rate = (float) rate1 / rate2;
		int toTransfer = values.get(2);//in H*1

		float result = (toTransfer * 64) * exchange_rate;

		return ((int) Math.floor(result / 64)) + "H* " + ((int) result % 64) + "C*";
	}

	public synchronized static String reverseExchangeLogic() {//stonks momento
		if (values.size() < 3) {
			return "0H* 0C*";
		}
		int wantedAmount = values.get(2);//in H*2

		int rate1 = values.get(0);//in C*1
		int rate2 = values.get(1);//in C*2

		if (rate2 == 0) {
			return "0H* 0C*";//prevents ArithmeticException
		}
		float exchange_rate = (float) rate1 / rate2;
		int CWantedAmount = wantedAmount * 64;

		return ((int) Math.floor((CWantedAmount * (1 / exchange_rate)) / 64)) + "H* " + ((int) (CWantedAmount * (1 / exchange_rate)) % 64) + "C*";
	}

	public static void tick() {
		if (INSTANCE.children.size() < 2) {
			output = "0H* 0C*";
			return;
		}

		for (int i = 0; i < INSTANCE.children.size(); i++) {
			String input = INSTANCE.children.get(i).getText();
			try {
				int e = input.isEmpty() ? 0 : parseInt(input);//!NumberFormatException
				if (values.size() >= i + 1) {
					values.set(i, e);
				} else {
					values.add(e);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

		switch (mode) {
			case NORMAL -> {
				output = logic();
				return;
			}
			case EXCHANGE -> {
				output = exchangeLogic();
				return;
			}
			case REVERSE_EXCHANGE -> {
				output = reverseExchangeLogic();
				return;
			}
		}

		output = "0H* 0C*";
	}


	//region renderer
	private int x;
	private int y;

	public final ArrayList<TextFieldWidget> children = new ArrayList<>();

	public ButtonWidget changeMode;

	public boolean shouldRender() {
		return mightRender()
			       && state == CalculatorState.OPEN;
	}

	public boolean mightRender() {
		return UnofficialMonumentaModClient.options.showCalculator
			       && Objects.equals(Locations.getShortShard(), "plots")
			       && (mc.currentScreen instanceof GenericContainerScreen ||
				           mc.currentScreen instanceof ShulkerBoxScreen);
	}

	public synchronized void resetPosition() {
		if (mc.currentScreen == null) {
			return;
		}

		int oldX = this.x;
		int oldY = this.y;

		this.x = ((HandledScreenAccessor) mc.currentScreen).getX() + ((HandledScreenAccessor) mc.currentScreen).getBackGroundWidth();
		this.y = ((HandledScreenAccessor) mc.currentScreen).getY();

		if (changeMode != null) {
			int XOffset = changeMode.x - oldX;
			int YOffset = changeMode.y - oldY;

			changeMode.x = this.x + XOffset;
			changeMode.y = this.y + YOffset;
		}

		for (TextFieldWidget widget : children) {
			int XOffset = widget.x - oldX;
			int YOffset = widget.y - oldY;

			widget.x = this.x + XOffset;
			widget.y = this.y + YOffset;
		}
	}

	private void initChildren(int x, int y, String text) {
		TextFieldWidget newWidget = new TextFieldWidget(mc.textRenderer, x, y, Math.max((int) Math.floor(mc.textRenderer.getWidth(text) / 1.5), 20), 10, Text.of(text));

		newWidget.setEditableColor(16777215);
		this.children.add(newWidget);
	}

	private void initNormal() {
		initChildren(this.x + 10, this.y + 30, "Enter price per units in (C*)");
		initChildren(this.x + 10, this.y + 75, "Enter number of units");
	}

	private void initExchange() {
		initChildren(this.x + 10, this.y + 30, "C*");
		initChildren(this.x + 40, this.y + 30, "C*");
		initChildren(this.x + 20, this.y + 75, "H*");
	}

	@SuppressWarnings("unchecked")
	protected <T extends Element> void addChild(T child) {
		if (mc.currentScreen == null) {
			return;
		}

		((List<Element>) mc.currentScreen.children()).add(child);
	}

	public void init() {
		if (!shouldRender()) {
			return;
		}
		resetPosition();

		if (this.changeMode == null) {
			this.changeMode = new ButtonWidget(x, y, mc.textRenderer.getWidth(mode.name) + 10, 12, Text.of(mode.name), (buttonWidget) -> {
				Calculator.switchMode();
				if (mc.currentScreen == null) {
					return;
				}
				for (TextFieldWidget widget : children) {
					mc.currentScreen.children().remove(widget);
				}
				init();

				buttonWidget.setMessage(Text.of(mode.name));
				buttonWidget.setWidth(mc.textRenderer.getWidth(mode.name) + 10);

				for (TextFieldWidget widget : children) {
					addChild(widget);
				}
			});
		}

		children.clear();

		switch (mode) {
			case NORMAL -> initNormal();
			case EXCHANGE, REVERSE_EXCHANGE -> initExchange();
		}
	}

	public void onClose() {
		children.clear();
		output = null;
		values.clear();
	}

	public boolean keyTyped(int keyCode, int scanCode, int modifiers) {
		if (mightRender()
			    && UnofficialMonumentaModClient.toggleCalculatorKeyBinding.matchesKey(keyCode, scanCode)
			    && modifiers == 0) {
			//invert current state;
			state = state == CalculatorState.OPEN ? CalculatorState.CLOSED : CalculatorState.OPEN;
			if (mc.currentScreen == null) {
				return false;
			}
			if (state == CalculatorState.OPEN) {
				for (TextFieldWidget widget : children) {
					mc.currentScreen.children().remove(widget);
				}
				init();
				for (TextFieldWidget widget : children) {
					addChild(widget);
				}
				addChild(changeMode);
			} else {
				for (TextFieldWidget widget : children) {
					mc.currentScreen.children().remove(widget);
				}
				mc.currentScreen.children().remove(changeMode);
			}
			return true;
		}
		return false;
	}

	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		if (!shouldRender()) {
			return;
		}

		fill(matrices, x, y, x + 160, y + 140, mc.options.getTextBackgroundColor(0.3f));

		changeMode.render(matrices, mouseX, mouseY, delta);
		for (TextFieldWidget widget : children) {
			mc.textRenderer.drawWithShadow(matrices, widget.getMessage(), widget.x, widget.y - 15, 0xffcccccc);
			widget.render(matrices, mouseX, mouseY, delta);
		}

		mc.textRenderer.drawWithShadow(matrices, Objects.requireNonNullElse(output, "0H* 0C*"), x + 10, y + 105, 0xffcccccc);
	}
	//endregion


	public enum CalculatorState {
		OPEN,
		CLOSED
	}

	public enum CalculatorMode {
		NORMAL("Normal"),
		EXCHANGE("Exchange"),
		REVERSE_EXCHANGE("Reverse exchange");

		public final String name;

		CalculatorMode(String name) {
			this.name = name;
		}
	}
}
