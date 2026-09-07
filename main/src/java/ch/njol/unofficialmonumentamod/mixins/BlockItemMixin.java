package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import java.util.Arrays;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

	@Shadow
	public abstract Block getBlock();

	/**
	 * Prevents Firmament (and its skinned version) from being consumed when placed, allowing fast placement regardless of ping.
	 */
	@Redirect(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;decrement(I)V"))
	public void place_consumeBlock(ItemStack itemStack, int amount) {
		if (itemStack.isEmpty() || !UnofficialMonumentaModClient.options.firmamentPingFix) {
			return;
		}
		if (getBlock() instanceof ShulkerBoxBlock
			    && Arrays.asList("Firmament", "Doorway from Eternity").contains(Utils.getPlainDisplayName(itemStack))) {
			// do nothing, i.e. keep the Firmament
			return;
		}
		itemStack.decrement(amount);
	}

	/**
	 * Instead of placing a Shulker box, make Firmament place a prismarine block (or blackstone if skinned).
	 * This both looks better and also prevents the client trying to open the placed Shulker box instead of placing another block.
	 */
	@Redirect(method = "getPlacementState(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/block/BlockState;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getPlacementState(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/block/BlockState;"))
	BlockState place_getPlacementState(Block block, ItemPlacementContext ctx) {
		if (!UnofficialMonumentaModClient.options.firmamentPingFix) {
			return block.getPlacementState(ctx);
		}
		ItemStack itemStack = ctx.getStack();
		if (getBlock() instanceof ShulkerBoxBlock && "Firmament".equals(Utils.getPlainDisplayName(itemStack))) {
			return Blocks.PRISMARINE.getDefaultState();
		} else if (getBlock() instanceof ShulkerBoxBlock && "Doorway from Eternity".equals(Utils.getPlainDisplayName(itemStack))) {
			return Blocks.BLACKSTONE.getDefaultState();
		}
		return block.getPlacementState(ctx);
	}

}
