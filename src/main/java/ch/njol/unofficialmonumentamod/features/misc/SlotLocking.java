package ch.njol.unofficialmonumentamod.features.misc;

import ch.njol.minecraft.uiframework.ModSpriteAtlasHolder;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.mixins.KeyBindingAccessor;
import ch.njol.unofficialmonumentamod.mixins.screen.HandledScreenAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import static ch.njol.minecraft.uiframework.hud.HudElement.drawSprite;

public class SlotLocking {
	// This feature is largely based on https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/src/main/java/io/github/moulberry/notenoughupdates/miscfeatures/SlotLocking.java per the LGPL 3.0 license
	private static final String CACHE_PATH = "monumenta/lockedSlots.json";
	
	private static final SlotLocking INSTANCE = new SlotLocking();
	private static final LockedSlot DEFAULT_LOCKED_SLOT = new LockedSlot();
	
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	public static SlotLocking getInstance() {
		return INSTANCE;
	}
	
	public static class LockedSlot {
		//fully locked (will stop most actions that would change the content of that slot)
		public boolean locked = false;
		//Left click in inventory
		public boolean lockHalfPickup = false;
		//Right click in inventory
		public boolean lockPickup = false;
		//Drop, both in inventory and outside of one
		public boolean lockDrop = false;
		
		public void switchLock(LockType type) {
			if (type != LockType.ALL && locked) {
				return;
			}
			
			switch (type) {
				case HALF_PICKUP -> lockHalfPickup = !lockHalfPickup;
				case PICKUP -> lockPickup = !lockPickup;
				case DROP -> lockDrop = !lockDrop;
				case ALL -> {
					locked = !locked;
					lockDrop = locked;
					lockHalfPickup = locked;
					lockPickup = locked;
				}
			}
		}
	}
	
	public enum LockType {
		HALF_PICKUP(),
		PICKUP(),
		DROP(),
		ALL()
	}
	
	public static class SlotLockData {
		public LockedSlot[] lockedSlots = new LockedSlot[41];
	}
	
	private static ModSpriteAtlasHolder atlas;
	
	private static Identifier LOCK;
	private static Identifier LEFT_CLICK_LOCK;
	private static Identifier RIGHT_CLICK_LOCK;
	private static Identifier DROP_LOCK;
	
	private static Identifier BASE_LOCK;

	public static KeyBinding LOCK_KEY = new KeyBinding("unofficial-monumenta-mod.keybinds.lock_slot", GLFW.GLFW_KEY_L, "unofficial-monumenta-mod.keybinds.category");

	private static final Style lockTextStyle = Style.EMPTY.withColor(TextColor.fromRgb(0xc49417)).withBold(true);

	private static int tickSinceLastLockText = 0;

	private static int getTextCooldown() {
		return Math.round(UnofficialMonumentaModClient.options.lock_textCooldown * 20);
	}

	//NOTE: doesn't work in creative inventory because it would require too many compatibility layers to actually make it work without having a lot of unintended behaviours.
	private final Utils.Lerp circleSize = new Utils.Lerp(0, 200);

	private SlotLockData config = new SlotLockData();

	private static boolean isHoldingLockKey = false;

	private int ticksSinceLastLockKeyClick = -1;

	private Slot activeSlot = null;
	
	public static void registerSprites() {
		if (atlas == null) {
			atlas = ModSpriteAtlasHolder.createAtlas(UnofficialMonumentaModClient.MOD_IDENTIFIER, "gui");
		} else {
			atlas.clearSprites();
		}
		LOCK = atlas.registerSprite("locks/locked");
		LEFT_CLICK_LOCK = atlas.registerSprite("locks/left-click");
		RIGHT_CLICK_LOCK = atlas.registerSprite("locks/right-click");
		DROP_LOCK = atlas.registerSprite("locks/drop");
		BASE_LOCK  = atlas.registerSprite("locks/base-lock");
	}

	private boolean isLockedSlot(LockedSlot locked) {
		if (locked == null) {
			return false;
		}

		return ((locked.locked || locked.lockDrop || locked.lockHalfPickup || locked.lockPickup));
	}

	public boolean isLockedSlot(Slot slot) {
		return isLockedSlot(getLockedSlot(slot));
	}

