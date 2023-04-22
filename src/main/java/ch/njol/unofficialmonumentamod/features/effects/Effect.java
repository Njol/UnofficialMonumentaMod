package ch.njol.unofficialmonumentamod.features.effects;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class Effect {

	private static final DecimalFormat POWER_FORMAT = new DecimalFormat("+0.##;-#");
	private static final Pattern EFFECT_PATTERN = Pattern.compile("(?:(?<effectPower>[+-]?\\d+(?:\\.\\d+)?)(?<percentage>%)? )?(?<effectName>.*) (?<timeRemaining>\\d*:\\d*)");

	String name;
	int effectTime;
	float effectPower;
	boolean isPercentage = false;
	boolean positiveEffect = true;
	final boolean isNonStackableEffect;

	public Effect(String name, float effectPower, int effectTime) {
		isNonStackableEffect = effectPower == 0;
		this.name = name;
		this.effectPower = effectPower;
		this.effectTime = effectTime;
	}

	public Effect(String name, float effectPower, int effectTime, boolean isPercentage) {
		isNonStackableEffect = effectPower == 0;
		this.name = name;
		this.effectPower = effectPower;
		this.effectTime = effectTime;
		this.isPercentage = isPercentage;
	}

	public boolean isPositive() {
		return positiveEffect;
	}

	@Override
	protected Effect clone() {
		//DON'T FORGET TO MAKE SURE THIS SETS ALL THE PARAMETERS CORRECTLY
		Effect effect = new Effect(name, effectPower, effectTime, isPercentage);
		effect.positiveEffect = positiveEffect;
		return effect;
	}

	public Text toText(float tickDelta, boolean rightAligned) {
		Text timeText = new LiteralText((rightAligned ? " " : "") + getTimeRemainingAsString(tickDelta) + (rightAligned ? "" : " "));
		Style effectStyle = Style.EMPTY.withColor((isPositive() == (effectTime >= 0)) ? Formatting.GREEN : Formatting.RED);
		String effectString = (effectPower != 0 ? POWER_FORMAT.format(effectPower) + (isPercentage ? "%" : "") + " " : "") + name;
		Text effectText = new LiteralText(effectString).setStyle(effectStyle);
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		int maxEffectWidth = UnofficialMonumentaModClient.options.effect_width - textRenderer.getWidth(timeText);
		if (textRenderer.getWidth(effectText) > maxEffectWidth) {
			int trimmedLength = textRenderer.getTextHandler().getTrimmedLength(effectString, maxEffectWidth - textRenderer.getWidth("..."), effectStyle);
			effectText = new LiteralText(effectString.substring(0, trimmedLength) + "...").setStyle(effectStyle);
		}
		return rightAligned ? new LiteralText("").append(effectText).append(timeText)
			       : new LiteralText("").append(timeText).append(effectText);
	}

	public void tick() {
		effectTime = Math.max(0, effectTime - 50); // never lower below 0; also don't remove until removed by the server
	}

	public static Effect from(PlayerListEntry entry) {
		if (entry.getDisplayName() == null) {
			return null;
		}
		Matcher matcher = EFFECT_PATTERN.matcher(entry.getDisplayName().getString());
		if (!matcher.matches()) {
			return null;
		}
		String effectPowerStr = matcher.group("effectPower");
		final float effectPower = effectPowerStr != null ? Float.parseFloat(effectPowerStr) : 0;

		String timeRemainingStr = matcher.group("timeRemaining");
		int timeRemaining = 0;
		String[] r = timeRemainingStr.split(":");
		int minutes = Integer.parseInt(r[0]);//mm
		int seconds = Integer.parseInt(r[1]);//ss

		timeRemaining += minutes * 60000;
		timeRemaining += seconds * 1000;

		Effect effect = new Effect(matcher.group("effectName"), effectPower, timeRemaining);
		effect.isPercentage = matcher.group("percentage") != null;
		effect.positiveEffect = entry.getDisplayName().getSiblings().stream().noneMatch((sibling) -> (sibling.getStyle() != null && sibling.getStyle().getColor() != null && sibling.getStyle().getColor().equals(TextColor.fromFormatting(Formatting.RED)))) && effect.effectPower >= 0;
		return effect;
	}

	public String getTimeRemainingAsString(float tickDelta) {
		Duration duration = Duration.ofMillis(effectTime + (int) (tickDelta * 50));
		long seconds = duration.getSeconds();
		if (seconds >= 3600) {
			return "**:**";
		}
		long MM = (seconds % 3600) / 60;
		long SS = seconds % 60;

		return String.format("%02d:%02d", MM, SS);
	}
}
