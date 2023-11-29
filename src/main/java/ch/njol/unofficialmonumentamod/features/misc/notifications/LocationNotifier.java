package ch.njol.unofficialmonumentamod.features.misc.notifications;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import ch.njol.unofficialmonumentamod.features.misc.managers.MessageNotifier;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class LocationNotifier {
	private static Double lastX;
	private static Double lastZ;

	public static void onDisconnect() {
		lastX = null;
		lastZ = null;
	}

	private static MessageNotifier.RenderedMessage lastNotification = null;

	public static void tick() {
		if (!UnofficialMonumentaModClient.options.notifyLocation) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();

		String shard = Locations.getShortShard();
		if (shard.equals("unknown") || client.player == null) {
			return;
		}

		MessageNotifier.RenderedMessage notification = null;

		String loc = UnofficialMonumentaModClient.locations.getLocation(client.player.getX(), client.player.getZ(), shard);
		if (lastX != null && lastZ != null) {
			String lastLoc = UnofficialMonumentaModClient.locations.getLocation(lastX, lastZ, shard);
			if (!Objects.equals(loc, shard) && !Objects.equals(loc, lastLoc) && !Objects.equals(loc, "Overworld")) {
				MutableText notificationText = Text.literal("Entering ")
								.setStyle(Style.EMPTY.withColor(Formatting.GREEN))
						.append(Text.literal(loc)
								.setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
				notification = new MessageNotifier.RenderedMessage(notificationText, 1.5F);
			} else if (Objects.equals(loc, shard) || Objects.equals(loc, "Overworld")) {
				MutableText notificationText = Text.literal("Leaving ")
								.setStyle(Style.EMPTY.withColor(Formatting.RED))
						.append(Text.literal(lastLoc)
								.setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
				notification = new MessageNotifier.RenderedMessage(notificationText, 1.5F);
				if ((lastNotification != null && lastNotification.equals(notification)) || Objects.equals(lastLoc, shard) || Objects.equals(lastLoc, "Overworld")
				) {
					return;
				}
			}
		} else if (!Objects.equals(shard, loc) && !Objects.equals(loc, "Overworld")) {
			MutableText notificationText = Text.literal("Entering ")
					.setStyle(Style.EMPTY.withColor(Formatting.GREEN))
					.append(Text.literal(loc)
							.setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
			notification = new MessageNotifier.RenderedMessage(notificationText, 1.5F);
		}
		
		if (notification != null) {
			MessageNotifier.getInstance().addMessageToQueue(notification);
			lastNotification = notification;
		}

		lastX = client.player.getX();
		lastZ = client.player.getZ();
	}
}
