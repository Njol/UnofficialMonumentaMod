package ch.njol.unofficialmonumentamod.features.misc.managers;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class MessageNotifier extends HudElement {
    private static final MessageNotifier INSTANCE = new MessageNotifier();

    public static MessageNotifier getInstance() {
        return INSTANCE;
    }

    private static final MinecraftClient client = MinecraftClient.getInstance();

    private final List<RenderedMessage> messages = new ArrayList<>();//mostly using for queue stuff :P

    public void addMessageToQueue(RenderedMessage message) {
        if (isRenderedQueueFull() && UnofficialMonumentaModClient.options.notifierEarlyDismiss) {
            //dismiss first element and add this to end
            for (RenderedMessage message1: messages) {
                if (!message1.willBeDismissed()) {
                    message1.setAsDismissed();
                    break;
                }
            }
        }
        messages.add(message);
    }

    private boolean isRenderedQueueFull() {
        return getCurrentMessageHeights() >= getHeight();
    }

    private int getCurrentMessageHeights() {
        int size = 0;
        for (RenderedMessage message : messages) {
            size += (int) message.getHeight();
        }

        return size;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected boolean isVisible() {
        return !messages.isEmpty();
    }

    @Override
    protected int getWidth() {
        return (int) Math.floor(200 * getBaseScaleFactor());
    }

    @Override
    protected int getHeight() {
        return (int) Math.floor(140 * getBaseScaleFactor());//~10 normal size messages
    }

    @Override
    protected ElementPosition getPosition() {
        return UnofficialMonumentaModClient.options.notifierPosition;
    }

    @Override
    protected int getZOffset() {
        return 0;
    }

    private float getBaseScaleFactor() {
        return UnofficialMonumentaModClient.options.notifierScaleFactor;
    }

    public int getRemovalTime() {
        return (int) UnofficialMonumentaModClient.options.notifierShowTime * 1000;
    }

    public int getAnimTime() {
        return getRemovalTime() / 30;
    }

    public void tick() {
        //through dismissal or reached end.
        messages.removeIf(message -> (message.willBeDismissed() && (message.dismissalTime + getAnimTime() < System.currentTimeMillis())) || (message.isInitialized() && message.firstRenderMillis + getRemovalTime() < System.currentTimeMillis()));
    }

    @Override
    protected void render(MatrixStack matrices, float tickDelta) {
        Rectangle dimension = getDimension();
        if (isInEditMode()) {
            renderOutline(matrices, new Rectangle(0, 0, dimension.width, dimension.height));
        }

        double y = Math.max(0, Math.max(dimension.getHeight(), getCurrentMessageHeights()));
        for (RenderedMessage message: messages) {
            if (y - (message.getHeight() + (2 * message.getScaleFactor())) <= 0) {
                //if inferior to 0, then it means it has gone above the maximum height.
                break;
            }

            matrices.push();
            matrices.scale(message.getScaleFactor(), message.getScaleFactor(), message.getScaleFactor());
            matrices.translate(5, y / message.getScaleFactor(), 0);
            message.draw(matrices, tickDelta);
            matrices.pop();

            y -= (message.getHeight() + (2 * message.getScaleFactor()));//size + small leeway
        }
    }

    //OPTIONS
    //position
    //max height
    //max width
    //scale factor?
    //notification render time (time before a message is dismissed)
    //early dismissal if notifications overflow the stack (e.g: if filled, then first will be forcibly dismissed even if it was supposed to stay much longer).

    public static class RenderedMessage {
        public long firstRenderMillis = -1;
        public final Text message;

        public boolean isInitialized() {
            return firstRenderMillis != -1;
        }

        public long dismissalTime = -1;

        public final float scaleFactor;

        public RenderedMessage(Text message) {
            this.message = message;
            this.scaleFactor = 1.0F;
        }

        public RenderedMessage(Text message, float scaleFactor) {
            this.message = message;
            this.scaleFactor = scaleFactor;
        }

        public float getScaleFactor() {
            return scaleFactor * MessageNotifier.getInstance().getBaseScaleFactor();
        }

        public double getWidth() {
            return client.textRenderer.getWidth(message) * getScaleFactor();
        }

        public double getHeight() {
            return client.textRenderer.fontHeight * getScaleFactor();
        }

        public void draw(MatrixStack matrices, float tickDelta) {
            if (firstRenderMillis == -1) {
                firstRenderMillis = System.currentTimeMillis();
            }

            final int remTime = getInstance().getRemovalTime();
            final int animTime = getInstance().getAnimTime();

            Rectangle parentDim = getInstance().getDimension();
            //x=0 is position when set, x=width is position at the start/at the end (e.g animation start/end position)

            //base animation duration is 3s -> 3000ms
            double currentXPosition = 0;

            if (firstRenderMillis + animTime > System.currentTimeMillis()) {
                //start animation
                long millisSinceAppearance = System.currentTimeMillis() - firstRenderMillis;
                float percent = ((float) millisSinceAppearance / animTime);
                currentXPosition = (parentDim.getWidth() / 2 * (1F - percent));
            }

            if ((firstRenderMillis + remTime) - System.currentTimeMillis() < animTime) {
                long millisTillRemoval = (firstRenderMillis + remTime) - System.currentTimeMillis();

                float percent = ((float) millisTillRemoval / animTime);
                currentXPosition = -(parentDim.getWidth() / 2 * (1F - percent));
            }

            if (willBeDismissed() && (dismissalTime + animTime > System.currentTimeMillis())) {
                long millisTillDismissal = (dismissalTime + animTime) - System.currentTimeMillis();

                float percent = ((float) millisTillDismissal / animTime);
                currentXPosition = -(parentDim.getWidth() / 2 * (1F - percent));

            }

            client.textRenderer.draw(matrices, message, (int) Math.floor(currentXPosition) / getScaleFactor(), (int) Math.floor(-getHeight() / 2), 0xFFFFFF);
        }

        public void setAsDismissed()
        {
            this.dismissalTime = System.currentTimeMillis();
        }

        public boolean willBeDismissed() {
            return dismissalTime != -1;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof RenderedMessage o)) {
                return false;
            }

            return message.equals(o.message);
        }
    }

    //TODO remove this when overlay editor branch is merged
    private static final int OUTLINE_COLOR = 0xFFadacac;
    public static void renderOutline(MatrixStack matrices, Rectangle pos) {
        renderOutline(matrices, pos, OUTLINE_COLOR);
    }

    public static void renderOutline(MatrixStack matrices, Rectangle pos, int color) {
        //x1 x2
        DrawableHelper.fill(matrices, pos.x, pos.y - 1, (int) pos.getMaxX(), pos.y + 1, color);
        //y1 y2
        DrawableHelper.fill(matrices, (int) pos.getMaxX() - 1, pos.y, (int) pos.getMaxX() + 1, (int) pos.getMaxY(), color);
        //x2 x1
        DrawableHelper.fill(matrices, (int) pos.getMaxX(), (int) pos.getMaxY() - 1, pos.x, (int) pos.getMaxY() + 1, color);
        //y2 y1
        DrawableHelper.fill(matrices, pos.x - 1, (int) pos.getMaxY(), pos.x + 1, pos.y, color);
    }
    //till this
}
