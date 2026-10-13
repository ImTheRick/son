package net.execheinz.upgrader;

import java.util.*;
import net.minecraft.item.ItemStack;

/** Fixed anchor prices (everything else is derived from recipes) and the built-in blacklist. */
public final class BaseValues {
	private static final Map<String, Map<Integer, Double>> VALUES = new HashMap<String, Map<Integer, Double>>();
	private static final Set<String> BLACKLIST = new HashSet<String>();

	/** entries: "modid:item" (any meta) or "modid:item@meta". */
	private static void put(double v, String... entries) {
		for (String e : entries) {
			int a = e.indexOf('@');
			String id = a < 0 ? e : e.substring(0, a);
			int meta = a < 0 ? -1 : Integer.parseInt(e.substring(a + 1));
			Map<Integer, Double> m = VALUES.get(id);
			if (m == null) { m = new HashMap<Integer, Double>(); VALUES.put(id, m); }
			m.put(meta, v);
		}
	}

	public static Double get(ItemStack s) {
		if (s.isEmpty() || s.getItem().getRegistryName() == null) return null;
		Map<Integer, Double> m = VALUES.get(s.getItem().getRegistryName().toString());
		if (m == null) return null;
		Double d = m.get(s.getMetadata());
		return d != null ? d : m.get(-1);
	}

	public static boolean isBlacklisted(ItemStack s) {
		return !s.isEmpty() && s.getItem().getRegistryName() != null && BLACKLIST.contains(s.getItem().getRegistryName().toString());
	}

	static {
		put(1, "minecraft:dirt", "minecraft:cobblestone", "minecraft:sand", "minecraft:gravel", "minecraft:netherrack",
				"minecraft:end_stone", "minecraft:stone@1", "minecraft:stone@3", "minecraft:stone@5",
				"minecraft:snowball", "minecraft:wheat_seeds");
		put(2, "minecraft:stone@0", "minecraft:flint", "minecraft:cactus", "minecraft:reeds", "minecraft:obsidian",
				"minecraft:soul_sand", "minecraft:clay_ball");
		put(4, "minecraft:log", "minecraft:log2");
		put(3, "minecraft:wheat", "minecraft:potato", "minecraft:carrot", "minecraft:beetroot", "minecraft:apple", "minecraft:egg");
		put(6, "minecraft:porkchop", "minecraft:beef", "minecraft:chicken", "minecraft:mutton", "minecraft:rabbit",
				"minecraft:fish@0", "minecraft:fish@1", "minecraft:fish@2");
		put(30, "minecraft:fish@3");
		put(4, "minecraft:chorus_fruit", "minecraft:string", "minecraft:feather", "minecraft:bone", "minecraft:rotten_flesh",
				"minecraft:spider_eye", "minecraft:dye@0");
		put(12, "minecraft:leather", "minecraft:slime_ball", "minecraft:rabbit_hide", "minecraft:prismarine_shard",
				"minecraft:prismarine_crystals");
		put(20, "minecraft:gunpowder", "minecraft:magma_cream");
		put(50, "minecraft:ender_pearl", "minecraft:blaze_rod");
		put(120, "minecraft:ghast_tear", "minecraft:rabbit_foot", "minecraft:shulker_shell");
		put(6, "minecraft:coal");
		put(8, "minecraft:redstone", "minecraft:glowstone_dust");
		put(10, "minecraft:dye@4");
		put(12, "minecraft:quartz");
		put(40, "minecraft:iron_ingot");
		put(60, "minecraft:gold_ingot");
		put(140, "minecraft:emerald");
		put(400, "minecraft:diamond");
		put(300, "minecraft:sponge");
		put(600, "minecraft:saddle", "minecraft:name_tag");
		put(1500, "minecraft:totem_of_undying");
		put(2500, "minecraft:golden_apple@1", "minecraft:elytra");
		put(4000, "minecraft:nether_star");
		put(1400, "minecraft:skull@1");
		put(20000, "minecraft:dragon_egg");

		Collections.addAll(BLACKLIST, "minecraft:air", "minecraft:bedrock", "minecraft:barrier", "minecraft:structure_block",
				"minecraft:structure_void", "minecraft:command_block", "minecraft:chain_command_block",
				"minecraft:repeating_command_block", "minecraft:command_block_minecart", "minecraft:knowledge_book",
				"minecraft:mob_spawner", "minecraft:end_portal_frame", "minecraft:farmland", "minecraft:grass_path",
				"minecraft:monster_egg", "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion",
				"minecraft:tipped_arrow", "minecraft:enchanted_book", "minecraft:written_book", "minecraft:filled_map",
				"minecraft:end_portal", "minecraft:portal", "minecraft:end_gateway");
	}
}
