package ch.njol.unofficialmonumentamod.features.discordrpc;

import java.util.HashMap;

public class Constants {
    private static final HashMap<String, String> ShardToOfficialNames = new HashMap<>();;

    public static String getOffName(String shard) {
        if (ShardToOfficialNames.containsKey(shard)) {
            return ShardToOfficialNames.get(shard);
        }
        return null;
    }

    static {
        //main zones
        ShardToOfficialNames.put(
                "valley", "King's Valley"
        );
        ShardToOfficialNames.put(
                "isles", "Celsian Isles"
        );
        ShardToOfficialNames.put(
                "ring", "Architect's Ring"
        );

        ShardToOfficialNames.put(
                "plots", "Plots"
        );
        ShardToOfficialNames.put(
                "playerplots", "Player plot"
        );


        //dungeons / special areas
        //R1
        ShardToOfficialNames.put(
                "tutorial", "No spoilers smh"
        );
        ShardToOfficialNames.put(
                "labs", "The Alchemy Labs"
        );
        ShardToOfficialNames.put(
                "white", "Halls of Wind And Blood"
        );
        ShardToOfficialNames.put(
                "orange", "Fallen Menagerie"
        );
        ShardToOfficialNames.put(
                "magenta", "Plagueroot Temple"
        );
        ShardToOfficialNames.put(
                "lightblue", "Arcane Rivalry"
        );
        ShardToOfficialNames.put(
                "yellow", "Vernal Nightmare"
        );
        ShardToOfficialNames.put(
                "willows", "The Black Willows"
        );
        ShardToOfficialNames.put(
                "corridors", "Ephemeral Corridors"
        );
        ShardToOfficialNames.put(
                "reverie", "Malevolent Reverie"
        );
        ShardToOfficialNames.put(
                "verdant", "Verdant Remnants"
        );
        ShardToOfficialNames.put(
                "sanctum", "Forsworn Sanctum"
        );


        //R2
        ShardToOfficialNames.put(
                "lime", "Salazar's Folly"
        );
        ShardToOfficialNames.put(
                "pink", "Harmonic Arboretum"
        );
        ShardToOfficialNames.put(
                "gray", "Valley of the Forgotten Pharaohs"
        );
        ShardToOfficialNames.put(
                "lightgray", "Palace Of Mirrors"
        );
        ShardToOfficialNames.put(
                "cyan", "Scourge Of Lunacy"
        );
        ShardToOfficialNames.put(
                "purple", "Grasp Of Avarice"
        );
        ShardToOfficialNames.put(
                "teal", "Echoes Of Oblivion"
        );
        ShardToOfficialNames.put(
                "shiftingcity", "City of Shifting Waters"
        );
        ShardToOfficialNames.put(
                "forum", "The Fallen Forum"
        );
        ShardToOfficialNames.put(
                "depths", "Darkest Depths"
        );
        ShardToOfficialNames.put(
                "mist", "The Black Mist"
        );
        ShardToOfficialNames.put(
                "remorse", "The Sealed Remorse"
        );
        ShardToOfficialNames.put(
                "rush", "Rush of Dissonance"
        );
    }
}
