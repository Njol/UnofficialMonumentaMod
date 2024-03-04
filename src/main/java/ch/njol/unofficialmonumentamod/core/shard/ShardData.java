package ch.njol.unofficialmonumentamod.core.shard;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
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
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.Resource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class ShardData {//TODO clean up this class
	public static final String UNKNOWN_SHARD = "unknown";
	public static final String DEFAULT_KEY_OVERRIDE = "default";

	private static final HashMap<String, Shard> SHARDS = new HashMap<>();
	private static final Identifier FILE_IDENTIFIER = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "override/shards.json");


	protected static boolean editedShard = false;
	public static boolean loadedAtLeastOnce = false;

	private static boolean pebWorldSpoofingEnabled = false;

	public static boolean isEditedShard() {
		return editedShard;
	}

	public static TabShard getCurrentShard() {
		return ShardLoader.getCurrentShard();
	}

	public static TabShard getLastShard() {
		return ShardLoader.getLastShard();
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

	public static String getShard(Text text) {
		String shard = Locations.getShortShardFrom(text);
		if (shard == null) {
			return ShardLoader.UNKNOWN_SHARD;
		}

		return shard;
	}

	public static class Shard {
		public final String officialName;
		public final ShardType shardType;

		@Nullable
		public final Integer maxChests;
		public final boolean canBeDelveBounty;

		public final String smallImageOverride;

		public Shard(String officialName, ShardType shardType, @Nullable Integer maxChests, @Nullable Boolean canBeDelveBounty, @Nullable String smallImageOverride) {
			this.officialName = officialName;
			this.shardType = shardType;
			this.maxChests = maxChests;
			this.canBeDelveBounty = Boolean.TRUE.equals(canBeDelveBounty);
			this.smallImageOverride = smallImageOverride != null ? smallImageOverride : "default";
		}

		@Override
		public String toString() {
			return "{ \"officialName\": \"" + officialName + "\", \"shardType\": \"" + shardType + "\", \"maxChests\": " + maxChests+ ",\"canBeDelveBounty\": \""+ canBeDelveBounty +"\", " + "\"smallImageOverride\": " + smallImageOverride + "\" }";
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

	@FunctionalInterface
	public interface ShardChangedEventCallback {
		Event<ShardChangedEventCallback> EVENT = EventFactory.createArrayBacked(ShardChangedEventCallback.class,
				(listeners) -> (currentShard, previousShard) -> {
					for (ShardChangedEventCallback listener: listeners) {
						//Invoke all event listeners
						listener.invoke(currentShard, previousShard);
					}
				});

		void invoke(TabShard currentShard, TabShard previousShard);
	}
}
