package net.execheinz.upgrader;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/** Upgrader, backported to Minecraft 1.12.2 Forge. Gambling-style item upgrader: risk an item for a better one. */
@Mod(modid = UpgraderMod.MODID, name = "Upgrader", version = "1.3.0-1.12.2", acceptedMinecraftVersions = "[1.12.2]")
@Mod.EventBusSubscriber(modid = UpgraderMod.MODID)
public class UpgraderMod {
	public static final String MODID = "upgrader";

	@Mod.Instance(MODID)
	public static UpgraderMod instance;

	@SidedProxy(clientSide = "net.execheinz.upgrader.client.ClientProxy", serverSide = "net.execheinz.upgrader.CommonProxy")
	public static CommonProxy proxy;

	public static final SimpleNetworkWrapper NET = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
	public static final CreativeTabs TAB = new CreativeTabs("upgrader.main") {
		@Override public ItemStack createIcon() { return new ItemStack(item); }
	};

	public static final Item item = new UpgraderItem();

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		UpgraderConfig.load(e.getSuggestedConfigurationFile());
		Net.register();
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new IGuiHandler() {
			public Object getServerGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
				return new UpgraderContainer(p.inventory);
			}
			public Object getClientGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
				return new net.execheinz.upgrader.client.GuiUpgrader(new UpgraderContainer(p.inventory));
			}
		});
	}

	@Mod.EventHandler
	public void serverStarting(FMLServerStartingEvent e) {
		ItemValues.invalidate();
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> e) {
		e.getRegistry().register(item);
	}
}
