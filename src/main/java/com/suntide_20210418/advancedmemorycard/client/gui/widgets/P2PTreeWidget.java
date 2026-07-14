package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.p2p.*;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.*;
import java.util.function.Consumer;

/**
 * P2P树形控件<br/>
 * 显示 P2P类型 -> 频段 -> P2P 的树形结构
 */
public class P2PTreeWidget extends AbstractWidget {

    // 行高（通过配置动态读取，保留静态常量以兼容外部引用）
    private static final int ROW_HEIGHT = 12;
    public P2PTreeWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public static int getRowHeight() {
        return ModConfigs.getClientConfig().treeRowHeight.get();
    }

    // 颜色从配置动态读取
    private int getColorBg() { return ModConfigs.getClientConfig().treeColorBg.get(); }

    private int getColorSelected() { return ModConfigs.getClientConfig().treeColorSelected.get(); }

    private int getColorHover() { return ModConfigs.getClientConfig().treeColorHover.get(); }

    // 布局从配置动态读取
    private int getIndentWidth() { return ModConfigs.getClientConfig().treeIndentWidth.get(); }

    private int getIconTextSpacing() { return ModConfigs.getClientConfig().treeIconTextSpacing.get(); }

    private int getExpandIconWidth() { return ModConfigs.getClientConfig().treeExpandIconWidth.get(); }
    // 数据源
    private Map<String, P2PTypeInfo> p2pTypeInfoMap;
    // 持久化展开状态缓存：即使父节点收起，子节点的展开状态也会被保留，
    // 下次展开父节点时自动恢复子节点的原有展开状态。
    private final Map<String, Boolean> expandedStateCache = new HashMap<>();
    private Map<P2PPosition, P2PInfo> p2pInfoMap;

    // 树节点列表（平铺展示）
    private List<TreeNode> flatNodes = new ArrayList<>();

    private int currentScrollOffset = 0;
    private int totalContentHeight = 0;

    // 选中项
    private TreeNode selectedNode = null;
    private P2PInfo selectedP2PInfo = null;
    private Map<String, ChannelInfo> channelInfoMap;
    // 选择监听器（仅 P2P 节点选中时回调）
    private Consumer<P2PInfo> selectionListener;

    // 悬停项
    private TreeNode hoveredNode = null;
    // 节点选择监听器（所有节点类型选中时回调，包括 P2P_TYPE、CHANNEL、P2P）
    private Consumer<TreeNode> nodeSelectionListener;
    // 搜索相关
    private String currentSearchQuery = null;
    private boolean isSearchMode = false;
    private int searchResultCount = 0;
    // 导航定位相关
    private P2PInfo pendingNavigateTarget = null;
    private boolean scrollToSelected = false;
    // 重入防护：防止 navigateToP2P -> rebuildTree -> restoreSelectedNode -> listener -> navigateToP2P 的无限递归
    private boolean isNavigating = false;
    // 重建标志：rebuildTree 期间抑制 listener 通知，防止重建过程中不必要的 navigateToP2P 触发滚动重置
    private boolean isRebuilding = false;

    private int getChannelWarningThreshold() { return ModConfigs.getClientConfig().treeChannelWarningThreshold.get(); }

    private int getP2PNameMaxLength() { return ModConfigs.getClientConfig().treeP2PNameMaxLength.get(); }

    /**
     * 更新数据并重建树
     */
    public void updateData(Map<String, P2PTypeInfo> p2pTypeInfoMap,
                           Map<String, ChannelInfo> channelInfoMap,
                           Map<P2PPosition, P2PInfo> p2pInfoMap) {
        this.p2pTypeInfoMap = p2pTypeInfoMap != null ? p2pTypeInfoMap : new HashMap<>();
        this.channelInfoMap = channelInfoMap != null ? channelInfoMap : new HashMap<>();
        this.p2pInfoMap = p2pInfoMap != null ? p2pInfoMap : new HashMap<>();

        rebuildTree();
    }

    /**
     * 设置选择监听器（仅 P2P 节点选中时回调）
     */
    public void setSelectionListener(Consumer<P2PInfo> listener) {
        this.selectionListener = listener;
    }

    /**
     * 设置节点选择监听器（所有节点类型选中时回调）
     */
    public void setNodeSelectionListener(Consumer<TreeNode> listener) {
        this.nodeSelectionListener = listener;
    }

