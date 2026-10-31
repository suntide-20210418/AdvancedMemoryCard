package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ConfirmableTextField;
import appeng.client.gui.widgets.Scrollbar;
import com.suntide_20210418.advancedmemorycard.client.ClientTickTasks;
import com.suntide_20210418.advancedmemorycard.client.P2PRenderHelper;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.ActionBarWidget;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.DetailPanelWidget;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.P2PTreeWidget;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.PairBarWidget;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.p2p.ChannelInfo;
import com.suntide_20210418.advancedmemorycard.p2p.NodeType;
import com.suntide_20210418.advancedmemorycard.p2p.P2PInfo;
import com.suntide_20210418.advancedmemorycard.p2p.TreeNode;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * P2P 配置界面。
 *
 * <p>版面（440×312，全部取自 assets/ae2/textures/guis/config_mode_menu.png 的实际像素）：
 * <pre>
 *   0..19    标题
 *   20..52   配对条（源 → 目标）
 *   53..71   状态行
 *   72..283  主面板
 *              76..90   搜索
 *              90..101  列头
 *              102..276 树（右侧留滚动条）
 *              75..282  检查器
 *   288..305 底部操作条
 * </pre>
 */
public class ConfigModeScreen extends AEBaseScreen<ConfigModeMenu> {

    private static final int UPDATE_ITEM_INFO_INTERVAL = 10;

    /** 列头画在树区上方 10px 的空带里 */
    private static final int COLUMN_HEADER_OFFSET = 10;

    private P2PTreeWidget p2pTree;
    private Scrollbar scrollbar;
    private AETextField searchField;
    private DetailPanelWidget detailPanel;
    private PairBarWidget pairBar;
    private ActionBarWidget actionBar;

    private boolean widgetsPopulated = false;
    private int updateItemInfoCooldown = 0;
    private int lastSyncVersion = -1;
    /** 上一次自动跟随过的配对源（稳定 id）。用 id 而非 P2PInfo.equals，避免连接状态一变就重新跳转。 */
    private String lastRevealedSourceId;

    public ConfigModeScreen(ConfigModeMenu menu, Inventory playerInventory,
                            Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        initWidgets();
    }

    private static String encodeP2PPosition(P2PInfo p2pInfo) {
        return p2pInfo.position().getX() + "|"
                + p2pInfo.position().getY() + "|"
                + p2pInfo.position().getZ() + "|"
                + p2pInfo.direction().ordinal() + "|"
                + p2pInfo.dimension().location().toString();
    }

