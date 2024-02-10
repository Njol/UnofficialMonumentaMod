package ch.njol.unofficialmonumentamod.core.shard;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import static ch.njol.unofficialmonumentamod.core.shard.ShardData.TabShard;
import static ch.njol.unofficialmonumentamod.core.shard.ShardData.ShardChangedEventCallback;

public class ShardLoader {
    public static final String UNKNOWN_SHARD = "unknown";

    private static TabShard currentShard = TabShard.UNKNOWN;
    private static TabShard lastLoadedShard = TabShard.UNKNOWN;

    private static boolean worldSpoofEnabled = false;

    public static TabShard getCurrentShard() {
        return currentShard;
    }

    public static TabShard getLastShard() {
        return lastLoadedShard;
    }

    public static boolean isWorldSpoofingEnabled() {
        return worldSpoofEnabled;
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
        loadShardFromTabHeader(shardName);
    }

    public static void onDisconnect() {
        //world spoofing could be enabled or disabled between this and next login for all we know, so we set it as uncertain
        debug("Disconnecting, un-setting world spoofing status");
        worldSpoofEnabled = false;
    }

    public static void onWorldLoaded() {
        if (MinecraftClient.getInstance().world != null) {
            loadWorldFromDimensionKey(MinecraftClient.getInstance().world.getRegistryKey());
            debug("Inferring shard name from dimension key");
        }
    }

    public static void loadShardFromTabHeader(String shardString) {
        TabShard shard = shardString.equalsIgnoreCase(UNKNOWN_SHARD) ? TabShard.UNKNOWN : new TabShard(shardString);

        if (worldSpoofEnabled) {
            debug("Skipping tab header " + shardString + " since world spoofing is enabled.");
            return;
        }
        debug("Received new tab header: " + shardString);

        checkAndDispatchEvent(shard);
    }

    public static void loadWorldFromDimensionKey(RegistryKey<World> dimKey) {
        String extractedShard = extractWorldFromDimKey(dimKey);
        debug("Received new world key: " + extractedShard);
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
            UnofficialMonumentaModClient.LOGGER.info("[Shard Loading] " + message);
        }
    }
}
