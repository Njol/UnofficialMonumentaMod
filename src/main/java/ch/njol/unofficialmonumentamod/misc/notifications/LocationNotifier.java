package ch.njol.unofficialmonumentamod.misc.notifications;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.misc.NotificationToast;
import ch.njol.unofficialmonumentamod.misc.Locations;
import ch.njol.unofficialmonumentamod.misc.managers.Notifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Objects;

public class LocationNotifier {
    private static Double lastX;
    private static Double lastZ;

    public static void onDisconnect() {
        lastX = 0.00;
        lastZ = 0.00;
    }

    public static void tick() {
        if (!UnofficialMonumentaModClient.options.notifyLocation) return;
        MinecraftClient client = MinecraftClient.getInstance();


        String shard = Locations.getShortShard();
        if (shard == null) return;

        assert client.player != null;

        String loc = UnofficialMonumentaModClient.locations.getLocation(client.player.getX(), client.player.getZ(), shard);

        if (lastX != null && lastZ != null) {
            String lastLoc = UnofficialMonumentaModClient.locations.getLocation(lastX, lastZ, shard);

            if (!Objects.equals(loc, shard) && !Objects.equals(loc, lastLoc) && !Objects.equals(loc, "Overworld")) {//TODO There seems to be a bug when you enter in a non-registered area, it won't load notifications.
                NotificationToast toast = new NotificationToast(Text.of("Entering Area"), Text.of("Entering " + loc), Notifier.getMillisHideTime());
                Notifier.addCustomToast(toast);
            } else if (Objects.equals(loc, shard) || Objects.equals(loc, "Overworld")) {
                NotificationToast toast = new NotificationToast(Text.of("Leaving Area"), Text.of("Leaving " + lastLoc), Notifier.getMillisHideTime());
                if ((Notifier.getLastToast() != null && Arrays.equals(Notifier.getLastToast().getDescription() != null ? Notifier.getLastToast().getDescription().toArray() : new Object[0], Objects.requireNonNull(toast.getDescription()).toArray())) || Objects.equals(lastLoc, shard) || Objects.equals(lastLoc, "Overworld")) return;
                Notifier.addCustomToast(toast);
            }
        } else if (!Objects.equals(shard, loc) && !Objects.equals(loc, "Overworld")) {
            NotificationToast toast = new NotificationToast(Text.of("Entering Area"), Text.of("Entering " + loc), Notifier.getMillisHideTime());
            Notifier.addCustomToast(toast);
        }
        lastX = client.player.getX();
        lastZ = client.player.getZ();
    }
}
