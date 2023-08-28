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
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class ShardData {
	public static final String UNKNOWN_SHARD = "unknown";

	private static final HashMap<String, Shard> SHARDS = new HashMap<>();
	private static final Identifier FILE_IDENTIFIER = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "override/shards.json");


	public static void reload() {
		SHARDS.clear();
		Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(FILE_IDENTIFIER);
		if (resource.isEmpty()) {
			return;
		}
		try (InputStream stream = resource.get().getInputStream()) {
			HashMap<String, Shard> hash = new GsonBuilder().create().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), new TypeToken<HashMap<String, Shard>>() {
			}.getType());
			if (hash != null) {
				SHARDS.putAll(hash);
			}
		} catch (IOException | JsonParseException e) {
			UnofficialMonumentaModClient.LOGGER.error("Caught error while trying to reload shards", e);
		}
	}

	private static boolean searchingForShard;
	private static boolean loadedFromWorldName;
	private static TabShard lastShard;
	private static TabShard currentShard = TabShard.UNKNOWN;

	protected static boolean editedShard = false;
	public static boolean loadedAtLeastOnce = false;

	public static boolean isEditedShard() {
		return editedShard;
	}

	public static TabShard getCurrentShard() {
		return currentShard;
	}

	public static TabShard getLastShard() {
		return lastShard;
	}

	public static boolean isSearchingForShard() {
		return searchingForShard;
	}

	public static void stopSearch() {
		searchingForShard = false;
	}

	public static void onWorldLoad() {
		if (!loadedAtLeastOnce) {
			loadedAtLeastOnce = true;
		}

		//set the last shard as the current loaded one if it exists
		lastShard = currentShard;
		searchingForShard = true;
		loadedFromWorldName = false;
		editedShard = false;

		//If player has world name spoofing on in the PEB
		if (MinecraftClient.getInstance().world != null) {
			Identifier worldName = MinecraftClient.getInstance().world.getRegistryKey().getValue();
			if (ShardData.isExistingShard(worldName.getPath())) {
				String shard = worldName.getPath();
				loadedFromWorldName = true;

				onShardChange(shard);
				if (UnofficialMonumentaModClient.options.shardDebug) {
					UnofficialMonumentaModClient.LOGGER.info("Inferred shard data from world name.");
				}
			}
		}
	}

	public static void onPlayerSynchronizePosition() {
		if (!loadedAtLeastOnce) {
			return;//first loading needs to be using world loading.
		}
		if (UnofficialMonumentaModClient.options.shardDebug) {
			UnofficialMonumentaModClient.LOGGER.info("Called Shard change from synchronization event.");
		}
		onWorldLoad();
	}

	protected static void onShardChangeSkipChecks(String shardName) {
		searchingForShard = true;
		onShardChange(shardName);
	}

	public static void onShardChange(String shardName) {
		if (shardName == null) {
			shardName = UNKNOWN_SHARD;
		}

		if (!searchingForShard) {
			//if not unknown and not last shard
			//if the new shard is not unknown and the last shard exists.
			if (UnofficialMonumentaModClient.options.shardDebug && !editedShard && !loadedFromWorldName && (!Objects.equals(shardName, UNKNOWN_SHARD) && !Objects.equals(currentShard.shardString, UNKNOWN_SHARD)) && (!Objects.equals(lastShard.shardString, shardName) && !Objects.equals(currentShard.shardString, shardName))) {
				UnofficialMonumentaModClient.LOGGER.warn("Unexpected shard change.\nNew shard: " + shardName + " Old shard: " + lastShard + " Currently loaded: " + currentShard);
			}
			return;
		}

		currentShard = shardName.equals(UNKNOWN_SHARD) ? TabShard.UNKNOWN : new TabShard(shardName);

		if (!Objects.equals(currentShard, lastShard) && currentShard != TabShard.UNKNOWN) {//shard changed and new shard is not unknown.
			Locations.resetCache();
			ChestCountOverlay.INSTANCE.onShardChange(shardName);
			Calculator.onChangeShardListener(shardName);

			if (UnofficialMonumentaModClient.options.shardDebug) {
				UnofficialMonumentaModClient.LOGGER.info("Shard changed.");
			}
		}
		if (currentShard != TabShard.UNKNOWN) {
			//continue search if shard is unknown.
			stopSearch();
		}
	}

	public static HashMap<String, Shard> getShards() {
		return SHARDS;
	}

	public static Shard getShard(String shard) {
		return SHARDS.get(shard);
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
			return "{ \"officialName\": \"" + officialName + "\", \"shardType\": \"" + shardType + "\", \"maxChests\": " + maxChests+ ",\"canBeDelveBounty\": \""+ canBeDelveBounty +"\" }";
		}
	}

	public static class TabShard {
		@NotNull
		public final String shardString;
		@NotNull
		public final String shortShard;
		@Nullable
		public final Shard shard;

		public TabShard(@NotNull String shard) {
			this.shardString = shard;
			this.shortShard = shardString.replaceFirst("-\\d+$", "");
			this.shard = getShard(shortShard);
		}

		protected static TabShard UNKNOWN = new TabShard(UNKNOWN_SHARD);

		@Override
		public String toString() {
			return shardString;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			TabShard other = (TabShard) o;
			//check object equality of every field.
			return shardString.equals(other.shardString) && shortShard.equals(other.shortShard) && Objects.equals(shard, other.shard);
		}
	}

	public enum ShardType {
		STRIKE,
		OVERWORLD,
		DUNGEON,
		MINIGAME
	}
}
