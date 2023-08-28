package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

	private static final Transformation THIRD_PERSON_RIGHT_HAND_TRANSFORM = new Transformation(new Vector3f(0, -90, 55), new Vector3f(0, 0.2f, 0), new Vector3f(1.7f, 1.7f, 0.85f));
	private static final Transformation THIRD_PERSON_LEFT_HAND_TRANSFORM = new Transformation(new Vector3f(0, 90, -55), new Vector3f(0, 0.2f, 0), new Vector3f(1.7f, 1.7f, 0.85f));
	private static final Transformation FIRST_PERSON_RIGHT_HAND_TRANSFORM = new Transformation(new Vector3f(0, -90, 70), new Vector3f(0.1f, 0.2f, 0.1f), new Vector3f(1.36f, 1.36f, 0.68f));
	private static final Transformation FIRST_PERSON_LEFT_HAND_TRANSFORM = new Transformation(new Vector3f(0, 90, -70), new Vector3f(0.1f, 0.2f, 0.1f), new Vector3f(1.36f, 1.36f, 0.68f));

	@Unique
	private ModelTransformationMode originalRenderMode;

	/**
	 * Pretend tridents are always rendered in GUI mode. This prevents Mojank code for tridents from being executed, and also causes CITResewn's trident hack to work.
	 * CITResewn is also the reason why the change happens in this method instead of the more general renderItem method - it injects at HEAD there, but we need to inject even earlier.
	 * The only other use of renderMode in the vanilla code is for transformations, which the next method in this mixin handles.
	 */
	@ModifyVariable(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
		at = @At("HEAD"), ordinal = 0, argsOnly = true)
	ModelTransformationMode renderItem_tridentFix_citResewnHack(ModelTransformationMode renderMode, @Nullable LivingEntity entity, ItemStack item, ModelTransformationMode renderMode2, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, @Nullable World world, int light, int overlay, int seed) {
		Item spoofedItem = UnofficialMonumentaModClient.spoofer.getSpoofItem(item);

		if (UnofficialMonumentaModClient.options.overrideTridentRendering
			    && !item.isEmpty()
			    && item.getItem() == Items.TRIDENT &&
				(spoofedItem != null && spoofedItem == Items.TRIDENT)) {
			originalRenderMode = renderMode;
			return ModelTransformationMode.GUI;
		}
		return renderMode;
	}

	/**
	 * Always use the hardcoded transforms here where the trident model was otherwise used (except HEAD which really should not be used...).
	 */
	@Redirect(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/model/json/ModelTransformation;getTransformation(Lnet/minecraft/client/render/model/json/ModelTransformationMode;)Lnet/minecraft/client/render/model/json/Transformation;"))
	Transformation getTransformation_tridentFix2(ModelTransformation modelTransformation, ModelTransformationMode renderMode, ItemStack stack, ModelTransformationMode renderMode2, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BakedModel model) {
		if (UnofficialMonumentaModClient.options.overrideTridentRendering
			    && stack.getItem() == Items.TRIDENT) {
			if (originalRenderMode != null) {
				renderMode = originalRenderMode;
				originalRenderMode = null;
			}
			return switch (renderMode) {
				case THIRD_PERSON_RIGHT_HAND -> THIRD_PERSON_RIGHT_HAND_TRANSFORM;
				case THIRD_PERSON_LEFT_HAND -> THIRD_PERSON_LEFT_HAND_TRANSFORM;
				case FIRST_PERSON_RIGHT_HAND -> FIRST_PERSON_RIGHT_HAND_TRANSFORM;
				case FIRST_PERSON_LEFT_HAND -> FIRST_PERSON_LEFT_HAND_TRANSFORM;
				default -> modelTransformation.getTransformation(renderMode);
			};
		}
		return modelTransformation.getTransformation(renderMode);
	}

	@ModifyVariable(method = "renderGuiItemIcon", at = @At("HEAD"), argsOnly = true)
	private ItemStack umm$editStack$renderGuiItemIcon(ItemStack value) {
		ItemStack edited = UnofficialMonumentaModClient.spoofer.apply(value);
		return edited != null ? edited : value;
	}

	@ModifyVariable(method = "innerRenderInGui(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;IIII)V", at = @At("HEAD"), argsOnly = true)
	private ItemStack umm$editStack$innerRenderInGui(ItemStack value) {
		ItemStack edited = UnofficialMonumentaModClient.spoofer.apply(value);
		return edited != null ? edited : value;
	}

	@ModifyVariable(method = "renderGuiItemModel", at = @At("HEAD"), argsOnly = true)
	private ItemStack umm$editStack$renderModel(ItemStack value) {
		ItemStack edited = UnofficialMonumentaModClient.spoofer.apply(value);
		return edited != null ? edited : value;
	}

	@ModifyVariable(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V", at = @At("HEAD"), argsOnly = true)
	private ItemStack umm$editStack$renderItemEntity(ItemStack value) {
		ItemStack edited = UnofficialMonumentaModClient.spoofer.apply(value);
		return edited != null ? edited : value;
	}
}
