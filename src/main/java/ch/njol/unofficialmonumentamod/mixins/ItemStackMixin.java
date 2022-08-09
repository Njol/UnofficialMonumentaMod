package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.misc.managers.ItemNameSpoofer;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Objects;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    /**
     * Renders spoofed name (and add the original name in tooltip)
     */
    @Inject(method = "getTooltip", at = @At(value = "INVOKE_ASSIGN", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onTooltip(@Nullable PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir, List<Text> list) {
        if (!Objects.equals(ItemNameSpoofer.getSpoofedName(((ItemStack)(Object)this)).getString(), ((ItemStack)(Object)this).getName().getString())) {
            list.remove(0);
            list.add(ItemNameSpoofer.getSpoofedName(((ItemStack)(Object)this)));
            list.add(new LiteralText("Spoofed: " + ((ItemStack)(Object)this).getName().getString().replaceAll("§.", "")).formatted(Formatting.DARK_GRAY));
        }
    }
}
