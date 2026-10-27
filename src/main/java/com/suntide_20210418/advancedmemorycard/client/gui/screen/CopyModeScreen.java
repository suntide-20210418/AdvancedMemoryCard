package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import com.suntide_20210418.advancedmemorycard.client.gui.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOption;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOptions;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Locale;

/**
 * 粘贴选项面板。
 *
 * <p>1.12.2 没有 AE2 的 ScreenStyle / 控件系统，所以这里手绘：8 个开关两列排列，
 * 底部一行控制「复制后自动弹出」。开关状态存在卡片 NBT 上，客户端点击后先本地更新
 * 再发包给服务端落盘，服务端回写物品 NBT 后客户端会收到同步。
 */
@SideOnly(Side.CLIENT)
public class CopyModeScreen extends GuiContainer {

    private static final String ACTION_SET_OPTIONS = "set_options:";
    private static final int AUTO_OPEN_BUTTON_ID = 100;

    private final CopyModeMenu menu;

    private CopyOptions options;
    private GuiButton autoOpenButton;
    private final GuiButton[] optionButtons = new GuiButton[CopyOption.VALUES.length];

    public CopyModeScreen(CopyModeMenu menu, EntityPlayer player) {
        super(menu);
        this.menu = menu;
        this.xSize = 220;
        this.ySize = 150;
    }

    @Override
    public void initGui() {
        this.xSize = 220;
        this.ySize = 150;
        super.initGui();

        this.options = readOptions();

        int colWidth = 100;
        int rowHeight = 20;
        int baseX = guiLeft + 8;
        int baseY = guiTop + 26;

        for (int i = 0; i < CopyOption.VALUES.length; i++) {
            int col = i % 2;
            int row = i / 2;
            GuiButton button = new GuiButton(i, baseX + col * (colWidth + 4), baseY + row * rowHeight,
                    colWidth, 18, "");
            optionButtons[i] = button;
            this.buttonList.add(button);
        }

        autoOpenButton = new GuiButton(AUTO_OPEN_BUTTON_ID, baseX, guiTop + ySize - 26,
                xSize - 16, 18, "");
        this.buttonList.add(autoOpenButton);

        refreshLabels();
    }

    private CopyOptions readOptions() {
        if (mc.player == null) {
            return CopyOptions.of(ItemStack.EMPTY);
        }
        return CopyOptions.of(mc.player.getHeldItem(menu.getHand()));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == AUTO_OPEN_BUTTON_ID) {
            options = options.withAutoOpen(!options.isAutoOpen());
        } else if (button.id >= 0 && button.id < CopyOption.VALUES.length) {
            CopyOption option = CopyOption.VALUES[button.id];
            options = options.with(option, !options.has(option));
        } else {
            return;
        }
        menu.dispatchClientAction(ACTION_SET_OPTIONS + options.mask() + ":" + (options.isAutoOpen() ? 1 : 0));
        refreshLabels();
    }

    private void refreshLabels() {
        for (int i = 0; i < CopyOption.VALUES.length; i++) {
            CopyOption option = CopyOption.VALUES[i];
            optionButtons[i].displayString = (options.has(option) ? "[x] " : "[  ] ")
                    + TranslateHelper.CopyMode.option(option.name().toLowerCase(Locale.ROOT));
        }
        autoOpenButton.displayString = (options.isAutoOpen() ? "[x] " : "[  ] ")
                + TranslateHelper.CopyMode.autoOpen();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // 服务端落盘后物品 NBT 会同步回来，以它为准刷新一次显示
        CopyOptions synced = readOptions();
        if (synced.mask() != options.mask() || synced.isAutoOpen() != options.isAutoOpen()) {
            options = synced;
            refreshLabels();
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // 面板底色：直接铺两块纯色，不依赖任何贴图
        Gui.drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF2B2B2B);
        Gui.drawRect(guiLeft + 2, guiTop + 2, guiLeft + xSize - 2, guiTop + ySize - 2, 0xFF3B3B3B);
        fontRenderer.drawString(TranslateHelper.CopyMode.optionsTitle(), guiLeft + 8, guiTop + 8, 0xFFFFFFFF);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