    /**
     * 导航到指定的 P2P 节点，自动展开父节点、选中目标并滚动到可见区域。
     * 当外部需要将树视图聚焦到某个特定 P2P 时调用此方法（例如从详情面板点击某个 P2P 条目）。
     */
    public void navigateToP2P(P2PInfo targetP2P) {
        if (targetP2P == null || isNavigating) return;
        this.pendingNavigateTarget = targetP2P;
        this.scrollToSelected = true;

        // 退出搜索模式，切换到默认树视图以便定位
        if (isSearchMode) {
            isSearchMode = false;
            currentSearchQuery = null;
        }

        isNavigating = true;
        try {
            rebuildTree();
        } finally {
            isNavigating = false;
        }
    }

    /**
     * 重建树结构（保留展开和选中状态）
     */
    private void rebuildTree() {
        // 保存当前展开状态
        Set<String> expandedTypeIds = new HashSet<>();
        saveExpandedState(expandedTypeIds);

        // 保存当前选中的节点ID
        String selectedNodeId = getSelectedNodeId();

        // 确定需要确保祖先展开的目标 P2P
        //   优先使用外部导航目标，其次使用当前已选中的 P2P 节点
        P2PInfo targetP2P = pendingNavigateTarget;
        if (targetP2P == null
                && selectedNode != null
                && selectedNode.type == NodeType.P2P
                && selectedNode.p2pInfo != null) {
            targetP2P = selectedNode.p2pInfo;
        }

        // 强制展开目标 P2P 的祖先节点，确保其在重建后可见
        if (targetP2P != null) {
            expandedTypeIds.add("type:" + targetP2P.p2pType());
            expandedTypeIds.add("channel:" + targetP2P.frequency());
            // 同步更新持久化缓存，确保导航触发的展开状态也被记忆
            expandedStateCache.put("type:" + targetP2P.p2pType(), true);
            expandedStateCache.put("channel:" + targetP2P.frequency(), true);
        }

        // 如果存在待导航目标，覆盖选中节点 ID 以确保导航到该目标
        if (pendingNavigateTarget != null) {
            selectedNodeId = "p2p:" + pendingNavigateTarget.hashCode()
                    + ":" + pendingNavigateTarget.frequency();
        }

        // 重建树
        flatNodes.clear();

        // 搜索模式
        if (isSearchMode && currentSearchQuery != null) {
            // 添加搜索结果头
            TreeNode headerNode = new TreeNode();
            headerNode.type = NodeType.SEARCH_HEADER;
            headerNode.displayComponent = formatSearchHeader();
            flatNodes.add(headerNode);

            // 构建搜索结果
            buildSearchResults(currentSearchQuery);
        } else {
            // 默认树
            buildDefaultTree(expandedTypeIds);
        }

        // 恢复选中状态（抑制 listener 通知，避免重建过程中不必要的 navigateToP2P 触发滚动重置）
        isRebuilding = true;
        try {
            restoreSelectedNode(selectedNodeId);
        } finally {
            isRebuilding = false;
        }

        // 导航定位完成后滚动视图使选中节点居中可见
        if (scrollToSelected && selectedNode != null) {
            scrollToSelectedNode();
            scrollToSelected = false;
        }

        // 清除一次性导航目标
        pendingNavigateTarget = null;
    }

    /**
     * 滚动视图使当前选中的节点处于可视区域中部
     */
    private void scrollToSelectedNode() {
        if (selectedNode == null) return;

        int index = flatNodes.indexOf(selectedNode);
        if (index < 0) return;

        int nodeY = index * getRowHeight();
        int visibleHeight = getHeight();

        // 将选中节点置于可见区域中部偏上
        int targetScroll = nodeY - visibleHeight / 2 + getRowHeight() / 2;
        targetScroll = Math.max(0,
                Math.min(targetScroll, Math.max(0, getTotalContentHeight() - visibleHeight)));

        this.currentScrollOffset = targetScroll;
    }

