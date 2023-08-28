package ch.njol.unofficialmonumentamod.core.shard;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.commands.Constants;
import ch.njol.unofficialmonumentamod.hud.strike.ChestCountOverlay;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ShardDebugCommand extends Constants {
    public LiteralArgumentBuilder<FabricClientCommandSource> register() {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = LiteralArgumentBuilder.literal("ummShard");

        //list shards
        builder.then(ClientCommandManager.literal("list").executes(ShardDebugCommand::executeList));
        builder.then(ClientCommandManager.literal("debug")
                .then(ClientCommandManager.literal("set").then(ClientCommandManager.argument("shard", ShardArgumentType.Key()).executes(ShardDebugCommand::executeDebugSet)))
                .then(ClientCommandManager.literal("get").then(ClientCommandManager.argument("shard", ShardArgumentType.Key()).executes(ShardDebugCommand::executeDebugGet)))
                .then(ClientCommandManager.literal("loaded").executes(ShardDebugCommand::executeDebugLoaded)));
        return builder;
    }

    public String getName() {
        return ShardDebugCommand.class.getSimpleName();
    }

    public static int executeList(CommandContext<FabricClientCommandSource> context) {
        try {
            final HashMap<String, ShardData.Shard> shards = ShardData.getShards();

            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("Currently loaded shards:").setStyle(MAIN_INFO_STYLE.withBold(true)));

            for (Map.Entry<String, ShardData.Shard> shardEntry : shards.entrySet()) {
                MutableText shardText = Text.literal(shardEntry.getKey());
                ShardData.Shard shard = shardEntry.getValue();

                shardText.setStyle(
                        MAIN_INFO_STYLE
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(
                                        "Official name: " + shard.officialName + "\nShard type: " + shard.shardType + "\nMax chests: " + (shard.maxChests != null ? shard.maxChests : "None")
                                )))
                );

                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(shardText);

            }
            return 0;
        } catch (Exception e) {
            UnofficialMonumentaModClient.LOGGER.error("Caught error while enumerating loaded shards", e);
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("[UMM] Caught error while trying to enumerate loaded shards").setStyle(ERROR_STYLE));
            return -1;
        }
    }

    public static int executeDebugSet(CommandContext<FabricClientCommandSource> context) {
        String shardName = context.getArgument("shard", String.class);

        ShardData.editedShard = true;
        ShardData.onShardChangeSkipChecks(shardName);
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("The Mod will now believe you are in: " + shardName).setStyle(MAIN_INFO_STYLE.withBold(true)));
        return 0;
    }

    public static int executeDebugGet(CommandContext<FabricClientCommandSource> context) {
        MutableText shardText = Text.literal("Shard: " + context.getArgument("shard", String.class));
        ShardData.Shard shard = ShardArgumentType.getShardFromKey(context, "shard");

        assert shard != null;
        shardText.setStyle(MAIN_INFO_STYLE.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Official name: " + shard.officialName + "\nShard type: " + shard.shardType + "\nMax chests: " + (shard.maxChests != null ? shard.maxChests : "None")))).withBold(true));

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(shardText);
        return 0;
    }

    public static int executeDebugLoaded(CommandContext<FabricClientCommandSource> context) {
        try {
            ChestCountOverlay chestCountOverlay = ChestCountOverlay.INSTANCE;

            Integer count = chestCountOverlay.getCurrentCount();
            Integer max = chestCountOverlay.getTotalChests();
            String lastShard = ShardData.getLastShard().shardString;
            String currentShard = ShardData.getCurrentShard().shardString;
            boolean isSearching = ShardData.isSearchingForShard();
            boolean isEdited = ShardData.editedShard;

            //check if it loaded correctly when entering the shard (should show false if it wasn't able to load the shard after world load)
            boolean loadedCorrectly = !isSearching && !Objects.equals(lastShard, currentShard);

            //count: (if max exists then count/max else just count) loaded shard: lastShard, current shard: currentShard
            MutableText text = Text.literal("[Current Shard]\n").setStyle(MAIN_INFO_STYLE);

            text.append(Text.literal("Count: ").setStyle(KEY_INFO_STYLE));
            text.append(Text.literal((max != null ? count + "/" + max : count) + "\n").setStyle(VALUE_STYLE));

            text.append(Text.literal("Last shard: ").setStyle(KEY_INFO_STYLE));
            text.append(Text.literal(lastShard).setStyle(VALUE_STYLE));

            text.append(Text.literal(" | Current shard: ").setStyle(KEY_INFO_STYLE));
            text.append(Text.literal(currentShard + "\n").setStyle(VALUE_STYLE));

            text.append(Text.literal("Loaded correctly: ").setStyle(KEY_INFO_STYLE));
            text.append(Text.literal(loadedCorrectly ? "Yes" : "No").setStyle(VALUE_STYLE));

            text.append(Text.literal(" | Was edited: ").setStyle(KEY_INFO_STYLE));
            text.append(Text.literal(isEdited ? "Yes" : "No").setStyle(VALUE_STYLE));

            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
            return 0;
        } catch (Exception e) {
            UnofficialMonumentaModClient.LOGGER.error("Caught error while enumerating current loaded shard", e);
            return -1;
        }
    }
}