    private void initWidgets() {
        if (this.p2pTree != null) return;

        scrollbar = this.widgets.addScrollBar("p2p_tree_scrollbar");
        p2pTree = new P2PTreeWidget(0, 0, 0, 0);

        pairBar = new PairBarWidget(0, 0, 0, 0);
        pairBar.setCallback(new PairBarWidget.Callback() {
            @Override
            public void onSourceCleared() {
                if (p2pTree != null) {
                    p2pTree.clearMultiSelection();
                }
                menu.dispatchClientAction("clear_pending_bind");
                resetUpdateCooldown();
            }

            @Override
            public void onTargetChanged(String frequency) {
                updateActionBar();
            }

            @Override
            public void onNewFrequency() {
                menu.dispatchClientAction("assign_freq");
                resetUpdateCooldown();
            }
        });
        this.widgets.add("pair_bar", pairBar);

        searchField = new SearchFieldWidget(this.style, this.font, 0, 0, 0, 0);
        searchField.setPlaceholder(Component.translatable(TranslateHelper.Keys.CONFIG_MODE_SCREEN_SEARCH_PLACEHOLDER));
        searchField.setBordered(false);
        searchField.setResponder(text -> {
            if (p2pTree != null) {
                p2pTree.setSearchFilter(text);
                updateScrollbar();
            }
        });
        this.widgets.add("search_field", searchField);

        detailPanel = new DetailPanelWidget(this.style, 0, 0, 0, 0);
        detailPanel.setActionCallback(new DetailPanelWidget.ActionCallback() {
            @Override
            public void onHighlightP2P(P2PInfo info) {
                doHighlightP2P(info);
            }

            @Override
            public void onHighlightChannel(String frequency) {
                doHighlightChannel(frequency);
            }

            @Override
            public void onTeleportP2P(P2PInfo info) {
                doTeleport(info);
            }

            @Override
            public void onUseAsTarget(String frequency) {
                ChannelInfo info = menu.getClientChannelInfoMap().get(frequency);
                if (info != null) {
                    pairBar.pickChannel(info);
                    updateActionBar();
                }
            }

            @Override
            public void onAssignFreq() {
                menu.dispatchClientAction("assign_freq");
                resetUpdateCooldown();
            }

            @Override
            public void onSwitchToInput(P2PInfo info) {
                menu.dispatchClientAction("switch_to_input", encodeP2PPosition(info));
                resetUpdateCooldown();
            }

            @Override
            public void onInitP2P() {
                menu.dispatchClientAction("auto_config_io");
                resetUpdateCooldown();
            }

            @Override
            public void onRenameP2P(P2PInfo info, String name) {
                menu.dispatchClientAction("set_p2p_alias", encodeP2PPosition(info) + "::" + name);
                resetUpdateCooldown();
            }

            @Override
            public void onRenameChannel(String frequency, String alias) {
                menu.dispatchClientAction("set_channel_alias", frequency + "|" + alias);
                resetUpdateCooldown();
            }

            @Override
            public void onRefresh() {
                menu.dispatchClientAction("refresh_p2p");
                resetUpdateCooldown();
            }
        });
        this.widgets.add("detail_panel", detailPanel);
        for (ConfirmableTextField field : detailPanel.getEditFields()) {
            addRenderableWidget(field);
        }

        actionBar = new ActionBarWidget(0, 0, 0, 0);
        actionBar.setOnBind(this::doBind);
        actionBar.setOnBatchToggle(this::toggleBatchMode);
        this.widgets.add("action_bar", actionBar);

        p2pTree.setNodeSelectionListener(this::onNodeSelected);
        p2pTree.setMultiSelectionChangedListener(this::onTreeMultiSelectionChanged);
        p2pTree.setBatchModeChangedListener(this::onBatchModeChanged);
        this.widgets.add("p2p_tree", p2pTree);

        refreshFromMenu();
    }

    /**
     * 树里的 Ctrl 多选变了（拖动框选时每个拖动事件都会调一次）。
     *
     * <p>配对条必须跟着变，否则拖动过程中「已选 N 台」要等下一次同步包（2 次/秒）才更新，
     * 松手时会看到数字慢半拍再跳一下。
     */
    private void onTreeMultiSelectionChanged() {
        if (pairBar == null || p2pTree == null) return;
        List<P2PInfo> candidates = p2pTree.getSourceCandidates();
        if (candidates.isEmpty()) {
            P2PInfo serverSource = menu.getClientPendingBindP2PInfo();
            pairBar.setSources(serverSource == null ? List.of() : List.of(serverSource));
        } else {
            pairBar.setSources(candidates);
        }
        updateActionBar();
    }

    /**
     * 底部「批量操作」按钮：进 / 出批量模式。
     *
     * <p>退出时**不**清掉多选集 —— 批量选完正好接着挑目标频段绑定，
     * 模式只决定「单击算不算加选」，与选中的内容无关。
     */
    private void toggleBatchMode() {
        if (p2pTree == null) return;
        p2pTree.setBatchMode(!p2pTree.isBatchMode());
    }

    /** 树的批量模式变了（可能是按钮点的，也可能是 Ctrl 点了非设备行）：按钮文字跟着改。 */
    private void onBatchModeChanged(boolean active) {
        if (actionBar != null) {
            actionBar.setBatchActive(active);
        }
    }

