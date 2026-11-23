package dev.doomscrolling.platform;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

final class Mc26Canvas implements UiCanvas {

	private final GuiGraphicsExtractor g;

	Mc26Canvas(GuiGraphicsExtractor g) {
		this.g = g;
	}

	@Override
	public int guiWidth() {
		return g.guiWidth();
	}

	@Override
	public int guiHeight() {
		return g.guiHeight();
	}

	@Override
	public void fill(int x1, int y1, int x2, int y2, int argb) {
		g.fill(x1, y1, x2, y2, argb);
	}

	@Override
	public void outline(int x, int y, int w, int h, int argb) {
		g.outline(x, y, w, h, argb);
	}

	@Override
	public void drawText(Font font, String text, int x, int y, int argb, boolean shadow) {
		g.text(font, text, x, y, argb, shadow);
	}

	@Override
	public void drawCenteredText(Font font, String text, int cx, int y, int argb) {
		g.centeredText(font, text, cx, y, argb);
	}

	@Override
	public void enableScissor(int x1, int y1, int x2, int y2) {
		g.enableScissor(x1, y1, x2, y2);
	}

	@Override
	public void disableScissor() {
		g.disableScissor();
	}

	@Override
	public void blitTexture(Object textureId, int x, int y, int w, int h) {
		if (textureId instanceof Identifier id) {
			g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0.0f, 0.0f, w, h, w, h);
		}
	}

	@Override
	public void blitCenteredScaled(Object textureId, float cx, float cy, float size, int argbTint) {
		if (!(textureId instanceof Identifier id) || size <= 1.0f) return;
		int base = 32;
		float scale = size / (float) base;
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().scale(scale, scale);
		g.blit(RenderPipelines.GUI_TEXTURED, id, -base / 2, -base / 2, 0.0f, 0.0f, base, base, base, base, argbTint);
		g.pose().popMatrix();
	}

	@Override
	public void requestCursor(CursorKind kind) {
		switch (kind) {
			case EW -> g.requestCursor(CursorTypes.RESIZE_EW);
			case NS -> g.requestCursor(CursorTypes.RESIZE_NS);
			case NWSE -> g.requestCursor(Mc26Shim.resizeNwseCursor());
			case NESW -> g.requestCursor(Mc26Shim.resizeNeswCursor());
			case MOVE -> g.requestCursor(CursorTypes.RESIZE_ALL);
			case DEFAULT -> {
			}
		}
	}
}
