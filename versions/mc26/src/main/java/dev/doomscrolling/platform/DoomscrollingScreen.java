package dev.doomscrolling.platform;

import dev.doomscrolling.browser.ShortsBrowserController;
import dev.doomscrolling.ui.LunarHudRenderer;
import dev.doomscrolling.ui.OverlayScreenLogic;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

final class DoomscrollingScreen extends Screen {

	private final OverlayScreenLogic logic;

	DoomscrollingScreen(Screen parent, ShortsBrowserController controller, LunarHudRenderer hudRenderer) {
		super(Component.translatable("screen.doomscrolling.title"));
		this.logic = new OverlayScreenLogic(parent, controller, hudRenderer);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		super.init();
		clearWidgets();
		logic.init(minecraft, font, width, height);
	}

	@Override
	public void resize(int w, int h) {
		super.resize(w, h);
		init();
	}

	@Override
	public void tick() {
		super.tick();
		logic.tick();
	}

	@Override
	public void onClose() {
		logic.onClose();
	}

	@Override
	public void extractBackground(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		logic.renderBackground(new Mc26Canvas(g));
	}

	@Override
	public void extractRenderState(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		logic.render(new Mc26Canvas(g), mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
		if (super.mouseClicked(event, isDoubleClick)) return true;
		return logic.mouseClicked(event.x(), event.y(), event.button());
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (logic.mouseReleased(event.x(), event.y(), event.button())) return true;
		return super.mouseReleased(event);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		logic.mouseMoved(mouseX, mouseY);
		super.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (logic.mouseDragged(event.x(), event.y())) return true;
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (logic.mouseScrolled(mouseX, mouseY, scrollY)) return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (logic.keyPressed(event.key(), Mc26Shim.scancode(event), event.modifiers(), event)) return true;
		return super.keyPressed(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (logic.keyReleased(event.key(), Mc26Shim.scancode(event), event.modifiers(), event)) return true;
		return super.keyReleased(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (logic.charTyped((char) event.codepoint())) return true;
		return super.charTyped(event);
	}
}
