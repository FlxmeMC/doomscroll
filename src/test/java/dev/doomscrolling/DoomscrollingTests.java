package dev.doomscrolling;

import dev.doomscrolling.browser.CommentsState;
import dev.doomscrolling.browser.ShortsState;
import dev.doomscrolling.browser.YouTubeShortsBridge;
import dev.doomscrolling.config.DoomscrollingConfig;
import dev.doomscrolling.ui.CommentsLayout;
import dev.doomscrolling.ui.HudBounds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class DoomscrollingTests {

	@Test
	void shortsStateInterpolatesSmoothlyWhilePlayingAndFreezesWhenPaused() {
		long t0 = 10_000_000_000L;
		ShortsState playing = new ShortsState(
				"abc123", "Cool Short", "@creator",
				5.0, 20.0, t0, 1.0f,
				false, false, false, true, true, false, false, true, false,
				"https://www.youtube.com/shorts/abc123"
		);

		assertEquals(7.5, playing.interpolatedTimeSec(t0 + 2_500_000_000L), 0.001);
		assertEquals(0.375f, playing.interpolatedProgress(t0 + 2_500_000_000L), 0.001f);
		assertEquals(20.0, playing.interpolatedTimeSec(t0 + 90_000_000_000L), 0.001);
		assertEquals(1.0f, playing.interpolatedProgress(t0 + 90_000_000_000L), 0.001f);

		ShortsState paused = new ShortsState(
				"abc123", "Cool Short", "@creator",
				5.0, 20.0, t0, 1.0f,
				true, false, false, true, true, false, false, true, false,
				"https://www.youtube.com/shorts/abc123"
		);
		assertEquals(5.0, paused.interpolatedTimeSec(t0 + 5_000_000_000L), 0.001);
	}

	@Test
	void formatsTimestampsCleanly() {
		assertEquals("0:00", ShortsState.formatTimestamp(0));
		assertEquals("0:07", ShortsState.formatTimestamp(7.9));
		assertEquals("1:05", ShortsState.formatTimestamp(65.2));
		assertEquals("0:00", ShortsState.formatTimestamp(-4.0));
		assertEquals("0:00", ShortsState.formatTimestamp(Double.NaN));
	}

	@Test
	void parsesValidAndInvalidBridgeMessagesSafely() {
		String raw = YouTubeShortsBridge.MESSAGE_PREFIX
				+ "{\"id\":\"xyz789\",\"title\":\"Insane Clutch\",\"channel\":\"@steve\",\"t\":12.5,\"d\":30.0,\"r\":1,\"p\":false,\"m\":false,\"l\":true,\"s\":true,\"sh\":true,\"b\":false,\"url\":\"https://www.youtube.com/shorts/xyz789\"}";

		ShortsState parsed = YouTubeShortsBridge.parseStateMessage(raw, 12345L);
		assertNotNull(parsed);
		assertEquals("xyz789", parsed.videoId());
		assertEquals("Insane Clutch", parsed.title());
		assertEquals("@steve", parsed.channel());
		assertEquals(12.5, parsed.currentTimeSec(), 0.001);
		assertEquals(30.0, parsed.durationSec(), 0.001);
		assertFalse(parsed.paused());
		assertTrue(parsed.liked());
		assertTrue(parsed.signedIn());
		assertTrue(parsed.onShortsPage());

		assertNull(YouTubeShortsBridge.parseStateMessage("unrelated console log", 1L));
		assertNull(YouTubeShortsBridge.parseStateMessage(YouTubeShortsBridge.MESSAGE_PREFIX + "{broken json", 1L));
		assertEquals("signin_required", YouTubeShortsBridge.parseEventMessage("__DS_EVT__:signin_required"));
		assertNull(YouTubeShortsBridge.parseEventMessage("other"));

		assertEquals("Insane clutch #shorts", YouTubeShortsBridge.sanitizeForMinecraftFont("Insane clutch 🤠😭🙏 #shorts"));

		String script = YouTubeShortsBridge.buildBootstrapScript(true, true, true, 250, 0.65f, false, true);
		assertTrue(script.contains("linear-gradient"));
		assertTrue(script.contains("dataset.dsHud = '1'"));
		assertTrue(script.contains("data-ds-cc"));
		assertTrue(script.contains("doomscrolling-ig-clean-css"));
		assertTrue(script.contains("data-ds-platform=\"instagram\""));
		assertFalse(script.contains("__DS_HUD_FLAG__"));
		assertFalse(script.contains("__DS_SUBTITLES__"));
		assertFalse(script.contains("__DS_INSTAGRAM_BRANCH__"));
	}

	@Test
	void supportsInstagramReelsStateUrlsAndMessages() {
		assertTrue(ShortsState.isReelsUrl("https://www.instagram.com/reels/"));
		assertTrue(ShortsState.isReelsUrl("https://www.instagram.com/reels/C8abcXYZ123/"));
		assertTrue(ShortsState.isReelsUrl("https://www.instagram.com/reel/C8abcXYZ123/"));
		assertFalse(ShortsState.isReelsUrl("https://www.instagram.com/reels/audio/123456789/"));
		assertFalse(ShortsState.isReelsUrl("https://www.instagram.com/accounts/login/?next=%2Freels%2F"));

		assertTrue(ShortsState.isFeedUrl("https://www.youtube.com/shorts/abc123"));
		assertTrue(ShortsState.isFeedUrl("https://www.instagram.com/reels/C8abcXYZ123/"));
		assertTrue(ShortsState.isSignInUrl("https://www.instagram.com/accounts/login/?next=%2Freels%2F"));
		assertTrue(ShortsState.isSignInUrl("https://www.instagram.com/two_factor/login/"));

		ShortsState initialIg = ShortsState.initialFor(DoomscrollingConfig.FeedPlatform.INSTAGRAM_REELS);
		assertEquals("Instagram Reels", initialIg.displayTitle());
		assertEquals(DoomscrollingConfig.DEFAULT_REELS_URL, initialIg.currentUrl());
		assertTrue(initialIg.onShortsPage());

		ShortsState igLogin = new ShortsState(
				"", "", "", 0, 0, 0L, 1.0f,
				true, false, false, false, false, false, false, false, true,
				"https://www.instagram.com/accounts/login/?next=%2Freels%2F"
		);
		assertEquals("Instagram Sign-In", igLogin.displayTitle());
		assertEquals("Local account session", igLogin.displayChannel());

		String igMsg = YouTubeShortsBridge.MESSAGE_PREFIX
				+ "{\"id\":\"C8abcXYZ123\",\"title\":\"POV mining diamonds\",\"channel\":\"@alex\",\"t\":4.2,\"d\":15.0,\"r\":1,\"p\":false,\"m\":false,\"l\":true,\"s\":true,\"b\":false,\"vr\":true,\"bg\":false,\"url\":\"https://www.instagram.com/reels/C8abcXYZ123/\"}";
		ShortsState parsed = YouTubeShortsBridge.parseStateMessage(igMsg, 555L);
		assertNotNull(parsed);
		assertEquals("C8abcXYZ123", parsed.videoId());
		assertEquals("POV mining diamonds", parsed.title());
		assertEquals("@alex", parsed.channel());
		assertTrue(parsed.onShortsPage());
		assertTrue(parsed.liked());
		assertTrue(parsed.signedIn());
		assertTrue(parsed.videoReady());
		assertFalse(parsed.botGateDetected());
	}

	@Test
	void configRoundTripsAndSanitizesOutOfRangeValues(@TempDir Path tempDir) {
		Path file = tempDir.resolve("doomscrolling.json");
		DoomscrollingConfig cfg = DoomscrollingConfig.load(file);
		assertEquals(DoomscrollingConfig.AccentTheme.YOUTUBE_RED, cfg.accentTheme);
		assertEquals(DoomscrollingConfig.FeedPlatform.YOUTUBE_SHORTS, cfg.platform);
		assertEquals("Shorts", cfg.layoutModeDisplayName());

		cfg.platform = DoomscrollingConfig.FeedPlatform.INSTAGRAM_REELS;
		cfg.startUrl = DoomscrollingConfig.DEFAULT_REELS_URL;
		cfg.hudWidth = 999;
		cfg.backgroundOpacity = 5.0f;
		cfg.browserFps = 12;
		cfg.volume = -3.0f;
		cfg.posX = 2.5f;
		cfg.posY = -1.0f;
		cfg.autoScroll = false;
		cfg.accentTheme = DoomscrollingConfig.AccentTheme.INSTAGRAM_PINK;
		cfg.layoutMode = DoomscrollingConfig.LayoutMode.COMPACT_AUDIO;
		cfg.save();

		DoomscrollingConfig reloaded = DoomscrollingConfig.load(file);
		assertEquals(DoomscrollingConfig.FeedPlatform.INSTAGRAM_REELS, reloaded.platform);
		assertEquals(DoomscrollingConfig.DEFAULT_REELS_URL, reloaded.effectiveStartUrl());
		assertEquals("Mini", reloaded.layoutModeDisplayName());
		assertEquals(280, reloaded.hudWidth);
		assertEquals(0.98f, reloaded.backgroundOpacity, 0.001f);
		assertEquals(24, reloaded.browserFps);
		assertEquals(0.0f, reloaded.volume, 0.001f);
		assertEquals(1.0f, reloaded.posX, 0.001f);
		assertEquals(0.0f, reloaded.posY, 0.001f);
		assertFalse(reloaded.autoScroll);
		assertEquals(DoomscrollingConfig.AccentTheme.INSTAGRAM_PINK, reloaded.accentTheme);
		assertEquals(DoomscrollingConfig.LayoutMode.COMPACT_AUDIO, reloaded.layoutMode);

		reloaded.layoutMode = DoomscrollingConfig.LayoutMode.CARD;
		assertEquals("Reels", reloaded.layoutModeDisplayName());
		assertEquals(DoomscrollingConfig.FeedPlatform.YOUTUBE_SHORTS, reloaded.platform.next());
	}

	@Test
	void legacyConfigWithoutPlatformInfersPlatformFromStartUrl(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("legacy.json");
		java.nio.file.Files.writeString(file, "{\"startUrl\":\"https://www.instagram.com/reels/\",\"hudWidth\":150}");
		DoomscrollingConfig loaded = DoomscrollingConfig.load(file);
		assertEquals(DoomscrollingConfig.FeedPlatform.INSTAGRAM_REELS, loaded.platform);
		assertEquals(DoomscrollingConfig.DEFAULT_REELS_URL, loaded.effectiveStartUrl());
	}

	@Test
	void hudBoundsStayInsideScreenForAllLayoutsAndResolutions() {
		int[][] screens = {{1920, 1080}, {960, 540}, {427, 240}};
		for (DoomscrollingConfig.LayoutMode mode : DoomscrollingConfig.LayoutMode.values()) {
			DoomscrollingConfig cfg = new DoomscrollingConfig();
			cfg.layoutMode = mode;
			for (int[] s : screens) {
				HudBounds b = HudBounds.compute(cfg, s[0], s[1]);
				assertTrue(b.x() >= 0, "x >= 0 for " + mode);
				assertTrue(b.y() >= 0, "y >= 0 for " + mode);
				assertTrue(b.x() + b.width() <= s[0], "right edge fits for " + mode + " on " + s[0] + "x" + s[1]);
				assertTrue(b.y() + b.height() <= s[1], "bottom edge fits for " + mode + " on " + s[0] + "x" + s[1]);
				assertTrue(b.videoWidth() > 0 && b.videoHeight() > 0);
			}
		}
	}

	@Test
	void parsesCommentsMessagesAndDropsBlankItems() {
		String raw = YouTubeShortsBridge.COMMENTS_PREFIX
				+ "{\"id\":\"abc123\",\"open\":true,\"loading\":false,\"sr\":false,\"cnt\":\"1.2K\",\"items\":["
				+ "{\"a\":\"@steve\",\"x\":\"First!\",\"l\":\"12\",\"t\":\"2 days ago\"},"
				+ "{\"a\":\"@alex\",\"x\":\"   \",\"l\":\"\",\"t\":\"\"},"
				+ "{\"a\":\"\",\"x\":\"no author\"}]}";

		CommentsState parsed = YouTubeShortsBridge.parseCommentsMessage(raw);
		assertNotNull(parsed);
		assertEquals("abc123", parsed.videoId());
		assertEquals("1.2K", parsed.totalLabel());
		assertTrue(parsed.open());
		assertFalse(parsed.loading());
		assertEquals(2, parsed.items().size());
		assertEquals("@steve", parsed.items().get(0).author());
		assertEquals("First!", parsed.items().get(0).text());
		assertEquals("12", parsed.items().get(0).likes());
		assertEquals("2 days ago", parsed.items().get(0).timeAgo());
		assertEquals("no author", parsed.items().get(1).text());

		assertNull(YouTubeShortsBridge.parseCommentsMessage("__DS_CMT__:{broken"));
		assertNull(YouTubeShortsBridge.parseCommentsMessage("__DS__:{}"));
		assertNull(YouTubeShortsBridge.parseCommentsMessage(null));

		CommentsState loading = CommentsState.loadingFor(null);
		assertEquals("", loading.videoId());
		assertTrue(loading.open() && loading.loading());
		assertFalse(CommentsState.CLOSED.open());
	}

	@Test
	void commentsResyncScriptOnlyReopensWhenClosed() {
		String script = YouTubeShortsBridge.buildCommentsResyncScript();
		assertTrue(script.contains("isCommentsOpen()"));
		assertTrue(script.contains("setCommentsOpen(true)"));
		assertEquals("if(window.__doomscrolling){window.__doomscrolling.setCommentsOpen(false);}",
				YouTubeShortsBridge.buildSetCommentsOpenScript(false));
		String bootstrap = YouTubeShortsBridge.buildBootstrapScript(true, true, true, 250, 0.65f, false, false);
		assertEquals(2, bootstrap.split("isCommentsOpen\\(\\) \\{", -1).length - 1,
				"both the YouTube and Instagram bridges expose isCommentsOpen()");
	}

	@Test
	void commentTextWrapsByWidthAndHardBreaksLongWords() {
		java.util.function.ToIntFunction<String> mono = text -> text.length() * 6;

		assertEquals(List.of("hello", "world"), CommentsLayout.wrap("hello world", 40, mono));
		assertEquals(List.of("hello world"), CommentsLayout.wrap("hello world", 66, mono));
		assertEquals(List.of("abcde", "fghij", "kl"), CommentsLayout.wrap("abcdefghijkl", 30, mono));
		assertEquals(List.of("one", "two"), CommentsLayout.wrap("one\ntwo", 200, mono));
		assertTrue(CommentsLayout.wrap("   ", 100, mono).isEmpty());
		assertTrue(CommentsLayout.wrap(null, 100, mono).isEmpty());
		for (String line : CommentsLayout.wrap("a very long comment that keeps going and going", 50, mono)) {
			assertTrue(mono.applyAsInt(line) <= 50, line);
		}
	}

	@Test
	void likesLabelsAreReadable() {
		assertEquals("12 likes", CommentsLayout.likesLabel("12"));
		assertEquals("1.2K likes", CommentsLayout.likesLabel("1.2K"));
		assertEquals("1 like", CommentsLayout.likesLabel("1"));
		assertEquals("", CommentsLayout.likesLabel("0"));
		assertEquals("", CommentsLayout.likesLabel(""));
	}

	@Test
	void commentsPanelSitsBesideTheCardAndStaysOnScreen() {
		DoomscrollingConfig cfg = new DoomscrollingConfig();
		int[][] screens = {{854, 480}, {427, 240}, {320, 240}, {1920, 1080}, {640, 360}};
		float[] positions = {0.0f, 0.5f, 1.0f};
		for (DoomscrollingConfig.LayoutMode mode : DoomscrollingConfig.LayoutMode.values()) {
			cfg.layoutMode = mode;
			for (int[] s : screens) {
				for (float px : positions) {
					cfg.posX = px;
					cfg.posY = px;
					HudBounds card = HudBounds.compute(cfg, s[0], s[1]);
					CommentsLayout.Rect r = CommentsLayout.computeBounds(card, s[0], s[1]);
					String where = mode + " " + s[0] + "x" + s[1] + " pos " + px;
					assertTrue(r.x() >= 0 && r.y() >= 0, "panel origin on screen for " + where);
					assertTrue(r.x() + r.w() <= s[0], "panel right edge fits for " + where);
					assertTrue(r.y() + r.h() <= s[1], "panel bottom edge fits for " + where);
					assertTrue(r.w() > 0 && r.h() > 0, "panel has size for " + where);
					boolean beside = r.x() >= card.x() + card.width() || r.x() + r.w() <= card.x();
					boolean overCard = r.x() == card.x() && r.w() == card.width();
					assertTrue(beside || overCard, "panel never half-overlaps the card for " + where);
				}
			}
		}
	}
}
