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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EffectOverlay extends DrawableHelper {
    private static final Pattern effectPattern = Pattern.compile("(?:(?<effectPower>[+-]*\\d*)%* )?(?<effectName>.*) (?<timeRemaining>\\d*:\\d*)");

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

    public ArrayList<Effect> getCumulativeEffects() {
        synchronized (effects) {
            final ArrayList<Effect> cumulativeEffects = new ArrayList<>();
            effectLoop:
            for (Effect effect : effects) {//problem, edit on the second array list edits the effect on the first one as well
                for (Effect cumulativeEffect : cumulativeEffects) {
                    if (Objects.equals(cumulativeEffect.name, effect.name)) {
                        cumulativeEffect.effectPower += effect.effectPower;
                        //get the lowest time before having to update that cumulative effect.
                        if (effect.parsedAt + effect.effectTime < cumulativeEffect.parsedAt + cumulativeEffect.effectTime) {
                            cumulativeEffect.parsedAt = effect.parsedAt;
                            cumulativeEffect.effectTime = effect.effectTime;
                        }
                        continue effectLoop;
                    }
                }
                cumulativeEffects.add(effect.clone());//add a clone of the object to avoid the actual one be referenced in changes.
            }
            return cumulativeEffects;
        }
    }

    public void tick() {
        if (lastUpdate < System.currentTimeMillis() + 2000) {
            //update every 2 seconds
            update();
            return;
        }

        for (Effect effect: effects) {
            effect.update();
        }
    }

    private static final int PADDING_VERTICAL = 5;
    private static final int PADDING_HORIZONTAL = 5;

    private final static char colorCode = '\u00A7';

    public void render(MatrixStack matrices, int scaledWidth, int scaledHeight) {
        render(matrices, scaledWidth, scaledHeight, shouldRenderDummy());
    }

    protected boolean shouldRender = true;

    public boolean shouldRenderDummy() {
        return !shouldRender;
    }

    private static final ArrayList<Effect> dummyEffects = new ArrayList<>();

    static {
        dummyEffects.add(new Effect(
                "Dummy Effect 1",
                0,
                360000
        ));
        dummyEffects.add(
                new Effect(
                        "Dummy Effect 2",
                        20,
                        360000
                )
        );
        dummyEffects.add(
                new Effect(
                        "Dummy Effect 3",
                        -20,
                        360000
                )
        );
    }

    protected void render(MatrixStack matrices, int scaledWidth, int scaledHeight, boolean bypass) {
        if ((effects.isEmpty() || !UnofficialMonumentaModClient.options.effect_active || !shouldRender) && !bypass) return;//don't render if empty except if bypass is true
        Options options = UnofficialMonumentaModClient.options;
        //if bypass isn't true, will check settings for whether it should collapse same typed effects or not, if it's using a bypass then it will render the dummy effects.
        ArrayList<Effect> cumulativeEffects = bypass ? dummyEffects : UnofficialMonumentaModClient.options.effect_compress ? getCumulativeEffects() : effects;
        TextRenderer tr = client.textRenderer;

        int x = (Math.round(scaledWidth * options.effect_offsetXRelative) + options.effect_offsetXAbsolute);
        int y = (Math.round(scaledHeight * options.effect_offsetYRelative) + options.effect_offsetYAbsolute);

        int height = getHeight();
        int width = getWidth();
        int currentY = y + PADDING_VERTICAL;

        fill(matrices, x, y, x+width, y+height, client.options.getTextBackgroundColor(0.3f));

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

    protected int getHeight() {
        TextRenderer tr = client.textRenderer;
        ArrayList<Effect> effects = shouldRenderDummy() ? dummyEffects : UnofficialMonumentaModClient.options.effect_compress ? getCumulativeEffects() : this.effects;

        return effects.size() * (tr.fontHeight + 2) + (2 * PADDING_VERTICAL);
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

        private Effect(String name, int effectTime, int effectPower, long parsedAt) {
            this.name = name;
            this.effectPower = effectPower;
            this.effectTime = effectTime;
            this.parsedAt = parsedAt;
        }

        @Override
        protected Effect clone() {
            return new Effect(name, effectTime, effectPower, parsedAt);
        }

        @Override
        public String toString() {
            return colorCode + (effectPower >= 0 ? "a" : "c") + (effectPower != 0 ? effectPower + "% " : "") + name + colorCode + "r " + getTimeRemainingAsString();
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
            int timeRemaining = 0;
            String[] r = timeRemainingStr.split(":");
            int minutes = Integer.parseInt(r[0]);//mm
            int seconds = Integer.parseInt(r[1]);//ss

            timeRemaining += minutes * 60000;
            timeRemaining += seconds * 1000;

            return new Effect(matcher.group("effectName"), effectPower, timeRemaining);
        }

        public String getTimeRemainingAsString() {
            Duration duration = Duration.ofMillis(effectTime);
            long seconds = duration.getSeconds();
            long HH = seconds / 3600;
            long MM = (seconds % 3600) / 60;
            long SS = seconds % 60;

            return String.format("%02d:%02d:%02d", HH, MM, SS);
        }
    }
}
