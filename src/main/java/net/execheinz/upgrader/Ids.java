package net.execheinz.upgrader;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Stack identity: "modid:name" or, for items with subtypes, "modid:name@meta". */
public final class Ids {
	private Ids() {}

	public static String key(ItemStack s) {
		if (s == null || s.isEmpty()) return "";
		Item i = s.getItem();
		ResourceLocation r = i.getRegistryName();
		if (r == null) return "";
		return i.getHasSubtypes() ? r.toString() + "@" + s.getMetadata() : r.toString();
	}

	public static String baseId(String key) {
		int a = key.indexOf('@');
		return a < 0 ? key : key.substring(0, a);
	}

	public static ItemStack stack(String key) {
		if (key == null || key.isEmpty()) return ItemStack.EMPTY;
		int a = key.indexOf('@');
		String id = a < 0 ? key : key.substring(0, a);
		int meta = 0;
		try {
			if (a >= 0) meta = Integer.parseInt(key.substring(a + 1));
			Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
			if (item == null || item == Items.AIR) return ItemStack.EMPTY;
			return new ItemStack(item, 1, meta);
		} catch (RuntimeException e) {
			return ItemStack.EMPTY;
		}
	}
}
