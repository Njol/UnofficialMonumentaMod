package ch.njol.unofficialmonumentamod.discordrpc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import static ch.njol.unofficialmonumentamod.misc.Locations.getShard;

import club.minnced.discord.rpc.*;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Objects;
import java.util.Locale;


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
        handlers.ready = (user) -> UnofficialMonumentaModClient.LOGGER.info("Loaded Discord RPC!");
        lib.Discord_Initialize(applicationId, handlers, true, steamId);

        startPresence();

        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                lib.Discord_RunCallbacks();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
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
            boolean isOnMonumenta = UnofficialMonumentaModClient.isOnMonumenta();

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
                    this.shard = getShard();

                    presence.state = this.shard != null ? "Playing Monumenta - " + this.shard : "Playing Monumenta";
                    //set small image
                    String shortShard = shard;
                    if (shard != null && shard.matches(".*-[1-3]")) shortShard = shard.substring(0, shard.length() - 2); //removes the isles / depths number

                    presence.smallImageKey = shortShard != null ? shortShard : "valley";

                    //set details

                    String detail = UnofficialMonumentaModClient.options.discordDetails;


                    //replace each call

                    if (detail.matches(".*?\\{.*?}.*?") && shortShard != null) {
                        detail = detail.replace("{player}", mc.player.getName().getString() != null ? mc.player.getName().getString() : "player");
                        detail = detail.replace("{shard}", this.shard != null ? this.shard : "Timed out");
                        detail = detail.replace("{holding}", !Objects.equals(mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString(), "Air") ? mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString() : "Nothing");
                        detail = detail.replace("{class}", UnofficialMonumentaModClient.abilityHandler.abilityData.size() > 0 ? UnofficialMonumentaModClient.abilityHandler.abilityData.get(0).className.toLowerCase(Locale.ROOT) : "Timed out");
                        detail = detail.replace("{location}", UnofficialMonumentaModClient.locations.getLocation(mc.player.getX(), mc.player.getZ(), shortShard));
                    } else if (shortShard == null) {
                        detail = "User timed out :) or I couldn't detect the user's shard";
                    }
                    //only runs those if there's at least one detected
                    presence.details = detail;



                }
            }

            lib.Discord_UpdatePresence(presence);
        } else {
            startPresence();
        }
    }

}
