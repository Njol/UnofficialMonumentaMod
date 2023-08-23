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
        builder.then(ClientCommandManager.literal("info")
                .then(ClientCommandManager.literal("modlist")
                        .then(ClientCommandManager.literal("clip").executes(ctw -> runCopyInfo()))
                        .executes(ctx -> runModList())));

        return builder;
    }

    public String getName() {
        return MainCommand.class.getSimpleName();
    }

    private static String getSelfInfoString() {
        StringBuilder data = new StringBuilder();
        data.append("[Mod Info]");

        if (FabricLoader.getInstance().isModLoaded(UnofficialMonumentaModClient.MOD_IDENTIFIER)) {
            ModMetadata thisMetadata = FabricLoader.getInstance().getModContainer(UnofficialMonumentaModClient.MOD_IDENTIFIER).get().getMetadata();
            Version version = thisMetadata.getVersion();
            String name = thisMetadata.getName();
            data.append("\nName: ").append(name);
            data.append("\nVersion: ").append(version.getFriendlyString());
        }

        data.append("\nMinecraft: ") .append(MinecraftClient.getInstance().getGameVersion()).append("-").append(SharedConstants.getGameVersion().getName());
        data.append("\nIn Development environment: ").append(FabricLoader.getInstance().isDevelopmentEnvironment() ? "Yes" : "No");
        return data.append("\n").toString();
    }

    private static String getModListString() {
        Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
        StringBuilder data = new StringBuilder();

        data.append("[Mod List]");
        for (ModContainer mod: mods) {
            ModMetadata metadata = mod.getMetadata();
            if (metadata.getId().startsWith("fabric-") || metadata.getId().equals("minecraft") || metadata.getId().equals("java")) {
                continue;//Skip fabric apis, Minecraft and Java.
            }
            data.append("\n").append(metadata.getName()).append(" (").append(metadata.getId()).append(") ").append(metadata.getVersion().getFriendlyString());
            if (mod.getContainingMod().isPresent()) {
                data.append(" via ").append(mod.getContainingMod().get().getMetadata().getId());
            }
        }

        return data.append("\n").toString();
    }

    private static int runCopyInfo() {
        MinecraftClient.getInstance().keyboard.setClipboard(getSelfInfoString().concat(getModListString()));
        MutableText text = Text.literal("Copied info to clipboard").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
        return 0;
    }

    private static int runSelfInfo() {
        if (FabricLoader.getInstance().getModContainer(UnofficialMonumentaModClient.MOD_IDENTIFIER).isEmpty()) {
            return 1;
        }
        ModContainer container = FabricLoader.getInstance().getModContainer(UnofficialMonumentaModClient.MOD_IDENTIFIER).get();

        ModMetadata thisMetadata = container.getMetadata();
        Version version = thisMetadata.getVersion();
        String name = thisMetadata.getName();

        MutableText text = Text.literal("[Mod Info]").setStyle(Style.EMPTY.withColor(Formatting.AQUA));

        text.append(Text.literal("\nName: ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        text.append(Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));

        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            text.append(Text.literal("\nFile name").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
            text.append(Text.literal(container.getOrigin().getPaths().get(0).getFileName().toString()).setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));
        }

        text.append(Text.literal("\nVersion: ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        text.append(Text.literal(version.getFriendlyString()).setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));

        text.append(Text.literal("\nMinecraft: ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        text.append(Text.literal(MinecraftClient.getInstance().getGameVersion() + "-" + SharedConstants.getGameVersion().getName()).setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));

        text.append(Text.literal("\nIn Development environment: ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
        text.append(Text.literal(FabricLoader.getInstance().isDevelopmentEnvironment() ? "Yes" : "No").setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA)));

        //other "pages"
        text.append(Text.literal("\n[Press Here to show modlist]").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/umm info modlist"))));
        text.append(Text.literal("\n[Press Here to show current shard]").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ummShard debug loaded"))));

        //copy all info to clipboard
        text.append(Text.literal("\n[Press Here to copy to clipboard]").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/umm info clip"))));

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }

    private static int runModList() {
        Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
        MutableText text = Text.literal("[Mod List]").setStyle(Style.EMPTY.withColor(Formatting.AQUA));

        for (ModContainer mod: mods) {
            ModMetadata metadata = mod.getMetadata();
            if (metadata.getId().startsWith("fabric-") || metadata.getId().equals("minecraft") || metadata.getId().equals("java")) {
                continue;//Skip fabric apis, Minecraft and Java.
            }

            MutableText modText = Text.literal("\n" + metadata.getName()).setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN));
            modText.append(Text.literal(" (" + metadata.getId() + ") ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
            modText.append(Text.literal(metadata.getVersion().getFriendlyString()).setStyle(Style.EMPTY.withColor(Formatting.DARK_GREEN)));

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
        MutableText text = Text.literal("[UMM] Successfully disabled warning message").setStyle(Style.EMPTY.withColor(Formatting.AQUA));
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }

    public static int runAddCount(CommandContext<FabricClientCommandSource> commandContext) {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            MutableText text = Text.literal("[UMM] nuh uh, not happening.").setStyle(Style.EMPTY.withColor(Formatting.AQUA));
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
            return 1;
        }
        int count = IntegerArgumentType.getInteger(commandContext, "count");
        ChestCountOverlay.INSTANCE.addCount(count);

        MutableText text = Text.literal("[UMM] added " + count + " to chestCountOverlay").setStyle(Style.EMPTY.withColor(Formatting.AQUA));
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }
}