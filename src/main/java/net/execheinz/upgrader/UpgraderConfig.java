package net.execheinz.upgrader;

import java.io.File;
import java.util.*;
import net.minecraftforge.common.config.Configuration;

public final class UpgraderConfig {
	public static double houseEdge = 0.9;
	public static double minChance = 0.001;
	public static double maxChance = 0.9;
	public static int spinTicks = 70;
	public static int maxTargetCount = 64;
	public static int attemptLimit = 0;
	public static int attemptWindowSeconds = 0;
	public static double globalValueMultiplier = 1.0;
	public static Map<String, Double> valueOverrides = new HashMap<String, Double>();
	public static Set<String> blacklist = new HashSet<String>();
	public static Map<String, Double> valueMultipliers = new HashMap<String, Double>();

	private UpgraderConfig() {}

	public static void load(File file) {
		Configuration c = new Configuration(file);
		c.load();
		houseEdge = c.get("general", "houseEdge", 0.9, "Multiplier on the raw value ratio. 1.0 = fair odds, 0.9 = 10% house edge.", 0.0, 100.0).getDouble();
		minChance = c.get("general", "minChance", 0.001, "Lowest possible success chance (0.001 = 0.1%).", 0.0, 1.0).getDouble();
		maxChance = c.get("general", "maxChance", 0.9, "Highest possible success chance, so a downgrade is never a free win.", 0.0, 1.0).getDouble();
		spinTicks = c.getInt("spinTicks", "general", 70, 1, 1200, "How long the wheel spins before the result lands. 20 ticks = 1 second.");
		maxTargetCount = c.getInt("maxTargetCount", "general", 64, 1, 4096, "Most target items one upgrade may ask for. The item's own stack size still applies.");
		attemptLimit = c.getInt("attemptLimit", "general", 0, 0, 100000, "Attempts a player gets per window. 0 removes the limit.");
		attemptWindowSeconds = c.getInt("attemptWindowSeconds", "general", 0, 0, 86400, "Length of that window in seconds. 0 removes the limit.");
		globalValueMultiplier = c.get("prices", "globalValueMultiplier", 1.0, "Scales every item's price, fixed prices included.", 0.0, 1.0E9).getDouble();
		valueOverrides = parsePairs(c.getStringList("valueOverrides", "prices", new String[0],
				"Fixed prices, one per line: modid:item=price, or modid:item@meta=price for items with subtypes."));
		valueMultipliers = parsePairs(c.getStringList("valueMultipliers", "prices", new String[0],
				"Per-item multipliers on top of the global one, same format as valueOverrides."));
		blacklist = new HashSet<String>(Arrays.asList(c.getStringList("blacklist", "prices", new String[0],
				"Items that can't be put in or chosen as a target: modid:item or modid:item@meta.")));
		if (c.hasChanged()) c.save();
		ItemValues.invalidate();
	}

	public static Map<String, Double> parsePairs(String[] lines) {
		Map<String, Double> m = new HashMap<String, Double>();
		for (String l : lines) {
			int i = l.lastIndexOf('=');
			if (i <= 0) continue;
			try {
				m.put(l.substring(0, i).trim(), Double.parseDouble(l.substring(i + 1).trim()));
			} catch (NumberFormatException ignored) {
			}
		}
		return m;
	}
}
