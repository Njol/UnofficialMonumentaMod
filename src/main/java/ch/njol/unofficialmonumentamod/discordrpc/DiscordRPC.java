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
    private final club.minnced.discord.rpc.DiscordRPC lib = club.minnced.discord.rpc.DiscordRPC.INSTANCE;
    private final DiscordEventHandlers handlers = new DiscordEventHandlers();
    private final Long start_time = System.currentTimeMillis() / 1000;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final Timer t = new Timer();

    public void init() {
        handlers.ready = (user) -> UnofficialMonumentaModClient.LOGGER.info("Loaded Discord RPC!");
        String applicationId = "989262014562070619";
        String steamId = "";
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
        presence.largeImageText = "Unofficial Monumenta Mod";
        presence.instance = 1;
        lib.Discord_UpdatePresence(presence);
    }

    private void updatePresence() {
        if (mc.world != null) {
            boolean isSinglePlayer = mc.isInSingleplayer();
            boolean isOnMonumenta = UnofficialMonumentaModClient.isOnMonumenta();

            DiscordRichPresence presence = new DiscordRichPresence();
            presence.startTimestamp = start_time;
            presence.largeImageKey = isOnMonumenta ? "monumenta" : "minecraft512";
            presence.largeImageText = "Unofficial Monumenta Mod";
            presence.instance = 1;

            if (isSinglePlayer) {
                presence.state = "Playing Singleplayer";
            } else {
                if (!isOnMonumenta) {
                    if (mc.getCurrentServerEntry() != null) {
                        presence.state = "Playing Multiplayer - " + mc.getCurrentServerEntry().name.toUpperCase();
                    } else presence.state = "Playing Multiplayer - Unknown server";
                } else {
                    String shard = getShard();

                    presence.state = shard != null ? "Playing Monumenta - " + shard : "Playing Monumenta";
                    //set small image
                    String shortShard = shard;
                    if (shard != null && shard.matches(".*-[1-3]")) shortShard = shard.substring(0, shard.length() - 2); //removes the isles / depths number

                    presence.smallImageKey = shortShard != null ? shortShard : "valley";

                    //set details
                    String detail = UnofficialMonumentaModClient.options.discordDetails;
                    if (detail.matches(".*?\\{.*?}.*?") && shortShard != null && mc.player != null) {
                        detail = detail.replace("{player}",mc.player.getName().getString());
                        detail = detail.replace("{shard}", shard);
                        detail = detail.replace("{holding}", !Objects.equals(mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString(), "Air") ? mc.player.getStackInHand(Hand.MAIN_HAND).getName().getString() : "Nothing");
                        detail = detail.replace("{class}", UnofficialMonumentaModClient.abilityHandler.abilityData.size() > 0 ? UnofficialMonumentaModClient.abilityHandler.abilityData.get(0).className.toLowerCase(Locale.ROOT) : "Timed out");
                        detail = detail.replace("{location}", UnofficialMonumentaModClient.locations.getLocation(mc.player.getX(), mc.player.getZ(), shortShard));
                    } else if (shortShard == null) {
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
