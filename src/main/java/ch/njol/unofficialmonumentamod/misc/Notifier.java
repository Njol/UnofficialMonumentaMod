package ch.njol.unofficialmonumentamod.misc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.Toast;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Objects;

public class Notifier {
    private static Double lastX;
    private static Double lastZ;

    private static CustomToast lastToast;

    private static ArrayList<CustomToast> queue = new ArrayList<>();

    private static long getMillisHideTime() {
        return Float.valueOf(UnofficialMonumentaModClient.options.notifierShowTime).longValue() * 1000;
    }

    public static void tick() {
        if (!UnofficialMonumentaModClient.options.notifyLocation) return;
        MinecraftClient client = MinecraftClient.getInstance();

        handleQueue();

        String shard = Locations.getShard();
        if (shard == null) return;

        assert client.player != null;

            String loc = UnofficialMonumentaModClient.locations.getLocation(client.player.getX(), client.player.getZ(), shard);

            if (lastX != null && lastZ != null) {
                if (!Objects.equals(loc, shard) && !Objects.equals(loc, UnofficialMonumentaModClient.locations.getLocation(lastX, lastZ, shard))) {
                    CustomToast toast = new CustomToast(Text.of("Entering Area"), Text.of("Entering " + loc), false, getMillisHideTime());
                    addCustomToast(toast);
                } else if (Objects.equals(loc, shard)) {
                    CustomToast toast = new CustomToast(Text.of("Leaving Area"), Text.of("Leaving " + UnofficialMonumentaModClient.locations.getLocation(lastX, lastZ, shard)), false, getMillisHideTime());
                    if (Objects.equals(lastToast.getDescription().getString(), toast.getDescription().getString()) || Objects.equals(UnofficialMonumentaModClient.locations.getLocation(lastX, lastZ, shard), shard)) return;
                    addCustomToast(toast);
                }
            } else {
                if (!Objects.equals(shard, loc)) {
                    CustomToast toast = new CustomToast(Text.of("Entering Area"), Text.of("Entering " + loc), false, getMillisHideTime());
                    addCustomToast(toast);
                }
            }
        lastX = client.player.getX();
        lastZ = client.player.getZ();
    }

    private static boolean lastToastActive() {
        if (lastToast == null) return false;
        return lastToast.getVisibility() == Toast.Visibility.SHOW;
    }

    private static void handleQueue() {
        queue.removeIf(toast -> toast.getVisibility() == Toast.Visibility.HIDE);
        if (queue.size() > 0) {
            if (!lastToastActive()) {
                queue.get(0).setHideTime(getMillisHideTime());
                lastToast = queue.get(0);
                MinecraftClient.getInstance().getToastManager().add(queue.get(0));
            }
        }
    }

    public static void addCustomToast(CustomToast toast) {
        boolean alreadyExists = false;
        for (CustomToast queueToast: queue) {
            try {
                if (Objects.equals(queueToast.getTitle().getString(), toast.getTitle().getString()) && Objects.equals(queueToast.getDescription().getString(), toast.getDescription().getString())) {
                    alreadyExists = true;
                    break;
                }
            }catch (Exception ignored) {}
        }
        if (!alreadyExists) queue.add(toast);
    }
}
