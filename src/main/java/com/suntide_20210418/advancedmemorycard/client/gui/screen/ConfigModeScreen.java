package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ConfirmableTextField;
import appeng.client.gui.widgets.Scrollbar;
import com.suntide_20210418.advancedmemorycard.client.ClientTickTasks;
import com.suntide_20210418.advancedmemorycard.client.P2PRenderHelper;
import com.suntide_20210418.advancedmemorycard.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.ActionBarWidget;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.DetailPanelWidget;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.P2PTreeWidget;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.PairBarWidget;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.p2p.ChannelInfo;
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

import java.util.ArrayList;
import java.util.List;

public class ConfigModeScreen extends AEBaseScreen<ConfigModeMenu> {

    private P2PTreeWidget p2pTree;
    private Scrollbar scrollbar;
    private PairBarWidget pairBar;
    private ActionBarWidget actionBar;
    private static final int UPDATE_ITEM_INFO_INTERVAL = 10; // 每 10 tick 发送一次
    private AETextField searchField;
    private DetailPanelWidget detailPanel;
    // 记录上次自动导航的待绑定 P2P，避免重复导航
    private P2PInfo lastNavigatedP2P = null;
    // 追踪 populateScreen 是否已完成，防止 render() 先于 init() 调用导致 VerticalButtonBar NPE
    private boolean widgetsPopulated = false;
    // update_item_info 请求的冷却计时器，避免每 tick 都发送请求
    private int updateItemInfoCooldown = 0;

    /** 列头画在树矩形上方的偏移量。 */
    private static final int COLUMN_HEADER_OFFSET = 10;

    /**
     * 上一次处理过的同步版本号。
     *
     * <p>以前每 tick 无条件重建整棵树（20 次/秒），数据没变时纯属白烧 CPU，
     * 还会打断正在进行的选择与就地编辑。现在只在服务端确实发过新包时重建。
     */
    private int lastSyncVersion = -1;

