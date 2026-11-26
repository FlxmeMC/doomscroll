package dev.doomscrolling.ui;

import dev.doomscrolling.browser.YouTubeShortsBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Minecraft-free layout helpers for the comments panel, kept separate so they can be unit tested.
 */
public final class CommentsLayout {

	static final int PREFERRED_WIDTH = 168;
	static final int MIN_SIDE_WIDTH = 110;
	static final int MIN_HEIGHT = 120;
	static final int GAP = 4;

	public record Rect(int x, int y, int w, int h) {
		public boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private CommentsLayout() {
	}

	/**
	 * Places the panel beside the card on whichever side has more room, or over the card
	 * when the screen is too narrow for a side panel.
	 */
	public static Rect computeBounds(HudBounds card, int screenWidth, int screenHeight) {
		int margin = HudBounds.SCREEN_MARGIN;
		int roomRight = screenWidth - margin - (card.x() + card.width() + GAP);
		int roomLeft = card.x() - GAP - margin;

		if (Math.max(roomRight, roomLeft) < MIN_SIDE_WIDTH) {
			return new Rect(card.x(), card.y(), card.width(), card.height());
		}

		boolean right = roomRight >= PREFERRED_WIDTH || roomRight >= roomLeft;
		int room = right ? roomRight : roomLeft;
		int w = Math.min(PREFERRED_WIDTH, room);
		int x = right ? card.x() + card.width() + GAP : card.x() - GAP - w;

		int maxH = Math.max(40, screenHeight - margin * 2);
		int h = Math.min(maxH, Math.max(MIN_HEIGHT, card.height()));
		int y = Math.max(margin, Math.min(card.y(), screenHeight - margin - h));
		return new Rect(x, y, w, h);
	}

	/** "1.2K" becomes "1.2K likes"; zero or empty counts are hidden. */
	public static String likesLabel(String rawLikes) {
		String likes = YouTubeShortsBridge.sanitizeForMinecraftFont(rawLikes);
		if (likes.isEmpty() || likes.equals("0")) {
			return "";
		}
		if (likes.matches("[\\d.,]+[KkMmBb]?")) {
			return likes + (likes.equals("1") ? " like" : " likes");
		}
		return likes;
	}

	/** Greedy word wrap that also hard-breaks words wider than the line. */
	public static List<String> wrap(String text, int maxWidth, ToIntFunction<String> measure) {
		List<String> out = new ArrayList<>();
		if (text == null || text.isBlank()) {
			return out;
		}
		int width = Math.max(1, maxWidth);
		for (String paragraph : text.split("\\R")) {
			StringBuilder line = new StringBuilder();
			for (String word : paragraph.trim().split("\\s+")) {
				if (word.isEmpty()) continue;
				String candidate = line.isEmpty() ? word : line + " " + word;
				if (measure.applyAsInt(candidate) <= width) {
					line.setLength(0);
					line.append(candidate);
					continue;
				}
				if (!line.isEmpty()) {
					out.add(line.toString());
					line.setLength(0);
				}
				String rest = word;
				while (measure.applyAsInt(rest) > width) {
					int cut = 1;
					while (cut < rest.length() && measure.applyAsInt(rest.substring(0, cut + 1)) <= width) {
						cut++;
					}
					out.add(rest.substring(0, cut));
					rest = rest.substring(cut);
				}
				line.append(rest);
			}
			if (!line.isEmpty()) {
				out.add(line.toString());
			}
		}
		return out;
	}

	public static String ellipsize(String text, int maxWidth, ToIntFunction<String> measure) {
		if (measure.applyAsInt(text) <= maxWidth) {
			return text;
		}
		String dots = "...";
		int end = text.length();
		while (end > 0 && measure.applyAsInt(text.substring(0, end) + dots) > maxWidth) {
			end--;
		}
		return end <= 0 ? dots : text.substring(0, end).stripTrailing() + dots;
	}
}
