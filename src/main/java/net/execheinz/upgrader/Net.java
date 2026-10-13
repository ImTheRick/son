package net.execheinz.upgrader;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/** Network messages. Client-bound handlers delegate to the sided proxy so no client class is loaded on a server. */
public final class Net {
	private Net() {}

	public static void register() {
		int id = 0;
		UpgraderMod.NET.registerMessage(SetTargetHandler.class, SetTargetMsg.class, id++, Side.SERVER);
		UpgraderMod.NET.registerMessage(SetCountHandler.class, SetCountMsg.class, id++, Side.SERVER);
		UpgraderMod.NET.registerMessage(StartHandler.class, StartMsg.class, id++, Side.SERVER);
		UpgraderMod.NET.registerMessage(CatalogHandler.class, CatalogMsg.class, id++, Side.CLIENT);
		UpgraderMod.NET.registerMessage(SyncHandler.class, SyncMsg.class, id++, Side.CLIENT);
		UpgraderMod.NET.registerMessage(ResultHandler.class, ResultMsg.class, id++, Side.CLIENT);
	}

	private static UpgraderContainer open(EntityPlayerMP p) {
		return p.openContainer instanceof UpgraderContainer ? (UpgraderContainer) p.openContainer : null;
	}

	// ---- client -> server ------------------------------------------------------------------

	public static class SetTargetMsg implements IMessage {
		public String key = "";
		public SetTargetMsg() {}
		public SetTargetMsg(String key) { this.key = key; }
		public void fromBytes(ByteBuf b) { key = ByteBufUtils.readUTF8String(b); }
		public void toBytes(ByteBuf b) { ByteBufUtils.writeUTF8String(b, key); }
	}

	public static class SetTargetHandler implements IMessageHandler<SetTargetMsg, IMessage> {
		public IMessage onMessage(final SetTargetMsg m, MessageContext ctx) {
			final EntityPlayerMP p = ctx.getServerHandler().player;
			p.getServerWorld().addScheduledTask(new Runnable() {
				public void run() { UpgraderContainer c = open(p); if (c != null) c.setTarget(p, m.key); }
			});
			return null;
		}
	}

	public static class SetCountMsg implements IMessage {
		public int count;
		public SetCountMsg() {}
		public SetCountMsg(int count) { this.count = count; }
		public void fromBytes(ByteBuf b) { count = b.readInt(); }
		public void toBytes(ByteBuf b) { b.writeInt(count); }
	}

	public static class SetCountHandler implements IMessageHandler<SetCountMsg, IMessage> {
		public IMessage onMessage(final SetCountMsg m, MessageContext ctx) {
			final EntityPlayerMP p = ctx.getServerHandler().player;
			p.getServerWorld().addScheduledTask(new Runnable() {
				public void run() { UpgraderContainer c = open(p); if (c != null) c.setCount(p, m.count); }
			});
			return null;
		}
	}

	public static class StartMsg implements IMessage {
		public StartMsg() {}
		public void fromBytes(ByteBuf b) {}
		public void toBytes(ByteBuf b) {}
	}

	public static class StartHandler implements IMessageHandler<StartMsg, IMessage> {
		public IMessage onMessage(StartMsg m, MessageContext ctx) {
			final EntityPlayerMP p = ctx.getServerHandler().player;
			p.getServerWorld().addScheduledTask(new Runnable() {
				public void run() { UpgraderContainer c = open(p); if (c != null) c.startUpgrade(p); }
			});
			return null;
		}
	}

	// ---- server -> client ------------------------------------------------------------------

	public static class CatalogMsg implements IMessage {
		public List<String> keys = new ArrayList<String>();
		public List<Double> values = new ArrayList<Double>();
		public CatalogMsg() {}
		public CatalogMsg(List<String> keys) {
			this.keys = keys;
			for (String k : keys) values.add(ItemValues.unitValue(Ids.stack(k)));
		}
		public void fromBytes(ByteBuf b) {
			int n = b.readInt();
			for (int i = 0; i < n; i++) { keys.add(ByteBufUtils.readUTF8String(b)); values.add(b.readDouble()); }
		}
		public void toBytes(ByteBuf b) {
			b.writeInt(keys.size());
			for (int i = 0; i < keys.size(); i++) { ByteBufUtils.writeUTF8String(b, keys.get(i)); b.writeDouble(values.get(i)); }
		}
	}

	public static class CatalogHandler implements IMessageHandler<CatalogMsg, IMessage> {
		public IMessage onMessage(CatalogMsg m, MessageContext ctx) { UpgraderMod.proxy.onCatalog(m); return null; }
	}

	public static class SyncMsg implements IMessage {
		public String target = "";
		public double inputValue, targetUnit, chance;
		public int count, cooldown, maxCount;
		public boolean spinning;
		public SyncMsg() {}
		public SyncMsg(String target, double inputValue, double targetUnit, int count, int cooldown, double chance, boolean spinning, int maxCount) {
			this.target = target; this.inputValue = inputValue; this.targetUnit = targetUnit; this.count = count;
			this.cooldown = cooldown; this.chance = chance; this.spinning = spinning; this.maxCount = maxCount;
		}
		public void fromBytes(ByteBuf b) {
			target = ByteBufUtils.readUTF8String(b); inputValue = b.readDouble(); targetUnit = b.readDouble(); chance = b.readDouble();
			count = b.readInt(); cooldown = b.readInt(); maxCount = b.readInt(); spinning = b.readBoolean();
		}
		public void toBytes(ByteBuf b) {
			ByteBufUtils.writeUTF8String(b, target); b.writeDouble(inputValue); b.writeDouble(targetUnit); b.writeDouble(chance);
			b.writeInt(count); b.writeInt(cooldown); b.writeInt(maxCount); b.writeBoolean(spinning);
		}
	}

	public static class SyncHandler implements IMessageHandler<SyncMsg, IMessage> {
		public IMessage onMessage(SyncMsg m, MessageContext ctx) { UpgraderMod.proxy.onSync(m); return null; }
	}

	public static class ResultMsg implements IMessage {
		public boolean win;
		public float angle, chance;
		public int ticks;
		public ResultMsg() {}
		public ResultMsg(boolean win, float angle, int ticks, float chance) { this.win = win; this.angle = angle; this.ticks = ticks; this.chance = chance; }
		public void fromBytes(ByteBuf b) { win = b.readBoolean(); angle = b.readFloat(); ticks = b.readInt(); chance = b.readFloat(); }
		public void toBytes(ByteBuf b) { b.writeBoolean(win); b.writeFloat(angle); b.writeInt(ticks); b.writeFloat(chance); }
	}

	public static class ResultHandler implements IMessageHandler<ResultMsg, IMessage> {
		public IMessage onMessage(ResultMsg m, MessageContext ctx) { UpgraderMod.proxy.onResult(m); return null; }
	}
}
