package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AECheckbox;
import com.suntide_20210418.advancedmemorycard.item.ModItems;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOption;
import com.suntide_20210418.advancedmemorycard.menu.CopyModeMenu;
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
 * <p>竖直排列 1 个标题文字（由样式 JSON 的 {@code copy_options_title} 直接渲染）和 4 个
 * {@link AECheckbox}，每项控制粘贴时是否应用对应的导出组件。
 *
 * <p>布局全部由 {@code copy_mode_menu.json} 定义，AE2 的 {@code WidgetContainer} 会按槽位
 * 位置放置控件并在 resize 时重新计算，因此这里只管状态，不管坐标：
 * <pre>
 *   left = 10（全部元素）
 *   标题 top = 10，高 9（font.lineHeight）
 *   复选框 top = 24 / 43 / 62 / 81，高 14（AECheckbox.SIZE），步进 = 14 + 5
 * </pre>
 *
 * <p>状态以服务端为准（{@link CopyModeMenu#getApplyMask()}），客户端点击只负责提交整张掩码；
 * 服务端回读后的同步值才是最终显示状态。
 */
public class CopyModeScreen extends AEBaseScreen<CopyModeMenu> {

    private final Map<CopyOption, AECheckbox> checkboxes = new EnumMap<>(CopyOption.class);

    /** 上一次已应用到界面上的同步值，用于避免每 tick 重设导致点击瞬间被旧值覆盖。 */
    private int lastSyncedMask = -1;

    public CopyModeScreen(CopyModeMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        // 必须在构造期注册：AEBaseScreen.init() 会统一把这些控件放进 renderables 并按 JSON 槽位摆位
        for (CopyOption option : CopyOption.values()) {
            AECheckbox checkbox = this.widgets.addCheckbox(
                    widgetId(option),
                    Component.translatable(option.translationKey()),
                    this::pushMask);
            // 默认全选：卡片没有该 NBT 键时就是这个状态，同步值到达后会立刻校正
            checkbox.setSelected(true);
            checkboxes.put(option, checkbox);
        }
    }

    private static String widgetId(CopyOption option) {
        return "opt_" + option.name().toLowerCase(Locale.ROOT);
    }

    /** 提交整张掩码而不是 delta：天然幂等，重复包/丢包都不会把状态写歪。 */
    private void pushMask() {
        menu.dispatchSetApplyMask(CopyOption.maskOf(o -> checkboxes.get(o).isSelected()));
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
}