    private void onNodeSelected(TreeNode node) {
        detailPanel.setSelectedNode(node,
                menu.getClientChannelInfoMap(),
                menu.getClientP2PTypeInfoMap());

        if (node == null) return;

        if (node.type == NodeType.CHANNEL && node.data instanceof ChannelInfo info) {
            // 选中频段行 = 把它填进目标槽，这是配对的主路径。
            // 类型取“它在树里挂的那个分组”，而不是 ChannelInfo 里的第一台设备类型。
            String groupType = node.parent != null ? node.parent.typeName : null;
            pairBar.pickChannel(info, groupType);
            updateActionBar();
        } else if (node.type == NodeType.P2P && node.p2pInfo != null) {
            // 换配对源时保留已选目标频段 —— 前提是类型没变。
            //
            // 判断依据取「目标频段自己属于哪个类型」，而不是上一台源设备：
            // 源设备要等服务端回包才会更新，连点时读到的还是旧值，于是会把
            // 同类型的切换误判成跨类型。目标类型在选中那一刻就确定了，不会有这个时序问题。
            String targetType = pairBar.getTargetType();
            if (targetType != null && !targetType.equals(node.p2pInfo.p2pType())) {
                pairBar.clearTarget();
                updateActionBar();
            }
            // 选中设备 = 它成为配对源。只改配对目标，不动扫描锚点。
            menu.dispatchClientAction("set_pending_bind", encodeP2PPosition(node.p2pInfo));
        }
    }

    // ==================== 数据同步 ====================

    private void refreshFromMenu() {
        if (p2pTree == null) return;
        lastSyncVersion = menu.getSyncVersion();

        p2pTree.updateData(
                menu.getClientP2PTypeInfoMap(),
                menu.getClientChannelInfoMap(),
                menu.getClientP2PInfoMap());

        pairBar.setChannels(new ArrayList<>(menu.getClientChannelInfoMap().values()));
        pairBar.setStatus(menu.getClientStatusCode(), menu.getClientStatusParam());

        P2PInfo source = menu.getClientPendingBindP2PInfo();

        // 配对源换人时，把树展开到那台设备并选中；已经在可视区里就不滚动。
        if (source != null) {
            String sourceId = TreeNode.p2pNodeId(source);
            if (!sourceId.equals(lastRevealedSourceId)) {
                lastRevealedSourceId = sourceId;
                p2pTree.revealP2P(source);
            }
        }

        // 配对源以树里的选择为准（可能是 Ctrl 多选的一组）；
        // 还没在树里点过时，退回“打开界面时右键的那一台”。
        List<P2PInfo> candidates = p2pTree.getSourceCandidates();
        if (candidates.isEmpty()) {
            P2PInfo serverSource = menu.getClientPendingBindP2PInfo();
            pairBar.setSources(serverSource == null ? List.of() : List.of(serverSource));
        } else {
            pairBar.setSources(candidates);
        }

        detailPanel.setSelectedNode(p2pTree.getSelectedNode(),
                menu.getClientChannelInfoMap(),
                menu.getClientP2PTypeInfoMap());

        updateScrollbar();
        updateActionBar();
    }

    private void updateActionBar() {
        if (actionBar == null || pairBar == null) return;
        List<P2PInfo> candidates = p2pTree.getSourceCandidates();
        P2PInfo source = candidates.isEmpty() ? pairBar.getSource()
                : candidates.get(0);
        String name = source == null ? "" : (source.name() == null || source.name().isEmpty()
                ? source.toShortString() : source.name());
        actionBar.setReady(pairBar.isReady(), name, pairBar.getTargetFrequency(),
                candidates.size());
    }

    private void updateScrollbar() {
        if (p2pTree == null || scrollbar == null) return;
        int totalHeight = p2pTree.getTotalContentHeight();
        int visibleHeight = p2pTree.getHeight();
        if (totalHeight <= visibleHeight) {
            scrollbar.setRange(0, 0, P2PTreeWidget.getRowHeight());
            scrollbar.setCurrentScroll(0);
            p2pTree.setScrollOffset(0);
        } else {
            scrollbar.setRange(0, totalHeight - visibleHeight, P2PTreeWidget.getRowHeight());
        }
        scrollbar.setVisible(true);
        // 树内部可能做过自动滚动（跟随选中），这里统一把 thumb 拉回树的真实偏移
        scrollbar.setCurrentScroll(p2pTree.getScrollOffset());
    }

