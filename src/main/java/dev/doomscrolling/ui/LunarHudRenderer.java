package dev.doomscrolling.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.doomscrolling.DoomscrollingClient;
import dev.doomscrolling.browser.ShortsBrowserController;
import dev.doomscrolling.browser.ShortsState;
import dev.doomscrolling.browser.YouTubeShortsBridge;
import dev.doomscrolling.config.DoomscrollingConfig;
import dev.doomscrolling.platform.McPlatform;
import dev.doomscrolling.platform.UiCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class LunarHudRenderer {

	private static final int OVERLAY_BTN_SIZE = 16;
	private static final int OVERLAY_BTN_GAP = 3;
	private static final int VOLUME_PILL_EXTRA_W = 58;
	private static final int RESIZE_EDGE_HIT = 7;

	private enum ResizeEdge {
		NONE, LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
	}

	private enum OverlayButton {
		LIKE, COMMENTS, PREV, PLAY, NEXT, VOLUME, CAPTIONS, SETTINGS
	}

	private static final List<OverlayButton> HUD_BUTTONS = List.of(OverlayButton.values());
	private static final List<OverlayButton> EDITOR_BUTTONS = List.of(
			OverlayButton.LIKE, OverlayButton.PREV, OverlayButton.PLAY, OverlayButton.NEXT,
			OverlayButton.VOLUME, OverlayButton.CAPTIONS
	);

	private final ShortsBrowserController controller;
	private final CommentsPanel commentsPanel;
	private boolean commentsPanelShown = false;

	private boolean draggingHud = false;
	private boolean scrubbing = false;
	private boolean draggingVolume = false;
	private boolean volumePillOpen = false;
	private ResizeEdge activeResizeEdge = ResizeEdge.NONE;

	private int dragOffsetX = 0;
	private int dragOffsetY = 0;
	private int resizeStartMouseX = 0;
	private int resizeStartMouseY = 0;
	private int resizeStartWidth = 144;
	private int resizeAnchorRight = 0;
	private int resizeAnchorBottom = 0;
	private int resizeStartLeft = 0;
	private int resizeStartTop = 0;
	private HudBounds lastBounds = null;

	public LunarHudRenderer(ShortsBrowserController controller) {
		this.controller = controller;
		this.commentsPanel = new CommentsPanel(controller);
	}

	public boolean isDraggingOrResizing() {
		return draggingHud || scrubbing || draggingVolume || activeResizeEdge != ResizeEdge.NONE;
	}

	public void resetInteraction() {
		draggingHud = false;
		scrubbing = false;
		draggingVolume = false;
		volumePillOpen = false;
		activeResizeEdge = ResizeEdge.NONE;
	}

	public void renderHud(UiCanvas canvas) {
		Minecraft mc = Minecraft.getInstance();
		if (!controller.config().hudVisible) {
			return;
		}
		Screen current = McPlatform.get().currentScreen(mc);
		if (McPlatform.get().isOverlayScreen(current) || current instanceof ChatScreen) {
			return;
		}
		renderCard(canvas, mc.font, canvas.guiWidth(), canvas.guiHeight(), -1, -1, false, false, false);
	}

	public HudBounds renderCard(
			UiCanvas g,
			Font font,
			int screenWidth,
			int screenHeight,
			int mouseX,
			int mouseY,
			boolean interactive,
			boolean forceShowControls,
			boolean editorMode
	) {
		DoomscrollingConfig config = controller.config();
		ShortsState state = controller.state();
		HudBounds b = HudBounds.compute(config, screenWidth, screenHeight);
		this.lastBounds = b;

		long nowNanos = System.nanoTime();
		float progress = state.interpolatedProgress(nowNanos);
		double currentTime = state.interpolatedTimeSec(nowNanos);
		int accent = config.accentTheme.argb();
		int bg = config.backgroundArgb();
		boolean hovered = interactive && b.contains(mouseX, mouseY);
		boolean showOverlay = interactive && (hovered || forceShowControls || isDraggingOrResizing());

		if (!showOverlay) {
			volumePillOpen = false;
		}

		if (config.layoutMode == DoomscrollingConfig.LayoutMode.COMPACT_AUDIO) {
			renderCompactMiniCard(g, font, b, state, config, progress, currentTime, accent, bg, mouseX, mouseY, showOverlay);
		} else {
			renderShortsOverlayCard(g, font, b, state, config, progress, currentTime, accent, bg, mouseX, mouseY, showOverlay, editorMode);
		}

		commentsPanelShown = commentsPanel.isVisible(editorMode);
		if (commentsPanelShown) {
			commentsPanel.render(g, font, b, screenWidth, screenHeight,
					interactive ? mouseX : -1, interactive ? mouseY : -1, accent, bg);
		}

		if (editorMode) {
			renderEditorHandles(g, font, b, mouseX, mouseY, accent);
		}

		return b;
	}

	private void renderShortsOverlayCard(
			UiCanvas g,
			Font font,
			HudBounds b,
			ShortsState state,
			DoomscrollingConfig config,
			float progress,
			double currentTime,
			int accent,
			int bg,
			int mouseX,
			int mouseY,
			boolean showOverlay,
			boolean editorMode
	) {
		boolean activeBorder = draggingHud || activeResizeEdge != ResizeEdge.NONE;
		LunarDraw.drawGlassCard(g, b.x(), b.y(), b.width(), b.height(), bg, activeBorder);

		renderVideoSurface(g, font, b.videoX(), b.videoY(), b.videoWidth(), b.videoHeight(), accent);

		controller.feedback().render(
				g,
				b.videoX() + b.videoWidth() / 2,
				b.videoY() + b.videoHeight() / 2,
				Math.round(b.videoWidth() * 0.34f),
				accent
		);

		renderToastOverlay(g, font, b.videoX(), b.videoY() + 6, b.videoWidth(), accent);

		if (showOverlay) {
			renderVideoOverlayControls(g, font, b, state, config, currentTime, accent, mouseX, mouseY, editorMode);
		}

		int barH = showOverlay ? 4 : b.footerHeight();
		int barY = b.y() + b.height() - barH - 1;
		if (state.loading() || state.buffering()) {
			LunarDraw.drawIndeterminateBar(g, b.footerX(), barY, b.footerWidth(), barH, accent);
		} else {
			LunarDraw.drawProgressBar(g, b.footerX(), barY, b.footerWidth(), barH, progress, accent, showOverlay);
		}
	}

	private void renderVideoOverlayControls(
			UiCanvas g,
			Font font,
			HudBounds b,
			ShortsState state,
			DoomscrollingConfig config,
			double currentTime,
			int accent,
			int mouseX,
			int mouseY,
			boolean editorMode
	) {
		String ts = ShortsState.formatTimestamp(currentTime) + " / " + ShortsState.formatTimestamp(state.durationSec());
		int tsW = font.width(ts) + 8;
		int topY = b.videoY() + 4;
		g.fill(b.videoX() + 4, topY, b.videoX() + 4 + tsW, topY + 12, LunarDraw.COLOR_OVERLAY_PILL);
		g.drawText(font, ts, b.videoX() + 8, topY + 2, LunarDraw.COLOR_TEXT_PRIMARY, false);

		int btnX = rightButtonX(b);
		List<OverlayButton> buttons = overlayButtons(editorMode);
		int[] ys = rightButtonYs(b, buttons.size());

		for (int i = 0; i < buttons.size(); i++) {
			OverlayButton kind = buttons.get(i);
			int y = ys[i];
			int cx = btnX + OVERLAY_BTN_SIZE / 2;
			int cy = y + OVERLAY_BTN_SIZE / 2;
			boolean hov = inBox(mouseX, mouseY, btnX, y, OVERLAY_BTN_SIZE);
			switch (kind) {
				case LIKE -> {
					LunarDraw.drawOverlayIconButton(g, btnX, y, OVERLAY_BTN_SIZE, hov, state.liked(), accent);
					LunarDraw.drawHeartIcon(g, cx, cy, state.liked() ? 0xFFFFFFFF : LunarDraw.COLOR_TEXT_PRIMARY);
				}
				case COMMENTS -> {
					boolean open = controller.isCommentsOpen();
					LunarDraw.drawOverlayIconButton(g, btnX, y, OVERLAY_BTN_SIZE, hov, open, accent);
					LunarDraw.drawCommentIcon(g, cx, cy, open ? 0xFFFFFFFF : LunarDraw.COLOR_TEXT_PRIMARY);
				}
				case PREV -> {
					LunarDraw.drawOverlayIconButton(g, btnX, y, OVERLAY_BTN_SIZE, hov, false, accent);
					LunarDraw.drawUpChevron(g, cx, cy, LunarDraw.COLOR_TEXT_PRIMARY);
				}
				case PLAY -> {
					LunarDraw.drawOverlayIconButton(g, btnX, y, OVERLAY_BTN_SIZE, hov, false, accent);
					if (state.paused()) {
						LunarDraw.drawPlayIcon(g, cx, cy, LunarDraw.COLOR_TEXT_PRIMARY);
					} else {
						LunarDraw.drawPauseIcon(g, cx, cy, LunarDraw.COLOR_TEXT_PRIMARY);
					}
				}
				case NEXT -> {
					LunarDraw.drawOverlayIconButton(g, btnX, y, OVERLAY_BTN_SIZE, hov, false, accent);
					LunarDraw.drawDownChevron(g, cx, cy, LunarDraw.COLOR_TEXT_PRIMARY);
				}
				case VOLUME -> {
					int pillW = Math.min(OVERLAY_BTN_SIZE + VOLUME_PILL_EXTRA_W, b.videoWidth() - 8);
					int pillX = btnX + OVERLAY_BTN_SIZE - pillW;
					boolean overVolPill = mouseX >= pillX && mouseX < btnX + OVERLAY_BTN_SIZE && mouseY >= y - 2 && mouseY < y + OVERLAY_BTN_SIZE + 2;
					if (hov || draggingVolume) {
						volumePillOpen = true;
					} else if (!overVolPill) {
						volumePillOpen = false;
					}
					if (volumePillOpen) {
						drawYouTubeVolumePill(g, pillX, y, pillW, OVERLAY_BTN_SIZE, config.volume, config.muted, true);
					} else {
						LunarDraw.drawOverlayIconButton(g, btnX, y, OVERLAY_BTN_SIZE, hov, config.muted, accent);
						LunarDraw.drawSpeakerIcon(g, cx, cy, config.muted, LunarDraw.COLOR_TEXT_PRIMARY);
					}
				}
				case CAPTIONS -> {
					LunarDraw.drawOverlayIconButton(g, btnX, y, OVERLAY_BTN_SIZE, hov, config.subtitles, accent);
					LunarDraw.drawCaptionsIcon(g, cx, cy, config.subtitles, LunarDraw.COLOR_TEXT_PRIMARY);
				}
				case SETTINGS -> {
					LunarDraw.drawOverlayIconButton(g, btnX, y, OVERLAY_BTN_SIZE, hov, false, accent);
					LunarDraw.drawSettingsIcon(g, cx, cy, LunarDraw.COLOR_TEXT_PRIMARY);
				}
			}
		}
	}

	private void drawYouTubeVolumePill(
			UiCanvas g,
			int x,
			int y,
			int w,
			int h,
			float volume,
			boolean muted,
			boolean iconOnRight
	) {
		int pillBg = 0xD8242424;
		g.fill(x + 2, y, x + w - 2, y + h, pillBg);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, pillBg);
		g.fill(x, y + 2, x + w, y + h - 2, pillBg);
		g.fill(x + 2, y, x + w - 2, y + 1, 0x33FFFFFF);

		int iconCx = iconOnRight ? (x + w - h / 2) : (x + h / 2);
		int iconCy = y + h / 2;
		LunarDraw.drawSpeakerIcon(g, iconCx, iconCy, muted, LunarDraw.COLOR_TEXT_PRIMARY);

		int trackLeft = iconOnRight ? (x + 7) : (x + h + 3);
		int trackRight = iconOnRight ? (x + w - h - 3) : (x + w - 7);
		int trackW = Math.max(12, trackRight - trackLeft);
		int trackY = y + h / 2 - 1;
		float level = muted ? 0.0f : Math.max(0.0f, Math.min(1.0f, volume));
		int fillW = Math.round(trackW * level);

		g.fill(trackLeft, trackY, trackLeft + trackW, trackY + 2, 0x55FFFFFF);
		if (fillW > 0) {
			g.fill(trackLeft, trackY, trackLeft + fillW, trackY + 2, 0xFFFFFFFF);
		}
		int knobCx = trackLeft + fillW;
		int knobCy = trackY + 1;
		g.fill(knobCx - 2, knobCy - 3, knobCx + 2, knobCy + 3, 0xFFFFFFFF);
		g.fill(knobCx - 3, knobCy - 2, knobCx + 3, knobCy + 2, 0xFFFFFFFF);
	}

	private void renderCompactMiniCard(
			UiCanvas g,
			Font font,
			HudBounds b,
			ShortsState state,
			DoomscrollingConfig config,
			float progress,
			double currentTime,
			int accent,
			int bg,
			int mouseX,
			int mouseY,
			boolean showOverlay
	) {
		boolean activeBorder = draggingHud || activeResizeEdge != ResizeEdge.NONE;
		LunarDraw.drawGlassCard(g, b.x(), b.y(), b.width(), b.height(), bg, activeBorder);
		renderVideoSurface(g, font, b.videoX(), b.videoY(), b.videoWidth(), b.videoHeight(), accent);

		int fx = b.footerX();
		int fy = b.footerY();
		int fw = b.footerWidth();

		int transportW = 64;
		int playCenterX = fx + fw - 23;
		int playCenterY = fy + 11;
		int playBox = 16;

		int volBtnX = playCenterX - 38;
		int volBtnY = playCenterY - 8;
		int miniPillW = Math.min(68, fw - 4);
		int miniPillX = volBtnX + 16 - miniPillW;
		boolean overVolBtn = showOverlay && inBox(mouseX, mouseY, volBtnX, volBtnY, 16);
		boolean overVolPill = showOverlay && mouseX >= miniPillX && mouseX < volBtnX + 16 && mouseY >= volBtnY - 2 && mouseY < volBtnY + 18;
		if (overVolBtn || draggingVolume) {
			volumePillOpen = true;
		} else if (!overVolPill) {
			volumePillOpen = false;
		}

		boolean hovPlay = showOverlay && inBox(mouseX, mouseY, playCenterX - 8, playCenterY - 8, playBox);
		int playColor = hovPlay ? accent : LunarDraw.COLOR_TEXT_PRIMARY;
		if (state.paused()) {
			LunarDraw.drawPlayIcon(g, playCenterX, playCenterY, playColor);
		} else {
			LunarDraw.drawPauseIcon(g, playCenterX, playCenterY, playColor);
		}
		boolean hovPrev = showOverlay && inBox(mouseX, mouseY, playCenterX - 24, playCenterY - 7, 14);
		boolean hovNext = showOverlay && inBox(mouseX, mouseY, playCenterX + 10, playCenterY - 7, 14);
		LunarDraw.drawPrevIcon(g, playCenterX - 17, playCenterY, hovPrev ? accent : LunarDraw.COLOR_TEXT_PRIMARY);
		LunarDraw.drawNextIcon(g, playCenterX + 17, playCenterY, hovNext ? accent : LunarDraw.COLOR_TEXT_PRIMARY);

		DoomscrollingConfig.FeedPlatform platform = config.platform != null
				? config.platform
				: DoomscrollingConfig.FeedPlatform.YOUTUBE_SHORTS;
		String safeTitle = YouTubeShortsBridge.sanitizeForMinecraftFont(state.displayTitle());
		if (safeTitle.isEmpty()) safeTitle = platform.displayName();
		String safeChannel = YouTubeShortsBridge.sanitizeForMinecraftFont(state.displayChannel());
		if (safeChannel.isEmpty()) safeChannel = platform.shortName();

		int textMaxW = Math.max(24, fw - transportW - 6);
		LunarDraw.drawMarqueeText(g, font, safeTitle, fx, fy + 2, textMaxW, LunarDraw.COLOR_TEXT_PRIMARY, false);
		LunarDraw.drawMarqueeText(g, font, safeChannel, fx, fy + 15, textMaxW, LunarDraw.COLOR_TEXT_SECONDARY, false);

		if (showOverlay && volumePillOpen) {
			drawYouTubeVolumePill(g, miniPillX, volBtnY, miniPillW, 16, config.volume, config.muted, true);
		} else if (showOverlay) {
			LunarDraw.drawSpeakerIcon(g, volBtnX + 8, volBtnY + 8, config.muted,
					overVolBtn ? accent : LunarDraw.COLOR_TEXT_SECONDARY);
		}

		int barY = fy + b.footerHeight() - 17;
		if (state.loading() || state.buffering()) {
			LunarDraw.drawIndeterminateBar(g, fx, barY, fw, 3, accent);
		} else {
			LunarDraw.drawProgressBar(g, fx, barY, fw, 3, progress, accent, showOverlay);
		}

		int timeY = barY + 6;
		String toast = controller.activeToast();
		if (toast != null) {
			g.drawText(font, toast, fx, timeY, accent, false);
		} else {
			g.drawText(font, ShortsState.formatTimestamp(currentTime), fx, timeY, LunarDraw.COLOR_TEXT_SECONDARY, false);
		}
		String dur = ShortsState.formatTimestamp(state.durationSec());
		g.drawText(font, dur, fx + fw - font.width(dur), timeY, LunarDraw.COLOR_TEXT_SECONDARY, false);

		controller.feedback().render(
				g,
				b.videoX() + b.videoWidth() / 2,
				b.videoY() + b.videoHeight() / 2,
				Math.min(30, Math.round(b.videoWidth() * 0.78f)),
				accent
		);
	}

	private void renderEditorHandles(UiCanvas g, Font font, HudBounds b, int mouseX, int mouseY, int accent) {
		ResizeEdge hoverEdge = activeResizeEdge != ResizeEdge.NONE ? activeResizeEdge : detectResizeEdge(b, mouseX, mouseY);
		int edgeIdle = 0x55FFFFFF;
		int cornerLen = 8;

		switch (hoverEdge) {
			case LEFT, RIGHT -> g.requestCursor(UiCanvas.CursorKind.EW);
			case TOP, BOTTOM -> g.requestCursor(UiCanvas.CursorKind.NS);
			case TOP_LEFT, BOTTOM_RIGHT -> g.requestCursor(UiCanvas.CursorKind.NWSE);
			case TOP_RIGHT, BOTTOM_LEFT -> g.requestCursor(UiCanvas.CursorKind.NESW);
			case NONE -> {
				if (draggingHud) {
					g.requestCursor(UiCanvas.CursorKind.MOVE);
				} else {
					g.requestCursor(UiCanvas.CursorKind.DEFAULT);
				}
			}
		}

		g.outline(b.x() - 1, b.y() - 1, b.width() + 2, b.height() + 2,
				(hoverEdge != ResizeEdge.NONE || draggingHud) ? accent : 0x38FFFFFF);

		drawCornerHandle(g, b.x() - 1, b.y() - 1, cornerLen, true, true,
				(hoverEdge == ResizeEdge.TOP_LEFT || hoverEdge == ResizeEdge.LEFT || hoverEdge == ResizeEdge.TOP) ? accent : edgeIdle);
		drawCornerHandle(g, b.x() + b.width() + 1, b.y() - 1, cornerLen, false, true,
				(hoverEdge == ResizeEdge.TOP_RIGHT || hoverEdge == ResizeEdge.RIGHT || hoverEdge == ResizeEdge.TOP) ? accent : edgeIdle);
		drawCornerHandle(g, b.x() - 1, b.y() + b.height() + 1, cornerLen, true, false,
				(hoverEdge == ResizeEdge.BOTTOM_LEFT || hoverEdge == ResizeEdge.LEFT || hoverEdge == ResizeEdge.BOTTOM) ? accent : edgeIdle);
		drawCornerHandle(g, b.x() + b.width() + 1, b.y() + b.height() + 1, cornerLen, false, false,
				(hoverEdge == ResizeEdge.BOTTOM_RIGHT || hoverEdge == ResizeEdge.RIGHT || hoverEdge == ResizeEdge.BOTTOM) ? accent : edgeIdle);

		if (activeResizeEdge != ResizeEdge.NONE) {
			String badge = b.width() + "x" + b.height();
			int bw = font.width(badge) + 8;
			int bx = b.x() + (b.width() - bw) / 2;
			int by = b.y() > 18 ? (b.y() - 15) : (b.y() + b.height() + 4);
			g.fill(bx, by, bx + bw, by + 12, 0xE80F0F0F);
			g.outline(bx, by, bw, 12, accent);
			g.drawCenteredText(font, badge, bx + bw / 2, by + 2, LunarDraw.COLOR_TEXT_PRIMARY);
		}
	}

	private static void drawCornerHandle(UiCanvas g, int cx, int cy, int len, boolean right, boolean down, int color) {
		int x1 = right ? cx : cx - len;
		int x2 = right ? cx + len : cx;
		int y1 = down ? cy : cy - 2;
		int y2 = down ? cy + 2 : cy;
		g.fill(x1, y1, x2, y2, color);

		int vx1 = right ? cx : cx - 2;
		int vx2 = right ? cx + 2 : cx;
		int vy1 = down ? cy : cy - len;
		int vy2 = down ? cy + len : cy;
		g.fill(vx1, vy1, vx2, vy2, color);
	}

	private void renderVideoSurface(UiCanvas g, Font font, int x, int y, int w, int h, int accent) {
		g.fill(x, y, x + w, y + h, 0xFF0F0F0F);

		if (controller.isRinkuInitFailed()) {
			if (w >= 64) {
				g.drawCenteredText(font, "Restart MC", x + w / 2, y + h / 2 - 4, accent);
				g.drawCenteredText(font, "(cache busy)", x + w / 2, y + h / 2 + 8, LunarDraw.COLOR_TEXT_MUTED);
			} else {
				LunarDraw.drawPlayIcon(g, x + w / 2, y + h / 2, accent);
			}
			return;
		}

		Object tex = controller.getTextureIdentifier();
		boolean texReady = tex != null && controller.isBrowserTextureReady();
		boolean firstLoaded = controller.isFirstShortLoaded();
		float overlayAlpha = controller.loadingOverlayAlpha();
		ShortsState st = controller.state();
		DoomscrollingConfig.FeedPlatform platform = controller.config().platform != null
				? controller.config().platform
				: DoomscrollingConfig.FeedPlatform.YOUTUBE_SHORTS;

		if (firstLoaded && st.botGateDetected() && !st.videoReady() && !st.signedIn()) {
			int cx = x + w / 2;
			int cy = y + h / 2;
			if (w >= 64) {
				g.drawCenteredText(font, "Sign in to", cx, cy - 12, LunarDraw.COLOR_TEXT_SECONDARY);
				g.drawCenteredText(font, platform.shortName(), cx, cy - 1, accent);
				g.drawCenteredText(font, "Press ] to log in", cx, cy + 13, LunarDraw.COLOR_TEXT_MUTED);
			} else {
				LunarDraw.drawPlayIcon(g, cx, cy, accent);
			}
			return;
		}

		if (texReady && firstLoaded) {
			g.blitTexture(tex, x, y, w, h);
		}

		if (!texReady || overlayAlpha > 0.01f) {
			float a = texReady ? overlayAlpha : 1.0f;
			int bgAlpha = Math.max(0, Math.min(255, Math.round(a * 255.0f)));
			g.fill(x, y, x + w, y + h, (bgAlpha << 24) | 0x0F0F0F);

			int cx = x + w / 2;
			int cy = y + h / 2;
			float emblemSize = Math.max(14.0f, Math.min(28.0f, w * 0.24f));
			controller.feedback().renderLoadingEmblem(g, cx, cy - (w >= 64 ? 8.0f : 0.0f), emblemSize, accent, a);

			if (w >= 64 && a > 0.2f) {
				int barW = Math.min(56, w - 24);
				if (barW > 14) {
					LunarDraw.drawIndeterminateBar(g, cx - barW / 2, cy + 12, barW, 2, LunarDraw.withAlpha(accent, bgAlpha));
				}
				int labelAlpha = Math.max(0, Math.min(255, Math.round(a * 170.0f)));
				g.drawCenteredText(font, "Loading " + platform.feedLabel(), cx, cy + 20, (labelAlpha << 24) | 0xAAAAAA);
			}
		}
	}

	private void renderToastOverlay(UiCanvas g, Font font, int x, int y, int w, int accent) {
		String toast = controller.activeToast();
		if (toast == null || toast.isEmpty()) {
			return;
		}
		int tw = Math.min(w - 8, font.width(toast) + 10);
		int tx = x + (w - tw) / 2;
		g.fill(tx, y, tx + tw, y + 13, 0xD80F0F0F);
		g.fill(tx, y + 12, tx + tw, y + 13, accent);
		g.drawCenteredText(font, toast, x + w / 2, y + 2, LunarDraw.COLOR_TEXT_PRIMARY);
	}

	public boolean handleMouseClick(
			double mx,
			double my,
			int button,
			int screenWidth,
			int screenHeight,
			boolean allowOpenSettings,
			boolean editorMode
	) {
		if (!controller.config().hudVisible || button != InputConstants.MOUSE_BUTTON_LEFT) {
			return false;
		}
		HudBounds b = HudBounds.compute(controller.config(), screenWidth, screenHeight);
		this.lastBounds = b;

		if (editorMode) {
			ResizeEdge edge = detectResizeEdge(b, mx, my);
			if (edge != ResizeEdge.NONE) {
				activeResizeEdge = edge;
				resizeStartMouseX = (int) mx;
				resizeStartMouseY = (int) my;
				resizeStartWidth = controller.config().hudWidth;
				resizeStartLeft = b.x();
				resizeStartTop = b.y();
				resizeAnchorRight = b.x() + b.width();
				resizeAnchorBottom = b.y() + b.height();
				return true;
			}
		}

		if (commentsPanel.isVisible(editorMode) && commentsPanel.handleClick(mx, my, b, screenWidth, screenHeight)) {
			return true;
		}

		if (!b.contains(mx, my)) {
			volumePillOpen = false;
			return false;
		}

		if (controller.config().layoutMode == DoomscrollingConfig.LayoutMode.COMPACT_AUDIO) {
			int fx = b.footerX();
			int fy = b.footerY();
			int fw = b.footerWidth();
			int playCenterX = fx + fw - 23;
			int playCenterY = fy + 11;
			int volBtnX = playCenterX - 38;
			int volBtnY = playCenterY - 8;
			int miniPillW = Math.min(68, fw - 4);
			int miniPillX = volBtnX + 16 - miniPillW;

			if (volumePillOpen && mx >= miniPillX && mx < volBtnX + 16 && my >= volBtnY - 2 && my < volBtnY + 18) {
				if (mx >= volBtnX) {
					controller.toggleMute();
				} else {
					draggingVolume = true;
					applyVolumeFromPill(mx, miniPillX, miniPillW, 16);
				}
				return true;
			}
			if (inBox(mx, my, volBtnX, volBtnY, 16)) {
				controller.toggleMute();
				return true;
			}
			if (inBox(mx, my, playCenterX - 24, playCenterY - 8, 15)) {
				controller.previousShort();
				return true;
			}
			if (inBox(mx, my, playCenterX - 8, playCenterY - 8, 16)) {
				controller.togglePlayPause();
				return true;
			}
			if (inBox(mx, my, playCenterX + 9, playCenterY - 8, 15)) {
				controller.nextShort();
				return true;
			}
			int barY = fy + b.footerHeight() - 20;
			if (mx >= fx && mx <= fx + fw && my >= barY && my <= barY + 10) {
				scrubbing = true;
				controller.seekToRatio((float) ((mx - fx) / Math.max(1.0, fw)));
				return true;
			}
			draggingHud = true;
			dragOffsetX = (int) mx - b.x();
			dragOffsetY = (int) my - b.y();
			return true;
		}

		int btnX = rightButtonX(b);
		List<OverlayButton> buttons = overlayButtons(editorMode);
		int[] ys = rightButtonYs(b, buttons.size());
		int volY = ys[buttons.indexOf(OverlayButton.VOLUME)];
		int pillW = Math.min(OVERLAY_BTN_SIZE + VOLUME_PILL_EXTRA_W, b.videoWidth() - 8);
		int pillX = btnX + OVERLAY_BTN_SIZE - pillW;

		if (volumePillOpen && mx >= pillX && mx < btnX + OVERLAY_BTN_SIZE && my >= volY - 2 && my < volY + OVERLAY_BTN_SIZE + 2) {
			if (mx >= btnX) {
				controller.toggleMute();
			} else {
				draggingVolume = true;
				applyVolumeFromPill(mx, pillX, pillW, OVERLAY_BTN_SIZE);
			}
			return true;
		}

		if (my >= b.y() + b.height() - 10) {
			scrubbing = true;
			controller.seekToRatio((float) ((mx - b.footerX()) / Math.max(1.0, b.footerWidth())));
			return true;
		}

		for (int i = 0; i < buttons.size(); i++) {
			if (!inBox(mx, my, btnX, ys[i], OVERLAY_BTN_SIZE)) continue;
			switch (buttons.get(i)) {
				case LIKE -> controller.toggleLike();
				case COMMENTS -> controller.toggleComments();
				case PREV -> controller.previousShort();
				case PLAY -> controller.togglePlayPause();
				case NEXT -> controller.nextShort();
				case VOLUME -> controller.toggleMute();
				case CAPTIONS -> controller.toggleSubtitles();
				case SETTINGS -> {
					if (allowOpenSettings) {
						McPlatform.get().setScreen(Minecraft.getInstance(), DoomscrollingClient.createScreen(null));
					}
				}
			}
			return true;
		}

		if (editorMode || my <= b.videoY() + 18) {
			draggingHud = true;
			dragOffsetX = (int) mx - b.x();
			dragOffsetY = (int) my - b.y();
			return true;
		}

		ShortsState st = controller.state();
		if (allowOpenSettings && !editorMode && st.botGateDetected() && !st.videoReady() && !st.signedIn()) {
			McPlatform.get().setScreen(Minecraft.getInstance(), DoomscrollingClient.createScreen(null));
			return true;
		}

		controller.togglePlayPause();
		return true;
	}

	public boolean handleMouseDrag(double mx, double my, int screenWidth, int screenHeight) {
		if (draggingVolume && lastBounds != null) {
			if (controller.config().layoutMode == DoomscrollingConfig.LayoutMode.COMPACT_AUDIO) {
				int fx = lastBounds.footerX();
				int fw = lastBounds.footerWidth();
				int volBtnX = (fx + fw - 23) - 38;
				int miniPillW = Math.min(68, fw - 4);
				int miniPillX = volBtnX + 16 - miniPillW;
				applyVolumeFromPill(mx, miniPillX, miniPillW, 16);
			} else {
				int btnX = rightButtonX(lastBounds);
				int pillW = Math.min(OVERLAY_BTN_SIZE + VOLUME_PILL_EXTRA_W, lastBounds.videoWidth() - 8);
				int pillX = btnX + OVERLAY_BTN_SIZE - pillW;
				applyVolumeFromPill(mx, pillX, pillW, OVERLAY_BTN_SIZE);
			}
			return true;
		}
		if (activeResizeEdge != ResizeEdge.NONE) {
			applyResizeDrag((int) mx, (int) my, screenWidth, screenHeight);
			return true;
		}
		if (scrubbing && lastBounds != null) {
			controller.seekToRatio((float) ((mx - lastBounds.footerX()) / Math.max(1.0, lastBounds.footerWidth())));
			return true;
		}
		if (draggingHud && lastBounds != null) {
			DoomscrollingConfig cfg = controller.config();
			cfg.posX = HudBounds.normalizedX((int) mx - dragOffsetX, lastBounds.width(), screenWidth);
			cfg.posY = HudBounds.normalizedY((int) my - dragOffsetY, lastBounds.height(), screenHeight);
			return true;
		}
		return false;
	}

	public boolean handleMouseRelease() {
		if (draggingVolume) {
			draggingVolume = false;
			controller.config().save();
			return true;
		}
		if (activeResizeEdge != ResizeEdge.NONE) {
			activeResizeEdge = ResizeEdge.NONE;
			controller.applySettingsChanged();
			return true;
		}
		if (scrubbing) {
			scrubbing = false;
			return true;
		}
		if (draggingHud) {
			draggingHud = false;
			controller.config().save();
			return true;
		}
		return false;
	}

	public boolean handleMouseScroll(double mx, double my, double scrollY, int screenWidth, int screenHeight) {
		if (!controller.config().hudVisible) {
			return false;
		}
		HudBounds b = HudBounds.compute(controller.config(), screenWidth, screenHeight);
		if (commentsPanelShown && commentsPanel.isVisible(false) && commentsPanel.handleScroll(mx, my, scrollY, b, screenWidth, screenHeight)) {
			return true;
		}
		if (!b.contains(mx, my)) {
			return false;
		}
		if (volumePillOpen) {
			controller.setVolume(controller.config().volume + (scrollY > 0 ? 0.05f : -0.05f));
			return true;
		}
		if (scrollY < -0.1) {
			controller.nextShort();
		} else if (scrollY > 0.1) {
			controller.previousShort();
		}
		return true;
	}

	private void applyVolumeFromPill(double mx, int pillX, int pillW, int pillH) {
		int trackLeft = pillX + 7;
		int trackRight = pillX + pillW - pillH - 3;
		int trackW = Math.max(12, trackRight - trackLeft);
		controller.setVolume((float) Math.max(0.0, Math.min(1.0, (mx - trackLeft) / (double) trackW)));
	}

	private void applyResizeDrag(int mx, int my, int screenWidth, int screenHeight) {
		DoomscrollingConfig cfg = controller.config();
		int dx = mx - resizeStartMouseX;
		int dy = my - resizeStartMouseY;

		int deltaW;
		switch (activeResizeEdge) {
			case LEFT -> deltaW = -dx;
			case RIGHT -> deltaW = dx;
			case TOP -> deltaW = Math.round(-dy * 9.0f / 16.0f);
			case BOTTOM -> deltaW = Math.round(dy * 9.0f / 16.0f);
			case TOP_LEFT -> deltaW = Math.round((-dx - dy * 9.0f / 16.0f) * 0.5f);
			case TOP_RIGHT -> deltaW = Math.round((dx - dy * 9.0f / 16.0f) * 0.5f);
			case BOTTOM_LEFT -> deltaW = Math.round((-dx + dy * 9.0f / 16.0f) * 0.5f);
			case BOTTOM_RIGHT -> deltaW = Math.round((dx + dy * 9.0f / 16.0f) * 0.5f);
			default -> deltaW = 0;
		}

		cfg.hudWidth = Math.max(100, Math.min(280, resizeStartWidth + deltaW));
		HudBounds updated = HudBounds.compute(cfg, screenWidth, screenHeight);

		boolean anchorRight = activeResizeEdge == ResizeEdge.LEFT
				|| activeResizeEdge == ResizeEdge.TOP_LEFT
				|| activeResizeEdge == ResizeEdge.BOTTOM_LEFT;
		boolean anchorBottom = activeResizeEdge == ResizeEdge.TOP
				|| activeResizeEdge == ResizeEdge.TOP_LEFT
				|| activeResizeEdge == ResizeEdge.TOP_RIGHT;

		int newX = anchorRight ? (resizeAnchorRight - updated.width()) : resizeStartLeft;
		int newY = anchorBottom ? (resizeAnchorBottom - updated.height()) : resizeStartTop;
		cfg.posX = HudBounds.normalizedX(newX, updated.width(), screenWidth);
		cfg.posY = HudBounds.normalizedY(newY, updated.height(), screenHeight);
	}

	private static ResizeEdge detectResizeEdge(HudBounds b, double mx, double my) {
		int pad = 4;
		if (mx < b.x() - pad || mx > b.x() + b.width() + pad || my < b.y() - pad || my > b.y() + b.height() + pad) {
			return ResizeEdge.NONE;
		}
		boolean nearL = mx <= b.x() + RESIZE_EDGE_HIT;
		boolean nearR = mx >= b.x() + b.width() - RESIZE_EDGE_HIT;
		boolean nearT = my <= b.y() + RESIZE_EDGE_HIT;
		boolean nearB = my >= b.y() + b.height() - RESIZE_EDGE_HIT;

		if (nearT && nearL) return ResizeEdge.TOP_LEFT;
		if (nearT && nearR) return ResizeEdge.TOP_RIGHT;
		if (nearB && nearL) return ResizeEdge.BOTTOM_LEFT;
		if (nearB && nearR) return ResizeEdge.BOTTOM_RIGHT;
		if (nearL) return ResizeEdge.LEFT;
		if (nearR) return ResizeEdge.RIGHT;
		if (nearT) return ResizeEdge.TOP;
		if (nearB) return ResizeEdge.BOTTOM;
		return ResizeEdge.NONE;
	}

	private static int rightButtonX(HudBounds b) {
		return b.videoX() + b.videoWidth() - OVERLAY_BTN_SIZE - 4;
	}

	private static List<OverlayButton> overlayButtons(boolean editorMode) {
		return editorMode ? EDITOR_BUTTONS : HUD_BUTTONS;
	}

	private static int[] rightButtonYs(HudBounds b, int count) {
		int totalH = count * OVERLAY_BTN_SIZE + (count - 1) * OVERLAY_BTN_GAP;
		int bottomLimit = b.videoY() + b.videoHeight() - 8;
		int startY = Math.max(b.videoY() + 18, bottomLimit - totalH);
		int[] ys = new int[count];
		for (int i = 0; i < count; i++) {
			ys[i] = startY + i * (OVERLAY_BTN_SIZE + OVERLAY_BTN_GAP);
		}
		return ys;
	}

	private static boolean inBox(double mx, double my, int x, int y, int size) {
		return mx >= x && mx < x + size && my >= y && my < y + size;
	}
}
