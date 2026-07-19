package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ConfirmableTextField;
import appeng.client.gui.widgets.Scrollbar;
import com.suntide_20210418.advancedmemorycard.client.P2PRenderHelper;
import com.suntide_20210418.advancedmemorycard.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.DetailPanelWidget;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.P2PTreeWidget;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.p2p.NodeType;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import com.suntide_20210418.advancedmemorycard.p2p.TreeNode;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class ConfigModeScreen extends AEBaseScreen<ConfigModeMenu> {

    private P2PTreeWidget p2pTree;
    private Scrollbar scrollbar;
    private static final int UPDATE_ITEM_INFO_INTERVAL = 10; // 每 10 tick 发送一次
    private AETextField searchField;
    private DetailPanelWidget detailPanel;
    // 记录上次自动导航的待绑定 P2P，避免重复导航
    private P2PInfo lastNavigatedP2P = null;
    // 追踪 populateScreen 是否已完成，防止 render() 先于 init() 调用导致 VerticalButtonBar NPE
    private boolean widgetsPopulated = false;
    // update_item_info 请求的冷却计时器，避免每 tick 都发送请求
    private int updateItemInfoCooldown = 0;

    public ConfigModeScreen(ConfigModeMenu menu, Inventory playerInventory,
                            Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        initWidgets();
    }

    /**
     * 将 P2PInfo 编码为位置字符串（格式：x|y|z|sideOrdinal|dimension），供服务端 parsingP2P 解析。
     * dimension 使用完整 ResourceLocation 字符串（如 minecraft:overworld），确保跨维度定位。
     */
    private static String encodeP2PPosition(P2PInfo p2pInfo) {
        return p2pInfo.position().getX() + "|"
                + p2pInfo.position().getY() + "|"
                + p2pInfo.position().getZ() + "|"
                + p2pInfo.direction().ordinal() + "|"
                + p2pInfo.dimension().location().toString();
    }

    private void initWidgets() {
        // 如果已经创建，则直接返回（避免重复添加）
        if (this.p2pTree != null) {
            return;
        }

        // 使用 widgets 管理器创建滚动条，而不是手动创建
        scrollbar = this.widgets.addScrollBar("p2p_tree_scrollbar");
        p2pTree = new P2PTreeWidget(0, 0, 0, 0);

        // 创建搜索框
        searchField = new AETextField(this.style, this.font, 0, 0, 0, 0);
        searchField.setPlaceholder(Component.translatable(TranslateHelper.Keys.CONFIG_MODE_SCREEN_SEARCH_PLACEHOLDER));
        searchField.setBordered(false);
        searchField.setResponder(text -> {
            if (p2pTree != null) {
                p2pTree.setSearchFilter(text);
                updateScrollbar();
            }
        });
        this.widgets.add("search_field", searchField);

        // 创建并注册右侧详情面板
        detailPanel = new DetailPanelWidget(this.style, 0, 0, 0, 0);
        detailPanel.setActionCallback(new DetailPanelWidget.ActionCallback() {
            @Override
            public void onRename(P2PInfo p2pInfo, String newName) {
                if (!newName.isEmpty()) {
                    actionSetP2PAlias(p2pInfo, newName);
                    // 立即触发一次数据刷新以获取服务端确认
                    resetUpdateCooldown();
                }
            }

            @Override
            public void onBind(P2PInfo p2pInfo) {
                actionBindFrequency(p2pInfo.frequency());
            }

            @Override
            public void onSelect(P2PInfo p2pInfo) {
                actionSetPendingBind(p2pInfo);
            }

            @Override
            public void onHighlight(P2PInfo p2pInfo) {
                // 在客户端旋转玩家视角使其面向目标 P2P 设备
                actionHighlightP2P(p2pInfo);
                if (ModConfigs.getClientConfig().rotateHeadOnHighlight.get()) {
                    rotatePlayerTowardsP2P(p2pInfo);
                }
            }

            @Override
            public void onLocate(P2PInfo p2pInfo) {
                navigateToP2P(p2pInfo);
            }

            @Override
            public void onAutoAssign() {
                actionAutoConfigIO();
            }

            @Override
            public void onAssignFreq(P2PInfo p2pInfo) {
                actionAssignFreq();
            }

            // ========== 频段模式回调 ==========

            @Override
            public void onChannelRename(String frequency, String newAlias) {
                // 将频段中输入端 P2P 的名称设置为新名称
                actionSetChannelAlias(frequency, newAlias);
                // 立即触发一次数据刷新以获取服务端确认
                resetUpdateCooldown();
            }

            @Override
            public void onChannelBind(String frequency) {
                actionBindFrequency(frequency);
            }

            @Override
            public void onChannelHighlight(String frequency) {
                // 关闭界面，高亮整个频段的 P2P 通道
                actionHighlightP2PTunnel(frequency);
            }

            // ========== 通用刷新回调 ==========

            @Override
            public void onRefresh() {
                actionRefreshP2P();
                // 手动刷新后立即请求同步数据
                resetUpdateCooldown();
            }
        });
        this.widgets.add("detail_panel", detailPanel);

        // 将改名用的 text field 注册为 screen children，确保能正确获取焦点和接收鼠标/键盘事件
        for (ConfirmableTextField field : detailPanel.getEditFields()) {
            addRenderableWidget(field);
        }

        // 树节点选择监听：当用户在树中选中任意节点时触发
        p2pTree.setNodeSelectionListener(node -> {
            if (node != null) {
                // 非搜索头节点：自动定位并展开祖先节点
                if (node.type != NodeType.SEARCH_HEADER) {
                    if (node.p2pInfo != null) {
                        p2pTree.navigateToP2P(node.p2pInfo);
                    }
                    updateScrollbar();
                    syncScrollbarToTree();
                }
            }
            updateDetailPanelNode(node);
        });
        // 注册 widget（键名必须和 JSON 中一致）
        this.widgets.add("p2p_tree", p2pTree);

        updateTreeData();
    }

    private void actionAssignFreq() {
        menu.dispatchClientAction("assign_freq");
    }

    /**
     * 导航到指定的 P2P 节点，左侧树会自动展开祖先节点、选中目标并滚动到可见区域。
     * 供外部（如详情面板、快捷键等）在点击具体 P2P 条目时调用。
     */
    public void navigateToP2P(P2PInfo targetP2P) {
        if (p2pTree != null && targetP2P != null) {
            p2pTree.navigateToP2P(targetP2P);
            updateScrollbar();
            // 将树自动居中计算后的滚动位置同步到滚动条
            syncScrollbarToTree();
        }
    }

    // 修改 updateTreeData 方法
    private void updateTreeData() {
        if (p2pTree != null) {
            p2pTree.updateData(
                    menu.getClientP2PTypeInfoMap(),
                    menu.getClientChannelInfoMap(),
                    menu.getClientP2PInfoMap()
            );
            updateScrollbar();
        }
    }

    // 修改 init 方法
    @Override
    protected void init() {
        super.init();
        widgetsPopulated = true;
        updateTreeData();
    }

    // 更新滚动条
    private void updateScrollbar() {
        if (p2pTree == null || scrollbar == null) return;

        int totalHeight = p2pTree.getTotalContentHeight();
        int visibleHeight = p2pTree.getHeight();

        if (totalHeight <= visibleHeight) {
            scrollbar.setRange(0, 0, P2PTreeWidget.getRowHeight());
            scrollbar.setVisible(true);
            p2pTree.setScrollOffset(0);
        } else {
            int maxScroll = totalHeight - visibleHeight;
            scrollbar.setRange(0, maxScroll, P2PTreeWidget.getRowHeight());
            scrollbar.setVisible(true);
        }
    }

    /**
     * 更新右侧详情面板信息（基于 TreeNode）
     */
    private void updateDetailPanelNode(TreeNode node) {
        if (detailPanel != null) {
            detailPanel.setSelectedNode(
                    node,
                    menu.getClientChannelInfoMap(),
                    menu.getClientP2PTypeInfoMap()
            );
        }
    }

    // 同步滚动偏移（滚动条 → 树）
    private void syncScrollOffset() {
        if (p2pTree != null && scrollbar != null) {
            p2pTree.setScrollOffset(scrollbar.getCurrentScroll());
        }
    }

    // ==================== Action 方法（客户端→服务端通信） ====================

    // 同步滚动偏移（树 → 滚动条）
    // 用于树内部通过 scrollToSelectedNode 自动调整滚动位置后，同步到滚动条
    private void syncScrollbarToTree() {
        if (p2pTree != null && scrollbar != null) {
            scrollbar.setCurrentScroll(p2pTree.getScrollOffset());
        }
    }

    /**
     * 重置 update_item_info 冷却，使下一 tick 立即发送请求。
     * 在用户执行改名、刷新等操作后调用，确保尽快获取服务端最新数据。
     */
    private void resetUpdateCooldown() {
        updateItemInfoCooldown = 0;
    }

    /**
     * 请求刷新 P2P 数据
     */
    public void actionRefreshP2P() {
        menu.dispatchClientAction("refresh_p2p");
    }

    /**
     * 请求更新物品信息（刷新 P2P 数据并同步到客户端）
     */
    public void actionUpdateItemInfo() {
        menu.dispatchClientAction("update_item_info");
    }

    /**
     * 绑定指定频率
     */
    public void actionBindFrequency(String frequencyHex) {
        menu.dispatchClientAction("bind_frequency", frequencyHex);
    }

    /**
     * 设置频段输入端名称（参数：frequency|newName）
     * 将频段中第一个输入端 P2P 的名称设置为 newName。
     */
    public void actionSetChannelAlias(String frequency, String alias) {
        menu.dispatchClientAction("set_channel_alias", frequency + "|" + alias);
    }

    /**
     * 设置 P2P 别名
     */
    public void actionSetP2PAlias(P2PInfo p2pInfo, String alias) {
        String positionData = encodeP2PPosition(p2pInfo);
        menu.dispatchClientAction("set_p2p_alias", positionData + "::" + alias);
    }

    /**
     * 设置待绑定的 P2P
     */
    public void actionSetPendingBind(P2PInfo p2pInfo) {
        String positionData = encodeP2PPosition(p2pInfo);
        menu.dispatchClientAction("set_pending_bind", positionData);
    }

    /**
     * 自动配置 P2P 输入/输出
     */
    public void actionAutoConfigIO() {
        menu.dispatchClientAction("auto_config_io");
    }

    /**
     * 高亮指定 P2P 设备
     */
    public void actionHighlightP2P(P2PInfo p2pInfo) {
        String positionData = encodeP2PPosition(p2pInfo);
        menu.dispatchClientAction("highlight_p2p", positionData);
        // 渲染只能在客户端进行：依据已同步到客户端的 P2P 数据触发高亮
        P2PRenderHelper.renderP2P(p2pInfo, menu.getClientChannelInfoMap().get(p2pInfo.frequency()));
    }

    /**
     * 高亮指定频段的所有 P2P 通道
     */
    public void actionHighlightP2PTunnel(String frequencyHex) {
        menu.dispatchClientAction("highlight_p2p_tunnel", frequencyHex);
        // 渲染只能在客户端进行：依据已同步到客户端的 P2P 数据触发整个频段高亮
        P2PRenderHelper.renderP2PTunnel(menu.getClientChannelInfoMap().get(frequencyHex));
    }

    /**
     * 在客户端旋转玩家视角，使其面向目标 P2P 设备。
     */
    private void rotatePlayerTowardsP2P(P2PInfo p2pInfo) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        BlockPos pos = p2pInfo.position();
        double dx = pos.getX() + 0.5 - player.getX();
        double dy = pos.getY() + 0.5 - (player.getY() + player.getEyeHeight());
        double dz = pos.getZ() + 0.5 - player.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontalDist)));

        player.setYRot(yaw);
        player.setXRot(pitch);
    }

    // ==================== 生命周期 ====================

    @Override
    public void containerTick() {
        super.containerTick();

        // 使用冷却机制减少 update_item_info 请求频率
        if (updateItemInfoCooldown <= 0) {
            actionUpdateItemInfo();
            updateItemInfoCooldown = UPDATE_ITEM_INFO_INTERVAL;
        } else {
            updateItemInfoCooldown--;
        }

        updateTreeData();

        // 数据刷新后，检查是否有待绑定的 P2P 需要自动导航定位
        tryAutoNavigateToPendingBind();

        // 每 tick 同步详情面板到树中当前选中的节点（rebuildTree 不会触发 listener）
        if (p2pTree != null) {
            updateDetailPanelNode(p2pTree.getSelectedNode());
        }
        // 同步待绑定 P2P 到详情面板，供"定位"按钮使用
        if (detailPanel != null) {
            detailPanel.setPendingBindP2P(lastNavigatedP2P);
        }
    }

    /**
     * 检查客户端缓存中是否存在待绑定（isPendingBind=true）的 P2P，
     * 如果存在且与上次导航目标不同，则自动在左侧树中定位并展开到该 P2P。
     */
    private void tryAutoNavigateToPendingBind() {
        P2PInfo pendingBind = menu.getClientPendingBindP2PInfo();
        if (pendingBind == null) {
            // 没有待绑定 P2P，清除导航记录
            lastNavigatedP2P = null;
            return;
        }

        // 与上次导航目标相同则跳过，避免每 tick 重复滚动
        if (lastNavigatedP2P != null && lastNavigatedP2P.equals(pendingBind)) {
            return;
        }

        lastNavigatedP2P = pendingBind;
        navigateToP2P(pendingBind);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 确保在 render 前 init() 已执行（正常流程下 init() 总是先于 render() 调用，
        // 此处防御极端边缘情况，防止 VerticalButtonBar.updateBeforeRender() 中 position 为 null 导致 NPE）
        if (!widgetsPopulated) {
            init();
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        setTextContent("title", Component.translatable(TranslateHelper.Keys.CONFIG_MODE_SCREEN_TITLE));
        setTextContent("info",  Component.translatable(TranslateHelper.Keys.CONFIG_MODE_SCREEN_INFO));
    }

    // 判断鼠标是否在树控件可见区域内（基于屏幕绝对坐标）
    private boolean isMouseOverTreeArea(double mouseX, double mouseY) {
        if (p2pTree == null) return false;
        return mouseX >= p2pTree.getX() && mouseX <= p2pTree.getX() + p2pTree.getWidth()
                && mouseY >= p2pTree.getY() && mouseY <= p2pTree.getY() + p2pTree.getHeight();
    }

    // 判断鼠标是否在滚动条上
    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        if (scrollbar == null || !scrollbar.isVisible()) {
            return false;
        }

        Rect2i bounds = scrollbar.getBounds();
        return mouseX >= bounds.getX() &&
                mouseX <= bounds.getX() + bounds.getWidth() &&
                mouseY >= bounds.getY() &&
                mouseY <= bounds.getY() + bounds.getHeight();
    }

    // ==================== 鼠标事件处理 ====================
    // 核心思路：让 AEBaseScreen -> WidgetContainer 的事件分发链正常运作，
    // 同时补充对 Scrollbar 的正确同步。

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollDelta) {
        // 只有当鼠标在树控件区域或滚动条区域内时，才让 scrollbar 处理滚轮
        // 否则让 super 处理（AEBaseScreen 会将事件分发给 WidgetContainer，Scrollbar 因 captureMouseWheel=true 也能收到）
        if (isMouseOverTreeArea(mouseX, mouseY) || isMouseOverScrollbar(mouseX, mouseY)) {
            if (scrollbar != null && scrollbar.isVisible()) {
                Point mousePoint = new Point((int) mouseX, (int) mouseY);
                if (scrollbar.onMouseWheel(mousePoint, scrollDelta)) {
                    syncScrollOffset();
                    return true;
                }
            }
        }
        // 始终调用父类，确保 WidgetContainer 也能收到事件（例如当 captureMouseWheel=true 时）
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollDelta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 拖拽事件：先让 WidgetContainer 处理（Scrollbar 的 onMouseDrag 需要全局捕获）
        // 但如果 super 不处理，我们手动处理 Scrollbar 的拖拽
        boolean superResult = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (superResult) {
            // WidgetContainer 已处理拖拽（包括 Scrollbar），同步滚动偏移
            syncScrollOffset();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // 让 WidgetContainer 处理 Scrollbar.onMouseUp（停止拖拽状态）
        boolean superResult = super.mouseReleased(mouseX, mouseY, button);
        if (superResult) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 先让父类（WidgetContainer）处理，Scrollbar 会作为 ICompositeWidget 收到事件
        boolean superResult = super.mouseClicked(mouseX, mouseY, button);
        if (superResult) {
            // 如果 Scrollbar 消费了点击，同步滚动偏移
            if (isMouseOverScrollbar(mouseX, mouseY)) {
                syncScrollOffset();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 优先转发到详情面板的 text field（编辑模式下）
        if (detailPanel != null && detailPanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // 优先转发到详情面板的 text field（编辑模式下）
        if (detailPanel != null && detailPanel.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
}