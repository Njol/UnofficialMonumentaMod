package ch.njol.unofficialmonumentamod.misc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class NotificationToast implements Toast {

    Identifier TEXTURE = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "/textures/gui/notifications.png");

    private final Text title;

    @Nullable
    private ArrayList<OrderedText> lines;
    @Nullable
    private final Text originalDescription;

    private Toast.Visibility visibility;
    private long hideTime;

    private RenderType renderType;

    public NotificationToast(Text title, @Nullable Text description, long timeBeforeRemove) {
        this.visibility = Visibility.SHOW;
        this.renderType = RenderType.RUSTIC;
        this.hideTime = System.currentTimeMillis() + timeBeforeRemove;

        this.title = title;
        this.originalDescription = description;
        this.lines = getTextAsList(originalDescription, this.renderType.offset);
    }

    private static ArrayList<OrderedText> getTextAsList(@Nullable Text text, @Nullable Integer offset) {
        if (text == null) {
            return new ArrayList<>();
        } else {
            ArrayList<OrderedText> list = new ArrayList<>();
            for (String line: text.getString().split("\n")) {
                list.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(StringVisitable.plain(line), 160 - (offset != null ? offset : 0)));
            }
            return list;
        }
    }

    public void wrapDescription() {
        if (this.originalDescription == null) return;
        this.lines = getTextAsList(this.originalDescription, this.renderType.offset);
    }

    public NotificationToast setToastRender(RenderType type) {
        this.renderType = type;
        wrapDescription();
        return this;
    }

    public Visibility getVisibility() {
        return this.visibility;
    }
    public Text getTitle() {
        return this.title;
    }
    @Nullable
    public ArrayList<OrderedText> getDescription() {
        return this.lines;
    }

    public void setHideTime(long newValue) {
        this.hideTime = System.currentTimeMillis() + newValue;
    }

    @Override
    public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
        if (System.currentTimeMillis() < this.hideTime) {
            manager.getGame().getTextureManager().bindTexture(TEXTURE);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            manager.drawTexture(matrices, 0, 0, 0, (this.renderType.type - 1) * 32, this.getWidth(), this.getHeight());

            int i = this.getWidth();
            int o;
            if (i == 160 && this.lines.size() <= 1) {
                manager.drawTexture(matrices, 0, 0, 0, (this.renderType.type - 1) * 32, i, this.getHeight());
            } else {
                o = this.getHeight() + Math.max(0, this.lines.size() - 1) * 12;
                int m = Math.min(4, o - 28);
                this.drawPart(matrices, manager, i, 0, 0, 28);

                for(int n = 28; n < o - m; n += 10) {
                    this.drawPart(matrices, manager, i, 16, n, Math.min(16, o - n - m));
                }

                this.drawPart(matrices, manager, i, 32 - m, o - m, m);
            }

            if (this.lines.size() == 0) {
                manager.getGame().textRenderer.draw(matrices, this.title, center(manager.getGame().textRenderer.getWidth(this.title)), 7.0F, -11534256);
            } else {
                manager.getGame().textRenderer.draw(matrices, this.title, center(manager.getGame().textRenderer.getWidth(this.title)), 7.0F, -11534256);
                for(o = 0; o < this.lines.size(); ++o) {
                    manager.getGame().textRenderer.draw(matrices, this.lines.get(o), center(manager.getGame().textRenderer.getWidth(this.lines.get(o))), (float)(18 + o * 12), 0x404040);
                }
            }

            return this.visibility;
        } else return this.visibility = Visibility.HIDE;
    }

    private void drawPart(MatrixStack matrices, ToastManager manager, int width, int textureV, int y, int height) {
        int i = textureV == 0 ? 20 : 5;
        int j = Math.min(60, width - i);
        manager.drawTexture(matrices, 0, y, 0, (this.renderType.type - 1) * 32 + textureV, i, height);

        for(int k = i; k < width - j; k += 64) {
            manager.drawTexture(matrices, k, y, 32, (this.renderType.type - 1) * 32 + textureV, Math.min(64, width - k - j), height);
        }

        manager.drawTexture(matrices, width - j, y, 160 - j, (this.renderType.type - 1) * 32 + textureV, j, height);
    }

    private int center(int fontWidth) {
        //toasts are 160x32
        int toastWidth = 160;

        if (((toastWidth - fontWidth)/2) < renderType.offset) {
            //text overlaps with first offset
            return ((toastWidth - fontWidth)/2) + renderType.offset;
        } else if (((toastWidth - fontWidth)/2) > toastWidth - renderType.offset) {
            //text overlaps with second offset
            return ((toastWidth - fontWidth)/2) - renderType.offset;
        } else return ((toastWidth - fontWidth) / 2);
    }

    private int align_right(int fontWidth) {
        return (fontWidth - this.renderType.offset);
    }

    private int align_left(int fontWidth) {
        return this.renderType.offset;
    }

    public void hide() {
        this.visibility = Visibility.HIDE;
    }

    public enum RenderType {
        ACHIEVEMENT(1, 10),
        RUSTIC(2, 10),
        SYSTEM(3, 20),
        TUTORIAL(4, 10);


        final int type;
        final int offset;

        RenderType(int type, int offset) {
            this.type = type;
            this.offset = offset;
        }
    }
}

