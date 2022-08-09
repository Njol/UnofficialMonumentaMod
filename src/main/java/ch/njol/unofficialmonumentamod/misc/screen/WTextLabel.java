package ch.njol.unofficialmonumentamod.misc.screen;

import io.github.cottonmc.cotton.gui.client.LibGuiClient;
import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class WTextLabel extends WWidget {
    protected  Supplier<Text> text;
    protected HorizontalAlignment alignment = HorizontalAlignment.LEFT;
    protected int color;
    protected int darkmodeColor;

    public static final int DEFAULT_TEXT_COLOR = 0x404040;
    public static final int DEFAULT_DARKMODE_TEXT_COLOR = 0xbcbcbc;

    public WTextLabel(Supplier<Text> text) {
        this(text, DEFAULT_TEXT_COLOR);
    }

    public WTextLabel(Supplier<Text> text, int color) {
        this.text = text;
        this.color = color;
        this.darkmodeColor = (color==DEFAULT_TEXT_COLOR) ? DEFAULT_DARKMODE_TEXT_COLOR : color;
    }

    public WTextLabel setDarkmodeColor(int color) {
        darkmodeColor = color;
        return this;
    }
    public WTextLabel disableDarkmode() {
        this.darkmodeColor = this.color;
        return this;
    }
    public WTextLabel setColor(int color, int darkmodeColor) {
        this.color = color;
        this.darkmodeColor = darkmodeColor;
        return this;
    }
    public WTextLabel setAlignment(HorizontalAlignment align) {
        this.alignment = align;
        return this;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void paint(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
        ScreenDrawing.drawString(matrices, text.get().asOrderedText(), alignment, x, y, this.getWidth(), LibGuiClient.config.darkMode ? darkmodeColor : color);
    }

    public WTextLabel setText(Supplier<Text> text) {
        this.text = text;
        return this;
    }

    @Override
    public boolean canResize() {
        return true;
    }
    @Override
    public void setSize(int x, int y) {
        super.setSize(x, 20);
    }
}
