package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;

import com.suntide_20210418.advancedmemorycard.client.gui.ClickSound;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOption;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOptions;
import com.suntide_20210418.advancedmemorycard.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.network.CopyOptionsSyncPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import com.suntide_20210418.advancedmemorycard.utils.ResourceLocationHelper;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 粘贴选项面板。
 *
 * <p>布局沿用 1.12.2 / 1.21.1 的 {@code copy_mode_menu}：面板 176×181、标题 (10,10)、
 * 每个选项一行（left 10、width 150、height 14、步进 19、首行 top 24）。
 *
 * <p>控件贴图用 {@code checkbox.png} 的四个 22×12 状态格：
 * <pre>
 *   未选中      (0, 28)    未选中+悬停  (22, 28)
 *   选中        (0, 40)    选中+悬停    (22, 40)
 * </pre>
 *
 * <p>状态以服务端为准：点击时先本地更新（立即出画面），同时把整张掩码发给服务端；
 * 服务端落盘后回发 {@link CopyOptionsSyncPacket}，界面收到后以它覆盖本地值。
 * 直接读物品 NBT 刷新是错的——{@code CopyModeMenu} 是空容器，服务端改 NBT 后不会触发
 * 槽位同步，客户端要重开界面才看得到。
 */
@SideOnly(Side.CLIENT)
public class CopyModeScreen extends GuiContainer {

    private static final String ACTION_SET_OPTIONS = "set_options:";

    private static final ResourceLocation MENU_TEXTURE = ResourceLocationHelper.guiTexture("copy_mode_menu.png");
    private static final ResourceLocation CHECKBOX_TEXTURE = ResourceLocationHelper.guiTexture("checkbox.png");

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

    /** 原图像素尺寸：1.7.10 的 drawTexturedModalRect 固定按 256×256 算 UV，非 256 的图必须走 custom sized。 */
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

    public CopyModeScreen(CopyModeMenu menu) {
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
        this.options = CopyOptions.of(null)
            .withMask(menu.getApplyMask())
            .withAutoOpen(menu.isAutoOpen());
        drainSync();
    }

    /**
     * 采纳服务端的权威状态；没有新包就保持本地（点击后的即时反馈）。
     *
     * <p>打开界面的同步包可能比开窗包先到，所以 initGui 里也要排空一次队列。
     */
    private void drainSync() {
        CopyOptionsSyncPacket packet;
        while ((packet = NetworkHandler.copyOptionsQueue.poll()) != null) {
            options = options.withMask(packet.getMask())
                .withAutoOpen(packet.isAutoOpen());
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        drainSync();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
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
                ClickSound.play();
                pushOptions();
                return true;
            }
        }
        if (isRowHovered(mouseX, mouseY, AUTO_OPEN_TOP)) {
            options = options.withAutoOpen(!options.isAutoOpen());
            ClickSound.play();
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
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(MENU_TEXTURE);
        Gui.func_146110_a(guiLeft, guiTop, 0, 0, xSize, ySize, MENU_TEX_SIZE, MENU_TEX_SIZE);

        fontRendererObj.drawString(
            TranslateHelper.CopyMode.optionsTitle(),
            guiLeft + ROW_LEFT,
            guiTop + 10,
            TEXT_COLOR);

        for (int i = 0; i < CopyOption.VALUES.length; i++) {
            CopyOption option = CopyOption.VALUES[i];
            drawRow(rowTop(i), option, options.has(option), mouseX, mouseY);
        }
        drawRow(AUTO_OPEN_TOP, null, options.isAutoOpen(), mouseX, mouseY);
    }

    /**
     * @param option 为 null 表示「复制后自动弹出」那一行，文案走单独的键
     */
    private void drawRow(int top, CopyOption option, boolean on, int mouseX, int mouseY) {
        int x = guiLeft + ROW_LEFT;
        int y = guiTop + top;

        boolean hovered = isRowHovered(mouseX, mouseY, top);
        if (hovered) {
            Gui.drawRect(x - 2, y - 1, x + ROW_WIDTH + 2, y + ROW_HEIGHT - 1, HOVER_COLOR);
        }

        // 每次画贴图前必须把当前颜色复位：
        // 1.7.10 的 Gui#drawRect 会把 GL 颜色改成矩形色且不还原（悬停高亮是 0x22000000），
        // FontRenderer#drawString 也会把颜色设成文字色（0x404040）。不复位就会把贴图整体乘暗。
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(CHECKBOX_TEXTURE);
        Gui.func_146110_a(x, y + 1, hovered ? CHECKBOX_U_HOVER : CHECKBOX_U_NORMAL, on ? CHECKBOX_V_ON : CHECKBOX_V_OFF,
            CHECKBOX_W, CHECKBOX_H, CHECKBOX_TEX_SIZE, CHECKBOX_TEX_SIZE);

        String label = option == null ? TranslateHelper.CopyMode.autoOpen() : TranslateHelper.CopyMode.option(option.name()
            .toLowerCase(java.util.Locale.ROOT));
        fontRendererObj.drawString(label, x + CHECKBOX_W + 4, y + 3, TEXT_COLOR);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
