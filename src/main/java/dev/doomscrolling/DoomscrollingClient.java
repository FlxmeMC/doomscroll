package dev.doomscrolling;

import com.mojang.blaze3d.platform.InputConstants;
import dev.doomscrolling.browser.ShortsBrowserController;
import dev.doomscrolling.config.DoomscrollingConfig;
import dev.doomscrolling.ui.DoomscrollingScreen;
import dev.doomscrolling.ui.LunarHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

public final class DoomscrollingClient implements ClientModInitializer {

	public static final String MOD_ID = "doomscrolling";

	private static ShortsBrowserController controller;
	private static LunarHudRenderer hudRenderer;

	private KeyMapping keyToggleHud;
	private KeyMapping keyNextShort;
	private KeyMapping keyPrevShort;
	private KeyMapping keyPlayPause;
	private KeyMapping keyLikeShort;
	private KeyMapping keyToggleAutoScroll;
	private KeyMapping keyToggleMute;
	private KeyMapping keyOpenOverlay;
	private KeyMapping keyCycleLayout;

	@Override
	public void onInitializeClient() {
		Path configPath = FabricLoader.getInstance().getConfigDir().resolve("doomscrolling.json");
		DoomscrollingConfig config = DoomscrollingConfig.load(configPath);
		controller = new ShortsBrowserController(config);
		hudRenderer = new LunarHudRenderer(controller);

		registerKeybinds();
		registerHud();
		registerChatInteraction();
		registerLifecycleHooks();
	}

	private void registerKeybinds() {
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "controls")
		);

		keyToggleHud = registerKey("key.doomscrolling.toggle_hud", GLFW.GLFW_KEY_BACKSLASH, category);
		keyNextShort = registerKey("key.doomscrolling.next_short", GLFW.GLFW_KEY_DOWN, category);
		keyPrevShort = registerKey("key.doomscrolling.prev_short", GLFW.GLFW_KEY_UP, category);
		keyPlayPause = registerKey("key.doomscrolling.play_pause", GLFW.GLFW_KEY_RIGHT, category);
		keyLikeShort = registerKey("key.doomscrolling.like_short", GLFW.GLFW_KEY_LEFT, category);
		keyToggleAutoScroll = registerKey("key.doomscrolling.toggle_autoscroll", GLFW.GLFW_KEY_APOSTROPHE, category);
		keyToggleMute = registerKey("key.doomscrolling.toggle_mute", GLFW.GLFW_KEY_SEMICOLON, category);
		keyOpenOverlay = registerKey("key.doomscrolling.open_overlay", GLFW.GLFW_KEY_RIGHT_BRACKET, category);
		keyCycleLayout = registerKey("key.doomscrolling.cycle_layout", GLFW.GLFW_KEY_LEFT_BRACKET, category);
	}

	private static KeyMapping registerKey(String translationKey, int glfwKey, KeyMapping.Category category) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(
				translationKey,
				InputConstants.Type.KEYSYM,
				glfwKey,
				category
		));
	}

	private void registerHud() {
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(MOD_ID, "shorts_hud"),
				hudRenderer::renderHud
		);
	}

	private void registerChatInteraction() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof ChatScreen)) {
				return;
			}
			hudRenderer.resetInteraction();

			ScreenEvents.afterExtract(screen).register((scr, guiGraphics, mouseX, mouseY, tickDelta) -> {
				if (controller != null && controller.config().hudVisible) {
					hudRenderer.renderCard(
							guiGraphics,
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
	}

	private void registerLifecycleHooks() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			if (controller != null) {
				controller.shutdown();
			}
		});
	}

	private void onEndClientTick(Minecraft minecraft) {
		if (controller == null) {
			return;
		}
		boolean overlayOpen = minecraft.gui.screen() instanceof DoomscrollingScreen;
		controller.tick(minecraft, overlayOpen);

		if (minecraft.gui.screen() != null) {
			return;
		}

		while (keyToggleHud.consumeClick()) {
			controller.toggleHudVisible();
		}
		while (keyNextShort.consumeClick()) {
			controller.nextShort();
		}
		while (keyPrevShort.consumeClick()) {
			controller.previousShort();
		}
		while (keyPlayPause.consumeClick()) {
			controller.togglePlayPause();
		}
		while (keyLikeShort.consumeClick()) {
			controller.toggleLike();
		}
		while (keyToggleAutoScroll.consumeClick()) {
			controller.toggleAutoScroll();
		}
		while (keyToggleMute.consumeClick()) {
			controller.toggleMute();
		}
		while (keyCycleLayout.consumeClick()) {
			controller.cycleLayoutMode();
		}
		while (keyOpenOverlay.consumeClick()) {
			minecraft.gui.setScreen(createScreen(null));
		}
	}

	public static Screen createScreen(Screen parent) {
		return new DoomscrollingScreen(parent, controller, hudRenderer);
	}
}
