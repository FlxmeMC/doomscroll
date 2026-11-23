package dev.doomscrolling.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

final class Mc1211Canvas implements UiCanvas {

	private static long cursorEw;
	private static long cursorNs;
	private static long cursorNwse;
	private static long cursorNesw;
	private static long cursorMove;
	private static CursorKind appliedCursor = CursorKind.DEFAULT;

	private final GuiGraphics g;

	Mc1211Canvas(GuiGraphics g) {
		this.g = g;
	}

	@Override
	public int guiWidth() {
		return g.guiWidth();
	}

	@Override
	public int guiHeight() {
		return g.guiHeight();
	}

	@Override
	public void fill(int x1, int y1, int x2, int y2, int argb) {
		g.fill(x1, y1, x2, y2, argb);
	}

	@Override
	public void drawText(Font font, String text, int x, int y, int argb, boolean shadow) {
		g.drawString(font, text, x, y, argb, shadow);
	}

	@Override
	public void enableScissor(int x1, int y1, int x2, int y2) {
		g.enableScissor(x1, y1, x2, y2);
	}

	@Override
	public void disableScissor() {
		g.disableScissor();
	}

	@Override
	public void blitTexture(Object textureId, int x, int y, int w, int h) {
		if (textureId instanceof ResourceLocation id) {
			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			g.blit(id, x, y, 0.0f, 0.0f, w, h, w, h);
		}
	}

	@Override
	public void blitCenteredScaled(Object textureId, float cx, float cy, float size, int argbTint) {
		if (!(textureId instanceof ResourceLocation id) || size <= 1.0f) return;
		int base = 32;
		float scale = size / (float) base;
		float a = ((argbTint >>> 24) & 0xFF) / 255.0f;
		float r = ((argbTint >>> 16) & 0xFF) / 255.0f;
		float gr = ((argbTint >>> 8) & 0xFF) / 255.0f;
		float b = (argbTint & 0xFF) / 255.0f;

		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(r, gr, b, a);
		g.pose().pushPose();
		g.pose().translate(cx, cy, 0.0f);
		g.pose().scale(scale, scale, 1.0f);
		g.blit(id, -base / 2, -base / 2, 0.0f, 0.0f, base, base, base, base);
		g.pose().popPose();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	@Override
	public void requestCursor(CursorKind kind) {
		applyCursor(kind);
	}

	static void resetCursor() {
		applyCursor(CursorKind.DEFAULT);
	}

	private static void applyCursor(CursorKind kind) {
		if (kind == appliedCursor) return;
		appliedCursor = kind;
		long window = Minecraft.getInstance().getWindow().getWindow();
		long handle = switch (kind) {
			case EW -> cursorEw != 0L ? cursorEw : (cursorEw = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_EW_CURSOR));
			case NS -> cursorNs != 0L ? cursorNs : (cursorNs = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NS_CURSOR));
			case NWSE -> cursorNwse != 0L ? cursorNwse : (cursorNwse = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR));
			case NESW -> cursorNesw != 0L ? cursorNesw : (cursorNesw = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR));
			case MOVE -> cursorMove != 0L ? cursorMove : (cursorMove = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_ALL_CURSOR));
			case DEFAULT -> 0L;
		};
		GLFW.glfwSetCursor(window, handle);
	}
}
