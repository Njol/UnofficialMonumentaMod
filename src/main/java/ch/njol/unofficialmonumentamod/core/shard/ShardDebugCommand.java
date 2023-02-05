package ch.njol.unofficialmonumentamod.core.shard;

import ch.njol.unofficialmonumentamod.features.strike.ChestCountOverlay;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ShardDebugCommand {
    public LiteralArgumentBuilder<FabricClientCommandSource> register() {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = LiteralArgumentBuilder.literal("ummShard");

        //list shards
        builder.then(
                ClientCommandManager.literal("list")
                        .executes(ShardDebugCommand::executeList)
        );
        //debug stuff e.g
        //change -> change loaded shard to chosen one
        //get -> sends to chat the information about the chosen shard (Shard "name" -> tooltip -> the json data)
        //chest -> sends the information loaded by ChestCountOverlay.java
        builder.then(
                ClientCommandManager.literal("debug")
                        .then(
                                ClientCommandManager.literal("change")
                                        .then(
                                                ClientCommandManager.argument("shard", ShardArgumentType.Key())
                                                        .executes(ShardDebugCommand::executeDebugChange)
                                        )
                        )
                        .then(
                                ClientCommandManager.literal("get")
                                        .then(
                                                ClientCommandManager.argument("shard", ShardArgumentType.Key())
                                                        .executes(ShardDebugCommand::executeDebugGet)
                                        )
                        )
                        .then(
                                ClientCommandManager.literal("loaded")
                                        .executes(ShardDebugCommand::executeDebugLoaded)
                        )
        );
        return builder;
    }

    public String getName() {
        return ShardDebugCommand.class.getSimpleName();
    }

    public static int executeList(CommandContext<FabricClientCommandSource> context) {
        try {
            final HashMap<String, ShardData.Shard> shards = ShardData.getShards();

            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText("Currently loaded shards:").setStyle(Style.EMPTY.withColor(Formatting.AQUA).withBold(true)));

            for (Map.Entry<String, ShardData.Shard> shardEntry : shards.entrySet()) {
                LiteralText shardText = new LiteralText(shardEntry.getKey());
                ShardData.Shard shard = shardEntry.getValue();

                shardText.setStyle(
                        Style.EMPTY
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(
                                        "Official name: " + shard.officialName + "\nShard type: " + shard.shardType + "\nMax chests: " + (shard.maxChests != null ? shard.maxChests : "None")
                                )))
                                .withColor(Formatting.AQUA)
                );

                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(shardText);

            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int executeDebugChange(CommandContext<FabricClientCommandSource> context) {
        String shardName = context.getArgument("shard", String.class);

        ShardData.editedShard = true;
        ShardData.bypassCheckOnShardChange(shardName);
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText("Successfully \"changed\" shards").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.AQUA)));
        return 0;
    }

    public static int executeDebugGet(CommandContext<FabricClientCommandSource> context) {
        LiteralText shardText = new LiteralText("Shard: " + context.getArgument("shard", String.class));
        ShardData.Shard shard = ShardArgumentType.getShardFromKey(context, "shard");

        assert shard != null;
        shardText.setStyle(
                Style.EMPTY
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(
                                "Official name: " + shard.officialName + "\nShard type: " + shard.shardType + "\nMax chests: " + (shard.maxChests != null ? shard.maxChests : "None")
                        )))
                        .withColor(Formatting.AQUA).withBold(true)
        );

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(shardText);
        return 0;
    }

    public static int executeDebugLoaded(CommandContext<FabricClientCommandSource> context) {
        try {
            ChestCountOverlay chestCountOverlay = ChestCountOverlay.INSTANCE;

            Integer count = chestCountOverlay.getCurrentCount();
            Integer max = chestCountOverlay.getTotalChests();
            String lastShard = ShardData.getLastShard();
            String currentShard = ShardData.getCurrentShard();
            boolean isSearching = ShardData.isSearchingForShard();
            boolean isEdited = ShardData.editedShard;

            //check if it loaded correctly when entering the shard (should show false if it wasn't able to load the shard after world load)
            boolean loadedCorrectly = !isSearching && !Objects.equals(lastShard, currentShard);

            //count: (if max exists then count/max else just count) loaded shard: lastShard, current shard: currentShard
            LiteralText text = new LiteralText("Current shard data: " + "\nCount: " + (max != null ? count + "/" + max : count) + "\nLast shard: " + lastShard + " | Current shard: " + currentShard + "\nLoaded correctly: " + loadedCorrectly + " | Was edited: " + isEdited);
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text.setStyle(Style.EMPTY.withColor(Formatting.AQUA).withItalic(true)));
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}