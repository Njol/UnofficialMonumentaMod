package ch.njol.unofficialmonumentamod.features.misc.managers;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.features.misc.NotificationToast;
import ch.njol.unofficialmonumentamod.features.misc.notifications.LocationNotifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.Toast;

public class Notifier {
	private static NotificationToast lastToast;

	private static final ArrayList<NotificationToast> queue = new ArrayList<>();

	public static long getMillisHideTime() {
		return Float.valueOf(UnofficialMonumentaModClient.options.notifierShowTime).longValue() * 1000;
	}

	public static void tick() {
		handleQueue();
		LocationNotifier.tick();
	}

	public static void onDisconnect() {
		queue.clear();
	}

	public static NotificationToast getLastToast() {
		return lastToast;
	}

	public static void addCustomToast(NotificationToast toast) {
		boolean alreadyExists = false;
		for (NotificationToast queueToast : queue) {
			try {
				if (queueToast.getDescription() == null || toast.getDescription() == null) {
					continue;
				}
				if (Objects.equals(queueToast.getTitle().getString(), toast.getTitle().getString()) && new HashSet<>(queueToast.getDescription()).containsAll(toast.getDescription())) {
					alreadyExists = true;
					break;
				}
			} catch (Exception ignored) {
			}
		}
		if (!alreadyExists) {
			queue.add(toast);
		}
	}

	private static void handleQueue() {
		queue.removeIf(toast -> toast.getVisibility() == Toast.Visibility.HIDE);
		if (!queue.isEmpty()) {
			if (!lastToastActive()) {
				queue.get(0).setHideTime(getMillisHideTime());
				lastToast = queue.get(0);
				MinecraftClient.getInstance().getToastManager().add(queue.get(0));
			}
		}
	}

	private static boolean lastToastActive() {
		if (lastToast == null) {
			return false;
		}
		return lastToast.getVisibility() == Toast.Visibility.SHOW;
	}
}
