package dev.doomscrolling.browser;

import java.util.List;

public record CommentsState(
		String videoId,
		String totalLabel,
		boolean open,
		boolean loading,
		boolean signInRequired,
		List<CommentItem> items
) {

	public record CommentItem(
			String author,
			String text,
			String likes,
			String timeAgo
	) {
	}

	public static final CommentsState CLOSED = new CommentsState("", "", false, false, false, List.of());

	public static CommentsState loadingFor(String videoId) {
		return new CommentsState(videoId == null ? "" : videoId, "", true, true, false, List.of());
	}
}
