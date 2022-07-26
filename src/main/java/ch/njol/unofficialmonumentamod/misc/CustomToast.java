package ch.njol.unofficialmonumentamod.misc;

import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public class CustomToast implements Toast {

    private final Text title;
    @Nullable
    private final Text description;
    private final boolean hasProgressBar;
    private Toast.Visibility visibility;
    private long hideTime;

    private long lastTime;
    private float lastProgress;
    private float progress;

    public CustomToast(Text title, @Nullable Text description, boolean hasProgressbar, long timeBeforeRemove) {
        this.visibility = Visibility.SHOW;

        this.hideTime = System.currentTimeMillis() + timeBeforeRemove;

        this.title = title;
        this.description = description;
        this.hasProgressBar = hasProgressbar;
    }

    public Visibility getVisibility() {
        return this.visibility;
    }
    public Text getTitle() {
        return this.title;
    }
    @Nullable
    public Text getDescription() {
        return this.description;
    }

    public void setHideTime(long newValue) {
        this.hideTime = System.currentTimeMillis() + newValue;
    }

    @Override
    public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {

        if (System.currentTimeMillis() < this.hideTime) {
            manager.getGame().getTextureManager().bindTexture(TEXTURE);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            manager.drawTexture(matrices, 0, 0, 0, 32, this.getWidth(), this.getHeight());

            if (this.description == null) {
                manager.getGame().textRenderer.draw(matrices, this.title, 5.0F, 12.0F, -11534256);
            } else {
                manager.getGame().textRenderer.draw(matrices, this.title, 5.0F, 7.0F, -11534256);
                manager.getGame().textRenderer.draw(matrices, this.description, 5.0F, 18.0F, -16777216);
            }

            if (this.hasProgressBar) {
                DrawableHelper.fill(matrices, 3, 28, 157, 29, -1);
                float f = (float) MathHelper.clampedLerp(this.lastProgress, this.progress, ((double)(float)(startTime - this.lastTime) / 100.0F));
                int i;
                if (this.progress >= this.lastProgress) {
                    i = -16755456;
                } else {
                    i = -11206656;
                }

                DrawableHelper.fill(matrices, 3, 28, (int)(3.0F + 154.0F * f), 29, i);
                this.lastProgress = f;
                this.lastTime = startTime;
            }

            return this.visibility;
        } else return this.visibility = Visibility.HIDE;
    }

    public void hide() {
        this.visibility = Visibility.HIDE;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }
}

