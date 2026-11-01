package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 扁平按钮。
 *
 * <p>只有三种语义，对应三种视觉重量：
 * <ul>
 *   <li>{@link Variant#PRIMARY} —— 整个界面上唯一的主操作（绑定）</li>
 *   <li>{@link Variant#SECONDARY} —— 检查器里的次级动作</li>
 *   <li>{@link Variant#GHOST} —— 行内小动作，无底色，靠 hover 提示</li>
 * </ul>
 * 以前所有按钮都是同一个灰块 + 白字，主次完全没有区分度。
 */
public class FlatButton extends AbstractWidget {

    public enum Variant { PRIMARY, SECONDARY, GHOST }

    private final Runnable onClickAction;
    private final Variant variant;
    private Component label;
    private boolean pressed = false;
    private boolean hovered = false;
    /** 开关型按钮的「已开启」状态（例如底部操作条的「批量操作」）。 */
    private boolean selected = false;

    public FlatButton(int x, int y, int width, int height, Component label,
                      Variant variant, Runnable onClick) {
        super(x, y, width, height, label);
        this.label = label;
        this.variant = variant;
        this.onClickAction = onClick;
    }

    public FlatButton(int x, int y, int width, int height, Component label, Runnable onClick) {
        this(x, y, width, height, label, Variant.SECONDARY, onClick);
    }

    public static int colorSurface() {
        return ModConfigs.getClientConfig().paletteSurface.get();
    }

    public static int colorSurfaceHover() {
        return ModConfigs.getClientConfig().paletteSurfaceHover.get();
    }

    public static int colorAccent() {
        return ModConfigs.getClientConfig().paletteAccent.get();
    }

    public static int colorAccentText() {
        return ModConfigs.getClientConfig().paletteAccentText.get();
    }

    public static int colorText() {
        return ModConfigs.getClientConfig().paletteTextPrimary.get();
    }

    public static int colorTextTertiary() {
        return ModConfigs.getClientConfig().paletteTextTertiary.get();
    }

    /** 把 a 向 b 线性混合，t ∈ [0,1]。用来从语义色推导 hover / pressed，不额外增加配置项。 */
    public static int mix(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = Math.round(aa + (ba - aa) * t);
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;

        boolean canClick = this.active;
        if (!pressed) {
            hovered = canClick && isMouseOver(mouseX, mouseY);
        }

        int bg;
        int fg;
        switch (variant) {
            case PRIMARY -> {
                if (!canClick) {
                    // 不可用时仍然保留一点“这是个按钮”的形，但明显退到背景里
                    bg = mix(colorSurface(), colorAccent(), 0.25f);
                    fg = colorTextTertiary();
                } else if (pressed) {
                    bg = mix(colorAccent(), 0xFF000000, 0.22f);
                    fg = colorAccentText();
                } else if (hovered) {
                    bg = mix(colorAccent(), 0xFFFFFFFF, 0.14f);
                    fg = colorAccentText();
                } else {
                    bg = colorAccent();
                    fg = colorAccentText();
                }
            }
            case GHOST -> {
                bg = pressed ? colorSurface() : (hovered ? colorSurfaceHover() : 0x00000000);
                fg = canClick ? colorText() : colorTextTertiary();
            }
            default -> {
                bg = hovered ? colorSurfaceHover() : colorSurface();
                if (selected && canClick) {
                    // 开关态用底色承载，不占用 PRIMARY —— PRIMARY 只留给「绑定」这个主操作
                    bg = mix(colorSurface(), colorAccent(), 0.42f);
                    if (hovered) bg = mix(bg, 0xFFFFFFFF, 0.10f);
                }
                if (pressed) bg = mix(bg, 0xFF000000, 0.12f);
                fg = canClick ? colorText() : colorTextTertiary();
            }
        }

        if ((bg >>> 24) != 0) {
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        }

        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(label);
        int textX = getX() + (width - textWidth) / 2;
        int textY = getY() + (height - 8) / 2;
        g.drawString(font, label, textX, textY, fg, false);
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

    public void setLabel(Component label) {
        this.label = label;
    }

    /** 开关型按钮的状态：开启时底色偏强调色，读作「现在正处在这个模式里」。 */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public Component getLabel() {
        return label;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationOutput) {
        this.defaultButtonNarrationText(narrationOutput);
    }
}
