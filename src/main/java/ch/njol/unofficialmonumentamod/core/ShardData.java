package ch.njol.unofficialmonumentamod.core;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class ShardData {

	private static final HashMap<String, Shard> SHARDS = new HashMap<>();

	private static final Identifier FILE_IDENTIFIER = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "override/shards.json");


	public static void reload() {
		SHARDS.clear();
		try (InputStream stream = MinecraftClient.getInstance().getResourceManager().getResource(FILE_IDENTIFIER).getInputStream()) {
			HashMap<String, Shard> hash = new GsonBuilder().create().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), new TypeToken<HashMap<String, Shard>>() {
			}.getType());
			if (hash != null) {
				SHARDS.putAll(hash);
			}
		} catch (IOException | JsonParseException e) {
			UnofficialMonumentaModClient.LOGGER.error("Caught error while trying to load shards");
			e.printStackTrace();
		}
	}

	public static String getOfficialName(String shard) {
		if (SHARDS.containsKey(shard)) {
			return SHARDS.get(shard).officialName;
		}
		return null;
	}

	public static Integer getMaxChests(String shard) {
		if (SHARDS.containsKey(shard)) {
			return SHARDS.get(shard).maxChests;
		}
		return null;
	}

	public static class Shard {
		public final String officialName;
		public final ShardType shardType;
		@Nullable
		public final Integer maxChests;

		public Shard(String officialName, ShardType shardType, @Nullable Integer maxChests) {
			this.officialName = officialName;
			this.shardType = shardType;
			this.maxChests = maxChests;
		}

		@Override
		public String toString() {
			return "{ \"officialName\": \"" + officialName + "\", \"shardType\": \"" + shardType.toString() + "\", \"maxChests\": " + maxChests + " }";
		}
	}

	public enum ShardType {
		STRIKE,
		OVERWORLD,
		DUNGEON,
		MINIGAME
	}
}
