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

import static java.lang.Integer.parseInt;

public class Locations {
    //region utilities
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Pattern shardGetterPattern = Pattern.compile(".*<(?<shard>\\w*)>.*");

    public static String getShard() {
        Text header = ((PlayerListHudAccessor) mc.inGameHud.getPlayerListHud()).getHeader();
        String shard = "unknown";
        if (header != null) {
            String text = header.getString();
            Matcher matcher = shardGetterPattern.matcher(text);
            if (matcher.matches()) {
                shard = matcher.group("shard");
            }
        }
        return shard;
    }

    public static String getShortShard() {
        String shard = getShard();
        if (shard.matches("\\w-[0-9]")) {
            shard = shard.substring(0, shard.length()-2);
        }

        return shard;
    }
    //endregion

    //region locations
    private static final Pattern LocValidator = Pattern.compile("\\((?<X1>-*[0-9]*):(?<Z1>-?[0-9]*)\\)\\((?<X2>-?[0-9]*):(?<Z2>-?[0-9]*)\\)/(?<name>.*)");
    @Expose
    public static HashMap<String, ArrayList<String>> locations = new java.util.HashMap<>();

    private void addToShard(String loc, String shard) {
        if (Objects.equals(shard, "unknown") || !LocValidator.matcher(loc).matches()) return;
        ArrayList<String> oldLocs = locations.get(shard);
        ArrayList<String> locs = oldLocs == null ? new ArrayList<>() : oldLocs;
        locs.add(loc);
        locations.put(shard, locs);
    }

    private void resetLocs() {
        locations.clear();
    }

    private void addToShard(String[] locs, String shard) {
        for (String loc: locs) {
            addToShard(loc, shard);
        }
    }

    //endregion

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();
    private static final String CACHE_FILE_PATH = "monumenta/unofficial-monumenta-mod-locations.json";

    private static final TypeToken<HashMap<String, ArrayList<String>>> typeToken = new TypeToken<HashMap<String, ArrayList<String>>>(){};

    public void load() {
        File file = FabricLoader.getInstance().getConfigDir().resolve(CACHE_FILE_PATH).toFile();
        if (!file.exists()) {
            newLocData();
            return;
        };

        try (FileReader reader = new FileReader(file)) {
            HashMap<String, ArrayList<String>> loadedLocs = GSON.fromJson(reader, typeToken.getType());

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
            System.out.println(locations);
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
                new String[]{
                        "(701:-32)(777:56)/Kaul Arena",
                        "(-497:-282)(-1069:343)/Sierhaven",
                        "(-78:-166)(-180:29)/Nyr",
                        "(658:100)(538:229)/Farr",
                        "(1319:-271)(1259:180)/Highwatch Monument",
                        "(1319:-271)(1115:-62)/Highwatch",
                        "(765:421)(642:513)/Lowtide",
                        "(642:513)(557:569)/Lowtide",
                        "(-1548:-18)(-1685:165)/Oceangate",
                        "(520:-400)(380:-340)/Ta\u0027eldim",
                        "(1340:-141)(1283:-99)/Azacor Lobby",
                        "(1645:-596)(-1733:569)/Overworld"
        },
                "VALLEY"
        );

        addToShard(
                new String[]{
                        "(-762:931)(-539:1210)/Player Market"
                },
                "PLOTS"
        );

        addToShard(
                new String[]{
                        "(-632:1218)(-871:1487)/Mistport",
                        "(-92:397)(-209:502)/Rahkeri",
                        "(460:640)(289:865)/Alnera",
                        "(130:-107)(-16:48)/Hekawt Arena",
                        "(316:2)(133:191)/Molta",
                        "(-1415:72)(-1523:246)/Eldrask Arena",
                        "(-1332:528)(-1371:551)/Nightroost",
                        "(-1241:444)(-1362:527)/Nightroost",
                        "(-1418:871)(-1640:1086)/Frostgate",
                        "(-1677:-135)(-1855:36)/Wispervale",
                        "(-671:-202)(-755:-139)/Breachpoint",
                        "(-511:-548)(-545:-514)/Steelmeld Monument",
                        "(-493:-563)(-676:-424)/Steelmeld",
                        "(-1155:-569)(-1273:-445)/Headless Horseman",
                        "(-412:1506)(-519:1615)/The Floating Carnival",
                        "(-64:3248)(-223:3375)/The Black Mist",
                        "(292:3367)(225:3447)/Sealed Remorse",
                        "(-1394:-1342)(-1450:-1275)/Darkest Depths",
                        "(862:-654)(-2222:1902)/Overworld"
                },
                "ISLES"
        );

        //Empty for now as I do not know important positions in r3 :)
        addToShard(
                new String[]{

            },
                "RING"
        );
        save();
    }

    private ArrayList<String> getLocs(String shard) {
        if (shard.matches("\\w*-[1-3]")) {
            shard = shard.substring(0, shard.length() - 2);
        }
        shard = shard.toUpperCase();

        return locations.get(shard);
    }

    public String getLocation(double X, double Z, String shard) {
        ArrayList<String> locations = getLocs(shard);
        if (locations == null) {
            return shard;
        }

        try {
            for (String location : locations) {
                Matcher matcher = LocValidator.matcher(location);
                if (!matcher.matches()) return shard;

                int X1 = parseInt(matcher.group("X1"));
                int Z1 = parseInt(matcher.group("Z1"));

                int X2 = parseInt(matcher.group("X2"));
                int Z2 = parseInt(matcher.group("Z2"));

                String locName = matcher.group("name");

                if ((X >= X1 && X <= X2) || (X <= X1 && X >= X2)) {
                    //X is between X1 and X2

                    if ((Z >= Z1 && Z <= Z2) || (Z <= Z1 && Z >= Z2)) {
                        //Z is between Z1 and Z2 (coordinates are between the two limits)

                        return locName;
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return shard;// probably ran into a NumberFormatException
        }

        return shard;
    }


}
