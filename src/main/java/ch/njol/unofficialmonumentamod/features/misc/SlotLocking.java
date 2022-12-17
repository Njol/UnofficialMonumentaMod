package ch.njol.unofficialmonumentamod.features.misc;

import ch.njol.minecraft.uiframework.ModSpriteAtlasHolder;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.mixins.KeyBindingAccessor;
import ch.njol.unofficialmonumentamod.mixins.screen.HandledScreenAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.MessageType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static ch.njol.minecraft.uiframework.hud.HudElement.drawSprite;

public class SlotLocking {
	// This feature is largely based on https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/src/main/java/io/github/moulberry/notenoughupdates/miscfeatures/SlotLocking.java
	private static final String CACHE_PATH = "monumenta/lockedSlots.json";
	
	//Currently set to a placeholder texture (will change it once I actually make one)
	private static Identifier LOCK;
	private static Identifier LEFT_CLICK_UNLOCKED;
	private static Identifier LEFT_CLICK_LOCKED;
	
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
		
		public boolean switchLock(LockType type) {
			if (type != LockType.ALL && locked) {
				return true;
			}
			boolean b = false;
			
			switch (type) {
				case HALF_PICKUP -> b = lockHalfPickup = !lockHalfPickup;
				case PICKUP -> b = lockPickup = !lockPickup;
				case DROP -> b = lockDrop = !lockDrop;
				case ALL -> {
					b = locked = !locked;
					lockDrop = locked;
					lockHalfPickup = locked;
					lockPickup = locked;
				}
			}
			//switchLock(ALL);
			// locked = true
			
			//locked -> false
			//lockDrop -> false
			//lockHalfPickup -> false
			//lockPickup -> false
			
			return b;
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
	
	public static void registerSprites() {
		if (atlas == null) {
			atlas = ModSpriteAtlasHolder.createAtlas(UnofficialMonumentaModClient.MOD_IDENTIFIER, "gui");
		} else {
			atlas.clearSprites();
		}
		LOCK = atlas.registerSprite("lock");
		LEFT_CLICK_LOCKED = atlas.registerSprite("lf_blocked");
		LEFT_CLICK_UNLOCKED = atlas.registerSprite("lf_unblocked");
	}
	
	private SlotLockData config = new SlotLockData();
	
	public void drawSlot(Screen screen, MatrixStack matrices, Slot slot) {
		int originX = slot.x;
		int originY = slot.y;
		
		LockedSlot locked = getLockedSlot(slot);
		
		if (locked == null) {
			return;
		}
		
		if (locked.locked && LOCK != null) {
			drawSprite(matrices, atlas.getSprite(LOCK), originX, originY, 16, 16);
		}
		
		//status of special locks
		DrawableHelper.drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, Text.of("L"), originX, originY, locked.lockPickup ? 0xFFFFFFFF : 0xFFFF0000);//locked.lockPickup
		DrawableHelper.drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, Text.of("D"), originX + MinecraftClient.getInstance().textRenderer.getWidth("L"), originY, locked.lockDrop ? 0xFFFFFFFF : 0xFFFF0000);//locked.lockDrop
		DrawableHelper.drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, Text.of("H"), originX + MinecraftClient.getInstance().textRenderer.getWidth("LD"), originY, locked.lockHalfPickup ? 0xFFFFFFFF : 0xFFFF0000);//locked.lockHalfPickup
	}
	
	public static KeyBinding LOCK_KEY = new KeyBinding("unofficial-monumenta-mod.keybinds.lock_slot", GLFW.GLFW_KEY_L, "unofficial-monumenta-mod.keybinds.category");
	
	public void onInputEvent(CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}
		
		if (client.options.dropKey.isPressed() || client.options.dropKey.wasPressed()) {//TODO make it only send message if it's first press //Seems like it sometimes bugs out and doesn't cancel
			LockedSlot activeHandLocked = getLockedSlotIndex(client.player.getInventory().selectedSlot);
			if (activeHandLocked != null && (activeHandLocked.lockDrop || activeHandLocked.locked)) {
				ci.cancel();
				if (!client.options.dropKey.wasPressed()) client.inGameHud.addChatMessage(MessageType.SYSTEM, Text.of("Stopped drop"), Util.NIL_UUID);
			}
		}
	}
	
	private static boolean isHoldingLockKey = false;
	
	public void __testRenderPolygon(MatrixStack matrices, int originX, int originY, int radius, int sides, int color) {
		float a = (float)(color >> 24 & 0xFF) / 255.0f;
		float r = (float)(color >> 16 & 0xFF) / 255.0f;
		float g = (float)(color >> 8 & 0xFF) / 255.0f;
		float b = (float)(color & 0xFF) / 255.0f;
		
		Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		
		bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
		bufferBuilder.vertex(positionMatrix, originX, originY, 0.0f).color(a, r, g, b).next();
		
		//very optimised (trust)
		for (int i = 0; i <= sides; i++) {
			double angle = ((Math.PI * 2) * i / sides) + Math.toRadians(180);
			bufferBuilder.vertex(originX + Math.sin(angle) * radius, originY + Math.cos(angle) * radius, 0.0f).color(a, r, g, b).next();
		}
		bufferBuilder.end();
		
		BufferRenderer.draw(bufferBuilder);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
	}
	
	private int ticksSinceLastLockKeyClick = -1;
	
	private Slot activeSlot = null;
	
	public void tickRender(MatrixStack matrices, int mouseX, int mouseY) {
		if (!(MinecraftClient.getInstance().currentScreen instanceof HandledScreen containerScreen) || MinecraftClient.getInstance().player == null || activeSlot == null) {
			return;
		}
		
		int containerOriginX = ((HandledScreenAccessor) containerScreen).getX();
		int containerOriginY = ((HandledScreenAccessor) containerScreen).getY();
		
		//origin of container + relative origin of slot
		int absoluteSlotX = containerOriginX + activeSlot.x;
		int absoluteSlotY = containerOriginY + activeSlot.y;
		
		if (isHoldingLockKey) {
			//is active and being held
			
			__testRenderPolygon(matrices, absoluteSlotX + 8, absoluteSlotY + 8, 20, 360, 0x404040a0);
			
			drawSprite(matrices, atlas.getSprite(LEFT_CLICK_UNLOCKED), absoluteSlotX - 15, absoluteSlotY - 15, 16, 16);
			drawSprite(matrices, atlas.getSprite(LEFT_CLICK_LOCKED), absoluteSlotX + 15, absoluteSlotY - 15, 16, 16);
			drawSprite(matrices, atlas.getSprite(LOCK), absoluteSlotX, absoluteSlotY + 15, 16, 16);
			
			//TODO render outside slots
			//TODO check for if released on one of the slots and act accordingly
		} else if (ticksSinceLastLockKeyClick <= 3) {
			//right after releasing lock key
			
			//the player is probably clicking close to the middle of the slot, so it should be precise enough to use.
			int dragX = (absoluteSlotX + 8) - mouseX;
			int dragY = (absoluteSlotY + 8) - mouseY;
			
			System.out.println(dragX + " " + dragY);
			
			int slotIndex = activeSlot.getIndex();
			
			if (config.lockedSlots[slotIndex] == null) {
				config.lockedSlots[slotIndex] = new LockedSlot();
			}
			
			//pos for full lock hitbox
			final int full_left = 8;//-
			final int full_right = 8;
			final int full_up = 8;
			final int full_down = 8;//-
			
			//pos for top buttons hitbox (left and right click locks)
			final int top_min_y = 2;
			final int left_max_right = 10;
			final int right_max_left = 10;
			
			//pos for drop button hitbox
			final int bottom_max_y = 8;
			final int side_max_x = 20;
			
			/* render debug hitboxes
			 	DrawableHelper.fill(matrices, (absoluteSlotX + 8) - full_left, (absoluteSlotY + 8) + full_up, (absoluteSlotX + 8) + full_right, (absoluteSlotY + 8) - full_down, 0xFFFFFF00);
		
				DrawableHelper.fill(matrices, 0, 0, (absoluteSlotX + 8) - left_max_right, (absoluteSlotY + 8) + top_min_y, 0xFF00FFFF);
				DrawableHelper.fill(matrices, (absoluteSlotX + 8) + right_max_left, 0, MinecraftClient.getInstance().getWindow().getScaledWidth(), (absoluteSlotY + 8) + top_min_y, 0xFF00FFFF);
		
				DrawableHelper.fill(matrices, (absoluteSlotX + 8) - side_max_x, (absoluteSlotY + 8) + bottom_max_y, (absoluteSlotX + 8) + side_max_x, MinecraftClient.getInstance().getWindow().getScaledHeight(),0xFFFF00FF);
			* */
			
			//A lil bit of fine tuning later
			if (dragX > left_max_right && dragY > top_min_y) {
				//LEFT button
				System.out.println("released on LEFT button");
				config.lockedSlots[slotIndex].switchLock(LockType.PICKUP);
			} else if (dragX < -right_max_left && dragY > top_min_y) {
				//RIGHT button
				System.out.println("released on RIGHT button");
				config.lockedSlots[slotIndex].switchLock(LockType.HALF_PICKUP);
			} else if ((dragX > -side_max_x && dragX < side_max_x) && dragY < -bottom_max_y) {
				//DROP button
				System.out.println("released on DROP button");
				config.lockedSlots[slotIndex].switchLock(LockType.DROP);
			} else if ((dragX > -full_left && dragX < full_right) && (dragY > -full_down && dragY < full_up)) {
				//FULL lock
				System.out.println("released on full lock");
				config.lockedSlots[slotIndex].switchLock(LockType.ALL);
			}
			
			//reset active slot
			activeSlot = null;
		}
	}
	
	private static int getLockKeyCode() {
		return ((KeyBindingAccessor) LOCK_KEY).getBoundKey().getCode();
	}
	
	public void onEndTick() {
		if (!InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), getLockKeyCode())) {
			if (ticksSinceLastLockKeyClick == -1) {
				ticksSinceLastLockKeyClick = 1;
			} else {
				ticksSinceLastLockKeyClick++;
			}
			
			if (isHoldingLockKey && ticksSinceLastLockKeyClick > 2) {
				isHoldingLockKey = false;
			}
		} else {
			isHoldingLockKey = true;
			ticksSinceLastLockKeyClick = 0;
		}
	}
	
	public void onKeyboardInput(Screen screen, int code, int scancode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (!(screen instanceof HandledScreen containerScreen) || MinecraftClient.getInstance().player == null) {
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
					
					//TODO add sound
				}
			}
		} else {
			//check for funny stuff
			
			MinecraftClient client = MinecraftClient.getInstance();
			
			for (int i = 0; i < client.options.hotbarKeys.length; i++) {
				KeyBinding keyBinding = client.options.hotbarKeys[i];
				if (keyBinding.matchesKey(code, scancode) &&
						getLockedSlotIndex(i) != null && getLockedSlotIndex(i).locked &&
						((HandledScreenAccessor) containerScreen).doGetSlotAt(mouseX, mouseY) != null) {
					cir.setReturnValue(true);
					client.inGameHud.addChatMessage(MessageType.SYSTEM, Text.of("Stopped slot exchange"), Util.NIL_UUID);
				}
			}
			
			if (client.options.dropKey.matchesKey(code, scancode)) {
				LockedSlot locked = getLockedSlot(((HandledScreenAccessor) containerScreen).doGetSlotAt(mouseX, mouseY));
				if (locked != null && (locked.lockDrop || locked.locked)) {
					cir.setReturnValue(true);
					client.inGameHud.addChatMessage(MessageType.SYSTEM, Text.of("Stopped drop"), Util.NIL_UUID);//DISCLAIMER: possibly will spam since yk I don't really check for anything to make sure it was the first keypress without the next ones.
				} else if (locked == null) {
					LockedSlot activeHandLocked = getLockedSlotIndex(client.player.getInventory().selectedSlot);
					if (activeHandLocked != null && (activeHandLocked.lockDrop || activeHandLocked.locked)) {
						cir.setReturnValue(true);
						client.inGameHud.addChatMessage(MessageType.SYSTEM, Text.of("Stopped drop"), Util.NIL_UUID);//DISCLAIMER: possibly will spam since yk I don't really check for anything to make sure it was the first keypress without the next ones.
					}
				}
			}
		}
	}
	
	public boolean onSlotClicked(Screen screen, Slot slot, int slotId, int button, SlotActionType actionType) {
		if (!(screen instanceof HandledScreen) || MinecraftClient.getInstance().player == null) {
			return false;
		}
		
		LockedSlot locked = getLockedSlot(slot);
		
		if (locked == null) {
			return false;
		}
		
		//left click, right click or all
		return (button == 0 ? (locked.lockPickup || locked.locked) : button == 1 ? (locked.lockHalfPickup || locked.locked) : locked.locked);
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
	
	public boolean isSlotIndexLocked(int index) {
		LockedSlot locked = getLockedSlotIndex(index);
		
		return locked != null &&
				locked.locked;
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
	
	public void reload() {
		File file = FabricLoader.getInstance().getConfigDir().resolve(CACHE_PATH).toFile();
		if (!file.exists()) {
			return;
		}
		
		try (FileReader reader = new FileReader(file)) {
			config = GSON.fromJson(reader, SlotLockData.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void save() {
		File file = FabricLoader.getInstance().getConfigDir().resolve(CACHE_PATH).toFile();
		
		if (!file.exists()) {
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(GSON.toJson(config));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
