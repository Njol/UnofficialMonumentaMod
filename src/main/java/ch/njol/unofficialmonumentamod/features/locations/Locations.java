package ch.njol.unofficialmonumentamod.features.locations;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.features.calculator.Calculator;
import ch.njol.unofficialmonumentamod.mixins.PlayerListHudAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class Locations {
	//region utilities
	private static final MinecraftClient mc = MinecraftClient.getInstance();
	private static final Pattern shardGetterPattern = Pattern.compile(".*<(?<shard>[-\\w\\d]*)>.*");

	//region cache
	private static long lastUpdateTimeShortShard;
	private static String cachedShortShard;

	private static long lastUpdateTimeShard;
	private static String cachedShard;

	public static void resetCache() {
		cachedShard = null;
		cachedShortShard = null;
		getShortShard();
	}
	//endregion

	public static String getShard() {
		if (cachedShard != null && lastUpdateTimeShard + 2000 > System.currentTimeMillis()) {
			return cachedShard;
		}
		Text header = ((PlayerListHudAccessor) mc.inGameHud.getPlayerListHud()).getHeader();
		String shard = "unknown";
		if (header != null) {
			String text = header.getString();
			Matcher matcher = shardGetterPattern.matcher(text);
			if (matcher.matches()) {
				shard = matcher.group("shard");
			}
		}

		cachedShard = shard;
		lastUpdateTimeShard = System.currentTimeMillis();
		return shard;
	}

	public static String getShardFrom(Text text) {
		if (text == null) {
			return null;
		}
		String message = text.getString();
		Matcher matcher = shardGetterPattern.matcher(message);

		String shard = null;
		if (matcher.matches()) {
			shard = matcher.group("shard");
		}

		return shard;
	}

	public static String getShortShardFrom(Text text) {
		String fullShard = getShardFrom(text);
		if (fullShard == null) {
			return null;
		}

		return cachedShard.replaceFirst("-\\d+$", "");
	}

	public static String getShortShard() {
		if (cachedShortShard != null && lastUpdateTimeShortShard + 2000 > System.currentTimeMillis()) {
			return cachedShortShard;
		}

		String shard = getShard();
		shard = shard.replaceFirst("-\\d+$", "");

		cachedShortShard = shard;
		lastUpdateTimeShortShard = System.currentTimeMillis();

		return shard;
	}
	//endregion

	@Expose
	public static HashMap<String, ArrayList<Location>> locations = new java.util.HashMap<>();

	private static final Gson GSON = new GsonBuilder()
		                                 .setPrettyPrinting()
		                                 .create();
	private static final Identifier FILE_IDENTIFIER = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "override/locations.json");

	public void reload() {
		locations.clear();
		Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(FILE_IDENTIFIER);
		if (resource.isEmpty()) {
			return;
		}
		try (InputStream stream = resource.get().getInputStream();
		     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			locations = GSON.fromJson(reader, new TypeToken<HashMap<String, ArrayList<Location>>>() {
			}.getType());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ArrayList<Location> getLocs(String shard) {
		shard = shard.replaceFirst("-\\d+$", "");
		shard = shard.toLowerCase(Locale.ROOT);

		return locations.get(shard);
	}

	public String getLocation(double x, double z, String shard) {
		ArrayList<Location> locations = getLocs(shard);
		if (locations == null) {
			return shard;
		}

		for (Location loc : locations) {
			if (loc.isInBounds(x, z)) {
				return loc.name;
			}
		}

		return shard;
	}

	public static class Location {
		int east;
		int north;
		int west;
		int south;

		String name;

		public Location(int east, int north, int west, int south, String name) {
			this.east = east;
			this.north = north;
			this.west = west;
			this.south = south;
			this.name = name;
		}

		public boolean isInBounds(double playerX, double playerZ) {
			return ((playerX >= east && playerX <= west) || (playerX <= east && playerX >= west))
				       && ((playerZ >= north && playerZ <= south) || (playerZ <= north && playerZ >= south));
		}
	}

	private static boolean searchingForShard = true;
	private static String lastShard = null;

	public static void onWorldLoad() {
		searchingForShard = true;
	}

	public static void onPlayerListHeader(Text text) {
		if (!searchingForShard || Locations.getShardFrom(text) == null) {
			return;
		}
		String shard = Locations.getShortShard();
		if (Objects.equals(shard, "unknown")) {
			return;
		}
		if (!Objects.equals(shard, lastShard)) { // reset
			Calculator.onChangeShardListener(shard);
			lastShard = shard;
			searchingForShard = false;
		}
	}

}
