package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import com.suntide_20210418.advancedmemorycard.client.gui.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.DetailPanelWidget;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.P2PTreeWidget;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.network.ConfigModeSyncPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import com.suntide_20210418.advancedmemorycard.p2p.NodeType;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import com.suntide_20210418.advancedmemorycard.p2p.TreeNode;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class ConfigModeScreen extends GuiContainer {

    private final ConfigModeMenu menu;
    private final EntityPlayer player;

    private P2PTreeWidget p2pTree;
    private DetailPanelWidget detailPanel;
    private GuiTextField searchField;

    /** 与 1.21.1 的 assets/ae2/screens/config_mode_menu.json 保持一致。 */
    private static final int PANEL_WIDTH = 352;
    private static final int PANEL_HEIGHT = 282;
    private static final int SEARCH_LEFT = 7;
    private static final int SEARCH_TOP = 25;
    private static final int SEARCH_WIDTH = 120;
    private static final int SEARCH_HEIGHT = 12;
    private static final int TREE_LEFT = 15;
    private static final int TREE_TOP = 53;
    private static final int TREE_WIDTH = 152;
    private static final int TREE_HEIGHT = 200;
    private static final int SCROLLBAR_LEFT = 169;
    private static final int DETAIL_LEFT = 184;
    private static final int DETAIL_WIDTH = 152;
    private static final int DETAIL_HEIGHT = 210;
    private static final int INFO_TOP = 269;
    private static final int TEXT_COLOR = 0x404040;

    private static final net.minecraft.util.ResourceLocation MENU_TEXTURE =
            new net.minecraft.util.ResourceLocation(
                    com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod.MOD_ID,
                    "textures/gui/config_mode_menu.png");

    private static final int UPDATE_ITEM_INFO_INTERVAL = 10;
    private int updateItemInfoCooldown = 0;
    private P2PInfo lastNavigatedP2P = null;

    private boolean scrollDragging = false;
    private int scrollDragStartY = 0;
    private int scrollDragStartOffset = 0;
    private String lastSearchText = "";

    public ConfigModeScreen(ConfigModeMenu menu, EntityPlayer player) {
        super(menu);
        this.menu = menu;
        this.player = player;
        this.xSize = PANEL_WIDTH;
        this.ySize = PANEL_HEIGHT;
    }

    private static String encodeP2PPosition(P2PInfo p2pInfo) {
        return p2pInfo.position().getX() + "|"
                + p2pInfo.position().getY() + "|"
                + p2pInfo.position().getZ() + "|"
                + p2pInfo.direction().ordinal() + "|"
                + p2pInfo.dimensionId();
    }

    @Override
    public void initGui() {
        this.xSize = PANEL_WIDTH;
        this.ySize = PANEL_HEIGHT;
        super.initGui();

        int treeX = guiLeft + TREE_LEFT;
        int treeY = guiTop + TREE_TOP;
        int treeW = TREE_WIDTH;
        int treeH = TREE_HEIGHT;

        p2pTree = new P2PTreeWidget(treeX, treeY, treeW, treeH);
        p2pTree.setNodeSelectionListener(node -> {
            if (node != null && node.type != NodeType.SEARCH_HEADER) {
                if (node.p2pInfo != null) {
                    p2pTree.navigateToP2P(node.p2pInfo);
                }
                updateDetailPanelNode(node);
            }
        });

        detailPanel = new DetailPanelWidget(guiLeft + DETAIL_LEFT, treeY, DETAIL_WIDTH, DETAIL_HEIGHT);
        detailPanel.setActionCallback(new DetailPanelWidget.ActionCallback() {
            @Override public void onRename(P2PInfo p2pInfo, String newName) {
                if (!newName.isEmpty()) { actionSetP2PAlias(p2pInfo, newName); resetUpdateCooldown(); }
            }
            @Override public void onBind(P2PInfo p2pInfo) { actionBindFrequency(p2pInfo.frequency()); }
            @Override public void onSelect(P2PInfo p2pInfo) { actionSetPendingBind(p2pInfo); }
            @Override public void onHighlight(P2PInfo p2pInfo) { actionHighlightP2P(p2pInfo); if (ModConfigs.getClientConfig().rotateHeadOnHighlight) rotatePlayerTowardsP2P(p2pInfo); }
            @Override public void onLocate(P2PInfo p2pInfo) { navigateToP2P(p2pInfo); }
            @Override public void onAutoAssign() { actionAutoConfigIO(); }
            @Override public void onAssignFreq(P2PInfo p2pInfo) { actionAssignFreq(); }
            @Override public void onChannelRename(String frequency, String newAlias) { actionSetChannelAlias(frequency, newAlias); resetUpdateCooldown(); }
            @Override public void onChannelBind(String frequency) { actionBindFrequency(frequency); }
            @Override public void onChannelHighlight(String frequency) { actionHighlightP2PTunnel(frequency); }
            @Override public void onRefresh() { actionRefreshP2P(); resetUpdateCooldown(); }
        });

        searchField = new GuiTextField(0, fontRenderer, guiLeft + SEARCH_LEFT, guiTop + SEARCH_TOP,
                SEARCH_WIDTH, SEARCH_HEIGHT);
        searchField.setMaxStringLength(64);
        searchField.setFocused(false);

        updateTreeData();
    }

    private void actionAssignFreq() { menu.dispatchClientAction("assign_freq"); }

    public void navigateToP2P(P2PInfo targetP2P) {
        if (p2pTree != null && targetP2P != null) {
            p2pTree.navigateToP2P(targetP2P);
        }
    }

    private void updateTreeData() {
        if (p2pTree != null) {
            p2pTree.updateData(menu.getClientP2PTypeInfoMap(), menu.getClientChannelInfoMap(), menu.getClientP2PInfoMap());
        }
    }

    private void updateDetailPanelNode(TreeNode node) {
        if (detailPanel != null) {
            detailPanel.setSelectedNode(node, menu.getClientChannelInfoMap(), menu.getClientP2PTypeInfoMap());
        }
    }

    private void resetUpdateCooldown() { updateItemInfoCooldown = 0; }

    public void actionRefreshP2P() { menu.dispatchClientAction("refresh_p2p"); }
    public void actionUpdateItemInfo() { menu.dispatchClientAction("update_item_info"); }
    public void actionBindFrequency(String frequencyHex) { menu.dispatchClientAction("bind_frequency", frequencyHex); }
    public void actionSetChannelAlias(String frequency, String alias) { menu.dispatchClientAction("set_channel_alias", frequency + "|" + alias); }
    public void actionSetP2PAlias(P2PInfo p2pInfo, String alias) {
        menu.dispatchClientAction("set_p2p_alias", encodeP2PPosition(p2pInfo) + "::" + alias);
    }
    public void actionSetPendingBind(P2PInfo p2pInfo) {
        menu.dispatchClientAction("set_pending_bind", encodeP2PPosition(p2pInfo));
    }
    public void actionAutoConfigIO() { menu.dispatchClientAction("auto_config_io"); }
    public void actionHighlightP2P(P2PInfo p2pInfo) {
        menu.dispatchClientAction("highlight_p2p", encodeP2PPosition(p2pInfo));
    }
    public void actionHighlightP2PTunnel(String frequencyHex) {
        menu.dispatchClientAction("highlight_p2p_tunnel", frequencyHex);
    }

    private void rotatePlayerTowardsP2P(P2PInfo p2pInfo) {
        EntityPlayer p = Minecraft.getMinecraft().player;
        if (p == null) return;
        BlockPos pos = p2pInfo.position();
        double dx = pos.getX() + 0.5 - p.posX;
        double dy = pos.getY() + 0.5 - (p.posY + p.eyeHeight);
        double dz = pos.getZ() + 0.5 - p.posZ;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontalDist)));
        p.rotationYaw = yaw;
        p.rotationPitch = pitch;
        p.setRotationYawHead(yaw);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        // 抽取同步数据包到本地缓存
        ConfigModeSyncPacket packet;
        while ((packet = NetworkHandler.syncQueue.poll()) != null) {
            menu.receiveSyncData(packet);
        }

        if (updateItemInfoCooldown <= 0) {
            actionUpdateItemInfo();
            updateItemInfoCooldown = UPDATE_ITEM_INFO_INTERVAL;
        } else {
            updateItemInfoCooldown--;
        }

        updateTreeData();

        tryAutoNavigateToPendingBind();

        if (p2pTree != null) {
            updateDetailPanelNode(p2pTree.getSelectedNode());
        }
        if (detailPanel != null) {
            detailPanel.setPendingBindP2P(lastNavigatedP2P);
            detailPanel.updateScreen();
        }

        // 搜索过滤
        String text = searchField != null ? searchField.getText() : "";
        if (!text.equals(lastSearchText)) {
            lastSearchText = text;
            p2pTree.setSearchFilter(text);
        }
    }

    private void tryAutoNavigateToPendingBind() {
        P2PInfo pendingBind = menu.getClientPendingBindP2PInfo();
        if (pendingBind == null) {
            lastNavigatedP2P = null;
            return;
        }
        if (lastNavigatedP2P != null && lastNavigatedP2P.equals(pendingBind)) {
            return;
        }
        lastNavigatedP2P = pendingBind;
        navigateToP2P(pendingBind);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // 面板背景：直接画 1.21.1 的贴图（352×282，取自 512×512 画布的 (0,0)）
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(MENU_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        // 标题与底部提示
        fontRenderer.drawString(TranslateHelper.translate(TranslateHelper.Keys.CONFIG_MODE_SCREEN_TITLE),
                guiLeft + 7, guiTop + 7, TEXT_COLOR);
        fontRenderer.drawString(TranslateHelper.translate(TranslateHelper.Keys.CONFIG_MODE_SCREEN_INFO),
                guiLeft + 7, guiTop + INFO_TOP, TEXT_COLOR);

        // 搜索框
        if (searchField != null) searchField.drawTextBox();

        // 树
        if (p2pTree != null) p2pTree.draw(fontRenderer, mouseX, mouseY);
        // 详情面板
        if (detailPanel != null) detailPanel.draw(fontRenderer, mouseX, mouseY);

        drawScrollbar();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

    }

    private void drawScrollbar() {
        int trackX = guiLeft + SCROLLBAR_LEFT;
        int trackY = p2pTree.getY();
        int trackH = p2pTree.getHeight();
        int total = p2pTree.getTotalContentHeight();
        if (total <= p2pTree.getHeight()) return;
        int maxScroll = total - p2pTree.getHeight();
        int thumbH = Math.max(10, (int) ((double) p2pTree.getHeight() / total * trackH));
        if (thumbH > trackH) thumbH = trackH;
        int thumbY = trackY + (int) ((double) p2pTree.getScrollOffset() / maxScroll * (trackH - thumbH));
        Gui.drawRect(trackX, trackY, trackX + 4, trackY + trackH, 0xFF555555);
        Gui.drawRect(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xFFAAAAAA);
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            if (p2pTree != null && p2pTree.mouseScrolled(mx, my, wheel > 0 ? 1 : -1)) {
                return;
            }
        }
        super.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        // 搜索框焦点
        if (searchField != null) {
            boolean inSearch = mouseX >= searchField.x && mouseX <= searchField.x + searchField.width
                    && mouseY >= searchField.y && mouseY <= searchField.y + searchField.height;
            if (!inSearch) searchField.setFocused(false);
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (p2pTree != null && p2pTree.mouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (detailPanel != null && detailPanel.mouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        // 滚动条拖动
        if (startScrollDrag(mouseX, mouseY)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (scrollDragging) {
            int trackY = p2pTree.getY();
            int trackH = p2pTree.getHeight();
            int total = p2pTree.getTotalContentHeight();
            int maxScroll = Math.max(1, total - p2pTree.getHeight());
            int thumbH = Math.max(10, (int) ((double) p2pTree.getHeight() / total * trackH));
            int usable = trackH - thumbH;
            if (usable > 0) {
                int delta = mouseY - scrollDragStartY;
                int newOffset = scrollDragStartOffset + (int) ((double) delta / usable * maxScroll);
                newOffset = Math.max(0, Math.min(newOffset, maxScroll));
                p2pTree.setScrollOffset(newOffset);
            }
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (scrollDragging) {
            scrollDragging = false;
            return;
        }
        if (detailPanel != null) detailPanel.mouseReleased(mouseX, mouseY, mouseButton);
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    private boolean startScrollDrag(int mouseX, int mouseY) {
        int trackX = guiLeft + SCROLLBAR_LEFT;
        int trackY = p2pTree.getY();
        int trackH = p2pTree.getHeight();
        if (p2pTree.getTotalContentHeight() <= p2pTree.getHeight()) return false;
        if (mouseX >= trackX && mouseX <= trackX + 4 && mouseY >= trackY && mouseY <= trackY + trackH) {
            scrollDragging = true;
            scrollDragStartY = mouseY;
            scrollDragStartOffset = p2pTree.getScrollOffset();
            return true;
        }
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (searchField != null && searchField.isFocused()) {
            if (searchField.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
            if (keyCode == 1) { // Escape
                super.keyTyped(typedChar, keyCode);
            }
            return;
        }
        if (detailPanel != null && detailPanel.keyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
