package ch.njol.unofficialmonumentamod.features.discordrpc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.shard.ShardData;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;

import java.util.*;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;


public class DiscordPresence {
	private DiscordRPC lib = null;
	String applicationId = "989262014562070619";
	String steamId = "";
	private DiscordEventHandlers handlers = null;
	Long start_time = System.currentTimeMillis() / 1000;

	MinecraftClient mc = MinecraftClient.getInstance();

	Integer times = 0;
	Timer updateTimer = new Timer();
	Thread callbackThread;

	public static DiscordPresence INSTANCE = new DiscordPresence();

	private boolean initialized = false;
	public boolean isInitialized() {
		return initialized;
	}

	public void Init() {
		if (initialized) {
			return;
		}

		lib = DiscordRPC.INSTANCE;
		handlers = new DiscordEventHandlers();

		handlers.ready = (user) -> UnofficialMonumentaModClient.LOGGER.info("Ready! Connected to Discord with " + user.username + "#" + user.discriminator);
		lib.Discord_Initialize(applicationId, handlers, true, steamId);

		startPresence();

		callbackThread = new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				lib.Discord_RunCallbacks();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ignored) {}
			}
		}, "RPC-Callback-Handler");

		callbackThread.start();

		updateTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					updatePresence();
				} catch (Exception e) {
					UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to update Discord Rich Presence", e);
				}
			}
		}, 15000, 15000);

		initialized = true;
	}

	public void shutdown() {
		lib.Discord_Shutdown();
		callbackThread.interrupt();
		//kill old timer and setup new one
		updateTimer.cancel();
		updateTimer = new Timer();
		//enable the ability to re-load discord.
		initialized = false;
	}

	private void startPresence() {

		DiscordRichPresence presence = new DiscordRichPresence();

		presence.startTimestamp = start_time;
		presence.details = "In the Main menu";
		presence.largeImageKey = "minecraft512";
		presence.largeImageText = getLargeImageText();
		presence.instance = 1;
		lib.Discord_UpdatePresence(presence);
	}

	private String getLargeImageText() {
		return FabricLoader.getInstance().isDevelopmentEnvironment() ? "Unofficial Monumenta Mod - Dev" : "Unofficial Monumenta Mod";
	}

	private void updatePresence() {
		if (mc.world != null) {
			times++;
			final boolean isSinglePlayer = mc.isInSingleplayer();
			final boolean userOnMonumenta = UnofficialMonumentaModClient.isOnMonumenta();

			DiscordRichPresence presence = new DiscordRichPresence();
			presence.startTimestamp = start_time;
			presence.largeImageKey = userOnMonumenta ? "monumenta" : "minecraft512";
			presence.largeImageText = getLargeImageText();
			presence.instance = 1;

			if (isSinglePlayer) {
				presence.state = "Playing Singleplayer";
			} else {
				if (!userOnMonumenta) {
					presence.state = "Playing Multiplayer - " + (mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().name.toUpperCase() : "Unknown");
				} else {
					String shard = getActiveShard();

					presence.state = !Objects.equals(shard, ShardData.UNKNOWN_SHARD) ? "Playing Monumenta - " + shard : "Playing Monumenta";

					String shardName = getShardOfficialName(shard);
					if (!Objects.equals(shard, ShardData.UNKNOWN_SHARD)) {
						//set small image
						presence.smallImageKey = shard;
						presence.smallImageText = shardName;
					}

					//set details
					String detail = UnofficialMonumentaModClient.options.discordDetails;
					ArrayList<Match> replacers = getDetectedDetails(detail);

					for (Match replacer: replacers) {
						switch (replacer.match) {
							case "player" -> {
								if (mc.player == null) {
									continue;
								}
								detail = replacer.replaceIn(detail, mc.player.getName().getString());
							}
							case "shard" -> {
								if (Objects.equals(shard, ShardData.UNKNOWN_SHARD)) {
									continue;
								}
								detail = replacer.replaceIn(detail, shardName);
							}
							case "holding" -> {
								if (mc.player == null) {
									continue;
								}
								detail = replacer.replaceIn(detail, !Objects.equals(mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString(), "Air") ? mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString() : "Nothing");
							}
							case "class" -> detail = replacer.replaceIn(detail, !UnofficialMonumentaModClient.abilityHandler.abilityData.isEmpty() ? UnofficialMonumentaModClient.abilityHandler.abilityData.get(0).className.toLowerCase(Locale.ROOT) : "Timed out");
							case "location" -> {
								if ((Objects.equals(shard, ShardData.UNKNOWN_SHARD)) || mc.player == null) {
									continue;
								}
								detail = replacer.replaceIn(detail, UnofficialMonumentaModClient.locations.getLocation(mc.player.getX(), mc.player.getZ(), shard));
							}
						}
					}

					presence.details = detail;
				}
			}

			lib.Discord_UpdatePresence(presence);
		} else {
			startPresence();
		}
	}

	private String getActiveShard() {
		if (UnofficialMonumentaModClient.options.hideShardMode) {
			return ShardData.UNKNOWN_SHARD;
		}
		String shard = Locations.getShortShard();

		if (shard == null) {
			return ShardData.UNKNOWN_SHARD;
		}

		return shard;
	}

	private String getShardOfficialName(String shard) {
		if (shard == null || shard.equals(ShardData.UNKNOWN_SHARD)) {
			return ShardData.UNKNOWN_SHARD;
		}
		String shardOfficialName = ShardData.getOfficialName(shard);
		if (shardOfficialName == null) {
			return ShardData.UNKNOWN_SHARD;
		}
		return shardOfficialName;
	}

	public void updateDiscordRPCDetails() {
		shouldUpdate = true;
		getDetectedDetails(UnofficialMonumentaModClient.options.discordDetails);
	}

	private final ArrayList<Match> cachedReplacers = new ArrayList<>();
	private boolean shouldUpdate = true;

	private ArrayList<Match> getDetectedDetails(String detailString) {
		if (!cachedReplacers.isEmpty() && shouldUpdate) {
			return cachedReplacers;
		}

		cachedReplacers.clear();
		int lastOpenBracketFound = -1;
		StringBuilder currentReplacerString = new StringBuilder();

		for (int i = 0; i < detailString.length(); i++) {
			char c = detailString.charAt(i);

			if (c == '{' && lastOpenBracketFound == -1) {
				lastOpenBracketFound = i;
			} else if (c == '}' && lastOpenBracketFound != -1) {
				//add the replacer to the matches
				cachedReplacers.add(new Match(currentReplacerString.toString()));
				//reset the string builder and open bracket index
				currentReplacerString = new StringBuilder();
				lastOpenBracketFound = -1;
			} else if ((Character.isDigit(c) || Character.isLetter(c)) && lastOpenBracketFound != -1) {
				currentReplacerString.append(c);
			}
		}

		shouldUpdate = false;
		return cachedReplacers;
	}

	private static class Match {
		String match;

		Match(String match) {
			this.match = match;
		}

		String replaceIn(String string, String replaceValue) {
			return string.replace("{" + match + "}", replaceValue);
		}
	}

}
