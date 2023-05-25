package ch.njol.unofficialmonumentamod.core.shard;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.features.calculator.Calculator;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import ch.njol.unofficialmonumentamod.hud.strike.ChestCountOverlay;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;
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

	private static boolean searchingForShard;
	private static String lastShard;
	private static String currentShard = "unknown";

	protected static boolean editedShard = false;

	public static boolean isEditedShard() {
		return editedShard;
	}

	public static String getCurrentShard() {
		return currentShard;
	}

	public static String getLastShard() {
		return lastShard;
	}

	public static boolean isSearchingForShard() {
		return searchingForShard;
	}

	public static void stopSearch() {
		searchingForShard = false;
	}

	public static void onWorldLoad() {
		//set the last shard as the current loaded one if it exists
		lastShard = currentShard;
		searchingForShard = true;
		editedShard = false;

		//If player has world name spoofing on in the PEB
		if (MinecraftClient.getInstance().world != null) {
			Identifier worldName = MinecraftClient.getInstance().world.getRegistryKey().getValue();
			if (ShardData.isExistingShard(worldName.getPath())) {
				String shard = worldName.getPath();

				onShardChange(shard);
				System.out.println("Inferred shard data from world name.");
			}
		}
	}

	protected static void bypassCheckOnShardChange(String shardName) {
		searchingForShard = true;
		onShardChange(shardName);
	}

	public static void onShardChange(String shardName) {
		if (shardName == null) {
			shardName = "unknown";
		}

		if (!searchingForShard) {
			//if not unknown and not last shard
			if (!editedShard && (!Objects.equals(shardName, "unknown") && !Objects.equals(currentShard, "unknown")) && (!Objects.equals(lastShard, shardName) && !Objects.equals(currentShard, shardName))) {
				System.out.println("Unexpected shard change.\nNew shard: " + shardName + " Old shard: " + lastShard + " Currently loaded: " + currentShard);
			}
			return;
		}

		currentShard = shardName;

		if (!Objects.equals(currentShard, lastShard)) {//shard changed
			Locations.resetCache();
			ChestCountOverlay.INSTANCE.onShardChange(shardName);
			Calculator.onChangeShardListener(shardName);
		}
		stopSearch();
	}

	public static HashMap<String, Shard> getShards() {
		return SHARDS;
	}

	public static boolean isExistingShard(String shard)  {
		return SHARDS.get(shard) != null;
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
		public final boolean canBeDelveBounty;

		public Shard(String officialName, ShardType shardType, @Nullable Integer maxChests, @Nullable Boolean canBeDelveBounty) {
			this.officialName = officialName;
			this.shardType = shardType;
			this.maxChests = maxChests;
			this.canBeDelveBounty = Boolean.TRUE.equals(canBeDelveBounty);
		}

		@Override
		public String toString() {
			return "{ \"officialName\": \"" + officialName + "\", \"shardType\": \"" + shardType + "\", \"maxChests\": " + maxChests + ",\"canBeDelveBounty\": \""+ canBeDelveBounty +"\" }";
		}
	}

	public enum ShardType {
		STRIKE,
		OVERWORLD,
		DUNGEON,
		MINIGAME
	}
}
