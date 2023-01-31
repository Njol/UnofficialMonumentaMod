package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.features.spoof.TextureSpoofer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ElytraFeatureRenderer.class)
public class ElytraFeatureRendererMixin<T extends LivingEntity> {
	@ModifyVariable(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At(value = "STORE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
	private ItemStack editStack(ItemStack value) {
		if (UnofficialMonumentaModClient.spoofer.spoofedItems.containsKey(TextureSpoofer.getKeyOf(value)) &&
			    !(Registries.ITEM.get(UnofficialMonumentaModClient.spoofer.spoofedItems.get(TextureSpoofer.getKeyOf(value)).getItemIdentifier()) instanceof ElytraItem)) {
			return value;
		}
		ItemStack edited = UnofficialMonumentaModClient.spoofer.apply(value);
		return edited != null ? edited : value;
	}
}