    /** 上一次把树展开到的配对源节点 id，用于避免每次刷新都重新居中。 */
    private String lastRevealedSourceId = null;

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
                // 目标频段只是客户端选择，不需要向服务端发请求；操作条的可用态在阶段 E 接
            }

            @Override
            public void onNewFrequency() {
                menu.dispatchClientAction("assign_freq");
                resetUpdateCooldown();
            }
        });
        this.widgets.add("pair_bar", pairBar);

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

        // 创建并注册右侧检查器
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

        // 将改名用的 text field 注册为 screen children，确保能正确获取焦点和接收鼠标/键盘事件
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

    /**
     * 收到新同步包后的整体刷新：树、配对条、滚动条。
     *
     * <p>只在 {@code syncVersion} 变化时调用 —— 之前是每 tick 无条件重建树，
     * 数据没变时纯属白烧 CPU，还会打断正在进行的选择与就地编辑。
     */
    private void refreshFromMenu() {
        if (p2pTree == null) return;
        lastSyncVersion = menu.getSyncVersion();

        p2pTree.updateData(
                menu.getClientP2PTypeInfoMap(),
                menu.getClientChannelInfoMap(),
                menu.getClientP2PInfoMap());

        if (pairBar != null) {
            pairBar.setChannels(new ArrayList<>(menu.getClientChannelInfoMap().values()));
            pairBar.setStatus(menu.getClientStatusCode(), menu.getClientStatusParam());

            // 配对源换人时把树展开到那台设备并选中；已在可视区就不滚动
            P2PInfo source = menu.getClientPendingBindP2PInfo();
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
        }

        detailPanel.setSelectedNode(p2pTree.getSelectedNode(),
                menu.getClientChannelInfoMap(),
                menu.getClientP2PTypeInfoMap());

        updateScrollbar();
    }

    /**
     * 树里的 Ctrl 多选变了（拖动框选时每个拖动事件都会调一次）。
     *
     * <p>配对条必须跟着变，否则拖动过程中「已选 N 台」要等下一次同步包才更新，
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
            String targetType = pairBar.getTargetType();
            if (targetType != null && !targetType.equals(node.p2pInfo.p2pType())) {
                pairBar.clearTarget();
                updateActionBar();
            }
            // 选中设备 = 它成为配对源。只改配对目标，不动扫描锚点。
            menu.dispatchClientAction("set_pending_bind", encodeP2PPosition(node.p2pInfo));
        }
    }

    private void updateActionBar() {
        if (actionBar == null || pairBar == null || p2pTree == null) return;
        List<P2PInfo> candidates = p2pTree.getSourceCandidates();
        P2PInfo source = candidates.isEmpty() ? pairBar.getSource() : candidates.get(0);
        String name = source == null ? "" : (source.name() == null || source.name().isEmpty()
                ? source.toShortString() : source.name());
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
                payload.append(';').append(encodeP2PPosition(info));
            }
            menu.dispatchClientAction("bind_frequency_batch", payload.toString());
        } else {
            menu.dispatchClientAction("bind_frequency", frequency);
        }
        // 绑定后刻意不清空目标：连续把几台设备绑到同一个频段是常见操作
        updateActionBar();
        resetUpdateCooldown();
        // 设备换到目标频段那一组后行会“跳走”，清掉跟随记录让下一次同步重新展开到它
        lastRevealedSourceId = null;
    }

    // ==================== 高亮 / 定位 / 传送 ====================

    /**
     * 高亮 + 定位（合并后的单一动作）。
     *
     * <p>顺序刻意固定为「发包并点亮 → 关界面 → 转头」：发包必须在关界面之前，
     * 否则服务端不再接受该 menu 的动作包，聊天栏位置信息会丢；关界面要在下一帧前完成；
     * 转头放最后，因为它改的是玩家朝向，界面没关时会被 GUI 的鼠标捕获抵消。
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

    /** 传送后等落点同步的最长 tick 数（5 秒）。 */
    private static final int TELEPORT_ARRIVAL_TIMEOUT_TICKS = 100;

    /** 落点判定半径的平方：玩家与设备的方块中心在 2 格内即认为已经站到了目标处。 */
    private static final double TELEPORT_ARRIVAL_DIST_SQR = 4.0;

    /**
     * 传送到目标设备。
     *
     * <p>真正的位移由服务端执行：客户端不可信，而且跨维度传送必须走服务端。
     * 这里只发位置键，然后关掉界面；点头与高亮要等「真的落地」再做 ——
     * 否则坐标还是旧的，跨维度时新维度的方块实体也还没加载。
     */
    private void doTeleport(P2PInfo info) {
        if (info == null) return;
        // 界关掉之后就取不到 menu 的镜像数据了，频段信息先抓在手里
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
     * 就完全不知道自己到了哪。而 {@code rotateHeadOnHighlight} 语义上是「高亮时别掰我的头」，
     * 不覆盖传送这种玩家主动发起的位移。
     */
    private void faceP2P(P2PInfo p2pInfo) {
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

        // 只在服务端发过新包时重建：syncVersion 每收一个同步包 +1
        if (menu.getSyncVersion() != lastSyncVersion) {
            refreshFromMenu();
        }

        // 数据刷新后，检查是否有待绑定的 P2P 需要自动导航定位
        tryAutoNavigateToPendingBind();
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

        // 列头与浮层必须画在所有 widget 之后，否则会被树/面板盖住
        if (p2pTree != null) {
            p2pTree.renderColumnHeader(guiGraphics, p2pTree.getX(), p2pTree.getY() - COLUMN_HEADER_OFFSET);
        }
        if (pairBar != null) {
            pairBar.renderOverlay(guiGraphics, mouseX, mouseY);
        }

        setTextContent("title", Component.translatable(TranslateHelper.Keys.CONFIG_MODE_SCREEN_TITLE));
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 浮层优先消费：否则会出现「浮层滚一格、树也滚一格」
        if (pairBar != null && pairBar.isDropdownOpen() && pairBar.handleOverlayScroll(delta)) {
            return true;
        }
        // 只有当鼠标在树控件区域或滚动条区域内时，才让 scrollbar 处理滚轮
        // 否则让 super 处理（AEBaseScreen 会将事件分发给 WidgetContainer，Scrollbar 因 captureMouseWheel=true 也能收到）
        if (isMouseOverTreeArea(mouseX, mouseY) || isMouseOverScrollbar(mouseX, mouseY)) {
            if (scrollbar != null && scrollbar.isVisible()) {
                Point mousePoint = new Point((int) mouseX, (int) mouseY);
                if (scrollbar.onMouseWheel(mousePoint, delta)) {
                    syncScrollOffset();
                    return true;
                }
            }
        }
        // 始终调用父类，确保 WidgetContainer 也能收到事件（例如当 captureMouseWheel=true 时）
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 框选时指针可能移出树矩形，WidgetContainer 就不会再把拖动派给树；
        // 这里先自行转发一次，让选择范围能跟着指针越界延伸。
        if (p2pTree != null && p2pTree.isDragSelecting()) {
            p2pTree.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true;
        }
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
        // 框选在树外松开时 super 不会派发，这里补一次，否则树会一直停“正在框选”
        if (p2pTree != null && p2pTree.isDragSelecting()) {
            p2pTree.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        // 让 WidgetContainer 处理 Scrollbar.onMouseUp（停止拖拽状态）
        boolean superResult = super.mouseReleased(mouseX, mouseY, button);
        if (superResult) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 浮层优先：它画在最上层，也必须最先吃掉点击，否则点到的是下面的树
        if (pairBar != null && pairBar.isDropdownOpen()
                && pairBar.handleOverlayClick(mouseX, mouseY, button)) {
            return true;
        }
        // AE2 的 AEBaseScreen.mouseClicked 在 button==1 且命中原版 child 时会把按键改写成 0
        // 再派发。树区域目前没有 child，但不要让“右键能到树”依赖这个前提：
        // 批量模式下的「右键展开/折叠频段」靠它，一旦将来在树上方叠任何原版控件就会静默失效。
        if (button == 1 && p2pTree != null && isMouseOverTreeArea(mouseX, mouseY)) {
            if (p2pTree.mouseClicked(mouseX, mouseY, 1)) {
                return true;
            }
        }
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
        // Esc 先关下拉浮层（256 = GLFW_KEY_ESCAPE），再考虑交给别的控件
        if (keyCode == 256 && pairBar != null && pairBar.isDropdownOpen()) {
            pairBar.closeDropdown();
            return true;
        }
        // 优先转发到详情面板的 text field（编辑模式下）
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
        // 优先转发到详情面板的 text field（编辑模式下）
        if (detailPanel != null && detailPanel.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
}