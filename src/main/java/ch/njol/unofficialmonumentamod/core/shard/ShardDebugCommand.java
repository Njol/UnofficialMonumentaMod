package ch.njol.unofficialmonumentamod.core.shard;

import ch.njol.unofficialmonumentamod.features.locations.Locations;
import ch.njol.unofficialmonumentamod.hud.strike.ChestCountOverlay;
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

        builder.then(ClientCommandManager.literal("list").executes(ShardDebugCommand::executeList));
        builder.then(ClientCommandManager.literal("debug")
                        .then(ClientCommandManager.literal("set").then(ClientCommandManager.argument("shard", ShardArgumentType.Key()).executes(ShardDebugCommand::executeDebugSet)))
                        .then(ClientCommandManager.literal("get").then(ClientCommandManager.argument("shard", ShardArgumentType.Key()).executes(ShardDebugCommand::executeDebugGet)))
                        .then(ClientCommandManager.literal("loaded").executes(ShardDebugCommand::executeDebugLoaded))
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
                        Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Official name: " + shard.officialName + "\nShard type: " + shard.shardType + "\nMax chests: " + (shard.maxChests != null ? shard.maxChests : "None")))).withColor(Formatting.AQUA)
                );

                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(shardText);

            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int executeDebugSet(CommandContext<FabricClientCommandSource> context) {
        String shardName = context.getArgument("shard", String.class);

        ShardData.editedShard = true;
        ShardData.onShardChangeSkipChecks(shardName);
        Locations.setShard(shardName);

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText("The Mod will now believe you are in: " + shardName).setStyle(Style.EMPTY.withBold(true).withColor(Formatting.AQUA)));
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
            String lastShard = ShardData.getLastShard().shardString;
            String currentShard = ShardData.getCurrentShard().shardString;
            boolean isSearching = ShardData.isSearchingForShard();
            boolean isEdited = ShardData.editedShard;

            //check if it loaded correctly when entering the shard (should show false if it wasn't able to load the shard after world load)
            boolean loadedCorrectly = !isSearching && !Objects.equals(lastShard, currentShard);

            MutableText text = new LiteralText("[Current Shard]\n").setStyle(Style.EMPTY.withColor(Formatting.AQUA));

            text.append(new LiteralText("Count: ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
            text.append(new LiteralText((max != null ? count + "/" + max : count) + "\n").setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));

            text.append(new LiteralText("Last shard: ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
            text.append(new LiteralText(lastShard).setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));

            text.append(new LiteralText(" | Current shard: ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
            text.append(new LiteralText(currentShard + "\n").setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));

            text.append(new LiteralText("Loaded correctly: ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
            text.append(new LiteralText(loadedCorrectly ? "Yes" : "No").setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));

            text.append(new LiteralText(" | Was edited: ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
            text.append(new LiteralText(isEdited ? "Yes" : "No").setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));

            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}