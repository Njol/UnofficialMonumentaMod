package ch.njol.unofficialmonumentamod.core.shard;

import ch.njol.unofficialmonumentamod.ChannelHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.shard.ShardData.ShardChangedEventCallback;
import ch.njol.unofficialmonumentamod.core.shard.ShardData.TabShard;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import static ch.njol.unofficialmonumentamod.core.shard.ShardData.ShardChangedEventCallback;
import static ch.njol.unofficialmonumentamod.core.shard.ShardData.TabShard;
import static ch.njol.unofficialmonumentamod.core.shard.ShardData.getShard;

public class ShardLoader {
    public static final String UNKNOWN_SHARD = "unknown";

    private static TabShard currentShard = TabShard.UNKNOWN;
    private static TabShard lastLoadedShard = TabShard.UNKNOWN;

    private static boolean worldSpoofEnabled = false;
    private static boolean receivingLocationPackets = false;

    public static TabShard getCurrentShard() {
        return currentShard;
    }

    public static TabShard getLastShard() {
        return lastLoadedShard;
    }

    public static boolean isWorldSpoofingEnabled() {
        return worldSpoofEnabled;
    }

    public static boolean receivesLocationPackets() {
        return receivingLocationPackets;
    }

    private static final String WORLD_SPOOF_MESSAGE_START = "World name spoofing is now ";

    public static void onMessageReceived(Text message, boolean overlay) {
        String text = message.getString();
        if (!text.startsWith(WORLD_SPOOF_MESSAGE_START)) {
            return;
        }

        String character = text.substring(WORLD_SPOOF_MESSAGE_START.length());
        if (character.charAt(0) == 'd') { //disabled
            //disabled
            worldSpoofEnabled = false;
        } else if (character.charAt(0) == 'e') { //enabled
            //enabled
            worldSpoofEnabled = true;
        } else {
            //Uh oh, this is either not up-to-date or the message was edited by another mod.
            UnofficialMonumentaModClient.LOGGER.warn("\"Received \"{character}\" but expected \"e\" or \"d\"\", could not set world spoofing status.");
        }
    }

    public static void onManualShardSet(String shardName) {
        debug("Manually set shard to: " + shardName);
        TabShard shard = shardName.equalsIgnoreCase(UNKNOWN_SHARD) ? TabShard.UNKNOWN : new TabShard(shardName);
        checkAndDispatchEvent(shard);
    }

    public static void onDisconnect() {
        //world spoofing could be enabled or disabled between this and next login for all we know, so we set it as uncertain
        debug("Disconnecting, un-setting non-persistent values");
        worldSpoofEnabled = false;
        receivingLocationPackets = false;
    }

    public static void onLocationUpdatedPacket(ChannelHandler.LocationUpdatedPacket packet) {
        receivingLocationPackets = true;
        String content = packet.getContent();
        String shard = packet.getShard();

        TabShard sherd = TabShard.UNKNOWN;
        if (!content.equalsIgnoreCase(shard)) {
            //get from content
            if (getShard(content) != null) {
                sherd = new TabShard(content);
            }
        }

        if (sherd == TabShard.UNKNOWN) {
            sherd = new TabShard(shard);
        }

        checkAndDispatchEvent(sherd);
    }

    public static void onWorldLoaded() {
        if (MinecraftClient.getInstance().world != null) {
            loadWorldFromDimensionKey(MinecraftClient.getInstance().world.getRegistryKey());
            debug("Inferring shard name from dimension key");
        }
    }

    public static void loadShardFromTabHeader(String shardString) {
        TabShard shard = shardString.equalsIgnoreCase(UNKNOWN_SHARD) ? TabShard.UNKNOWN : new TabShard(shardString);

        if (worldSpoofEnabled || receivingLocationPackets) {
            debug("Skipping tab header " + shardString + ", receiving location from packets (world spoof or locationUpdatedPacket).");
            return;
        }
        debug("Received tab header update: " + shardString);
        checkAndDispatchEvent(shard);
    }

    public static void loadWorldFromDimensionKey(RegistryKey<World> dimKey) {
        String extractedShard = extractWorldFromDimKey(dimKey);
        debug("Received new world key: " + extractedShard);

        if (receivingLocationPackets) {
            debug("Skipping world spoof key " + extractedShard + ", receiving location from location updated packet.");
            return;
        }

        if (extractedShard.equalsIgnoreCase(UNKNOWN_SHARD)) {
            return;
        }
        worldSpoofEnabled = true;

        TabShard shard = new TabShard(extractedShard);
        checkAndDispatchEvent(shard);
    }

    public static String extractWorldFromDimKey(RegistryKey<World> dimensionKey) {
        Identifier worldName = dimensionKey.getValue();
        if (ShardData.isExistingShard(worldName.getPath())) {
            return worldName.getPath();
        }

        return UNKNOWN_SHARD;
    }

    private static void checkAndDispatchEvent(TabShard shard) {
        if (!Objects.equals(shard, currentShard) && !Objects.equals(shard, TabShard.UNKNOWN)) {
            debug("Shard changed, dispatching event");

            ShardChangedEventCallback.EVENT.invoker().invoke(shard, currentShard);
            currentShard = shard;
            lastLoadedShard = currentShard;
        }
    }

    private static void debug(String message) {
        if (UnofficialMonumentaModClient.options.shardDebug) {
            UnofficialMonumentaModClient.debug("[Shard Loading] " + message);
        }
    }
}