package dev.doomscrolling.platform;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import de.keksuccino.rinku.RinkuBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

final class Mc26Shim {

	private static CursorType cursorNwse;
	private static CursorType cursorNesw;

	private Mc26Shim() {
	}

	static Screen currentScreen(Minecraft mc) {
		return mc.gui.screen();
	}

	static void setScreen(Minecraft mc, Screen screen) {
		mc.gui.setScreen(screen);
	}

	static int scancode(KeyEvent event) {
		return event.scancode();
	}

	static void sendKeyPress(RinkuBrowser browser, int key, int scancode, int modifiers, Object rawEvent) {
		browser.sendKeyPress(key, scancode, modifiers);
	}

	static void sendKeyRelease(RinkuBrowser browser, int key, int scancode, int modifiers, Object rawEvent) {
		browser.sendKeyRelease(key, scancode, modifiers);
	}

	static CursorType resizeNwseCursor() {
		if (cursorNwse == null) {
			cursorNwse = CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR, "doomscrolling_nwse", CursorTypes.RESIZE_ALL);
		}
		return cursorNwse;
	}

	static CursorType resizeNeswCursor() {
		if (cursorNesw == null) {
			cursorNesw = CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR, "doomscrolling_nesw", CursorTypes.RESIZE_ALL);
		}
		return cursorNesw;
	}
}
