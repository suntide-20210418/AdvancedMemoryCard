package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.suntide_20210418.advancedmemorycard.client.gui.ConfigUiLayout;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.ActionBarWidget;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.DetailPanelWidget;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.P2PTreeWidget;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.PairBarWidget;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.network.ConfigModeSyncPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
import com.suntide_20210418.advancedmemorycard.p2p.ChannelInfo;
import com.suntide_20210418.advancedmemorycard.p2p.NodeType;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import com.suntide_20210418.advancedmemorycard.p2p.TreeNode;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;
import com.suntide_20210418.advancedmemorycard.utils.ResourceLocationHelper;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ConfigModeScreen extends GuiContainer {

    private final ConfigModeMenu menu;
    private final EntityPlayer player;

    private P2PTreeWidget p2pTree;
    private DetailPanelWidget detailPanel;
    private PairBarWidget pairBar;
    private ActionBarWidget actionBar;
    private GuiTextField searchField;

    private static final int UPDATE_ITEM_INFO_INTERVAL = 10;
    private int updateItemInfoCooldown = 0;
    private P2PInfo lastNavigatedP2P = null;

    // 与基准 1.20.1 / 1.21.1 的 440×312 面板一致；旧版没有 AE2 的 ScreenStyle，
    // 这里用 ConfigUiLayout 同数值的矩形承担，面板背景直接铺基准那张图集。
    private static final int PANEL_WIDTH = ConfigUiLayout.PANEL_W;
    private static final int PANEL_HEIGHT = ConfigUiLayout.PANEL_H;
    private static final ResourceLocation PANEL_TEXTURE = ResourceLocationHelper.guiTexture("config_mode_menu.png");
    private static final int TEXT_COLOR = 0x404040;
    /** 关掉原版底框后文字贴着 yPosition 画，下移这么多才能在那个 13px 槽位里居中（字高 8）。 */
    private static final int SEARCH_TEXT_OFFSET = 2;
    private static final int PLACEHOLDER_COLOR = 0x6E7488;

    private boolean scrollDragging = false;
    private int scrollDragStartY = 0;
    private int scrollDragStartOffset = 0;
    private String lastSearchText = "";

    /** 同步版本号：每收到一个同步包 +1。只在变化时刷新配对条/操作条的“来自服务端”数据。 */
    private int lastSyncVersion = -1;

    public ConfigModeScreen(ConfigModeMenu menu, EntityPlayer player) {
        super(menu);
        this.menu = menu;
        this.player = player;
        this.xSize = PANEL_WIDTH;
        this.ySize = PANEL_HEIGHT;
    }

    private static String encodeP2PPosition(P2PInfo p2pInfo) {
        return p2pInfo.position()
            .getX() + "|"
            + p2pInfo.position()
                .getY()
            + "|"
            + p2pInfo.position()
                .getZ()
            + "|"
            + p2pInfo.direction()
                .ordinal()
            + "|"
            + p2pInfo.dimensionId();
    }

    @Override
    public void initGui() {
        this.xSize = PANEL_WIDTH;
        this.ySize = PANEL_HEIGHT;
        super.initGui();

        int treeX = guiLeft + ConfigUiLayout.TREE.x;
        int treeY = guiTop + ConfigUiLayout.TREE.y;

        p2pTree = new P2PTreeWidget(treeX, treeY, ConfigUiLayout.TREE.w, ConfigUiLayout.TREE.h);
        p2pTree.setNodeSelectionListener(this::onNodeSelected);
        p2pTree.setBatchModeChangedListener(this::onBatchModeChanged);

        int detailX = guiLeft + ConfigUiLayout.DETAIL.x;
        int detailY = guiTop + ConfigUiLayout.DETAIL.y;
        detailPanel = new DetailPanelWidget(detailX, detailY, ConfigUiLayout.DETAIL.w, ConfigUiLayout.DETAIL.h);
        detailPanel.setActionCallback(new DetailPanelWidget.ActionCallback() {

            @Override
            public void onHighlightP2P(P2PInfo p2pInfo) {
                actionHighlightP2P(p2pInfo);
                if (ModConfigs.getClientConfig().rotateHeadOnHighlight) rotatePlayerTowardsP2P(p2pInfo);
            }

            @Override
            public void onHighlightChannel(String frequency) {
                actionHighlightP2PTunnel(frequency);
            }

            @Override
            public void onTeleportP2P(P2PInfo p2pInfo) {
                actionTeleportP2P(p2pInfo);
            }

            @Override
            public void onUseAsTarget(String frequency) {
                String alias = menu.getClientP2PFrequencyAndAlias()
                    .getOrDefault(frequency, frequency);
                pairBar.setTarget(frequency, alias);
                updateActionBar();
            }

            @Override
            public void onAssignFreq() {
                actionAssignFreq();
            }

            @Override
            public void onSwitchToInput(P2PInfo p2pInfo) {
                actionSwitchToInput(p2pInfo);
            }

            @Override
            public void onInitP2P() {
                actionAutoConfigIO();
            }

            @Override
            public void onRenameP2P(P2PInfo p2pInfo, String newName) {
                if (!newName.isEmpty()) {
                    actionSetP2PAlias(p2pInfo, newName);
                    resetUpdateCooldown();
                }
            }

            @Override
            public void onRenameChannel(String frequency, String newAlias) {
                actionSetChannelAlias(frequency, newAlias);
                resetUpdateCooldown();
            }

            @Override
            public void onRefresh() {
                actionRefreshP2P();
                resetUpdateCooldown();
            }
        });

        // 配对条：源 → 目标 + 频段下拉 + 状态行
        pairBar = new PairBarWidget(
            guiLeft + ConfigUiLayout.PAIR_BAR.x,
            guiTop + ConfigUiLayout.PAIR_BAR.y,
            ConfigUiLayout.PAIR_BAR.w,
            ConfigUiLayout.PAIR_BAR.h);
        pairBar.setCallback(new PairBarWidget.Callback() {

            @Override
            public void onSourceCleared() {
                menu.dispatchClientAction("clear_pending_bind");
            }

            @Override
            public void onTargetChanged(String frequency) {
                // 目标频段是纯客户端概念（点了树里的频段行或下拉项），无需上报；
                // 真正绑定时再随 bind_frequency 一起发给服务端。
                updateActionBar();
            }

            @Override
            public void onNewFrequency() {
                actionAssignFreq();
            }
        });

        // 底部操作条
        actionBar = new ActionBarWidget(
            guiLeft + ConfigUiLayout.ACTION_BAR.x,
            guiTop + ConfigUiLayout.ACTION_BAR.y,
            ConfigUiLayout.ACTION_BAR.w,
            ConfigUiLayout.ACTION_BAR.h);
        actionBar.setOnBind(this::doBind);
        actionBar.setOnBatchToggle(this::toggleBatchMode);

        searchField = new GuiTextField(
            fontRendererObj,
            guiLeft + ConfigUiLayout.SEARCH.x,
            // 关掉原版底框后文字贴着 yPosition 画，下移 2px 才能在那个 13px 的槽位里居中
            guiTop + ConfigUiLayout.SEARCH.y + SEARCH_TEXT_OFFSET,
            ConfigUiLayout.SEARCH.w,
            ConfigUiLayout.SEARCH.h - SEARCH_TEXT_OFFSET);
        searchField.setMaxStringLength(64);
        searchField.setFocused(false);
        // 面板底图自带凹槽，原版输入框的黑底 + 灰边会破坏配色：只保留文字与光标
        searchField.setEnableBackgroundDrawing(false);
        searchField.setTextColor(TEXT_COLOR);
        searchField.setDisabledTextColour(PLACEHOLDER_COLOR);

        updateTreeData();
    }

    private void actionAssignFreq() {
        menu.dispatchClientAction("assign_freq");
    }

    public void navigateToP2P(P2PInfo targetP2P) {
        if (p2pTree != null && targetP2P != null) {
            p2pTree.navigateToP2P(targetP2P);
        }
    }

    private void updateTreeData() {
        if (p2pTree != null) {
            p2pTree
                .updateData(menu.getClientP2PTypeInfoMap(), menu.getClientChannelInfoMap(), menu.getClientP2PInfoMap());
        }
    }

    private void updateDetailPanelNode(TreeNode node) {
        if (detailPanel != null) {
            detailPanel.setSelectedNode(node, menu.getClientChannelInfoMap(), menu.getClientP2PTypeInfoMap());
        }
    }

    private void resetUpdateCooldown() {
        updateItemInfoCooldown = 0;
    }

    public void actionRefreshP2P() {
        menu.dispatchClientAction("refresh_p2p");
    }

    public void actionUpdateItemInfo() {
        menu.dispatchClientAction("update_item_info");
    }

    public void actionBindFrequency(String frequencyHex) {
        menu.dispatchClientAction("bind_frequency", frequencyHex);
    }

    public void actionSetChannelAlias(String frequency, String alias) {
        menu.dispatchClientAction("set_channel_alias", frequency + "|" + alias);
    }

    public void actionSetP2PAlias(P2PInfo p2pInfo, String alias) {
        menu.dispatchClientAction("set_p2p_alias", encodeP2PPosition(p2pInfo) + "::" + alias);
    }

    public void actionSetPendingBind(P2PInfo p2pInfo) {
        menu.dispatchClientAction("set_pending_bind", encodeP2PPosition(p2pInfo));
    }

    public void actionAutoConfigIO() {
        menu.dispatchClientAction("auto_config_io");
    }

    public void actionHighlightP2P(P2PInfo p2pInfo) {
        menu.dispatchClientAction("highlight_p2p", encodeP2PPosition(p2pInfo));
    }

    public void actionHighlightP2PTunnel(String frequencyHex) {
        menu.dispatchClientAction("highlight_p2p_tunnel", frequencyHex);
    }

    public void actionTeleportP2P(P2PInfo p2pInfo) {
        menu.dispatchClientAction("teleport_p2p", encodeP2PPosition(p2pInfo));
    }

    public void actionSwitchToInput(P2PInfo p2pInfo) {
        menu.dispatchClientAction("switch_to_input", encodeP2PPosition(p2pInfo));
    }

    private void rotatePlayerTowardsP2P(P2PInfo p2pInfo) {
        EntityPlayer p = Minecraft.getMinecraft().thePlayer;
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

    // ==================== 树节点选择 → 配对条 ====================

    private void onNodeSelected(TreeNode node) {
        detailPanel.setSelectedNode(node, menu.getClientChannelInfoMap(), menu.getClientP2PTypeInfoMap());

        if (node == null) return;

        if (node.type == NodeType.CHANNEL && node.data instanceof ChannelInfo) {
            // 选中频段行 = 把它填进目标槽，这是配对的主路径。
            // 类型取“它在树里挂的那个分组”，而不是 ChannelInfo 里的第一台设备类型。
            ChannelInfo info = (ChannelInfo) node.data;
            String groupType = node.parent != null ? node.parent.typeName : null;
            pairBar.pickChannel(info, groupType);
            updateActionBar();
        } else if (node.type == NodeType.P2P && node.p2pInfo != null) {
            // 换配对源时保留已选目标频段 —— 前提是类型没变。
            String targetType = pairBar.getTargetType();
            if (targetType != null && !targetType.equals(node.p2pInfo.p2pType())) {
                pairBar.clearTarget();
            }
            // 选中设备 = 它成为配对源。只改配对目标，不动扫描锚点。
            actionSetPendingBind(node.p2pInfo);
        }
    }

    private void onBatchModeChanged(boolean active) {
        if (actionBar != null) {
            actionBar.setBatchActive(active);
        }
    }

    private void toggleBatchMode() {
        if (p2pTree == null) return;
        p2pTree.setBatchMode(!p2pTree.isBatchMode());
    }

    private void updateActionBar() {
        if (actionBar == null || pairBar == null || p2pTree == null) return;
        List<P2PInfo> candidates = p2pTree.getSourceCandidates();
        P2PInfo source = candidates.isEmpty() ? pairBar.getSource() : candidates.get(0);
        String name = source == null ? ""
            : (source.name() == null || source.name()
                .isEmpty() ? source.toShortString() : source.name());
        actionBar.setReady(pairBar.isReady(), name, pairBar.getTargetFrequency(), candidates.size());
    }

    private void doBind() {
        if (pairBar == null || p2pTree == null || !pairBar.isReady()) return;
        String frequency = pairBar.getTargetFrequency();
        List<P2PInfo> candidates = p2pTree.getSourceCandidates();
        if (candidates.size() > 1) {
            // 多选批量绑定：payload = "频段;位置1;位置2;…"
            StringBuilder payload = new StringBuilder(frequency);
            for (P2PInfo info : candidates) {
                payload.append(';')
                    .append(encodeP2PPosition(info));
            }
            menu.dispatchClientAction("bind_frequency_batch", payload.toString());
        } else {
            menu.dispatchClientAction("bind_frequency", frequency);
        }
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

        // 服务端数据变化时才刷新“来自服务端”的配对条字段（频道列表 / 状态 / 配对源回执）
        if (menu.getSyncVersion() != lastSyncVersion) {
            lastSyncVersion = menu.getSyncVersion();
            if (pairBar != null) {
                pairBar.setChannels(
                    new ArrayList<>(
                        menu.getClientChannelInfoMap()
                            .values()));
                pairBar.setStatus(menu.getClientStatusCode(), menu.getClientStatusParam());
            }
        }

        // 配对源以树里的选择为准（可能是 Ctrl 多选的一组）；还没在树里点过时退回服务端配对源。
        if (pairBar != null && p2pTree != null) {
            List<P2PInfo> candidates = p2pTree.getSourceCandidates();
            P2PInfo serverSource = menu.getClientPendingBindP2PInfo();
            if (candidates.isEmpty()) {
                pairBar.setSources(
                    serverSource == null ? Collections.emptyList() : Collections.singletonList(serverSource));
            } else {
                pairBar.setSources(candidates);
            }
            updateActionBar();
        }

        tryAutoNavigateToPendingBind();

        if (p2pTree != null) {
            updateDetailPanelNode(p2pTree.getSelectedNode());
        }
        if (detailPanel != null) {
            detailPanel.updateScreen();
        }

        // 搜索过滤
        String text = searchField != null ? searchField.getText() : "";
        if (!text.equals(lastSearchText)) {
            lastSearchText = text;
            if (p2pTree != null) p2pTree.setSearchFilter(text);
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

        // 面板背景：与基准 1.21.1 同一张 guis/config_mode_menu.png（512×512 图集，取左上 440×312）。
        // drawTexturedModalRect 固定按 256×256 算 UV，非 256 的图必须走 custom sized。
        // 前一步 drawDefaultBackground 会把 GL 颜色压暗，画贴图前要复位成白色。
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(PANEL_TEXTURE);
        Gui.func_146110_a(
            guiLeft,
            guiTop,
            0,
            0,
            PANEL_WIDTH,
            PANEL_HEIGHT,
            ConfigUiLayout.PANEL_TEX_SIZE,
            ConfigUiLayout.PANEL_TEX_SIZE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // 标题
        String title = TranslateHelper.translate(TranslateHelper.Keys.CONFIG_MODE_SCREEN_TITLE);
        fontRendererObj
            .drawString(title, guiLeft + ConfigUiLayout.TITLE.x, guiTop + ConfigUiLayout.TITLE.y, TEXT_COLOR);

        // 搜索框：贴图自带凹槽，框只画文字与光标，空且未聚焦时补占位符
        if (searchField != null) {
            if (searchField.getText()
                .isEmpty() && !searchField.isFocused()) {
                fontRendererObj.drawString(
                    TranslateHelper.translate(TranslateHelper.Keys.CONFIG_MODE_SCREEN_SEARCH_PLACEHOLDER),
                    searchField.xPosition,
                    searchField.yPosition,
                    PLACEHOLDER_COLOR);
            }
            searchField.drawTextBox();
        }

        // 树
        if (p2pTree != null) {
            p2pTree.render(mouseX, mouseY, 0.0F);
            // 列头画在树矩形上方，必须在树之后画才不会被行背景盖住
            p2pTree.renderColumnHeader(p2pTree.x, p2pTree.y - ConfigUiLayout.COLUMN_HEADER_OFFSET);
        }
        // 详情面板
        if (detailPanel != null) detailPanel.render(mouseX, mouseY, 0);

        // 配对条 + 操作条
        if (pairBar != null) pairBar.render(mouseX, mouseY, 0.0F);
        if (actionBar != null) actionBar.render(mouseX, mouseY, 0.0F);
        // 配对条下拉浮层最后画，盖在一切之上
        if (pairBar != null) pairBar.renderOverlay(mouseX, mouseY);

        drawScrollbar();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

    }

    private void drawScrollbar() {
        int trackX = guiLeft + ConfigUiLayout.SCROLLBAR.x;
        int trackY = p2pTree.y;
        int trackH = p2pTree.height;
        int total = p2pTree.getTotalContentHeight();
        if (total <= p2pTree.height) return;
        int maxScroll = total - p2pTree.height;
        int thumbH = Math.max(10, (int) ((double) p2pTree.height / total * trackH));
        if (thumbH > trackH) thumbH = trackH;
        int thumbY = trackY + (int) ((double) p2pTree.getScrollOffset() / maxScroll * (trackH - thumbH));
        Gui.drawRect(trackX, trackY, trackX + ConfigUiLayout.SCROLLBAR.w, trackY + trackH, 0xFF555555);
        Gui.drawRect(trackX, thumbY, trackX + ConfigUiLayout.SCROLLBAR.w, thumbY + thumbH, 0xFFAAAAAA);
    }

    @Override
    public void handleMouseInput() {
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            if (pairBar != null && pairBar.isDropdownOpen() && pairBar.handleOverlayScroll(wheel > 0 ? 1 : -1)) {
                return;
            }
            if (p2pTree != null && p2pTree.mouseScrolled(mx, my, wheel > 0 ? 1 : -1)) {
                return;
            }
        }
        super.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // 搜索框焦点
        if (searchField != null) {
            boolean inSearch = mouseX >= searchField.xPosition && mouseX <= searchField.xPosition + searchField.width
                && mouseY >= searchField.yPosition
                && mouseY <= searchField.yPosition + searchField.height;
            if (!inSearch) searchField.setFocused(false);
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        // 配对条下拉浮层优先吃掉点击
        if (pairBar != null && pairBar.isDropdownOpen() && pairBar.handleOverlayClick(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (pairBar != null && pairBar.mouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (p2pTree != null && p2pTree.mouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (detailPanel != null && detailPanel.mouseClicked(mouseX, mouseY, mouseButton)) {
            return;
        }
        if (actionBar != null && actionBar.mouseClicked(mouseX, mouseY, mouseButton)) {
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
        // 框选优先：指针可能移出树矩形，WidgetContainer 式分发不会再到达树，
        // 这里先全局转发一次，让选择范围能跟着指针越界延伸
        if (p2pTree != null && p2pTree.mouseDragged(mouseX, mouseY, clickedMouseButton, 0, 0)) {
            return;
        }
        if (scrollDragging) {
            int trackY = p2pTree.y;
            int trackH = p2pTree.height;
            int total = p2pTree.getTotalContentHeight();
            int maxScroll = Math.max(1, total - p2pTree.height);
            int thumbH = Math.max(10, (int) ((double) p2pTree.height / total * trackH));
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
    protected void mouseMovedOrUp(int mouseX, int mouseY, int mouseButton) {
        if (scrollDragging) {
            scrollDragging = false;
            return;
        }
        // 1.7.10 只有 mouseMovedOrUp，没有独立的 mouseReleased。mouseButton == -1 表示
        // “本次只是移动、并没有松开任何键”。旧代码无条件往下传，于是按住左键从按钮上
        // 划开再移动一下就会触发该按钮的动作 —— 因为 FlatButton 只看 pressed 和
        // 指针是否在矩形内，不看是哪个键松开的。
        if (mouseButton >= 0) {
            // 框选在树外松开时没有别的地方会把释放派给树，这里补一次
            if (pairBar != null) pairBar.mouseReleased(mouseX, mouseY, mouseButton);
            if (p2pTree != null) p2pTree.mouseReleased(mouseX, mouseY, mouseButton);
            if (detailPanel != null) detailPanel.mouseReleased(mouseX, mouseY, mouseButton);
            if (actionBar != null) actionBar.mouseReleased(mouseX, mouseY, mouseButton);
        }
        super.mouseMovedOrUp(mouseX, mouseY, mouseButton);
    }

    private boolean startScrollDrag(int mouseX, int mouseY) {
        int trackX = guiLeft + ConfigUiLayout.SCROLLBAR.x;
        int trackY = p2pTree.y;
        int trackH = p2pTree.height;
        if (p2pTree.getTotalContentHeight() <= p2pTree.height) return false;
        if (mouseX >= trackX && mouseX <= trackX + ConfigUiLayout.SCROLLBAR.w
            && mouseY >= trackY
            && mouseY <= trackY + trackH) {
            scrollDragging = true;
            scrollDragStartY = mouseY;
            scrollDragStartOffset = p2pTree.getScrollOffset();
            return true;
        }
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (searchField != null && searchField.isFocused()) {
            if (searchField.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
            if (keyCode == 1) { // Escape
                super.keyTyped(typedChar, keyCode);
            }
            return;
        }
        // Esc 关掉配对条下拉
        if (pairBar != null && pairBar.isDropdownOpen()) {
            if (keyCode == 1) {
                pairBar.closeDropdown();
                return;
            }
        }
        if (detailPanel != null && detailPanel.keyTyped(typedChar, keyCode)) {
            return;
        }
        // 回车 = 绑定（搜索框/详情改名框聚焦时让位给它们）
        if ((keyCode == 28 || keyCode == 156) && pairBar != null && pairBar.isReady()) {
            doBind();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
