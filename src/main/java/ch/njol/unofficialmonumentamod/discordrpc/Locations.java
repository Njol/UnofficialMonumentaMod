package ch.njol.unofficialmonumentamod.discordrpc;

import ch.njol.unofficialmonumentamod.mixins.PlayerListHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.reflect.Field;

import static java.lang.Integer.parseInt;

public class Locations {

    public ArrayList<String> VALLEY;
    public ArrayList<String> PLOTS;
    public ArrayList<String> ISLES;


    public Locations() {
    }

    public static String getShard() {
        MinecraftClient mc = MinecraftClient.getInstance();

        Text header = ((PlayerListHudAccessor) mc.inGameHud.getPlayerListWidget()).getHeader();

        String shard = "unknown";

        for (Text text: header.getSiblings()) {
            if (text.getString().matches("<.*>")) {
                //player shard
                shard = text.getString().substring(1, text.getString().length()-1);
            }
        }

        return shard;
    }

    private void addToShard(String addition, String shard) {
        addToShard(new String[]{addition}, shard);
    }

    private void addToShard(String[] additions, String shard) {
        try {
            ArrayList<String> location = getLocations(shard.toUpperCase());
            if (location != null) {//shard's list exist
                for (String addition: additions) {
                    if (!addition.matches("\\((?<X1>-*[0-9]*):(?<Z1>-?[0-9]*)\\)\\((?<X2>-?[0-9]*):(?<Z2>-?[0-9]*)\\)\\/(?<name>.*)")) continue;//only add correctly made locations
                    location.add(addition);
                };
                this.getClass().getField(shard.toUpperCase()).set(this, location);
            };
        } catch (Exception ignored) {}
    }

    public void populate() {
        VALLEY = new ArrayList<>();
        PLOTS = new ArrayList<>();
        ISLES = new ArrayList<>();

        /*
          locations that will be included:
          cities (ex: Ta'eldim)
          World bosses (ex: Kaul Arena)
          Important locations (ex: Player Market)

           this method is probably gonna take the data from an internet hosted data sheet at one point or another

           (+X:-Z)(-X:+Z)/Name
         */
        addToShard("(-762:931)(-539:1210)/Player Market", "plots");

        addToShard(new String[]{
                "(701:-32)(777:56)/Kaul Arena",
                "(-497:-282)(-1069:343)/Sierhaven",
                "(-78:-166)(-180:29)/Nyr",
                "(658:100)(538:229)/Farr",
                "(1319:-271)(1259:180)/Highwatch Monument",
                "(1319:-271)(1115:-62)/Highwatch",//added 30:20 as overflow to East-North
                "(765:421)(642:513)/Lowtide",//added overflow 20:0 as overflow to East-North
                "(642:513)(557:569)/Lowtide",//lowtide gate (lowtide and gate have different borders)
                "(-1548:-18)(-1685:165)/Oceangate",
                "(520:-400)(380:-340)/Ta'eldim",
                //TODO add Azacor lobby / Azacor Arena coords
                "/Overworld"//TODO get the coordinates of the playable borders
        }, "valley");

        addToShard(new String[]{
                "(-632:1218)(-871:1487)/Mistport",
                "(-92:397)(-209:502)/Rahkeri",//not based on border
                "(460:640)(289:865)/Alnera",
                "(130:-107)(34:48)/Hekawt Arena",//should contain both the arena and the place where the npcs are
                "(316:2)(133:191)/Molta",
                "(-1415:72)(-1523:246)/Eldrask Arena",
                "(-1332:528)(-1371:551)/Nightroost",//around the drask tp
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
        }, "isles");
    }

    private ArrayList<String> getLocations(String shard) throws IllegalAccessException {
        for (Field f: this.getClass().getFields()) {
            if (f.getName().equals(shard.toUpperCase()) && f.getType().getTypeName().equals(this.VALLEY.getClass().getTypeName()))
                    return (ArrayList<String>) f.get(this);
        }

        return null;
    }

    public String getLocation(double X, double Z, String shard) {
        try {
            for (String location : Objects.requireNonNull(getLocations(shard))) {
                if (location.matches("\\((?<X1>-*[0-9]*):(?<Z1>-?[0-9]*)\\)\\((?<X2>-?[0-9]*):(?<Z2>-?[0-9]*)\\)\\/(?<name>.*)")) {//skips badly made locations
                    Pattern locationTest = Pattern.compile("\\((?<X1>-*[0-9]*):(?<Z1>-?[0-9]*)\\)\\((?<X2>-?[0-9]*):(?<Z2>-?[0-9]*)\\)\\/(?<name>.*)");
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

            return "Unknown";//doesn't exist in lists
        }

        return "Unknown";//didn't found coordinates in existing lists
    }


}
