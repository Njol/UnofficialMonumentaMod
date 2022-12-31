package ch.njol.unofficialmonumentamod.features.discordrpc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.ShardData;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRichPresence;

import java.util.*;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;


public class DiscordRPC {
	club.minnced.discord.rpc.DiscordRPC lib = club.minnced.discord.rpc.DiscordRPC.INSTANCE;
	String applicationId = "989262014562070619";
	String steamId = "";
	DiscordEventHandlers handlers = new DiscordEventHandlers();
	Long start_time = System.currentTimeMillis() / 1000;

	MinecraftClient mc = MinecraftClient.getInstance();

	Integer times = 0;
	Timer t = new Timer();

	public void Init() {
		handlers.ready = (user) -> UnofficialMonumentaModClient.LOGGER.info("Ready! Connected to Discord with " + user.username + "#" + user.discriminator);
		lib.Discord_Initialize(applicationId, handlers, true, steamId);

		startPresence();

		new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				lib.Discord_RunCallbacks();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ignored) {

				}
			}
		}, "RPC-Callback-Handler").start();

		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					updatePresence();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 15000, 15000);
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
		return FabricLoader.getInstance().isDevelopmentEnvironment() ? "Unofficial Monumenta Mod - Development Instance" : "Unofficial Monumenta Mod";
	}

	private void updatePresence() {
		if (mc.world != null) {
			times++;
			final boolean isSinglePlayer = mc.isInSingleplayer();
			final boolean isOnMonumenta = UnofficialMonumentaModClient.isOnMonumenta();

			DiscordRichPresence presence = new DiscordRichPresence();
			presence.startTimestamp = start_time;
			presence.largeImageKey = isOnMonumenta ? "monumenta" : "minecraft512";
			presence.largeImageText = getLargeImageText();
			presence.instance = 1;

			if (isSinglePlayer) {
				presence.state = "Playing Singleplayer";
			} else {
				if (!isOnMonumenta) {
					presence.state = "Playing Multiplayer - " + (mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().name.toUpperCase() : "Unknown");
				} else {
					String shard = Locations.getShortShard();

					presence.state = !Objects.equals(shard, "unknown") ? "Playing Monumenta - " + shard : "Playing Monumenta";

					String coolName = ShardData.getOfficialName(shard);
					if (!shard.equals("unknown")) {
						//set small image
						presence.smallImageKey = shard;
						presence.smallImageText = coolName != null ? coolName : shard;
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
								if (shard.equals("unknown")) {
									continue;
								}
								detail = replacer.replaceIn(detail, coolName != null ? coolName : shard);
							}
							case "holding" -> {
								if (mc.player == null) {
									continue;
								}
								detail = replacer.replaceIn(detail, !Objects.equals(mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString(), "Air") ? mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString() : "Nothing");
							}
							case "class" -> detail = replacer.replaceIn(detail, UnofficialMonumentaModClient.abilityHandler.abilityData.size() > 0 ? UnofficialMonumentaModClient.abilityHandler.abilityData.get(0).className.toLowerCase(Locale.ROOT) : "Timed out");
							case "location" -> {
								if (shard.equals("unknown") || mc.player == null) {
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

	private ArrayList<Match> getDetectedDetails(String detailString) {
		int lastOpenBracketFound = -1;
		StringBuilder currentReplacerString = new StringBuilder();

		ArrayList<Match> matches = new ArrayList<>();

		for (int i = 0; i < detailString.length(); i++) {
			char c = detailString.charAt(i);

			if (c == '{' && lastOpenBracketFound == -1) {
				lastOpenBracketFound = i;
			} else if (c == '}' && lastOpenBracketFound != -1) {
				//add the replacer to the matches
				matches.add(new Match(currentReplacerString.toString()));
				//reset the string builder and open bracket index
				currentReplacerString = new StringBuilder();
				lastOpenBracketFound = -1;
			} else if ((Character.isDigit(c) || Character.isLetter(c)) && lastOpenBracketFound != -1) {
				currentReplacerString.append(c);
			}
		}

		return matches;
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
