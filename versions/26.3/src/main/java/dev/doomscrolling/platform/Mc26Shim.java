package dev.doomscrolling.platform;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.keksuccino.rinku.RinkuBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

final class Mc26Shim {

	private Mc26Shim() {
	}

	static Screen currentScreen(Minecraft mc) {
		return mc.gui.screen();
	}

	static void setScreen(Minecraft mc, Screen screen) {
		mc.gui.setScreen(screen);
	}

	static int scancode(KeyEvent event) {
		return event.keycode();
	}

	static void sendKeyPress(RinkuBrowser browser, int key, int scancode, int modifiers, Object rawEvent) {
		if (rawEvent instanceof KeyEvent event) {
			browser.sendKeyPress(event);
		} else {
			browser.sendKeyPress(key, scancode, modifiers);
		}
	}

	static void sendKeyRelease(RinkuBrowser browser, int key, int scancode, int modifiers, Object rawEvent) {
		if (rawEvent instanceof KeyEvent event) {
			browser.sendKeyRelease(event);
		} else {
			browser.sendKeyRelease(key, scancode, modifiers);
		}
	}

	static CursorType resizeNwseCursor() {
		return CursorTypes.RESIZE_ALL;
	}

	static CursorType resizeNeswCursor() {
		return CursorTypes.RESIZE_ALL;
	}
}
