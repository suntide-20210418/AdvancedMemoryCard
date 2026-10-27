package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOption;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOptions;
import com.suntide_20210418.advancedmemorycard.network.CopyOptionsSyncPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Locale;

/**
 * 粘贴选项面板。
 *
 * <p>布局沿用 1.21.1 的 {@code assets/ae2/screens/copy_mode_menu.json}：面板 176×181、
 * 标题 (10,10)、每个选项一行（left 10、width 150、height 14、步进 19、首行 top 24）。
 *
 * <p>控件贴图用 {@code checkbox.png} 的四个 22×12 状态格：
 * <pre>
 *   未选中      (0, 28)    未选中+悬停  (22, 28)
 *   选中        (0, 40)    选中+悬停    (22, 40)
 * </pre>
 *
 * <p>状态以服务端为准：点击时先本地更新（立即出画面），同时把整张掩码发给服务端；
 * 服务端落盘后回发 {@link CopyOptionsSyncPacket}，界面收到后以它覆盖本地值。
 * 早前直接读物品 NBT 刷新是错的——{@code CopyModeMenu} 是空容器，服务端改 NBT 后
 * 不会触发 {@code SPacketSetSlot}，客户端要重开界面才看得到。
 */
@SideOnly(Side.CLIENT)
public class CopyModeScreen extends GuiContainer {

    private static final String ACTION_SET_OPTIONS = "set_options:";

    private static final ResourceLocation MENU_TEXTURE =
            new ResourceLocation(AdvancedMemoryCardMod.MOD_ID, "textures/gui/copy_mode_menu.png");
    private static final ResourceLocation CHECKBOX_TEXTURE =
            new ResourceLocation(AdvancedMemoryCardMod.MOD_ID, "textures/gui/checkbox.png");

    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 181;
    private static final int ROW_LEFT = 10;
    private static final int ROW_WIDTH = 150;
    private static final int ROW_HEIGHT = 14;
    private static final int ROW_TOP = 24;
    private static final int ROW_STEP = 19;
    private static final int AUTO_OPEN_TOP = 160;

    private static final int TEXT_COLOR = 0x404040;
    private static final int HOVER_COLOR = 0x22000000;

    /** 原图像素尺寸：1.12.2 的 drawTexturedModalRect 固定按 256×256 算 UV，非 256 的图必须走 custom sized。 */
    private static final int MENU_TEX_SIZE = 256;
    private static final int CHECKBOX_TEX_SIZE = 64;

    private static final int CHECKBOX_W = 22;
    private static final int CHECKBOX_H = 12;
    private static final int CHECKBOX_V_OFF = 28;
    private static final int CHECKBOX_V_ON = 40;
    private static final int CHECKBOX_U_NORMAL = 0;
    private static final int CHECKBOX_U_HOVER = 22;

    private final CopyModeMenu menu;
    private CopyOptions options;

    public CopyModeScreen(CopyModeMenu menu, EntityPlayer player) {
        super(menu);
        this.menu = menu;
        this.xSize = PANEL_WIDTH;
        this.ySize = PANEL_HEIGHT;
    }

    @Override
    public void initGui() {
        this.xSize = PANEL_WIDTH;
        this.ySize = PANEL_HEIGHT;
        super.initGui();

        // 初值取菜单（服务端会在创建容器时发一次同步包），先按当前已知值铺一遍
        this.options = CopyOptions.of(ItemStack.EMPTY)
                .withMask(menu.getApplyMask())
                .withAutoOpen(menu.isAutoOpen());
        drainSync();
    }

    /** 采纳服务端的权威状态；没有新包就保持本地（点击后的即时反馈）。 */
    private void drainSync() {
        CopyOptionsSyncPacket packet;
        while ((packet = NetworkHandler.copyOptionsQueue.poll()) != null) {
            options = options.withMask(packet.getMask()).withAutoOpen(packet.isAutoOpen());
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        drainSync();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (mouseButton == 0 && applyClick(mouseX, mouseY)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean applyClick(int mouseX, int mouseY) {
        for (int i = 0; i < CopyOption.VALUES.length; i++) {
            if (isRowHovered(mouseX, mouseY, rowTop(i))) {
                CopyOption option = CopyOption.VALUES[i];
                options = options.with(option, !options.has(option));
                pushOptions();
                return true;
            }
        }
        if (isRowHovered(mouseX, mouseY, AUTO_OPEN_TOP)) {
            options = options.withAutoOpen(!options.isAutoOpen());
            pushOptions();
            return true;
        }
        return false;
    }

    /** 提交整张掩码而不是增量：幂等，重复包/丢包都不会把状态写歪。 */
    private void pushOptions() {
        menu.dispatchClientAction(ACTION_SET_OPTIONS + options.mask() + ":" + (options.isAutoOpen() ? 1 : 0));
    }

    private int rowTop(int index) {
        return ROW_TOP + index * ROW_STEP;
    }

    private boolean isRowHovered(int mouseX, int mouseY, int top) {
        int x = guiLeft + ROW_LEFT;
        int y = guiTop + top;
        return mouseX >= x && mouseX < x + ROW_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(MENU_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, xSize, ySize,
                MENU_TEX_SIZE, MENU_TEX_SIZE);

        fontRenderer.drawString(TranslateHelper.CopyMode.optionsTitle(),
                guiLeft + ROW_LEFT, guiTop + 10, TEXT_COLOR);

        for (int i = 0; i < CopyOption.VALUES.length; i++) {
            CopyOption option = CopyOption.VALUES[i];
            drawRow(rowTop(i), option.name().toLowerCase(Locale.ROOT), options.has(option), mouseX, mouseY);
        }
        drawRow(AUTO_OPEN_TOP, null, options.isAutoOpen(), mouseX, mouseY);
    }

    /**
     * @param optionId 为 null 表示「复制后自动弹出」那一行，文案走单独的键
     */
    private void drawRow(int top, String optionId, boolean on, int mouseX, int mouseY) {
        int x = guiLeft + ROW_LEFT;
        int y = guiTop + top;

        boolean hovered = isRowHovered(mouseX, mouseY, top);
        if (hovered) {
            Gui.drawRect(x - 2, y - 1, x + ROW_WIDTH + 2, y + ROW_HEIGHT - 1, HOVER_COLOR);
        }

        mc.getTextureManager().bindTexture(CHECKBOX_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(x, y + 1,
                hovered ? CHECKBOX_U_HOVER : CHECKBOX_U_NORMAL,
                on ? CHECKBOX_V_ON : CHECKBOX_V_OFF,
                CHECKBOX_W, CHECKBOX_H, CHECKBOX_TEX_SIZE, CHECKBOX_TEX_SIZE);

        String label = optionId == null
                ? TranslateHelper.CopyMode.autoOpen()
                : TranslateHelper.CopyMode.option(optionId);
        fontRenderer.drawString(label, x + CHECKBOX_W + 4, y + 3, TEXT_COLOR);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
