package dev.doomscrolling.browser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class YouTubeShortsBridge {

	public static final String MESSAGE_PREFIX = "__DS__:";
	public static final String EVENT_PREFIX = "__DS_EVT__:";
	public static final String COMMENTS_PREFIX = "__DS_CMT__:";

	private YouTubeShortsBridge() {
	}

	public static String buildBootstrapScript(
			boolean hudMode,
			boolean showCaption,
			boolean autoScroll,
			int autoScrollDelayMs,
			float volume,
			boolean muted,
			boolean subtitles
	) {
		String template = BOOTSTRAP_TEMPLATE.replace("__DS_INSTAGRAM_BRANCH__", InstagramReelsBridge.INSTAGRAM_BRANCH_SCRIPT);
		return template
				.replace("__DS_HUD_FLAG__", hudMode ? "1" : "0")
				.replace("__DS_SHOW_CAPTION__", showCaption ? "true" : "false")
				.replace("__DS_AUTO_SCROLL__", autoScroll ? "true" : "false")
				.replace("__DS_AUTO_DELAY__", Integer.toString(Math.max(0, autoScrollDelayMs)))
				.replace("__DS_VOLUME__", String.format(Locale.ROOT, "%.3f", clamp01(volume)))
				.replace("__DS_MUTED__", muted ? "true" : "false")
				.replace("__DS_SUBTITLES__", subtitles ? "true" : "false");
	}

	public static String buildSyncConfigScript(
			boolean hudMode,
			boolean showCaption,
			boolean autoScroll,
			int autoScrollDelayMs,
			float volume,
			boolean muted,
			boolean subtitles
	) {
		return String.format(
				Locale.ROOT,
				"if(window.__doomscrolling){window.__doomscrolling.configure(%s,%s,%s,%d,%.3f,%s,%s);}",
				hudMode ? "true" : "false",
				showCaption ? "true" : "false",
				autoScroll ? "true" : "false",
				Math.max(0, autoScrollDelayMs),
				clamp01(volume),
				muted ? "true" : "false",
				subtitles ? "true" : "false"
		);
	}

	public static String buildActionScript(String methodCall) {
		return "if(window.__doomscrolling){window.__doomscrolling." + methodCall + ";}";
	}

	public static String buildSetCommentsOpenScript(boolean open) {
		return buildActionScript("setCommentsOpen(" + open + ")");
	}

	/**
	 * Re-opens comments on a freshly loaded page (full reloads reset the page-side state)
	 * without restarting them on pages where they are already open.
	 */
	public static String buildCommentsResyncScript() {
		return "if(window.__doomscrolling&&window.__doomscrolling.isCommentsOpen"
				+ "&&!window.__doomscrolling.isCommentsOpen()){window.__doomscrolling.setCommentsOpen(true);}";
	}

	public static String buildSeekScript(float ratio) {
		return String.format(
				Locale.ROOT,
				"if(window.__doomscrolling){window.__doomscrolling.seekRatio(%.4f);}",
				clamp01(ratio)
		);
	}

	public static ShortsState parseStateMessage(String consoleMessage, long nowNanos) {
		if (consoleMessage == null || !consoleMessage.startsWith(MESSAGE_PREFIX)) {
			return null;
		}
		String jsonText = consoleMessage.substring(MESSAGE_PREFIX.length());
		try {
			JsonElement element = JsonParser.parseString(jsonText);
			if (!element.isJsonObject()) {
				return null;
			}
			JsonObject obj = element.getAsJsonObject();
			String url = getString(obj, "url", "");
			String videoId = getString(obj, "id", "");
			String title = getString(obj, "title", "");
			String channel = getString(obj, "channel", "");
			double time = getDouble(obj, "t", 0.0);
			double duration = getDouble(obj, "d", 0.0);
			float rate = (float) getDouble(obj, "r", 1.0);
			boolean paused = getBoolean(obj, "p", true);
			boolean muted = getBoolean(obj, "m", false);
			boolean liked = getBoolean(obj, "l", false);
			boolean signedIn = getBoolean(obj, "s", false);
			boolean onShorts = getBoolean(obj, "sh", ShortsState.isFeedUrl(url));
			boolean buffering = getBoolean(obj, "b", false);
			boolean videoReady = getBoolean(obj, "vr", false);
			boolean botGate = getBoolean(obj, "bg", false);

			return new ShortsState(
					videoId,
					title,
					channel,
					Math.max(0.0, time),
					Math.max(0.0, duration),
					nowNanos,
					rate > 0.05f ? rate : 1.0f,
					paused,
					muted,
					liked,
					signedIn,
					onShorts,
					false,
					buffering,
					videoReady,
					botGate,
					url
			);
		} catch (Exception ignored) {
			return null;
		}
	}

	public static String parseEventMessage(String consoleMessage) {
		if (consoleMessage == null || !consoleMessage.startsWith(EVENT_PREFIX)) {
			return null;
		}
		return consoleMessage.substring(EVENT_PREFIX.length()).trim();
	}

	public static CommentsState parseCommentsMessage(String consoleMessage) {
		if (consoleMessage == null || !consoleMessage.startsWith(COMMENTS_PREFIX)) {
			return null;
		}
		String jsonText = consoleMessage.substring(COMMENTS_PREFIX.length());
		try {
			JsonElement element = JsonParser.parseString(jsonText);
			if (!element.isJsonObject()) {
				return null;
			}
			JsonObject obj = element.getAsJsonObject();
			String videoId = getString(obj, "id", "");
			String totalLabel = getString(obj, "cnt", "");
			boolean open = getBoolean(obj, "open", false);
			boolean loading = getBoolean(obj, "loading", false);
			boolean signInRequired = getBoolean(obj, "sr", false);
			List<CommentsState.CommentItem> items = new ArrayList<>();
			JsonElement arrEl = obj.get("items");
			if (arrEl != null && arrEl.isJsonArray()) {
				JsonArray arr = arrEl.getAsJsonArray();
				int limit = Math.min(arr.size(), 30);
				for (int i = 0; i < limit; i++) {
					JsonElement itemEl = arr.get(i);
					if (itemEl == null || !itemEl.isJsonObject()) continue;
					JsonObject itemObj = itemEl.getAsJsonObject();
					String author = getString(itemObj, "a", "").trim();
					String text = getString(itemObj, "x", "").trim();
					String likes = getString(itemObj, "l", "").trim();
					String timeAgo = getString(itemObj, "t", "").trim();
					if (!text.isEmpty()) {
						items.add(new CommentsState.CommentItem(author, text, likes, timeAgo));
					}
				}
			}
			return new CommentsState(videoId, totalLabel, open, loading, signInRequired, List.copyOf(items));
		} catch (Exception ignored) {
			return null;
		}
	}

	public static String sanitizeForMinecraftFont(String input) {
		if (input == null || input.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder(input.length());
		int i = 0;
		while (i < input.length()) {
			int cp = input.codePointAt(i);
			i += Character.charCount(cp);
			if (cp > 0xFFFF || cp == 0x200D || cp == 0xFE0F || cp == 0xFE0E
					|| (cp >= 0x2600 && cp <= 0x27BF)) {
				continue;
			}
			sb.appendCodePoint(cp);
		}
		return sb.toString().replaceAll("\\s{2,}", " ").trim();
	}

	private static String getString(JsonObject obj, String key, String fallback) {
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull()) return fallback;
		try {
			return el.getAsString();
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static double getDouble(JsonObject obj, String key, double fallback) {
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull()) return fallback;
		try {
			double v = el.getAsDouble();
			return (Double.isNaN(v) || Double.isInfinite(v)) ? fallback : v;
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull()) return fallback;
		try {
			return el.getAsBoolean();
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static float clamp01(float value) {
		return Math.max(0.0f, Math.min(1.0f, value));
	}

	private static final String BOOTSTRAP_TEMPLATE = """
			(function() {
			  const host = ((location && location.hostname) || '').toLowerCase();
			  const isYouTube = host.includes('youtube.com');
			  const isInstagram = host.includes('instagram.com');
			  if (isInstagram) {
			__DS_INSTAGRAM_BRANCH__
			    return;
			  }
			  if (!isYouTube) {
			    document.documentElement.dataset.dsHud = '0';
			    const oldCap = document.getElementById('ds-caption-overlay');
			    if (oldCap) oldCap.remove();
			    try {
			      console.info('__DS__:' + JSON.stringify({
			        id: '', title: document.title || 'Sign In', channel: host,
			        t: 0, d: 0, r: 1, p: true, m: false, l: false, s: false, sh: false, b: false, url: location.href
			      }));
			    } catch (_) {}
			    return;
			  }
			  document.documentElement.dataset.dsPlatform = 'youtube';

			  const STYLE_ID = 'doomscrolling-clean-css';
			  if (!document.getElementById(STYLE_ID)) {
			    const style = document.createElement('style');
			    style.id = STYLE_ID;
			    style.textContent = `
			      html[data-ds-hud="1"],
			      html[data-ds-hud="1"] body {
			        background: #0f0f0f !important;
			        overflow: hidden !important;
			        margin: 0 !important;
			        padding: 0 !important;
			        scrollbar-width: none !important;
			      }
			      html[data-ds-hud="1"] ::-webkit-scrollbar { display: none !important; }
			      html[data-ds-hud="1"] ytd-masthead,
			      html[data-ds-hud="1"] #masthead-container,
			      html[data-ds-hud="1"] tp-yt-app-drawer,
			      html[data-ds-hud="1"] ytd-mini-guide-renderer,
			      html[data-ds-hud="1"] #guide,
			      html[data-ds-hud="1"] #cinematics,
			      html[data-ds-hud="1"] ytd-shorts-player-controls,
			      html[data-ds-hud="1"] ytd-reel-player-overlay-renderer,
			      html[data-ds-hud="1"] reel-player-overlay-renderer,
			      html[data-ds-hud="1"] #overlay.ytd-reel-video-renderer,
			      html[data-ds-hud="1"] .overlay.ytd-reel-video-renderer,
			      html[data-ds-hud="1"] #actions.ytd-reel-player-overlay-renderer,
			      html[data-ds-hud="1"] .navigation-container.ytd-shorts,
			      html[data-ds-hud="1"] #navigation-button-up,
			      html[data-ds-hud="1"] #navigation-button-down,
			      html[data-ds-hud="1"] #shorts-panel-container,
			      html[data-ds-hud="1"] ytd-engagement-panel-section-list-renderer,
			      html[data-ds-hud="1"] .ytp-chrome-top,
			      html[data-ds-hud="1"] .ytp-chrome-bottom,
			      html[data-ds-hud="1"] .ytp-bezel,
			      html[data-ds-hud="1"] .ytp-bezel-text-wrapper,
			      html[data-ds-hud="1"] .ytp-gradient-top,
			      html[data-ds-hud="1"] .ytp-gradient-bottom,
			      html[data-ds-hud="1"] ytd-popup-container tp-yt-paper-dialog,
			      html[data-ds-hud="1"] ytd-consent-bump-v2-lightbox {
			        display: none !important;
			        visibility: hidden !important;
			        opacity: 0 !important;
			        pointer-events: none !important;
			      }
			      html[data-ds-hud="1"][data-ds-comments="1"] #shorts-panel-container,
			      html[data-ds-hud="1"][data-ds-comments="1"] ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-comments-section"] {
			        display: block !important;
			        visibility: visible !important;
			        opacity: 0 !important;
			        pointer-events: none !important;
			        position: fixed !important;
			        left: 0 !important;
			        top: 0 !important;
			        width: 360px !important;
			        height: 640px !important;
			        z-index: 1 !important;
			      }
			      html[data-ds-hud="1"] ytd-page-manager,
			      html[data-ds-hud="1"] #page-manager,
			      html[data-ds-hud="1"] ytd-shorts,
			      html[data-ds-hud="1"] #shorts-container,
			      html[data-ds-hud="1"] #shorts-inner-container {
			        margin: 0 !important;
			        padding: 0 !important;
			        top: 0 !important;
			        left: 0 !important;
			        width: 100vw !important;
			        height: 100vh !important;
			        max-width: 100vw !important;
			        max-height: 100vh !important;
			        background: #0f0f0f !important;
			      }
			      html[data-ds-hud="1"] ytd-reel-video-renderer[is-active] {
			        position: fixed !important;
			        inset: 0 !important;
			        margin: 0 !important;
			        padding: 0 !important;
			        width: 100vw !important;
			        height: 100vh !important;
			        overflow: hidden !important;
			        border-radius: 0 !important;
			        z-index: 10 !important;
			      }
			      html[data-ds-hud="1"] ytd-reel-video-renderer:not([is-active]) {
			        visibility: hidden !important;
			      }
			      html[data-ds-hud="1"] ytd-reel-video-renderer[is-active] #player-container,
			      html[data-ds-hud="1"] ytd-reel-video-renderer[is-active] #player-container-outer,
			      html[data-ds-hud="1"] ytd-reel-video-renderer[is-active] #player-container-inner,
			      html[data-ds-hud="1"] ytd-reel-video-renderer[is-active] #shorts-player,
			      html[data-ds-hud="1"] ytd-reel-video-renderer[is-active] .html5-video-player,
			      html[data-ds-hud="1"] ytd-reel-video-renderer[is-active] .html5-video-container {
			        position: fixed !important;
			        inset: 0 !important;
			        width: 100vw !important;
			        height: 100vh !important;
			        max-width: 100vw !important;
			        max-height: 100vh !important;
			        margin: 0 !important;
			        padding: 0 !important;
			        border-radius: 0 !important;
			        background: #0f0f0f !important;
			      }
			      html[data-ds-hud="1"] ytd-reel-video-renderer[is-active] video.html5-main-video,
			      html[data-ds-hud="1"] ytd-reel-video-renderer[is-active] video {
			        position: fixed !important;
			        inset: 0 !important;
			        width: 100vw !important;
			        height: 100vh !important;
			        left: 0 !important;
			        top: 0 !important;
			        object-fit: cover !important;
			        border-radius: 0 !important;
			      }
			      html[data-ds-hud="1"] * {
			        animation-duration: 0.001ms !important;
			        transition-duration: 0.001ms !important;
			      }
			      #ds-caption-overlay {
			        display: none;
			        position: fixed;
			        left: 0;
			        right: 0;
			        bottom: 0;
			        z-index: 2147483647;
			        pointer-events: none;
			        padding: 46px 56px 14px 12px;
			        background: linear-gradient(to top, rgba(15, 15, 15, 0.90) 0%, rgba(15, 15, 15, 0.48) 58%, rgba(15, 15, 15, 0) 100%);
			        font-family: "YouTube Sans", "Roboto", "Segoe UI", "Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", sans-serif;
			        box-sizing: border-box;
			      }
			      html[data-ds-hud="1"] #ds-caption-overlay.ds-visible {
			        display: block !important;
			      }
			      #ds-caption-channel {
			        color: #ffffff;
			        font-size: 13px;
			        font-weight: 700;
			        line-height: 1.25;
			        text-shadow: 0 1px 3px rgba(0, 0, 0, 0.85);
			        white-space: nowrap;
			        overflow: hidden;
			        text-overflow: ellipsis;
			        margin-bottom: 3px;
			      }
			      #ds-caption-title {
			        color: #f1f1f1;
			        font-size: 12px;
			        font-weight: 400;
			        line-height: 1.3;
			        text-shadow: 0 1px 3px rgba(0, 0, 0, 0.85);
			        display: -webkit-box;
			        -webkit-line-clamp: 2;
			        -webkit-box-orient: vertical;
			        overflow: hidden;
			        word-break: break-word;
			      }
			      html[data-ds-hud="1"][data-ds-cc="0"] .ytp-caption-window-container {
			        display: none !important;
			        visibility: hidden !important;
			        opacity: 0 !important;
			      }
			      html[data-ds-hud="1"][data-ds-cc="1"] .ytp-caption-window-container {
			        display: block !important;
			        visibility: visible !important;
			        opacity: 1 !important;
			        position: fixed !important;
			        left: 4% !important;
			        right: 4% !important;
			        bottom: 66px !important;
			        top: auto !important;
			        width: 92% !important;
			        height: auto !important;
			        z-index: 2147483646 !important;
			        pointer-events: none !important;
			      }
			      html[data-ds-hud="1"][data-ds-cc="1"] .caption-window {
			        position: relative !important;
			        left: auto !important;
			        right: auto !important;
			        top: auto !important;
			        bottom: auto !important;
			        margin: 0 auto !important;
			        width: 100% !important;
			        max-width: 100% !important;
			        text-align: center !important;
			        transform: none !important;
			        background: transparent !important;
			      }
			      html[data-ds-hud="1"][data-ds-cc="1"] .captions-text {
			        display: inline !important;
			        white-space: normal !important;
			      }
			      html[data-ds-hud="1"][data-ds-cc="1"] .ytp-caption-segment {
			        font-family: "YouTube Sans", "Roboto", "Segoe UI", sans-serif !important;
			        font-size: clamp(17px, 5.6vw, 30px) !important;
			        line-height: 1.28 !important;
			        font-weight: 700 !important;
			        padding: 2px 7px !important;
			        border-radius: 4px !important;
			        background: rgba(12, 12, 12, 0.80) !important;
			        color: #ffffff !important;
			        text-shadow: 0 1px 3px rgba(0, 0, 0, 0.92) !important;
			        white-space: pre-wrap !important;
			      }
			    `;
			    (document.head || document.documentElement).appendChild(style);
			  }

			  function ensureCaptionOverlay() {
			    let box = document.getElementById('ds-caption-overlay');
			    if (!box && document.body) {
			      box = document.createElement('div');
			      box.id = 'ds-caption-overlay';
			      const ch = document.createElement('div');
			      ch.id = 'ds-caption-channel';
			      const ti = document.createElement('div');
			      ti.id = 'ds-caption-title';
			      box.appendChild(ch);
			      box.appendChild(ti);
			      document.body.appendChild(box);
			    }
			    return box;
			  }

			  if (!window.__doomscrolling) {
			    const state = {
			      hudMode: true,
			      showCaption: true,
			      autoScroll: true,
			      autoScrollDelayMs: 250,
			      volume: 0.65,
			      muted: false,
			      subtitles: false,
			      commentsOpen: false,
			      commentsOpenedForId: '',
			      commentsOpenAtMs: 0,
			      lastCommentsKey: '',
			      lastCommentsPushMs: 0,
			      commentsClickedAtMs: 0,
			      commentsRetryId: '',
			      commentsRetries: 0,
			      lastSubtitlesSyncId: '',
			      lastPushMs: 0,
			      lastKey: '',
			      lastVideoId: '',
			      lastTime: 0,
			      autoScrollTimer: null,
			      autoScrolledForId: '',
			      lastNavMs: 0
			    };

			    function getShortsId() {
			      const m = location.pathname.match(/\\/shorts\\/([a-zA-Z0-9_-]+)/);
			      return m ? m[1] : '';
			    }

			    function getActiveReel() {
			      const active = document.querySelector('ytd-reel-video-renderer[is-active]');
			      if (active) return active;
			      const reels = document.querySelectorAll('ytd-reel-video-renderer');
			      let best = null;
			      let bestVis = -1;
			      for (const r of reels) {
			        const rect = r.getBoundingClientRect();
			        const vis = Math.max(0, Math.min(rect.bottom, window.innerHeight) - Math.max(rect.top, 0));
			        if (vis > bestVis) { bestVis = vis; best = r; }
			      }
			      return best;
			    }

			    function getActiveVideo() {
			      const reel = getActiveReel();
			      if (reel) {
			        const v = reel.querySelector('video.html5-main-video') || reel.querySelector('video');
			        if (v) return v;
			      }
			      const videos = document.querySelectorAll('video');
			      for (const v of videos) {
			        if (v.readyState > 0 && !v.paused) return v;
			      }
			      return videos.length > 0 ? videos[0] : null;
			    }

			    function pickText(root, selectors) {
			      if (!root) return '';
			      for (const sel of selectors) {
			        const el = root.querySelector(sel);
			        if (el) {
			          const txt = (el.textContent || '').replace(/\\s+/g, ' ').trim();
			          if (txt.length > 0) return txt;
			        }
			      }
			      return '';
			    }

			    function getTitle(reel) {
			      let title = pickText(reel, [
			        'h2.ytShortsVideoTitleViewModelShortsVideoTitle',
			        '.ytShortsVideoTitleViewModelShortsVideoTitle',
			        '.ytShortsVideoTitleViewModelHost',
			        'yt-formatted-string.ytd-reel-player-header-renderer',
			        '#overlay h2',
			        'h2'
			      ]);
			      if (!title) {
			        title = (document.title || '').replace(/\\s*-\\s*YouTube\\s*$/i, '').trim();
			      }
			      if (title.length > 140) title = title.slice(0, 140);
			      return title;
			    }

			    function getChannel(reel) {
			      let ch = pickText(reel, [
			        'a.yt-core-attributed-string__link[href*="/@"]',
			        'yt-reel-channel-bar-view-model a',
			        '.ytReelChannelBarViewModelChannelName a',
			        '.ytReelChannelBarViewModelChannelName',
			        'reel-channel-bar-view-model a',
			        'ytd-channel-name a',
			        '#channel-name a',
			        '#channel-name',
			        'a[href*="/@"]'
			      ]);
			      if (!ch && reel) {
			        const links = reel.querySelectorAll('a, span');
			        for (const el of links) {
			          const t = (el.textContent || '').trim();
			          if (t.startsWith('@') && t.length >= 2 && t.length <= 40 && !t.includes(' ')) {
			            ch = t;
			            break;
			          }
			        }
			      }
			      if (ch.length > 64) ch = ch.slice(0, 64);
			      return ch;
			    }

			    function getLikeButton(reel) {
			      if (!reel) return null;
			      return reel.querySelector('like-button-view-model button')
			        || reel.querySelector('#like-button button')
			        || reel.querySelector('button[aria-label*="like this video" i]')
			        || reel.querySelector('button[aria-label*="Like" i]');
			    }

			    function isLiked(reel) {
			      const btn = getLikeButton(reel);
			      if (!btn) return false;
			      return btn.getAttribute('aria-pressed') === 'true';
			    }

			    function isSignedIn() {
			      try {
			        if (window.ytcfg && typeof window.ytcfg.get === 'function') {
			          const logged = window.ytcfg.get('LOGGED_IN');
			          if (typeof logged === 'boolean') return logged;
			        }
			      } catch (_) {}
			      if (document.querySelector('#avatar-btn, button#avatar-btn, img#img[alt*="Avatar" i]')) return true;
			      return false;
			    }

			    function updateNativeCaption(title, channel, onShorts, hasVideo) {
			      const box = ensureCaptionOverlay();
			      if (!box) return;
			      const shouldShow = state.hudMode && state.showCaption && onShorts && (title || channel);
			      if (!shouldShow) {
			        box.classList.remove('ds-visible');
			        return;
			      }
			      box.classList.add('ds-visible');
			      const chEl = document.getElementById('ds-caption-channel');
			      const tiEl = document.getElementById('ds-caption-title');
			      if (chEl && chEl.textContent !== channel) chEl.textContent = channel || '';
			      if (tiEl && tiEl.textContent !== title) tiEl.textContent = title || '';
			    }

			    function applyAudio(video) {
			      if (!video) return;
			      try {
			        if (Math.abs(video.volume - state.volume) > 0.01) video.volume = state.volume;
			        if (video.muted !== state.muted) video.muted = state.muted;
			      } catch (_) {}
			    }

			    function applySubtitles(force) {
			      document.documentElement.dataset.dsCc = state.subtitles ? '1' : '0';
			      const id = getShortsId();
			      if (!force && state.lastSubtitlesSyncId === (id + ':' + state.subtitles)) {
			        return;
			      }
			      const player = document.querySelector('#shorts-player')
			        || document.querySelector('ytd-reel-video-renderer[is-active] .html5-video-player')
			        || document.querySelector('.html5-video-player');
			      const ccBtn = (player && player.querySelector('.ytp-subtitles-button'))
			        || document.querySelector('.ytp-subtitles-button');
			      let synced = false;
			      if (ccBtn) {
			        const isOn = ccBtn.getAttribute('aria-pressed') === 'true';
			        if (isOn !== state.subtitles) {
			          ccBtn.click();
			        }
			        synced = true;
			      } else if (player) {
			        try {
			          if (state.subtitles) {
			            if (typeof player.loadModule === 'function') player.loadModule('captions');
			            if (typeof player.getOption === 'function' && typeof player.setOption === 'function') {
			              const tracks = player.getOption('captions', 'tracklist');
			              if (Array.isArray(tracks) && tracks.length > 0) {
			                const preferred = tracks.find(tr => tr.languageCode && tr.languageCode.startsWith('en')) || tracks[0];
			                player.setOption('captions', 'track', preferred);
			              }
			            }
			          } else if (typeof player.unloadModule === 'function') {
			            player.unloadModule('captions');
			          }
			          synced = true;
			        } catch (_) {}
			      }
			      if (synced && id) {
			        state.lastSubtitlesSyncId = id + ':' + state.subtitles;
			      }
			    }

			    function dismissConsentIfAny() {
			      const btns = document.querySelectorAll('button');
			      for (const b of btns) {
			        const label = (b.textContent || b.getAttribute('aria-label') || '').trim().toLowerCase();
			        if (label === 'accept all' || label === 'reject all' || label === 'i agree') {
			          b.click();
			          return;
			        }
			      }
			    }

			    function navigateReel(direction) {
			      const now = performance.now();
			      if (now - state.lastNavMs < 200) return;
			      state.lastNavMs = now;
			      clearTimeout(state.autoScrollTimer);
			      state.autoScrollTimer = null;

			      const isNext = direction > 0;
			      const btn = document.querySelector(
			        isNext
			          ? '#navigation-button-down button, button[aria-label*="Next video" i]'
			          : '#navigation-button-up button, button[aria-label*="Previous video" i]'
			      );
			      if (btn) {
			        btn.click();
			      } else {
			        const key = isNext ? 'ArrowDown' : 'ArrowUp';
			        const keyCode = isNext ? 40 : 38;
			        const target = document.querySelector('ytd-shorts') || document.body || document;
			        const evtInit = { key, code: key, keyCode, which: keyCode, bubbles: true, cancelable: true };
			        target.dispatchEvent(new KeyboardEvent('keydown', evtInit));
			        document.dispatchEvent(new KeyboardEvent('keydown', evtInit));
			        const scroller = document.querySelector('#shorts-container')
			          || document.querySelector('#shorts-inner-container')
			          || document.scrollingElement;
			        if (scroller) {
			          scroller.dispatchEvent(new WheelEvent('wheel', { deltaY: direction * 360, bubbles: true, cancelable: true }));
			          scroller.scrollBy({ top: direction * Math.max(window.innerHeight, 480), behavior: 'smooth' });
			        }
			      }
			      setTimeout(() => pushState(true), 120);
			      setTimeout(() => pushState(true), 420);
			    }

			    function scheduleAutoScroll(videoId) {
			      if (!state.autoScroll || !state.hudMode) return;
			      if (state.autoScrolledForId === videoId && videoId) return;
			      state.autoScrolledForId = videoId || ('anon-' + performance.now());
			      clearTimeout(state.autoScrollTimer);
			      const delay = Math.max(0, state.autoScrollDelayMs);
			      state.autoScrollTimer = setTimeout(() => {
			        state.autoScrollTimer = null;
			        if (state.autoScroll && location.pathname.includes('/shorts')) {
			          navigateReel(1);
			        }
			      }, delay);
			    }

			    function getCommentsButton(reel) {
			      if (!reel) return null;
			      return reel.querySelector('#comments-button button')
			        || reel.querySelector('ytd-button-renderer#comments-button button')
			        || reel.querySelector('button[aria-label*="comment" i]')
			        || reel.querySelector('[aria-label*="comment" i] button');
			    }

			    function getCommentsPanel() {
			      return document.querySelector('ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-comments-section"]')
			        || document.querySelector('#shorts-panel-container ytd-engagement-panel-section-list-renderer')
			        || document.querySelector('#shorts-panel-container');
			    }

			    function isCommentsPanelExpanded(panel) {
			      if (!panel) return false;
			      const vis = panel.getAttribute('visibility');
			      if (vis === 'ENGAGEMENT_PANEL_VISIBILITY_EXPANDED') return true;
			      return !!panel.querySelector('ytd-comment-thread-renderer, ytd-comment-view-model, ytd-comment-renderer');
			    }

			    function openNativeComments(reel, id) {
			      const panel = getCommentsPanel();
			      if (panel && panel.getAttribute('visibility') === 'ENGAGEMENT_PANEL_VISIBILITY_EXPANDED') {
			        state.commentsOpenedForId = id || '';
			        return;
			      }
			      const btn = getCommentsButton(reel);
			      if (!btn) return;
			      state.commentsOpenedForId = id || '';
			      state.commentsClickedAtMs = performance.now();
			      try { btn.click(); } catch (_) {}
			    }

			    function closeNativeComments() {
			      const panel = getCommentsPanel();
			      if (!panel) return;
			      const vis = panel.getAttribute('visibility');
			      if (vis && vis !== 'ENGAGEMENT_PANEL_VISIBILITY_EXPANDED') return;
			      const closeBtn = panel.querySelector('#visibility-button button, button[aria-label="Close"], ytd-button-renderer#visibility-button button');
			      if (closeBtn) {
			        try { closeBtn.click(); } catch (_) {}
			      }
			    }

			    function scrapeYouTubeComments(id) {
			      const panel = getCommentsPanel();
			      const items = [];
			      let totalLabel = '';
			      let emptyConfirmed = false;

			      if (panel) {
			        const countEl = panel.querySelector('#contextual-info, ytd-engagement-panel-title-header-renderer #contextual-info');
			        if (countEl) {
			          totalLabel = (countEl.textContent || '').replace(/\\s+/g, ' ').trim().slice(0, 24);
			        }
			        const msgEl = panel.querySelector('ytd-message-renderer, #message');
			        if (msgEl) {
			          const mt = (msgEl.textContent || '').toLowerCase();
			          if (mt.includes('comments are turned off') || mt.includes('no comments')) {
			            emptyConfirmed = true;
			          }
			        }

			        let nodes = panel.querySelectorAll('ytd-comment-thread-renderer');
			        if (!nodes || nodes.length === 0) {
			          nodes = panel.querySelectorAll('ytd-comment-view-model, ytd-comment-renderer');
			        }
			        const seen = new Set();
			        for (const node of nodes) {
			          if (items.length >= 25) break;
			          const root = node.querySelector('#comment, ytd-comment-view-model, ytd-comment-renderer') || node;
			          let author = pickText(root, ['#author-text', '#header-author a', 'a[href*="/@"]']);
			          const text = pickText(root, ['#content-text', 'yt-attributed-string#content-text', '.yt-core-attributed-string']);
			          if (!text) continue;
			          if (author && !author.startsWith('@') && !author.includes(' ')) author = '@' + author;
			          const likes = pickText(root, ['#vote-count-middle', 'span#vote-count-middle']);
			          const timeAgo = pickText(root, ['#published-time-text a', '#published-time-text', 'a[href*="lc="]']);
			          const clippedText = text.length > 240 ? text.slice(0, 240) : text;
			          const dedupe = author + '|' + clippedText;
			          if (seen.has(dedupe)) continue;
			          seen.add(dedupe);
			          items.push({
			            a: author.slice(0, 48),
			            x: clippedText,
			            l: likes.slice(0, 16),
			            t: timeAgo.replace(/\\s*\\(edited\\)/i, '').trim().slice(0, 24)
			          });
			        }
			      }

			      const hasSpinner = !!(panel && panel.querySelector('tp-yt-paper-spinner, ytd-continuation-item-renderer'));
			      const waitingWindow = (performance.now() - state.commentsOpenAtMs) < 2800;
			      const loading = items.length === 0 && !emptyConfirmed && (hasSpinner || waitingWindow || !isCommentsPanelExpanded(panel));
			      return { id: id || '', open: state.commentsOpen, loading, sr: false, cnt: totalLabel, items };
			    }

			    function pushComments(force) {
			      if (!state.commentsOpen) return;
			      const now = performance.now();
			      if (!force && now - state.lastCommentsPushMs < 850) return;
			      const onShorts = location.pathname.includes('/shorts');
			      const id = getShortsId();
			      if (state.commentsRetryId !== id) {
			        state.commentsRetryId = id;
			        state.commentsRetries = 0;
			      }
			      if (onShorts && id && state.commentsOpenedForId === id && state.commentsRetries < 2
			          && now - state.commentsClickedAtMs > 2500 && !isCommentsPanelExpanded(getCommentsPanel())) {
			        state.commentsRetries++;
			        state.commentsOpenedForId = '';
			      }
			      if (onShorts && id && state.commentsOpenedForId !== id) {
			        openNativeComments(getActiveReel(), id);
			      }
			      const payload = scrapeYouTubeComments(id);
			      const key = payload.id + '|' + payload.loading + '|' + payload.cnt + '|' + payload.items.length + '|' + (payload.items[0] ? payload.items[0].x : '');
			      if (!force && key === state.lastCommentsKey) return;
			      state.lastCommentsKey = key;
			      state.lastCommentsPushMs = now;
			      try {
			        console.info('__DS_CMT__:' + JSON.stringify(payload));
			      } catch (_) {}
			    }

			    function pushState(force) {
			      const reel = getActiveReel();
			      const video = getActiveVideo();
			      applyAudio(video);

			      const id = getShortsId();
			      const t = video && Number.isFinite(video.currentTime) ? video.currentTime : 0;
			      const d = video && Number.isFinite(video.duration) ? video.duration : 0;
			      const r = video && Number.isFinite(video.playbackRate) ? video.playbackRate : 1;
			      const p = video ? !!video.paused : true;
			      const b = video ? (video.readyState < 3 && !video.paused) : false;
			      const title = getTitle(reel);
			      const channel = getChannel(reel);
			      const liked = isLiked(reel);
			      const signed = isSignedIn();
			      const onShorts = location.pathname.includes('/shorts');
			      const videoReady = !!(video && video.readyState >= 2 && video.videoWidth > 0 && Number.isFinite(d) && d > 0.1);
			      const botGate = !signed && !videoReady && onShorts && !!document.querySelector('ytd-enforcement-message-view-model, .yt-playability-error-supported-renderers');

			      updateNativeCaption(title, channel, onShorts, videoReady);

			      if (id !== state.lastVideoId) {
			        state.lastVideoId = id;
			        state.lastTime = t;
			        state.autoScrolledForId = '';
			        clearTimeout(state.autoScrollTimer);
			        state.autoScrollTimer = null;
			        try {
			          const allVideos = document.querySelectorAll('video');
			          for (const v of allVideos) {
			            if (v !== video && !v.paused) v.pause();
			          }
			        } catch (_) {}
			        applySubtitles(false);
			        if (state.commentsOpen && onShorts) {
			          state.commentsOpenedForId = '';
			          state.lastCommentsKey = '';
			          state.commentsOpenAtMs = performance.now();
			          try {
			            console.info('__DS_CMT__:' + JSON.stringify({ id, open: true, loading: true, sr: false, cnt: '', items: [] }));
			          } catch (_) {}
			          setTimeout(() => {
			            if (!state.commentsOpen) return;
			            openNativeComments(getActiveReel(), getShortsId());
			            pushComments(true);
			          }, 350);
			          setTimeout(() => pushComments(true), 1200);
			        }
			        force = true;
			      } else if (onShorts && state.subtitles && state.lastSubtitlesSyncId !== (id + ':true') && videoReady) {
			        applySubtitles(false);
			      } else if (onShorts && state.autoScroll && !p && d > 1.5) {
			        const wrapped = (state.lastTime >= d - 0.45 && t < 0.75);
			        const ended = video && video.ended;
			        const nearEndNoLoop = video && !video.loop && (t >= d - 0.12);
			        if (wrapped || ended || nearEndNoLoop) {
			          scheduleAutoScroll(id);
			        }
			        state.lastTime = t;
			      } else {
			        state.lastTime = t;
			      }

			      if (state.commentsOpen && onShorts) {
			        pushComments(false);
			      }

			      const now = performance.now();
			      const key = id + '|' + title + '|' + channel + '|' + p + '|' + state.muted + '|' + liked + '|' + signed + '|' + onShorts + '|' + b + '|' + videoReady + '|' + botGate + '|' + Math.round(d * 10);
			      const elapsed = now - state.lastPushMs;

			      if (!force && key === state.lastKey) {
			        if (p || elapsed < 400) return;
			      }

			      state.lastKey = key;
			      state.lastPushMs = now;
			      try {
			        console.info('__DS__:' + JSON.stringify({
			          id,
			          title,
			          channel,
			          t: Math.round(t * 100) / 100,
			          d: Math.round(d * 100) / 100,
			          r: Math.round(r * 100) / 100,
			          p,
			          m: state.muted,
			          l: liked,
			          s: signed,
			          sh: onShorts,
			          b,
			          vr: videoReady,
			          bg: botGate,
			          url: location.href
			        }));
			      } catch (_) {}
			    }

			    window.__doomscrolling = {
			      configure(hudMode, showCaption, autoScroll, delayMs, volume, muted, subtitles) {
			        state.hudMode = !!hudMode;
			        state.showCaption = !!showCaption;
			        state.autoScroll = !!autoScroll;
			        state.autoScrollDelayMs = Math.max(0, delayMs | 0);
			        state.volume = Math.max(0, Math.min(1, Number(volume) || 0));
			        state.muted = !!muted;
			        state.subtitles = !!subtitles;
			        document.documentElement.dataset.dsHud = state.hudMode ? '1' : '0';
			        document.documentElement.dataset.dsComments = state.commentsOpen ? '1' : '0';
			        applyAudio(getActiveVideo());
			        applySubtitles(true);
			        pushState(true);
			      },
			      next() { navigateReel(1); },
			      prev() { navigateReel(-1); },
			      togglePlayPause() {
			        const v = getActiveVideo();
			        if (!v) return;
			        if (v.paused) {
			          v.play().catch(() => {});
			        } else {
			          v.pause();
			        }
			        setTimeout(() => pushState(true), 40);
			      },
			      setPaused(shouldPause) {
			        const v = getActiveVideo();
			        if (!v) return;
			        if (shouldPause) {
			          if (!v.paused) v.pause();
			        } else {
			          if (v.paused) v.play().catch(() => {});
			        }
			        setTimeout(() => pushState(true), 40);
			      },
			      toggleLike() {
			        if (!isSignedIn()) {
			          console.info('__DS_EVT__:signin_required');
			          return;
			        }
			        const reel = getActiveReel();
			        const btn = getLikeButton(reel);
			        if (btn) {
			          btn.click();
			          setTimeout(() => {
			            const nowLiked = isLiked(reel);
			            console.info('__DS_EVT__:' + (nowLiked ? 'liked' : 'unliked'));
			            pushState(true);
			          }, 120);
			        }
			      },
			      seekRatio(ratio) {
			        const v = getActiveVideo();
			        if (!v || !Number.isFinite(v.duration) || v.duration <= 0) return;
			        v.currentTime = Math.max(0, Math.min(v.duration - 0.05, ratio * v.duration));
			        state.lastTime = v.currentTime;
			        pushState(true);
			      },
			      setCommentsOpen(open) {
			        state.commentsOpen = !!open;
			        document.documentElement.dataset.dsComments = state.commentsOpen ? '1' : '0';
			        state.lastCommentsKey = '';
			        const id = getShortsId();
			        if (state.commentsOpen) {
			          state.commentsOpenAtMs = performance.now();
			          openNativeComments(getActiveReel(), id);
			          try {
			            console.info('__DS_CMT__:' + JSON.stringify({ id, open: true, loading: true, sr: false, cnt: '', items: [] }));
			          } catch (_) {}
			          setTimeout(() => pushComments(true), 450);
			          setTimeout(() => pushComments(true), 1200);
			          setTimeout(() => pushComments(true), 2400);
			        } else {
			          state.commentsOpenedForId = '';
			          closeNativeComments();
			          try {
			            console.info('__DS_CMT__:' + JSON.stringify({ id, open: false, loading: false, sr: false, cnt: '', items: [] }));
			          } catch (_) {}
			        }
			      },
			      isCommentsOpen() {
			        return !!state.commentsOpen;
			      }
			    };

			    setInterval(() => {
			      if (document.hidden) return;
			      pushState(false);
			    }, 220);

			    setTimeout(dismissConsentIfAny, 1200);
			  }

			  document.documentElement.dataset.dsHud = '__DS_HUD_FLAG__';
			  window.__doomscrolling.configure(
			    document.documentElement.dataset.dsHud === '1',
			    __DS_SHOW_CAPTION__,
			    __DS_AUTO_SCROLL__,
			    __DS_AUTO_DELAY__,
			    __DS_VOLUME__,
			    __DS_MUTED__,
			    __DS_SUBTITLES__
			  );
			})();
			""";
}