    private void resetUpdateCooldown() {
        updateItemInfoCooldown = 0;
    }

    // ==================== 动作 ====================

    private void doBind() {
        if (pairBar == null || !pairBar.isReady()) return;
        String frequency = pairBar.getTargetFrequency();
        List<P2PInfo> candidates = p2pTree.getSourceCandidates();
        if (candidates.size() > 1) {
            // 多选批量绑定：payload = "频段;位置1;位置2;…"
            StringBuilder payload = new StringBuilder(frequency);
            for (P2PInfo info : candidates) {
                payload.append(';').append(encodeP2PPosition(info));
            }
            menu.dispatchClientAction("bind_frequency_batch", payload.toString());
        } else {
            menu.dispatchClientAction("bind_frequency", frequency);
        }
        // 绑定后刻意不清空目标：连续把几台设备绑到同一个频段是常见操作，
        // 每绑一次都要重选一遍纯属折腾。
        updateActionBar();
        resetUpdateCooldown();
        // 设备换到目标频段那一组后行会“跳走”，清掉跟随记录让下一次同步重新展开到它。
        lastRevealedSourceId = null;
    }

    private void actionHighlightP2P(P2PInfo p2pInfo) {
        menu.dispatchClientAction("highlight_p2p", encodeP2PPosition(p2pInfo));
        P2PRenderHelper.renderP2P(p2pInfo, menu.getClientChannelInfoMap().get(p2pInfo.frequency()));
    }

    private void actionHighlightP2PTunnel(String frequencyHex) {
        menu.dispatchClientAction("highlight_p2p_tunnel", frequencyHex);
        P2PRenderHelper.renderP2PTunnel(menu.getClientChannelInfoMap().get(frequencyHex));
    }

    // ==================== 高亮 / 定位 / 传送 ====================

    /**
     * 高亮 + 定位（合并后的单一动作）。
     *
     * <p>顺序刻意固定为「发包并点亮 → 关界面 → 转头」：
     * <ul>
     *   <li>发包必须在关界面之前：{@code setScreen(null)} 会立刻发出容器关闭包，
     *       服务端随后就不再接受这个 menu 的动作包，聊天栏的位置信息会丢；</li>
     *   <li>关界面必须在下一帧渲染前完成，否则会看到“界面还在、世界已被点亮”；</li>
     *   <li>转头放最后，因为它改的是玩家朝向，界面没关时会被 GUI 的鼠标捕获抵消。</li>
     * </ul>
     * 转头仍然受客户端配置 {@code highlight.rotateHeadOnHighlight} 约束 —— 关掉它的玩家
     * 只想看高亮，不想被强行掰头。
     */
    private void doHighlightP2P(P2PInfo info) {
        if (info == null) return;
        actionHighlightP2P(info);
        Minecraft.getInstance().setScreen(null);
        rotatePlayerTowardsP2P(info);
    }

    private void doHighlightChannel(String frequency) {
        if (frequency == null) return;
        actionHighlightP2PTunnel(frequency);
        Minecraft.getInstance().setScreen(null);
        P2PInfo firstInput = firstInputOfChannel(frequency);
        if (firstInput != null) {
            rotatePlayerTowardsP2P(firstInput);
        }
    }

    /** 传送后等落点同步的最长 tick 数（5 秒）：跨维度换维度 + 加载区块都在这个窗口里。 */
    private static final int TELEPORT_ARRIVAL_TIMEOUT_TICKS = 100;

    /** 落点判定半径的平方：玩家与设备的方块中心在 2 格内即认为已经站到了目标处。 */
    private static final double TELEPORT_ARRIVAL_DIST_SQR = 4.0;

