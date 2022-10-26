package ch.njol.unofficialmonumentamod.features.locations;

import ch.njol.unofficialmonumentamod.mixins.PlayerListHudAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Locations {
    //region utilities
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Pattern shardGetterPattern = Pattern.compile(".*<(?<shard>[-\\w\\d]*)>.*");

    //region cache
    private static long lastUpdateTimeShortShard;
    private static String cachedShortShard;

    private static long lastUpdateTimeShard;
    private static String cachedShard;
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

        if (cachedShard == null || lastUpdateTimeShard + 2000 < System.currentTimeMillis()) {
            cachedShard = shard;
            lastUpdateTimeShard = System.currentTimeMillis();
        }
        return shard;
    }

    public static String getShortShard() {
        if (cachedShortShard != null && lastUpdateTimeShortShard + 2000 > System.currentTimeMillis()) {
            return cachedShortShard;
        }

        String shard = getShard();
        if (shard.matches("\\w*-[0-9]")) {
            shard = shard.substring(0, shard.length() - 2);
        }

        if (cachedShortShard == null || lastUpdateTimeShortShard + 2000 < System.currentTimeMillis()) {
            cachedShortShard = shard;
            lastUpdateTimeShortShard = System.currentTimeMillis();
        }

        return shard;
    }
    //endregion

    //region locations
    @Expose
    public static HashMap<String, ArrayList<Location>> locations = new java.util.HashMap<>();

    private void addToShard(Location loc, String shard) {
        if (Objects.equals(shard, "unknown")) return;
        ArrayList<Location> oldLocs = locations.get(shard);
        ArrayList<Location> locs = oldLocs == null ? new ArrayList<>() : oldLocs;
        locs.add(loc);
        locations.put(shard, locs);
    }

    private void resetLocs() {
        locations.clear();
    }

    private void addToShard(Location[] locs, String shard) {
        for (Location loc : locs) {
            addToShard(loc, shard);
        }
    }

    //endregion

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();
    private static final String CACHE_FILE_PATH = "monumenta/locations.json";

    private static final TypeToken<HashMap<String, ArrayList<String>>> typeToken = new TypeToken<HashMap<String, ArrayList<String>>>() {
    };

    public void load() {
        File file = FabricLoader.getInstance().getConfigDir().resolve(CACHE_FILE_PATH).toFile();
        if (!file.exists()) {
            newLocData();
            return;
        }
        ;

        try (FileReader reader = new FileReader(file)) {
            HashMap<String, ArrayList<Location>> loadedLocs = GSON.fromJson(reader, typeToken.getType());

            if (loadedLocs == null) {
                newLocData();
            } else {
                locations = loadedLocs;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        File file = FabricLoader.getInstance().getConfigDir().resolve(CACHE_FILE_PATH).toFile();

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(GSON.toJson(locations));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDisconnect() {
        save();
    }

    private void newLocData() {
        locations.clear();

        addToShard(
                new Location[]{
                        new Location(
                                701, -32, 777, 56, "Kaul Arena"
                        ),
                        new Location(
                                -497, -282, -1069, 343, "Sierhaven"
                        ),
                        new Location(
                                -78, -166, -180, 29, "Nyr"
                        ),
                        new Location(
                                658, 100, 538, 229, "Farr"
                        ),
                        new Location(
                                1319, -271, 1259, 180, "Highwatch Monument"
                        ),
                        new Location(
                                1319, -271, 1115, -62, "Highwatch"
                        ),
                        new Location(
                                765, 421, 642, 513, "Lowtide"
                        ),
                        new Location(
                                642, 513, 557, 569, "Lowtide"
                        ),
                        new Location(
                                -1548, -18, -1685, 165, "Oceangate"
                        ),
                        new Location(
                                520, -400, 380, -340, "Ta\u0027eldim"
                        ),
                        new Location(
                                1340, -141, 1283, -99, "Azacor Lobby"
                        ),
                        new Location(
                                1645, -596, -1733, 569, "Overworld"
                        )
                },
                "VALLEY"
        );

        addToShard(
                new Location[]{
                        new Location(
                                -762, 931, -539, 1210, "Player market"
                        )
                },
                "PLOTS"
        );

        addToShard(
                new Location[]{
                        new Location(
                                -632, 1218, -871, 1487, "Mistport"
                        ),
                        new Location(
                                -92, 397, -209, 502, "Rahkeri"
                        ),
                        new Location(
                                460, 640, 289, 865, "Alnera"
                        ),
                        new Location(
                                130, -107, -16, 48, "Hekawt Arena"
                        ),
                        new Location(
                                316, 2, 133, 191, "Molta"
                        ),
                        new Location(
                                -1415, 72, -1523, 246, "Eldrask Arena"
                        ),
                        new Location(
                                -1332, 528, -1371, 551, "Nightroost"
                        ),
                        new Location(
                                -1241, 444, -1362, 527, "Nightroost"
                        ),
                        new Location(
                                -1418, 871, -1640, 1086, "Frostgate"
                        ),
                        new Location(
                                -1677, -135, -1855, 36, "Wispervale"
                        ),
                        new Location(
                                -671, -202, -755, -139, "Breachpoint"
                        ),
                        new Location(
                                -511, -548, -545, -514, "Steelmeld Monument"
                        ),
                        new Location(
                                -493, -563, -676, -424, "Steelmeld"
                        ),
                        new Location(
                                -1155, -569, -1273, -445, "Headless Horseman"
                        ),
                        new Location(
                                -412, 1506, -519, 1615, "The Floating Carnival"
                        ),
                        new Location(
                                -64, 3248, -223, 3375, "The Black Mist"
                        ),
                        new Location(
                                292, 3367, 225, 3447, "Sealed Remorse"
                        ),
                        new Location(
                                -1394, -1342, -1450, -1275, "Darkest Depths"
                        ),
                        new Location(
                                862, -654, -2222, 1902, "Overworld"
                        )
                },
                "ISLES"
        );

        //Empty for now as I do not know important positions in r3 :)
        addToShard(
                new Location[]{

                },
                "RING"
        );
        save();
    }

    private ArrayList<Location> getLocs(String shard) {
        if (shard.matches("\\w*-[1-3]")) {
            shard = shard.substring(0, shard.length() - 2);
        }
        shard = shard.toUpperCase();

        return locations.get(shard);
    }

    public String getLocation(double X, double Z, String shard) {
        ArrayList<Location> locations = getLocs(shard);
        if (locations == null) {
            return shard;
        }

        for (Location loc : locations) {
            if (loc.isInBounds(X, Z)) return loc.name;
        }

        return shard;
    }

    public static class Location {
        @Expose
        int east;
        @Expose
        int north;
        @Expose
        int west;
        @Expose
        int south;

        @Expose
        String name;

        public Location(int east, int north, int west, int south, String name) {
            this.east = east;
            this.north = north;
            this.west = west;
            this.south = south;
            this.name = name;
        }

        public boolean isInBounds(double playerX, double playerZ) {
            if ((playerX >= east && playerX <= west) || (playerX <= east && playerX >= west)) {
                return (playerZ >= north && playerZ <= south) || (playerZ <= north && playerZ >= south);
            }
            return false;
        }
    }

}
