package ch.njol.unofficialmonumentamod.misc;

import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.mixins.PlayerListHudAccessor;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.reflect.Field;

import static ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient.writeJsonFile;
import static java.lang.Integer.parseInt;

public class Locations {
    //add shards with locations here
    public ArrayList<String> VALLEY;
    public ArrayList<String> PLOTS;
    public ArrayList<String> ISLES;

    private static final String CACHE_FILE_PATH = "monumenta/unofficial-monumenta-mod-locations.json";

    public static class locationFile {
        //add the shards here too
        public String[] VALLEY;
        public String[] PLOTS;
        public String[] ISLES;

    }

    public Locations() {
        for (Field f: this.getClass().getDeclaredFields()) {
            if (f.getType() != java.util.ArrayList.class) continue;
            try {
                f.set(this, new ArrayList<>());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getShard() {
        MinecraftClient mc = MinecraftClient.getInstance();
        Text header = ((PlayerListHudAccessor) mc.inGameHud.getPlayerListWidget()).getHeader();
        if (header == null) return null;

        String shard = null;

        for (Text text : header.getSiblings()) {
            if (text.getString().matches("<.*>")) {
                //player shard
                shard = text.getString().substring(1, text.getString().length() - 1);
            }
        }
        return shard;
    }

    public static String getShortShard() {
        String shard = getShard();
        if (shard == null) return null;
        if (shard.matches(".*-[1-3]")) shard = shard.substring(0, shard.length() - 2);

        return shard;
    }

    private void addToShard(String addition, String shard) {
        addToShard(new String[]{addition}, shard);
    }

    private void resetLocations() {
        VALLEY.clear();
        ISLES.clear();
        PLOTS.clear();
    }

    private void addToShard(String[] additions, String shard) {
            ArrayList<String> location = getLocations(shard.toUpperCase());
            if (location != null) {//shard's list exist
                for (String addition: additions) {
                    if (!addition.matches("\\((?<X1>-*[0-9]*):(?<Z1>-?[0-9]*)\\)\\((?<X2>-?[0-9]*):(?<Z2>-?[0-9]*)\\)/(?<name>.*)")) continue;//only adds correctly made locations
                    location.add(addition);
                }
                try {
                    this.getClass().getField(shard.toUpperCase()).set(this, location);
                } catch (NoSuchFieldException | IllegalAccessException ignored) {}
            }
    }

    private void loadJson(String jsonString) {
        JsonParser jsonParser = new JsonParser();

        JsonObject json = jsonParser.parse(jsonString).getAsJsonObject();
        Gson gson = new Gson();
        Type type = new TypeToken<String[]>(){}.getType();

        resetLocations();
        for (Map.Entry<String, JsonElement> entry: json.entrySet()) {
            if (Objects.equals(entry.getKey(), "update_commit")) continue;

            addToShard((String[]) gson.fromJson(entry.getValue(), type), entry.getKey());
        }
    }

    private void CreateNewLocationFile() {
        VALLEY.addAll(Arrays.asList(
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
        ));

        PLOTS.add(
                "(-762:931)(-539:1210)/Player Market"
        );

        ISLES.addAll(Arrays.asList(
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
        ));

        writeJsonFile(this, CACHE_FILE_PATH);
    }

    public void load() {
        try {
            String cache = Utils.readFile(CACHE_FILE_PATH);

            loadJson(cache);

            //remove badly made locations
            Validate("valley");
            Validate("plots");
            Validate("isles");
        } catch (FileNotFoundException | NoSuchFileException e) {
            // file doesn't exist
            resetLocations();
            CreateNewLocationFile();

        } catch (IOException | JsonParseException e) {
            e.printStackTrace();
        }
        /*
          locations that will be included:
          cities (ex: Ta'eldim)
          World bosses (ex: Kaul Arena)
          Important locations (ex: Player Market)

           (+X:-Z)(-X:+Z)/Name
         */
    }

    private ArrayList<String> getLocations(String shard) {
        try {
            if (shard.matches(".*-[1-3]")) shard = shard.substring(0, shard.length() - 2);
            for (Field f : this.getClass().getFields()) {
                if (f.getName().equals(shard.toUpperCase()) && f.getType().getTypeName().equals(this.VALLEY.getClass().getTypeName()))
                    return (ArrayList<String>) f.get(this);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void Validate(String shard) {
        try {
            ArrayList<String> locations = getLocations(shard);
            if (Objects.isNull(locations)) return;
            locations.removeIf(location -> !location.matches("\\((?<X1>-*[0-9]*):(?<Z1>-?[0-9]*)\\)\\((?<X2>-?[0-9]*):(?<Z2>-?[0-9]*)\\)/(?<name>.*)"));
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getLocation(double X, double Z, String shard) {
        try {
            if (shard.matches(".*-[1-3]")) shard = shard.substring(0, shard.length() - 2);
            ArrayList<String> locations = getLocations(shard);
            if (Objects.isNull(locations)) return shard;

            for (String location : locations) {
                    Pattern locationTest = Pattern.compile("\\((?<X1>-*[0-9]*):(?<Z1>-?[0-9]*)\\)\\((?<X2>-?[0-9]*):(?<Z2>-?[0-9]*)\\)/(?<name>.*)");
                    Matcher matcher = locationTest.matcher(location);
                    if (!matcher.matches()) return shard;

                    int X1 = parseInt(matcher.group("X1"));
                    int Z1 = parseInt(matcher.group("Z1"));

                    int X2 = parseInt(matcher.group("X2"));
                    int Z2 = parseInt(matcher.group("Z2"));

                    String locationName = matcher.group("name");

                    if ((X >= X1 && X <= X2) || (X <= X1 && X >= X2)) {
                        //X is between X1 and X2

                        if ((Z >= Z1 && Z <= Z2) || (Z <= Z1 && Z >= Z2)) {
                            //Z is between Z1 and Z2 (coordinates are between the two limits)

                            return locationName;
                        }
                    }
            }

        } catch (Exception e) {
            e.printStackTrace();

            return shard;//doesn't exist in lists
        }

        return shard;//didn't find coordinates in existing lists
    }


}
