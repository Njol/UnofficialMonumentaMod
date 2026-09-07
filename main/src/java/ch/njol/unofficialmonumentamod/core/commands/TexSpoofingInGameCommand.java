package ch.njol.unofficialmonumentamod.core.commands;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.features.spoof.TextureSpoofer;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Locale;
import java.util.UUID;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class TexSpoofingInGameCommand extends Constants {
    public LiteralArgumentBuilder<FabricClientCommandSource> register(CommandRegistryAccess access) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = LiteralArgumentBuilder.literal("texspoof");

        builder.then(
                ClientCommandManager.literal("add")
                        .then(
                                ClientCommandManager.argument("name", StringArgumentType.string())
                                        .then(
                                                ClientCommandManager.argument("item", ItemStackArgumentType.itemStack(access))
                                                        .then(
                                                                ClientCommandManager.argument("replacement", StringArgumentType.string())
                                                                        .then(
                                                                                ClientCommandManager.argument("override", BoolArgumentType.bool()
                                                                                ).executes(TexSpoofingInGameCommand::runAdd)
                                                                        )
                                                        )
                                        )
                        )
        );
        builder.then(
                ClientCommandManager.literal("remove")
                        .then(
                                ClientCommandManager.argument("name", StringArgumentType.string())
                                        .suggests(UnofficialMonumentaModClient.spoofer.spoofedItemsKeyProvider())
                                        .executes(TexSpoofingInGameCommand::runRemove)
                                        .then(ClientCommandManager.literal("hope").executes(TexSpoofingInGameCommand::runRemoveHope))
                        )
        );
        builder.then(
                ClientCommandManager.literal("edit")
                        .then(
                                ClientCommandManager.argument("name", StringArgumentType.string())
                                        .suggests(UnofficialMonumentaModClient.spoofer.spoofedItemsKeyProvider())
                                        .then(
                                                ClientCommandManager.literal("replacement")
                                                        .then(
                                                                ClientCommandManager.argument("replacement", StringArgumentType.string())
                                                                        .executes(TexSpoofingInGameCommand::runEditReplacement)
                                                        )
                                        )
                                        .then(
                                                ClientCommandManager.literal("override")
                                                        .then(
                                                                ClientCommandManager.argument("override", BoolArgumentType.bool())
                                                                        .executes(TexSpoofingInGameCommand::runEditOverride)
                                                        )
                                        )
                                        .then(
                                                ClientCommandManager.literal("item")
                                                        .then(
                                                                ClientCommandManager.argument("item", ItemStackArgumentType.itemStack(access))
                                                                        .executes(TexSpoofingInGameCommand::runEditItem)
                                                        )
                                        )
                                        .then(
                                                ClientCommandManager.literal("hope")
                                                        .then(
                                                                ClientCommandManager.argument("uuid", UuidArgumentType.uuid())
                                                                        .executes(TexSpoofingInGameCommand::runEditHope)
                                                        )
                                        )
                        )
        );

        return builder;
    }

    private static String cleanName(String name) {
        return name.toLowerCase(Locale.ROOT).strip();
    }

    private static int runAdd(CommandContext<FabricClientCommandSource> ctx) {
        String itemName = ctx.getArgument("name", String.class);
        itemName = cleanName(itemName);

        //check if item already exists.
        @Nullable TextureSpoofer.SpoofItem item = UnofficialMonumentaModClient.spoofer.getSpoofItemFromName(itemName);
        if (item != null) {
            //There is a spoofed item for this item, send error message and exit.
            ctx.getSource().sendError(Text.of("This item is already spoofed, please use either remove the old one or edit it."));
            return 1;
        }

        String displayName = ctx.getArgument("replacement", String.class);
        ItemStackArgument replacementStack = ctx.getArgument("item", ItemStackArgument.class);
        Boolean override = ctx.getArgument("override", Boolean.class);

        String finalItemName = itemName;
        UnofficialMonumentaModClient.spoofer.runThenSaveFile(() -> UnofficialMonumentaModClient.spoofer.addSpoof(finalItemName, replacementStack.getItem(), displayName, override))
                .thenRun(() -> ctx.getSource().sendError(MutableText.of(PlainTextContent.of("Successfully created new spoof on \"" + finalItemName + "\"")).fillStyle(MAIN_INFO_STYLE)));
        return 0;
    }

    private static int runRemove(CommandContext<FabricClientCommandSource> ctx) {
        String itemName = ctx.getArgument("name", String.class);
        itemName = cleanName(itemName);

        //check if item already exists.
        @Nullable TextureSpoofer.SpoofItem item = UnofficialMonumentaModClient.spoofer.getSpoofItemFromName(itemName);
        if (item == null) {
            //There is not a spoofed item for this item, send error message and exit.
            ctx.getSource().sendError(Text.of("This item is not spoofed."));
            return 1;
        }

        String finalItemName = itemName;
        UnofficialMonumentaModClient.spoofer.runThenSaveFile(() -> UnofficialMonumentaModClient.spoofer.deleteSpoof(finalItemName))
                .thenRun(() -> ctx.getSource().sendFeedback(MutableText.of(PlainTextContent.of("Successfully deleted spoof on \"" + finalItemName + "\"")).fillStyle(MAIN_INFO_STYLE)));
        return 0;
    }

    private static int runEditReplacement(CommandContext<FabricClientCommandSource> ctx) {
        String itemName = ctx.getArgument("name", String.class);
        itemName = cleanName(itemName);

        @Nullable TextureSpoofer.SpoofItem item = UnofficialMonumentaModClient.spoofer.getSpoofItemFromName(itemName);
        if (item == null) {
            //There is not a spoofed item for this item, send error message and exit.
            ctx.getSource().sendError(Text.of("This item is not spoofed."));
            return 1;
        }

        String displayName = ctx.getArgument("replacement", String.class);

        String finalItemName = itemName;
        UnofficialMonumentaModClient.spoofer.runThenSaveFile(() -> UnofficialMonumentaModClient.spoofer.editDisplayName(finalItemName, displayName))
                .thenRun(() -> ctx.getSource().sendFeedback(MutableText.of(PlainTextContent.of("Successfully changed display name replacement on \"" + finalItemName + "\" to \"" + displayName + "\"")).fillStyle(MAIN_INFO_STYLE)));

        return 0;
    }

    private static int runEditOverride(CommandContext<FabricClientCommandSource> ctx) {
        String itemName = ctx.getArgument("name", String.class);
        itemName = cleanName(itemName);

        @Nullable TextureSpoofer.SpoofItem item = UnofficialMonumentaModClient.spoofer.getSpoofItemFromName(itemName);
        if (item == null) {
            //There is not a spoofed item for this item, send error message and exit.
            ctx.getSource().sendError(Text.of("This item is not spoofed."));
            return 1;
        }

        Boolean override = ctx.getArgument("override", Boolean.class);

        String finalItemName = itemName;
        UnofficialMonumentaModClient.spoofer.runThenSaveFile(() -> UnofficialMonumentaModClient.spoofer.editOverride(finalItemName, override))
                .thenRun(() -> ctx.getSource().sendFeedback(MutableText.of(PlainTextContent.of("Successfully changed display override on \"" + finalItemName + "\" to \"" + override + "\"")).fillStyle(MAIN_INFO_STYLE)));
        return 0;
    }

    private static int runEditItem(CommandContext<FabricClientCommandSource> ctx) {
        String itemName = ctx.getArgument("name", String.class);
        itemName = cleanName(itemName);

        @Nullable TextureSpoofer.SpoofItem item = UnofficialMonumentaModClient.spoofer.getSpoofItemFromName(itemName);
        if (item == null) {
            //There is not a spoofed item for this item, send error message and exit.
            ctx.getSource().sendError(Text.of("This item is not spoofed."));
            return 1;
        }

        ItemStackArgument replacementStack = ctx.getArgument("item", ItemStackArgument.class);

        String finalItemName = itemName;
        UnofficialMonumentaModClient.spoofer.runThenSaveFile(() -> UnofficialMonumentaModClient.spoofer.editItem(finalItemName, replacementStack.getItem()))
                .thenRun(() -> ctx.getSource().sendFeedback(MutableText.of(PlainTextContent.of("Successfully changed item replacement on \"" + finalItemName + "\" to \"" + replacementStack.getItem().getName().getString() + "\"")).fillStyle(MAIN_INFO_STYLE)));
        return 0;
    }

    private static int runEditHope(CommandContext<FabricClientCommandSource> ctx) {
        String itemName = ctx.getArgument("name", String.class);
        itemName = cleanName(itemName);

        @Nullable TextureSpoofer.SpoofItem item = UnofficialMonumentaModClient.spoofer.getSpoofItemFromName(itemName);
        if (item == null) {
            //There is not a spoofed item for this item, send error message and exit.
            ctx.getSource().sendError(Text.of("This item is not spoofed."));
            return 1;
        }

        UUID uuid = ctx.getArgument("uuid", UUID.class);

        String finalItemName = itemName;
        UnofficialMonumentaModClient.spoofer.runThenSaveFile(() -> UnofficialMonumentaModClient.spoofer.editAppliedHope(finalItemName, uuid))
                .thenRun(() -> ctx.getSource().sendFeedback(MutableText.of(PlainTextContent.of("Successfully changed item hope replacement on \"" + finalItemName + "\" to \"" + uuid.toString() + "\"")).fillStyle(MAIN_INFO_STYLE)));
        return 0;
    }

    private static int runRemoveHope(CommandContext<FabricClientCommandSource> ctx) {
        String itemName = ctx.getArgument("name", String.class);
        itemName = cleanName(itemName);

        @Nullable TextureSpoofer.SpoofItem item = UnofficialMonumentaModClient.spoofer.getSpoofItemFromName(itemName);
        if (item == null) {
            //There is not a spoofed item for this item, send error message and exit.
            ctx.getSource().sendError(Text.of("This item is not spoofed."));
            return 1;
        }

        String finalItemName = itemName;
        UnofficialMonumentaModClient.spoofer.runThenSaveFile(() -> UnofficialMonumentaModClient.spoofer.editAppliedHope(finalItemName, null))
                .thenRun(() -> ctx.getSource().sendFeedback(MutableText.of(PlainTextContent.of("Successfully removed hope replacement on \"" + finalItemName + "\"")).fillStyle(MAIN_INFO_STYLE)));
        return 0;
    }
}