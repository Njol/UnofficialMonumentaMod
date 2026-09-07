package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.features.spoof.TextureSpoofer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ElytraFeatureRenderer.class)
public class ElytraFeatureRendererMixin<T extends LivingEntity> {
	@Unique
	private static LivingEntity contextEntity;

	@Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at  = @At("HEAD"),  cancellable = true)
	public void  umm$renderElytraMonitor(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
		contextEntity = livingEntity;
	}

	@ModifyVariable(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At(value = "STORE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
	private ItemStack editStack(ItemStack value) {
		if (!Utils.isEntityMainPlayer(contextEntity) || (UnofficialMonumentaModClient.spoofer.spoofedItems.containsKey(TextureSpoofer.getKeyOf(value)) &&
			    !(Registries.ITEM.get(UnofficialMonumentaModClient.spoofer.spoofedItems.get(TextureSpoofer.getKeyOf(value)).getItemIdentifier()) instanceof ElytraItem))) {
			return value;
		}
		ItemStack edited = UnofficialMonumentaModClient.spoofer.apply(value);
		return edited != null ? edited : value;
	}
}
