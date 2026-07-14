package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 扁平风格按钮控件<br/>
 * 与 P2PTreeWidget 列表项风格一致：扁平色块 + 白色文字，悬停时蓝色高亮。<br/>
 * 供 DetailPanelWidget 和 P2PTreeWidget 共用，保持视觉风格统一。<br/>
 * 颜色从客户端配置读取，可在配置文件中自定义。
 */
public class FlatButton extends AbstractWidget {

    // 颜色从配置动态读取，保留静态常量引用以兼容旧代码
    // 向后兼容的静态常量（通过 getter 访问配置）
    public static final int COLOR_BG = 0xFF7E8299;
    public static final int COLOR_HOVER = 0xFF3A5A8A;
    public static final int COLOR_CLICK = 0xFF2A4A7A;

    /** 按钮默认背景色 */
    public static int getColorBg() { return ModConfigs.getClientConfig().buttonColorBg.get(); }

    /** 按钮/列表行悬停背景色 */
    public static int getColorHover() { return ModConfigs.getClientConfig().buttonColorHover.get(); }

    /** 按钮点击按下时的背景色 */
    public static int getColorClick() { return ModConfigs.getClientConfig().buttonColorClick.get(); }

    /** 按钮/列表行文字颜色 */
    public static int getColorText() { return ModConfigs.getClientConfig().buttonColorText.get(); }
    public static final int COLOR_TEXT = 0xFFFFFFFF;
    private final Runnable onClickAction;
    private Component label;
    private boolean hovered = false;
    private boolean pressed = false;

    /**
     * @param x       按钮 X 坐标
     * @param y       按钮 Y 坐标
     * @param width   按钮宽度
     * @param height  按钮高度
     * @param label   按钮文字
     * @param onClick 点击回调（可为 null）
     */
    public FlatButton(int x, int y, int width, int height, Component label, Runnable onClick) {
        super(x, y, width, height, label);
        this.label = label;
        this.onClickAction = onClick;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 在 render 中直接计算悬停状态（与 P2PTreeWidget 相同的可靠模式），
        // 避免依赖外部 mouseMoved 回调可能不被 AE2 系统路由到嵌套子控件的问题
        if (!pressed) {
            hovered = isMouseOver(mouseX, mouseY);
        }

        // 绘制按钮背景（扁平色块，无边框），优先级：点击 > 悬停 > 默认
        int bgColor = pressed ? getColorClick() : (hovered ? getColorHover() : getColorBg());
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

        // 绘制按钮文字（居中）
        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(label);
        int textX = getX() + (width - textWidth) / 2;
        int textY = getY() + (height - 8) / 2;
        guiGraphics.drawString(font, label, textX, textY, getColorText(), false);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!pressed) {
            hovered = isMouseOver(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && button == 0 && isMouseOver(mouseX, mouseY)) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            pressed = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (pressed) {
            pressed = false;
            if (isMouseOver(mouseX, mouseY)) {
                onClick(mouseX, mouseY);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (onClickAction != null) {
            onClickAction.run();
        }
    }

    /**
     * 动态修改按钮文字
     */
    public void setLabel(Component label) {
        this.label = label;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationOutput) {
        this.defaultButtonNarrationText(narrationOutput);
    }
}
