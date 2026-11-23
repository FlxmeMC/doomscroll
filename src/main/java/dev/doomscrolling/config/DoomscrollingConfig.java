package dev.doomscrolling.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class DoomscrollingConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger("doomscrolling");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static final String DEFAULT_SHORTS_URL = "https://www.youtube.com/shorts";
	public static final String GOOGLE_SIGNIN_URL =
			"https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fwww.youtube.com%2Fshorts";

	public enum LayoutMode {
		CARD("Shorts"),
		COMPACT_AUDIO("Mini");

		private final String displayName;

		LayoutMode(String displayName) {
			this.displayName = displayName;
		}

		public String displayName() {
			return displayName;
		}

		public LayoutMode next() {
			LayoutMode[] values = values();
			return values[(ordinal() + 1) % values.length];
		}
	}

	public enum QualityPreset {
		ECO("270p", 270, 480),
		BALANCED("360p", 360, 640),
		CRISP("450p", 450, 800),
		ULTRA("540p", 540, 960);

		private final String displayName;
		private final int width;
		private final int height;

		QualityPreset(String displayName, int width, int height) {
			this.displayName = displayName;
			this.width = width;
			this.height = height;
		}

		public String displayName() {
			return displayName;
		}

		public int width() {
			return width;
		}

		public int height() {
			return height;
		}

		public QualityPreset next() {
			QualityPreset[] values = values();
			return values[(ordinal() + 1) % values.length];
		}
	}

	public enum AccentTheme {
		YOUTUBE_RED("YouTube Red", 0xFFFF0033),
		PURE_WHITE("Clean White", 0xFFF1F1F1),
		LUNAR_GREEN("Emerald", 0xFF1ED760),
		ARCTIC_CYAN("Cyan", 0xFF38BDF8);

		private final String displayName;
		private final int argb;

		AccentTheme(String displayName, int argb) {
			this.displayName = displayName;
			this.argb = argb;
		}

		public String displayName() {
			return displayName;
		}

		public int argb() {
			return argb;
		}

		public AccentTheme next() {
			AccentTheme[] values = values();
			return values[(ordinal() + 1) % values.length];
		}
	}

	public boolean hudVisible = true;
	public LayoutMode layoutMode = LayoutMode.CARD;
	public AccentTheme accentTheme = AccentTheme.YOUTUBE_RED;
	public QualityPreset qualityPreset = QualityPreset.BALANCED;
	public float posX = 0.985f;
	public float posY = 0.04f;
	public int hudWidth = 144;
	public float backgroundOpacity = 0.88f;
	public int browserFps = 30;
	public boolean autoScroll = true;
	public int autoScrollDelayMs = 250;
	public float volume = 0.65f;
	public boolean muted = false;
	public boolean subtitles = false;
	public boolean pauseWhenHidden = true;
	public String startUrl = DEFAULT_SHORTS_URL;

	private transient Path filePath;

	public static DoomscrollingConfig load(Path configPath) {
		DoomscrollingConfig config = null;
		if (Files.exists(configPath)) {
			try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
				config = GSON.fromJson(reader, DoomscrollingConfig.class);
			} catch (Exception e) {
				LOGGER.warn("Failed to read Doomscrolling config at {}; using defaults.", configPath, e);
			}
		}
		if (config == null) {
			config = new DoomscrollingConfig();
		}
		config.filePath = configPath;
		config.sanitize();
		config.save();
		return config;
	}

	public void sanitize() {
		if (layoutMode == null) layoutMode = LayoutMode.CARD;
		if (accentTheme == null) accentTheme = AccentTheme.YOUTUBE_RED;
		if (qualityPreset == null) qualityPreset = QualityPreset.BALANCED;
		posX = clampFloat(posX, 0.0f, 1.0f);
		posY = clampFloat(posY, 0.0f, 1.0f);
		hudWidth = clampInt(hudWidth, 100, 280);
		backgroundOpacity = clampFloat(backgroundOpacity, 0.45f, 0.98f);
		browserFps = snapFps(browserFps);
		autoScrollDelayMs = clampInt(autoScrollDelayMs, 0, 3000);
		volume = clampFloat(volume, 0.0f, 1.0f);
		if (startUrl == null || startUrl.isBlank()) {
			startUrl = DEFAULT_SHORTS_URL;
		}
	}

	public void save() {
		if (filePath == null) {
			return;
		}
		sanitize();
		try {
			Path parent = filePath.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Path tempFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
			Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException atomicFailure) {
			try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			} catch (IOException e) {
				LOGGER.warn("Failed to save Doomscrolling config to {}", filePath, e);
			}
		}
	}

	public void resetPosition() {
		posX = 0.985f;
		posY = 0.04f;
	}

	public int nextFpsCap() {
		return switch (browserFps) {
			case 24 -> 30;
			case 30 -> 60;
			default -> 24;
		};
	}

	public int nextAutoScrollDelayMs() {
		if (autoScrollDelayMs < 250) return 250;
		if (autoScrollDelayMs < 500) return 500;
		if (autoScrollDelayMs < 1000) return 1000;
		return 0;
	}

	public int backgroundArgb() {
		int alpha = clampInt(Math.round(backgroundOpacity * 255.0f), 115, 250);
		return (alpha << 24) | 0x0F0F0F;
	}

	private static int snapFps(int fps) {
		if (fps <= 26) return 24;
		if (fps <= 45) return 30;
		return 60;
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float clampFloat(float value, float min, float max) {
		if (Float.isNaN(value) || Float.isInfinite(value)) {
			return min;
		}
		return Math.max(min, Math.min(max, value));
	}
}
