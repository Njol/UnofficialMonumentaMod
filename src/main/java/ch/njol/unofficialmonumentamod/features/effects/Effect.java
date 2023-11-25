package ch.njol.unofficialmonumentamod.features.effects;

import ch.njol.unofficialmonumentamod.ChannelHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class Effect {

	private static final DecimalFormat POWER_FORMAT = new DecimalFormat("+0.##;-#");
	private static final Pattern EFFECT_PATTERN = Pattern.compile("(?:(?<effectPower>[+-]?\\d+(?:\\.\\d+)?)(?<percentage>%)? )?(?<effectName>.*) (?<timeRemaining>\\d*:\\d*)");


	@Expose
	public final UUID uuid;

	@Expose
	public String name;
	@Expose
	public int effectTime;
	@Expose
	public float effectPower;
	@Expose
	public boolean isPercentage = false;
	@Expose
	public boolean positiveEffect = true;
	@Expose
	public int displayPriority = 0;
	@Expose
	public boolean isNonStackableEffect;

	public Effect(String name, float effectPower, int effectTime) {
		isNonStackableEffect = effectPower == 0;
		this.name = name;
		this.effectPower = effectPower;
		this.effectTime = effectTime;
		this.uuid = UUID.randomUUID();
	}

	public Effect(String name, float effectPower, int effectTime, boolean isPercentage) {
		isNonStackableEffect = effectPower == 0;
		this.name = name;
		this.effectPower = effectPower;
		this.effectTime = effectTime;
		this.isPercentage = isPercentage;
		this.uuid = UUID.randomUUID();
	}

	public Effect(String name, float effectPower, int effectTime, UUID uuid) {
		isNonStackableEffect = effectPower == 0;
		this.name = name;
		this.effectPower = effectPower;
		this.effectTime = effectTime;
		this.uuid = uuid;
	}

	public Effect(String name, float effectPower, int effectTime, boolean isPercentage, UUID uuid) {
		isNonStackableEffect = effectPower == 0;
		this.name = name;
		this.effectPower = effectPower;
		this.effectTime = effectTime;
		this.isPercentage = isPercentage;
		this.uuid = uuid;
	}

	public boolean isPositive() {
		return positiveEffect;
	}

	public static Effect from(ChannelHandler.EffectInfo effectInfo) {
		int tickDuration = effectInfo.duration;
		int millisDuration = tickDuration * 50;

		Effect effect = new Effect(effectInfo.name, (float) effectInfo.power, tickDuration == -1 ? -1 : millisDuration, UUID.fromString(effectInfo.UUID));
		effect.isPercentage = effectInfo.percentage;
		effect.positiveEffect = effectInfo.positive;
		effect.displayPriority = effectInfo.displayPriority;

		return effect;
	}

	public void updateFrom(ChannelHandler.EffectUpdatePacket packet) {
		ChannelHandler.EffectInfo info = packet.effect;
		if (info.duration == 0) {
			return;
		}

		int tickDuration = info.duration;
		int millisDuration = tickDuration * 50;

		effectPower = (float) info.power;
		name = info.name;
		effectTime = tickDuration != -1 ? millisDuration : -1;
		isPercentage = info.percentage;
		positiveEffect = info.positive;
		displayPriority = info.displayPriority;

		isNonStackableEffect = effectPower == 0;
	}

	public boolean isInfiniteDuration() {
		return effectTime == -1;
	}

	@Override
	protected Effect clone() {
		//DON'T FORGET TO MAKE SURE THIS SETS ALL THE PARAMETERS CORRECTLY
		Effect effect = new Effect(name, effectPower, effectTime, isPercentage);

		effect.positiveEffect = positiveEffect;
		return effect;
	}

	public Text toText(float tickDelta, boolean rightAligned) {
		Text timeText = MutableText.of(new LiteralTextContent((rightAligned ? " " : "") + getTimeRemainingAsString(tickDelta) + (rightAligned ? "" : " ")));
		Style effectStyle = Style.EMPTY.withColor((isPositive() == (effectTime >= 0)) ? Formatting.GREEN : Formatting.RED);
		String effectString = (effectPower != 0 ? POWER_FORMAT.format(effectPower) + (isPercentage ? "%" : "") + " " : "") + name;
		Text effectText = MutableText.of(new LiteralTextContent(effectString)).setStyle(effectStyle);
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		int maxEffectWidth = UnofficialMonumentaModClient.options.effect_width - textRenderer.getWidth(timeText);
		if (textRenderer.getWidth(effectText) > maxEffectWidth) {
			int trimmedLength = textRenderer.getTextHandler().getTrimmedLength(effectString, maxEffectWidth - textRenderer.getWidth("..."), effectStyle);
			effectText = MutableText.of(new LiteralTextContent(effectString.substring(0, trimmedLength) + "...")).setStyle(effectStyle);
		}
		return rightAligned ? MutableText.of(new LiteralTextContent("")).append(effectText).append(timeText)
			       : MutableText.of(new LiteralTextContent("")).append(timeText).append(effectText);
	}

	public void tick() {
		if (isInfiniteDuration()) return;
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
		if (timeRemainingStr != null) {
			String[] r = timeRemainingStr.split(":");
			int minutes = Integer.parseInt(r[0]);//mm
			int seconds = Integer.parseInt(r[1]);//ss

			timeRemaining += minutes * 60000;
			timeRemaining += seconds * 1000;
		} else {
			//set as infinite duration.
			timeRemaining = -1;
		}

		Effect effect = new Effect(matcher.group("effectName"), effectPower, timeRemaining);
		effect.isPercentage = matcher.group("percentage") != null;
		effect.positiveEffect = entry.getDisplayName().getSiblings().stream().noneMatch(
				(sibling) -> (sibling.getStyle() != null &&
						sibling.getStyle().getColor() != null &&
						sibling.getStyle().getColor().equals(TextColor.fromFormatting(Formatting.RED)))) &&
				effect.effectPower >= 0;
		return effect;
	}

	public String getTimeRemainingAsString(float tickDelta) {
		if (isInfiniteDuration()) {
			return "";
		}
		Duration duration = Duration.ofMillis(effectTime + (int) (tickDelta * 50));
		long seconds = duration.getSeconds();
		if (seconds >= 3600) {
			return "**:**";
		}
		long MM = (seconds % 3600) / 60;
		long SS = seconds % 60;

		return String.format("%02d:%02d", MM, SS);
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

	@Override
	public String toString() {
		return GSON.toJson(this);
	}
}
