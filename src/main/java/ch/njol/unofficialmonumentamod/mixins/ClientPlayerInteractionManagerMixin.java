package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

	/**
	 * Prevent flickering caused by items with the infinity enchantment by not "consuming" them client-side
	 * <p>
	 * TODO currently does not work for some reason?
	 */
	@Redirect(method = "interactItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/TypedActionResult;"))
	public TypedActionResult<ItemStack> interactItem_useItemStack(ItemStack itemStack, World world, PlayerEntity user, Hand hand) {
		int oldCount = itemStack.getCount();
		TypedActionResult<ItemStack> result = itemStack.use(world, user, hand);
		if (EnchantmentHelper.getLevel(Enchantments.INFINITY, itemStack) <= 0)
			return result;
		if (result.getResult() == ActionResult.CONSUME) {
			return TypedActionResult.success(result.getValue(), true);
		} else if (result.getResult() == ActionResult.SUCCESS
				&& result.getValue().getItem() == itemStack.getItem()
				&& result.getValue().getCount() < oldCount) {
			result.getValue().setCount(oldCount);
			user.setStackInHand(hand, result.getValue());
			return result;
		}
		return result;
	}

	/**
	 * Optionally disable the quicksort feature (sort inventory on double right click)
	 */
	@Inject(method = "clickSlot(IIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/item/ItemStack;",
			at = @At("HEAD"), cancellable = true)
	public void clickSlot_head(int syncId, int slotId, int clickData, SlotActionType actionType, PlayerEntity player, CallbackInfoReturnable<ItemStack> cir) {
		if (actionType == SlotActionType.PICKUP // single click
				&& clickData == 1 // right click
				&& player.inventory.getCursorStack().isEmpty()
				&& player.currentScreenHandler.getSlot(slotId).getStack().isEmpty()
				&& isChestSortDisabledForInventory(player.currentScreenHandler, slotId)) {
			cir.setReturnValue(ItemStack.EMPTY);
			cir.cancel();
		}
	}

	private boolean isChestSortDisabledForInventory(ScreenHandler screenHandler, int slotId) {
		if (screenHandler.getSlot(slotId).inventory instanceof PlayerInventory)
			return UnofficialMonumentaModClient.options.chestsortDisabledForInventory;
		if (MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen
				&& !(screenHandler.getSlot(slotId).inventory instanceof PlayerInventory)
				&& "Ender Chest".equals(MinecraftClient.getInstance().currentScreen.getTitle().getString()))
			return UnofficialMonumentaModClient.options.chestsortDisabledForEnderchest;
		return UnofficialMonumentaModClient.options.chestsortDisabledEverywhereElse;
	}

}
