package ch.njol.unofficialmonumentamod.misc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.misc.notifications.LocationNotifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.Toast;

import java.util.ArrayList;
import java.util.Objects;

public class Notifier {
    private static CustomToast lastToast;

    private static ArrayList<CustomToast> queue = new ArrayList<>();

    public static long getMillisHideTime() {
        return Float.valueOf(UnofficialMonumentaModClient.options.notifierShowTime).longValue() * 1000;
    }

    public static void tick() {
        handleQueue();
        LocationNotifier.tick();
    }

    public static CustomToast getLastToast() {
        return lastToast;
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

    private static boolean lastToastActive() {
        if (lastToast == null) return false;
        return lastToast.getVisibility() == Toast.Visibility.SHOW;
    }
}
