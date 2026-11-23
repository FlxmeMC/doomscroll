package dev.doomscrolling.ui;

import dev.doomscrolling.config.DoomscrollingConfig;

public record HudBounds(
		int x,
		int y,
		int width,
		int height,
		int videoX,
		int videoY,
		int videoWidth,
		int videoHeight,
		int footerX,
		int footerY,
		int footerWidth,
		int footerHeight
) {

	public static final int SCREEN_MARGIN = 6;
	public static final int FRAME_BORDER = 2;
	public static final int PROGRESS_BAR_H = 3;

	public static HudBounds compute(DoomscrollingConfig config, int screenWidth, int screenHeight) {
		DoomscrollingConfig.LayoutMode mode = config.layoutMode != null
				? config.layoutMode
				: DoomscrollingConfig.LayoutMode.CARD;

		int width;
		int height;
		int videoRelX;
		int videoRelY;
		int videoW;
		int videoH;
		int footerRelX;
		int footerRelY;
		int footerW;
		int footerH;

		if (mode == DoomscrollingConfig.LayoutMode.COMPACT_AUDIO) {
			float scale = config.hudWidth / 144.0f;
			width = clampInt(Math.round(234 * scale), 196, Math.max(196, screenWidth - SCREEN_MARGIN * 2));
			height = clampInt(Math.round(74 * scale), 64, 96);
			int pad = 5;
			videoH = height - pad * 2;
			videoW = Math.max(24, Math.round(videoH * 9.0f / 16.0f));
			videoRelX = pad;
			videoRelY = pad;
			footerRelX = videoRelX + videoW + 8;
			footerRelY = pad;
			footerW = width - footerRelX - pad;
			footerH = height - pad * 2;
		} else {
			int maxCardW = Math.max(92, Math.min(config.hudWidth, screenWidth - SCREEN_MARGIN * 2));
			int maxVideoH = Math.max(110, Math.min(
					screenHeight - SCREEN_MARGIN * 2 - FRAME_BORDER * 2 - PROGRESS_BAR_H,
					Math.round(screenHeight * 0.86f)
			));
			int candidateVideoW = maxCardW - FRAME_BORDER * 2;
			videoW = Math.min(candidateVideoW, Math.round(maxVideoH * 9.0f / 16.0f));
			videoH = Math.round(videoW * 16.0f / 9.0f);
			width = videoW + FRAME_BORDER * 2;
			height = videoH + FRAME_BORDER * 2 + PROGRESS_BAR_H;
			videoRelX = FRAME_BORDER;
			videoRelY = FRAME_BORDER;
			footerRelX = FRAME_BORDER;
			footerRelY = FRAME_BORDER + videoH;
			footerW = videoW;
			footerH = PROGRESS_BAR_H;
		}

		int availW = Math.max(0, screenWidth - width - SCREEN_MARGIN * 2);
		int availH = Math.max(0, screenHeight - height - SCREEN_MARGIN * 2);
		int x = SCREEN_MARGIN + Math.round(clamp01(config.posX) * availW);
		int y = SCREEN_MARGIN + Math.round(clamp01(config.posY) * availH);

		return new HudBounds(
				x, y, width, height,
				x + videoRelX, y + videoRelY, videoW, videoH,
				x + footerRelX, y + footerRelY, footerW, footerH
		);
	}

	public boolean contains(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	public static float normalizedX(int pixelX, int cardWidth, int screenWidth) {
		int availW = Math.max(1, screenWidth - cardWidth - SCREEN_MARGIN * 2);
		return clamp01((pixelX - SCREEN_MARGIN) / (float) availW);
	}

	public static float normalizedY(int pixelY, int cardHeight, int screenHeight) {
		int availH = Math.max(1, screenHeight - cardHeight - SCREEN_MARGIN * 2);
		return clamp01((pixelY - SCREEN_MARGIN) / (float) availH);
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float clamp01(float value) {
		if (Float.isNaN(value) || Float.isInfinite(value)) return 0.0f;
		return Math.max(0.0f, Math.min(1.0f, value));
	}
}
