package dev.doomscrolling.browser;

import de.keksuccino.rinku.Rinku;
import de.keksuccino.rinku.RinkuBrowser;
import dev.doomscrolling.config.DoomscrollingConfig;
import dev.doomscrolling.platform.McPlatform;
import dev.doomscrolling.ui.ActionFeedbackOverlay;
import dev.doomscrolling.ui.LunarDraw;
import net.minecraft.client.Minecraft;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandler;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

public final class ShortsBrowserController {

	private static final Logger LOGGER = LoggerFactory.getLogger("doomscrolling");
	private static final long TOAST_DURATION_MS = 1800L;

	public enum SurfaceMode {
		HUD,
		SIGN_IN
	}

	private final DoomscrollingConfig config;
	private final AtomicReference<ShortsState> state = new AtomicReference<>(ShortsState.INITIAL);
	private final AtomicReference<CommentsState> commentsState = new AtomicReference<>(CommentsState.CLOSED);
	private final ActionFeedbackOverlay feedbackOverlay = new ActionFeedbackOverlay();
	private volatile boolean commentsOpen = false;
	private volatile int commentsOpenGeneration = 0;

	private RinkuBrowser browser;
	private CefDisplayHandler displayHandler;
	private CefLoadHandler loadHandler;
	private boolean initHookRegistered = false;
	private volatile boolean rinkuInitFailed = false;

	private SurfaceMode surfaceMode = SurfaceMode.HUD;
	private boolean suspended = false;
	private boolean userPausedBeforeSuspend = false;

	private int currentBrowserWidth = 0;
	private int currentBrowserHeight = 0;
	private int appliedFpsCap = -1;
	private long lastPeriodicReinjectMs = 0L;
	private long unfocusedSinceMs = 0L;
	private volatile long lastScheduledInjectMs = 0L;

	private volatile String toastMessage = "";
	private volatile long toastExpiresAtMs = 0L;
	private volatile boolean firstShortLoaded = false;
	private volatile long firstShortLoadedAtMs = 0L;
	private volatile long loadingStartedAtMs = 0L;

	public ShortsBrowserController(DoomscrollingConfig config) {
		this.config = config;
		this.state.set(ShortsState.initialFor(config.platform));
	}

	public DoomscrollingConfig config() {
		return config;
	}

	public ShortsState state() {
		return state.get();
	}

	public CommentsState commentsState() {
		return commentsState.get();
	}

	public boolean isCommentsOpen() {
		return commentsOpen;
	}

	/** Increments every time comments are opened, so the panel can reset per open. */
	public int commentsOpenGeneration() {
		return commentsOpenGeneration;
	}

	public ActionFeedbackOverlay feedback() {
		return feedbackOverlay;
	}

	public RinkuBrowser browser() {
		return browser;
	}

	public boolean isRinkuInitFailed() {
		return rinkuInitFailed || (!Rinku.isInitialized() && !Rinku.isInitializationAllowed());
	}

	public boolean isBrowserTextureReady() {
		return browser != null && browser.isTextureReady();
	}

	public boolean isFirstShortLoaded() {
		return firstShortLoaded;
	}

	public float loadingOverlayAlpha() {
		if (!firstShortLoaded) {
			return 1.0f;
		}
		long elapsed = LunarDraw.nowMs() - firstShortLoadedAtMs;
		if (elapsed >= 280L) {
			return 0.0f;
		}
		float t = Math.max(0.0f, elapsed / 280.0f);
		return 1.0f - t * t;
	}

	private void resetInitialLoadingGate() {
		firstShortLoaded = false;
		firstShortLoadedAtMs = 0L;
		loadingStartedAtMs = LunarDraw.nowMs();
	}

	public Object getTextureIdentifier() {
		return browser != null ? McPlatform.get().getBrowserTexture(browser) : null;
	}

	public String activeToast() {
		if (toastMessage == null || toastMessage.isEmpty() || LunarDraw.nowMs() > toastExpiresAtMs) {
			return null;
		}
		return toastMessage;
	}

	public void showToast(String message) {
		this.toastMessage = message == null ? "" : message;
		this.toastExpiresAtMs = LunarDraw.nowMs() + TOAST_DURATION_MS;
	}

