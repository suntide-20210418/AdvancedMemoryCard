package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 底部操作条：左侧是当前该做什么的一句话提示，右侧是全窗口唯一的主操作。
 *
 * <p>旧版的六个按钮随节点类型换位置换含义，这里把“执行”收敛到一个固定像素位，
 * 玩家不需要再记“现在第 4 个格子是什么”。
 */
public class ActionBarWidget extends AbstractWidget {

    private static final int BUTTON_W = 76;
    private static final int BUTTON_H = 14;
    private static final int RIGHT_PAD = 4;
    /** 两个按钮之间的间距 */
    private static final int BTN_GAP = 6;

    private final FlatButton bindButton;
    private final FlatButton batchButton;
    private Runnable onBind;
    private Runnable onBatchToggle;
    private Component hint = TranslateHelper.ActionBar.hint();

    public ActionBarWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.bindButton = new FlatButton(0, 0, BUTTON_W, BUTTON_H,
                TranslateHelper.ActionBar.bind(), FlatButton.Variant.PRIMARY,
                () -> {
                    if (onBind != null) onBind.run();
                });
        bindButton.active = false;
        // 批量操作是开关，不是一次性的动作：进模式后文字改成「退出批量操作」，
        // 一直亮着提示玩家当前处在框选模式里（此时单击就等于 Ctrl 单击）
        this.batchButton = new FlatButton(0, 0, BUTTON_W, BUTTON_H,
                TranslateHelper.ActionBar.batch(), FlatButton.Variant.SECONDARY,
                () -> {
                    if (onBatchToggle != null) onBatchToggle.run();
                });
        positionButton();
    }

    public void setOnBind(Runnable onBind) {
        this.onBind = onBind;
    }

    public void setOnBatchToggle(Runnable onBatchToggle) {
        this.onBatchToggle = onBatchToggle;
    }

    /** 同步批量模式的开关状态到按钮的文字与底色。 */
    public void setBatchActive(boolean active) {
        batchButton.setSelected(active);
        batchButton.setLabel(active ? TranslateHelper.ActionBar.exitBatch()
                : TranslateHelper.ActionBar.batch());
    }

    private void positionButton() {
        int bindX = getX() + width - RIGHT_PAD - BUTTON_W;
        int y = getY() + (height - BUTTON_H) / 2;
        bindButton.setX(bindX);
        bindButton.setY(y);
        batchButton.setX(bindX - BTN_GAP - BUTTON_W);
        batchButton.setY(y);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        positionButton();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        positionButton();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        positionButton();
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        positionButton();
    }

    /** 配对是否齐备：齐备时主按钮才亮起，提示文案同步改成“将 A 绑到 B”。 */
    public void setReady(boolean ready, String sourceName, String frequency, int sourceCount) {
        bindButton.active = ready;
        if (!ready) {
            hint = TranslateHelper.ActionBar.hint();
            return;
        }
        if (sourceCount > 1) {
            hint = TranslateHelper.ActionBar.hintReadyMulti(
                    String.valueOf(sourceCount),
                    frequency == null ? "" : frequency);
        } else {
            hint = TranslateHelper.ActionBar.hintReady(
                    sourceName == null ? "" : sourceName,
                    frequency == null ? "" : frequency);
        }
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        Font font = Minecraft.getInstance().font;
        int secondary = com.suntide_20210418.advancedmemorycard.config.ModConfigs
                .getClientConfig().paletteTextSecondary.get();
        int textX = getX() + 4;
        int textY = getY() + (height - 8) / 2;
        // 提示文字不能盖到两个按钮上
        int budget = 2 * BUTTON_W + BTN_GAP + RIGHT_PAD + 8;
        String text = font.plainSubstrByWidth(hint.getString(), width - budget);
        g.drawString(font, text, textX, textY, secondary, false);

        batchButton.render(g, mouseX, mouseY, partialTick);
        bindButton.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;
        // 先问批量按钮：它常亮，而绑定按钮在配对不齐时是灰的
        if (batchButton.mouseClicked(mouseX, mouseY, button)) return true;
        return bindButton.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible || !active) return false;
        if (batchButton.mouseReleased(mouseX, mouseY, button)) return true;
        return bindButton.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        batchButton.mouseMoved(mouseX, mouseY);
        bindButton.mouseMoved(mouseX, mouseY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationOutput) {
        this.defaultButtonNarrationText(narrationOutput);
    }
}
