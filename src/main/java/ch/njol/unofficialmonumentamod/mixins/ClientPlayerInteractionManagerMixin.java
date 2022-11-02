package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.features.misc.managers.CooldownManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static ch.njol.unofficialmonumentamod.Utils.isChestSortDisabledForInventory;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

	/**
     * Optionally disable the quicksort feature (sort inventory on double right click)
     * And handles part of the detection used for the Item cooldowns.
     */
    @Inject(method = "clickSlot(IIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V",
            at = @At("HEAD"), cancellable = true)
    public void clickSlot_head(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (CooldownManager.shouldRender() &&
                (slotId < player.currentScreenHandler.slots.size()) &&
                (slotId > 0) &&
                CooldownManager.getCooldownFromItem(player.currentScreenHandler.getSlot(slotId).getStack()) != null &&
                actionType == SlotActionType.PICKUP && button == 1) {
            CooldownManager.addCooldownToItem(player.currentScreenHandler.getSlot(slotId).getStack(), CooldownManager.Trigger.INVENTORY);
        } else if (!CooldownManager.shouldRender()) {
            CooldownManager.removeCooldownFromItem(player.currentScreenHandler.getSlot(slotId).getStack(), CooldownManager.Trigger.INVENTORY);
        }
        if (actionType == SlotActionType.PICKUP // single click
                && button == 1 // right click
                && player.currentScreenHandler.getCursorStack().isEmpty()
                && player.currentScreenHandler.getSlot(slotId).getStack().isEmpty()
                && isChestSortDisabledForInventory(player.currentScreenHandler, slotId)) {
            ci.cancel();
        }
    }

}
