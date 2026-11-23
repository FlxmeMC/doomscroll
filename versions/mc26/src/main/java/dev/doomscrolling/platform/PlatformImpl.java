package dev.doomscrolling.platform;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.rinku.RinkuBrowser;
import dev.doomscrolling.DoomscrollingClient;
import dev.doomscrolling.browser.ShortsBrowserController;
import dev.doomscrolling.ui.LunarHudRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

final class PlatformImpl implements McPlatform {

	static final PlatformImpl INSTANCE = new PlatformImpl();

	private PlatformImpl() {
	}

	@Override
	public Object registerArgbTexture(String name, int width, int height, int[] argbPixels) {
		NativeImage image = new NativeImage(width, height, true);
		for (int py = 0; py < height; py++) {
			int row = py * width;
			for (int px = 0; px < width; px++) {
				image.setPixel(px, py, argbPixels[row + px]);
			}
		}
		Identifier id = Identifier.fromNamespaceAndPath(DoomscrollingClient.MOD_ID, name);
		DynamicTexture tex = new DynamicTexture(() -> "doomscrolling_" + name, image);
		Minecraft.getInstance().getTextureManager().register(id, tex);
		return id;
	}

	@Override
	public Object getBrowserTexture(RinkuBrowser browser) {
		return browser.getTextureIdentifier();
	}

	@Override
	public Screen currentScreen(Minecraft mc) {
		return Mc26Shim.currentScreen(mc);
	}

	@Override
	public void setScreen(Minecraft mc, Screen screen) {
		Mc26Shim.setScreen(mc, screen);
	}

	@Override
	public boolean isOverlayScreen(Screen screen) {
		return screen instanceof DoomscrollingScreen;
	}

	@Override
	public void sendKeyPress(RinkuBrowser browser, int key, int scancode, int modifiers, Object rawEvent) {
		Mc26Shim.sendKeyPress(browser, key, scancode, modifiers, rawEvent);
	}

	@Override
	public void sendKeyRelease(RinkuBrowser browser, int key, int scancode, int modifiers, Object rawEvent) {
		Mc26Shim.sendKeyRelease(browser, key, scancode, modifiers, rawEvent);
	}

	@Override
	public Screen createOverlayScreen(Screen parent, ShortsBrowserController controller, LunarHudRenderer hudRenderer) {
		return new DoomscrollingScreen(parent, controller, hudRenderer);
	}

	@Override
	public void registerClientHooks(ShortsBrowserController controller, LunarHudRenderer hudRenderer, Runnable openEditor) {
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(DoomscrollingClient.MOD_ID, "controls")
		);

		KeyMapping keyToggleHud = registerKey("key.doomscrolling.toggle_hud", InputConstants.KEY_BACKSLASH, category);
		KeyMapping keyNextShort = registerKey("key.doomscrolling.next_short", InputConstants.KEY_DOWN, category);
		KeyMapping keyPrevShort = registerKey("key.doomscrolling.prev_short", InputConstants.KEY_UP, category);
		KeyMapping keyPlayPause = registerKey("key.doomscrolling.play_pause", InputConstants.KEY_RIGHT, category);
		KeyMapping keyLikeShort = registerKey("key.doomscrolling.like_short", InputConstants.KEY_LEFT, category);
		KeyMapping keyToggleAutoScroll = registerKey("key.doomscrolling.toggle_autoscroll", InputConstants.KEY_APOSTROPHE, category);
		KeyMapping keyToggleMute = registerKey("key.doomscrolling.toggle_mute", InputConstants.KEY_SEMICOLON, category);
		KeyMapping keyOpenOverlay = registerKey("key.doomscrolling.open_overlay", InputConstants.KEY_RBRACKET, category);
		KeyMapping keyCycleLayout = registerKey("key.doomscrolling.cycle_layout", InputConstants.KEY_LBRACKET, category);

		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(DoomscrollingClient.MOD_ID, "shorts_hud"),
				(guiGraphics, deltaTracker) -> hudRenderer.renderHud(new Mc26Canvas(guiGraphics))
		);

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof ChatScreen)) {
				return;
			}
			hudRenderer.resetInteraction();

			ScreenEvents.afterExtract(screen).register((scr, guiGraphics, mouseX, mouseY, tickDelta) -> {
				if (controller.config().hudVisible) {
					hudRenderer.renderCard(
							new Mc26Canvas(guiGraphics),
							client.font,
							guiGraphics.guiWidth(),
							guiGraphics.guiHeight(),
							mouseX,
							mouseY,
							true,
							false,
							false
					);
				}
			});

			ScreenMouseEvents.allowMouseClick(screen).register((scr, event) ->
					!hudRenderer.handleMouseClick(event.x(), event.y(), event.button(), scr.width, scr.height, true, false)
			);

			ScreenMouseEvents.allowMouseDrag(screen).register((scr, event, dragX, dragY) ->
					!hudRenderer.handleMouseDrag(event.x(), event.y(), scr.width, scr.height)
			);

			ScreenMouseEvents.allowMouseRelease(screen).register((scr, event) ->
					!hudRenderer.handleMouseRelease()
			);

			ScreenMouseEvents.allowMouseScroll(screen).register((scr, mouseX, mouseY, horizontalAmount, verticalAmount) ->
					!hudRenderer.handleMouseScroll(mouseX, mouseY, verticalAmount, scr.width, scr.height)
			);

			ScreenEvents.remove(screen).register(scr -> hudRenderer.resetInteraction());
		});

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			Screen current = currentScreen(minecraft);
			controller.tick(minecraft, isOverlayScreen(current));
			if (current != null) {
				return;
			}
			while (keyToggleHud.consumeClick()) controller.toggleHudVisible();
			while (keyNextShort.consumeClick()) controller.nextShort();
			while (keyPrevShort.consumeClick()) controller.previousShort();
			while (keyPlayPause.consumeClick()) controller.togglePlayPause();
			while (keyLikeShort.consumeClick()) controller.toggleLike();
			while (keyToggleAutoScroll.consumeClick()) controller.toggleAutoScroll();
			while (keyToggleMute.consumeClick()) controller.toggleMute();
			while (keyCycleLayout.consumeClick()) controller.cycleLayoutMode();
			while (keyOpenOverlay.consumeClick()) openEditor.run();
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> controller.shutdown());
	}

	private static KeyMapping registerKey(String translationKey, int keyCode, KeyMapping.Category category) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(translationKey, keyCode, category));
	}
}