	public void tick(Minecraft minecraft, boolean overlayOpen) {
		boolean inWorld = minecraft.level != null;
		boolean shouldBeActive = overlayOpen || (inWorld && config.hudVisible);

		if (!shouldBeActive) {
			unfocusedSinceMs = 0L;
			if (browser != null && (config.pauseWhenHidden || !inWorld) && !suspended) {
				suspendBrowser();
			}
			return;
		}

		if (browser == null) {
			if (minecraft.isWindowActive()) {
				ensureBrowserCreated(minecraft);
			}
			return;
		}

		long now = LunarDraw.nowMs();
		if (!minecraft.isWindowActive()) {
			applyFpsCap(1);
			if (unfocusedSinceMs == 0L) {
				unfocusedSinceMs = now;
			} else if (config.pauseWhenHidden && !suspended && now - unfocusedSinceMs >= 1000L) {
				suspendBrowser();
			}
			return;
		}
		unfocusedSinceMs = 0L;

		if (suspended) {
			resumeBrowser();
		}

		if (surfaceMode == SurfaceMode.HUD) {
			applyTargetResolution(config.qualityPreset.width(), config.qualityPreset.height());
		}
		applyFpsCap(config.browserFps);

		if (now - lastPeriodicReinjectMs > 4000L && !browser.isLoading()) {
			lastPeriodicReinjectMs = now;
			injectBridgeIfMissing();
		}
		if (!firstShortLoaded && loadingStartedAtMs > 0L && now - loadingStartedAtMs > 12_000L && !browser.isLoading()) {
			firstShortLoaded = true;
			firstShortLoadedAtMs = now;
		}
	}

	private void ensureBrowserCreated(Minecraft minecraft) {
		if (browser != null || rinkuInitFailed) {
			return;
		}
		if (!Rinku.isInitialized()) {
			if (!initHookRegistered) {
				initHookRegistered = true;
				Rinku.scheduleForInit(success -> {
					if (!success) {
						rinkuInitFailed = true;
						return;
					}
					minecraft.execute(() -> {
						if (config.hudVisible && minecraft.level != null) {
							ensureBrowserCreated(minecraft);
						}
					});
				});
			}
			return;
		}

		try {
			int initialW = config.qualityPreset.width();
			int initialH = config.qualityPreset.height();
			String startUrl = config.effectiveStartUrl();
			browser = Rinku.createBrowser(startUrl, false, initialW, initialH);
			browser.useBrowserControls(false);
			resetInitialLoadingGate();
			currentBrowserWidth = initialW;
			currentBrowserHeight = initialH;
			applyFpsCap(config.browserFps);
			browser.setAudioMuted(config.muted);
			registerHandlers(minecraft);
			state.set(ShortsState.initialFor(config.platform).withUrl(startUrl, true));
		} catch (Throwable t) {
			LOGGER.error("Failed to create Doomscrolling browser", t);
		}
	}

