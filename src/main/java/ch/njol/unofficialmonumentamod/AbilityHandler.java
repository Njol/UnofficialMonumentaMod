package ch.njol.unofficialmonumentamod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AbilityHandler {

	public static final int MAX_ANIMATION_TICKS = 20;

	public static class AbilityInfo {
		public String name;
		public String className;
		public int initialCooldown;
		public int remainingCooldown;
		public int offCooldownAnimationTicks;
		public int charges;
		public int maxCharges;

		public AbilityInfo(ChannelHandler.ClassUpdatePacket.AbilityInfo info) {
			this.name = info.name;
			this.className = info.className;
			this.initialCooldown = info.initialCooldown;
			this.remainingCooldown = info.remainingCooldown;
			this.charges = info.remainingCharges;
			this.maxCharges = info.maxCharges;
			offCooldownAnimationTicks = MAX_ANIMATION_TICKS;
		}

		public String getOrderId() {
			return (className + "/" + name).toLowerCase(Locale.ROOT);
		}
	}

	// accesses must be synchronized on the AbilityHandler
	public final List<AbilityInfo> abilityData = new ArrayList<>();

	// accesses should be synchronized on the AbilityHandler (if reading both fields)
	public int initialSilenceDuration = 0;
	public int silenceDuration = 0;

	public void updateAbilities(ChannelHandler.ClassUpdatePacket packet) {
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

	public void sortAbilities() {
		List<String> order = UnofficialMonumentaModClient.options.abilitiesDisplay_order;
		abilityData.sort(Comparator.comparingInt(info -> order.indexOf(info.getOrderId())));
	}

	public void updateAbility(ChannelHandler.AbilityUpdatePacket packet) {
		for (AbilityInfo abilityInfo : this.abilityData) {
			if (abilityInfo.name.equals(packet.name)) {
				int previousCooldown = abilityInfo.remainingCooldown;
				abilityInfo.remainingCooldown = packet.remainingCooldown;
				if (previousCooldown > 0 && packet.remainingCooldown == 0) {
					abilityInfo.offCooldownAnimationTicks = 0;
				}
				abilityInfo.charges = packet.remainingCharges;
				return;
			}
		}
	}

	public void updateStatus(ChannelHandler.PlayerStatusPacket packet) {
		silenceDuration = packet.silenceDuration;
		initialSilenceDuration = packet.silenceDuration;
	}

	public void onDisconnect() {
		abilityData.clear();
		initialSilenceDuration = 0;
		silenceDuration = 0;
	}

	public void tick() {
		// this never lowers cooldowns/silence below 1 - only the message from the server that a cooldown is over can set it to 0
		for (AbilityInfo abilityInfo : this.abilityData) {
			if (abilityInfo.remainingCooldown > 1) {
				abilityInfo.remainingCooldown--;
			}
			if (abilityInfo.offCooldownAnimationTicks < MAX_ANIMATION_TICKS) {
				abilityInfo.offCooldownAnimationTicks++;
			}
		}
        if (silenceDuration > 1) {
            silenceDuration--;
        }
	}


}
