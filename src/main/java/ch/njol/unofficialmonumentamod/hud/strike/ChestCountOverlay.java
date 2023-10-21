package ch.njol.unofficialmonumentamod.hud.strike;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import ch.njol.unofficialmonumentamod.ChannelHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.PersistentData;
import ch.njol.unofficialmonumentamod.core.shard.ShardData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChestCountOverlay extends HudElement {
	//TODO generalize this class to be able to create one for any actionbar based counter / maybe other sources?

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

		client.getItemRenderer().renderInGui(matrices, CHEST, 4, (HEIGHT - 16) / 2);

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
		if (text.getString().equals("+1 Chest added to lootroom.") || text.getString().matches("\u00a76\\+1 Chest \u00a7cadded to lootroom\\..*")) {
			addCount(1);
			return;
		}

		if (text.getString().equals("+5 Chests added to lootroom.") || text.getString().matches("\u00a76\\+5 Chests \u00a7cadded to lootroom\\..*")) {
			addCount(5);
		}
	}

	public void initializeListeners() {
		PersistentData.PersistentDataLoadedCallback.EVENT.register((persistentData) -> {
			if (persistentData.chestCount.value.shard.equals(ShardData.getCurrentShard().shortShard)) {
				currentCount = Integer.valueOf(persistentData.chestCount.value.value);
			}
		});
		PersistentData.PersistentDataSavingCallback.EVENT.register((persistentData) -> persistentData.chestCount = new PersistentData.DatedHolder<>(System.currentTimeMillis(), new PersistentData.ShardedHolder<>(ShardData.getCurrentShard().shortShard, currentCount != null ? currentCount.shortValue() : 0)));

		ShardData.ShardChangedEventCallback.EVENT.register((currentShard, lastShard) -> {
			if (!currentShard.equals(lastShard)) {
				onShardChange(currentShard.shortShard);
			}
		});
	}

	public void onStrikeChestUpdatePacket(ChannelHandler.StrikeChestUpdatePacket packet) {
		totalChests = packet.newLimit;
		if (packet.count != null) {
			currentCount = packet.count;
		}
		ShardData.stopSearch();
	}

	public void addCount(int num) {
		currentCount += num;

		if (currentCount > totalChests) {
			//means that the current max count is probably not correct
			MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(MutableText.of(Text.of(
					"[UMM] Current shard's max count seems incorrect.\nPlease report the new count to a Unofficial Monumenta Mod maintainer: " + currentCount + "\nYou can disable this message by clicking on it."
			).getContent()).setStyle(Style.EMPTY.withColor(Formatting.DARK_RED).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/umm disableChestCountError"))));
		}
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