	private void registerHandlers(Minecraft minecraft) {
		if (displayHandler == null) {
			displayHandler = new CefDisplayHandlerAdapter() {
				@Override
				public void onAddressChange(CefBrowser cefBrowser, CefFrame frame, String url) {
					if (!isOwnBrowser(cefBrowser) || frame == null || !frame.isMain()) {
						return;
					}
					state.set(state.get().withUrl(url, cefBrowser.isLoading()));
					scheduleInjectBridge(minecraft);
				}

				@Override
				public boolean onConsoleMessage(
						CefBrowser cefBrowser,
						CefSettings.LogSeverity level,
						String message,
						String source,
						int line
				) {
					if (!isOwnBrowser(cefBrowser) || message == null) {
						return false;
					}
					if (message.startsWith(YouTubeShortsBridge.MESSAGE_PREFIX)) {
						ShortsState parsed = YouTubeShortsBridge.parseStateMessage(message, System.nanoTime());
						if (parsed != null) {
							state.set(parsed);
							if (!firstShortLoaded && (parsed.videoReady() || parsed.botGateDetected())) {
								firstShortLoaded = true;
								firstShortLoadedAtMs = LunarDraw.nowMs();
							}
						}
						return true;
					}
					if (message.startsWith(YouTubeShortsBridge.COMMENTS_PREFIX)) {
						if (commentsOpen) {
							CommentsState parsedComments = YouTubeShortsBridge.parseCommentsMessage(message);
							if (parsedComments != null && parsedComments.open()) {
								commentsState.set(parsedComments);
							}
						}
						return true;
					}
					if (message.startsWith(YouTubeShortsBridge.EVENT_PREFIX)) {
						handleBridgeEvent(YouTubeShortsBridge.parseEventMessage(message));
						return true;
					}
					return false;
				}
			};
			Rinku.getClient().addDisplayHandler(displayHandler);
		}

		if (loadHandler == null) {
			loadHandler = new CefLoadHandlerAdapter() {
				@Override
				public void onLoadingStateChange(CefBrowser cefBrowser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
					if (!isOwnBrowser(cefBrowser)) return;
					state.set(state.get().withUrl(cefBrowser.getURL(), isLoading));
					if (!isLoading) {
						scheduleInjectBridge(minecraft);
					}
				}

				@Override
				public void onLoadStart(CefBrowser cefBrowser, CefFrame frame, CefRequest.TransitionType transitionType) {
					if (!isOwnBrowser(cefBrowser) || frame == null || !frame.isMain()) return;
					state.set(state.get().withUrl(cefBrowser.getURL(), true));
				}

				@Override
				public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
					if (!isOwnBrowser(cefBrowser) || frame == null || !frame.isMain()) return;
					scheduleInjectBridge(minecraft);
				}
			};
			Rinku.getClient().addLoadHandler(loadHandler);
		}
	}

	private void scheduleInjectBridge(Minecraft minecraft) {
		if (suspended) return;
		long now = LunarDraw.nowMs();
		if (now - lastScheduledInjectMs < 400L) return;
		lastScheduledInjectMs = now;
		minecraft.execute(() -> {
			if (browser != null && !suspended) {
				injectBridge();
			}
		});
	}

	private void handleBridgeEvent(String event) {
		if (event == null || event.isEmpty()) return;
		switch (event) {
			case "signin_required" -> showToast("Sign in ( ] ) to like");
			case "liked" -> feedbackOverlay.trigger(ActionFeedbackOverlay.FeedbackType.LIKE);
			case "unliked" -> feedbackOverlay.trigger(ActionFeedbackOverlay.FeedbackType.UNLIKE);
			default -> {
			}
		}
	}

	private boolean isOwnBrowser(CefBrowser candidate) {
		return browser != null && candidate != null && candidate.getIdentifier() == browser.getIdentifier();
	}

	private void suspendBrowser() {
		if (browser == null || suspended) return;
		suspended = true;
		userPausedBeforeSuspend = state.get().paused();
		executeJs(YouTubeShortsBridge.buildActionScript("setPaused(true)"));
		browser.setAudioMuted(true);
		applyFpsCap(1);
	}

	private void resumeBrowser() {
		if (browser == null || !suspended) return;
		suspended = false;
		applyFpsCap(config.browserFps);
		browser.setAudioMuted(config.muted);
		if (!userPausedBeforeSuspend) {
			executeJs(YouTubeShortsBridge.buildActionScript("setPaused(false)"));
		}
		syncBridgeConfig();
	}

	public void setSurfaceMode(SurfaceMode mode, int targetWidth, int targetHeight) {
		this.surfaceMode = mode;
		if (browser == null) {
			ensureBrowserCreated(Minecraft.getInstance());
		}
		if (browser == null) {
			return;
		}
		if (suspended) {
			resumeBrowser();
		}
		applyTargetResolution(targetWidth, targetHeight);
		applyFpsCap(mode == SurfaceMode.HUD ? config.browserFps : Math.max(config.browserFps, 30));
		syncBridgeConfig();
	}

	public void returnToHudMode() {
		this.surfaceMode = SurfaceMode.HUD;
		if (browser == null) return;
		applyTargetResolution(config.qualityPreset.width(), config.qualityPreset.height());
		applyFpsCap(config.browserFps);
		syncBridgeConfig();
		if (!config.hudVisible && config.pauseWhenHidden) {
			suspendBrowser();
		}
	}

	public void applyTargetResolution(int width, int height) {
		if (browser == null) return;
		int clampedW = Math.max(160, Math.min(1280, width));
		int clampedH = Math.max(160, Math.min(960, height));
		if (clampedW != currentBrowserWidth || clampedH != currentBrowserHeight) {
			currentBrowserWidth = clampedW;
			currentBrowserHeight = clampedH;
			browser.resize(clampedW, clampedH);
		}
	}

	public void applyFpsCap(int fps) {
		if (browser == null) return;
		int clamped = Math.max(1, Math.min(60, fps));
		if (clamped != appliedFpsCap) {
			appliedFpsCap = clamped;
			try {
				browser.setWindowlessFrameRate(clamped);
			} catch (Throwable ignored) {
			}
		}
	}

	public void injectBridge() {
		if (browser == null) return;
		boolean hudMode = (surfaceMode == SurfaceMode.HUD);
		boolean showCaption = hudMode && config.layoutMode == DoomscrollingConfig.LayoutMode.CARD;
		executeJs(YouTubeShortsBridge.buildBootstrapScript(
				hudMode,
				showCaption,
				config.autoScroll,
				config.autoScrollDelayMs,
				config.volume,
				config.muted,
				config.subtitles
		));
		resyncCommentsIfOpen();
	}

	private void resyncCommentsIfOpen() {
		if (commentsOpen) {
			executeJs(YouTubeShortsBridge.buildCommentsResyncScript());
		}
	}

	private void injectBridgeIfMissing() {
		if (browser == null) return;
		boolean hudMode = (surfaceMode == SurfaceMode.HUD);
		boolean showCaption = hudMode && config.layoutMode == DoomscrollingConfig.LayoutMode.CARD;
		String bootstrap = YouTubeShortsBridge.buildBootstrapScript(
				hudMode,
				showCaption,
				config.autoScroll,
				config.autoScrollDelayMs,
				config.volume,
				config.muted,
				config.subtitles
		);
		executeJs("if(!window.__doomscrolling){" + bootstrap + "}");
		resyncCommentsIfOpen();
	}

	public void syncBridgeConfig() {
		if (browser == null) return;
		browser.setAudioMuted(config.muted || suspended);
		boolean hudMode = (surfaceMode == SurfaceMode.HUD);
		boolean showCaption = hudMode && config.layoutMode == DoomscrollingConfig.LayoutMode.CARD;
		executeJs(YouTubeShortsBridge.buildSyncConfigScript(
				hudMode,
				showCaption,
				config.autoScroll,
				config.autoScrollDelayMs,
				config.volume,
				config.muted,
				config.subtitles
		));
	}

	public void toggleHudVisible() {
		config.hudVisible = !config.hudVisible;
		config.save();
		if (config.hudVisible) {
			ensureBrowserCreated(Minecraft.getInstance());
			if (suspended) resumeBrowser();
		} else {
			setCommentsOpen(false);
			if (config.pauseWhenHidden) {
				suspendBrowser();
			}
		}
	}

	public void toggleComments() {
		setCommentsOpen(!commentsOpen);
	}

	public void setCommentsOpen(boolean open) {
		if (open == commentsOpen) return;
		if (open && !ensureReadyForAction()) return;
		commentsOpen = open;
		if (open) {
			commentsOpenGeneration++;
		}
		commentsState.set(open ? CommentsState.loadingFor(state.get().videoId()) : CommentsState.CLOSED);
		executeJs(YouTubeShortsBridge.buildSetCommentsOpenScript(open));
	}

	public void nextShort() {
		if (!ensureReadyForAction()) return;
		executeJs(YouTubeShortsBridge.buildActionScript("next()"));
		feedbackOverlay.trigger(ActionFeedbackOverlay.FeedbackType.NEXT);
	}

	public void previousShort() {
		if (!ensureReadyForAction()) return;
		executeJs(YouTubeShortsBridge.buildActionScript("prev()"));
		feedbackOverlay.trigger(ActionFeedbackOverlay.FeedbackType.PREV);
	}

	public void togglePlayPause() {
		if (!ensureReadyForAction()) return;
		boolean willPause = !state.get().paused();
		userPausedBeforeSuspend = willPause;
		executeJs(YouTubeShortsBridge.buildActionScript("togglePlayPause()"));
		feedbackOverlay.trigger(willPause
				? ActionFeedbackOverlay.FeedbackType.PAUSE
				: ActionFeedbackOverlay.FeedbackType.PLAY);
	}

	public void toggleLike() {
		if (!ensureReadyForAction()) return;
		ShortsState current = state.get();
		if (current.signedIn()) {
			feedbackOverlay.trigger(current.liked()
					? ActionFeedbackOverlay.FeedbackType.UNLIKE
					: ActionFeedbackOverlay.FeedbackType.LIKE);
		}
		executeJs(YouTubeShortsBridge.buildActionScript("toggleLike()"));
	}

	public void toggleAutoScroll() {
		config.autoScroll = !config.autoScroll;
		config.save();
		syncBridgeConfig();
		feedbackOverlay.trigger(config.autoScroll
				? ActionFeedbackOverlay.FeedbackType.AUTOSCROLL_ON
				: ActionFeedbackOverlay.FeedbackType.AUTOSCROLL_OFF);
	}

	public void toggleMute() {
		config.muted = !config.muted;
		config.save();
		syncBridgeConfig();
		feedbackOverlay.trigger(config.muted
				? ActionFeedbackOverlay.FeedbackType.MUTE
				: ActionFeedbackOverlay.FeedbackType.UNMUTE);
	}

	public void toggleSubtitles() {
		config.subtitles = !config.subtitles;
		config.save();
		syncBridgeConfig();
		feedbackOverlay.trigger(config.subtitles
				? ActionFeedbackOverlay.FeedbackType.CAPTIONS_ON
				: ActionFeedbackOverlay.FeedbackType.CAPTIONS_OFF);
	}

	public void setVolume(float volume) {
		config.volume = Math.max(0.0f, Math.min(1.0f, volume));
		if (config.volume > 0.01f && config.muted) {
			config.muted = false;
		}
		config.save();
		syncBridgeConfig();
	}

	public void seekToRatio(float ratio) {
		if (!ensureReadyForAction()) return;
		executeJs(YouTubeShortsBridge.buildSeekScript(ratio));
	}

	public void cycleLayoutMode() {
		config.layoutMode = config.layoutMode.next();
		config.save();
		syncBridgeConfig();
	}

	public void navigateTo(String url) {
		if (url == null || url.isBlank()) return;
		String normalized = normalizeUrl(url.trim());
		if (browser == null) {
			ensureBrowserCreated(Minecraft.getInstance());
		}
		if (browser != null) {
			browser.loadURL(normalized);
		}
	}

	public void setPlatform(DoomscrollingConfig.FeedPlatform platform) {
		if (platform == null || config.platform == platform) {
			return;
		}
		config.platform = platform;
		config.startUrl = platform.feedUrl();
		config.save();
		userPausedBeforeSuspend = false;
		resetInitialLoadingGate();
		state.set(ShortsState.initialFor(platform));
		if (commentsOpen) {
			commentsState.set(CommentsState.loadingFor(""));
		}
		showToast(platform.displayName());
		if (browser != null) {
			if (suspended) {
				resumeBrowser();
			}
			browser.loadURL(platform.feedUrl());
		}
	}

	public void cyclePlatform() {
		DoomscrollingConfig.FeedPlatform current = config.platform != null
				? config.platform
				: DoomscrollingConfig.FeedPlatform.YOUTUBE_SHORTS;
		setPlatform(current.next());
	}

	public void openSignInPage() {
		DoomscrollingConfig.FeedPlatform active = config.platform != null
				? config.platform
				: DoomscrollingConfig.FeedPlatform.YOUTUBE_SHORTS;
		navigateTo(active.signInUrl());
	}

	public void openShortsFeed() {
		resetInitialLoadingGate();
		state.set(ShortsState.initialFor(config.platform));
		navigateTo(config.effectiveStartUrl());
	}

	public void reloadPage() {
		if (browser != null) {
			if (surfaceMode == SurfaceMode.HUD) {
				resetInitialLoadingGate();
			}
			browser.reload();
		}
	}

	public void applySettingsChanged() {
		config.save();
		if (browser != null) {
			if (surfaceMode == SurfaceMode.HUD) {
				applyTargetResolution(config.qualityPreset.width(), config.qualityPreset.height());
			}
			applyFpsCap(suspended ? 1 : config.browserFps);
			syncBridgeConfig();
		}
	}

	public void shutdown() {
		try {
			if (displayHandler != null && Rinku.isInitialized()) {
				Rinku.getClient().removeDisplayHandler(displayHandler);
			}
		} catch (Throwable ignored) {
		}
		displayHandler = null;
		loadHandler = null;

		RinkuBrowser toClose = this.browser;
		this.browser = null;
		if (toClose != null) {
			try {
				toClose.close(true);
			} catch (Throwable t) {
				LOGGER.warn("Error closing Doomscrolling browser", t);
			}
		}
	}

	private boolean ensureReadyForAction() {
		if (browser == null) {
			ensureBrowserCreated(Minecraft.getInstance());
			if (browser == null) {
				return false;
			}
		}
		if (suspended) {
			resumeBrowser();
		}
		return true;
	}

	private void executeJs(String script) {
		RinkuBrowser active = this.browser;
		if (active == null || script == null || script.isEmpty()) {
			return;
		}
		try {
			active.executeJavaScript(script, "doomscrolling://bridge.js", 0);
		} catch (Throwable t) {
			LOGGER.warn("Failed to execute Doomscrolling bridge JS", t);
		}
	}

	private static String normalizeUrl(String input) {
		if (input.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
			return input;
		}
		return "https://" + input;
	}
}
