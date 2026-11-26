package dev.doomscrolling.ui;

import dev.doomscrolling.platform.UiCanvas;
import net.minecraft.client.gui.Font;

public final class LunarDraw {

	public static final int COLOR_YT_BLACK = 0xE80F0F0F;
	public static final int COLOR_SURFACE_ELEVATED = 0xE0212121;
	public static final int COLOR_SURFACE_HOVER = 0xF0323232;
	public static final int COLOR_OVERLAY_PILL = 0xB80F0F0F;
	public static final int COLOR_OVERLAY_PILL_HOVER = 0xE6272727;
	public static final int COLOR_BORDER_SUBTLE = 0x22FFFFFF;
	public static final int COLOR_BORDER_DARK = 0x77000000;
	public static final int COLOR_TRACK = 0x66484848;
	public static final int COLOR_TEXT_PRIMARY = 0xFFF1F1F1;
	public static final int COLOR_TEXT_SECONDARY = 0xFFAAAAAA;
	public static final int COLOR_TEXT_MUTED = 0xFF717171;
	public static final int COLOR_YT_RED = 0xFFFF0033;

	private LunarDraw() {
	}

	public static long nowMs() {
		return System.nanoTime() / 1_000_000L;
	}

	public static void drawGlassCard(UiCanvas g, int x, int y, int w, int h, int bgArgb, boolean highlightBorder) {
		g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x3D000000);
		g.fill(x, y, x + w, y + h, bgArgb);
		int topBorder = highlightBorder ? COLOR_YT_RED : COLOR_BORDER_SUBTLE;
		g.fill(x, y, x + w, y + 1, topBorder);
		g.fill(x, y + h - 1, x + w, y + h, highlightBorder ? COLOR_YT_RED : COLOR_BORDER_DARK);
		g.fill(x, y + 1, x + 1, y + h - 1, highlightBorder ? COLOR_YT_RED : 0x18FFFFFF);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, highlightBorder ? COLOR_YT_RED : 0x18FFFFFF);
	}

	public static void drawProgressBar(
			UiCanvas g,
			int x,
			int y,
			int w,
			int h,
			float progress,
			int accentArgb,
			boolean showKnob
	) {
		g.fill(x, y, x + w, y + h, COLOR_TRACK);
		float clamped = Math.max(0.0f, Math.min(1.0f, progress));
		int fillW = Math.round(w * clamped);
		if (fillW > 0) {
			g.fill(x, y, x + fillW, y + h, accentArgb);
		}
		if (showKnob) {
			int kx = Math.max(x, Math.min(x + w - 4, x + fillW - 2));
			g.fill(kx, y - 1, kx + 4, y + h + 1, 0xFFFFFFFF);
		}
	}

	public static void drawIndeterminateBar(UiCanvas g, int x, int y, int w, int h, int accentArgb) {
		g.fill(x, y, x + w, y + h, COLOR_TRACK);
		int segW = Math.max(18, w / 3);
		int travel = w + segW;
		int offset = (int) ((nowMs() / 5L) % travel) - segW;
		int start = Math.max(x, x + offset);
		int end = Math.min(x + w, x + offset + segW);
		if (end > start) {
			g.fill(start, y, end, y + h, accentArgb);
		}
	}

	public static void drawMarqueeText(
			UiCanvas g,
			Font font,
			String text,
			int x,
			int y,
			int maxWidth,
			int color,
			boolean shadow
	) {
		if (text == null || text.isEmpty() || maxWidth <= 4) {
			return;
		}
		int textW = font.width(text);
		if (textW <= maxWidth) {
			g.drawText(font, text, x, y, color, shadow);
			return;
		}

		int overflow = textW - maxWidth + 6;
		long cycleMs = 4200L + overflow * 35L;
		long phase = nowMs() % cycleMs;
		int offsetPx;
		if (phase < 1400L) {
			offsetPx = 0;
		} else if (phase < 1400L + overflow * 25L) {
			offsetPx = (int) ((phase - 1400L) / 25L);
		} else if (phase < 2800L + overflow * 25L) {
			offsetPx = overflow;
		} else {
			long backElapsed = phase - (2800L + overflow * 25L);
			offsetPx = Math.max(0, overflow - (int) (backElapsed / 10L));
		}

		g.enableScissor(x, y - 1, x + maxWidth, y + font.lineHeight + 1);
		g.drawText(font, text, x - offsetPx, y, color, shadow);
		g.disableScissor();
	}

	public static void drawOverlayIconButton(UiCanvas g, int x, int y, int size, boolean hovered, boolean active, int accentArgb) {
		int bg = active ? withAlpha(accentArgb, 0xCC) : (hovered ? COLOR_OVERLAY_PILL_HOVER : COLOR_OVERLAY_PILL);
		g.fill(x + 1, y, x + size - 1, y + size, bg);
		g.fill(x, y + 1, x + size, y + size - 1, bg);
		if (hovered) {
			g.fill(x + 1, y, x + size - 1, y + 1, 0x55FFFFFF);
		}
	}

	public static void drawPrevIcon(UiCanvas g, int cx, int cy, int color) {
		g.fill(cx - 4, cy - 3, cx - 3, cy + 4, color);
		g.fill(cx - 2, cy, cx - 1, cy + 1, color);
		g.fill(cx - 1, cy - 1, cx, cy + 2, color);
		g.fill(cx, cy - 2, cx + 1, cy + 3, color);
		g.fill(cx + 1, cy - 3, cx + 2, cy + 4, color);
	}

	public static void drawNextIcon(UiCanvas g, int cx, int cy, int color) {
		g.fill(cx - 2, cy - 3, cx - 1, cy + 4, color);
		g.fill(cx - 1, cy - 2, cx, cy + 3, color);
		g.fill(cx, cy - 1, cx + 1, cy + 2, color);
		g.fill(cx + 1, cy, cx + 2, cy + 1, color);
		g.fill(cx + 3, cy - 3, cx + 4, cy + 4, color);
	}

	public static void drawUpChevron(UiCanvas g, int cx, int cy, int color) {
		g.fill(cx, cy - 2, cx + 1, cy - 1, color);
		g.fill(cx - 1, cy - 1, cx + 2, cy, color);
		g.fill(cx - 2, cy, cx - 1, cy + 1, color);
		g.fill(cx + 2, cy, cx + 3, cy + 1, color);
		g.fill(cx - 3, cy + 1, cx - 2, cy + 2, color);
		g.fill(cx + 3, cy + 1, cx + 4, cy + 2, color);
	}

	public static void drawDownChevron(UiCanvas g, int cx, int cy, int color) {
		g.fill(cx - 3, cy - 2, cx - 2, cy - 1, color);
		g.fill(cx + 3, cy - 2, cx + 4, cy - 1, color);
		g.fill(cx - 2, cy - 1, cx - 1, cy, color);
		g.fill(cx + 2, cy - 1, cx + 3, cy, color);
		g.fill(cx - 1, cy, cx + 2, cy + 1, color);
		g.fill(cx, cy + 1, cx + 1, cy + 2, color);
	}

	public static void drawPauseIcon(UiCanvas g, int cx, int cy, int color) {
		g.fill(cx - 3, cy - 3, cx - 1, cy + 4, color);
		g.fill(cx + 1, cy - 3, cx + 3, cy + 4, color);
	}

	public static void drawPlayIcon(UiCanvas g, int cx, int cy, int color) {
		g.fill(cx - 2, cy - 3, cx - 1, cy + 4, color);
		g.fill(cx - 1, cy - 2, cx, cy + 3, color);
		g.fill(cx, cy - 1, cx + 1, cy + 2, color);
		g.fill(cx + 1, cy, cx + 3, cy + 1, color);
	}

	public static void drawHeartIcon(UiCanvas g, int cx, int cy, int color) {
		g.fill(cx - 3, cy - 2, cx - 1, cy + 1, color);
		g.fill(cx + 1, cy - 2, cx + 3, cy + 1, color);
		g.fill(cx - 2, cy - 3, cx - 1, cy + 2, color);
		g.fill(cx + 1, cy - 3, cx + 2, cy + 2, color);
		g.fill(cx - 1, cy - 1, cx + 1, cy + 3, color);
		g.fill(cx, cy + 3, cx + 1, cy + 4, color);
	}

	public static void drawSpeakerIcon(UiCanvas g, int cx, int cy, boolean muted, int color) {
		g.fill(cx - 4, cy - 1, cx - 2, cy + 2, color);
		g.fill(cx - 2, cy - 2, cx - 1, cy + 3, color);
		g.fill(cx - 1, cy - 3, cx, cy + 4, color);
		if (muted) {
			g.fill(cx + 1, cy - 2, cx + 2, cy - 1, COLOR_YT_RED);
			g.fill(cx + 2, cy - 1, cx + 3, cy, COLOR_YT_RED);
			g.fill(cx + 3, cy - 2, cx + 4, cy - 1, COLOR_YT_RED);
			g.fill(cx + 2, cy, cx + 3, cy + 1, COLOR_YT_RED);
			g.fill(cx + 1, cy + 1, cx + 2, cy + 2, COLOR_YT_RED);
			g.fill(cx + 3, cy + 1, cx + 4, cy + 2, COLOR_YT_RED);
		} else {
			g.fill(cx + 1, cy - 1, cx + 2, cy + 2, color);
			g.fill(cx + 3, cy - 2, cx + 4, cy + 3, color);
		}
	}

	public static void drawSettingsIcon(UiCanvas g, int cx, int cy, int color) {
		g.fill(cx - 3, cy - 2, cx + 4, cy - 1, color);
		g.fill(cx - 1, cy - 3, cx + 1, cy, color);
		g.fill(cx - 3, cy + 1, cx + 4, cy + 2, color);
		g.fill(cx + 1, cy, cx + 3, cy + 3, color);
	}

	public static void drawCaptionsIcon(UiCanvas g, int cx, int cy, boolean active, int color) {
		g.fill(cx - 5, cy - 3, cx + 5, cy - 2, color);
		g.fill(cx - 5, cy + 3, cx + 5, cy + 4, color);
		g.fill(cx - 5, cy - 2, cx - 4, cy + 3, color);
		g.fill(cx + 4, cy - 2, cx + 5, cy + 3, color);
		g.fill(cx - 3, cy - 1, cx + 1, cy, color);
		g.fill(cx + 2, cy - 1, cx + 3, cy, color);
		g.fill(cx - 3, cy + 1, cx - 1, cy + 2, color);
		g.fill(cx, cy + 1, cx + 3, cy + 2, color);
		if (active) {
			g.fill(cx - 4, cy + 4, cx + 4, cy + 5, COLOR_YT_RED);
		}
	}

	public static void drawCommentIcon(UiCanvas g, int cx, int cy, int color) {
		// Rounded speech bubble outline with 2 subtle text lines and tail
		g.fill(cx - 4, cy - 3, cx + 4, cy - 2, color);
		g.fill(cx - 4, cy + 2, cx - 1, cy + 3, color);
		g.fill(cx + 1, cy + 2, cx + 4, cy + 3, color);
		g.fill(cx - 5, cy - 2, cx - 4, cy + 2, color);
		g.fill(cx + 4, cy - 2, cx + 5, cy + 2, color);
		// Tail
		g.fill(cx - 2, cy + 3, cx - 1, cy + 4, color);
		g.fill(cx - 1, cy + 3, cx, cy + 4, color);
		g.fill(cx, cy + 2, cx + 1, cy + 3, color);
		// Inner text lines
		g.fill(cx - 3, cy - 1, cx + 3, cy, color);
		g.fill(cx - 3, cy + 1, cx + 1, cy + 2, color);
	}

	public static void drawLunarButton(
			UiCanvas g,
			Font font,
			int x,
			int y,
			int w,
			int h,
			String label,
			boolean hovered,
			boolean active,
			int accentArgb
	) {
		int bg = active ? withAlpha(accentArgb, 0x3D) : (hovered ? COLOR_SURFACE_HOVER : COLOR_SURFACE_ELEVATED);
		int border = active ? accentArgb : (hovered ? 0x55FFFFFF : COLOR_BORDER_SUBTLE);
		int textColor = active ? COLOR_TEXT_PRIMARY : (hovered ? COLOR_TEXT_PRIMARY : 0xFFD4D4D4);

		g.fill(x, y, x + w, y + h, bg);
		g.fill(x, y, x + w, y + 1, border);
		g.fill(x, y + h - 1, x + w, y + h, active ? accentArgb : COLOR_BORDER_DARK);
		g.fill(x, y, x + 1, y + h, border);
		g.fill(x + w - 1, y, x + w, y + h, border);

		int textY = y + (h - font.lineHeight) / 2 + 1;
		g.drawCenteredText(font, label, x + w / 2, textY, textColor);
	}

	public static int withAlpha(int argb, int alpha) {
		return ((alpha & 0xFF) << 24) | (argb & 0x00FFFFFF);
	}
}
