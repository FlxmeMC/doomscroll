package dev.doomscrolling.browser;

public final class InstagramReelsBridge {

	private InstagramReelsBridge() {
	}

	static final String INSTAGRAM_BRANCH_SCRIPT = """
			  document.documentElement.dataset.dsPlatform = 'instagram';

			  const IG_STYLE_ID = 'doomscrolling-ig-clean-css';
			  if (!document.getElementById(IG_STYLE_ID)) {
			    const style = document.createElement('style');
			    style.id = IG_STYLE_ID;
			    style.textContent = `
			      html[data-ds-hud="1"][data-ds-platform="instagram"],
			      html[data-ds-hud="1"][data-ds-platform="instagram"] body {
			        background: #0f0f0f !important;
			        overflow: hidden !important;
			        margin: 0 !important;
			        padding: 0 !important;
			        scrollbar-width: none !important;
			      }
			      html[data-ds-hud="1"][data-ds-platform="instagram"] ::-webkit-scrollbar {
			        display: none !important;
			      }
			      html[data-ds-hud="1"][data-ds-platform="instagram"] nav,
			      html[data-ds-hud="1"][data-ds-platform="instagram"] header,
			      html[data-ds-hud="1"][data-ds-platform="instagram"] [role="navigation"],
			      html[data-ds-hud="1"][data-ds-platform="instagram"] [role="banner"],
			      html[data-ds-hud="1"][data-ds-platform="instagram"] [role="tablist"] {
			        display: none !important;
			        visibility: hidden !important;
			        opacity: 0 !important;
			        pointer-events: none !important;
			      }
			      html[data-ds-hud="1"][data-ds-platform="instagram"] main {
			        margin: 0 !important;
			        padding: 0 !important;
			        width: 100vw !important;
			        max-width: 100vw !important;
			        left: 0 !important;
			        background: #0f0f0f !important;
			      }
			      html[data-ds-hud="1"][data-ds-platform="instagram"] [data-ds-ig-ancestor="1"] {
			        z-index: auto !important;
			        filter: none !important;
			        backdrop-filter: none !important;
			        contain: none !important;
			        will-change: auto !important;
			      }
			      html[data-ds-hud="1"][data-ds-platform="instagram"] [data-ds-ig-zero-tx="1"] {
			        transform: none !important;
			      }
			      html[data-ds-hud="1"][data-ds-platform="instagram"] video[data-ds-ig-active="1"] {
			        position: fixed !important;
			        inset: 0 !important;
			        top: 0 !important;
			        left: 0 !important;
			        width: 100vw !important;
			        height: 100vh !important;
			        max-width: 100vw !important;
			        max-height: 100vh !important;
			        object-fit: cover !important;
			        border-radius: 0 !important;
			        background: #0f0f0f !important;
			        z-index: 2147483640 !important;
			        pointer-events: none !important;
			      }
			      html[data-ds-hud="1"][data-ds-platform="instagram"] video:not([data-ds-ig-active="1"]) {
			        visibility: hidden !important;
			      }
			      html[data-ds-hud="1"][data-ds-platform="instagram"] * {
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
			        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", "Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", sans-serif;
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
			      html[data-ds-hud="1"][data-ds-platform="instagram"][data-ds-comments="1"] [role="dialog"] {
			        z-index: 1 !important;
			        opacity: 0 !important;
			        pointer-events: none !important;
			      }
			      html[data-ds-hud="1"][data-ds-platform="instagram"][data-ds-cc="0"] video::cue {
			        opacity: 0 !important;
			        color: transparent !important;
			        background: transparent !important;
			      }
			      html[data-ds-hud="1"][data-ds-platform="instagram"][data-ds-cc="1"] video::cue {
			        font-family: "Roboto", "Segoe UI", sans-serif !important;
			        font-size: clamp(16px, 5.2vw, 28px) !important;
			        line-height: 1.28 !important;
			        font-weight: 700 !important;
			        background: rgba(12, 12, 12, 0.80) !important;
			        color: #ffffff !important;
			        text-shadow: 0 1px 3px rgba(0, 0, 0, 0.92) !important;
			      }
			    `;
			    (document.head || document.documentElement).appendChild(style);
			  }

			  function ensureIgCaptionOverlay() {
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
			    const RESERVED_PATHS = new Set([
			      'reels', 'reel', 'p', 'explore', 'direct', 'accounts', 'stories',
			      'audio', 'tags', 'locations', 'about', 'legal', 'privacy', 'terms',
			      'api', 'developer', 'session', 'challenge', 'two_factor', 'consent'
			    ]);
			    const CAPTION_NOISE = new Set([
			      'follow', 'following', 'requested', 'more', '... more', '… more',
			      'original audio', 'audio', 'audio is muted', 'audio is playing',
			      'like', 'unlike', 'comment', 'share', 'save', 'saved', 'send',
			      'verified', 'see translation', 'sponsored', 'log in', 'sign up',
			      'open app', 'use the app', 'not now'
			    ]);
			    const COMMENT_NOISE = new Set([
			      'comments', 'reply', 'see translation', 'hide replies', 'view replies',
			      'like', 'unlike', 'more', 'post', 'verified', 'author', 'pinned',
			      'add a comment...', 'log in to like or comment.', 'no comments yet.',
			      'start the conversation.', 'edited', 'meta verified'
			    ]);

			    let nextVidSeq = 1;
			    const state = {
			      hudMode: true,
			      showCaption: true,
			      autoScroll: true,
			      autoScrollDelayMs: 250,
			      volume: 0.65,
			      muted: false,
			      subtitles: false,
			      userPaused: false,
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
			      lastNavMs: 0,
			      lastUnmuteClickMs: 0,
			      lastPopupCheckMs: 0,
			      markedVideo: null
			    };

			    function isAuthRoute(pathname) {
			      const p = pathname || '';
			      return /^\\/(accounts\\/(login|emailsignup|password)|challenge|two_factor|consent)/i.test(p);
			    }

			    function isOnReelsRoute(pathname) {
			      const p = (pathname || '').toLowerCase();
			      if (p.includes('/reels/audio/')) return false;
			      return /^\\/reels?(\\/|$)/i.test(p);
			    }

			    function scrollingAncestor(el) {
			      for (let cur = el ? el.parentElement : null; cur && cur !== document.documentElement; cur = cur.parentElement) {
			        try {
			          const cs = getComputedStyle(cur);
			          if ((cs.overflowY === 'auto' || cs.overflowY === 'scroll') && cur.scrollHeight > cur.clientHeight + 20) {
			            return cur;
			          }
			        } catch (_) {}
			      }
			      return null;
			    }

			    function getDirectFeedChild(el, feed) {
			      if (!el || !feed) return null;
			      let cur = el;
			      while (cur && cur.parentElement && cur.parentElement !== feed) {
			        cur = cur.parentElement;
			      }
			      return cur && cur.parentElement === feed ? cur : null;
			    }

			    function getMeasureElement(video) {
			      if (!video) return null;
			      if (video.dataset.dsIgActive !== '1') {
			        const r = video.getBoundingClientRect();
			        if (r.width >= 40 && r.height >= 40) return video;
			      }
			      const feed = scrollingAncestor(video);
			      const parent = video.parentElement;
			      if (parent && parent !== feed && parent !== document.body) {
			        const pr = parent.getBoundingClientRect();
			        if (pr.width >= 40 && pr.height >= 80) return parent;
			      }
			      const directChild = getDirectFeedChild(video, feed);
			      if (directChild) {
			        const dr = directChild.getBoundingClientRect();
			        if (dr.width >= 40 && dr.height >= 80) return directChild;
			      }
			      for (let cur = parent; cur && cur !== document.body; cur = cur.parentElement) {
			        const cr = cur.getBoundingClientRect();
			        if (cr.width >= 40 && cr.height >= 80) return cur;
			      }
			      return video;
			    }

			    function getActiveVideo() {
			      const all = Array.from(document.querySelectorAll('video'));
			      if (all.length === 0) return null;
			      const nonDialog = all.filter(v => !v.closest('[role="dialog"]'));
			      const candidates = nonDialog.length > 0 ? nonDialog : all;
			      const vw = window.innerWidth || 360;
			      const vh = window.innerHeight || 640;
			      const midY = vh * 0.5;

			      let best = null;
			      let bestShare = 0;
			      let bestArea = 0;
			      let bestCenterDist = Infinity;

			      for (const v of candidates) {
			        const measureEl = getMeasureElement(v);
			        if (!measureEl) continue;
			        const rect = measureEl.getBoundingClientRect();
			        if (rect.width < 30 || rect.height < 30) continue;
			        const visW = Math.min(rect.right, vw) - Math.max(rect.left, 0);
			        const visH = Math.min(rect.bottom, vh) - Math.max(rect.top, 0);
			        if (visW <= 0 || visH <= 0) continue;
			        const area = visW * visH;
			        const share = area / Math.max(1, rect.width * rect.height);
			        const centerDist = Math.abs((rect.top + rect.bottom) * 0.5 - midY);

			        if (
			          share > bestShare + 0.02
			          || (Math.abs(share - bestShare) <= 0.02 && centerDist < bestCenterDist - 8)
			          || (Math.abs(share - bestShare) <= 0.02 && Math.abs(centerDist - bestCenterDist) <= 8 && area > bestArea)
			        ) {
			          best = v;
			          bestShare = share;
			          bestArea = area;
			          bestCenterDist = centerDist;
			        }
			      }
			      if (best) return best;
			      for (const v of candidates) {
			        if (v.readyState > 0 && !v.paused) return v;
			      }
			      return candidates[0];
			    }

			    function getReelContainer(video) {
			      if (!video) return null;
			      const feed = scrollingAncestor(video);
			      const direct = getDirectFeedChild(video, feed);
			      if (direct) return direct;
			      return video.closest('article') || video.closest('section') || video.closest('main') || video.parentElement;
			    }

			    function isZeroTranslationTransform(transform) {
			      if (!transform || transform === 'none') return false;
			      const m2 = transform.match(/^matrix\\(([^)]+)\\)$/);
			      if (m2) {
			        const parts = m2[1].split(',').map(Number);
			        return parts.length === 6 && Math.abs(parts[4]) < 0.5 && Math.abs(parts[5]) < 0.5;
			      }
			      const m3 = transform.match(/^matrix3d\\(([^)]+)\\)$/);
			      if (m3) {
			        const parts = m3[1].split(',').map(Number);
			        return parts.length === 16 && Math.abs(parts[12]) < 0.5 && Math.abs(parts[13]) < 0.5;
			      }
			      return false;
			    }

			    function clearActiveVideoMarks() {
			      state.markedVideo = null;
			      document.querySelectorAll('[data-ds-ig-active="1"]').forEach(el => delete el.dataset.dsIgActive);
			      document.querySelectorAll('[data-ds-ig-ancestor="1"]').forEach(el => delete el.dataset.dsIgAncestor);
			      document.querySelectorAll('[data-ds-ig-zero-tx="1"]').forEach(el => delete el.dataset.dsIgZeroTx);
			    }

			    function syncActiveVideoVisuals(video, onReels) {
			      if (!state.hudMode || !onReels || !video) {
			        if (state.markedVideo || document.querySelector('[data-ds-ig-active="1"]')) {
			          clearActiveVideoMarks();
			        }
			        return;
			      }
			      if (state.markedVideo === video && video.dataset.dsIgActive === '1' && video.isConnected) {
			        return;
			      }
			      clearActiveVideoMarks();
			      video.dataset.dsIgActive = '1';
			      state.markedVideo = video;
			      for (let cur = video.parentElement; cur && cur !== document.body && cur !== document.documentElement; cur = cur.parentElement) {
			        cur.dataset.dsIgAncestor = '1';
			        try {
			          const tx = getComputedStyle(cur).transform;
			          if (isZeroTranslationTransform(tx)) {
			            cur.dataset.dsIgZeroTx = '1';
			          }
			        } catch (_) {}
			      }
			    }

			    function getReelId(reel, video) {
			      const pathMatch = (location.pathname || '').match(/\\/reels?\\/(?:videos\\/)?([a-zA-Z0-9_-]+)/);
			      if (pathMatch && pathMatch[1] && pathMatch[1].toLowerCase() !== 'audio') {
			        return pathMatch[1];
			      }
			      if (reel) {
			        const links = reel.querySelectorAll('a[href*="/reel/"], a[href*="/reels/"], a[href*="/p/"]');
			        for (const a of links) {
			          const href = a.getAttribute('href') || '';
			          const m = href.match(/\\/(?:reels?|p)\\/([a-zA-Z0-9_-]+)/);
			          if (m && m[1] && m[1].toLowerCase() !== 'audio') return m[1];
			        }
			      }
			      if (video) {
			        if (!video.dataset.dsVidSeq) {
			          video.dataset.dsVidSeq = 'ig-' + (nextVidSeq++);
			        }
			        return video.dataset.dsVidSeq;
			      }
			      return '';
			    }

			    function getChannel(reel) {
			      if (!reel) return '';
			      const links = reel.querySelectorAll('a[href^="/"]');
			      for (const a of links) {
			        if (a.closest('[role="dialog"], nav, header')) continue;
			        const href = a.getAttribute('href') || '';
			        const m = href.match(/^\\/([a-zA-Z0-9._]{1,35})\\/?(?:\\?.*)?$/);
			        if (!m) continue;
			        const slug = m[1];
			        if (RESERVED_PATHS.has(slug.toLowerCase())) continue;
			        const text = (a.textContent || '').replace(/\\s+/g, ' ').trim();
			        const handle = (text && !text.includes(' ') && text.length <= 36) ? text : slug;
			        return handle.startsWith('@') ? handle : ('@' + handle);
			      }
			      const avatar = reel.querySelector('img[alt*="profile picture" i]');
			      if (avatar) {
			        const alt = (avatar.getAttribute('alt') || '').trim();
			        const m = alt.match(/^([a-zA-Z0-9._]+)'s profile picture/i);
			        if (m) return '@' + m[1];
			      }
			      return '';
			    }

			    function getTitle(reel, channel) {
			      const cleanChannel = (channel || '').replace(/^@/, '').toLowerCase();
			      if (reel) {
			        const nodes = reel.querySelectorAll('h1, span[dir="auto"], div[dir="auto"]');
			        let best = '';
			        for (const el of nodes) {
			          if (el.closest('[role="dialog"], nav, header')) continue;
			          if (el.querySelector('video')) continue;
			          if (el.children.length > 8) continue;
			          let txt = (el.textContent || '').replace(/\\s+/g, ' ').trim();
			          txt = txt.replace(/(?:\\.\\.\\.|…)\\s*more$/i, '').trim();
			          if (!txt || txt.length < 2) continue;
			          const lower = txt.toLowerCase();
			          if (lower === cleanChannel || lower === ('@' + cleanChannel)) continue;
			          if (CAPTION_NOISE.has(lower)) continue;
			          if (/^[\\d.,]+\\s*[kmb]?$/i.test(txt)) continue;
			          if (/^original audio\\b/i.test(txt)) continue;
			          const inBtn = el.closest('button, [role="button"]');
			          if (inBtn && txt.length < 12) continue;
			          if (txt.length > best.length) {
			            best = txt;
			          }
			        }
			        if (best) {
			          return best.length > 140 ? best.slice(0, 140) : best;
			        }
			      }
			      let docTitle = (document.title || '')
			        .replace(/\\s*[•·|-]\\s*Instagram.*$/i, '')
			        .replace(/^Instagram$/i, '')
			        .trim();
			      if (docTitle.length > 140) docTitle = docTitle.slice(0, 140);
			      return docTitle;
			    }

			    function getLikeControl(reel) {
			      const root = reel || document;
			      const candidates = root.querySelectorAll('svg[aria-label], button[aria-label], [role="button"][aria-label]');
			      let best = null;
			      let bestScore = -1;
			      for (const el of candidates) {
			        if (el.closest('[role="dialog"]')) continue;
			        const label = (el.getAttribute('aria-label') || '').trim().toLowerCase();
			        const isUnlike = label === 'unlike';
			        const isLike = label === 'like';
			        if (!isLike && !isUnlike) continue;
			        const btn = el.closest('button, [role="button"]') || el;
			        const rect = el.getBoundingClientRect();
			        const size = Math.max(rect.width, rect.height);
			        const score = (size >= 18 ? 10 : 1) + (el.tagName.toLowerCase() === 'svg' ? 2 : 0);
			        if (score > bestScore) {
			          bestScore = score;
			          best = { btn, liked: isUnlike };
			        }
			      }
			      return best;
			    }

			    function isLiked(reel) {
			      const ctrl = getLikeControl(reel);
			      return !!(ctrl && ctrl.liked);
			    }

			    function getCommentsButton(reel) {
			      const root = reel || document;
			      const candidates = root.querySelectorAll('svg[aria-label], button[aria-label], [role="button"][aria-label]');
			      for (const el of candidates) {
			        if (el.closest('[role="dialog"]')) continue;
			        const label = (el.getAttribute('aria-label') || '').trim().toLowerCase();
			        if (label === 'comment' || label === 'comments' || label.includes('comment')) {
			          return el.closest('button, [role="button"]') || el;
			        }
			      }
			      return null;
			    }

			    function getCommentsDialog() {
			      const dialogs = document.querySelectorAll('[role="dialog"]');
			      for (const d of dialogs) {
			        if (d.querySelector('input[name="password"], form#loginForm')) continue;
			        if (d.querySelector('textarea[placeholder*="comment" i], input[placeholder*="comment" i], time')) {
			          return d;
			        }
			        const txt = (d.textContent || '').toLowerCase();
			        if (txt.includes('comments') || txt.includes('no comments yet')) {
			          return d;
			        }
			      }
			      const input = document.querySelector('textarea[placeholder*="comment" i], input[placeholder*="comment" i]');
			      if (input) {
			        return input.closest('[role="dialog"], section, aside') || input.parentElement;
			      }
			      return null;
			    }

			    function openNativeComments(reel, id) {
			      if (getCommentsDialog()) {
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
			      const dialog = getCommentsDialog();
			      if (dialog) {
			        const closeEl = dialog.querySelector('svg[aria-label="Close"], button[aria-label="Close"], [role="button"][aria-label="Close"]');
			        if (closeEl) {
			          const btn = closeEl.closest('button, [role="button"]') || closeEl;
			          try { btn.click(); return; } catch (_) {}
			        }
			        try {
			          document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape', keyCode: 27, which: 27, bubbles: true }));
			        } catch (_) {}
			      }
			    }

			    function extractAuthorFromContainer(container) {
			      const links = container.querySelectorAll('a[href^="/"]');
			      for (const a of links) {
			        const href = a.getAttribute('href') || '';
			        const m = href.match(/^\\/([a-zA-Z0-9._]{1,35})\\/?(?:\\?.*)?$/);
			        if (!m) continue;
			        const slug = m[1];
			        if (RESERVED_PATHS.has(slug.toLowerCase())) continue;
			        const txt = (a.textContent || '').replace(/\\s+/g, ' ').trim();
			        const handle = (txt && !txt.includes(' ') && txt.length <= 36) ? txt : slug;
			        return handle.startsWith('@') ? handle : ('@' + handle);
			      }
			      return '';
			    }

			    function scrapeInstagramComments(id, signed) {
			      const dialog = getCommentsDialog();
			      const items = [];
			      let signInRequired = false;
			      let emptyConfirmed = false;

			      if (dialog) {
			        const fullLower = (dialog.textContent || '').toLowerCase();
			        if (fullLower.includes('no comments yet') || fullLower.includes('be the first to comment')) {
			          emptyConfirmed = true;
			        }
			        if (!signed && dialog.querySelector('a[href*="/accounts/login"], input[name="password"]')) {
			          signInRequired = true;
			        }

			        const seenContainers = new Set();
			        const seenKeys = new Set();
			        const anchors = Array.from(dialog.querySelectorAll('time'));
			        for (const timeEl of anchors) {
			          if (items.length >= 25) break;
			          let container = null;
			          let cur = timeEl.parentElement;
			          for (let depth = 0; cur && cur !== dialog && depth < 6; depth++, cur = cur.parentElement) {
			            const timeCount = cur.querySelectorAll('time').length;
			            if (timeCount > 1) break;
			            const author = extractAuthorFromContainer(cur);
			            if (author && cur.querySelectorAll('span[dir="auto"], span').length >= 2) {
			              container = cur;
			              break;
			            }
			          }
			          if (!container || seenContainers.has(container)) continue;
			          seenContainers.add(container);

			          const author = extractAuthorFromContainer(container);
			          if (!author) continue;
			          const cleanAuthor = author.replace(/^@/, '').toLowerCase();
			          const timeAgo = (timeEl.textContent || '').replace(/\\s+/g, ' ').trim().slice(0, 20);
			          let likes = '';
			          let bestText = '';

			          const spans = container.querySelectorAll('span[dir="auto"], span, div[dir="auto"]');
			          for (const sp of spans) {
			            if (sp.querySelector('time, svg, textarea, input')) continue;
			            if (sp.children.length > 4) continue;
			            const txt = (sp.textContent || '').replace(/\\s+/g, ' ').trim();
			            if (!txt) continue;
			            const lower = txt.toLowerCase();
			            if (lower === cleanAuthor || lower === author.toLowerCase() || lower === timeAgo.toLowerCase()) continue;
			            const likeMatch = txt.match(/^([\\d.,]+\\s*[kmb]?)\\s+likes?$/i);
			            if (likeMatch) {
			              if (!likes) likes = likeMatch[1].replace(/\\s+/g, '');
			              continue;
			            }
			            if (COMMENT_NOISE.has(lower)) continue;
			            if (/^view\\s+(all\\s+)?\\d+\\s+repl/i.test(txt)) continue;
			            if (/^view\\s+replies/i.test(txt)) continue;
			            if (/^liked\\s+by\\b/i.test(txt)) continue;
			            if (sp.closest('a[href^="/"]') && txt.length <= 36 && !txt.includes(' ')) continue;
			            if (txt.length > bestText.length) {
			              bestText = txt;
			            }
			          }

			          if (!bestText) continue;
			          if (bestText.length > 240) bestText = bestText.slice(0, 240);
			          const dedupeKey = author.toLowerCase() + '|' + bestText;
			          if (seenKeys.has(dedupeKey)) continue;
			          seenKeys.add(dedupeKey);
			          items.push({ a: author, x: bestText, l: likes, t: timeAgo });
			        }
			      } else if (!signed && isLoginGate(signed, false)) {
			        signInRequired = true;
			      }

			      const waitingWindow = (performance.now() - state.commentsOpenAtMs) < 2800;
			      const dialogBusy = !dialog || !!dialog.querySelector('[role="progressbar"], svg[aria-label*="Loading" i]');
			      const loading = items.length === 0 && !emptyConfirmed && !signInRequired && (waitingWindow || dialogBusy);
			      return { id: id || '', open: state.commentsOpen, loading, sr: signInRequired, cnt: '', items };
			    }

			    function pushComments(force) {
			      if (!state.commentsOpen) return;
			      const now = performance.now();
			      if (!force && now - state.lastCommentsPushMs < 850) return;
			      const onReels = isOnReelsRoute(location.pathname);
			      const video = onReels ? getActiveVideo() : null;
			      const reel = video ? getReelContainer(video) : null;
			      const id = onReels ? getReelId(reel, video) : '';
			      const signed = isSignedIn();
			      if (state.commentsRetryId !== id) {
			        state.commentsRetryId = id;
			        state.commentsRetries = 0;
			      }
			      if (onReels && id && state.commentsOpenedForId === id && state.commentsRetries < 2
			          && now - state.commentsClickedAtMs > 2500 && !getCommentsDialog()) {
			        state.commentsRetries++;
			        state.commentsOpenedForId = '';
			      }
			      if (onReels && id && state.commentsOpenedForId !== id) {
			        openNativeComments(reel, id);
			      }
			      const payload = scrapeInstagramComments(id, signed);
			      const key = payload.id + '|' + payload.loading + '|' + payload.sr + '|' + payload.items.length + '|' + (payload.items[0] ? payload.items[0].x : '');
			      if (!force && key === state.lastCommentsKey) return;
			      state.lastCommentsKey = key;
			      state.lastCommentsPushMs = now;
			      try {
			        console.info('__DS_CMT__:' + JSON.stringify(payload));
			      } catch (_) {}
			    }

			    function isSignedIn() {
			      const path = location.pathname || '';
			      if (isAuthRoute(path)) return false;
			      if (path.startsWith('/accounts/onetap')) return true;
			      if (document.querySelector('form#loginForm, input[name="password"], input[name="username"]')) {
			        return false;
			      }
			      if (document.querySelector(
			        'a[href="/direct/inbox/"], a[href*="/direct/inbox"], '
			        + 'a[href="/accounts/edit/"], a[href*="/accounts/activity"], '
			        + 'svg[aria-label="Direct"], svg[aria-label="Messenger"], svg[aria-label="Messages"], '
			        + 'svg[aria-label="Notifications"], svg[aria-label="New post"], svg[aria-label="Create"], '
			        + 'svg[aria-label="Settings"]'
			      )) {
			        return true;
			      }
			      try {
			        if (/(?:^|;\\s*)ds_user_id=\\d+/.test(document.cookie || '')) return true;
			      } catch (_) {}
			      return false;
			    }

			    function isLoginGate(signed, videoReady) {
			      if (signed || videoReady) return false;
			      if (isAuthRoute(location.pathname)) return true;
			      if (document.querySelector('form#loginForm, input[name="password"], input[name="username"]')) return true;
			      const dialogs = document.querySelectorAll('[role="dialog"]');
			      for (const d of dialogs) {
			        if (d.querySelector('input[name="password"], input[name="username"], a[href*="/accounts/login"]')) {
			          return true;
			        }
			      }
			      return false;
			    }

			    function updateNativeCaption(title, channel, onReels) {
			      const box = ensureIgCaptionOverlay();
			      if (!box) return;
			      const shouldShow = state.hudMode && state.showCaption && onReels && (title || channel);
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

			    function applyAudio(video, reel, onReels) {
			      if (!video) return;
			      try {
			        if (Math.abs(video.volume - state.volume) > 0.01) video.volume = state.volume;
			        if (video.muted !== state.muted) video.muted = state.muted;
			      } catch (_) {}

			      if (!state.muted && onReels && state.hudMode) {
			        const now = performance.now();
			        if (now - state.lastUnmuteClickMs > 1500) {
			          const root = reel || document;
			          const mutedIcon = root.querySelector(
			            'svg[aria-label="Audio is muted"], button[aria-label*="Audio is muted" i], [role="button"][aria-label*="Audio is muted" i], button[aria-label*="Unmute" i]'
			          );
			          if (mutedIcon && !mutedIcon.closest('[role="dialog"]')) {
			            const btn = mutedIcon.closest('button, [role="button"]') || mutedIcon;
			            state.lastUnmuteClickMs = now;
			            try { btn.click(); } catch (_) {}
			          }
			        }
			      }
			    }

			    function applySubtitles(video, reel, id, force) {
			      document.documentElement.dataset.dsCc = state.subtitles ? '1' : '0';
			      if (!force && state.lastSubtitlesSyncId === (id + ':' + state.subtitles)) {
			        return;
			      }
			      let synced = false;
			      if (video && video.textTracks && video.textTracks.length > 0) {
			        try {
			          for (let i = 0; i < video.textTracks.length; i++) {
			            video.textTracks[i].mode = state.subtitles ? (i === 0 ? 'showing' : 'hidden') : 'hidden';
			          }
			          synced = true;
			        } catch (_) {}
			      }
			      const root = reel || document;
			      const ccEl = root.querySelector('button[aria-label*="Caption" i], [role="button"][aria-label*="Caption" i], svg[aria-label*="Caption" i]');
			      if (ccEl && !ccEl.closest('[role="dialog"]')) {
			        const ccBtn = ccEl.closest('button, [role="button"]') || ccEl;
			        const pressed = ccBtn.getAttribute('aria-pressed');
			        if (pressed !== null) {
			          const isOn = pressed === 'true';
			          if (isOn !== state.subtitles) {
			            try { ccBtn.click(); } catch (_) {}
			          }
			          synced = true;
			        }
			      }
			      if (synced && id) {
			        state.lastSubtitlesSyncId = id + ':' + state.subtitles;
			      }
			    }

			    function dismissPopupsIfAny() {
			      const now = performance.now();
			      if (now - state.lastPopupCheckMs < 2000) return;
			      state.lastPopupCheckMs = now;

			      const btns = document.querySelectorAll('button, [role="button"]');
			      for (const b of btns) {
			        const label = (b.textContent || b.getAttribute('aria-label') || '').replace(/\\s+/g, ' ').trim().toLowerCase();
			        if (
			          label === 'allow all cookies'
			          || label === 'decline optional cookies'
			          || label === 'allow essential and optional cookies'
			          || label === 'accept all'
			        ) {
			          try { b.click(); } catch (_) {}
			          return;
			        }
			      }

			      if (!state.hudMode || state.commentsOpen) return;
			      const dialogs = document.querySelectorAll('[role="dialog"]');
			      for (const d of dialogs) {
			        if (d.querySelector('input[name="password"], input[name="username"], form#loginForm')) continue;
			        const dBtns = d.querySelectorAll('button, [role="button"]');
			        for (const b of dBtns) {
			          const label = (b.textContent || b.getAttribute('aria-label') || '').replace(/\\s+/g, ' ').trim().toLowerCase();
			          if (label === 'not now' || label === 'continue on web' || label === 'close') {
			            try { b.click(); } catch (_) {}
			            return;
			          }
			        }
			      }
			    }

			    function navigateReel(direction) {
			      const now = performance.now();
			      if (now - state.lastNavMs < 220) return;
			      state.lastNavMs = now;
			      clearTimeout(state.autoScrollTimer);
			      state.autoScrollTimer = null;

			      if (state.commentsOpen) {
			        closeNativeComments();
			      }

			      const isNext = direction > 0;
			      const video = getActiveVideo();
			      const feed = video ? scrollingAncestor(video) : null;
			      let scrolled = false;

			      if (feed && video) {
			        const reel = getDirectFeedChild(video, feed);
			        if (reel) {
			          let target = isNext ? reel.nextElementSibling : reel.previousElementSibling;
			          while (target && target.getBoundingClientRect().height < 40) {
			            target = isNext ? target.nextElementSibling : target.previousElementSibling;
			          }
			          if (target) {
			            const feedRect = feed.getBoundingClientRect();
			            const targetRect = target.getBoundingClientRect();
			            feed.scrollTo({
			              top: feed.scrollTop + (targetRect.top - feedRect.top),
			              behavior: 'instant'
			            });
			            scrolled = true;
			          }
			        }
			      }

			      if (!scrolled) {
			        const btn = document.querySelector(
			          isNext
			            ? 'button[aria-label*="Next" i], [role="button"][aria-label*="Next" i], svg[aria-label="Down chevron"]'
			            : 'button[aria-label*="Previous" i], [role="button"][aria-label*="Previous" i], svg[aria-label="Up chevron"]'
			        );
			        const clickable = btn ? (btn.closest('button, [role="button"]') || btn) : null;
			        if (clickable && !clickable.closest('[role="dialog"]')) {
			          clickable.click();
			          scrolled = true;
			        }
			      }

			      if (!scrolled) {
			        const key = isNext ? 'ArrowDown' : 'ArrowUp';
			        const keyCode = isNext ? 40 : 38;
			        const evtInit = { key, code: key, keyCode, which: keyCode, bubbles: true, cancelable: true };
			        (document.activeElement || document.body || document).dispatchEvent(new KeyboardEvent('keydown', evtInit));
			        document.dispatchEvent(new KeyboardEvent('keydown', evtInit));
			        const scroller = feed || document.scrollingElement || document.documentElement;
			        if (scroller) {
			          scroller.dispatchEvent(new WheelEvent('wheel', { deltaY: direction * 360, bubbles: true, cancelable: true }));
			          scroller.scrollBy({ top: direction * Math.max(window.innerHeight, 480), behavior: 'instant' });
			        }
			      }

			      setTimeout(() => pushState(true), 140);
			      setTimeout(() => pushState(true), 450);
			      setTimeout(() => pushState(true), 850);
			    }

			    function scheduleAutoScroll(videoId) {
			      if (!state.autoScroll || !state.hudMode) return;
			      if (state.autoScrolledForId === videoId && videoId) return;
			      state.autoScrolledForId = videoId || ('anon-' + performance.now());
			      clearTimeout(state.autoScrollTimer);
			      const delay = Math.max(0, state.autoScrollDelayMs);
			      state.autoScrollTimer = setTimeout(() => {
			        state.autoScrollTimer = null;
			        if (state.autoScroll && isOnReelsRoute(location.pathname)) {
			          navigateReel(1);
			        }
			      }, delay);
			    }

			    function pushState(force) {
			      dismissPopupsIfAny();
			      const onReels = isOnReelsRoute(location.pathname);
			      const video = onReels ? getActiveVideo() : null;
			      const reel = video ? getReelContainer(video) : null;

			      syncActiveVideoVisuals(video, onReels);
			      applyAudio(video, reel, onReels);

			      const id = onReels ? getReelId(reel, video) : '';
			      const t = video && Number.isFinite(video.currentTime) ? video.currentTime : 0;
			      const d = video && Number.isFinite(video.duration) ? video.duration : 0;
			      const r = video && Number.isFinite(video.playbackRate) ? video.playbackRate : 1;
			      const p = video ? !!video.paused : true;
			      const b = video ? (video.readyState < 3 && !video.paused) : false;
			      const channel = onReels ? getChannel(reel) : '';
			      const title = onReels ? getTitle(reel, channel) : '';
			      const liked = onReels ? isLiked(reel) : false;
			      const signed = isSignedIn();
			      const videoReady = !!(video && video.readyState >= 2 && video.videoWidth > 0 && Number.isFinite(d) && d > 0.1);
			      const botGate = isLoginGate(signed, videoReady);

			      updateNativeCaption(title, channel, onReels && !botGate);

			      if (id !== state.lastVideoId) {
			        state.lastVideoId = id;
			        state.lastTime = t;
			        state.userPaused = false;
			        state.autoScrolledForId = '';
			        clearTimeout(state.autoScrollTimer);
			        state.autoScrollTimer = null;
			        try {
			          const allVideos = document.querySelectorAll('video');
			          for (const v of allVideos) {
			            if (v !== video && !v.paused) v.pause();
			          }
			        } catch (_) {}
			        if (video && onReels && state.hudMode && !state.userPaused && video.paused && video.readyState >= 2) {
			          video.play().catch(() => {});
			        }
			        applySubtitles(video, reel, id, false);
			        if (state.commentsOpen && onReels) {
			          state.commentsOpenedForId = '';
			          state.lastCommentsKey = '';
			          state.commentsOpenAtMs = performance.now();
			          try {
			            console.info('__DS_CMT__:' + JSON.stringify({ id, open: true, loading: true, sr: false, cnt: '', items: [] }));
			          } catch (_) {}
			          setTimeout(() => {
			            if (!state.commentsOpen) return;
			            const nv = getActiveVideo();
			            const nr = nv ? getReelContainer(nv) : null;
			            openNativeComments(nr, getReelId(nr, nv));
			            pushComments(true);
			          }, 380);
			          setTimeout(() => pushComments(true), 1200);
			        }
			        force = true;
			      } else if (onReels && state.subtitles && state.lastSubtitlesSyncId !== (id + ':true') && videoReady) {
			        applySubtitles(video, reel, id, false);
			      } else if (onReels && state.autoScroll && !p && d > 1.5) {
			        const wrapped = (state.lastTime >= d - 0.55 && t < 0.85);
			        const ended = video && video.ended;
			        const nearEndNoLoop = video && !video.loop && (t >= d - 0.15);
			        if (wrapped || ended || nearEndNoLoop) {
			          scheduleAutoScroll(id);
			        }
			        state.lastTime = t;
			      } else {
			        state.lastTime = t;
			      }

			      if (state.commentsOpen && onReels) {
			        pushComments(false);
			      }

			      const now = performance.now();
			      const key = id + '|' + title + '|' + channel + '|' + p + '|' + state.muted + '|' + liked + '|' + signed + '|' + onReels + '|' + b + '|' + videoReady + '|' + botGate + '|' + Math.round(d * 10);
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
			          sh: onReels,
			          b,
			          vr: videoReady,
			          bg: botGate,
			          url: location.href
			        }));
			      } catch (_) {}
			    }

			    document.addEventListener('ended', (ev) => {
			      if (!state.autoScroll || !state.hudMode || !isOnReelsRoute(location.pathname)) return;
			      const v = ev && ev.target;
			      if (v && v.tagName === 'VIDEO' && v === getActiveVideo()) {
			        const reel = getReelContainer(v);
			        scheduleAutoScroll(getReelId(reel, v));
			      }
			    }, true);

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
			        const v = getActiveVideo();
			        const reel = v ? getReelContainer(v) : null;
			        syncActiveVideoVisuals(v, isOnReelsRoute(location.pathname));
			        applyAudio(v, reel, isOnReelsRoute(location.pathname));
			        applySubtitles(v, reel, getReelId(reel, v), true);
			        pushState(true);
			      },
			      next() { navigateReel(1); },
			      prev() { navigateReel(-1); },
			      togglePlayPause() {
			        const v = getActiveVideo();
			        if (!v) return;
			        if (v.paused) {
			          state.userPaused = false;
			          v.play().catch(() => {});
			        } else {
			          state.userPaused = true;
			          v.pause();
			        }
			        setTimeout(() => pushState(true), 40);
			      },
			      setPaused(shouldPause) {
			        const v = getActiveVideo();
			        if (!v) return;
			        state.userPaused = !!shouldPause;
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
			        const v = getActiveVideo();
			        const reel = getReelContainer(v);
			        const ctrl = getLikeControl(reel);
			        if (ctrl && ctrl.btn) {
			          ctrl.btn.click();
			          setTimeout(() => {
			            const nowLiked = isLiked(reel);
			            console.info('__DS_EVT__:' + (nowLiked ? 'liked' : 'unliked'));
			            pushState(true);
			          }, 150);
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
			        const v = getActiveVideo();
			        const reel = v ? getReelContainer(v) : null;
			        const id = getReelId(reel, v);
			        if (state.commentsOpen) {
			          state.commentsOpenAtMs = performance.now();
			          openNativeComments(reel, id);
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

			    setTimeout(dismissPopupsIfAny, 1000);
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
			""";
}
