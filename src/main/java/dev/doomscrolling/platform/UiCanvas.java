package dev.doomscrolling.platform;

import net.minecraft.client.gui.Font;

public interface UiCanvas {

	enum CursorKind {
		DEFAULT,
		EW,
		NS,
		NWSE,
		NESW,
		MOVE
	}

	int guiWidth();

	int guiHeight();

	void fill(int x1, int y1, int x2, int y2, int argb);

	void drawText(Font font, String text, int x, int y, int argb, boolean shadow);

	void enableScissor(int x1, int y1, int x2, int y2);

	void disableScissor();

	void blitTexture(Object textureId, int x, int y, int w, int h);

	void blitCenteredScaled(Object textureId, float cx, float cy, float size, int argbTint);

	void requestCursor(CursorKind kind);

	default void outline(int x, int y, int w, int h, int argb) {
		fill(x, y, x + w, y + 1, argb);
		fill(x, y + h - 1, x + w, y + h, argb);
		fill(x, y + 1, x + 1, y + h - 1, argb);
		fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
	}

	default void drawCenteredText(Font font, String text, int cx, int y, int argb) {
		drawText(font, text, cx - font.width(text) / 2, y, argb, false);
	}
}
