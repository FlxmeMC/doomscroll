package dev.doomscrolling.ui;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.rinku.RinkuBrowser;
import dev.doomscrolling.browser.ShortsBrowserController;
import dev.doomscrolling.browser.ShortsState;
import dev.doomscrolling.config.DoomscrollingConfig;
import dev.doomscrolling.platform.McPlatform;
import dev.doomscrolling.platform.UiCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public final class OverlayScreenLogic {

	private record ClickArea(int x, int y, int w, int h, Runnable action, String label, boolean active) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private final Screen parent;
	private final ShortsBrowserController controller;
	private final LunarHudRenderer hudRenderer;

	private Minecraft minecraft;
	private Font font;
	private int width;
	private int height;

	private boolean signInModalOpen = false;
	private int modalX, modalY, modalW, modalH;
	private int modalRenderW, modalRenderH;
	private boolean browserMouseDown = false;

	private int panelX, panelY, panelW, panelH, footerTextY;
	private final List<ClickArea> buttons = new ArrayList<>();
	private boolean lastSignedInState = false;

	public OverlayScreenLogic(Screen parent, ShortsBrowserController controller, LunarHudRenderer hudRenderer) {
		this.parent = parent;
		this.controller = controller;
		this.hudRenderer = hudRenderer;
	}

	public void init(Minecraft minecraft, Font font, int width, int height) {
		this.minecraft = minecraft;
		this.font = font;
		this.width = width;
		this.height = height;
		lastSignedInState = controller.state().signedIn();

		if (signInModalOpen) {
			layoutSignInModal();
		} else {
			DoomscrollingConfig cfg = controller.config();
			controller.setSurfaceMode(ShortsBrowserController.SurfaceMode.HUD, cfg.qualityPreset.width(), cfg.qualityPreset.height());
		}
		rebuildControls();
	}

	private void openSignInModal() {
		signInModalOpen = true;
		browserMouseDown = false;
		layoutSignInModal();
		controller.openSignInPage();
		rebuildControls();
	}

	private void closeSignInModal(boolean returnToShorts) {
		signInModalOpen = false;
		browserMouseDown = false;
		if (returnToShorts) {
			controller.openShortsFeed();
		}
		DoomscrollingConfig cfg = controller.config();
		controller.setSurfaceMode(ShortsBrowserController.SurfaceMode.HUD, cfg.qualityPreset.width(), cfg.qualityPreset.height());
		rebuildControls();
	}

	private void layoutSignInModal() {
		int headerH = 24;
		int maxW = Math.max(220, width - 32);
		int maxH = Math.max(180, height - 32 - headerH);
		modalW = Math.min(380, maxW);
		modalH = Math.min(420, maxH);
		modalX = (width - modalW) / 2;
		modalY = (height - (modalH + headerH)) / 2 + headerH;

		float scale = minecraft != null
				? Math.max(1.0f, Math.min(1.5f, (float) minecraft.getWindow().getGuiScale()))
				: 1.0f;
		if (modalW * scale > 760) scale = 760.0f / modalW;
		if (modalH * scale > 800) scale = 800.0f / modalH;
		modalRenderW = Math.max(360, Math.round(modalW * scale));
		modalRenderH = Math.max(260, Math.round(modalH * scale));
		controller.setSurfaceMode(ShortsBrowserController.SurfaceMode.SIGN_IN, modalRenderW, modalRenderH);
	}

	private void rebuildControls() {
		buttons.clear();

		if (signInModalOpen) {
			int barY = modalY - 22;
			buttons.add(new ClickArea(modalX + 2, barY, 54, 18, () -> closeSignInModal(true), "<- Back", false));
			buttons.add(new ClickArea(modalX + modalW - 44, barY, 42, 18, controller::reloadPage, "Reload", false));
			return;
		}

		DoomscrollingConfig cfg = controller.config();
		DoomscrollingConfig.FeedPlatform activePlatform = cfg.platform != null
				? cfg.platform
				: DoomscrollingConfig.FeedPlatform.YOUTUBE_SHORTS;
		ShortsState st = controller.state();
		boolean compact = height < 260;
		int btnH = compact ? 16 : 20;
		int gap = compact ? 3 : 5;
		boolean showSignInBtn = !st.signedIn();

		panelW = Math.min(204, Math.max(182, width / 2 - 24));
		int rows = showSignInBtn ? 7 : 6;
		int buttonsBlockH = rows * btnH + (rows - 1) * gap + (showSignInBtn ? 1 : 0);
		int footerBandH = compact ? 18 : 22;
		panelH = 23 + buttonsBlockH + footerBandH;

		boolean hudOnRight = cfg.posX >= 0.45f;
		panelX = hudOnRight ? 12 : Math.max(12, width - panelW - 12);
		panelY = Math.max(8, (height - panelH) / 2);
		if (panelY + panelH > height - 6) {
			panelY = Math.max(6, height - panelH - 6);
		}
		int buttonsBottomY = panelY + 23 + buttonsBlockH;
		footerTextY = buttonsBottomY + (footerBandH - font.lineHeight) / 2 + 1;

		buttons.add(new ClickArea(panelX + panelW - 21, panelY + 4, 16, 14, this::onClose, "x", false));

		int ix = panelX + 8;
		int iw = panelW - 16;
		int halfW = (iw - 4) / 2;
		int y = panelY + 23;

		buttons.add(new ClickArea(ix, y, halfW, btnH, () -> {
			controller.setPlatform(DoomscrollingConfig.FeedPlatform.YOUTUBE_SHORTS);
			rebuildControls();
		}, "YouTube", activePlatform == DoomscrollingConfig.FeedPlatform.YOUTUBE_SHORTS));

		buttons.add(new ClickArea(ix + halfW + 4, y, iw - halfW - 4, btnH, () -> {
			controller.setPlatform(DoomscrollingConfig.FeedPlatform.INSTAGRAM_REELS);
			rebuildControls();
		}, "Instagram", activePlatform == DoomscrollingConfig.FeedPlatform.INSTAGRAM_REELS));
		y += btnH + gap;

		if (showSignInBtn) {
			buttons.add(new ClickArea(ix, y, iw, btnH, this::openSignInModal, "Sign in to " + activePlatform.shortName(), true));
			y += btnH + gap + 1;
		}

		buttons.add(new ClickArea(ix, y, halfW, btnH, () -> {
			cfg.autoScroll = !cfg.autoScroll;
			controller.applySettingsChanged();
			rebuildControls();
		}, "Auto: " + (cfg.autoScroll ? "ON" : "OFF"), cfg.autoScroll));

		buttons.add(new ClickArea(ix + halfW + 4, y, iw - halfW - 4, btnH, () -> {
			cfg.autoScrollDelayMs = cfg.nextAutoScrollDelayMs();
			controller.applySettingsChanged();
			rebuildControls();
		}, "Delay: " + cfg.autoScrollDelayMs + "ms", false));
		y += btnH + gap;

		buttons.add(new ClickArea(ix, y, halfW, btnH, () -> {
			cfg.layoutMode = cfg.layoutMode.next();
			controller.applySettingsChanged();
			rebuildControls();
		}, "View: " + cfg.layoutModeDisplayName(), false));

		buttons.add(new ClickArea(ix + halfW + 4, y, iw - halfW - 4, btnH, () -> {
			cfg.accentTheme = cfg.accentTheme.next();
			controller.applySettingsChanged();
			rebuildControls();
		}, cfg.accentTheme.displayName(), true));
		y += btnH + gap;

		buttons.add(new ClickArea(ix, y, halfW, btnH, () -> {
			cfg.qualityPreset = cfg.qualityPreset.next();
			controller.applySettingsChanged();
			rebuildControls();
		}, "Res: " + cfg.qualityPreset.displayName(), false));

		buttons.add(new ClickArea(ix + halfW + 4, y, iw - halfW - 4, btnH, () -> {
			cfg.browserFps = cfg.nextFpsCap();
			controller.applySettingsChanged();
			rebuildControls();
		}, "FPS: " + cfg.browserFps, false));
		y += btnH + gap;

		buttons.add(new ClickArea(ix, y, halfW, btnH, () -> {
			cfg.pauseWhenHidden = !cfg.pauseWhenHidden;
			controller.applySettingsChanged();
			rebuildControls();
		}, "Sleep: " + (cfg.pauseWhenHidden ? "ON" : "OFF"), cfg.pauseWhenHidden));

		buttons.add(new ClickArea(ix + halfW + 4, y, iw - halfW - 4, btnH, () -> {
			controller.toggleHudVisible();
			rebuildControls();
		}, "HUD: " + (cfg.hudVisible ? "ON" : "OFF"), cfg.hudVisible));
		y += btnH + gap;

		buttons.add(new ClickArea(ix, y, halfW, btnH, controller::openShortsFeed, "Refresh", false));
		buttons.add(new ClickArea(ix + halfW + 4, y, iw - halfW - 4, btnH, () -> {
			cfg.resetPosition();
			cfg.hudWidth = 144;
			controller.applySettingsChanged();
			rebuildControls();
		}, "Reset Layout", false));
	}

	public void tick() {
		ShortsState st = controller.state();
		if (signInModalOpen && st.signedIn()) {
			closeSignInModal(!st.onShortsPage());
			return;
		}
		if (st.signedIn() != lastSignedInState) {
			lastSignedInState = st.signedIn();
			rebuildControls();
		}
	}

	public void onClose() {
		hudRenderer.resetInteraction();
		if (signInModalOpen) {
			closeSignInModal(true);
			return;
		}
		controller.returnToHudMode();
		if (minecraft != null) {
			McPlatform.get().setScreen(minecraft, parent);
		}
	}

	public void renderBackground(UiCanvas g) {
		g.fill(0, 0, width, height, signInModalOpen ? 0xBB080808 : 0x48080808);
	}

	public void render(UiCanvas g, int mouseX, int mouseY) {
		int accent = controller.config().accentTheme.argb();
		DoomscrollingConfig.FeedPlatform activePlatform = controller.config().platform != null
				? controller.config().platform
				: DoomscrollingConfig.FeedPlatform.YOUTUBE_SHORTS;

		if (signInModalOpen) {
			int headerH = 24;
			LunarDraw.drawGlassCard(g, modalX - 2, modalY - headerH, modalW + 4, modalH + headerH + 2, 0xF20F0F0F, false);
			g.drawCenteredText(font, "Sign in to " + activePlatform.shortName(), modalX + modalW / 2, modalY - 17, LunarDraw.COLOR_TEXT_PRIMARY);
			blitSignInBrowser(g, modalX, modalY, modalW, modalH, accent, activePlatform);
		} else {
			if (controller.config().hudVisible) {
				hudRenderer.renderCard(g, font, width, height, mouseX, mouseY, true, true, true);
			}

			LunarDraw.drawGlassCard(g, panelX, panelY, panelW, panelH, LunarDraw.COLOR_YT_BLACK, false);
			g.drawText(font, "Doomscroll", panelX + 8, panelY + 7, LunarDraw.COLOR_TEXT_PRIMARY, false);
			g.drawCenteredText(font, "Drag to move • Edges to resize", panelX + panelW / 2, footerTextY, LunarDraw.COLOR_TEXT_MUTED);
		}

		for (ClickArea btn : buttons) {
			LunarDraw.drawLunarButton(g, font, btn.x, btn.y, btn.w, btn.h, btn.label, btn.contains(mouseX, mouseY), btn.active, accent);
		}
	}

	private void blitSignInBrowser(UiCanvas g, int x, int y, int w, int h, int accent, DoomscrollingConfig.FeedPlatform platform) {
		g.fill(x, y, x + w, y + h, 0xFF0F0F0F);
		Object tex = controller.getTextureIdentifier();
		if (tex != null && controller.isBrowserTextureReady()) {
			g.blitTexture(tex, x, y, w, h);
		} else {
			String provider = platform == DoomscrollingConfig.FeedPlatform.INSTAGRAM_REELS ? "Instagram" : "Google";
			g.drawCenteredText(font, "Loading " + provider + " Sign-In...", x + w / 2, y + h / 2 - 6, LunarDraw.COLOR_TEXT_SECONDARY);
			LunarDraw.drawIndeterminateBar(g, x + w / 2 - 44, y + h / 2 + 8, 88, 3, accent);
		}
	}

	private boolean isInSignInModal(double mx, double my) {
		return signInModalOpen && mx >= modalX && my >= modalY && mx < modalX + modalW && my < modalY + modalH;
	}

	private int toModalX(double mx) {
		return Math.round((float) ((mx - modalX) / Math.max(1, modalW) * modalRenderW));
	}

	private int toModalY(double my) {
		return Math.round((float) ((my - modalY) / Math.max(1, modalH) * modalRenderH));
	}

	public boolean mouseClicked(double mx, double my, int button) {
		if (button == InputConstants.MOUSE_BUTTON_LEFT) {
			for (ClickArea btn : buttons) {
				if (btn.contains(mx, my)) {
					btn.action.run();
					return true;
				}
			}
		}

		if (signInModalOpen) {
			if (isInSignInModal(mx, my) && controller.browser() != null) {
				RinkuBrowser b = controller.browser();
				b.setFocus(true);
				browserMouseDown = true;
				b.sendMousePress(toModalX(mx), toModalY(my), button);
				return true;
			}
			return false;
		}

		return hudRenderer.handleMouseClick(mx, my, button, width, height, false, true);
	}

	public boolean mouseReleased(double mx, double my, int button) {
		if (browserMouseDown && controller.browser() != null) {
			browserMouseDown = false;
			int bx = Math.max(0, Math.min(modalRenderW, toModalX(mx)));
			int by = Math.max(0, Math.min(modalRenderH, toModalY(my)));
			controller.browser().sendMouseRelease(bx, by, button);
			return true;
		}
		if (hudRenderer.handleMouseRelease()) {
			rebuildControls();
			return true;
		}
		return false;
	}

	public void mouseMoved(double mx, double my) {
		if (isInSignInModal(mx, my) && controller.browser() != null) {
			controller.browser().sendMouseMove(toModalX(mx), toModalY(my));
		}
	}

	public boolean mouseDragged(double mx, double my) {
		if (signInModalOpen && isInSignInModal(mx, my) && controller.browser() != null) {
			controller.browser().sendMouseMove(toModalX(mx), toModalY(my));
			return true;
		}
		return !signInModalOpen && hudRenderer.handleMouseDrag(mx, my, width, height);
	}

	public boolean mouseScrolled(double mx, double my, double scrollY) {
		if (isInSignInModal(mx, my) && controller.browser() != null) {
			controller.browser().sendMouseWheel(toModalX(mx), toModalY(my), scrollY, 0);
			return true;
		}
		return !signInModalOpen && hudRenderer.handleMouseScroll(mx, my, scrollY, width, height);
	}

	public boolean keyPressed(int key, int scancode, int modifiers, Object rawEvent) {
		if (key == InputConstants.KEY_ESCAPE) {
			onClose();
			return true;
		}
		if (signInModalOpen && controller.browser() != null) {
			controller.browser().setFocus(true);
			McPlatform.get().sendKeyPress(controller.browser(), key, scancode, modifiers, rawEvent);
			return true;
		}
		return false;
	}

	public boolean keyReleased(int key, int scancode, int modifiers, Object rawEvent) {
		if (signInModalOpen && controller.browser() != null) {
			McPlatform.get().sendKeyRelease(controller.browser(), key, scancode, modifiers, rawEvent);
			return true;
		}
		return false;
	}

	public boolean charTyped(char ch) {
		if (signInModalOpen && controller.browser() != null && ch != 0) {
			controller.browser().sendKeyTyped(ch, 0);
			return true;
		}
		return false;
	}
}
