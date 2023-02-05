package ch.njol.unofficialmonumentamod.features.strike;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import ch.njol.unofficialmonumentamod.ChannelHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.shard.ShardData;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import java.awt.Rectangle;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class ChestCountOverlay extends HudElement {

	public static final ChestCountOverlay INSTANCE = new ChestCountOverlay();

	private static final int WIDTH = 64;
	private static final int HEIGHT = 24;

	private Integer currentCount; //get from actionbar
	private Integer totalChests; //take from loaded shards as a HashMap<shard, chestCount> (String, int)

	private final ItemStack CHEST = new ItemStack(Items.CHEST);

	public Integer getCurrentCount() {
		return currentCount;
	}
	public Integer getTotalChests() {
		return totalChests;
	}

	@Override
	protected void render(MatrixStack matrices, float tickDelta) {
		final TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		DrawableHelper.fill(matrices, 0, 0, WIDTH, HEIGHT, MinecraftClient.getInstance().options.getTextBackgroundColor(UnofficialMonumentaModClient.options.overlay_opacity));

		Rectangle dimension = getDimension();
		client.getItemRenderer().renderGuiItemIcon(CHEST, dimension.x + 4, dimension.y + (HEIGHT - 16) / 2);

		Text text;
		if (isInEditMode()) {
			text = Text.of("Chests");
		} else {
			text = Text.of("" + (totalChests != null && totalChests > 0 ? currentCount + "/" + totalChests : currentCount));
		}

		// if total is 0 or the current count is under the total then render in gold if equal or higher, then render in bright green
		int color = totalChests != null && totalChests > 0 && currentCount >= totalChests ? 0xFF1FD655 : 0xFFFCCD12;

		// center text
		int x = 20 + (WIDTH - 20) / 2 - tr.getWidth(text) / 2;
		int y = HEIGHT / 2 - tr.fontHeight / 2;

		tr.draw(matrices, text, x, y, color);
	}


	public void onActionbarReceived(Text text) {
		//first one is non-edited the second one is for edited by vlado's counter mod.
		if (text.getString().equals("+1 Chest added to lootroom.") || text.getString().matches("\u00a76\\+1 Chest \u00a7cadded to lootroom\\..*")) {
			currentCount++;

			if (currentCount > totalChests) {
				//means that the current max count is probably not correct
				MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(MutableText.of(Text.of(
						"Current max count seems incorrect.\nIf you haven't edited the count yourself, please report to the developer the new count: " + currentCount
				).getContent()).setStyle(Style.EMPTY.withColor(Formatting.DARK_RED).withBold(true)));
			}
		}
		//TODO handle miniboss added count.
	}

	public void onStrikeChestUpdatePacket(ChannelHandler.StrikeChestUpdatePacket packet) {
		totalChests = packet.newLimit;
		if (packet.count != null) {
			currentCount = packet.count;
		}
		ShardData.stopSearch();
	}

	public void onShardChange(String shardName) {
			totalChests = ShardData.getMaxChests(shardName); // if null then non strike, if 0 then strike but max is unknown, > 0 means it's known so then render the max
			currentCount = 0;
	}

	@Override
	protected boolean isEnabled() {
		return UnofficialMonumentaModClient.options.chestCount_enabled;
	}

	@Override
	protected boolean isVisible() {
		return totalChests != null;
	}

	@Override
	protected int getWidth() {
		return WIDTH;
	}

	@Override
	protected int getHeight() {
		return HEIGHT;
	}

	@Override
	protected ElementPosition getPosition() {
		return UnofficialMonumentaModClient.options.chestCount_position;
	}

	@Override
	protected int getZOffset() {
		return 0;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragging) {
			UnofficialMonumentaModClient.saveConfig();
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

}
