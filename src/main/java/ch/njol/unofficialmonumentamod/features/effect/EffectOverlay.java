package ch.njol.unofficialmonumentamod.features.effect;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.options.Options;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EffectOverlay extends DrawableHelper {
    private static final Pattern effectPattern = Pattern.compile("(?:(?<effectPower>[+-]*\\d*)%* )?(?<effectName>.*) (?<timeRemaining>\\d?\\d:\\d\\d)");

    private final ArrayList<Effect> effects = new ArrayList<>();
    private long lastUpdate = 0;


    private static final MinecraftClient client = MinecraftClient.getInstance();

    public void update() {
        if (client.getNetworkHandler() == null) return;
        effects.clear();
        Collection<PlayerListEntry> entries = client.getNetworkHandler().getPlayerList();

        for (PlayerListEntry entry: entries) {
            Effect effect = Effect.from(entry);
            if (effect != null) {
                effects.add(effect);
            }
        }

        lastUpdate = System.currentTimeMillis();
    }

    public ArrayList<Effect> getCumulativeEffect() {
        final ArrayList<Effect> cumulativeEffects = new ArrayList<>();
        for (Effect effect: effects) {
            boolean stacked = false;
            for (Effect cumulativeEffect: cumulativeEffects) {
                if (Objects.equals(cumulativeEffect.name, effect.name)) {
                    stacked = true;
                    cumulativeEffect.effectPower+= effect.effectPower;
                    //get the lowest time before having to update that cumulative effect.
                    if (effect.parsedAt + effect.effectTime < cumulativeEffect.parsedAt + cumulativeEffect.effectTime) {
                        cumulativeEffect.parsedAt = effect.parsedAt;
                        cumulativeEffect.effectTime = effect.effectTime;
                    }
                }
            }
            if (!stacked) cumulativeEffects.add(effect);
        }

        return cumulativeEffects;
    }

    public void tick() {
        if (lastUpdate < System.currentTimeMillis() + 2000) {
            //update every 2 seconds
            update();
            return;
        }

        for (Effect effect: effects) {
            effect.update();
            if (effect.effectTime < 0) {// sometimes bug out when cumulative effects exist.
                effects.remove(effect);
            }
        }
    }
    //0-0 => top left -> just need to get da formula

    private static final int PADDING_VERTICAL = 5;
    private static final int PADDING_HORIZONTAL = 5;

    private final static char colorCode = '\u00A7';

    protected boolean shouldRender = true;

    public void render(MatrixStack matrices, int scaledWidth, int scaledHeight) {
        if (!shouldRender) return;
        Options options = UnofficialMonumentaModClient.options;
        ArrayList<Effect> cumulativeEffects = getCumulativeEffect();
        TextRenderer tr = client.textRenderer;

        int x = (int) (Math.round(scaledWidth * options.effect_offsetXRelative) + options.effect_offsetXAbsolute);
        int y = (int) (Math.round(scaledHeight * options.effect_offsetYRelative) + options.effect_offsetYAbsolute);

        int height = getHeight(false);
        int currentY = y + PADDING_VERTICAL;

        for (Effect effect: cumulativeEffects) {
            Text text = Text.of(colorCode + (effect.effectPower >= 0 ? "a" : "c") + (effect.effectPower != 0 ? effect.effectPower + "% " : "") + effect.name + colorCode + "r " + effect.getTimeRemainingAsString());
            List<OrderedText> ot = tr.wrapLines(text, 200);
            for (OrderedText t: ot) {
                tr.draw(matrices, t, x + PADDING_HORIZONTAL, currentY, 0xFFFFFFFF);
                currentY+= tr.fontHeight + 2;
            }
        }
    }

    protected int getWidth() {
        return 200 + (2 * PADDING_HORIZONTAL);
    }

    protected int getHeight(boolean dummy) {
        TextRenderer tr = client.textRenderer;
        return dummy ? 7 * (tr.fontHeight + 2) + (2 * PADDING_VERTICAL) : effects.size() * (tr.fontHeight + 2) + (2 * PADDING_VERTICAL);
    }

    protected void renderDummy(MatrixStack matrixStack, int scaledWidth, int scaledHeight) {
        TextRenderer tr = client.textRenderer;
        Options options = UnofficialMonumentaModClient.options;

        int x = (int) (Math.round(scaledWidth * options.effect_offsetXRelative) + options.effect_offsetXAbsolute);
        int y = (int) (Math.round(scaledHeight * options.effect_offsetYRelative) + options.effect_offsetYAbsolute);

        System.out.println("x: " + x + " y:  " + y);

        int height = getHeight(true);
        int width = getWidth();
        int currentY = y + PADDING_VERTICAL;
        fill(matrixStack, x, y, x+width, y+height, client.options.getTextBackgroundColor(0.3f));
        List<OrderedText> loremIpsum = tr.wrapLines(Text.of("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque porta at mi a sodales. Aliquam commodo, magna sed tempus posuere, sem leo ultrices leo, eu aliquam purus est ac enim. Donec ac sodales urna. Maecenas efficitur gravida luctus. Etiam et."), 200);
        for (OrderedText orderedText : loremIpsum) {
            tr.draw(matrixStack, orderedText, x + PADDING_HORIZONTAL, currentY, 0xFFFFFFFF);
            currentY+= tr.fontHeight + 2;
        }
    }

    public static class Effect {
        String name;
        int effectTime;
        int effectPower;
        long parsedAt;

        public Effect(String name, int effectPower, int effectTime) {
            this.name = name;
            this.effectPower = effectPower;
            this.effectTime = effectTime;
            parsedAt = System.currentTimeMillis();
        }

        public void update() {//new time - old time + effect time
            effectTime = (int) (effectTime - (System.currentTimeMillis() - parsedAt));
        }

        public static Effect from(PlayerListEntry entry) {
            if (entry.getDisplayName() == null) return null;
            Matcher matcher = effectPattern.matcher(entry.getDisplayName().getString());
            if (!matcher.matches()) return null;
            String effectPowerStr = matcher.group("effectPower");
            if (effectPowerStr != null) {
                if (effectPowerStr.charAt(0) == '+') {
                    effectPowerStr = effectPowerStr.substring(1);
                }
            }
            final int effectPower = effectPowerStr != null ? Integer.parseInt(effectPowerStr) : 0;

            String timeRemainingStr = matcher.group("timeRemaining");
            //minutes:seconds -> 99:60 max
            int timeRemaining = 0;

            if (timeRemainingStr.equals("0:00")) return null;//don't create one if it's "deleting it"

            for (int i = 0; i < timeRemainingStr.length(); i++) {
                char chr = timeRemainingStr.charAt(i);
                if (chr == ':') continue;


                if (timeRemainingStr.length() == 5) {// 00:00
                    switch (i) {
                        case 0 -> timeRemaining += Character.getNumericValue(chr) * 600000;
                        case 1 -> timeRemaining += Character.getNumericValue(chr) * 60000;
                        case 3 -> timeRemaining += Character.getNumericValue(chr) * 10000;
                        case 4 -> timeRemaining += Character.getNumericValue(chr) * 1000;
                    }
                } else if (timeRemainingStr.length() == 4) {// 0:00
                    switch (i) {
                        case 0 -> timeRemaining += Character.getNumericValue(chr) * 60000;
                        case 2 -> timeRemaining += Character.getNumericValue(chr) * 10000;
                        case 3 -> timeRemaining += Character.getNumericValue(chr) * 1000;
                    }
                }
            }

            return new Effect(matcher.group("effectName"), effectPower, timeRemaining);
        }

        public String getTimeRemainingAsString() {
            int t = effectTime;
            int minutes10 = Math.round(t / 600000);
            t-= (minutes10 * 600000);
            int minutes1 = Math.round(t / 60000);
            t-= (minutes1 * 60000);
            int seconds10 = Math.round(t / 10000);
            t-= (seconds10 * 10000);
            int seconds1 = Math.round(t / 1000);
            t-= (seconds1 * 1000);

            return minutes10+ +minutes1+":"+seconds10+seconds1;
        }
    }
}
