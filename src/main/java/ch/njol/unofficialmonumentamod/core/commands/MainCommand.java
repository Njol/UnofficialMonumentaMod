package ch.njol.unofficialmonumentamod.core.commands;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.hud.strike.ChestCountOverlay;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MainCommand {
    public LiteralArgumentBuilder<FabricClientCommandSource> register() {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = LiteralArgumentBuilder.literal("umm");

        builder.then(ClientCommandManager.literal("disableChestCountError").executes((ctx) -> runExecuteDisableChestCountError()));

        builder.then(ClientCommandManager.literal("debug").then(ClientCommandManager.literal("addCount").then(ClientCommandManager.argument("count", IntegerArgumentType.integer(0)).executes((MainCommand::runAddCount)))));

        return builder;
    }

    public String getName() {
        return MainCommand.class.getSimpleName();
    }

    public static int runExecuteDisableChestCountError() {
        if (!UnofficialMonumentaModClient.options.enableChestCountMaxError) {
            return 1;
        }
        UnofficialMonumentaModClient.options.enableChestCountMaxError = false;
        //wouldn't want to mitigate the effect of the command.
        UnofficialMonumentaModClient.saveConfig();
        MutableText text = MutableText.of(Text.of("[UMM] Successfully disabled warning message").getContent()).setStyle(Style.EMPTY.withColor(Formatting.AQUA));
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }

    public static int runAddCount(CommandContext<FabricClientCommandSource> commandContext) {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            MutableText text = MutableText.of(Text.of("[UMM] nuh uh, not happening.").getContent()).setStyle(Style.EMPTY.withColor(Formatting.AQUA));
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
            return 1;
        }
        int count = IntegerArgumentType.getInteger(commandContext, "count");
        ChestCountOverlay.INSTANCE.addCount(count);

        MutableText text = MutableText.of(Text.of("[UMM] added " + count + " to chestCountOverlay").getContent()).setStyle(Style.EMPTY.withColor(Formatting.AQUA));
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }
}
