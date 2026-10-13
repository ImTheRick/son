package net.execheinz.upgrader;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;

public class UpgraderContainer extends Container {
	public static final int INPUT_SLOT = 0;

	public final EntityPlayer player;
	public final InventoryBasic input = new InventoryBasic("upgrader", false, 1);

	// State shared by both sides (client copies are filled in by SyncMsg / ResultMsg).
	public String targetKey = "";
	public int targetCount = 1;
	public boolean spinning;
	public double inputValue, targetUnitValue, chance;
	public int cooldown, maxCount = 64;
	// Client-only animation data.
	public boolean resultWin;
	public float resultAngle;
	public int resultTicks;
	public long resultStartMs, resultEndMs;

	// Server-only.
	private int spinTicksLeft;
	private boolean pendingWin;
	private boolean catalogSent;
	private boolean synced;
	private String lastTarget = "";
	private long lastIn = -1, lastTgt = -1;
	private int lastCount = -1, lastCooldown = -1;
	private boolean lastSpinning;

	public UpgraderContainer(InventoryPlayer inv) {
		this.player = inv.player;
		addSlotToContainer(new Slot(input, INPUT_SLOT, 17, 45) {
			@Override public boolean isItemValid(ItemStack s) { return !spinning && !ItemValues.isBlacklisted(s); }
			@Override public boolean canTakeStack(EntityPlayer p) { return !spinning; }
		});
		for (int r = 0; r < 3; r++)
			for (int c = 0; c < 9; c++)
				addSlotToContainer(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 144 + r * 18));
		for (int c = 0; c < 9; c++)
			addSlotToContainer(new Slot(inv, c, 8 + c * 18, 202));
	}

	public ItemStack getInputStack() { return input.getStackInSlot(INPUT_SLOT); }

	public ItemStack getTargetStack() { return Ids.stack(targetKey); }

	private int clampCount(int n) {
		ItemStack t = getTargetStack();
		int max = Math.max(1, Math.min(UpgraderConfig.maxTargetCount, t.isEmpty() ? 64 : t.getMaxStackSize()));
		return Math.max(1, Math.min(max, n));
	}

	// ---- server actions -------------------------------------------------------------------

	public void setTarget(EntityPlayerMP p, String key) {
		if (spinning) return;
		ItemStack s = Ids.stack(key);
		if (s.isEmpty() || ItemValues.isBlacklisted(s)) return;
		targetKey = Ids.key(s);
		targetCount = clampCount(targetCount);
		sync(p, false);
	}

	public void setCount(EntityPlayerMP p, int n) {
		if (spinning) return;
		targetCount = clampCount(n);
		sync(p, false);
	}

	public void startUpgrade(EntityPlayerMP p) {
		if (spinning) return;
		ItemStack in = getInputStack();
		ItemStack tgt = getTargetStack();
		if (in.isEmpty() || tgt.isEmpty()) return;
		if (ItemValues.isBlacklisted(in) || ItemValues.isBlacklisted(tgt)) return;
		if (AttemptLimiter.cooldownRemaining(p) > 0) { sync(p, true); return; }
		AttemptLimiter.record(p);
		targetCount = clampCount(targetCount);
		double c = UpgraderOdds_chance(in, tgt);
		pendingWin = p.getRNG().nextDouble() < c;
		spinning = true;
		spinTicksLeft = UpgraderConfig.spinTicks;
		float angle = landingAngle(p.getRNG(), c, pendingWin);
		UpgraderMod.NET.sendTo(new Net.ResultMsg(pendingWin, angle, spinTicksLeft, (float) c), p);
		sync(p, true);
	}

	private double UpgraderOdds_chance(ItemStack in, ItemStack tgt) {
		return UpgradeOdds.chance(in, tgt, targetCount);
	}

	private static float landingAngle(java.util.Random r, double chance, boolean win) {
		float a = (float) (chance * 360.0);
		float w = win ? a : 360.0f - a;
		float margin = Math.min(1.5f, w * 0.2f);
		float span = Math.max(0.01f, w - margin * 2.0f);
		float off = margin + r.nextFloat() * span;
		return win ? off : (a + off) % 360.0f;
	}

	private void applyResult() {
		spinning = false;
		if (getInputStack().isEmpty()) return;
		ItemStack tgt = getTargetStack();
		if (pendingWin && !tgt.isEmpty()) {
			tgt.setCount(clampCount(targetCount));
			input.setInventorySlotContents(INPUT_SLOT, tgt);
		} else {
			input.setInventorySlotContents(INPUT_SLOT, ItemStack.EMPTY);
		}
		input.markDirty();
		if (player instanceof EntityPlayerMP) {
			player.world.playSound(null, player.getPosition(),
					pendingWin ? SoundEvents.ENTITY_PLAYER_LEVELUP : SoundEvents.BLOCK_ANVIL_LAND,
					SoundCategory.PLAYERS, 0.7f, pendingWin ? 1.4f : 0.8f);
		}
	}

	@Override
	public void detectAndSendChanges() {
		if (player instanceof EntityPlayerMP && !player.world.isRemote) {
			EntityPlayerMP p = (EntityPlayerMP) player;
			if (!catalogSent) {
				catalogSent = true;
				UpgraderMod.NET.sendTo(new Net.CatalogMsg(ItemValues.catalog()), p);
			}
			if (spinTicksLeft > 0 && --spinTicksLeft == 0) {
				applyResult();
				sync(p, true);
			}
			sync(p, false);
		}
		super.detectAndSendChanges();
	}

	private void sync(EntityPlayerMP p, boolean force) {
		ItemStack in = getInputStack(), tgt = getTargetStack();
		inputValue = ItemValues.stackValue(in);
		targetUnitValue = tgt.isEmpty() ? 0 : ItemValues.unitValue(tgt);
		chance = UpgradeOdds.chance(inputValue, targetUnitValue * targetCount);
		cooldown = AttemptLimiter.cooldownRemaining(p);
		maxCount = Math.max(1, Math.min(UpgraderConfig.maxTargetCount, tgt.isEmpty() ? 64 : tgt.getMaxStackSize()));
		long li = Math.round(inputValue * 100), lt = Math.round(targetUnitValue * 100);
		if (!force && synced && targetKey.equals(lastTarget) && li == lastIn && lt == lastTgt && targetCount == lastCount
				&& cooldown == lastCooldown && spinning == lastSpinning) return;
		synced = true;
		lastTarget = targetKey; lastIn = li; lastTgt = lt; lastCount = targetCount; lastCooldown = cooldown; lastSpinning = spinning;
		UpgraderMod.NET.sendTo(new Net.SyncMsg(targetKey, inputValue, targetUnitValue, targetCount, cooldown, chance, spinning, maxCount), p);
	}

	// ---- container plumbing ----------------------------------------------------------------

	@Override
	public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer p) {
		if (spinning) return ItemStack.EMPTY;
		return super.slotClick(slotId, dragType, clickType, p);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer p, int index) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = inventorySlots.get(index);
		if (slot != null && slot.getHasStack() && !spinning) {
			ItemStack s = slot.getStack();
			result = s.copy();
			boolean moved;
			if (index == INPUT_SLOT) moved = mergeItemStack(s, 1, 37, true);
			else moved = mergeItemStack(s, INPUT_SLOT, INPUT_SLOT + 1, false);
			if (!moved) return ItemStack.EMPTY;
			if (s.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
			if (s.getCount() == result.getCount()) return ItemStack.EMPTY;
			slot.onTake(p, s);
		}
		return result;
	}

	@Override
	public void onContainerClosed(EntityPlayer p) {
		super.onContainerClosed(p);
		if (!p.world.isRemote) {
			if (spinning) { spinTicksLeft = 0; applyResult(); }
			ItemStack s = input.removeStackFromSlot(INPUT_SLOT);
			if (!s.isEmpty() && !p.inventory.addItemStackToInventory(s)) p.dropItem(s, false);
		}
	}

	@Override
	public boolean canInteractWith(EntityPlayer p) { return true; }
}
