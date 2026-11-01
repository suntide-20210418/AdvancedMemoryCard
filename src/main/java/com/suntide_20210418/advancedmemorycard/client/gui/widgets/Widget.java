package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

/**
 * 自绘控件基类（1.7.10 / 1.12.2 共用语义）。
 *
 * <p>
 * 基准 1.21.1 的控件继承 AE2 的 {@code AbstractWidget}（由 WidgetContainer 定位、分发
 * 输入事件）。旧版没有那套机制，这里用最小的基类承担同样的角色：
 * 矩形、可见性、命中判定，以及四个输入回调。返回值语义与基准一致 ——
 * 返回 true 表示「事件已消费」，调用方不要再往下派发。
 */
public abstract class Widget {

    public int x;
    public int y;
    public int width;
    public int height;
    public boolean visible = true;

    protected Widget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** 每帧绘制。坐标是屏幕绝对坐标（界面在渲染前已 setPosition）。 */
    public abstract void render(double mouseX, double mouseY, float partialTick);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /** 拖动事件由 Screen 全局转发（指针可能移出控件矩形）。 */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    /** 滚轮；返回 true 表示已消费。 */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }
}
