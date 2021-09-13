package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

	@Shadow
	@Final
	private ItemModels models;

	/**
	 * Pretend that tridents are apples so that the trident-specific code is not executed
	 */
	@Redirect(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
	public Item renderItem_tridentFix1(ItemStack itemStack) {
		Item item = itemStack.getItem();
		if (UnofficialMonumentaModClient.options.overrideTridentRendering && item == Items.TRIDENT)
			return Items.APPLE;
		return item;
	}

	/**
	 * Always use the trident item model instead of the entity model
	 */
	@ModifyVariable(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
			at = @At("HEAD"),
			ordinal = 0)
	public BakedModel renderItem_tridentFix2(BakedModel model, ItemStack stack, ModelTransformation.Mode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BakedModel model2) {
		if (UnofficialMonumentaModClient.options.overrideTridentRendering
				&& stack.getItem() == Items.TRIDENT)
			return models.getModelManager().getModel(new ModelIdentifier("minecraft:trident#inventory"));
		return model;
	}

}
