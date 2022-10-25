package ch.njol.unofficialmonumentamod.features.discordrpc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
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
    String shard = null;

    MinecraftClient mc = MinecraftClient.getInstance();

    Integer times = 0;
    Timer t = new Timer();

    public void Init() {
        handlers.ready = (user) -> System.out.println("Ready!");
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
            boolean isSinglePlayer = mc.isInSingleplayer();
            boolean isOnMonumenta = !isSinglePlayer && Objects.requireNonNull(mc.getCurrentServerEntry()).address.toLowerCase().matches("(?i)server.playmonumenta.com|monumenta-11.playmonumenta.com|monumenta-8.playmonumenta.com");

            DiscordRichPresence presence = new DiscordRichPresence();
            presence.startTimestamp = start_time;
            presence.largeImageKey = isOnMonumenta ? "monumenta" : "minecraft512";
            presence.largeImageText = "Njol's Unofficial Monumenta Mod";
            presence.instance = 1;

            if (isSinglePlayer) {
                presence.state = "Playing Singleplayer";
            } else {
                if (!isOnMonumenta) {
                    presence.state = "Playing Multiplayer - " + mc.getCurrentServerEntry().name.toUpperCase();
                } else {
                    String shard = Locations.getShortShard();

                    presence.state = !Objects.equals(this.shard, "unknown") ? "Playing Monumenta - " + this.shard : "Playing Monumenta";
                    //set smol image

                    presence.smallImageKey = !shard.equals("unknown") ? shard : "valley";

                    //set details
                    String detail = UnofficialMonumentaModClient.options.discordDetails;
                    if (detail.matches(".*?\\{.*?}.*?") && !shard.equals("unknown") && mc.player != null) {
                        detail = detail.replace("{player}",mc.player.getName().getString());
                        detail = detail.replace("{shard}", shard);
                        detail = detail.replace("{holding}", !Objects.equals(mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString(), "Air") ? mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString() : "Nothing");
                        detail = detail.replace("{class}", UnofficialMonumentaModClient.abilityHandler.abilityData.size() > 0 ? UnofficialMonumentaModClient.abilityHandler.abilityData.get(0).className.toLowerCase(Locale.ROOT) : "Timed out");
                        detail = detail.replace("{location}", UnofficialMonumentaModClient.locations.getLocation(mc.player.getX(), mc.player.getZ(), shard));
                    } else if (shard.equals("unknown")) {
                        detail = "User timed out or the shard couldn't be detected.";
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
