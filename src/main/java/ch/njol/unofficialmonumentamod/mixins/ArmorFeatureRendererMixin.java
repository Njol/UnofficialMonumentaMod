package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.features.spoof.TextureSpoofer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Equipment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin<T extends LivingEntity, M extends BipedEntityModel<T>, A extends BipedEntityModel<T>> extends FeatureRenderer<T, M> {

	public ArmorFeatureRendererMixin(FeatureRendererContext<T, M> context) {
		super(context);
	}

	@Unique
	private static EquipmentSlot contextSlot;

	/**
	 * If a helmet has a model, do not render it as usual and instead render its model
	 */
	@Inject(method = "renderArmor(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;ILnet/minecraft/client/render/entity/model/BipedEntityModel;)V",
		at = @At("HEAD"), cancellable = true)
	public void renderArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, T livingEntity, EquipmentSlot equipmentSlot,
	                        int i, M bipedEntityModel, CallbackInfo ci) {
		contextSlot = equipmentSlot;
		if (equipmentSlot != EquipmentSlot.HEAD) {
			return;
		}

		ItemStack itemStack = livingEntity.getEquippedStack(EquipmentSlot.HEAD);
		if (!(itemStack.getItem() instanceof ArmorItem)) {
			return;
		}

		BakedModelManager bakedModelManager = MinecraftClient.getInstance().getBakedModelManager();
		BakedModel headModel = MinecraftClient.getInstance().getItemRenderer().getModel(itemStack, livingEntity.world, livingEntity, 0);
		headModel = headModel.getOverrides().apply(headModel, itemStack, (ClientWorld) livingEntity.world, livingEntity, 0);
		if (headModel == null || headModel == bakedModelManager.getMissingModel() || !headModel.hasDepth()) {
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
		matrices.multiply(new Quaternionf(0, 180, 0, 0));
		matrices.scale(0.625F, -0.625F, -0.625F);
		if (!UnofficialMonumentaModClient.options.lowerVillagerHelmets && (livingEntity instanceof VillagerEntity || livingEntity instanceof ZombieVillagerEntity)) {
			matrices.translate(0.0D, 0.1875D, 0.0D);
		}
		MinecraftClient.getInstance().getItemRenderer().renderItem(itemStack, ModelTransformationMode.HEAD,
			false, matrices, vertexConsumers, i, OverlayTexture.DEFAULT_UV, headModel);
		matrices.pop();
	}

	@ModifyVariable(method = "renderArmor", at = @At(value = "STORE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
	private ItemStack editStack(ItemStack value) {
		//override logic, if override is false and the item is wearable it will remove the original stack else it will carry on.
		if (UnofficialMonumentaModClient.options.enableTextureSpoofing &&
			    UnofficialMonumentaModClient.spoofer.spoofedItems.containsKey(TextureSpoofer.getKeyOf(value)) &&
			    !(Registries.ITEM.get(UnofficialMonumentaModClient.spoofer.spoofedItems.get(TextureSpoofer.getKeyOf(value)).getItemIdentifier()) instanceof ArmorItem)) {
			if (UnofficialMonumentaModClient.spoofer.spoofedItems.get(TextureSpoofer.getKeyOf(value)).override &&
					((Registries.ITEM.get(UnofficialMonumentaModClient.spoofer.spoofedItems.get(TextureSpoofer.getKeyOf(value)).getItemIdentifier()) instanceof Equipment) ||
				    (contextSlot == EquipmentSlot.HEAD))) {
				return ItemStack.EMPTY;
			}
			//if the item is not an armor item (e.g: iron helmet, diamond leggings, golden boots, etc...) but is also not wearable
			return value;
		}
		
		ItemStack edited = UnofficialMonumentaModClient.spoofer.apply(value);
		return edited != null ? edited : value;
	}

}
