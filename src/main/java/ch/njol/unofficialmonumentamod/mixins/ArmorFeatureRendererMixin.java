package ch.njol.unofficialmonumentamod.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin<T extends LivingEntity, M extends BipedEntityModel<T>, A extends BipedEntityModel<T>> extends FeatureRenderer<T, M> {

	public ArmorFeatureRendererMixin(FeatureRendererContext<T, M> context) {
		super(context);
	}

	/**
	 * If a helmet has a model, do not render it as usual and instead render its model
	 */
	@Inject(method = "renderArmor(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;ILnet/minecraft/client/render/entity/model/BipedEntityModel;)V",
			at = @At("HEAD"), cancellable = true)
	public void renderArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, T livingEntity, EquipmentSlot equipmentSlot,
							int i, M bipedEntityModel, CallbackInfo ci) {
		if (equipmentSlot != EquipmentSlot.HEAD)
			return;

		ItemStack itemStack = livingEntity.getEquippedStack(EquipmentSlot.HEAD);
		if (!(itemStack.getItem() instanceof ArmorItem))
			return;

		BakedModelManager bakedModelManager = MinecraftClient.getInstance().getBakedModelManager();
		BakedModel headModel = MinecraftClient.getInstance().getItemRenderer().getHeldItemModel(itemStack, livingEntity.world, livingEntity);
		headModel = headModel.getOverrides().apply(headModel, itemStack, (ClientWorld) livingEntity.world, livingEntity);
		if (headModel == null || headModel == bakedModelManager.getMissingModel() || !headModel.hasDepth())
			return;

		ci.cancel();

		// this code is mostly copied from HeadFeatureRenderer
		matrices.push();
		if (livingEntity.isBaby() && !(livingEntity instanceof VillagerEntity)) {
			matrices.translate(0.0D, 0.03125D, 0.0D);
			matrices.scale(0.7F, 0.7F, 0.7F);
			matrices.translate(0.0D, 1.0D, 0.0D);
		}
		this.getContextModel().getHead().rotate(matrices);
		matrices.translate(0.0D, -0.25D, 0.0D);
		matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0F));
		matrices.scale(0.625F, -0.625F, -0.625F);
		if (livingEntity instanceof VillagerEntity || livingEntity instanceof ZombieVillagerEntity) {
			matrices.translate(0.0D, 0.1875D, 0.0D);
		}
		MinecraftClient.getInstance().getItemRenderer().renderItem(itemStack, ModelTransformation.Mode.HEAD,
				false, matrices, vertexConsumers, i, OverlayTexture.DEFAULT_UV, headModel);
		matrices.pop();
	}

}