	public void drawSlot(Screen screen, MatrixStack matrices, Slot slot) {
		int originX = slot.x;
		int originY = slot.y;
		
		LockedSlot locked = getLockedSlot(slot);
		
		if (locked == null) {
			return;
		}
		
		if (isLockedSlot(locked) && LOCK != null) {
			drawSprite(matrices, atlas.getSprite(LOCK), originX, originY, 16, 16);
		}
		
		//status of special locks
		if (UnofficialMonumentaModClient.options.lock_renderDebuggingAdvancedLock) {
			DrawableHelper.drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, Text.of("L"), originX, originY, locked.lockPickup ? 0xFFFFFFFF : 0xFFFF0000);//locked.lockPickup
			DrawableHelper.drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, Text.of("D"), originX + MinecraftClient.getInstance().textRenderer.getWidth("L"), originY, locked.lockDrop ? 0xFFFFFFFF : 0xFFFF0000);//locked.lockDrop
			DrawableHelper.drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, Text.of("H"), originX + MinecraftClient.getInstance().textRenderer.getWidth("LD"), originY, locked.lockHalfPickup ? 0xFFFFFFFF : 0xFFFF0000);//locked.lockHalfPickup
		}
	}

	public void onInputEvent(CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || (client.currentScreen instanceof CreativeInventoryScreen)) {
			return;
		}
		
		if (client.options.dropKey.isPressed() || client.options.dropKey.wasPressed()) {
			LockedSlot activeHandLocked = getLockedSlotIndex(client.player.getInventory().selectedSlot);
			if (activeHandLocked != null && (activeHandLocked.lockDrop || activeHandLocked.locked)) {
				if (tickSinceLastLockText >= getTextCooldown()) {
					client.inGameHud.getChatHud().addMessage(MutableText.of(Text.of("Stopped dropping of locked item").getContent()).setStyle(lockTextStyle));
					tickSinceLastLockText = 0;
				}
				ci.cancel();
			}
		}
	}
	
	public void tickRender(MatrixStack matrices, int mouseX, int mouseY) {
		if (!(MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?> containerScreen) || MinecraftClient.getInstance().player == null || activeSlot == null) {
			return;
		}
		
		circleSize.tick();
		
		int slotIndex = activeSlot.getIndex();
		
		int containerOriginX = ((HandledScreenAccessor) containerScreen).getX();
		int containerOriginY = ((HandledScreenAccessor) containerScreen).getY();
		
		//origin of container + relative origin of slot
		int absoluteSlotX = containerOriginX + activeSlot.x;
		int absoluteSlotY = containerOriginY + activeSlot.y;
		
		if (isHoldingLockKey) {
			if (circleSize.getTarget() != 20.0f) {
				circleSize.setTarget(20.0f);
				circleSize.resetTimer();
			}
		} else {
			if (circleSize.getValue() != 0.0f) {
				circleSize.setValue(0.0f);
				circleSize.setTarget(0.0f);
			}
		}
		
		if (isHoldingLockKey) {
			LockedSlot slot = config.lockedSlots[slotIndex];
			//is active and being held
			
			Utils.drawFilledPolygon(matrices, absoluteSlotX + 8, absoluteSlotY + 8, circleSize.getValue(), 360, 0x404040a0);
			
			drawSprite(matrices, atlas.getSprite(LEFT_CLICK_LOCK), absoluteSlotX - 15, absoluteSlotY - 15, 16, 16);
			if (slot.lockPickup) {
				drawSprite(matrices, atlas.getSprite(BASE_LOCK), absoluteSlotX - 15, absoluteSlotY - 15, 16, 16);
			}
			drawSprite(matrices, atlas.getSprite(RIGHT_CLICK_LOCK), absoluteSlotX + 15, absoluteSlotY - 15, 16, 16);
			if (slot.lockHalfPickup) {
				drawSprite(matrices, atlas.getSprite(BASE_LOCK), absoluteSlotX + 15, absoluteSlotY - 15, 16, 16);
			}
			drawSprite(matrices, atlas.getSprite(DROP_LOCK), absoluteSlotX, absoluteSlotY + 15, 16, 16);
			if (slot.lockDrop) {
				drawSprite(matrices, atlas.getSprite(BASE_LOCK), absoluteSlotX, absoluteSlotY + 15, 16, 16);
			}
			
		} else if (ticksSinceLastLockKeyClick <= 2) {
			//right after releasing lock key
			
			//use middle of the slot as the origin.
			//Since the detection of the slot is done in a 16x16 area from its origin it shouldn't cause an issue.
			int dragX = (absoluteSlotX + 8) - mouseX;
			int dragY = (absoluteSlotY + 8) - mouseY;
			
			if (config.lockedSlots[slotIndex] == null) {
				config.lockedSlots[slotIndex] = new LockedSlot();
			}
			
			//position for full lock hitbox
			final int full_left = 8;//-
			final int full_right = 8;
			final int full_up = 8;
			final int full_down = 8;//-
			
			//position for top buttons hitbox (left and right click locks)
			final int top_min_y = 2;
			final int left_max_right = 10;
			final int right_max_left = 10;
			
			//position for drop button hitbox
			final int bottom_max_y = 8;
			final int side_max_x = 20;
			
			if (dragX > left_max_right && dragY > top_min_y) {
				//LEFT button
				config.lockedSlots[slotIndex].switchLock(LockType.PICKUP);
			} else if (dragX < -right_max_left && dragY > top_min_y) {
				//RIGHT button
				config.lockedSlots[slotIndex].switchLock(LockType.HALF_PICKUP);
			} else if ((dragX > -side_max_x && dragX < side_max_x) && dragY < -bottom_max_y) {
				//DROP button
				config.lockedSlots[slotIndex].switchLock(LockType.DROP);
			} else if ((dragX > -full_left && dragX < full_right) && (dragY > -full_down && dragY < full_up)) {
				//FULL lock
				config.lockedSlots[slotIndex].switchLock(LockType.ALL);
			}

			MinecraftClient.getInstance().getSoundManager().play(new PositionedSoundInstance(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, isLockedSlot(activeSlot) ? 0.943f : 0.1f, Random.create(), MinecraftClient.getInstance().player.getBlockPos()));

			//reset active slot
			activeSlot = null;
		}
	}
	
	private static int getLockKeyCode() {
		return ((KeyBindingAccessor) LOCK_KEY).getBoundKey().getCode();
	}

	private static boolean isLockKeyPressed() {
		if (Objects.equals(((KeyBindingAccessor) LOCK_KEY).getBoundKey().getCategory(), InputUtil.Type.MOUSE)) {
			return GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), getLockKeyCode()) == 1;
		} else {
			return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), getLockKeyCode());
		}
	}

	public void onEndTick() {
		if (MinecraftClient.getInstance().world == null) {
			return;
		}
		tickSinceLastLockText++;
		if (!isLockKeyPressed()) {
			if (ticksSinceLastLockKeyClick == -1) {
				ticksSinceLastLockKeyClick = 1;
			} else {
				ticksSinceLastLockKeyClick++;
			}

			if (isHoldingLockKey && ticksSinceLastLockKeyClick > 1) {
				isHoldingLockKey = false;
			}
		} else {
			isHoldingLockKey = true;
			ticksSinceLastLockKeyClick = 0;
		}
	}

	public void onKeyboardInput(Screen screen, int code, int scancode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (!(screen instanceof HandledScreen<?> containerScreen) || MinecraftClient.getInstance().player == null || (screen instanceof CreativeInventoryScreen)) {
			return;
		}
		
		final int scaledWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
		final int scaledHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
		double mouseX = MinecraftClient.getInstance().mouse.getX() * (double)scaledWidth / (double)MinecraftClient.getInstance().getWindow().getWidth();
		double mouseY = MinecraftClient.getInstance().mouse.getY() * (double)scaledHeight / (double)MinecraftClient.getInstance().getWindow().getHeight();
		if (LOCK_KEY.matchesKey(code, scancode) && !isHoldingLockKey) {
			isHoldingLockKey = true;
			
			Slot slot = ((HandledScreenAccessor) containerScreen).doGetSlotAt(mouseX, mouseY);
			if (slot != null &&
					slot.inventory instanceof PlayerInventory) {
				int slotI = slot.getIndex();
				if ((slotI >= 0 && slotI <= 40) && config.lockedSlots != null) {
					if (config.lockedSlots[slotI] == null) {
						config.lockedSlots[slotI] = new LockedSlot();
					}
					
					activeSlot = slot;
				}
			}
		}
	}
	
	public boolean onSlotClicked(Screen screen, Slot slot, int slotId, int button, SlotActionType actionType) {
		if (!(screen instanceof HandledScreen) || MinecraftClient.getInstance().player == null) {
			return false;
		}
		
		return handleSlotInteraction(slot, button, actionType);
	}

	private boolean handleSlotInteraction(Slot slot, int button, SlotActionType actionType) {
		LockedSlot locked = getLockedSlot(slot);
		if (locked == null) {
			return false;
		}

		boolean shouldBlock = false;

		switch (actionType) {
			case PICKUP -> shouldBlock = shouldBlockPickupAction(slot, button, actionType);
			case SWAP -> shouldBlock = shouldBlockSwapAction(slot, button, actionType);
			case THROW -> {
				if (!getLockedSlot(slot).lockDrop) break;
				shouldBlock = true;
				if (tickSinceLastLockText < getTextCooldown()) break;

				MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(MutableText.of(Text.of("Stopped dropping of locked item").getContent()).setStyle(lockTextStyle));
				tickSinceLastLockText = 0;
			}
		}

		if (shouldBlock && MinecraftClient.getInstance().player != null) {
			MinecraftClient.getInstance().getSoundManager().play(new PositionedSoundInstance(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1.0f, 0.1f, Random.create(), MinecraftClient.getInstance().player.getBlockPos()));
		}

		return shouldBlock;
	}

	private boolean shouldBlockPickupAction(Slot slot, int button, SlotActionType actionType) {
		if (actionType != SlotActionType.PICKUP || slot == null) {
			return false;
		}
		LockedSlot locked = getLockedSlot(slot);
		if (locked == null) {
			return false;
		}

		return button == 0 ? (locked.lockPickup || locked.locked) : button == 1 ? (locked.lockHalfPickup || locked.locked) : locked.locked;
	}


	private boolean shouldBlockSwapAction(Slot slot, int button, SlotActionType actionType) {
		if (actionType != SlotActionType.SWAP || slot == null) {
			return false;
		}

		for (int i = 0; i < MinecraftClient.getInstance().options.hotbarKeys.length; i++) {
			if (button == i && (getLockedSlotIndex(i) != null && getLockedSlotIndex(i).locked || getLockedSlot(slot) != null && getLockedSlot(slot).locked)) {
				if (tickSinceLastLockText >= getTextCooldown()) {
					MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(MutableText.of(Text.of("Stopped exchange of locked item").getContent()).setStyle(lockTextStyle));
					tickSinceLastLockText = 0;
				}
				return true;
			}
		}
		return false;
	}

	public boolean isSlotIndexLocked(int index) {
		LockedSlot locked = getLockedSlotIndex(index);

		return locked != null &&
				locked.locked;
	}
	
	public LockedSlot getLockedSlot(Slot slot) {
		if (slot == null || MinecraftClient.getInstance().player == null) {
			return null;
		}
		if (!(slot.inventory instanceof PlayerInventory)) {
			return null;
		}
		int index = slot.getIndex();
		if (index < 0 || index > 40) {
			return null;
		}
		return getLockedSlotIndex(index);
	}
	
	private LockedSlot getLockedSlot(LockedSlot[] lockedSlots, int index) {
		if (lockedSlots == null) {
			return DEFAULT_LOCKED_SLOT;
		}
		
		LockedSlot slot = lockedSlots[index];
		
		if (slot == null) {
			return DEFAULT_LOCKED_SLOT;
		}
		
		return slot;
	}
	
	public LockedSlot getLockedSlotIndex(int index) {
		if (config == null) {
			return null;
		}
		return getLockedSlot(config.lockedSlots, index);
	}

	private File getFile() {
		return FabricLoader.getInstance().getConfigDir().resolve(CACHE_PATH).toFile();
	}

	public void reload() {
		File file = getFile();
		if (!file.exists()) {
			return;
		}
		
		try (FileReader reader = new FileReader(file)) {
			config = GSON.fromJson(reader, SlotLockData.class);
		} catch (Exception e) {
			UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to reload slot locking data", e);
		}
	}
	
	public void save() {
		File file = getFile();
		if (!file.exists()) {
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to create files for slot locking data", e);
			}
		}
		
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(GSON.toJson(config));
		} catch (Exception e) {
			UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to save slot locking data", e);
		}
	}
}
