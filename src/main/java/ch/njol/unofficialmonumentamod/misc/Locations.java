package ch.njol.unofficialmonumentamod.misc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.mixins.PlayerListHudAccessor;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.reflect.Field;

import static ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient.writeJsonFile;
import static ch.njol.unofficialmonumentamod.Utils.getUrl;
import static java.lang.Integer.parseInt;

public class Locations {


    //add shards with locations here
    public ArrayList<String> VALLEY;
    public ArrayList<String> PLOTS;
    public ArrayList<String> ISLES;

    private static final String CACHE_FILE_PATH = "monumenta/unofficial-monumenta-mod-locations.json";
    private String update_commit;
    private static final String UPDATE_GIST_URL = "https://api.github.com/gists/4b1602b907da62a9cca6f135fd334737";//put new locations in that gist

    public static class locationFile {
        //add the shards here too
        public String[] VALLEY;
        public String[] PLOTS;
        public String[] ISLES;

        public String update_commit;
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

        try {
            Text header = ((PlayerListHudAccessor) mc.inGameHud.getPlayerListWidget()).getHeader();

            String shard = null;

            for (Text text : header.getSiblings()) {
                if (text.getString().matches("<.*>")) {
                    //player shard
                    shard = text.getString().substring(1, text.getString().length() - 1);
                }
            }
            return shard;
        }catch (NullPointerException ignored) {};

        return null;
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
        try {
            ArrayList<String> location = getLocations(shard.toUpperCase());
            if (location != null) {//shard's list exist
                for (String addition: additions) {
                    if (!addition.matches("\\((?<X1>-*[0-9]*):(?<Z1>-?[0-9]*)\\)\\((?<X2>-?[0-9]*):(?<Z2>-?[0-9]*)\\)/(?<name>.*)")) continue;//only adds correctly made locations
                    location.add(addition);
                };
                this.getClass().getField(shard.toUpperCase()).set(this, location);
            };
        } catch (Exception ignored) {}
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

    private void update() {
        try {
            URL url = new URL(UPDATE_GIST_URL);
            String content = getUrl(url);

            JsonParser jsonParser = new JsonParser();

            JsonObject json =  jsonParser.parse(content).getAsJsonObject();

            JsonObject jsonContent = jsonParser.parse(json.get("files").getAsJsonObject().get("Locations.json").getAsJsonObject().get("content").getAsString()).getAsJsonObject();
            loadJson(jsonContent.toString());

            this.update_commit = jsonParser.parse(getUrl(new URL(UPDATE_GIST_URL + "/commits"))).getAsJsonArray().get(0).getAsJsonObject().get("version").getAsString();

        }catch (Exception e) {
            e.printStackTrace();
        }

        writeJsonFile(this, CACHE_FILE_PATH);
    }

    public void load() {
        try {
            String cache = Utils.readFile(CACHE_FILE_PATH);

            if (UnofficialMonumentaModClient.options.locationUpdate) {
                JsonParser jsonParser = new JsonParser();
                String remoteVersion = jsonParser.parse(getUrl(new URL(UPDATE_GIST_URL + "/commits"))).getAsJsonArray().get(0).getAsJsonObject().get("version").getAsString();
                String localVersion = jsonParser.parse(cache).getAsJsonObject().get("update_commit").getAsString();
                if (!Objects.equals(remoteVersion, localVersion)) {
                    UnofficialMonumentaModClient.LOGGER.info(String.format("Found new update for location file %s -> %s", localVersion, remoteVersion));
                    update();
                    return;
                }
            }

            loadJson(cache);

        } catch (FileNotFoundException | NoSuchFileException e) {
            // file doesn't exist,
           if (UnofficialMonumentaModClient.options.locationUpdate) update();
           else writeJsonFile(this, CACHE_FILE_PATH);//create with empty values

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

    private ArrayList<String> getLocations(String shard) throws IllegalAccessException {
        if (shard.matches(".*-[1-3]")) shard = shard.substring(0, shard.length() - 2);
        for (Field f: this.getClass().getFields()) {
            if (f.getName().equals(shard.toUpperCase()) && f.getType().getTypeName().equals(this.VALLEY.getClass().getTypeName()))
                    return (ArrayList<String>) f.get(this);
        }

        return null;
    }

    public String getLocation(double X, double Z, String shard) {
        try {
            if (shard.matches(".*-[1-3]")) shard = shard.substring(0, shard.length() - 2);
            ArrayList<String> locations = getLocations(shard);
            if (Objects.isNull(locations)) return shard;

            for (String location : locations) {
                if (location.matches("\\((?<X1>-*[0-9]*):(?<Z1>-?[0-9]*)\\)\\((?<X2>-?[0-9]*):(?<Z2>-?[0-9]*)\\)/(?<name>.*)")) {//skips badly made locations
                    Pattern locationTest = Pattern.compile("\\((?<X1>-*[0-9]*):(?<Z1>-?[0-9]*)\\)\\((?<X2>-?[0-9]*):(?<Z2>-?[0-9]*)\\)/(?<name>.*)");
                    Matcher matcher = locationTest.matcher(location);
                    matcher.matches();//runs the pattern

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
            }

        } catch (Exception e) {
            e.printStackTrace();

            return shard;//doesn't exist in lists
        }

        return shard;//didn't find coordinates in existing lists
    }


}
