package dev.doomscrolling.ui;

import dev.doomscrolling.browser.CommentsState;
import dev.doomscrolling.browser.ShortsBrowserController;
import dev.doomscrolling.browser.YouTubeShortsBridge;
import dev.doomscrolling.platform.UiCanvas;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Read-only comments panel shown next to the HUD card. It only displays what the
 * page bridge scrapes: there is no way to post, reply, or like from here.
 */
public final class CommentsPanel {

	private static final int HEADER_H = 20;
	private static final int PAD = 7;
	private static final int LINE_H = 10;
	private static final int ITEM_GAP = 8;
	private static final int MAX_LINES_PER_COMMENT = 8;
	private static final int SCROLL_STEP = 20;
	private static final int CLOSE_SIZE = 11;

	private record Row(int y, int x, String text, int color) {
	}

	private record Layout(List<Row> rows, List<Integer> separators, int contentHeight) {
	}

	private final ShortsBrowserController controller;

	private static final long LOADING_TIMEOUT_MS = 12_000L;

	private int scrollPx = 0;
	private String scrollVideoId = "";
	private long loadingSinceMs = 0L;
	private int openGeneration = -1;
	private CommentsState cachedState = null;
	private int cachedWidth = -1;
	private Layout cachedLayout = new Layout(List.of(), List.of(), 0);

	public CommentsPanel(ShortsBrowserController controller) {
		this.controller = controller;
	}

	public boolean isVisible(boolean editorMode) {
		return !editorMode && controller.isCommentsOpen() && controller.config().hudVisible;
	}

