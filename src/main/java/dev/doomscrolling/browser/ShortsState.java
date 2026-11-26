package dev.doomscrolling.browser;

import dev.doomscrolling.config.DoomscrollingConfig;

import java.util.Locale;

public record ShortsState(
		String videoId,
		String title,
		String channel,
		double currentTimeSec,
		double durationSec,
		long capturedAtNanos,
		float playbackRate,
		boolean paused,
		boolean muted,
		boolean liked,
		boolean signedIn,
		boolean onShortsPage,
		boolean loading,
		boolean buffering,
		boolean videoReady,
		boolean botGateDetected,
		String currentUrl
) {

	public static final ShortsState INITIAL = initialFor(DoomscrollingConfig.FeedPlatform.YOUTUBE_SHORTS);

	public static ShortsState initialFor(DoomscrollingConfig.FeedPlatform platform) {
		DoomscrollingConfig.FeedPlatform active = platform != null
				? platform
				: DoomscrollingConfig.FeedPlatform.YOUTUBE_SHORTS;
		return new ShortsState(
				"",
				active.displayName(),
				"Loading...",
				0.0,
				0.0,
				0L,
				1.0f,
				true,
				false,
				false,
				false,
				true,
				true,
				false,
				false,
				false,
				active.feedUrl()
		);
	}

	public double interpolatedTimeSec(long nowNanos) {
		if (durationSec <= 0.0) {
			return Math.max(0.0, currentTimeSec);
		}
		if (paused || buffering || loading || capturedAtNanos <= 0L) {
			return clamp(currentTimeSec, 0.0, durationSec);
		}
		double elapsedSec = Math.max(0.0, (nowNanos - capturedAtNanos) / 1_000_000_000.0);
		double rate = playbackRate > 0.05f ? playbackRate : 1.0;
		return clamp(currentTimeSec + elapsedSec * rate, 0.0, durationSec);
	}

	public float interpolatedProgress(long nowNanos) {
		if (durationSec <= 0.05) {
			return 0.0f;
		}
		return (float) clamp(interpolatedTimeSec(nowNanos) / durationSec, 0.0, 1.0);
	}

	public String displayTitle() {
		if (title != null && !title.isBlank()) {
			return title;
		}
		if (!onShortsPage && currentUrl != null) {
			if (currentUrl.contains("accounts.google.com")) {
				return "Google Sign-In";
			}
			if (isInstagramSignInUrl(currentUrl)) {
				return "Instagram Sign-In";
			}
		}
		if (isInstagramUrl(currentUrl)) {
			return loading ? "Loading Reels..." : "Instagram Reels";
		}
		return loading ? "Loading Shorts..." : "YouTube Shorts";
	}

	public String displayChannel() {
		if (channel != null && !channel.isBlank()) {
			return channel;
		}
		if (!onShortsPage && currentUrl != null && isSignInUrl(currentUrl)) {
			return "Local account session";
		}
		if (isInstagramUrl(currentUrl)) {
			return signedIn ? "Instagram Reels" : "Guest";
		}
		return signedIn ? "YouTube Shorts" : "Guest";
	}

	public ShortsState withUrl(String url, boolean isLoading) {
		return new ShortsState(
				videoId,
				title,
				channel,
				currentTimeSec,
				durationSec,
				capturedAtNanos,
				playbackRate,
				paused,
				muted,
				liked,
				signedIn,
				isFeedUrl(url),
				isLoading,
				buffering,
				videoReady,
				botGateDetected,
				url == null ? "" : url
		);
	}

	public static boolean isShortsUrl(String url) {
		return url != null && url.toLowerCase(Locale.ROOT).contains("youtube.com/shorts");
	}

	public static boolean isInstagramUrl(String url) {
		return url != null && url.toLowerCase(Locale.ROOT).contains("instagram.com");
	}

	public static boolean isReelsUrl(String url) {
		if (!isInstagramUrl(url)) {
			return false;
		}
		String lower = url.toLowerCase(Locale.ROOT);
		if (lower.contains("/reels/audio/")) {
			return false;
		}
		return lower.contains("/reels") || lower.contains("/reel/");
	}

	public static boolean isFeedUrl(String url) {
		return isShortsUrl(url) || isReelsUrl(url);
	}

	public static boolean isInstagramSignInUrl(String url) {
		if (!isInstagramUrl(url)) {
			return false;
		}
		String lower = url.toLowerCase(Locale.ROOT);
		return lower.contains("/accounts/login")
				|| lower.contains("/accounts/emailsignup")
				|| lower.contains("/challenge")
				|| lower.contains("/two_factor");
	}

	public static boolean isSignInUrl(String url) {
		if (url == null) {
			return false;
		}
		return url.toLowerCase(Locale.ROOT).contains("accounts.google.com") || isInstagramSignInUrl(url);
	}

	public static String formatTimestamp(double seconds) {
		if (Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds < 0.0) {
			return "0:00";
		}
		int total = (int) Math.floor(seconds);
		int mins = total / 60;
		int secs = total % 60;
		return secs < 10 ? mins + ":0" + secs : mins + ":" + secs;
	}

	private static double clamp(double value, double min, double max) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return min;
		}
		return Math.max(min, Math.min(max, value));
	}
}
