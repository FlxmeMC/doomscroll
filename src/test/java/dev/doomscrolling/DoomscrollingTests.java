package dev.doomscrolling;

import dev.doomscrolling.browser.ShortsState;
import dev.doomscrolling.browser.YouTubeShortsBridge;
import dev.doomscrolling.config.DoomscrollingConfig;
import dev.doomscrolling.ui.HudBounds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

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
		assertFalse(script.contains("__DS_HUD_FLAG__"));
		assertFalse(script.contains("__DS_SUBTITLES__"));
	}

	@Test
	void configRoundTripsAndSanitizesOutOfRangeValues(@TempDir Path tempDir) {
		Path file = tempDir.resolve("doomscrolling.json");
		DoomscrollingConfig cfg = DoomscrollingConfig.load(file);
		assertEquals(DoomscrollingConfig.AccentTheme.YOUTUBE_RED, cfg.accentTheme);
		cfg.hudWidth = 999;
		cfg.backgroundOpacity = 5.0f;
		cfg.browserFps = 12;
		cfg.volume = -3.0f;
		cfg.posX = 2.5f;
		cfg.posY = -1.0f;
		cfg.autoScroll = false;
		cfg.accentTheme = DoomscrollingConfig.AccentTheme.YOUTUBE_RED;
		cfg.layoutMode = DoomscrollingConfig.LayoutMode.COMPACT_AUDIO;
		cfg.save();

		DoomscrollingConfig reloaded = DoomscrollingConfig.load(file);
		assertEquals(280, reloaded.hudWidth);
		assertEquals(0.98f, reloaded.backgroundOpacity, 0.001f);
		assertEquals(24, reloaded.browserFps);
		assertEquals(0.0f, reloaded.volume, 0.001f);
		assertEquals(1.0f, reloaded.posX, 0.001f);
		assertEquals(0.0f, reloaded.posY, 0.001f);
		assertFalse(reloaded.autoScroll);
		assertEquals(DoomscrollingConfig.AccentTheme.YOUTUBE_RED, reloaded.accentTheme);
		assertEquals(DoomscrollingConfig.LayoutMode.COMPACT_AUDIO, reloaded.layoutMode);
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
}
