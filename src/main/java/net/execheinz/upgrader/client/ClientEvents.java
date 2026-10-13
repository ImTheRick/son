package net.execheinz.upgrader.client;

import net.execheinz.upgrader.UpgraderMod;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = UpgraderMod.MODID, value = Side.CLIENT)
public class ClientEvents {
	@SubscribeEvent
	public static void models(ModelRegistryEvent e) {
		ModelLoader.setCustomModelResourceLocation(UpgraderMod.item, 0,
				new ModelResourceLocation(UpgraderMod.item.getRegistryName(), "inventory"));
	}
}
