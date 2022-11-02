package ch.njol.unofficialmonumentamod.features.discordrpc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.Constants;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import club.minnced.discord.rpc.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

import java.util.*;

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
        presence.largeImageText = "Njol's Unofficial Monumenta Mod";
        presence.instance = 1;
        lib.Discord_UpdatePresence(presence);
    }

    private void updatePresence() {
        if (mc.world != null) {
            times++;
            final boolean isSinglePlayer = mc.isInSingleplayer();
            final boolean isOnMonumenta = UnofficialMonumentaModClient.isOnMonumenta();

            DiscordRichPresence presence = new DiscordRichPresence();
            presence.startTimestamp = start_time;
            presence.largeImageKey = isOnMonumenta ? "monumenta" : "minecraft512";
            presence.largeImageText = "Njol's Unofficial Monumenta Mod";
            presence.instance = 1;

            if (isSinglePlayer) {
                presence.state = "Playing Singleplayer";
            } else {
                if (!isOnMonumenta) {
                    presence.state = "Playing Multiplayer - " + (mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().name.toUpperCase() : "Unknown");
                } else {
                    String shard = Locations.getShortShard();

                    presence.state = !Objects.equals(shard, "unknown") ? "Playing Monumenta - " + shard : "Playing Monumenta";

                    String coolName = Constants.getOfficialName(shard);
                    if (!shard.equals("unknown")) {
                        //set small image
                        presence.smallImageKey = shard;
                        presence.smallImageText = coolName != null ? coolName : shard;
                    }

                    //set details
                    String detail = UnofficialMonumentaModClient.options.discordDetails;
                    if (detail.matches(".*?\\{.*?}.*?") && !shard.equals("unknown") && mc.player != null) {
                        detail = detail.replace("{player}", mc.player.getName().getString());
                        detail = detail.replace("{shard}", coolName != null ? coolName : shard);
                        detail = detail.replace("{holding}", !Objects.equals(mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString(), "Air") ? mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString() : "Nothing");
                        detail = detail.replace("{class}", UnofficialMonumentaModClient.abilityHandler.abilityData.size() > 0 ? UnofficialMonumentaModClient.abilityHandler.abilityData.get(0).className.toLowerCase(Locale.ROOT) : "Timed out");
                        detail = detail.replace("{location}", UnofficialMonumentaModClient.locations.getLocation(mc.player.getX(), mc.player.getZ(), shard));
                    } else if (shard.equals("unknown")) {
                        detail = "User timed out or their shard couldn't be detected.";
                    }
                    presence.details = detail;
                }
            }

            lib.Discord_UpdatePresence(presence);
        } else {
            startPresence();
        }
    }

}
