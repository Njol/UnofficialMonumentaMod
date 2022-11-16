package ch.njol.unofficialmonumentamod.features.strike;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.ShardData;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import java.awt.Rectangle;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class ChestCountOverlay extends HudElement {

	public static final ChestCountOverlay INSTANCE = new ChestCountOverlay();

	private static final int WIDTH = 64;
	private static final int HEIGHT = 24;

	private Integer currentCount; //get from actionbar
	private Integer totalChests; //take from Constants as a HashMap<shard, chestCount> (String, int)
	private String lastShard;

	private boolean searchingForShard = false;

	private final ItemStack CHEST = new ItemStack(Items.CHEST);

	@Override
	protected void render(MatrixStack matrices, float tickDelta) {
		final TextRenderer tr = MinecraftClient.getInstance().textRenderer;

		DrawableHelper.fill(matrices, 0, 0, WIDTH, HEIGHT, MinecraftClient.getInstance().options.getTextBackgroundColor(0.3f));

		Rectangle dimension = getDimension();
		client.getItemRenderer().renderGuiItemIcon(CHEST, dimension.x + 4, dimension.y + (HEIGHT - 16) / 2);

		Text text;
		if (isInEditMode()) {
			text = Text.of("Chests");
		} else {
			text = Text.of("" + (totalChests != null && totalChests > 0 ? currentCount + "/" + totalChests : currentCount));
		}

		// if total is 0 or the current count is under the total then render in gold else render in bright green
		int color = totalChests != null && totalChests > 0 && currentCount >= totalChests ? 0xFF1FD655 : 0xFFFCCD12;

		// center text
		int x = 20 + (WIDTH - 20) / 2 - tr.getWidth(text) / 2;
		int y = HEIGHT / 2 - tr.fontHeight / 2;

		tr.draw(matrices, text, x, y, color);
	}


	public void onActionbarReceived(Text text) {
		//first one is non-edited the second one is for edited by vlado's counter mod.
		if (text.getString().equals("+1 Chest added to lootroom.") || text.getString().matches("\u00a76+1 Chest \u00a7cadded to lootroom\\..*")) {
			currentCount++;
		}
	}

	public void onWorldLoad() {
		searchingForShard = true;
	}

	public void onPlayerListHeader(Text text) {
		if (!searchingForShard || Locations.getShardFrom(text) == null) {
			return;
		}
		String shard = Locations.getShortShard();
		if (Objects.equals(shard, "unknown")) {
			return;
		}
		if (!Objects.equals(shard, lastShard)) { // reset
			totalChests = ShardData.getMaxChests(shard); // if null then non strike, if 0 then strike but max is unknown, > 0 means it's known so then render the max
			currentCount = 0;
			lastShard = shard;
			searchingForShard = false;
		}
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
