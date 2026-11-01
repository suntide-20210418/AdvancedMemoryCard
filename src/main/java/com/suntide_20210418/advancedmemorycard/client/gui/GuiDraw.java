package com.suntide_20210418.advancedmemorycard.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.opengl.GL11;

/**
 * 1.7.10 的绘制兼容层。
 *
 * <p>
 * 1.20.1 / 1.21.1 的控件写的是 {@code GuiGraphics}（drawString / fill / scissor），
 * 旧版没有这套 API。与其把每个控件重写一遍 GL 调用，不如在这里提供一个语义相同的
 * 小接口，让控件代码维持「贴着基准写」的形态 —— 移植与比对成本都低得多。
 *
 * <p>
 * 裁剪坐标要翻转：GL 的 scissor 以左下角为原点，屏幕坐标以左上角为原点。
 */
public final class GuiDraw {

    private GuiDraw() {}

    public static FontRenderer font() {
        return Minecraft.getMinecraft().fontRenderer;
    }

    public static void drawString(String text, int x, int y, int color) {
        Minecraft.getMinecraft().fontRenderer.drawString(text, x, y, color);
    }

    public static void drawStringWithShadow(String text, int x, int y, int color) {
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, x, y, color);
    }

    public static void drawCenteredString(String text, int x, int y, int color) {
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, x - font().getStringWidth(text) / 2, y, color);
    }

    public static int width(String text) {
        return text == null ? 0 : Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
    }

    /** 省略号截断：超宽就截到能放下「…」的宽度，再补一个省略号。 */
    public static String trim(String text, int maxWidth) {
        if (text == null || width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        FontRenderer fr = font();
        String cut = fr.trimStringToWidth(text, Math.max(0, maxWidth - fr.getStringWidth(ellipsis)));
        return cut + ellipsis;
    }

    public static void fill(int x1, int y1, int x2, int y2, int color) {
        Gui.drawRect(x1, y1, x2, y2, color);
    }

    public static void fillRect(int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        Gui.drawRect(x, y, x + w, y + h, color);
    }

    public static void enableScissor(int x, int y, int w, int h) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        double scaleW = (double) mc.displayWidth / sr.getScaledWidth_double();
        double scaleH = (double) mc.displayHeight / sr.getScaledHeight_double();
        int glX = (int) Math.round(x * scaleW);
        int glY = (int) Math.round((sr.getScaledHeight_double() - y - h) * scaleH);
        int glW = Math.max(0, (int) Math.round(w * scaleW));
        int glH = Math.max(0, (int) Math.round(h * scaleH));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(glX, glY, glW, glH);
    }

    public static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
