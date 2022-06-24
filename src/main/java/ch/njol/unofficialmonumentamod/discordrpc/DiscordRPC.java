package ch.njol.unofficialmonumentamod.discordrpc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.mixins.PlayerListHudAccessor;
import club.minnced.discord.rpc.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
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
                    Text header = ((PlayerListHudAccessor) mc.inGameHud.getPlayerListHud()).getHeader();

                    for (Text text: header.getSiblings()) {
                        if (text.getString().matches("<.*>")) {
                            //player shard
                            this.shard = text.getString().substring(1, text.getString().length()-1);
                        }
                    }

                    presence.state = this.shard != null ? "Playing Monumenta - " + this.shard : "Playing Monumenta";
                    //set smol image
                    String shortShard = shard;
                    if (shard.matches(".*-[1-3]")) shortShard = shard.substring(0, shard.length() - 2); //removes the isle number

                    presence.smallImageKey = shortShard; //this is a test

                    //set details

                    String detail = UnofficialMonumentaModClient.options.discordDetails;


                    //replace each call
                    detail = detail.replace("{player}", mc.player.getName().getString());
                    detail = detail.replace("{shard}", this.shard);
                    detail = detail.replace("{server}", mc.getCurrentServerEntry().name);
                    detail = detail.replace("{holding}", !Objects.equals(mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString(), "Air") ? mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString() : "Nothing");


                    presence.details = detail;



                }
            }

            lib.Discord_UpdatePresence(presence);
        } else {
            startPresence();
        }
    }

}
