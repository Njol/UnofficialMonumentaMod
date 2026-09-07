package ch.njol.unofficialmonumentamod.features.misc.managers;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class MessageNotifier extends HudElement {
    private static final MessageNotifier INSTANCE = new MessageNotifier();
    private static final float MAX_MESSAGE_SCALE_FACTOR = getInstance().getBaseScaleFactor() * 1.6F;

    public static MessageNotifier getInstance() {
        return INSTANCE;
    }

    private static final MinecraftClient client = MinecraftClient.getInstance();

    private final List<RenderedMessage> messages = new ArrayList<>();

    public void addOrStackMessageToQueue(RenderedMessage message) {
        for (RenderedMessage renderedMessage : messages) {
            if (renderedMessage.equals(message)) {
                //Only extend by a bit to avoid an "infinite" loop of growing.
                renderedMessage.firstRenderMillis += (System.currentTimeMillis() - renderedMessage.firstRenderMillis) / 4;

                //visually show the stacks.
                renderedMessage.setScaleFactor(Math.min(MAX_MESSAGE_SCALE_FACTOR, renderedMessage.scaleFactor * 1.05F));
                return;
            }
        }

        addMessageToQueue(message);
    }

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
    protected void render(DrawContext ctx, float tickDelta) {
        Rectangle dimension = getDimension();
        if (isInEditMode()) {
            renderOutline(ctx, new Rectangle(0, 0, dimension.width, dimension.height));
        }

        double y = Math.max(0, Math.max(dimension.getHeight(), getCurrentMessageHeights()));
        for (RenderedMessage message: messages) {
            if (y - (message.getHeight() + (2 * message.getScaleFactor())) <= 0) {
                //if inferior to 0, then it means it has gone above the maximum height.
                break;
            }

            MatrixStack matrices = ctx.getMatrices();
            matrices.push();
            matrices.scale(message.getScaleFactor(), message.getScaleFactor(), message.getScaleFactor());
            matrices.translate(5, y / message.getScaleFactor(), 0);
            message.draw(ctx, tickDelta);
            matrices.pop();

            y -= (message.getHeight() + (2 * message.getScaleFactor()));//size + small leeway
        }
    }

    public static class RenderedMessage {
        public long firstRenderMillis = -1;

        private final Text originalMessage;
        public List<Text> message;

        public boolean isInitialized() {
            return firstRenderMillis != -1;
        }

        public long dismissalTime = -1;

        public float scaleFactor;

        public RenderedMessage(Text message) {
            this.originalMessage = message;
            this.message = truncateTextToWidth(message, 1.0F);
            this.scaleFactor = 1.0F;
        }

        public RenderedMessage(Text message, float scaleFactor) {
            this.originalMessage = message;
            this.message = truncateTextToWidth(message, scaleFactor);
            this.scaleFactor = scaleFactor;
        }

        protected void setScaleFactor(float scaleFactor) {
            this.scaleFactor = scaleFactor;
            this.message = truncateTextToWidth(originalMessage, scaleFactor);
        }

        private static List<Text> truncateTextToWidth(Text originalMessage, float scaleFactor) {
            scaleFactor *= MessageNotifier.getInstance().getBaseScaleFactor();

            int width = MessageNotifier.getInstance().getDimension().width;
            if ((client.textRenderer.getWidth(originalMessage) * scaleFactor) > width) {
                List<Text> lines = new ArrayList<>();
                StringBuilder newMessage = new StringBuilder();
                String messageContent = originalMessage.getString();
                for (int i = 0; i < messageContent.length(); i++) {
                    if ((client.textRenderer.getWidth(newMessage.toString()) * scaleFactor) >= width) {
                        lines.add(MutableText.of(PlainTextContent.of(newMessage.substring(0, newMessage.length() - 1).trim() + "⁻"))
                                .fillStyle(originalMessage.getStyle()));
                        newMessage.delete(0, newMessage.length() - 1);
                    }
                    newMessage.append(messageContent.charAt(i));
                }

                if (!newMessage.isEmpty()) {
                    lines.add(MutableText.of(PlainTextContent.of(newMessage.toString().trim()))
                            .fillStyle(originalMessage.getStyle()));
                }

                return lines;
            }
            return List.of(originalMessage);
        }

        public float getScaleFactor() {
            return scaleFactor * MessageNotifier.getInstance().getBaseScaleFactor();
        }

        public double getWidth() {
            return client.textRenderer.getWidth(getLongestLine()) * getScaleFactor();
        }

        public double getHeight() {
            return client.textRenderer.fontHeight * getScaleFactor() * message.size();
        }

        private Text getLongestLine() {
            Text longestLine = null;
            double longestLineWidth = 0;
            for (Text text : message) {
                if (longestLine == null) {
                    longestLine = text;
                    longestLineWidth = client.textRenderer.getWidth(longestLine);
                    continue;
                }
                if (client.textRenderer.getWidth(text) > longestLineWidth) {
                    longestLine = text;
                    longestLineWidth = client.textRenderer.getWidth(text);
                }
            }

            return longestLine;
        }

        public void draw(DrawContext ctx, float tickDelta) {
            if (firstRenderMillis == -1) {
                firstRenderMillis = System.currentTimeMillis();
            }

            final int remTime = getInstance().getRemovalTime();
            final int animTime = getInstance().getAnimTime();

            Rectangle parentDim = getInstance().getDimension();
            //x=0 is position when set, x=width is position at the start/at the end (e.g. animation start/end position)

            //base animation duration is 3s -> 3000ms
            double currentXPosition = 0;

            if (firstRenderMillis + animTime > System.currentTimeMillis()) {
                //start animation
                long millisSinceAppearance = System.currentTimeMillis() - firstRenderMillis;
                float percent = ((float) millisSinceAppearance * tickDelta / animTime);
                currentXPosition = (parentDim.getWidth() / 2 * (1F - percent));
            }

            if ((firstRenderMillis + remTime) - System.currentTimeMillis() < animTime) {
                long millisTillRemoval = (firstRenderMillis + remTime) - System.currentTimeMillis();

                float percent = ((float) millisTillRemoval * tickDelta / animTime);
                currentXPosition = -(parentDim.getWidth() / 2 * (1F - percent));
            }

            if (willBeDismissed() && (dismissalTime + animTime > System.currentTimeMillis())) {
                long millisTillDismissal = (dismissalTime + animTime) - System.currentTimeMillis();

                float percent = ((float) millisTillDismissal * tickDelta / animTime);
                currentXPosition = -(parentDim.getWidth() / 2 * (1F - percent));

            }

            int baseX = (int) (Math.floor(currentXPosition) / getScaleFactor());
            int baseY = (int) (Math.floor(-getHeight() / 2) - ((client.textRenderer.fontHeight * getScaleFactor()) / 2));

            for (Text text : message) {
                ctx.drawText(client.textRenderer, text, baseX, baseY, 0xFFFFFF, false);
                baseY += client.textRenderer.fontHeight;
            }
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

            return originalMessage.getString().equals(o.originalMessage.getString()) && originalMessage.getStyle().equals(o.originalMessage.getStyle());
        }
    }

    //TODO remove this when overlay editor branch is merged
    private static final int OUTLINE_COLOR = 0xFFadacac;
    public static void renderOutline(DrawContext ctx, Rectangle pos) {
        renderOutline(ctx, pos, OUTLINE_COLOR);
    }

    public static void renderOutline(DrawContext ctx, Rectangle pos, int color) {
        //x1 x2
        ctx.fill(pos.x, pos.y - 1, (int) pos.getMaxX(), pos.y + 1, color);
        //y1 y2
        ctx.fill((int) pos.getMaxX() - 1, pos.y, (int) pos.getMaxX() + 1, (int) pos.getMaxY(), color);
        //x2 x1
        ctx.fill((int) pos.getMaxX(), (int) pos.getMaxY() - 1, pos.x, (int) pos.getMaxY() + 1, color);
        //y2 y1
        ctx.fill(pos.x - 1, (int) pos.getMaxY(), pos.x + 1, pos.y, color);
    }
    //till this
}