    /**
     * 传送到目标设备。
     *
     * <p>真正的位移由服务端执行：客户端不可信，而且跨维度传送必须走服务端
     * （{@code ClientLevel} 里没有别维度的实体）。这里只发位置键，然后关掉界面 ——
     * 否则传送完成后玩家还对着 GUI，看不到自己已经到了哪。
     *
     * <p>点头与高亮都要等「真的落地」再做，不能紧跟发送动作包：那时坐标还是旧的，
     * 算出来的朝向对着旧位置；跨维度时新维度的方块实体还没加载，渲染也拿不到部件。
     * 所以交给 {@link ClientTickTasks} 每 tick 探一次，落地了再点亮 + 转头。
     */
    private void doTeleport(P2PInfo info) {
        if (info == null) return;
        // 界关掉之后就拿不到 menu 的镜像数据了，频段信息先抓在手里
        ChannelInfo channelInfo = menu.getClientChannelInfoMap().get(info.frequency());
        menu.dispatchClientAction("teleport_p2p", encodeP2PPosition(info));
        Minecraft.getInstance().setScreen(null);

        ClientTickTasks.when(mc -> hasArrivedAt(mc, info), () -> {
            P2PRenderHelper.renderP2P(info, channelInfo);
            faceP2P(info);
        }, TELEPORT_ARRIVAL_TIMEOUT_TICKS);
    }

    private static boolean hasArrivedAt(Minecraft mc, P2PInfo info) {
        if (mc.player == null || mc.level == null) {
            return false;
        }
        // 跨维度传送时新维度还在加载，此时 level 仍是旧维度，不能拿旧世界的方块实体去点亮
        if (info.dimension() != null && !mc.level.dimension().equals(info.dimension())) {
            return false;
        }
        return mc.player.blockPosition().distSqr(info.position()) <= TELEPORT_ARRIVAL_DIST_SQR;
    }

    /** 频段节点没有“自己”的位置：取其第一台输入端设备。 */
    private P2PInfo firstInputOfChannel(String frequency) {
        if (frequency == null) return null;
        ChannelInfo info = menu.getClientChannelInfoMap().get(frequency);
        if (info == null || info.p2pInfoList() == null) return null;
        for (P2PInfo p : info.p2pInfoList()) {
            if (!p.isOutput()) return p;
        }
        return null;
    }

    /**
     * 高亮后的转头：受 {@code rotateHeadOnHighlight} 约束 —— 关掉它的玩家只想看高亮，不想被强行掰头。
     */
    private void rotatePlayerTowardsP2P(P2PInfo p2pInfo) {
        if (!ModConfigs.getClientConfig().rotateHeadOnHighlight.get()) return;
        faceP2P(p2pInfo);
    }

