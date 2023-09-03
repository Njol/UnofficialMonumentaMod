package ch.njol.unofficialmonumentamod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

public class AbilityHandler {

	private static final Identifier COOLDOWN_SOUND = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "cooldown_ping");
	private static final Identifier COOLDOWN_SOUND_ALT = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "cooldown_ping_alt");

	public static final int MAX_ANIMATION_TICKS = 20;

	private static final Random random = Random.create();

	public static class AbilityInfo {
		public String name;
		public String className;
		public int initialCooldown;
		public int remainingCooldown;
		public int offCooldownAnimationTicks;
		public int charges;
		public int maxCharges;
		public int initialDuration;
		public int remainingDuration;
		public @Nullable String mode;

		public AbilityInfo(ChannelHandler.ClassUpdatePacket.AbilityInfo info) {
			this.name = info.name;
			this.className = info.className;
			this.initialCooldown = info.initialCooldown;
			this.remainingCooldown = info.remainingCooldown;
			this.charges = info.remainingCharges;
			this.maxCharges = info.maxCharges;
			this.initialDuration = info.initialDuration == null ? 0 : info.initialDuration;
			this.remainingDuration = info.remainingDuration == null ? 0 : info.remainingDuration;
			this.mode = info.mode;
			offCooldownAnimationTicks = MAX_ANIMATION_TICKS;
		}

		public String getOrderId() {
			return (className + "/" + name).toLowerCase(Locale.ROOT);
		}

		public void tick() {
			if (remainingCooldown > 1) {
				remainingCooldown--;
			}
			if (remainingDuration > 1) {
				remainingDuration--;
			}
		}

	}

	// accesses must be synchronized on the AbilityHandler
	public final List<AbilityInfo> abilityData = new ArrayList<>();

	// accesses should be synchronized on the AbilityHandler (if reading both fields)
	public volatile int initialSilenceDuration = 0;
	public volatile int silenceDuration = 0;

	public synchronized void updateAbilities(ChannelHandler.ClassUpdatePacket packet) {
		abilityData.clear();
		// class update also clears silence
		initialSilenceDuration = 0;
		silenceDuration = 0;
		for (ChannelHandler.ClassUpdatePacket.AbilityInfo abilityInfo : packet.abilities) {
			abilityData.add(new AbilityInfo(abilityInfo));
		}
		List<String> order = new ArrayList<>(UnofficialMonumentaModClient.options.abilitiesDisplay_order);
		boolean added = false;
		for (AbilityInfo info : abilityData) {
			String id = info.getOrderId();
			if (!order.contains(id)) {
				order.add(id);
				added = true;
			}
		}
		if (added) {
			UnofficialMonumentaModClient.options.abilitiesDisplay_order = order;
			UnofficialMonumentaModClient.saveConfig();
		}
		sortAbilities();
	}

	public synchronized void sortAbilities() {
		List<String> order = UnofficialMonumentaModClient.options.abilitiesDisplay_order;
		abilityData.sort(Comparator.comparingInt(info -> order.indexOf(info.getOrderId())));
	}

	public synchronized void updateAbility(ChannelHandler.AbilityUpdatePacket packet) {
		for (AbilityInfo abilityInfo : this.abilityData) {
			if (abilityInfo.name.equals(packet.name)) {
				int previousCooldown = abilityInfo.remainingCooldown;
				abilityInfo.remainingCooldown = packet.remainingCooldown;
				if (previousCooldown > 0 && packet.remainingCooldown == 0) {
					abilityInfo.offCooldownAnimationTicks = 0;
				}
				abilityInfo.charges = packet.remainingCharges;
				abilityInfo.mode = packet.mode;
				abilityInfo.initialDuration = packet.initialDuration == null ? 0 : packet.initialDuration;
				abilityInfo.remainingDuration = packet.remainingDuration == null ? 0 : packet.remainingDuration;
				return;
			}
		}
	}

	public synchronized void updateStatus(ChannelHandler.PlayerStatusPacket packet) {
		silenceDuration = packet.silenceDuration;
		initialSilenceDuration = packet.silenceDuration;
	}

	public synchronized void onDisconnect() {
		abilityData.clear();
		initialSilenceDuration = 0;
		silenceDuration = 0;
	}

	public synchronized void tick() {
		// this never lowers cooldowns/silence below 1 - only the message from the server that a cooldown is over can set it to 0
		List<AbilityInfo> data = this.abilityData;
		for (int i = 0; i < data.size(); i++) {
			AbilityInfo abilityInfo = data.get(i);
			abilityInfo.tick();
			if (abilityInfo.offCooldownAnimationTicks == 0 && UnofficialMonumentaModClient.options.abilitiesDisplay_offCooldownSoundVolume > 0) {
				float pitchMin = UnofficialMonumentaModClient.options.abilitiesDisplay_offCooldownSoundPitchMin;
				float pitchMax = UnofficialMonumentaModClient.options.abilitiesDisplay_offCooldownSoundPitchMax;
				MinecraftClient.getInstance().getSoundManager().play(
					new PositionedSoundInstance(UnofficialMonumentaModClient.options.abilitiesDisplay_offCooldownSoundUseAlt ? COOLDOWN_SOUND_ALT : COOLDOWN_SOUND,
						SoundCategory.MASTER, UnofficialMonumentaModClient.options.abilitiesDisplay_offCooldownSoundVolume,
						pitchMin + (i == 0 ? 0 : (pitchMax - pitchMin) * i / (data.size() - 1)),
						random, false, 0, SoundInstance.AttenuationType.NONE, 0, 0, 0, true), 0);
			}
			if (abilityInfo.offCooldownAnimationTicks < MAX_ANIMATION_TICKS) {
				abilityInfo.offCooldownAnimationTicks++;
			}
		}
		if (silenceDuration > 1) {
			silenceDuration--;
		}
	}

	public enum DurationRenderMode {
		CIRCLE(),
		BAR()
	}
}
