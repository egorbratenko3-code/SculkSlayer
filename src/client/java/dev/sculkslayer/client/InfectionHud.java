package dev.sculkslayer.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Top-right overlay: infected %, stage and monolith coordinates. */
public final class InfectionHud {
	public static void render(GuiGraphics graphics, DeltaTracker tracker) {
		if (!HudData.active) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui) return;
		Font font = mc.font;

		String[] lines;
		int color = HudData.cleansed ? 0xFF7CFF9B : 0xFF7FE9FF;
		if (HudData.cleansed) {
			lines = new String[] {"world cleansed", "monolite in: " + HudData.monolith};
		} else if (HudData.core.isEmpty()) {
			lines = new String[] {"infected: " + HudData.infected + "%", "stage: " + HudData.stage, "monolite in: " + HudData.monolith};
		} else {
			lines = new String[] {"infected: " + HudData.infected + "%", "stage: " + HudData.stage, "monolite in: " + HudData.monolith, "CORE: " + HudData.core};
		}

		int y = 6;
		for (int i = 0; i < lines.length; i++) {
			int w = font.width(lines[i]);
			int c = (i == 3) ? 0xFFFF5555 : color;
			graphics.drawString(font, lines[i], graphics.guiWidth() - w - 6, y, c, true);
			y += 11;
		}
	}

	private InfectionHud() {}
}
