package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.IconButton;
import com.suntide_20210418.advancedmemorycard.item.ModItems;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOption;
import com.suntide_20210418.advancedmemorycard.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * 复制模式选项界面。
 *
 * <p>竖直排列 1 个标题文字（由样式 JSON 的 {@code copy_options_title} 直接渲染）和 8 个
 * {@link AECheckbox}，每项控制粘贴时是否应用对应的导出组件；左上方一个
 * {@link AutoOpenButton} 切换「每次复制后是否自动弹出本界面」。
 *
 * <p>复选框与标题的布局由 {@code copy_mode_menu.json} 定义，AE2 的 {@code WidgetContainer}
 * 会按槽位位置放置控件并在 resize 时重新计算；左侧按钮则是手动摆位（见 {@link #init()}）：
 * <pre>
 *   按钮 16x16：(leftPos + 4, topPos + 3)
 *   标题 left = 30，top = 10，高 9（font.lineHeight）
 *   复选框 left = 30，top = 24 / 43 / 62 / 81 / 100 / 119 / 138 / 157
 *             高 14（AECheckbox.SIZE），步进 = 14 + 5
 * </pre>
 *
 * <p>状态以服务端为准（{@link CopyModeMenu#getApplyMask()} / {@link CopyModeMenu#isAutoOpen()}），
 * 客户端点击只负责提交新值；服务端回读后的同步值才是最终显示状态。
 */
public class CopyModeScreen extends AEBaseScreen<CopyModeMenu> {

    /** 按钮相对 GUI 左上角的位置。 */
    private static final int BUTTON_X = 4;
    private static final int BUTTON_Y = 3;

    private final Map<CopyOption, AECheckbox> checkboxes = new EnumMap<>(CopyOption.class);

    /** 上一次已应用到界面上的同步值，用于避免每 tick 重设导致点击瞬间被旧值覆盖。 */
    private int lastSyncedMask = -1;

    private AutoOpenButton autoOpenButton;

    public CopyModeScreen(CopyModeMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        // 必须在构造期注册：AEBaseScreen.init() 会统一把这些控件放进 renderables 并按 JSON 槽位摆位
        for (CopyOption option : CopyOption.values()) {
            AECheckbox checkbox = this.widgets.addCheckbox(
                    widgetId(option),
                    Component.translatable(option.translationKey()),
                    this::pushMask);
            // 未配置过的卡默认全不选；同步值到达后会立刻校正
            checkbox.setSelected(false);
            checkboxes.put(option, checkbox);
        }
    }

    /**
     * 不用 AE2 的左侧竖排工具栏（{@code VerticalButtonBar}）：它的背景条自带边框与底部阴影，
     * 只有一个按钮时那条阴影会横在按钮下方的空白处，和面板背景对不上。
     * 这里改成手动摆位，界面里只留按钮自己的背景。
     */
    @Override
    protected boolean shouldAddToolbar() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        if (autoOpenButton == null) {
            autoOpenButton = new AutoOpenButton(menu, this::toggleAutoOpen);
        }
        autoOpenButton.setX(leftPos + BUTTON_X);
        autoOpenButton.setY(topPos + BUTTON_Y);
        // rebuildWidgets() 会先清空 renderables，所以 resize 后重新加入不会重复
        addRenderableWidget(autoOpenButton);
    }

    private static String widgetId(CopyOption option) {
        return "opt_" + option.name().toLowerCase(Locale.ROOT);
    }

    /** 提交整张掩码而不是 delta：天然幂等，重复包/丢包都不会把状态写歪。 */
    private void pushMask() {
        menu.dispatchSetApplyMask(CopyOption.maskOf(o -> checkboxes.get(o).isSelected()));
    }

    private void toggleAutoOpen() {
        menu.dispatchSetAutoOpen(!menu.isAutoOpen());
    }

    @Override
    public void containerTick() {
        super.containerTick();

        int mask = menu.getApplyMask();
        if (mask != lastSyncedMask) {
            lastSyncedMask = mask;
            for (Map.Entry<CopyOption, AECheckbox> entry : checkboxes.entrySet()) {
                entry.getValue().setSelected((mask & entry.getKey().mask()) != 0);
            }
        }

        // 卡片被换走或切到别的模式后，这个界面已经没有写入目标，直接关掉
        if (!isHandStillCopyMode()) {
            onClose();
        }
    }

    private boolean isHandStillCopyMode() {
        InteractionHand hand = menu.getHand();
        if (hand == null) {
            // 同步值还没到，未知即视为有效，避免刚打开就被关掉
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        ItemStack stack = minecraft.player.getItemInHand(hand);
        return stack.is(ModItems.ADVANCED_MEMORY_CARD.get())
                && CardMode.of(stack) instanceof CopyMode;
    }

    /**
     * 左侧「复制后自动弹界面」开关。
     *
     * <p>图标直接由同步值决定（每帧取一次），因此不需要额外维护按钮状态：
     * 服务端写入成功后同步回客户端，下一帧图标就变了。
     */
    private static final class AutoOpenButton extends IconButton {
        private final CopyModeMenu menu;

        AutoOpenButton(CopyModeMenu menu, Runnable onPress) {
            super(btn -> onPress.run());
            this.menu = menu;
            setMessage(Component.translatable(TranslateHelper.Keys.COPY_MODE_SCREEN_AUTO_OPEN));
        }

        @Override
        protected Icon getIcon() {
            return menu.isAutoOpen() ? Icon.COG : Icon.COG_DISABLED;
        }
    }
}
