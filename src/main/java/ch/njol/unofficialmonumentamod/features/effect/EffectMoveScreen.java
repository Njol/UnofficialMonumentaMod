package ch.njol.unofficialmonumentamod.features.effect;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.options.Options;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;

import java.awt.*;

public class EffectMoveScreen extends Screen {
    public EffectMoveScreen() {
        super(new TranslatableText("unofficial-monumenta-mod.move.effect"));
    }

    private boolean draggingOverlay = false;

    private double dragX;
    private double dragY;

    @Override
    public void removed() {
        super.removed();
        draggingOverlay = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Point origin = getOverlayOrigin();
        EffectOverlay eo = UnofficialMonumentaModClient.eOverlay;

        if ((origin.x > mouseX && origin.x + eo.getWidth() < mouseX) || (origin.x < mouseX && origin.x + eo.getWidth() > mouseX)) {
            if ((origin.y > mouseY && origin.y + eo.getHeight() < mouseY) || (origin.y < mouseY && origin.y + eo.getHeight() > mouseY)) {
                //mouse in overlay

                if (button == 0) {
                    //clicked
                    synchronized (origin) {
                        dragX = mouseX - origin.x;
                        dragY = mouseY - origin.y;
                        draggingOverlay = true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void setToInBoundPos() {
        Options options = UnofficialMonumentaModClient.options;
        if (options.effect_offsetXRelative == 0.0f && options.effect_offsetXAbsolute < 0) {
            options.effect_offsetXAbsolute = 0;
        }
        if (options.effect_offsetXRelative == 1.0f && options.effect_offsetXAbsolute > width / 2.0) {
            options.effect_offsetXAbsolute = width / 2;
        }

        if (options.effect_offsetYRelative == 0.0f && options.effect_offsetYAbsolute < 0) {
            options.effect_offsetYAbsolute = 0;
        }
        if (options.effect_offsetYRelative == 1.0f && options.effect_offsetYAbsolute > height / 2.0) {
            options.effect_offsetYAbsolute = height / 2;
        }
    }

    @Override//not 100% accurate but it annoys me so not going further
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        EffectOverlay eo = UnofficialMonumentaModClient.eOverlay;
        Options options = UnofficialMonumentaModClient.options;
        if (draggingOverlay) {
            synchronized (eo) {
                double newX = mouseX - dragX;
                double newY = mouseY - dragY;

                double left = newX;
                double horizontalMiddle = Math.abs(newX + eo.getWidth() / 2.0 - width / 2.0);
                double right = width - (newX + eo.getWidth());
                double top = newY;
                double verticalMiddle = Math.abs(newY + eo.getHeight() / 2.0 - height / 2.0);
                double bottom = height - (newY + eo.getHeight());

                options.effect_offsetXRelative = left < horizontalMiddle && left < right ? 0 : horizontalMiddle < right ? 0.5f : 1;
                options.effect_offsetYRelative = top < verticalMiddle && top < bottom ? 0 : verticalMiddle < bottom ? 0.5f : 1;
                options.effect_offsetXAbsolute = (int) Math.round(newX + (options.effect_offsetXRelative * eo.getWidth()) - (this.width * options.effect_offsetXRelative));
                options.effect_offsetYAbsolute = (int) Math.round(newY + 0 - (this.height * options.effect_offsetYRelative));

                setToInBoundPos();

                //snap to original pos
                if (options.effect_offsetXRelative == 0.0f && Math.abs(options.effect_offsetXAbsolute) < 10) {
                    options.effect_offsetXAbsolute = 0;
                }
                if (options.effect_offsetYRelative == 0.0f && Math.abs(options.effect_offsetYAbsolute) < 10) {
                    options.effect_offsetYAbsolute = 0;
                }

                UnofficialMonumentaModClient.saveConfig();
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingOverlay) {
            draggingOverlay = false;

            UnofficialMonumentaModClient.saveConfig();
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    }

    public Point getOverlayOrigin() {
        Options o = UnofficialMonumentaModClient.options;

        int x = Math.round(width * o.effect_offsetXRelative) + o.effect_offsetXAbsolute;
        int y = Math.round(height * o.effect_offsetYRelative) + o.effect_offsetYAbsolute;

        return new Point(x, y);
    }
}
