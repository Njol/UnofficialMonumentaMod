package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.features.misc.managers.CooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {
    /**
     *  Part of the detection used for item cooldowns.
     */
    @Inject(at=@At("HEAD"), method="internalOnSlotClick")
    private void onPlayerInventoryInteraction(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if ((CooldownManager.shouldRender() &&
                slotIndex < player.currentScreenHandler.slots.size()) &&
                (slotIndex > 0) &&
                CooldownManager.getCooldownFromItem(player.currentScreenHandler.getSlot(slotIndex).getStack()) != null &&
                actionType == SlotActionType.PICKUP && button == 1) {
            CooldownManager.addCooldownToItem(player.currentScreenHandler.getSlot(slotIndex).getStack(), CooldownManager.Trigger.INVENTORY);
        } else if (!CooldownManager.shouldRender()) {
            CooldownManager.removeCooldownFromItem(player.currentScreenHandler.getSlot(slotIndex).getStack(), CooldownManager.Trigger.INVENTORY);
        }
    }
}
