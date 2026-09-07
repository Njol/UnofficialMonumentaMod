package ch.njol.unofficialmonumentamod.mixins;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ThrowablePotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ThrowablePotionItem.class)
public class ThrowablePotionItemMixin {

	// Don't (visually) consume the last of a stack of infinite throwing potions
	@Redirect(method = "use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/TypedActionResult;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;decrement(I)V"))
	void use(ItemStack itemStack, int amount) {
		if (itemStack.getCount() == 1 && EnchantmentHelper.getLevel(Enchantments.INFINITY, itemStack) > 0) {
			return;
		}
		itemStack.decrement(amount);
	}

}
