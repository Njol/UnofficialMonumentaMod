package ch.njol.unofficialmonumentamod;

import ch.njol.unofficialmonumentamod.hud.strike.ChestCountOverlay;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class ChannelHandler implements ClientPlayNetworking.PlayChannelHandler {

	public static final Identifier CHANNEL_ID = new Identifier("monumenta:client_channel_v1");

	private final Gson gson;
	private final AbilityHandler abilityHandler;
	private final ChestCountOverlay chestCountOverlay;

	public ChannelHandler() {
		gson = new GsonBuilder().create();
		abilityHandler = UnofficialMonumentaModClient.abilityHandler;
		chestCountOverlay = ChestCountOverlay.INSTANCE;
	}

	public static class EffectInfo {
		public String UUID;
		public int displayPriority;

		public String name;
		public Integer duration;
		public double power;

		public boolean positive;
		public boolean percentage;
	}

	public static class MassEffectUpdatePacket {
		String _type = "MassEffectUpdatePacket";

		//when received, will clear stored effects.
		public EffectInfo[] effects;
	}

	public static class EffectUpdatePacket {
		String _type = "EffectUpdatePacket";

		public EffectInfo effect;
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

			@Nullable String mode;
			@Nullable Integer remainingDuration;
			@Nullable Integer initialDuration;

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

		@Nullable String mode;
		@Nullable Integer remainingDuration;
		@Nullable Integer initialDuration;

	}

	/**
	 * Custom player status effects that effect abilities
	 */
	public static class PlayerStatusPacket {

		final String _type = "PlayerStatusPacket";

		int silenceDuration;

	}

	/**
	 * Sent whenever the number of chests in a strike changes
	 */
	public static class StrikeChestUpdatePacket {

		final String _type = "StrikeChestUpdatePacket";

		public int newLimit;

		@Nullable
		public Integer count;

	}

	@Override
	public void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
		String message = buf.readCharSequence(buf.readableBytes(), StandardCharsets.UTF_8).toString();
		JsonElement json = JsonParser.parseString(message);
		if (UnofficialMonumentaModClient.options.logPackets) {
			UnofficialMonumentaModClient.LOGGER.info("[UMM] read packet: " + json);
		}
		client.execute(() -> {
			String packetType = json.getAsJsonObject().getAsJsonPrimitive("_type").getAsString();
			switch (packetType) {
				case "ClassUpdatePacket" -> {
					ClassUpdatePacket packet = gson.fromJson(json, ClassUpdatePacket.class);
					abilityHandler.updateAbilities(packet);
				}
				case "AbilityUpdatePacket" -> {
					AbilityUpdatePacket packet = gson.fromJson(json, AbilityUpdatePacket.class);
					abilityHandler.updateAbility(packet);
				}
				case "PlayerStatusPacket" -> {
					PlayerStatusPacket packet = gson.fromJson(json, PlayerStatusPacket.class);
					abilityHandler.updateStatus(packet);
				}
				case "StrikeChestUpdatePacket" -> {
					StrikeChestUpdatePacket packet = gson.fromJson(json, StrikeChestUpdatePacket.class);
					chestCountOverlay.onStrikeChestUpdatePacket(packet);
				}
				case "MassEffectUpdatePacket" -> {
					MassEffectUpdatePacket packet = gson.fromJson(json, MassEffectUpdatePacket.class);
					UnofficialMonumentaModClient.effectOverlay.onMassEffectUpdatePacket(packet);
				}
				case "EffectUpdatePacket" -> {
					EffectUpdatePacket packet = gson.fromJson(json, EffectUpdatePacket.class);
					UnofficialMonumentaModClient.effectOverlay.onEffectUpdatePacket(packet);
				}
			}
		});
	}

}
