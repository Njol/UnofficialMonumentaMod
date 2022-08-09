package ch.njol.unofficialmonumentamod.misc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.misc.managers.Notifier;
import ch.njol.unofficialmonumentamod.mixins.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Calculator {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final CalculatorRender renderer = new CalculatorRender();

    private static ArrayList<Integer> values = new ArrayList<>();

    private static String output;
    private static String mode = "normal"; //"normal" / "exchange" / "reverse-exchange"

    private static boolean hasShownError = false;

    private static void switchMode() {
        mode = Objects.equals(mode, "normal") ? "exchange" : Objects.equals(mode, "exchange") ? "reverse-exchange" : "normal";
    }

    public synchronized static String logic() {
        int HyperValue = (int) Math.floor((values.get(1) * values.get(0)) / 64);
        int CompressedValue = (values.get(1) * values.get(0)) % 64;
        if (!hasShownError && (HyperValue - 2147483647) > 0) {
            hasShownError = true;
            Notifier.addCustomToast(new NotificationToast(Text.of("Calculator"), Text.of("A value is higher than the 32bit integer limit, Expect glitches."), Notifier.getMillisHideTime()).setToastRender(NotificationToast.RenderType.SYSTEM));
        }

        return HyperValue + "H* " + CompressedValue + "C*";
    }

    public synchronized static String exchangeLogic() {
        int rate1 = values.get(0);//in C*1
        int rate2 = values.get(1);//in C*2

        float exchange_rate = (float) rate1/rate2;
        int toTransfer = values.get(2);//in H*1

        float result = (toTransfer * 64) * exchange_rate;

        return ((int) Math.floor(result / 64)) + "H* " + ((int) result % 64) + "C*";
    }

    public synchronized static String reverseExchangeLogic() {//stonks momento
        int wantedAmount = values.get(2);//in H*2

        int rate1 = values.get(0);//in C*1
        int rate2 = values.get(1);//in C*2

        if (rate2 == 0) return "0H* 0C*";//prevents ArithmeticException
        float exchange_rate = (float) rate1/rate2;
        int CWantedAmount = wantedAmount * 64;

        return ((int) Math.floor((CWantedAmount * (1 / exchange_rate)) / 64)) + "H* " + ((int) (CWantedAmount * (1 / exchange_rate)) % 64) + "C*";
    }

    public static void tick() {
        if (values.size() < 2) {
            output = "0H* 0C*";
            return;
        }
        if (Objects.equals(mode, "normal")) output = logic();
        else if (Objects.equals(mode, "exchange") && values.size() >= 3) output = exchangeLogic();
        else if (Objects.equals(mode, "reverse-exchange") && values.size() >= 3) output = reverseExchangeLogic();
        else output ="0H* 0C*";
    }

    public static class CalculatorRender extends DrawableHelper {
        private int x;
        private int y;

        private final Identifier background = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/gui/calc_background.png");

        public ArrayList<TextFieldWidget> children = new ArrayList<>();

        public ButtonWidget changeMode;

        CalculatorRender() {}

        public boolean shouldRender() {
            return UnofficialMonumentaModClient.options.showCalculatorInPlots && (Objects.equals(Locations.getShortShard(), "plots") && (mc.currentScreen != null && mc.currentScreen.getClass().equals(GenericContainerScreen.class)));
        }

        private void resetPosition() {
            assert mc.currentScreen != null;
            int oldX = this.x;
            int oldY = this.y;

            this.x = ((HandledScreenAccessor)mc.currentScreen).getX() + ((HandledScreenAccessor)mc.currentScreen).getBackGroundWidth();
            this.y = ((HandledScreenAccessor)mc.currentScreen).getY();

            if (changeMode != null) {
                int XOffset = changeMode.x - oldX;
                int YOffset = changeMode.y - oldY;

                changeMode.x = this.x + XOffset;
                changeMode.y = this.y + YOffset;
            }

            for (TextFieldWidget widget: children) {
                int XOffset = widget.x - oldX;
                int YOffset = widget.y - oldY;

                widget.x = this.x + XOffset;
                widget.y = this.y + YOffset;
            }
        }

        private void initFieldWidget(int x, int y, String text, int slot) {
            if (children == null) children = new ArrayList<>();
            TextFieldWidget newWidget = new TextFieldWidget(mc.textRenderer, x, y, Math.max((int) Math.floor(mc.textRenderer.getWidth(text) / 1.5), 20), 10, Text.of(text));

            newWidget.setEditableColor(16777215);

            newWidget.setChangedListener((I) -> {
                try {
                    if (Calculator.values.size() < slot+1) {
                        for (int i = 0; i < slot+1; i++) {
                            try {
                                if (Calculator.values.get(i) == null) Calculator.values.add(0);
                            } catch (IndexOutOfBoundsException e) {
                                Calculator.values.add(0);
                            }
                        }
                    }

                    Calculator.values.set(slot, Integer.parseInt(I));
                } catch (Exception ignored) {
                    Calculator.values.set(slot, 0);
                }
            });

            this.children.add(newWidget);
        }

        private void initNormal() {
            initFieldWidget(this.x+10, this.y + 30, "Enter price per unit (in C*)", 0);
            initFieldWidget(this.x+10, this.y+75, "Enter number of units", 1);
        }

        private void initExchange() {
            initFieldWidget(this.x+10, this.y+30, "C*", 0);
            initFieldWidget(this.x+40, this.y+30, "C*", 1);
            initFieldWidget(this.x+10, this.y+75, "H*", 2);
        }

        @SuppressWarnings("unchecked")//I know what I am doing, (actually I don't please send help)
        protected <T extends Element> void addChild(T child) {
            assert mc.currentScreen != null;

            ((List<Element>) mc.currentScreen.children()).add(child);
        }

        public void init() {
            if (!shouldRender()) return;
            resetPosition();

            if (this.changeMode == null) this.changeMode = new ButtonWidget(x, y, mc.textRenderer.getWidth(Calculator.mode) + 10, 10, Text.of(Calculator.mode), (buttonWidget) -> {
                Calculator.switchMode();
                assert mc.currentScreen != null;
                for (TextFieldWidget widget: children) {
                    mc.currentScreen.children().remove(widget);
                }
                Calculator.renderer.init();

                for (TextFieldWidget widget: children) {
                    addChild(widget);
                }

                for (int i = 0; i < Calculator.values.size(); i++) {
                    if (Calculator.values.get(i) != 0) Calculator.values.set(i, 0);
                }

                buttonWidget.setMessage(Text.of(Calculator.mode));
                buttonWidget.setWidth(mc.textRenderer.getWidth(Calculator.mode) + 10);
            });

            children.clear();
            if (Objects.equals(mode, "normal")) initNormal();
            else if (Objects.equals(mode, "exchange") || Objects.equals(mode, "reverse-exchange")) initExchange();
        }

        public void onClose() {
            children.clear();
            Calculator.hasShownError = false;
            Calculator.output = null;
        }

        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (!shouldRender()) return;
            resetPosition();

            mc.getTextureManager().bindTexture(background);
            super.drawTexture(matrices, x, y, 0, 0, 256, 256);

            if (changeMode != null) changeMode.render(matrices, mouseX, mouseY, delta);

            for (TextFieldWidget widget: children) {
                mc.textRenderer.draw(matrices, widget.getMessage(), widget.x, widget.y-15, 4210752);
                widget.render(matrices, mouseX, mouseY, delta);
            }

            if (output != null) {
                mc.textRenderer.draw(matrices, output, x + 10, y + 105, 4210752);
            } else mc.textRenderer.draw(matrices, "0H* 0C*", x + 10, y + 105, 4210752);
        }

    }
}
