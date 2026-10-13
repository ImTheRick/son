package net.execheinz.upgrader;

import java.util.*;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Item prices. Anchors (BaseValues + config overrides) are fixed; every other price is the cheapest
 * recipe cost found by relaxation; unpriced leftovers fall back to a rarity-based price.
 */
public final class ItemValues {
	private static final int MAX_PASSES = 16;
	private static final int MAX_FLOOR_ROUNDS = 24;

	private static final class Recipe {
		final String result; final int count; final List<List<String>> groups;
		Recipe(String result, int count, List<List<String>> groups) { this.result = result; this.count = count; this.groups = groups; }

		double cost(Map<String, Double> p) {
			double sum = 0;
			for (List<String> g : groups) {
				double best = Double.POSITIVE_INFINITY;
				for (String k : g) { Double v = p.get(k); if (v != null && v < best) best = v; }
				if (Double.isInfinite(best)) return Double.POSITIVE_INFINITY;
				sum += best;
			}
			return sum <= 0 ? Double.POSITIVE_INFINITY : sum / count;
		}
	}

	private static Map<String, Double> prices;
	private static Map<String, ItemStack> stacks;
	private static List<String> catalog;

	private ItemValues() {}

	public static synchronized void invalidate() { prices = null; stacks = null; catalog = null; }

	public static boolean isBuiltInBlacklisted(ItemStack s) { return BaseValues.isBlacklisted(s); }

	public static boolean isBlacklisted(ItemStack s) {
		if (s.isEmpty() || BaseValues.isBlacklisted(s)) return true;
		String k = Ids.key(s);
		return UpgraderConfig.blacklist.contains(k) || UpgraderConfig.blacklist.contains(Ids.baseId(k));
	}

	private static double rarityFallback(ItemStack s) {
		switch (s.getRarity()) {
			case UNCOMMON: return 150;
			case RARE: return 600;
			case EPIC: return 2500;
			default: return 3;
		}
	}

	private static double mult(String key) {
		Double d = UpgraderConfig.valueMultipliers.get(key);
		if (d == null) d = UpgraderConfig.valueMultipliers.get(Ids.baseId(key));
		return d == null ? 1.0 : d;
	}

	public static synchronized double unitValue(ItemStack s) {
		if (s.isEmpty()) return 0;
		Map<String, Double> t = table();
		String k = Ids.key(s);
		Double raw = t.get(k);
		double v = raw != null ? raw : rarityFallback(s);
		return v * UpgraderConfig.globalValueMultiplier * mult(k);
	}

	public static double stackValue(ItemStack s) {
		if (s.isEmpty()) return 0;
		double v = unitValue(s) * s.getCount();
		if (s.isItemStackDamageable() && s.getMaxDamage() > 0) {
			double left = 1.0 - (double) s.getItemDamage() / (double) s.getMaxDamage();
			v *= Math.max(0.05, left);
		}
		return v;
	}

	/** All choosable item keys, cheapest first. */
	public static synchronized List<String> catalog() {
		if (catalog == null) {
			table();
			final List<String> l = new ArrayList<String>();
			for (Map.Entry<String, ItemStack> e : stacks.entrySet()) if (!isBlacklisted(e.getValue())) l.add(e.getKey());
			final Map<String, Double> v = new HashMap<String, Double>();
			for (String k : l) v.put(k, unitValue(stacks.get(k)));
			Collections.sort(l, new Comparator<String>() {
				public int compare(String a, String b) {
					int c = Double.compare(v.get(a), v.get(b));
					return c != 0 ? c : a.compareTo(b);
				}
			});
			catalog = l;
		}
		return catalog;
	}

	private static Map<String, Double> table() {
		if (prices == null) compute();
		return prices;
	}

	private static void compute() {
		Map<String, Double> p = new HashMap<String, Double>();
		Set<String> anchors = new HashSet<String>();
		Map<String, ItemStack> all = new LinkedHashMap<String, ItemStack>();
		for (Item item : Item.REGISTRY) {
			if (item == Items.AIR) continue;
			NonNullList<ItemStack> sub = NonNullList.create();
			try { item.getSubItems(CreativeTabs.SEARCH, sub); } catch (Throwable ignored) { }
			for (ItemStack s : sub) {
				if (s.isEmpty()) continue;
				String k = Ids.key(s);
				if (k.isEmpty() || all.containsKey(k)) continue;
				all.put(k, new ItemStack(s.getItem(), 1, s.getMetadata()));
			}
		}
		for (Map.Entry<String, ItemStack> e : all.entrySet()) {
			Double o = UpgraderConfig.valueOverrides.get(e.getKey());
			if (o == null) o = UpgraderConfig.valueOverrides.get(Ids.baseId(e.getKey()));
			if (o == null) o = BaseValues.get(e.getValue());
			if (o != null) { p.put(e.getKey(), o); anchors.add(e.getKey()); }
		}
		List<Recipe> recipes = normalize();
		relax(recipes, p, anchors);
		for (int round = 0; round < MAX_FLOOR_ROUNDS; round++) {
			if (conversionFloors(recipes, p, anchors, all) == 0) break;
			relax(recipes, p, anchors);
		}
		stacks = all;
		prices = p;
	}

