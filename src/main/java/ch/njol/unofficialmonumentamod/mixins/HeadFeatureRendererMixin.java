package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BuiltinBakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeadFeatureRenderer.class)
public abstract class HeadFeatureRendererMixin<T extends LivingEntity, M extends EntityModel<T> & ModelWithHead> extends FeatureRenderer<T, M> {

	public HeadFeatureRendererMixin(FeatureRendererContext<T, M> context) {
		super(context);
	}

	/**
	 * If a skull block has an "on_head" model, do not render it as usual and instead render that model
	 */
	@Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
		at = @At("HEAD"), cancellable = true)
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int i, T livingEntity, float f, float g, float h, float j, float k, float l, CallbackInfo ci) {
		ItemStack itemStack = UnofficialMonumentaModClient.spoofer.apply(livingEntity.getEquippedStack(EquipmentSlot.HEAD));

		Item item = itemStack.getItem();
		if (!(item instanceof BlockItem) || !(((BlockItem) item).getBlock() instanceof AbstractSkullBlock)) {
			return;
		}

		if (livingEntity instanceof VillagerEntity
			    && !livingEntity.isInvisible()
			    && UnofficialMonumentaModClient.options.hideVillagerPlayerHeads) {
			ci.cancel();
			return;
		}

		BakedModelManager bakedModelManager = MinecraftClient.getInstance().getBakedModelManager();
		BakedModel baseModel = MinecraftClient.getInstance().getItemRenderer().getModel(itemStack, livingEntity.world, livingEntity, 0);
		BakedModel headModel = baseModel.getOverrides().apply(baseModel, itemStack, (ClientWorld) livingEntity.world, livingEntity, 0);
		if (headModel == null || headModel == bakedModelManager.getMissingModel() || !headModel.hasDepth() || headModel instanceof BuiltinBakedModel) {
			return;
		}

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
		if (!UnofficialMonumentaModClient.options.lowerVillagerHelmets && (livingEntity instanceof VillagerEntity || livingEntity instanceof ZombieVillagerEntity)) {
			matrices.translate(0.0D, 0.1875D, 0.0D);
		}
		MinecraftClient.getInstance().getItemRenderer().renderItem(itemStack, ModelTransformation.Mode.HEAD,
			false, matrices, vertexConsumers, i, OverlayTexture.DEFAULT_UV, headModel);
		matrices.pop();
	}

	@Redirect(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V"))
	public void render_MatrixStack_translate(MatrixStack instance, double x, double y, double z) {
		if (UnofficialMonumentaModClient.options.lowerVillagerHelmets
			    && x == 0
			    && y == 0.0625D
			    && z == 0) {
			return; // don't translate
		}
		instance.translate(x, y, z);
	}

	@ModifyVariable(method = "translate(Lnet/minecraft/client/util/math/MatrixStack;Z)V", at = @At("HEAD"), argsOnly = true)
	private static boolean render_MatrixStack_translate(boolean villager) {
		return !UnofficialMonumentaModClient.options.lowerVillagerHelmets && villager;
	}

	@ModifyVariable(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At(value = "STORE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z"))
	private ItemStack editStack(ItemStack value) {
		return UnofficialMonumentaModClient.spoofer.apply(value);
	}

}
