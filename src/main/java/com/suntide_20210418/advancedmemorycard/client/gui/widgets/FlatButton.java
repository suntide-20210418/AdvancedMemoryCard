package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import net.minecraft.client.gui.FontRenderer;

import com.suntide_20210418.advancedmemorycard.config.ModConfigs;

/**
 * 扁平风格按钮控件（1.7.10 vanilla 实现）。
 * 扁平色块 + 文字，悬停时高亮，颜色从客户端配置读取。
 */
public class FlatButton {

    private int x;
    private int y;
    private int width;
    private int height;
    private String label;
    private final Runnable onClickAction;

    private boolean hovered = false;
    private boolean pressed = false;
    public boolean visible = true;
    public boolean active = true;

    public FlatButton(int x, int y, int width, int height, String label, Runnable onClick) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.onClickAction = onClick;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void draw(FontRenderer font, int mouseX, int mouseY) {
        if (!visible) return;
        if (!pressed) {
            hovered = isMouseOver(mouseX, mouseY);
        }
        int bg = pressed ? ModConfigs.getClientConfig().buttonColorClick
            : (hovered ? ModConfigs.getClientConfig().buttonColorHover : ModConfigs.getClientConfig().buttonColorBg);
        net.minecraft.client.gui.Gui.drawRect(x, y, x + width, y + height, bg);

        int textWidth = font.getStringWidth(label);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        font.drawString(label, textX, textY, ModConfigs.getClientConfig().buttonColorText);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (active && visible && button == 0 && isMouseOver(mouseX, mouseY)) {
            com.suntide_20210418.advancedmemorycard.client.gui.ClickSound.play();
            pressed = true;
            return true;
        }
        return false;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        // 只认左键：1.7.10 的 mouseMovedOrUp 会用 -1 表示“纯移动”，
        // 右键（1）也不该把按下态的左键按钮当成释放。
        if (button != 0) {
            return false;
        }
        if (pressed) {
            pressed = false;
            if (isMouseOver(mouseX, mouseY)) {
                onClick(mouseX, mouseY);
            }
            return true;
        }
        return false;
    }

    public void onClick(int mouseX, int mouseY) {
        if (onClickAction != null) {
            onClickAction.run();
        }
    }
}
