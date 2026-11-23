package dev.doomscrolling.ui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class ActionFeedbackOverlay {

	public enum FeedbackType {
		PLAY(560L),
		PAUSE(560L),
		LIKE(760L),
		UNLIKE(520L),
		NEXT(460L),
		PREV(460L),
		MUTE(560L),
		UNMUTE(560L),
		AUTOSCROLL_ON(600L),
		AUTOSCROLL_OFF(600L),
		CAPTIONS_ON(560L),
		CAPTIONS_OFF(560L);

		final long durationMs;

		FeedbackType(long durationMs) {
			this.durationMs = durationMs;
		}
	}

	private static final int ICON_SIZE = 128;

	private static final float[][] HEART_PARTICLES = {
			{-135f, 26f, 8.5f, 20f},
			{-105f, 32f, 10.0f, 0f},
			{-75f, 31f, 9.5f, 35f},
			{-45f, 25f, 8.0f, 15f},
			{-155f, 20f, 7.0f, 55f},
			{-25f, 21f, 7.0f, 45f}
	};

	private static boolean texturesCreated = false;
	private static Identifier texPlay;
	private static Identifier texPause;
	private static Identifier texHeart;
	private static Identifier texHeartOutline;
	private static Identifier texChevronDown;
	private static Identifier texChevronUp;
	private static Identifier texVolumeOn;
	private static Identifier texVolumeMuted;
	private static Identifier texAutoOn;
	private static Identifier texAutoOff;
	private static Identifier texCaptionsOn;
	private static Identifier texCaptionsOff;

	private volatile FeedbackType activeType = null;
	private volatile long startedAtMs = 0L;

	public void trigger(FeedbackType type) {
		this.activeType = type;
		this.startedAtMs = Util.getMillis();
	}

	public void renderLoadingEmblem(GuiGraphicsExtractor g, float cx, float cy, float size, int accentArgb, float alpha) {
		if (alpha <= 0.01f) return;
		ensureTextures();
		float pulse = 1.0f + 0.06f * (float) Math.sin(Util.getMillis() / 260.0);
		float drawSize = size * pulse;
		drawCenteredScaled(g, texPlay, cx, cy + 1.0f, drawSize * 1.08f, withAlpha(0x00000000, alpha * 0.45f));
		drawCenteredScaled(g, texPlay, cx, cy, drawSize, withAlpha(accentArgb & 0x00FFFFFF, alpha));
	}

	public void render(GuiGraphicsExtractor g, int centerX, int centerY, int maxDiameter, int accentArgb) {
		FeedbackType type = this.activeType;
		if (type == null) {
			return;
		}
		long elapsed = Util.getMillis() - startedAtMs;
		if (elapsed < 0L || elapsed >= type.durationMs) {
			this.activeType = null;
			return;
		}

		ensureTextures();

		float t = elapsed / (float) type.durationMs;
		float alpha = computeAlpha(t);
		if (alpha <= 0.01f) {
			return;
		}

		float scale = computeScale(type, t);
		float offsetY = computeOffsetY(type, t);
		int baseSize = Math.max(22, Math.min(36, maxDiameter));

		Identifier iconTex;
		int iconColor;
		float iconRatio;

		switch (type) {
			case PLAY -> {
				iconTex = texPlay;
				iconColor = 0x00FFFFFF;
				iconRatio = 0.84f;
			}
			case PAUSE -> {
				iconTex = texPause;
				iconColor = 0x00FFFFFF;
				iconRatio = 0.84f;
			}
			case LIKE -> {
				iconTex = texHeart;
				iconColor = LunarDraw.COLOR_YT_RED & 0x00FFFFFF;
				iconRatio = 0.90f;
			}
			case UNLIKE -> {
				iconTex = texHeartOutline;
				iconColor = 0x00E8E8E8;
				iconRatio = 0.86f;
			}
			case NEXT -> {
				iconTex = texChevronDown;
				iconColor = 0x00FFFFFF;
				iconRatio = 0.82f;
			}
			case PREV -> {
				iconTex = texChevronUp;
				iconColor = 0x00FFFFFF;
				iconRatio = 0.82f;
			}
			case MUTE -> {
				iconTex = texVolumeMuted;
				iconColor = LunarDraw.COLOR_YT_RED & 0x00FFFFFF;
				iconRatio = 0.88f;
			}
			case UNMUTE -> {
				iconTex = texVolumeOn;
				iconColor = 0x00FFFFFF;
				iconRatio = 0.88f;
			}
			case AUTOSCROLL_ON -> {
				iconTex = texAutoOn;
				iconColor = accentArgb & 0x00FFFFFF;
				iconRatio = 0.86f;
			}
			case AUTOSCROLL_OFF -> {
				iconTex = texAutoOff;
				iconColor = 0x00C8C8C8;
				iconRatio = 0.86f;
			}
			case CAPTIONS_ON -> {
				iconTex = texCaptionsOn;
				iconColor = accentArgb & 0x00FFFFFF;
				iconRatio = 0.88f;
			}
			case CAPTIONS_OFF -> {
				iconTex = texCaptionsOff;
				iconColor = 0x00C8C8C8;
				iconRatio = 0.88f;
			}
			default -> {
				return;
			}
		}

		float drawSize = baseSize * iconRatio * scale;
		drawCenteredScaled(g, iconTex, centerX, centerY + offsetY + 1.5f, drawSize * 1.06f, withAlpha(0x00000000, alpha * 0.55f));
		drawCenteredScaled(g, iconTex, centerX, centerY + offsetY, drawSize, withAlpha(iconColor, alpha));

		if (type == FeedbackType.LIKE) {
			float sizeScale = baseSize / 38.0f;
			for (float[] p : HEART_PARTICLES) {
				float pElapsed = elapsed - p[3];
				if (pElapsed <= 0f) continue;
				float pt = Math.min(1.0f, pElapsed / (type.durationMs - p[3]));
				float ease = 1.0f - (1.0f - pt) * (1.0f - pt) * (1.0f - pt);
				double rad = Math.toRadians(p[0]);
				float dist = p[1] * sizeScale * ease;
				float px = centerX + (float) Math.cos(rad) * dist;
				float py = centerY + (float) Math.sin(rad) * dist - 6.0f * sizeScale * pt;
				float pAlpha = pt < 0.2f ? (pt / 0.2f) : Math.max(0.0f, 1.0f - (pt - 0.2f) / 0.8f);
				float pScale = (0.6f + 0.5f * ease) * (1.0f - 0.25f * pt);
				float pSize = p[2] * sizeScale * pScale;
				if (pAlpha > 0.02f && pSize > 1.5f) {
					drawCenteredScaled(g, texHeart, px, py, pSize, withAlpha(LunarDraw.COLOR_YT_RED & 0x00FFFFFF, pAlpha * 0.95f));
				}
			}
		}
	}

	private static float computeAlpha(float t) {
		if (t < 0.16f) {
			return t / 0.16f;
		}
		if (t < 0.58f) {
			return 1.0f;
		}
		float fade = (t - 0.58f) / 0.42f;
		return Math.max(0.0f, 1.0f - fade * fade);
	}

	private static float computeScale(FeedbackType type, float t) {
		if (type == FeedbackType.LIKE) {
			if (t < 0.24f) {
				float k = t / 0.24f;
				return 0.48f + 0.74f * easeOutBack(k);
			}
			if (t < 0.48f) {
				float k = (t - 0.24f) / 0.24f;
				return 1.22f - 0.20f * k;
			}
			float k = (t - 0.48f) / 0.52f;
			return 1.02f + 0.10f * k;
		}
		if (type == FeedbackType.UNLIKE) {
			return 1.06f - 0.18f * t;
		}
		if (t < 0.22f) {
			float k = t / 0.22f;
			return 0.74f + 0.28f * easeOutCubic(k);
		}
		float k = (t - 0.22f) / 0.78f;
		return 1.02f + 0.14f * k;
	}

	private static float computeOffsetY(FeedbackType type, float t) {
		float ease = easeOutCubic(t);
		if (type == FeedbackType.NEXT) {
			return -4.0f + 12.0f * ease;
		}
		if (type == FeedbackType.PREV) {
			return 4.0f - 12.0f * ease;
		}
		return 0.0f;
	}

	private static float easeOutCubic(float x) {
		float inv = 1.0f - x;
		return 1.0f - inv * inv * inv;
	}

	private static float easeOutBack(float x) {
		float c1 = 1.70158f;
		float c3 = c1 + 1.0f;
		float xm1 = x - 1.0f;
		return 1.0f + c3 * xm1 * xm1 * xm1 + c1 * xm1 * xm1;
	}

	private static void drawCenteredScaled(GuiGraphicsExtractor g, Identifier tex, float cx, float cy, float size, int argbTint) {
		if (tex == null || size <= 1.0f) return;
		int base = 32;
		float scale = size / (float) base;
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().scale(scale, scale);
		g.blit(RenderPipelines.GUI_TEXTURED, tex, -base / 2, -base / 2, 0.0f, 0.0f, base, base, base, base, argbTint);
		g.pose().popMatrix();
	}

	private static int withAlpha(int rgb, float alpha) {
		int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
		return (a << 24) | (rgb & 0x00FFFFFF);
	}

	private static void ensureTextures() {
		if (texturesCreated) return;
		texturesCreated = true;

		texPlay = registerSdfIcon("feedback_play", ActionFeedbackOverlay::sdRoundedPlayTriangle);
		texPause = registerSdfIcon("feedback_pause", ActionFeedbackOverlay::sdPauseBars);
		texHeart = registerSdfIcon("feedback_heart", ActionFeedbackOverlay::sdHeart);
		texHeartOutline = registerSdfIcon("feedback_heart_outline", (x, y) -> Math.abs(sdHeart(x, y)) - 0.075f);
		texChevronDown = registerSdfIcon("feedback_chevron_down", (x, y) -> sdChevron(x, y, true));
		texChevronUp = registerSdfIcon("feedback_chevron_up", (x, y) -> sdChevron(x, y, false));
		texVolumeOn = registerSdfIcon("feedback_volume_on", (x, y) -> sdSpeaker(x, y, false));
		texVolumeMuted = registerSdfIcon("feedback_volume_muted", (x, y) -> sdSpeaker(x, y, true));
		texAutoOn = registerSdfIcon("feedback_auto_on", (x, y) -> sdAutoLoop(x, y, false));
		texAutoOff = registerSdfIcon("feedback_auto_off", (x, y) -> sdAutoLoop(x, y, true));
		texCaptionsOn = registerSdfIcon("feedback_cc_on", (x, y) -> sdCaptions(x, y, false));
		texCaptionsOff = registerSdfIcon("feedback_cc_off", (x, y) -> sdCaptions(x, y, true));
	}

	@FunctionalInterface
	private interface SdfFunction {
		float eval(float x, float y);
	}

	private static Identifier registerSdfIcon(String name, SdfFunction sdf) {
		NativeImage image = new NativeImage(ICON_SIZE, ICON_SIZE, true);
		float aaWidth = 2.6f / ICON_SIZE;
		for (int py = 0; py < ICON_SIZE; py++) {
			float ny = ((py + 0.5f) / ICON_SIZE) * 2.0f - 1.0f;
			for (int px = 0; px < ICON_SIZE; px++) {
				float nx = ((px + 0.5f) / ICON_SIZE) * 2.0f - 1.0f;
				float d = sdf.eval(nx, ny);
				float cov = smoothstep(aaWidth, -aaWidth, d);
				int a = Math.max(0, Math.min(255, Math.round(cov * 255.0f)));
				image.setPixel(px, py, (a << 24) | 0x00FFFFFF);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath("doomscrolling", name);
		DynamicTexture tex = new DynamicTexture(() -> "doomscrolling_" + name, image);
		Minecraft.getInstance().getTextureManager().register(id, tex);
		return id;
	}

	private static float smoothstep(float edge0, float edge1, float x) {
		float t = Math.max(0.0f, Math.min(1.0f, (x - edge0) / (edge1 - edge0)));
		return t * t * (3.0f - 2.0f * t);
	}

	private static float sdRoundedBox(float x, float y, float bx, float by, float r) {
		float qx = Math.abs(x) - bx + r;
		float qy = Math.abs(y) - by + r;
		float out = (float) Math.hypot(Math.max(qx, 0.0f), Math.max(qy, 0.0f));
		float in = Math.min(Math.max(qx, qy), 0.0f);
		return out + in - r;
	}

	private static float sdSegment(float px, float py, float ax, float ay, float bx, float by, float r) {
		float pax = px - ax;
		float pay = py - ay;
		float bax = bx - ax;
		float bay = by - ay;
		float denom = bax * bax + bay * bay;
		float h = denom > 1e-6f ? Math.max(0.0f, Math.min(1.0f, (pax * bax + pay * bay) / denom)) : 0.0f;
		return (float) Math.hypot(pax - bax * h, pay - bay * h) - r;
	}

	private static float sdRoundedPlayTriangle(float x, float y) {
		return sdTriangle(x - 0.06f, y, -0.44f, -0.52f, -0.44f, 0.52f, 0.54f, 0.0f) - 0.11f;
	}

	private static float sdTriangle(float px, float py, float p0x, float p0y, float p1x, float p1y, float p2x, float p2y) {
		float e0x = p1x - p0x, e0y = p1y - p0y;
		float e1x = p2x - p1x, e1y = p2y - p1y;
		float e2x = p0x - p2x, e2y = p0y - p2y;

		float v0x = px - p0x, v0y = py - p0y;
		float v1x = px - p1x, v1y = py - p1y;
		float v2x = px - p2x, v2y = py - p2y;

		float t0 = clamp01((v0x * e0x + v0y * e0y) / (e0x * e0x + e0y * e0y));
		float t1 = clamp01((v1x * e1x + v1y * e1y) / (e1x * e1x + e1y * e1y));
		float t2 = clamp01((v2x * e2x + v2y * e2y) / (e2x * e2x + e2y * e2y));

		float pq0x = v0x - e0x * t0, pq0y = v0y - e0y * t0;
		float pq1x = v1x - e1x * t1, pq1y = v1y - e1y * t1;
		float pq2x = v2x - e2x * t2, pq2y = v2y - e2y * t2;

		float s = Math.signum(e0x * e2y - e0y * e2x);
		float d0 = pq0x * pq0x + pq0y * pq0y;
		float d1 = pq1x * pq1x + pq1y * pq1y;
		float d2 = pq2x * pq2x + pq2y * pq2y;
		float minDistSq = Math.min(d0, Math.min(d1, d2));

		float s0 = s * (v0x * e0y - v0y * e0x);
		float s1 = s * (v1x * e1y - v1y * e1x);
		float s2 = s * (v2x * e2y - v2y * e2x);
		float minSign = Math.min(s0, Math.min(s1, s2));

		return (float) (-Math.sqrt(minDistSq) * Math.signum(minSign));
	}

	private static float sdPauseBars(float x, float y) {
		float left = sdRoundedBox(x + 0.30f, y, 0.17f, 0.58f, 0.085f);
		float right = sdRoundedBox(x - 0.30f, y, 0.17f, 0.58f, 0.085f);
		return Math.min(left, right);
	}

	private static float sdHeart(float x, float y) {
		float px = Math.abs(x * 0.78f);
		float py = (-y) * 0.78f + 0.52f;
		if (py + px > 1.0f) {
			float dx = px - 0.25f;
			float dy = py - 0.75f;
			return (float) Math.hypot(dx, dy) - (float) (Math.sqrt(2.0) / 4.0);
		}
		float dx1 = px;
		float dy1 = py - 1.0f;
		float d1 = dx1 * dx1 + dy1 * dy1;
		float m = 0.5f * Math.max(px + py, 0.0f);
		float dx2 = px - m;
		float dy2 = py - m;
		float d2 = dx2 * dx2 + dy2 * dy2;
		return (float) (Math.sqrt(Math.min(d1, d2)) * Math.signum(px - py));
	}

	private static float sdChevron(float x, float y, boolean down) {
		float sy = down ? y : -y;
		float left = sdSegment(x, sy, -0.50f, -0.20f, 0.0f, 0.30f, 0.12f);
		float right = sdSegment(x, sy, 0.0f, 0.30f, 0.50f, -0.20f, 0.12f);
		return Math.min(left, right);
	}

	private static float sdSpeaker(float x, float y, boolean muted) {
		float body = sdRoundedBox(x + 0.44f, y, 0.18f, 0.22f, 0.06f);
		float cone = sdTriangle(x, y, -0.42f, 0.0f, 0.05f, -0.50f, 0.05f, 0.50f) - 0.05f;
		float speaker = Math.min(body, cone);
		if (muted) {
			float x1 = sdSegment(x, y, 0.24f, -0.24f, 0.68f, 0.24f, 0.075f);
			float x2 = sdSegment(x, y, 0.24f, 0.24f, 0.68f, -0.24f, 0.075f);
			return Math.min(speaker, Math.min(x1, x2));
		}
		float r = (float) Math.hypot(x - 0.02f, y);
		float angle = (float) Math.atan2(y, x - 0.02f);
		float wave = 10.0f;
		if (Math.abs(angle) < 0.78f && x > 0.08f) {
			float w1 = Math.abs(r - 0.34f) - 0.065f;
			float w2 = Math.abs(r - 0.62f) - 0.065f;
			wave = Math.min(w1, w2);
		}
		return Math.min(speaker, wave);
	}

	private static float sdAutoLoop(float x, float y, boolean off) {
		float r = (float) Math.hypot(x, y);
		float angle = (float) Math.atan2(y, x);
		float ring = Math.abs(r - 0.54f) - 0.085f;
		if (angle > -0.75f && angle < 0.15f) {
			ring = 1.0f;
		}
		float arrow1 = sdSegment(x, y, 0.48f, -0.34f, 0.22f, -0.36f, 0.075f);
		float arrow2 = sdSegment(x, y, 0.48f, -0.34f, 0.52f, -0.62f, 0.075f);
		float d = Math.min(ring, Math.min(arrow1, arrow2));
		if (off) {
			float slash = sdSegment(x, y, -0.58f, -0.58f, 0.58f, 0.58f, 0.08f);
			d = Math.min(d, slash);
		}
		return d;
	}

	private static float sdCaptions(float x, float y, boolean off) {
		float box = Math.abs(sdRoundedBox(x, y, 0.72f, 0.50f, 0.14f)) - 0.065f;
		float line1 = sdSegment(x, y, -0.42f, -0.10f, 0.06f, -0.10f, 0.065f);
		float line2 = sdSegment(x, y, 0.24f, -0.10f, 0.42f, -0.10f, 0.065f);
		float line3 = sdSegment(x, y, -0.42f, 0.16f, -0.18f, 0.16f, 0.065f);
		float line4 = sdSegment(x, y, 0.00f, 0.16f, 0.42f, 0.16f, 0.065f);
		float lines = Math.min(Math.min(line1, line2), Math.min(line3, line4));
		float d = Math.min(box, lines);
		if (off) {
			float slash = sdSegment(x, y, -0.62f, -0.56f, 0.62f, 0.56f, 0.075f);
			d = Math.min(d, slash);
		}
		return d;
	}

	private static float clamp01(float v) {
		return Math.max(0.0f, Math.min(1.0f, v));
	}
}