	public void render(
			UiCanvas g,
			Font font,
			HudBounds card,
			int screenWidth,
			int screenHeight,
			int mouseX,
			int mouseY,
			int accent,
			int bg
	) {
		CommentsLayout.Rect r = CommentsLayout.computeBounds(card, screenWidth, screenHeight);
		CommentsState cs = controller.commentsState();
		String currentVideoId = controller.state().videoId();

		int generation = controller.commentsOpenGeneration();
		if (!cs.videoId().equals(scrollVideoId) || generation != openGeneration) {
			scrollVideoId = cs.videoId();
			openGeneration = generation;
			scrollPx = 0;
			loadingSinceMs = 0L;
		}

		LunarDraw.drawGlassCard(g, r.x(), r.y(), r.w(), r.h(), bg, false);

		int closeX = r.x() + r.w() - PAD - CLOSE_SIZE + 2;
		int closeY = r.y() + 4;
		g.drawText(font, "Comments", r.x() + PAD, r.y() + 6, LunarDraw.COLOR_TEXT_PRIMARY, false);
		String count = YouTubeShortsBridge.sanitizeForMinecraftFont(cs.totalLabel());
		int countX = r.x() + PAD + font.width("Comments") + 5;
		if (!count.isEmpty() && countX + font.width(count) < closeX - 3) {
			g.drawText(font, count, countX, r.y() + 6, LunarDraw.COLOR_TEXT_MUTED, false);
		}
		boolean hovClose = inBox(mouseX, mouseY, closeX, closeY, CLOSE_SIZE);
		drawCloseIcon(g, closeX + CLOSE_SIZE / 2, closeY + CLOSE_SIZE / 2, hovClose ? accent : LunarDraw.COLOR_TEXT_SECONDARY);
		g.fill(r.x() + PAD, r.y() + HEADER_H - 1, r.x() + r.w() - PAD, r.y() + HEADER_H, LunarDraw.COLOR_BORDER_SUBTLE);

		int bodyX = r.x() + PAD;
		int bodyY = r.y() + HEADER_H + 4;
		int bodyW = Math.max(20, r.w() - PAD * 2 - 4);
		int bodyH = Math.max(10, r.h() - HEADER_H - 4 - PAD);

		boolean stale = !cs.videoId().isEmpty() && !currentVideoId.isEmpty() && !cs.videoId().equals(currentVideoId);
		boolean hasItems = !cs.items().isEmpty() && !stale;

		if (!hasItems) {
			int cx = r.x() + r.w() / 2;
			int cy = bodyY + bodyH / 2;
			boolean loading = cs.loading() || stale;
			if (!loading) {
				loadingSinceMs = 0L;
			} else if (loadingSinceMs == 0L) {
				loadingSinceMs = LunarDraw.nowMs();
			}
			boolean timedOut = loading && LunarDraw.nowMs() - loadingSinceMs > LOADING_TIMEOUT_MS;
			if (cs.signInRequired() && !stale) {
				drawCenteredLines(g, font, CommentsLayout.wrap("Sign in to see comments", bodyW, font::width), cx, cy - 10, LunarDraw.COLOR_TEXT_SECONDARY);
				g.drawCenteredText(font, "Press ] to log in", cx, cy + 10, LunarDraw.COLOR_TEXT_MUTED);
			} else if (timedOut) {
				drawCenteredLines(g, font, CommentsLayout.wrap("Couldn't load comments", bodyW, font::width), cx, cy - 4, LunarDraw.COLOR_TEXT_MUTED);
			} else if (loading) {
				int barW = Math.min(56, bodyW - 8);
				LunarDraw.drawIndeterminateBar(g, cx - barW / 2, cy - 4, barW, 2, accent);
				g.drawCenteredText(font, "Loading comments", cx, cy + 4, LunarDraw.COLOR_TEXT_MUTED);
			} else {
				g.drawCenteredText(font, "No comments yet", cx, cy - 4, LunarDraw.COLOR_TEXT_MUTED);
			}
			return;
		}

		loadingSinceMs = 0L;
		Layout layout = layoutFor(cs, bodyW, font);
		int maxScroll = Math.max(0, layout.contentHeight() - bodyH);
		scrollPx = Math.max(0, Math.min(maxScroll, scrollPx));

		g.enableScissor(bodyX, bodyY, bodyX + bodyW, bodyY + bodyH);
		for (int sepY : layout.separators()) {
			int y = bodyY + sepY - scrollPx;
			if (y >= bodyY - 1 && y <= bodyY + bodyH) {
				g.fill(bodyX, y, bodyX + bodyW, y + 1, LunarDraw.COLOR_BORDER_SUBTLE);
			}
		}
		for (Row row : layout.rows()) {
			int y = bodyY + row.y() - scrollPx;
			if (y + LINE_H < bodyY || y > bodyY + bodyH) continue;
			g.drawText(font, row.text(), bodyX + row.x(), y, row.color(), false);
		}
		g.disableScissor();

		if (maxScroll > 0) {
			int trackX = r.x() + r.w() - PAD + 2;
			g.fill(trackX, bodyY, trackX + 2, bodyY + bodyH, 0x22FFFFFF);
			int thumbH = Math.max(12, bodyH * bodyH / Math.max(1, layout.contentHeight()));
			int thumbY = bodyY + Math.round((bodyH - thumbH) * (scrollPx / (float) maxScroll));
			g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0x99FFFFFF);
		}
	}

	/** Returns true when the click landed on the panel (it is consumed either way). */
	public boolean handleClick(double mx, double my, HudBounds card, int screenWidth, int screenHeight) {
		CommentsLayout.Rect r = CommentsLayout.computeBounds(card, screenWidth, screenHeight);
		if (!r.contains(mx, my)) {
			return false;
		}
		int closeX = r.x() + r.w() - PAD - CLOSE_SIZE + 2;
		int closeY = r.y() + 4;
		if (inBox(mx, my, closeX - 2, closeY - 2, CLOSE_SIZE + 4)) {
			controller.setCommentsOpen(false);
		}
		return true;
	}

	public boolean handleScroll(double mx, double my, double scrollY, HudBounds card, int screenWidth, int screenHeight) {
		CommentsLayout.Rect r = CommentsLayout.computeBounds(card, screenWidth, screenHeight);
		if (!r.contains(mx, my)) {
			return false;
		}
		if (scrollY > 0.01) {
			scrollPx -= SCROLL_STEP;
		} else if (scrollY < -0.01) {
			scrollPx += SCROLL_STEP;
		}
		scrollPx = Math.max(0, scrollPx);
		return true;
	}

	private Layout layoutFor(CommentsState cs, int width, Font font) {
		if (cs != cachedState || width != cachedWidth) {
			cachedState = cs;
			cachedWidth = width;
			cachedLayout = buildLayout(cs, width, font::width);
		}
		return cachedLayout;
	}

	private static Layout buildLayout(CommentsState cs, int width, ToIntFunction<String> measure) {
		List<Row> rows = new ArrayList<>();
		List<Integer> separators = new ArrayList<>();
		int y = 0;
		List<CommentsState.CommentItem> items = cs.items();
		for (int i = 0; i < items.size(); i++) {
			CommentsState.CommentItem item = items.get(i);
			if (i > 0) {
				separators.add(y - ITEM_GAP / 2 - 1);
			}

			String author = YouTubeShortsBridge.sanitizeForMinecraftFont(item.author());
			if (author.isEmpty()) author = "User";
			String when = YouTubeShortsBridge.sanitizeForMinecraftFont(item.timeAgo());
			String authorFit = CommentsLayout.ellipsize(author, when.isEmpty() ? width : Math.max(24, width - measure.applyAsInt(when) - 6), measure);
			rows.add(new Row(y, 0, authorFit, LunarDraw.COLOR_TEXT_SECONDARY));
			if (!when.isEmpty()) {
				int whenX = measure.applyAsInt(authorFit) + 6;
				if (whenX + measure.applyAsInt(when) <= width) {
					rows.add(new Row(y, whenX, when, LunarDraw.COLOR_TEXT_MUTED));
				}
			}
			y += LINE_H + 1;

			List<String> lines = CommentsLayout.wrap(YouTubeShortsBridge.sanitizeForMinecraftFont(item.text()), width, measure);
			int shown = Math.min(lines.size(), MAX_LINES_PER_COMMENT);
			for (int li = 0; li < shown; li++) {
				String line = lines.get(li);
				if (li == shown - 1 && lines.size() > shown) {
					line = CommentsLayout.ellipsize(line + "...", width, measure);
				}
				rows.add(new Row(y, 0, line, LunarDraw.COLOR_TEXT_PRIMARY));
				y += LINE_H;
			}

			String likes = CommentsLayout.likesLabel(item.likes());
			if (!likes.isEmpty()) {
				rows.add(new Row(y + 1, 0, likes, LunarDraw.COLOR_TEXT_MUTED));
				y += LINE_H + 1;
			}
			y += ITEM_GAP;
		}
		return new Layout(List.copyOf(rows), List.copyOf(separators), Math.max(0, y - ITEM_GAP));
	}

	private static void drawCenteredLines(UiCanvas g, Font font, List<String> lines, int cx, int y, int color) {
		for (int i = 0; i < lines.size(); i++) {
			g.drawCenteredText(font, lines.get(i), cx, y + i * LINE_H, color);
		}
	}

	private static void drawCloseIcon(UiCanvas g, int cx, int cy, int color) {
		for (int i = -3; i <= 3; i++) {
			g.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, color);
			g.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, color);
		}
	}

	private static boolean inBox(double mx, double my, int x, int y, int size) {
		return mx >= x && mx < x + size && my >= y && my < y + size;
	}
}
