package net.execheinz.upgrader.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.execheinz.upgrader.Ids;
import net.execheinz.upgrader.Net;
import net.execheinz.upgrader.UpgraderContainer;
import net.execheinz.upgrader.UpgraderMod;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiUpgrader extends GuiContainer {
	private static final int BTN_MINUS = 0, BTN_PLUS = 1, BTN_UPGRADE = 2;
	private static final int PICK_W = 178, PICK_H = 150, COLS = 9, ROWS = 6, CELL = 18;

	private final UpgraderContainer menu;
	private GuiButton minus, plus, upgrade;
	private GuiTextField search;
	private boolean pickerOpen;
	private int scrollRow;
	private List<String> filtered = new ArrayList<String>();
	private String lastQuery = null;
	private float idleAngle;

	public GuiUpgrader(UpgraderContainer menu) {
		super(menu);
		this.menu = menu;
		this.xSize = 176;
		this.ySize = 226;
	}

	@Override
	public void initGui() {
		super.initGui();
		minus = new GuiButton(BTN_MINUS, guiLeft + 44, guiTop + 104, 16, 16, "-");
		plus = new GuiButton(BTN_PLUS, guiLeft + 116, guiTop + 104, 16, 16, "+");
		upgrade = new GuiButton(BTN_UPGRADE, guiLeft + 38, guiTop + 122, 100, 18, I18n.format("gui.upgrader.upgrade"));
		buttonList.add(minus);
		buttonList.add(plus);
		buttonList.add(upgrade);
		search = new GuiTextField(10, fontRenderer, guiLeft + (xSize - PICK_W) / 2 + 6, guiTop + 14, PICK_W - 12, 14);
		search.setMaxStringLength(64);
		Keyboard.enableRepeatEvents(true);
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		Keyboard.enableRepeatEvents(false);
	}

	public void onCatalogChanged() { lastQuery = null; }

	// ---- logic -----------------------------------------------------------------------------

	@Override
	public void updateScreen() {
		super.updateScreen();
		if (search != null) search.updateCursorCounter();
		boolean hasBoth = !menu.getInputStack().isEmpty() && !menu.getTargetStack().isEmpty();
		upgrade.enabled = hasBoth && !menu.spinning && menu.cooldown <= 0;
		upgrade.displayString = menu.cooldown > 0 ? I18n.format("gui.upgrader.cooldown", menu.cooldown) : I18n.format("gui.upgrader.upgrade");
		minus.enabled = plus.enabled = !menu.spinning && !menu.getTargetStack().isEmpty();
	}

	@Override
	protected void actionPerformed(GuiButton b) throws IOException {
		int step = isShiftKeyDown() ? 10 : 1;
		if (b.id == BTN_MINUS) UpgraderMod.NET.sendToServer(new Net.SetCountMsg(Math.max(1, menu.targetCount - step)));
		else if (b.id == BTN_PLUS) UpgraderMod.NET.sendToServer(new Net.SetCountMsg(Math.min(menu.maxCount, menu.targetCount + step)));
		else if (b.id == BTN_UPGRADE) UpgraderMod.NET.sendToServer(new Net.StartMsg());
	}

	private void refilter() {
		String q = search.getText().trim().toLowerCase();
		if (q.equals(lastQuery)) return;
		lastQuery = q;
		filtered.clear();
		for (String k : ClientProxy.catalogKeys) {
			if (q.isEmpty() || k.toLowerCase().contains(q)) { filtered.add(k); continue; }
			ItemStack s = Ids.stack(k);
			if (!s.isEmpty() && s.getDisplayName().toLowerCase().contains(q)) filtered.add(k);
		}
		scrollRow = 0;
	}

	private int pickX() { return guiLeft + (xSize - PICK_W) / 2; }
	private int pickY() { return guiTop + 8; }
	private int gridX() { return pickX() + 8; }
	private int gridY() { return pickY() + 36; }
	private int totalRows() { return Math.max(1, (filtered.size() + COLS - 1) / COLS); }

	private int cellAt(int mx, int my) {
		int cx = (mx - gridX()) / CELL, cy = (my - gridY()) / CELL;
		if (mx < gridX() || my < gridY() || cx >= COLS || cy >= ROWS) return -1;
		int i = (scrollRow + cy) * COLS + cx;
		return i < filtered.size() ? i : -1;
	}

	private boolean overTarget(int mx, int my) {
		return mx >= guiLeft + 141 && mx < guiLeft + 159 && my >= guiTop + 44 && my < guiTop + 62;
	}

	// ---- input -----------------------------------------------------------------------------

	@Override
	protected void mouseClicked(int mx, int my, int btn) throws IOException {
		if (pickerOpen) {
			int px = pickX(), py = pickY();
			if (mx < px || mx >= px + PICK_W || my < py || my >= py + PICK_H) { pickerOpen = false; return; }
			search.mouseClicked(mx, my, btn);
			int i = cellAt(mx, my);
			if (i >= 0 && btn == 0) {
				UpgraderMod.NET.sendToServer(new Net.SetTargetMsg(filtered.get(i)));
				pickerOpen = false;
			}
			return;
		}
		if (btn == 0 && overTarget(mx, my) && !menu.spinning) {
			pickerOpen = true;
			lastQuery = null;
			search.setText("");
			search.setFocused(true);
			return;
		}
		super.mouseClicked(mx, my, btn);
	}

	@Override protected void renderHoveredToolTip(int x, int y) { if (!pickerOpen) super.renderHoveredToolTip(x, y); }

	@Override protected void mouseReleased(int mx, int my, int state) { if (!pickerOpen) super.mouseReleased(mx, my, state); }

	@Override protected void mouseClickMove(int mx, int my, int btn, long t) { if (!pickerOpen) super.mouseClickMove(mx, my, btn, t); }

	@Override protected void handleMouseClick(Slot s, int id, int btn, ClickType type) { if (!pickerOpen) super.handleMouseClick(s, id, btn, type); }

	@Override
	protected void keyTyped(char c, int key) throws IOException {
		if (pickerOpen) {
			if (key == Keyboard.KEY_ESCAPE) { pickerOpen = false; return; }
			search.textboxKeyTyped(c, key);
			return;
		}
		super.keyTyped(c, key);
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int w = Mouse.getEventDWheel();
		if (w != 0 && pickerOpen) {
			scrollRow = Math.max(0, Math.min(Math.max(0, totalRows() - ROWS), scrollRow + (w > 0 ? -1 : 1)));
		}
	}

	// ---- drawing ---------------------------------------------------------------------------

	private static String fmt(double v) {
		if (v >= 1000) return String.format("%,.0f", v);
		if (Math.abs(v - Math.rint(v)) < 1.0E-9) return String.valueOf((long) v);
		return String.format("%.1f", v);
	}

	private static String pct(double c) {
		double p = c * 100.0;
		return p < 0.1 && p > 0 ? String.format("%.2f%%", p) : String.format("%.1f%%", p);
	}

	private void fillArc(float cx, float cy, float r0, float r1, float a0, float a1, int argb) {
		float a = (argb >> 24 & 255) / 255f, r = (argb >> 16 & 255) / 255f, g = (argb >> 8 & 255) / 255f, b = (argb & 255) / 255f;
		if (a1 <= a0) return;
		GlStateManager.disableTexture2D();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		Tessellator t = Tessellator.getInstance();
		BufferBuilder bb = t.getBuffer();
		bb.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
		int steps = Math.max(1, (int) Math.ceil((a1 - a0) / 3.0f));
		for (int i = 0; i <= steps; i++) {
			double ang = Math.toRadians(a0 + (a1 - a0) * i / steps);
			float sx = (float) Math.sin(ang), sy = (float) -Math.cos(ang);
			bb.pos(cx + sx * r0, cy + sy * r0, 0).color(r, g, b, a).endVertex();
			bb.pos(cx + sx * r1, cy + sy * r1, 0).color(r, g, b, a).endVertex();
		}
		t.draw();
		GlStateManager.disableBlend();
		GlStateManager.enableTexture2D();
	}

	private float pointerAngle() {
		long now = System.currentTimeMillis();
		if (menu.spinning && menu.resultEndMs > now) {
			float p = Math.max(0f, Math.min(1f, (now - menu.resultStartMs) / (float) Math.max(1L, menu.resultEndMs - menu.resultStartMs)));
			float eased = 1f - (1f - p) * (1f - p) * (1f - p);
			return menu.resultAngle - (1f - eased) * 360f * 4f;
		}
		if (menu.resultEndMs > 0) idleAngle = menu.resultAngle;
		return idleAngle;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
		int x = guiLeft, y = guiTop;
		drawRect(x, y, x + xSize, y + ySize, 0xFF1B1B26);
		drawRect(x + 1, y + 1, x + xSize - 1, y + ySize - 1, 0xFF2A2A3A);
		drawRect(x + 4, y + 4, x + xSize - 4, y + 140, 0xFF20202D);
		// slot frames
		for (Slot s : inventorySlots.inventorySlots) drawRect(x + s.xPos - 1, y + s.yPos - 1, x + s.xPos + 17, y + s.yPos + 17, 0xFF12121A);
		for (Slot s : inventorySlots.inventorySlots) drawRect(x + s.xPos, y + s.yPos, x + s.xPos + 16, y + s.yPos + 16, 0xFF3A3A4C);
		// target box
		drawRect(x + 141, y + 44, x + 159, y + 62, overTarget(mx, my) && !menu.spinning ? 0xFF8A8AFF : 0xFF12121A);
		drawRect(x + 142, y + 45, x + 158, y + 61, 0xFF3A3A4C);

		// wheel
		float cx = x + 88, cy = y + 52, r0 = 20, r1 = 32;
		boolean live = menu.chance > 0 && !menu.getInputStack().isEmpty() && !menu.getTargetStack().isEmpty();
		if (live) {
			float a = (float) (menu.chance * 360.0);
			fillArc(cx, cy, r0, r1, 0, a, 0xFF3FBF5F);
			fillArc(cx, cy, r0, r1, a, 360, 0xFFBF3F4F);
		} else {
			fillArc(cx, cy, r0, r1, 0, 360, 0xFF55556A);
		}
		float ang = pointerAngle();
		fillArc(cx, cy, 0, r1 + 4, ang - 1.4f, ang + 1.4f, 0xFFFFFFFF);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mx, int my) {
		fontRenderer.drawString(I18n.format("menu.upgrader.title"), 8, 7, 0xFFD2A6);
		ItemStack tgt = menu.getTargetStack();
		// target item
		if (!tgt.isEmpty()) {
			RenderHelper.enableGUIStandardItemLighting();
			itemRender.renderItemAndEffectIntoGUI(tgt, 142, 45);
			RenderHelper.disableStandardItemLighting();
		}
		if (menu.getInputStack().isEmpty() && !pickerOpen)
			drawCenteredString(fontRenderer, I18n.format("gui.upgrader.pick_input"), 88, 66, 0x9A9AB0);
		if (tgt.isEmpty() && !pickerOpen) {
			// hint under the target box
		}
		int chanceColor = menu.chance >= 0.5 ? 0x5FE07F : menu.chance >= 0.15 ? 0xE0C85F : 0xE05F6F;
		String chanceText = menu.chance > 0 ? I18n.format("gui.upgrader.estimate", pct(menu.chance)) : I18n.format("gui.upgrader.pick_target");
		drawCenteredString(fontRenderer, chanceText, 88, 88, menu.chance > 0 ? chanceColor : 0x9A9AB0);
		if (!tgt.isEmpty()) {
			String v = I18n.format("gui.upgrader.value", fmt(menu.inputValue) + " → " + fmt(menu.targetUnitValue * menu.targetCount));
			drawCenteredString(fontRenderer, v, 88, 97, 0xB8B8D0);
			drawCenteredString(fontRenderer, I18n.format("gui.upgrader.amount") + ": " + menu.targetCount, 88, 108, 0xFFFFFF);
		}
		// result banner
		long now = System.currentTimeMillis();
		if (menu.resultEndMs > 0 && now > menu.resultEndMs && now < menu.resultEndMs + 4000 && !menu.spinning) {
			drawCenteredString(fontRenderer, I18n.format(menu.resultWin ? "gui.upgrader.success" : "gui.upgrader.failure"), 88, 49,
					menu.resultWin ? 0x5FE07F : 0xE05F6F);
		} else if (menu.spinning) {
			drawCenteredString(fontRenderer, I18n.format("gui.upgrader.spinning"), 88, 49, 0xFFFFFF);
		}
	}

	@Override
	public void drawScreen(int mx, int my, float pt) {
		drawDefaultBackground();
		super.drawScreen(mx, my, pt);
		if (!pickerOpen) {
			if (!menu.getTargetStack().isEmpty() && overTarget(mx, my)) renderToolTip(menu.getTargetStack(), mx, my);
			return;
		}
		refilter();
		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 0, 400);
		int px = pickX(), py = pickY();
		drawRect(px - 1, py - 1, px + PICK_W + 1, py + PICK_H + 1, 0xFF000000);
		drawRect(px, py, px + PICK_W, py + PICK_H, 0xFF2A2A3A);
		drawCenteredString(fontRenderer, I18n.format("gui.upgrader.pick_target"), px + PICK_W / 2, py + 4, 0xFFD2A6);
		search.drawTextBox();
		if (search.getText().isEmpty() && !search.isFocused()) fontRenderer.drawString(I18n.format("gui.upgrader.search"), search.x + 4, search.y + 3, 0x707080);
		int hover = cellAt(mx, my);
		RenderHelper.enableGUIStandardItemLighting();
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				int i = (scrollRow + r) * COLS + c;
				if (i >= filtered.size()) break;
				int ix = gridX() + c * CELL, iy = gridY() + r * CELL;
				drawRect(ix, iy, ix + CELL - 1, iy + CELL - 1, i == hover ? 0xFF6A6AA0 : 0xFF3A3A4C);
				itemRender.renderItemAndEffectIntoGUI(Ids.stack(filtered.get(i)), ix + 1, iy + 1);
			}
		}
		RenderHelper.disableStandardItemLighting();
		String footer = I18n.format("gui.upgrader.picker_footer", filtered.size(), Math.min(totalRows(), scrollRow + 1), totalRows());
		fontRenderer.drawString(footer, px + 8, py + PICK_H - 11, 0x9A9AB0);
		if (hover >= 0) {
			String key = filtered.get(hover);
			ItemStack s = Ids.stack(key);
			List<String> tip = new ArrayList<String>();
			tip.add(s.getDisplayName());
			Double v = ClientProxy.catalogValues.get(key);
			if (v != null) tip.add("§7" + I18n.format("gui.upgrader.value", fmt(v)));
			drawHoveringText(tip, mx, my);
		}
		GlStateManager.popMatrix();
	}
}
