package ch.njol.unofficialmonumentamod.features.effect;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.options.Options;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;

import java.awt.*;

public class EffectMoveScreen extends Screen {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    public EffectMoveScreen() {
        super(new TranslatableText("unofficial-monumenta-mod.move.effect"));
        UnofficialMonumentaModClient.eOverlay.shouldRender = false;
    }

    private double dragX;
    private double dragY;

    private boolean draggingOverlay = false;

    @Override
    public void close() {
        super.close();
        UnofficialMonumentaModClient.eOverlay.shouldRender = true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        EffectOverlay eo = UnofficialMonumentaModClient.eOverlay;
        synchronized (eo) {
                    //is hovering the overlay's dummy
                    draggingOverlay = true;
                    Point overlayOrigin = getOverlayOrigin();
                    dragX = mouseX - overlayOrigin.x;
                    dragY = mouseY - overlayOrigin.y;

                    System.out.println("Printing mouseClicked data");
                    System.out.println(dragX);
                    System.out.println(dragY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!draggingOverlay) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        Options options = UnofficialMonumentaModClient.options;
        System.out.println("trying to move");
        EffectOverlay eo = UnofficialMonumentaModClient.eOverlay;
        synchronized (eo) {
            double left = mouseX - dragX;
            double horizontalMiddle = Math.abs(left + eo.getWidth() / 2.0 - width / 2.0);
            double right = width - (left + eo.getWidth());
            double top = mouseY - dragY;
            double verticalMiddle = Math.abs(top + eo.getHeight(true) / 2.0 - height / 2.0);
            double bottom = height - (left + eo.getHeight(true));

            UnofficialMonumentaModClient.options.effect_offsetXRelative = left < horizontalMiddle && left < right ? 0 : horizontalMiddle < right ? 0.5f : 1;
            UnofficialMonumentaModClient.options.effect_offsetYRelative = top < verticalMiddle && top < bottom ? 0 : verticalMiddle < bottom ? 0.5f : 1;;
            UnofficialMonumentaModClient.options.effect_offsetXAbsolute = (int) Math.round(left + (width) - (width * options.effect_offsetXRelative));
            UnofficialMonumentaModClient.options.effect_offsetYAbsolute = (int) Math.round(right - (height * options.effect_offsetYRelative));

            UnofficialMonumentaModClient.saveConfig();
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingOverlay) {
            draggingOverlay = false;
            dragX = 0;
            dragY = 0;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!MinecraftClient.isFancyGraphicsOrBetter()) {
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.defaultBlendFunc();
        }
        EffectOverlay eo = UnofficialMonumentaModClient.eOverlay;
        eo.renderDummy(matrices, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        super.render(matrices, mouseX, mouseY, delta);
    }

    private Point getOverlayOrigin() {
        Options options = UnofficialMonumentaModClient.options;
       int x = (int) (Math.round(width * options.effect_offsetXRelative) + options.effect_offsetXAbsolute);
       int y = (int) (Math.round(height * options.effect_offsetYRelative) + options.effect_offsetYAbsolute);
       return new Point(x, y);
    }
}
