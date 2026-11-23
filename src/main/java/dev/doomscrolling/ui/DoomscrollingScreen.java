package dev.doomscrolling.ui;

import de.keksuccino.rinku.RinkuBrowser;
import dev.doomscrolling.browser.ShortsBrowserController;
import dev.doomscrolling.browser.ShortsState;
import dev.doomscrolling.config.DoomscrollingConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class DoomscrollingScreen extends Screen {

	private record ClickArea(int x, int y, int w, int h, Runnable action, String label, boolean active) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private final Screen parent;
	private final ShortsBrowserController controller;
	private final LunarHudRenderer hudRenderer;

	private boolean signInModalOpen = false;
	private int modalX, modalY, modalW, modalH;
	private int modalRenderW, modalRenderH;
	private boolean browserMouseDown = false;

	private int panelX, panelY, panelW, panelH, footerTextY;
	private final List<ClickArea> buttons = new ArrayList<>();
	private boolean lastSignedInState = false;

	public DoomscrollingScreen(Screen parent, ShortsBrowserController controller, LunarHudRenderer hudRenderer) {
		super(Component.translatable("screen.doomscrolling.title"));
		this.parent = parent;
		this.controller = controller;
		this.hudRenderer = hudRenderer;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		super.init();
		clearWidgets();
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
		modalW = Math.min(340, maxW);
		modalH = Math.min(410, maxH);
		modalX = (width - modalW) / 2;
		modalY = (height - (modalH + headerH)) / 2 + headerH;

		float scale = Math.max(1.0f, Math.min(1.5f, (float) minecraft.getWindow().getGuiScale()));
		if (modalW * scale > 720) scale = 720.0f / modalW;
		if (modalH * scale > 780) scale = 780.0f / modalH;
		modalRenderW = Math.max(300, Math.round(modalW * scale));
		modalRenderH = Math.max(240, Math.round(modalH * scale));
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
		ShortsState st = controller.state();
		boolean compact = height < 260;
		int btnH = compact ? 17 : 20;
		int gap = compact ? 4 : 5;
		boolean showSignInBtn = !st.signedIn();

		panelW = Math.min(204, Math.max(182, width / 2 - 24));
		int rows = showSignInBtn ? 6 : 5;
		int buttonsBlockH = rows * btnH + (rows - 1) * gap + (showSignInBtn ? 1 : 0);
		int footerBandH = 22;
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

		if (showSignInBtn) {
			buttons.add(new ClickArea(ix, y, iw, btnH, this::openSignInModal, "Sign in to YouTube", true));
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
		}, "View: " + cfg.layoutMode.displayName(), false));

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

	@Override
	public void resize(int w, int h) {
		super.resize(w, h);
		init();
	}

	@Override
	public void tick() {
		super.tick();
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

	@Override
	public void onClose() {
		hudRenderer.resetInteraction();
		if (signInModalOpen) {
			closeSignInModal(true);
			return;
		}
		controller.returnToHudMode();
		if (minecraft != null) {
			minecraft.gui.setScreen(parent);
		}
	}

	@Override
	public void extractBackground(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, signInModalOpen ? 0xBB080808 : 0x48080808);
	}

	@Override
	public void extractRenderState(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		int accent = controller.config().accentTheme.argb();

		if (signInModalOpen) {
			int headerH = 24;
			LunarDraw.drawGlassCard(g, modalX - 2, modalY - headerH, modalW + 4, modalH + headerH + 2, 0xF20F0F0F, false);
			g.centeredText(font, "Sign in to YouTube", modalX + modalW / 2, modalY - 17, LunarDraw.COLOR_TEXT_PRIMARY);
			blitSignInBrowser(g, modalX, modalY, modalW, modalH, accent);
		} else {
			if (controller.config().hudVisible) {
				hudRenderer.renderCard(g, font, width, height, mouseX, mouseY, true, true, true);
			}

			LunarDraw.drawGlassCard(g, panelX, panelY, panelW, panelH, LunarDraw.COLOR_YT_BLACK, false);
			g.text(font, "Doomscrolling", panelX + 8, panelY + 7, LunarDraw.COLOR_TEXT_PRIMARY, false);
			g.centeredText(font, "Drag to move • Edges to resize", panelX + panelW / 2, footerTextY, LunarDraw.COLOR_TEXT_MUTED);
		}

		for (ClickArea btn : buttons) {
			LunarDraw.drawLunarButton(g, font, btn.x, btn.y, btn.w, btn.h, btn.label, btn.contains(mouseX, mouseY), btn.active, accent);
		}
	}

	private void blitSignInBrowser(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent) {
		g.fill(x, y, x + w, y + h, 0xFF0F0F0F);
		Identifier tex = controller.getTextureIdentifier();
		if (tex != null && controller.isBrowserTextureReady()) {
			g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0f, 0.0f, w, h, w, h);
		} else {
			g.centeredText(font, "Loading Google Sign-In...", x + w / 2, y + h / 2 - 6, LunarDraw.COLOR_TEXT_SECONDARY);
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

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
		if (super.mouseClicked(event, isDoubleClick)) {
			return true;
		}
		double mx = event.x();
		double my = event.y();

		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
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
				b.sendMousePress(toModalX(mx), toModalY(my), event.button());
				return true;
			}
			return false;
		}

		return hudRenderer.handleMouseClick(mx, my, event.button(), width, height, false, true);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (browserMouseDown && controller.browser() != null) {
			browserMouseDown = false;
			int bx = Math.max(0, Math.min(modalRenderW, toModalX(event.x())));
			int by = Math.max(0, Math.min(modalRenderH, toModalY(event.y())));
			controller.browser().sendMouseRelease(bx, by, event.button());
			return true;
		}
		if (hudRenderer.handleMouseRelease()) {
			rebuildControls();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		if (isInSignInModal(mouseX, mouseY) && controller.browser() != null) {
			controller.browser().sendMouseMove(toModalX(mouseX), toModalY(mouseY));
		}
		super.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (signInModalOpen && isInSignInModal(event.x(), event.y()) && controller.browser() != null) {
			controller.browser().sendMouseMove(toModalX(event.x()), toModalY(event.y()));
			return true;
		}
		if (!signInModalOpen && hudRenderer.handleMouseDrag(event.x(), event.y(), width, height)) {
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (isInSignInModal(mouseX, mouseY) && controller.browser() != null) {
			controller.browser().sendMouseWheel(toModalX(mouseX), toModalY(mouseY), scrollY, 0);
			return true;
		}
		if (!signInModalOpen && hudRenderer.handleMouseScroll(mouseX, mouseY, scrollY, width, height)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		if (signInModalOpen && controller.browser() != null) {
			controller.browser().setFocus(true);
			controller.browser().sendKeyPress(event.key(), event.scancode(), event.modifiers());
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (signInModalOpen && controller.browser() != null) {
			controller.browser().sendKeyRelease(event.key(), event.scancode(), event.modifiers());
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (signInModalOpen && controller.browser() != null) {
			if (event.codepoint() != 0) {
				controller.browser().sendKeyTyped((char) event.codepoint(), 0);
				return true;
			}
		}
		return super.charTyped(event);
	}
}
