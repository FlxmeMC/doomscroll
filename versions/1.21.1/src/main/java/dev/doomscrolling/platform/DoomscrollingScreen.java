package dev.doomscrolling.platform;

import dev.doomscrolling.browser.ShortsBrowserController;
import dev.doomscrolling.ui.LunarHudRenderer;
import dev.doomscrolling.ui.OverlayScreenLogic;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
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
	public void tick() {
		super.tick();
		logic.tick();
	}

	@Override
	public void onClose() {
		Mc1211Canvas.resetCursor();
		logic.onClose();
	}

	@Override
	public void removed() {
		Mc1211Canvas.resetCursor();
		super.removed();
	}

	@Override
	public void renderBackground(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		logic.renderBackground(new Mc1211Canvas(g));
	}

	@Override
	public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		logic.render(new Mc1211Canvas(g), mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		return logic.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (logic.mouseReleased(mouseX, mouseY, button)) return true;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		logic.mouseMoved(mouseX, mouseY);
		super.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (logic.mouseDragged(mouseX, mouseY)) return true;
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (logic.mouseScrolled(mouseX, mouseY, scrollY)) return true;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (logic.keyPressed(keyCode, scanCode, modifiers, null)) return true;
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (logic.keyReleased(keyCode, scanCode, modifiers, null)) return true;
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (logic.charTyped(codePoint)) return true;
		return super.charTyped(codePoint, modifiers);
	}
}
