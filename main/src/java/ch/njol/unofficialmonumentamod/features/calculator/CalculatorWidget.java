package ch.njol.unofficialmonumentamod.features.calculator;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.gui.InventoryWidget;
import ch.njol.unofficialmonumentamod.mixins.screen.HandledScreenAccessor;
import java.awt.Rectangle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class CalculatorWidget extends InventoryWidget {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static CalculatorState state = CalculatorState.CLOSED;
    private static CalculatorMode mode = CalculatorMode.NORMAL;
    protected static void switchMode() {
        mode = CalculatorMode.values()[(mode.ordinal() + 1) % CalculatorMode.values().length];
        if (Calculator.lastWidgetInitialized != null) {
            Calculator.lastWidgetInitialized.clearValues();
        }
    }

    protected static void setState(CalculatorState state) {
        CalculatorWidget.state = state;
    }

    public static CalculatorState getState() {
        return state;
    }

    public static CalculatorMode getMode() {
        return mode;
    }

    private static double v1 = -1;
    private static double v2 = -1;
    private static double wanted = -1;

    public double tryParseDouble(String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public void onParentClosed() {
        clear();
        if (!UnofficialMonumentaModClient.options.calculatorPersistOnClosed) {
            clearValues();
            mode = CalculatorMode.NORMAL;
        }
    }

    protected void clearValues() {
        v1 = -1;
        v2 = -1;
        wanted = -1;
    }

    public void init(CalculatorMode mode) {
        clear();
        Rectangle dimension = getDimension();

        switch(mode) {
            case NORMAL: {
                Text cText = Text.of("Enter price per units in (C*)");

                TextFieldWidget cWidget = new TextFieldWidget(client.textRenderer, dimension.x + 10, dimension.y + 30, Math.max((int) Math.floor(client.textRenderer.getWidth(cText) / 1.5), 20), 10, cText);
                cWidget.setEditableColor(16777215);
                cWidget.setPlaceholder(Text.of("Price per unit"));
                if (v1 != -1) {
                    cWidget.setText(String.valueOf(v1));
                }
                cWidget.setChangedListener((string) -> v1 = tryParseDouble(string));

                addDrawableChild(cWidget);

                Text numText = Text.of("Enter number of units");

                TextFieldWidget numWidget = new TextFieldWidget(client.textRenderer, dimension.x + 10, dimension.y + 75, Math.max((int) Math.floor(client.textRenderer.getWidth(numText) / 1.5), 20), 10, numText);
                numWidget.setEditableColor(16777215);
                numWidget.setPlaceholder(Text.of("Units"));
                if (v2 != -1) {
                    numWidget.setText(String.valueOf(v2));
                }
                numWidget.setChangedListener((string) -> v2 = tryParseDouble(string));

                addDrawableChild(numWidget);
                break;
            }
            case REVERSE_EXCHANGE:
            case EXCHANGE: {
                Text currencyCompressedText = Text.of("(C*)");
                Text currencyHyperText = Text.of("(H*)");

                TextFieldWidget currencyCompressed1Widget = new TextFieldWidget(client.textRenderer, dimension.x + 10, dimension.y + 30, Math.max((int) Math.floor(client.textRenderer.getWidth(currencyCompressedText) / 1.5), 20), 10, currencyCompressedText);
                TextFieldWidget currencyCompressed2Widget = new TextFieldWidget(client.textRenderer, dimension.x + 40, dimension.y + 30, Math.max((int) Math.floor(client.textRenderer.getWidth(currencyCompressedText) / 1.5), 20), 10, currencyCompressedText);
                TextFieldWidget currencyHyperWidget = new TextFieldWidget(client.textRenderer, dimension.x + 20, dimension.y + 75, Math.max((int) Math.floor(client.textRenderer.getWidth(currencyHyperText) / 1.5), 20), 10, currencyHyperText);

                currencyCompressed1Widget.setEditableColor(16777215);
                currencyCompressed2Widget.setEditableColor(16777215);
                currencyHyperWidget.setEditableColor(16777215);

                currencyCompressed1Widget.setPlaceholder(Text.of("(C*)"));
                currencyCompressed2Widget.setPlaceholder(Text.of("(C*)"));
                currencyHyperWidget.setPlaceholder(Text.of("(H*)"));

                if (v1 != -1) {
                    currencyCompressed1Widget.setText(String.valueOf(v1));
                }
                if (v2 != -1) {
                    currencyCompressed2Widget.setText(String.valueOf(v2));
                }
                if (wanted != -1) {
                    currencyHyperWidget.setText(String.valueOf(wanted));
                }

                currencyCompressed1Widget.setChangedListener((string) -> v1 = tryParseDouble(string));
                currencyCompressed2Widget.setChangedListener((string) -> v2 = tryParseDouble(string));
                currencyHyperWidget.setChangedListener((string) -> wanted = tryParseDouble(string));

                addDrawableChild(currencyCompressed1Widget);
                addDrawableChild(currencyCompressed2Widget);
                addDrawableChild(currencyHyperWidget);
                break;
            }
            default: {
                throw new IllegalStateException("calculator mode " + mode.name() + " not handled");
            }
        }

        ButtonWidget.Builder buttonBuilder = new ButtonWidget.Builder(Text.of(mode.name), (button) -> {
            switchMode();
            clear();
            init(getMode());
        });
        buttonBuilder.dimensions(dimension.x, dimension.y, client.textRenderer.getWidth(mode.name) + 10, 12);
        addDrawableChild(buttonBuilder.build());
    }

    private static String formatResult(double result) {
        if (result < 0) {
            return "***";
        }

        double rest = (result % 64);
        double underCompressed = (rest - Math.floor(rest)) * 8;

        return (int) Math.floor((result / 64)) + "H* " + (int) Math.floor(rest) + "C*" + (underCompressed > 0 ? " " + (int) Math.floor(underCompressed) + "*" : "");
    }

    public static String getOutput(CalculatorMode mode) {
        double output;
        switch (mode) {
            case NORMAL -> output = normalLogic();
            case EXCHANGE -> output = exLogic();
            case REVERSE_EXCHANGE -> output = reverseExLogic();
            default -> throw new IllegalStateException("Calculator mode " + mode.name() + " not handled");
        }

        return formatResult(output);
    }

    public static double normalLogic() {
        if (v1 == -1 || v2 == -1) {
            return -1;
        }

        return v1 * v2;
    }

    public static double exLogic() {
        if (v1 == -1 || v2 <= 0 || wanted == -1) {
            return -1;
        }

        return (wanted * 64 * v1 + v2 - 1) / v2;
    }

    public static double reverseExLogic() {
        if (v1 <= 0 || v2 == -1 || wanted == -1) {
            return -1;
        }

        return (wanted * 64 * v2 + v1 - 1) / v1;
    }

    public CalculatorWidget(Screen parent) {
        super(parent);
    }

    public Rectangle getDimension() {
        int x = ((HandledScreenAccessor) parentScreen).getX() + ((HandledScreenAccessor) parentScreen).getBackGroundWidth();
        int y = ((HandledScreenAccessor) parentScreen).getY();
        final int width = state == CalculatorState.OPEN ? 140 : 20;
        final int height = state == CalculatorState.OPEN ? 160 : 40;

        return new Rectangle(x, y, width, height);
    }

    @Override public void renderBackground(DrawContext ctx, Rectangle dimension) {
        final int bgColour = client.options.getTextBackgroundColor(0.3f);
        ctx.fill(dimension.x, dimension.y, (int) dimension.getMaxX(), (int) dimension.getMaxY(), bgColour);
    }

    @Override public void render(DrawContext ctx, Rectangle dimension, int mouseX, int mouseY, float delta) {
        ctx.drawTextWithShadow(client.textRenderer, getOutput(mode), dimension.x + 10, dimension.y + 105, 0xffcccccc);
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        //nuh uh
    }

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
