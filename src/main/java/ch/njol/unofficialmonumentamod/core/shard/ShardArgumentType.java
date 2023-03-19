package ch.njol.unofficialmonumentamod.core.shard;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ShardArgumentType implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("valley", "plots");

    private ShardArgumentType() {}

    public static ShardArgumentType Key() {
        return new ShardArgumentType();
    }

    public static ShardData.Shard getShardFromKey(final CommandContext<?> context, final String name) {
        try {
            return ShardData.getShards().get(context.getArgument(name, String.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String parse(StringReader reader) {
        try {
            String readString = reader.getRemaining();
            reader.readString();
            readString = readString.trim();

            for (Map.Entry<String, ShardData.Shard> entry: ShardData.getShards().entrySet()) {
                if (!entry.getKey().startsWith(readString) && !entry.getValue().officialName.startsWith(readString)) continue;
                return entry.getKey();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <S>CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        for (Map.Entry<String, ShardData.Shard> entry: ShardData.getShards().entrySet()) {
            if (entry.getKey().startsWith(builder.getRemaining()) || entry.getValue().officialName.startsWith(builder.getRemaining())) {
                builder.suggest(entry.getKey());
            }
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
