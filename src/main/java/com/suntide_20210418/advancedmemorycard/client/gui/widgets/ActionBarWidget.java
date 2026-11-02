package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import com.suntide_20210418.advancedmemorycard.client.gui.GuiDraw;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;

/**
 * 底部操作条：左侧是当前该做什么的一句话提示，右侧是全窗口唯一的主操作。
 *
 * <p>1.7.10 移植：基准 1.21.1 / 1.20.1 用 {@code AbstractWidget} + 自带 {@code FlatButton}
 * （Variant / setSelected）。本仓 1.7.10 的 {@link FlatButton} 更简单 —— 用 {@code active}
 * 控制可点性、用 {@code setLabel} 切换文字、用 {@code draw(FontRenderer, mx, my)} 自绘。
 * 这里直接复用它，批量模式的“已激活”视觉用按钮文字切换表达。
 */
public class ActionBarWidget extends Widget {

    private static final int BUTTON_W = 76;
    private static final int BUTTON_H = 14;
    private static final int RIGHT_PAD = 4;
    /** 两个按钮之间的间距 */
    private static final int BTN_GAP = 6;

    private final FlatButton bindButton;
    private final FlatButton batchButton;
    private Runnable onBind;
    private Runnable onBatchToggle;
    private String hint = TranslateHelper.ActionBar.hint();

    public ActionBarWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.bindButton = new FlatButton(0, 0, BUTTON_W, BUTTON_H,
                TranslateHelper.ActionBar.bind(), () -> {
                    if (onBind != null) onBind.run();
                });
        bindButton.active = false;
        // 批量操作是开关，不是一次性的动作：进模式后文字改成「退出批量操作」，
        // 一直亮着提示玩家当前处在框选模式里（此时单击就等于 Ctrl 单击）
        this.batchButton = new FlatButton(0, 0, BUTTON_W, BUTTON_H,
                TranslateHelper.ActionBar.batch(), () -> {
                    if (onBatchToggle != null) onBatchToggle.run();
                });
    }

    public void setOnBind(Runnable onBind) {
        this.onBind = onBind;
    }

    public void setOnBatchToggle(Runnable onBatchToggle) {
        this.onBatchToggle = onBatchToggle;
    }

    /** 同步批量模式的开关状态到按钮的文字（1.7.10 的 FlatButton 无 selected 视觉，用文字表达）。 */
    public void setBatchActive(boolean active) {
        batchButton.setLabel(active ? TranslateHelper.ActionBar.exitBatch()
                : TranslateHelper.ActionBar.batch());
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

    /** 按当前 action bar 的 x/y/width 把两个按钮摆到右端。render 每帧调用，便宜。 */
    private void layout() {
        int bindX = x + width - RIGHT_PAD - BUTTON_W;
        int by = y + (height - BUTTON_H) / 2;
        bindButton.setX(bindX);
        bindButton.setY(by);
        batchButton.setX(bindX - BTN_GAP - BUTTON_W);
        batchButton.setY(by);
    }

    @Override
    public void render(double mouseX, double mouseY, float partialTick) {
        if (!visible) return;
        layout();

        int secondary = ModConfigs.getClientConfig().paletteTextSecondary;
        int textX = x + 4;
        int textY = y + (height - 8) / 2;
        // 提示文字不能盖到两个按钮上
        int budget = 2 * BUTTON_W + BTN_GAP + RIGHT_PAD + 8;
        String text = GuiDraw.font().trimStringToWidth(hint, Math.max(0, width - budget));
        GuiDraw.drawString(text, textX, textY, secondary);

        batchButton.draw(GuiDraw.font(), (int) mouseX, (int) mouseY);
        bindButton.draw(GuiDraw.font(), (int) mouseX, (int) mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        // 先问批量按钮：它常亮，而绑定按钮在配对不齐时是灰的
        if (batchButton.mouseClicked((int) mouseX, (int) mouseY, button)) return true;
        return bindButton.mouseClicked((int) mouseX, (int) mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        if (batchButton.mouseReleased((int) mouseX, (int) mouseY, button)) return true;
        return bindButton.mouseReleased((int) mouseX, (int) mouseY, button);
    }
}