    /**
     * 构建默认树
     */
    private void buildDefaultTree(Set<String> expandedTypeIds) {

        // 按 P2P 类型排序
        List<String> sortedTypes = new ArrayList<>(p2pTypeInfoMap.keySet());
        sortedTypes.sort(String::compareTo);

        for (String typeName : sortedTypes) {
            P2PTypeInfo typeInfo = p2pTypeInfoMap.get(typeName);
            if (typeInfo == null) continue;

            TreeNode typeNode = new TreeNode();
            typeNode.type = NodeType.P2P_TYPE;
            typeNode.typeName = typeName;
            typeNode.displayComponent = formatTypeDisplayName(typeName, typeInfo);  // 存储 Component
            // 优先使用本次重建会话中的展开状态，然后回退到持久化缓存
            typeNode.expanded = expandedTypeIds.contains(typeNode.getNodeId())
                    || expandedStateCache.getOrDefault(typeNode.getNodeId(), false);
            typeNode.data = typeInfo;

            flatNodes.add(typeNode);

            if (typeNode.expanded) {
                // 获取该类型下的所有频段
                List<ChannelInfo> channels = typeInfo.channelInfoList();
                if (channels != null) {
                    channels.sort(Comparator.comparingInt(ch -> Integer.parseInt(ch.frequency(), 16)));

                    for (ChannelInfo channelInfo : channels) {
                        TreeNode channelNode = new TreeNode();
                        channelNode.type = NodeType.CHANNEL;
                        channelNode.frequency = channelInfo.frequency();
                        channelNode.displayComponent = formatChannelDisplayName(channelInfo);
                        // 恢复展开状态：优先本次会话，再回退到持久化缓存
                        channelNode.expanded = expandedTypeIds.contains(channelNode.getNodeId())
                                || expandedStateCache.getOrDefault(channelNode.getNodeId(), false);
                        channelNode.data = channelInfo;
                        channelNode.parent = typeNode;

                        flatNodes.add(channelNode);

                        if (channelNode.expanded) {
                            // 添加该频段下的所有 P2P
                            List<P2PInfo> p2ps = channelInfo.p2pInfoList();
                            if (p2ps != null) {
                                p2ps.sort((a, b) -> Boolean.compare(a.isOutput(), b.isOutput()));

                                for (P2PInfo p2pInfo : p2ps) {
                                    TreeNode p2pNode = new TreeNode();
                                    p2pNode.type = NodeType.P2P;
                                    p2pNode.p2pInfo = p2pInfo;
                                    p2pNode.displayComponent = formatP2PDisplayName(p2pInfo);
                                    p2pNode.parent = channelNode;
                                    p2pNode.frequency = String.valueOf(p2pInfo.frequency());
                                    flatNodes.add(p2pNode);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 保存当前展开状态，同时同步到持久化缓存，确保父节点收起后子节点状态不丢失
     */
    private void saveExpandedState(Set<String> expandedNodeIds) {
        for (TreeNode node : flatNodes) {
            if (node.expanded) {
                expandedNodeIds.add(node.getNodeId());
            }
            // 将所有可展开节点的状态同步到持久化缓存
            if (node.type == NodeType.P2P_TYPE || node.type == NodeType.CHANNEL) {
                expandedStateCache.put(node.getNodeId(), node.expanded);
            }
        }
    }

    /**
     * 获取当前选中的 P2P 信息，供外部（如详情面板）同步使用。
     */
    public P2PInfo getSelectedP2PInfo() {
        return selectedP2PInfo;
    }

    /**
     * 获取当前选中的树节点，供外部（如详情面板）同步使用。
     */
    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public int getScrollOffset() {
        return currentScrollOffset;
    }

    // 添加新的方法供外部设置滚动偏移
    public void setScrollOffset(int scrollOffset) {
        this.currentScrollOffset = scrollOffset;
    }

    // 添加获取总内容高度的方法
    public int getTotalContentHeight() {
        return flatNodes.size() * getRowHeight();
    }

    /**
     * 设置搜索过滤条件
     * @param query 搜索关键字，为空或 null 时恢复默认树
     */
    public void setSearchFilter(String query) {
        if (query == null || query.trim().isEmpty()) {
            if (!isSearchMode) return; // 已经是默认模式，无需重建
            this.currentSearchQuery = null;
            this.isSearchMode = false;
        } else {
            this.currentSearchQuery = query.trim().toLowerCase();
            this.isSearchMode = true;
        }
        rebuildTree();
    }

    /**
     * 构建搜索结果树
     */
    private void buildSearchResults(String query) {
        String lowerQuery = query.toLowerCase();
        Map<String, Map<String, List<P2PInfo>>> groupedResults = new LinkedHashMap<>();
        searchResultCount = 0;

        // 遍历所有 P2P 类型，获取其下的所有 P2PInfo
        for (P2PTypeInfo typeInfo : p2pTypeInfoMap.values()) {
            List<P2PInfo> allP2ps = typeInfo.p2pInfoList();
            if (allP2ps == null) continue;

            List<P2PInfo> matchedP2ps = allP2ps.stream()
                    .filter(p2p -> matchesSearch(p2p, lowerQuery))
                    .toList();

            if (matchedP2ps.isEmpty()) continue;

            // 按频段分组
            Map<String, List<P2PInfo>> byFrequency = new LinkedHashMap<>();
            for (P2PInfo p2p : matchedP2ps) {
                String freq = String.valueOf(p2p.frequency());
                byFrequency.computeIfAbsent(freq, k -> new ArrayList<>()).add(p2p);
            }

            groupedResults.put(typeInfo.p2pType(), byFrequency);
            searchResultCount += matchedP2ps.size();
        }

        // 构建树结构：类型 -> 频段 -> P2P
        for (var typeEntry : groupedResults.entrySet()) {
            String typeName = typeEntry.getKey();
            Map<String, List<P2PInfo>> freqMap = typeEntry.getValue();

            P2PTypeInfo typeInfo = p2pTypeInfoMap.get(typeName);
            int typeMatchCount = freqMap.values().stream().mapToInt(List::size).sum();

            TreeNode typeNode = new TreeNode();
            typeNode.type = NodeType.P2P_TYPE;
            typeNode.typeName = typeName;
            typeNode.displayComponent = formatSearchTypeName(typeName, typeMatchCount);
            typeNode.expanded = true; // 搜索模式下默认展开
            typeNode.data = typeInfo;
            flatNodes.add(typeNode);

            // 按频段数值排序
            List<String> sortedFreqs = new ArrayList<>(freqMap.keySet());
            sortedFreqs.sort(Comparator.comparingInt(f -> Integer.parseInt(f, 16)));

            for (String freq : sortedFreqs) {
                List<P2PInfo> p2ps = freqMap.get(freq);
                ChannelInfo channelInfo = channelInfoMap.get(freq);

                TreeNode channelNode = new TreeNode();
                channelNode.type = NodeType.CHANNEL;
                channelNode.frequency = freq;
                channelNode.displayComponent = formatSearchChannelName(freq, channelInfo, p2ps.size());
                channelNode.expanded = true; // 搜索模式下默认展开
                channelNode.data = channelInfo;
                channelNode.parent = typeNode;
                flatNodes.add(channelNode);

                // P2P 设备按输出在前排序
                p2ps.sort((a, b) -> Boolean.compare(a.isOutput(), b.isOutput()));
                for (P2PInfo p2pInfo : p2ps) {
                    TreeNode p2pNode = new TreeNode();
                    p2pNode.type = NodeType.P2P;
                    p2pNode.p2pInfo = p2pInfo;
                    p2pNode.displayComponent = formatP2PDisplayName(p2pInfo);
                    p2pNode.parent = channelNode;
                    p2pNode.frequency = String.valueOf(p2pInfo.frequency());
                    flatNodes.add(p2pNode);
                }
            }
        }
    }

    /**
     * 判断 P2P 是否匹配搜索条件（部分匹配）
     */
    private boolean matchesSearch(P2PInfo p2p, String lowerQuery) {
        // 匹配 P2P 类型
        if (p2p.p2pType() != null && p2p.p2pType().toLowerCase().contains(lowerQuery)) {
            return true;
        }
        // 匹配频段
        if (String.valueOf(p2p.frequency()).toLowerCase().contains(lowerQuery)) {
            return true;
        }
        // 匹配 P2P 名字
        if (p2p.name() != null && p2p.name().toLowerCase().contains(lowerQuery)) {
            return true;
        }
        // 匹配频段别名（即输入端 P2P 名称）
        ChannelInfo channelInfo = channelInfoMap.get(String.valueOf(p2p.frequency()));
        if (channelInfo != null && channelInfo.alias() != null
                && channelInfo.alias().toLowerCase().contains(lowerQuery)) {
            return true;
        }
        return false;
    }

    /**
     * 格式化搜索结果中的类型名称
     */
    private Component formatSearchTypeName(String typeName, int matchCount) {
        return Component.literal("📁 ")
                .append(Component.literal(typeName).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(String.format(" (%d)", matchCount))
                        .withStyle(ChatFormatting.GRAY));
    }

    /**
     * 格式化搜索结果中的频段名称
     */
    private Component formatSearchChannelName(String freq, ChannelInfo channelInfo, int matchCount) {
        String alias = channelInfo != null ? channelInfo.alias() : null;
        MutableComponent freqComponent;

        if (alias != null && !alias.equals(freq)) {
            freqComponent = Component.literal(alias).withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(String.format(" (%s)", freq)).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            freqComponent = Component.literal(freq).withStyle(ChatFormatting.AQUA);
        }

        return Component.literal("📡 ")
                .append(freqComponent)
                .append(Component.literal(String.format(" (%d)", matchCount))
                        .withStyle(ChatFormatting.GREEN));
    }

    /**
     * 格式化搜索结果头
     */
    private Component formatSearchHeader() {
        return TranslateHelper.P2PTree.searchResultCount(searchResultCount)
                .copy().withStyle(ChatFormatting.YELLOW);
    }

    /**
     * 获取当前选中节点的唯一标识
     */
    private String getSelectedNodeId() {
        if (selectedNode == null) return null;
        return selectedNode.getNodeId();
    }

    /**
     * 恢复选中的节点
     */
    private void restoreSelectedNode(String selectedNodeId) {
        if (selectedNodeId == null) {
            selectedNode = null;
            selectedP2PInfo = null;
            // 重建期间抑制 listener 通知
            if (!isRebuilding && nodeSelectionListener != null) {
                nodeSelectionListener.accept(null);
            }
            return;
        }

        for (TreeNode node : flatNodes) {
            if (selectedNodeId.equals(node.getNodeId())) {
                selectedNode = node;
                selectedP2PInfo = node.type == NodeType.P2P ? node.p2pInfo : null;
                if (selectionListener != null && selectedP2PInfo == null) {
                    selectionListener.accept(null);
                }
                // 重建期间抑制 listener 通知
                if (!isRebuilding && nodeSelectionListener != null) {
                    nodeSelectionListener.accept(node);
                }
                break;
            }
        }

        if (selectedNode == null) {
            selectedP2PInfo = null;
            if (selectionListener != null) {
                selectionListener.accept(null);
            }
            // 重建期间抑制 listener 通知
            if (!isRebuilding && nodeSelectionListener != null) {
                nodeSelectionListener.accept(null);
            }
        }
    }

    /**
     * 切换节点的展开/收起状态
     */
    private void toggleNode(TreeNode node) {
        if (node.type == NodeType.P2P) return; // P2P 不可展开
        if (node.type == NodeType.SEARCH_HEADER) return; // 搜索头不可展开

        node.expanded = !node.expanded;
        rebuildTree();
    }

    /**
     * 格式化类型显示名称（返回 Component）
     */
    private Component formatTypeDisplayName(String typeName, P2PTypeInfo typeInfo) {
        int p2pCount = typeInfo.p2pCount();
        int channelCount = typeInfo.channelCount();

        return Component.literal("📁 ")
                .append(Component.literal(typeName).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(String.format(" (%d P2P, %d Freq)", p2pCount, channelCount))
                        .withStyle(ChatFormatting.GRAY));
    }

    /**
     * 格式化频段显示名称
     */
    private Component formatChannelDisplayName(ChannelInfo channelInfo) {
        String alias = channelInfo.alias();
        String freq = channelInfo.frequency();
        int channelRemaining = channelInfo.channelRemaining();
        int p2pCount = channelInfo.p2pCount();
        boolean isMEP2P = channelInfo.p2pType().equals("me_p2p_tunnel");

        MutableComponent freqComponent;

        if (alias != null && !alias.equals(freq)) {
            freqComponent = Component.literal(alias).withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(String.format(" (%s)", freq)).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            freqComponent = Component.literal(freq).withStyle(ChatFormatting.AQUA);
        }

        MutableComponent result = Component.literal("📡 ")
                .append(freqComponent);

        if (isMEP2P) {
            // ME P2P：显示 P2P 数量和剩余频道数，颜色根据剩余频道数动态变化
            ChatFormatting channelColor = ChatFormatting.GREEN;
            if (channelRemaining <= 0) {
                channelColor = ChatFormatting.RED;
            } else if (channelRemaining < getChannelWarningThreshold()) {
                channelColor = ChatFormatting.YELLOW;
            }
            result.append(Component.literal(String.format(" [%d P2P | ⚡%d]", p2pCount, channelRemaining))
                    .withStyle(channelColor));
        } else {
            // 非 ME P2P：不显示频道数，恒定绿色
            result.append(Component.literal(String.format(" [%d P2P]", p2pCount))
                    .withStyle(ChatFormatting.GREEN));
        }

        return result;
    }

    /**
     * 格式化 P2P 显示名称
     */
    private Component formatP2PDisplayName(P2PInfo p2pInfo) {
        String name = p2pInfo.name().isEmpty() ? p2pInfo.toShortString() : p2pInfo.name();
        boolean isOutput = p2pInfo.isOutput();
        boolean isActive = p2pInfo.isActive();
        boolean isConnected = p2pInfo.isConnected();
        boolean isPendingBind = p2pInfo.isPendingBind();

        // 状态图标和颜色
        MutableComponent statusComponent;
        if (!isActive) {
            statusComponent = Component.literal("⭕").withStyle(ChatFormatting.GRAY);
        } else if (isConnected) {
            statusComponent = Component.literal("🟢").withStyle(ChatFormatting.GREEN);
        } else {
            statusComponent = Component.literal("🔴").withStyle(ChatFormatting.RED);
        }

        // 方向图标
        MutableComponent directionComponent = isOutput ?
                Component.literal("📤").withStyle(ChatFormatting.YELLOW) :
                Component.literal("📥").withStyle(ChatFormatting.BLUE);

        // 名称颜色（待绑定的特殊标记）
        MutableComponent nameComponent;
        if (isPendingBind) {
            nameComponent = Component.literal(truncateString(name, getP2PNameMaxLength()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        } else if (isActive && isConnected) {
            nameComponent = Component.literal(truncateString(name, getP2PNameMaxLength()))
                    .withStyle(ChatFormatting.WHITE);
        } else {
            nameComponent = Component.literal(truncateString(name, getP2PNameMaxLength()))
                    .withStyle(ChatFormatting.YELLOW);
        }

        MutableComponent result = Component.literal("")
                .append(statusComponent)
                .append(directionComponent)
                .append(Component.literal(" ").withStyle(ChatFormatting.RESET))
                .append(nameComponent);

        if (isPendingBind) {
            result.append(Component.literal(" ◆ ").withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        return result;
    }

    /**
     * 截断字符串
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }

    // 修改 getNodeAt 方法，移除滚动条检测
    private TreeNode getNodeAt(double mouseX, double mouseY) {
        if (mouseX < getX() || mouseX > getX() + width ||
                mouseY < getY() || mouseY > getY() + height) {
            return null;
        }

        int relativeY = (int) (mouseY - getY() + currentScrollOffset);
        int index = relativeY / getRowHeight();

        if (index >= 0 && index < flatNodes.size()) {
            return flatNodes.get(index);
        }
        return null;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        TreeNode clicked = getNodeAt(mouseX, mouseY);
        if (clicked != null) {
            // 搜索头不响应点击
            if (clicked.type == NodeType.SEARCH_HEADER) return;
            // 可展开的节点类型
            toggleNode(clicked);
            selectNode(clicked);
        }
    }

    /**
     * 选中节点
     */
    private void selectNode(TreeNode node) {
        this.selectedNode = node;

        if (node.type == NodeType.P2P) {
            this.selectedP2PInfo = node.p2pInfo;
            if (selectionListener != null) {
                selectionListener.accept(node.p2pInfo);
            }
        } else {
            this.selectedP2PInfo = null;
            if (selectionListener != null) {
                selectionListener.accept(null);
            }
        }

        // 重建期间抑制 listener 通知，避免重建过程中触发 navigateToP2P 导致滚动重置
        if (!isRebuilding && nodeSelectionListener != null) {
            nodeSelectionListener.accept(node);
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        // 更新原生 isHovered 状态（MC 依赖此字段判断 isHoveredOrFocused()）
        this.isHovered = mouseX >= getX() && mouseX <= getX() + width
                && mouseY >= getY() && mouseY <= getY() + height;

        // 更新悬停项
        hoveredNode = getNodeAt(mouseX, mouseY);

        // 绘制背景
        renderBackground(guiGraphics);

        // 启用 scissor 裁剪
        guiGraphics.enableScissor(getX(), getY(), getX() + width, getY() + height);

        // 绘制树内容
        renderTreeContent(guiGraphics, mouseX, mouseY);

        // 禁用 scissor
        guiGraphics.disableScissor();
    }

    /**
     * 绘制背景
     */
    private void renderBackground(GuiGraphics guiGraphics) {
        // 使用 GUI 默认背景色（透明或半透明，让 JSON 定义的背景透出）
        // 这里绘制半透明深色作为基础背景
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, getColorBg());
    }

    /**
     * 绘制树内容（修改后，排除滚动条区域）
     */
    private void renderTreeContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (flatNodes.isEmpty()) {
            Component emptyText = TranslateHelper.P2PTree.empty()
                    .copy().withStyle(ChatFormatting.BLACK);
            int textWidth = Minecraft.getInstance().font.width(emptyText);
            guiGraphics.drawString(Minecraft.getInstance().font, emptyText,
                    getX() + (width - textWidth) / 2,
                    getY() + height / 2 - 4,
                    -1, false);
            return;
        }

        int rowHeight = getRowHeight();
        int startIndex = currentScrollOffset / rowHeight;
        int endIndex = Math.min(flatNodes.size(), startIndex + (height + rowHeight - 1) / rowHeight + 1);
        int yOffset = getY() - currentScrollOffset + startIndex * rowHeight;

        int clickableWidth = width;

        for (int i = startIndex; i < endIndex; i++) {
            TreeNode node = flatNodes.get(i);
            int rowY = yOffset + (i - startIndex) * rowHeight;

            if (rowY + rowHeight < getY() || rowY > getY() + height) {
                continue;
            }

            int indent = getIndent(node);
            int iconX = getX() + indent;
            int textX = getX() + indent + getExpandIconWidth() + getIconTextSpacing();
            int textY = rowY + (rowHeight - 8) / 2;

            // 绘制选中背景
            if (node == selectedNode) {
                guiGraphics.fill(getX(), rowY, getX() + clickableWidth, rowY + rowHeight, getColorSelected());
            } else if (node == hoveredNode) {
                guiGraphics.fill(getX(), rowY, getX() + clickableWidth, rowY + rowHeight, getColorHover());
            }

            // 绘制展开/收起图标（对于可展开的节点）
            if (node.type == NodeType.P2P_TYPE || node.type == NodeType.CHANNEL) {
                String iconSymbol = node.expanded ? "▼" : "▶";
                MutableComponent iconComponent = Component.literal(iconSymbol)
                        .withStyle(ChatFormatting.GOLD);
                guiGraphics.drawString(Minecraft.getInstance().font, iconComponent,
                        iconX, rowY + (rowHeight - 8) / 2, -1, false);
            }

            // 绘制文本（scissor 已启用，无需手动截断，直接绘制以保留颜色样式）
            Component displayComponent = getDisplayComponent(node);
            guiGraphics.drawString(Minecraft.getInstance().font, displayComponent,
                    textX, textY, -1, false);
        }
    }

    private Component getDisplayComponent(TreeNode node) {
        return switch (node.type) {
            case P2P_TYPE -> {
                if (node.data instanceof P2PTypeInfo typeInfo) {
                    yield formatTypeDisplayName(node.typeName, typeInfo);
                }
                yield node.displayComponent != null ? node.displayComponent : Component.literal(node.typeName);
            }
            case CHANNEL -> {
                if (node.data instanceof ChannelInfo) {
                    yield formatChannelDisplayName((ChannelInfo) node.data);
                }
                yield node.displayComponent != null ? node.displayComponent : Component.literal(node.frequency);
            }
            case P2P -> {
                if (node.p2pInfo != null) {
                    yield formatP2PDisplayName(node.p2pInfo);
                }
                yield TranslateHelper.P2PTree.unknownP2P();
            }
            case SEARCH_HEADER -> node.displayComponent != null
                    ? node.displayComponent
                    : TranslateHelper.P2PTree.searchResults();
        };
    }

    /**
     * 获取缩进级别（基于父链深度）
     */
    private int getIndent(TreeNode node) {
        int depth = 0;
        TreeNode current = node.parent;
        while (current != null) {
            depth++;
            current = current.parent;
        }
        return depth * getIndentWidth();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationOutput) {
        this.defaultButtonNarrationText(narrationOutput);
    }
}