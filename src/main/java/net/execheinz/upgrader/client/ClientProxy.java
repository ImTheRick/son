package net.execheinz.upgrader.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.execheinz.upgrader.CommonProxy;
import net.execheinz.upgrader.Net;
import net.execheinz.upgrader.UpgraderContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;

public class ClientProxy extends CommonProxy {
	/** Catalogue received from the server: keys cheapest-first plus unit values. */
	public static List<String> catalogKeys = new java.util.ArrayList<String>();
	public static Map<String, Double> catalogValues = new HashMap<String, Double>();

	private static UpgraderContainer current() {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player == null) return null;
		Container c = mc.player.openContainer;
		return c instanceof UpgraderContainer ? (UpgraderContainer) c : null;
	}

	@Override
	public void onCatalog(final Net.CatalogMsg m) {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			public void run() {
				catalogKeys = m.keys;
				Map<String, Double> v = new HashMap<String, Double>();
				for (int i = 0; i < m.keys.size(); i++) v.put(m.keys.get(i), m.values.get(i));
				catalogValues = v;
				Minecraft mc = Minecraft.getMinecraft();
				if (mc.currentScreen instanceof GuiUpgrader) ((GuiUpgrader) mc.currentScreen).onCatalogChanged();
			}
		});
	}

	@Override
	public void onSync(final Net.SyncMsg m) {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			public void run() {
				UpgraderContainer c = current();
				if (c == null) return;
				c.targetKey = m.target; c.inputValue = m.inputValue; c.targetUnitValue = m.targetUnit; c.targetCount = m.count;
				c.cooldown = m.cooldown; c.chance = m.chance; c.spinning = m.spinning; c.maxCount = m.maxCount;
			}
		});
	}

	@Override
	public void onResult(final Net.ResultMsg m) {
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			public void run() {
				UpgraderContainer c = current();
				if (c == null) return;
				c.resultWin = m.win; c.resultAngle = m.angle; c.resultTicks = m.ticks; c.chance = m.chance;
				c.resultStartMs = System.currentTimeMillis();
				c.resultEndMs = c.resultStartMs + m.ticks * 50L;
			}
		});
	}
}
