package net.execheinz.upgrader;

import net.minecraft.item.ItemStack;

public final class UpgradeOdds {
	private UpgradeOdds() {}

	public static double chance(ItemStack input, ItemStack target, int count) {
		if (input.isEmpty() || target.isEmpty() || count <= 0) return 0;
		return chance(ItemValues.stackValue(input), ItemValues.unitValue(target) * count);
	}

	public static double chance(double inputValue, double targetValue) {
		if (inputValue <= 0 || targetValue <= 0) return 0;
		double c = UpgraderConfig.houseEdge * inputValue / targetValue;
		return Math.max(UpgraderConfig.minChance, Math.min(UpgraderConfig.maxChance, c));
	}
}
