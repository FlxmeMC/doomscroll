package dev.doomscrolling.platform;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

final class Mc12111Canvas implements UiCanvas {

	private static CursorType cursorNwse;
	private static CursorType cursorNesw;

	private final GuiGraphics g;

	Mc12111Canvas(GuiGraphics g) {
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
	public void drawText(Font font, String text, int x, int y, int argb, boolean shadow) {
		g.drawString(font, text, x, y, argb, shadow);
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
			case NWSE -> g.requestCursor(resizeNwseCursor());
			case NESW -> g.requestCursor(resizeNeswCursor());
			case MOVE -> g.requestCursor(CursorTypes.RESIZE_ALL);
			case DEFAULT -> {
			}
		}
	}

	private static CursorType resizeNwseCursor() {
		if (cursorNwse == null) {
			cursorNwse = CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR, "doomscrolling_nwse", CursorTypes.RESIZE_ALL);
		}
		return cursorNwse;
	}

	private static CursorType resizeNeswCursor() {
		if (cursorNesw == null) {
			cursorNesw = CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR, "doomscrolling_nesw", CursorTypes.RESIZE_ALL);
		}
		return cursorNesw;
	}
}