	private static void relax(List<Recipe> recipes, Map<String, Double> p, Set<String> anchors) {
		for (int pass = 0; pass < MAX_PASSES; pass++) {
			boolean changed = false;
			for (Recipe r : recipes) {
				if (anchors.contains(r.result)) continue;
				double c = r.cost(p);
				if (Double.isInfinite(c)) continue;
				c = Math.max(1.0, c);
				Double old = p.get(r.result);
				if (old == null || c < old - 1.0E-6) { p.put(r.result, c); changed = true; }
			}
			if (!changed) return;
		}
	}

	/** Prices the single unpriced ingredient of a recipe whose result is already priced. */
	private static int conversionFloors(List<Recipe> recipes, Map<String, Double> p, Set<String> anchors, Map<String, ItemStack> all) {
		Map<String, Double> floors = new HashMap<String, Double>();
		for (Recipe r : recipes) {
			Double res = p.get(r.result);
			if (res == null) continue;
			double total = res * r.count, known = 0;
			Map<String, Integer> unknown = new HashMap<String, Integer>();
			boolean ok = true;
			for (List<String> g : r.groups) {
				if (g.size() == 1) {
					Double v = p.get(g.get(0));
					if (v != null) known += v; else Map_inc(unknown, g.get(0));
				} else {
					double best = Double.POSITIVE_INFINITY;
					for (String k : g) { Double v = p.get(k); if (v != null && v < best) best = v; }
					if (Double.isInfinite(best)) { ok = false; break; }
					known += best;
				}
			}
			if (!ok || unknown.isEmpty()) continue;
			int n = 0;
			for (int c : unknown.values()) n += c;
			double each = (total - known) / n;
			for (String k : unknown.keySet()) {
				ItemStack st = all.get(k);
				double min = st == null ? 3.0 : rarityFallback(st);
				if (each > min + 1.0E-6) {
					Double old = floors.get(k);
					if (old == null || each > old) floors.put(k, each);
				}
			}
		}
		for (Map.Entry<String, Double> e : floors.entrySet()) { p.put(e.getKey(), e.getValue()); anchors.add(e.getKey()); }
		return floors.size();
	}

	private static void Map_inc(Map<String, Integer> m, String k) {
		Integer c = m.get(k);
		m.put(k, c == null ? 1 : c + 1);
	}

	private static List<Recipe> normalize() {
		List<Recipe> out = new ArrayList<Recipe>();
		for (IRecipe r : ForgeRegistries.RECIPES.getValuesCollection()) {
			try {
				ItemStack res = r.getRecipeOutput();
				if (res.isEmpty()) continue;
				List<List<String>> groups = new ArrayList<List<String>>();
				for (Ingredient ing : r.getIngredients()) {
					List<String> opts = options(ing);
					if (!opts.isEmpty()) groups.add(opts);
				}
				if (!groups.isEmpty()) out.add(new Recipe(Ids.key(res), Math.max(1, res.getCount()), groups));
			} catch (RuntimeException ignored) {
			}
		}
		for (Map.Entry<ItemStack, ItemStack> e : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
			ItemStack in = e.getKey(), res = e.getValue();
			if (in.isEmpty() || res.isEmpty()) continue;
			List<String> opts = expand(in);
			if (opts.isEmpty()) continue;
			List<List<String>> groups = new ArrayList<List<String>>();
			groups.add(opts);
			out.add(new Recipe(Ids.key(res), Math.max(1, res.getCount()), groups));
		}
		return out;
	}

	private static List<String> options(Ingredient ing) {
		Set<String> s = new LinkedHashSet<String>();
		for (ItemStack st : ing.getMatchingStacks()) s.addAll(expand(st));
		return new ArrayList<String>(s);
	}

	private static List<String> expand(ItemStack st) {
		List<String> l = new ArrayList<String>();
		if (st.isEmpty()) return l;
		if (st.getMetadata() == OreDictionary.WILDCARD_VALUE && st.getItem().getHasSubtypes()) {
			NonNullList<ItemStack> sub = NonNullList.create();
			try { st.getItem().getSubItems(CreativeTabs.SEARCH, sub); } catch (Throwable ignored) { }
			for (ItemStack s : sub) { String k = Ids.key(s); if (!k.isEmpty() && !l.contains(k)) l.add(k); }
		} else {
			String k = Ids.key(st);
			if (!k.isEmpty()) l.add(k);
		}
		return l;
	}
}
