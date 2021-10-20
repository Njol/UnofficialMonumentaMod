package ch.njol.unofficialmonumentamod;

import java.util.ArrayList;
import java.util.List;

public class AbilityHandler {

	public static class AbilityInfo {
		public String name;
		public String className;
		public int initialCooldown;
		public int remainingCooldown;
		public int charges;
		public int maxCharges;

		AbilityInfo(ChannelHandler.ClassUpdatePacket.AbilityInfo info) {
			this.name = info.name;
			this.className = info.className;
			this.initialCooldown = info.initialCooldown;
			this.remainingCooldown = info.remainingCooldown;
			this.charges = info.remainingCharges;
			this.maxCharges = info.maxCharges;
		}
	}

	public final List<AbilityInfo> abilityData = new ArrayList<>();

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
	}

	public void updateAbility(ChannelHandler.AbilityUpdatePacket packet) {
		for (AbilityInfo abilityInfo : this.abilityData) {
			if (abilityInfo.name.equals(packet.name)) {
				abilityInfo.initialCooldown = packet.initialCooldown;
				abilityInfo.remainingCooldown = packet.remainingCooldown;
				abilityInfo.charges = packet.remainingCharges;
				abilityInfo.maxCharges = packet.maxCharges;
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
		// this never lowers cooldowns below 1 - only the message from the server that a cooldown is over can set it to 0
		for (AbilityInfo abilityInfo : this.abilityData) {
			if (abilityInfo.remainingCooldown > 1) {
				abilityInfo.remainingCooldown--;
			}
		}
		if (silenceDuration > 1)
			silenceDuration--;
	}


}
