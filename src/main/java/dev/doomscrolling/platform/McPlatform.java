package dev.doomscrolling.platform;

import de.keksuccino.rinku.RinkuBrowser;
import dev.doomscrolling.browser.ShortsBrowserController;
import dev.doomscrolling.ui.LunarHudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public interface McPlatform {

	static McPlatform get() {
		return PlatformImpl.INSTANCE;
	}

	Object registerArgbTexture(String name, int width, int height, int[] argbPixels);

	Object getBrowserTexture(RinkuBrowser browser);

	Screen currentScreen(Minecraft mc);

	void setScreen(Minecraft mc, Screen screen);

	boolean isOverlayScreen(Screen screen);

	void sendKeyPress(RinkuBrowser browser, int key, int scancode, int modifiers, Object rawEvent);

	void sendKeyRelease(RinkuBrowser browser, int key, int scancode, int modifiers, Object rawEvent);

	Screen createOverlayScreen(Screen parent, ShortsBrowserController controller, LunarHudRenderer hudRenderer);

	void registerClientHooks(ShortsBrowserController controller, LunarHudRenderer hudRenderer, Runnable openEditor);
}
