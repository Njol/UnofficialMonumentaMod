package ch.njol.unofficialmonumentamod;

import net.minecraft.item.ItemStack;

public abstract class Utils {

    private Utils() {
    }

    /**
     * Gets the plain display name of an items. This is used by Monumenta to distinguish items.
     *
     * @param itemStack An item stack
     * @return The plain display name of the item, i.e. the value of NBT node plain.display.Name.
     */
    public static String getPlainDisplayName(ItemStack itemStack) {
        return itemStack.getTag() == null ? null : itemStack.getTag().getCompound("plain").getCompound("display").getString("Name");
    }

    public static float smoothStep(float f) {
        if (f <= 0) return 0;
        if (f >= 1) return 1;
        return f * f * (3 - 2 * f);
    }

}
