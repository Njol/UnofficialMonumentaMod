package ch.njol.unofficialmonumentamod.misc.notifications;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.misc.CustomToast;
import ch.njol.unofficialmonumentamod.misc.Locations;
import ch.njol.unofficialmonumentamod.misc.Notifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Objects;

public class LocationNotifier {
    private static Double lastX;
    private static Double lastZ;

    public static void tick() {
        if (!UnofficialMonumentaModClient.options.notifyLocation) return;
        MinecraftClient client = MinecraftClient.getInstance();


        String shard = Locations.getShortShard();
        if (shard == null) return;

        assert client.player != null;

        String loc = UnofficialMonumentaModClient.locations.getLocation(client.player.getX(), client.player.getZ(), shard);

        if (lastX != null && lastZ != null) {
            if (!Objects.equals(loc, shard) && !Objects.equals(loc, UnofficialMonumentaModClient.locations.getLocation(lastX, lastZ, shard))) {
                CustomToast toast = new CustomToast(Text.of("Entering Area"), Text.of("Entering " + loc), false, Notifier.getMillisHideTime());
                Notifier.addCustomToast(toast);
            } else if (Objects.equals(loc, shard)) {
                CustomToast toast = new CustomToast(Text.of("Leaving Area"), Text.of("Leaving " + UnofficialMonumentaModClient.locations.getLocation(lastX, lastZ, shard)), false, Notifier.getMillisHideTime());
                if ((Notifier.getLastToast() != null && Objects.equals(Notifier.getLastToast().getDescription().getString(), toast.getDescription().getString())) || Objects.equals(UnofficialMonumentaModClient.locations.getLocation(lastX, lastZ, shard), shard)) return;
                Notifier.addCustomToast(toast);
            }
        } else {
            if (!Objects.equals(shard, loc)) {
                CustomToast toast = new CustomToast(Text.of("Entering Area"), Text.of("Entering " + loc), false, Notifier.getMillisHideTime());
                Notifier.addCustomToast(toast);
            }
        }
        lastX = client.player.getX();
        lastZ = client.player.getZ();
    }
}
