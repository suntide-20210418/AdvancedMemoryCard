package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOption;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOptions;
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
 * <p>布局直接沿用 1.21.1 的 {@code assets/ae2/screens/copy_mode_menu.json}：
 * <ul>
 *   <li>面板贴图 {@code copy_mode_menu.png}，176×181，从 256×256 画布的 (0,0) 取；</li>
 *   <li>标题在 (10, 10)；</li>
 *   <li>每个选项一行，left = 10、width = 150、height = 14，纵向步进 19，首行 top = 24。</li>
 * </ul>
 * 1.12.2 没有 AE2 的 {@code AECheckbox} 与 {@code IconButton}，所以复选框改用一张
 * 22×11 的双状态精灵（未选中 / 选中），只做「画图 + 命中判定」，不再用文字模拟控件。
 *
 * <p>与 1.21.1 的唯一偏差：「复制后自动弹出」在 1.21.1 是挂在面板左侧外缘的图标按钮，
 * 1.12.2 没有那套 Icon 精灵，改为面板底部（top = 160）同一视觉语言的一行开关。
 */
@SideOnly(Side.CLIENT)
public class CopyModeScreen extends GuiContainer {

    private static final String ACTION_SET_OPTIONS = "set_options:";

    private static final ResourceLocation MENU_TEXTURE =
            new ResourceLocation(AdvancedMemoryCardMod.MOD_ID, "textures/gui/copy_mode_menu.png");
    private static final ResourceLocation CHECKBOX_TEXTURE =
            new ResourceLocation(AdvancedMemoryCardMod.MOD_ID, "textures/gui/checkbox.png");

    /**
     * 复选框精灵：取 checkbox.png 左下角那一列——上格是未选中（灰），下格是选中（蓝底浅色竖条）。
     * 两个状态都是 12×12，原图坐标 (0,28) 与 (0,40)。
     */
    private static final int CHECKBOX_SIZE = 12;
    private static final int CHECKBOX_U = 0;
    private static final int CHECKBOX_V_UNCHECKED = 28;
    private static final int CHECKBOX_V_CHECKED = 40;

    /** 与 1.21.1 的 copy_mode_menu.json 保持一致。 */
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
        this.options = readOptions();
    }

    private CopyOptions readOptions() {
        if (mc.player == null) {
            return CopyOptions.of(ItemStack.EMPTY);
        }
        return CopyOptions.of(mc.player.getHeldItem(menu.getHand()));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // 服务端落盘后物品 NBT 会同步回来，以它为准刷新显示
        CopyOptions synced = readOptions();
        if (synced.mask() != options.mask() || synced.isAutoOpen() != options.isAutoOpen()) {
            options = synced;
        }
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
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

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

        if (isRowHovered(mouseX, mouseY, top)) {
            Gui.drawRect(x - 2, y - 1, x + ROW_WIDTH + 2, y + ROW_HEIGHT - 1, HOVER_COLOR);
        }

        mc.getTextureManager().bindTexture(CHECKBOX_TEXTURE);
        drawTexturedModalRect(x, y + 1,
                CHECKBOX_U, on ? CHECKBOX_V_CHECKED : CHECKBOX_V_UNCHECKED,
                CHECKBOX_SIZE, CHECKBOX_SIZE);

        String label = optionId == null
                ? TranslateHelper.CopyMode.autoOpen()
                : TranslateHelper.CopyMode.option(optionId);
        fontRenderer.drawString(label, x + CHECKBOX_SIZE + 4, y + 3, TEXT_COLOR);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
