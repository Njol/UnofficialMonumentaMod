package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

	/**
	 * Optionally disable the quicksort feature (sort inventory on double right click)
	 */
	@Inject(method = "clickSlot(IIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V",
		at = @At("HEAD"), cancellable = true)
	public void clickSlot_head(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
		if (actionType == SlotActionType.PICKUP // single click
			    && button == 1 // right click
			    && player.currentScreenHandler.getCursorStack().isEmpty()
			    && player.currentScreenHandler.getSlot(slotId).getStack().isEmpty()
			    && isChestSortDisabledForInventory(player.currentScreenHandler, slotId)) {
			ci.cancel();
		}
	}

	@Unique
	private boolean isChestSortDisabledForInventory(ScreenHandler screenHandler, int slotId) {
		if (screenHandler.getSlot(slotId).inventory instanceof PlayerInventory) {
			return UnofficialMonumentaModClient.options.chestsortDisabledForInventory;
		}
		if (MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen
			    && !(screenHandler.getSlot(slotId).inventory instanceof PlayerInventory)
			    && ("Ender Chest".equals(MinecraftClient.getInstance().currentScreen.getTitle().getString()) // fake Ender Chest inventory (opened via Remnant)
				        || MinecraftClient.getInstance().currentScreen.getTitle() instanceof TranslatableTextContent
					           && "container.enderchest".equals(((TranslatableTextContent) MinecraftClient.getInstance().currentScreen.getTitle()).getKey()))) {
			return UnofficialMonumentaModClient.options.chestsortDisabledForEnderchest;
		}
		return UnofficialMonumentaModClient.options.chestsortDisabledEverywhereElse;
	}

}
