package dev.doomscrolling;

import dev.doomscrolling.browser.ShortsBrowserController;
import dev.doomscrolling.config.DoomscrollingConfig;
import dev.doomscrolling.platform.McPlatform;
import dev.doomscrolling.ui.LunarHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.nio.file.Path;

public final class DoomscrollingClient implements ClientModInitializer {

	public static final String MOD_ID = "doomscrolling";

	private static ShortsBrowserController controller;
	private static LunarHudRenderer hudRenderer;

	@Override
	public void onInitializeClient() {
		Path configPath = FabricLoader.getInstance().getConfigDir().resolve("doomscrolling.json");
		DoomscrollingConfig config = DoomscrollingConfig.load(configPath);
		controller = new ShortsBrowserController(config);
		hudRenderer = new LunarHudRenderer(controller);

		McPlatform.get().registerClientHooks(controller, hudRenderer, () ->
				McPlatform.get().setScreen(Minecraft.getInstance(), createScreen(null))
		);
	}

	public static Screen createScreen(Screen parent) {
		return McPlatform.get().createOverlayScreen(parent, controller, hudRenderer);
	}
}
