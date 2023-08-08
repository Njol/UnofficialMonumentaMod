package ch.njol.unofficialmonumentamod.core.commands;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.hud.strike.ChestCountOverlay;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MainCommand {
    public LiteralArgumentBuilder<FabricClientCommandSource> register() {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = LiteralArgumentBuilder.literal("umm");

        builder.then(ClientCommandManager.literal("disableChestCountError").executes((ctx) -> runExecuteDisableChestCountError()));

        builder.then(ClientCommandManager.literal("debug").then(ClientCommandManager.literal("addCount").then(ClientCommandManager.argument("count", IntegerArgumentType.integer(0)).executes((MainCommand::runAddCount)))));

        builder.then(ClientCommandManager.literal("info").executes(ctx -> runSelfInfo()));
        builder.then(ClientCommandManager.literal("info").then(ClientCommandManager.literal("modlist").executes(ctx -> runModList())));

        return builder;
    }

    public String getName() {
        return MainCommand.class.getSimpleName();
    }

    private static int runSelfInfo() {
        if (FabricLoader.getInstance().getModContainer(UnofficialMonumentaModClient.MOD_IDENTIFIER).isEmpty()) {
            return 1;
        }
        ModMetadata thisMetadata = FabricLoader.getInstance().getModContainer(UnofficialMonumentaModClient.MOD_IDENTIFIER).get().getMetadata();
        Version version = thisMetadata.getVersion();
        String name = thisMetadata.getName();

        MutableText text = MutableText.of(Text.of("[Mod Info]").getContent()).setStyle(Style.EMPTY.withColor(Formatting.AQUA));

        text.append(MutableText.of(Text.of("\nName: ").getContent()).setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        text.append(MutableText.of(Text.of(name).getContent()).setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));

        text.append(MutableText.of(Text.of("\nVersion: ").getContent()).setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        text.append(MutableText.of(Text.of(version.getFriendlyString()).getContent()).setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));

        text.append(MutableText.of(Text.of("\nMinecraft: ").getContent()).setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        text.append(MutableText.of(Text.of(MinecraftClient.getInstance().getGameVersion() + "-" + SharedConstants.getGameVersion().getName()).getContent()).setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));

        text.append(MutableText.of(Text.of("\nIsDevEnvironment: ").getContent()).setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        text.append(MutableText.of(Text.of(FabricLoader.getInstance().isDevelopmentEnvironment() ? "Yes" : "No").getContent()).setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));

        //other "pages"
        text.append(MutableText.of(Text.of("\n[Press Here to show modlist]").getContent()).setStyle(Style.EMPTY.withColor(Formatting.GRAY).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/umm info modlist"))));
        text.append(MutableText.of(Text.of("\n[Press Here to show current shard]").getContent()).setStyle(Style.EMPTY.withColor(Formatting.GRAY).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ummShard debug loaded"))));

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }

    private static int runModList() {
        Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
        MutableText text = MutableText.of(Text.of("[Mod List]").getContent()).setStyle(Style.EMPTY.withColor(Formatting.AQUA));

        for (ModContainer mod: mods) {
            ModMetadata metadata = mod.getMetadata();
            if (metadata.getId().startsWith("fabric-") || metadata.getId().equals("minecraft") || metadata.getId().equals("java")) {
                continue;//Skip fabric apis, Minecraft and Java.
            }

            MutableText modText = MutableText.of(Text.of("\n" + metadata.getName()).getContent()).setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN));
            modText.append(MutableText.of(Text.of(" (" + metadata.getId() + ") ").getContent()).setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
            modText.append(MutableText.of(Text.of(metadata.getVersion().getFriendlyString()).getContent()).setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN)));

            text.append(modText);
        }

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
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