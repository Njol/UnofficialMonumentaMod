package ch.njol.unofficialmonumentamod.core.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.jetbrains.annotations.NotNull;

public class CommandHelpBuilder {
    private static final Map<String, String> commandTrees = new HashMap<>();

    public static String getTreeOf(String commandRoot) {
        return commandTrees.getOrDefault(commandRoot, "UNSET");
    }

    public static Map<String, String> getCommandTrees() {
        return commandTrees;
    }

    public static SuggestionProvider<FabricClientCommandSource> CommandHelpSuggestionProvider() {
        return (context, builder) -> {
            Set<String> keys = commandTrees.keySet();
            for (String key: keys) {
                builder.suggest(key);
            }
            return builder.buildFuture();
        };
    }

    public static void initialize(List<LiteralArgumentBuilder<FabricClientCommandSource>> roots) {
        commandTrees.clear();
        for (LiteralArgumentBuilder<FabricClientCommandSource> root : roots) {
            commandTrees.put(root.getLiteral(), convertCommandToTree(root));
        }
    }

    private static String convertCommandToTree(LiteralArgumentBuilder<FabricClientCommandSource> root) {
        List<List<ArgumentBuilder<?,?>>> tree = explorePath(root);
        StringBuilder builder = new StringBuilder();
        builder.append(root.getLiteral()).append(" ");

        for (int x = 0; x < tree.size(); x++) {
            List<ArgumentBuilder<?,?>> args = tree.get(x);
            if (args.isEmpty()) {
                continue;
            }
            //depth woo

            String argumentsString = getArgumentsString(args);
            builder.append("<").append(argumentsString).append(">");
            if (x < tree.size() - 1) {
                builder.append(" ");
            }
        }

        return builder.toString();
    }

    private static @NotNull String getArgumentsString(List<ArgumentBuilder<?, ?>> args) {
        StringBuilder subBuilder = new StringBuilder();

        for (int y = 0; y < args.size(); y++) {
            ArgumentBuilder<?,?> arg = args.get(y);

            if (arg instanceof RequiredArgumentBuilder<?,?> requiredArg) {
                subBuilder.append(requiredArg.getName());
            } else if (arg instanceof LiteralArgumentBuilder<?> literalArg) {
                subBuilder.append(literalArg.getLiteral());
            } else {
                subBuilder.append("UNKNOWN");
            }

            if (y < args.size() - 1) {
                subBuilder.append(" | ");
            }
        }
        return subBuilder.toString();
    }

    private static List<List<ArgumentBuilder<?,?>>> explorePath(ArgumentBuilder<?,?> root) {
        int depth = 0;
        return explorePath(new ArrayList<>(), root.build(), depth);
    }

    //matrix x = depth. This is so bad of an idea.
    private static List<List<ArgumentBuilder<?,?>>> explorePath(List<List<ArgumentBuilder<?,?>>> matrix, CommandNode<?> pathRoot, int depth) {
        List<ArgumentBuilder<?,?>> path = new ArrayList<>();

        Collection<? extends CommandNode<?>> arguments = pathRoot.getChildren();
        for (CommandNode<?> node: arguments) {
            ArgumentBuilder<?,?> nodeBuilder = node.createBuilder();
            path.add(nodeBuilder);
            explorePath(matrix, node, depth + 1);
        }

        if (matrix.size() > depth) {
            matrix.get(depth).addAll(path);
        } else {
            matrix.add(path);
        }
        return matrix;
    }
}