    /**
     * 把视角转向目标设备，不看配置。
     *
     * <p>传送必须用它：玩家是被服务端挪过去的，落点贴到设备身上时如果还保持原来的朝向，
     * 就完全不知道自己到了哪、看到的又是哪。而 {@code rotateHeadOnHighlight} 语义上是
     * 「高亮时别掰我的头」，不覆盖传送这种玩家主动发起的位移。
     */
    private void faceP2P(P2PInfo p2pInfo) {
        if (p2pInfo == null) return;
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
    protected void init() {
        super.init();
        widgetsPopulated = true;
        refreshFromMenu();
    }

    @Override
    public void containerTick() {
        super.containerTick();

        if (updateItemInfoCooldown <= 0) {
            menu.dispatchClientAction("update_item_info");
            updateItemInfoCooldown = UPDATE_ITEM_INFO_INTERVAL;
        } else {
            updateItemInfoCooldown--;
        }

        // 只在收到新同步包时重建：旧版每 tick 调一次 updateData → rebuildTree，
        // 20 次/秒重建整棵树并丢弃全部 TreeNode，纯属白烧 CPU 和 GC。
        if (menu.getSyncVersion() != lastSyncVersion) {
            refreshFromMenu();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        if (!widgetsPopulated) init();
        super.render(g, mouseX, mouseY, partialTicks);

        // 列头与浮层都在所有 widget 之后画，保证不被树/面板盖住
        if (p2pTree != null) {
            p2pTree.renderColumnHeader(g, p2pTree.getX(), p2pTree.getY() - COLUMN_HEADER_OFFSET);
        }
        if (pairBar != null) {
            pairBar.renderOverlay(g, mouseX, mouseY);
        }

        setTextContent("title", Component.translatable(TranslateHelper.Keys.CONFIG_MODE_SCREEN_TITLE));
    }

    // ==================== 输入 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 浮层优先：它盖在树和面板之上
        if (pairBar != null && pairBar.handleOverlayClick(mouseX, mouseY, button)) {
            return true;
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled && isMouseOverScrollbar(mouseX, mouseY)) {
            syncScrollOffset();
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollDelta) {
        if (pairBar != null && pairBar.isDropdownOpen() && pairBar.handleOverlayScroll(scrollDelta)) {
            return true;
        }
        if (isMouseOverTreeArea(mouseX, mouseY) || isMouseOverScrollbar(mouseX, mouseY)) {
            if (scrollbar != null && scrollbar.isVisible()) {
                Point point = new Point((int) mouseX, (int) mouseY);
                if (scrollbar.onMouseWheel(point, scrollDelta)) {
                    syncScrollOffset();
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollDelta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 框选拖动自己转发一次：指针划出树的矩形之后，屏幕的焦点分发不一定还落得到树上，
        // 而框选恰恰要求指针越过边界继续往首行 / 末行延伸。
        if (p2pTree != null && p2pTree.isDragSelecting()
                && p2pTree.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        boolean handled = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        if (handled) {
            syncScrollOffset();
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (pairBar != null && pairBar.isDropdownOpen() && keyCode == 256) {
            pairBar.closeDropdown();
            return true;
        }
        if (detailPanel != null && detailPanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        // 回车 = 提交配对。但搜索框有焦点时不能抢：否则打字回车会误触发绑定。
        boolean typing = (searchField != null && searchField.isFocused())
                || (detailPanel != null && detailPanel.isEditing());
        if (!typing && (keyCode == 257 || keyCode == 335) && pairBar != null && pairBar.isReady()) {
            doBind();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (detailPanel != null && detailPanel.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private boolean isMouseOverTreeArea(double mouseX, double mouseY) {
        if (p2pTree == null) return false;
        return mouseX >= p2pTree.getX() && mouseX <= p2pTree.getX() + p2pTree.getWidth()
                && mouseY >= p2pTree.getY() && mouseY <= p2pTree.getY() + p2pTree.getHeight();
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        if (scrollbar == null || !scrollbar.isVisible()) return false;
        Rect2i bounds = scrollbar.getBounds();
        return mouseX >= bounds.getX() && mouseX <= bounds.getX() + bounds.getWidth()
                && mouseY >= bounds.getY() && mouseY <= bounds.getY() + bounds.getHeight();
    }

    private void syncScrollOffset() {
        if (p2pTree != null && scrollbar != null) {
            p2pTree.setScrollOffset(scrollbar.getCurrentScroll());
        }
    }

    /** 搜索框自带底板：新贴图里没有单独的搜索框凹槽，靠 widget 自己画一层底色。 */
    private static class SearchFieldWidget extends AETextField {
        SearchFieldWidget(ScreenStyle style, Font font, int x, int y, int width, int height) {
            super(style, font, x, y, width, height);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int surface = ModConfigs.getClientConfig().paletteSurface.get();
            int border = ModConfigs.getClientConfig().paletteDivider.get();
            // 只向上扩 1px：再高就会盖住主面板顶部的深色分隔线
            g.fill(getX() - 2, getY() - 1, getX() + width + 2, getY() + height + 1, surface);
            g.fill(getX() - 2, getY() + height + 1, getX() + width + 2, getY() + height + 2, border);
            super.renderWidget(g, mouseX, mouseY, partialTick);
        }
    }
}
