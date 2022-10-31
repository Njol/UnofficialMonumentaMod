package ch.njol.unofficialmonumentamod.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class MoveScreen extends Screen {
    protected MoveScreen(Text title) {
        super(title);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Text text = Text.of("Press R to reset to default values");
        double x = width * 0.5f - (tr.getWidth(text) / 2.0);
        double y = height * 0.5f;

        tr.drawWithShadow(matrices, text,  (float) x, (float) y, 0xFFFFFFFF);

        super.render(matrices, mouseX, mouseY, delta);
    }
}
