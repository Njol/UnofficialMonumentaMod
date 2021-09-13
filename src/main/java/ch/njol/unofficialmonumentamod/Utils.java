package ch.njol.unofficialmonumentamod;

import net.minecraft.item.ItemStack;

public class Utils {

	/**
	 * Gets the plain display name of an items. This is used by Monumenta to distinguish items.
	 *
	 * @param itemStack An item stack
	 * @return The plain display name of the item, i.e. the value of NBT node plain.display.Name.
	 */
	public static String getPlainDisplayName(ItemStack itemStack) {
		return itemStack.getNbt() == null ? null : itemStack.getNbt().getCompound("plain").getCompound("display").getString("Name");
	}

}
