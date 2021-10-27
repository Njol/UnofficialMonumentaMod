package ch.njol.unofficialmonumentamod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;

public class ChannelHandler implements ClientPlayNetworking.PlayChannelHandler {

	public static final Identifier CHANNEL_ID = new Identifier("monumenta:client_channel_v1");

	private final Gson gson;
	private final AbilityHandler abilityHandler;

	public ChannelHandler() {
		gson = new GsonBuilder().create();
		abilityHandler = UnofficialMonumentaModClient.abilityHandler;
	}

	/**
	 * Sent whenever a player's class is updated.
	 */
	public static class ClassUpdatePacket {

		String _type = "ClassUpdatePacket";

		AbilityInfo[] abilities;

		public static class AbilityInfo {

			public String name;
			public String className;

			int remainingCooldown;
			int initialCooldown;

			int remainingCharges;
			int maxCharges;

		}

	}

	/**
	 * Sent whenever an ability is used or changed in any way
	 */
	public static class AbilityUpdatePacket {

		String _type = "AbilityUpdatePacket";

		public String name;

		int remainingCooldown;

		int remainingCharges;

	}

	/**
	 * Custom player status effects that effect abilities
	 */
	public static class PlayerStatusPacket {

		final String _type = "PlayerStatusPacket";

		int silenceDuration;

	}

	@Override
	public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
		String message = buf.readCharSequence(buf.readableBytes(), StandardCharsets.UTF_8).toString();
		JsonElement json = new JsonParser().parse(message);
		if (UnofficialMonumentaModClient.options.logPackets)
			System.out.println("[UMM] read packet: " + json);
		String packetType = json.getAsJsonObject().getAsJsonPrimitive("_type").getAsString();
		switch (packetType) {
			case "ClassUpdatePacket": {
				ClassUpdatePacket packet = gson.fromJson(json, ClassUpdatePacket.class);
				abilityHandler.updateAbilities(packet);
				break;
			}
			case "AbilityUpdatePacket": {
				AbilityUpdatePacket packet = gson.fromJson(json, AbilityUpdatePacket.class);
				abilityHandler.updateAbility(packet);
				break;
			}
			case "PlayerStatusPacket": {
				PlayerStatusPacket packet = gson.fromJson(json, PlayerStatusPacket.class);
				abilityHandler.updateStatus(packet);
				break;
			}
		}
	}

}
