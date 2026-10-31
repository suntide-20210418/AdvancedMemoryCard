package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import com.suntide_20210418.advancedmemorycard.client.gui.P2PTypeNames;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.p2p.*;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

/**
 * P2P 树控件 —— 列式渲染：状态 / 名称 / 频段 / 汇总。
 *
 * <p>和旧版的区别（都是可验证的测量结果，不是审美偏好）：
 * <ul>
 *   <li>旧版每级缩进 2px、图标预留 2px，三级文字起点只差 4px，层级等于没画；
 *       现在 10px/级，靠“有没有折叠箭头 + 名称起点”表达层级。</li>
 *   <li>旧版把别名/hex/台数/剩余频道拼成一个长串，152px 宽被 scissor 硬裁且没有省略号，
 *       关键的剩余频道常年被切掉；现在拆成两个右对齐的定宽列，超宽按像素截断加省略号。</li>
 *   <li>旧版用 GOLD/AQUA/GRAY 表达语义，实测对比度 1.08~1.75:1，等于不可读；
 *       现在只用语义色板的深色文本 + 一个小圆点。</li>
 * </ul>
 */
public class P2PTreeWidget extends AbstractWidget {

    /** 状态列宽（只有 P2P 叶子节点画圆点） */
    private static final int COL_STATUS_W = 12;
    /** 频段列宽 */
    private static final int COL_FREQ_W = 40;
    /** 汇总列宽 */
    private static final int COL_SUMMARY_W = 52;
    /** 右侧给滚动条留的槽位宽度（滚动条 12px + 1px 间隙） */
    private static final int SCROLLBAR_GUTTER = 14;
    /** 折叠箭头的实际绘制宽度（MC 里箭头字宽约 7px，留 9 才不压字） */
    private static final int CHEVRON_W = 9;
    private static final int RIGHT_PAD = 4;

    private Map<String, P2PTypeInfo> p2pTypeInfoMap;
    private Map<P2PPosition, P2PInfo> p2pInfoMap;
    private Map<String, ChannelInfo> channelInfoMap;

    private final Map<String, Boolean> expandedStateCache = new HashMap<>();
    private final List<TreeNode> flatNodes = new ArrayList<>();

    private int currentScrollOffset = 0;

    private TreeNode selectedNode = null;
    private P2PInfo selectedP2PInfo = null;

    private Consumer<P2PInfo> selectionListener;
    private Consumer<TreeNode> nodeSelectionListener;

    private String currentSearchQuery = null;
    private boolean isSearchMode = false;
    private int searchResultCount = 0;

    private P2PInfo pendingNavigateTarget = null;
    private boolean scrollToSelected = false;
    private boolean pendingCenter = true;
    private boolean isNavigating = false;
    private boolean isRebuilding = false;

    private TreeNode hoveredNode = null;

    /** Ctrl+点击选中的多台设备（按选中顺序）。键是位置，保证重建树后依然对得上。 */
    private final Set<P2PPosition> multiSelection = new LinkedHashSet<>();
    /** 多选集合的类型。多选只允许同类型，跨类型时整组作废。 */
    private String multiSelectionType;
    /** 多选行的底色：比单选浅一档，左侧加一条绿条区分 */
    private static final int MULTI_SELECTED_BG = 0xFF4A6EA9;
    private static final int MULTI_SELECTED_BAR = 0xFF9BE3A8;

    /**
     * Ctrl + 按住左键向下拖动框选的锚点行下标，-1 表示当前没有拖动。
     *
     * <p>锚点用行下标而不是节点引用：拖动过程中树可能被同步包重建，
     * 下标落在「第几行」这个语义上，重建后依然指得准。
     */
    private int dragAnchorIndex = -1;
    private TreeNode dragAnchorNode;
    /** 是否已经越过起始行 —— 越过之后才叫拖动，否则仍是一次普通点击 */
    private boolean dragStarted;
    /** 按下瞬间的多选集快照：每次拖动都从它重新累加，往回拖才能取消掉多余的行 */
    private final LinkedHashSet<P2PPosition> dragBaseSelection = new LinkedHashSet<>();
    private String dragBaseType;
    /** 这次拖动的方向：true = 扫过即选中，false = 扫过即取消（由起手那一行的选中状态决定） */
    private boolean dragSelecting = true;
    /** 多选集变化回调（配对条要实时显示「已选 N 台」） */
    private Runnable multiSelectionChangedListener;
    /** 批量操作模式：单击即加选、拖动即框选，不必按着 Ctrl */
    private boolean batchMode;
    /** 批量模式开关状态回调（底部操作条要把按钮文字改成「退出批量操作」） */
    private Consumer<Boolean> batchModeChangedListener;

    public P2PTreeWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public static int getRowHeight() {
        return ModConfigs.getClientConfig().treeRowHeight.get();
    }

    private int getIndentWidth() {
        return ModConfigs.getClientConfig().treeIndentWidth.get();
    }

    private int getChannelWarningThreshold() {
        return ModConfigs.getClientConfig().treeChannelWarningThreshold.get();
    }

    private int getP2PNameMaxLength() {
        return ModConfigs.getClientConfig().treeP2PNameMaxLength.get();
    }

    private int colorSelected() {
        return ModConfigs.getClientConfig().paletteRowSelected.get();
    }

    private int colorHover() {
        return ModConfigs.getClientConfig().paletteRowHover.get();
    }

    private int colorText() {
        return ModConfigs.getClientConfig().paletteTextPrimary.get();
    }

    private int colorTextSecondary() {
        return ModConfigs.getClientConfig().paletteTextSecondary.get();
    }

    private int colorTextOnSelected() {
        return 0xFFFFFFFF;
    }

    private int colorTextOnSelectedSecondary() {
        return 0xFFCCD6E8;
    }

    private int colorOk() {
        return ModConfigs.getClientConfig().paletteStatusOk.get();
    }

    private int colorWarn() {
        return ModConfigs.getClientConfig().paletteStatusWarn.get();
    }

    private int colorMuted() {
        return ModConfigs.getClientConfig().paletteTextTertiary.get();
    }

    // ==================== 数据 ====================

    public void updateData(Map<String, P2PTypeInfo> p2pTypeInfoMap,
                           Map<String, ChannelInfo> channelInfoMap,
                           Map<P2PPosition, P2PInfo> p2pInfoMap) {
        this.p2pTypeInfoMap = p2pTypeInfoMap != null ? p2pTypeInfoMap : new HashMap<>();
        this.channelInfoMap = channelInfoMap != null ? channelInfoMap : new HashMap<>();
        this.p2pInfoMap = p2pInfoMap != null ? p2pInfoMap : new HashMap<>();
        rebuildTree();
    }

    public void setSelectionListener(Consumer<P2PInfo> listener) {
        this.selectionListener = listener;
    }

    public void setNodeSelectionListener(Consumer<TreeNode> listener) {
        this.nodeSelectionListener = listener;
    }

    /** 强制居中到目标设备（用于“定位”这类显式跳转）。 */
    public void navigateToP2P(P2PInfo targetP2P) {
        navigate(targetP2P, true);
    }

    /**
     * 展开祖先并选中目标设备；只有当它不在可视区时才滚动。
     *
     * <p>同步包每 0.5 秒来一次，如果每次都强制居中，玩家刚滚到别处就会被拽回去。
     * 所以“跟随当前配对源”用这个入口，显式跳转才用 {@link #navigateToP2P}。
     */
    public void revealP2P(P2PInfo targetP2P) {
        navigate(targetP2P, false);
    }

    private void navigate(P2PInfo targetP2P, boolean alwaysCenter) {
        if (targetP2P == null || isNavigating) return;
        this.pendingNavigateTarget = targetP2P;
        this.scrollToSelected = true;
        this.pendingCenter = alwaysCenter;
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

    /** 当前选中的行是否完整落在可视区内 */
    private boolean isSelectedRowVisible() {
        if (selectedNode == null) return false;
        int index = flatNodes.indexOf(selectedNode);
        if (index < 0) return false;
        int rowHeight = getRowHeight();
        int top = index * rowHeight;
        return top >= currentScrollOffset
                && top + rowHeight <= currentScrollOffset + getHeight();
    }

    private void rebuildTree() {
        Set<String> expandedTypeIds = new HashSet<>();
        saveExpandedState(expandedTypeIds);

        String selectedNodeId = getSelectedNodeId();

        P2PInfo targetP2P = pendingNavigateTarget;
        if (targetP2P == null && selectedNode != null
                && selectedNode.type == NodeType.P2P && selectedNode.p2pInfo != null) {
            targetP2P = selectedNode.p2pInfo;
        }

        if (targetP2P != null) {
            expandedTypeIds.add("type:" + targetP2P.p2pType());
            expandedTypeIds.add("channel:" + targetP2P.frequency());
            expandedStateCache.put("type:" + targetP2P.p2pType(), true);
            expandedStateCache.put("channel:" + targetP2P.frequency(), true);
        }

        if (pendingNavigateTarget != null) {
            selectedNodeId = TreeNode.p2pNodeId(pendingNavigateTarget);
        }

        flatNodes.clear();

        if (isSearchMode && currentSearchQuery != null) {
            buildSearchResults(currentSearchQuery);
        } else {
            buildDefaultTree(expandedTypeIds);
        }

        isRebuilding = true;
        try {
            restoreSelectedNode(selectedNodeId);
        } finally {
            isRebuilding = false;
        }

        if (scrollToSelected && selectedNode != null) {
            if (pendingCenter || !isSelectedRowVisible()) {
                scrollToSelectedNode();
            }
            scrollToSelected = false;
        }
        pendingNavigateTarget = null;
    }

    private void scrollToSelectedNode() {
        if (selectedNode == null) return;
        int index = flatNodes.indexOf(selectedNode);
        if (index < 0) return;
        int rowHeight = getRowHeight();
        int nodeY = index * rowHeight;
        int targetScroll = nodeY - getHeight() / 2 + rowHeight / 2;
        targetScroll = Math.max(0, Math.min(targetScroll, Math.max(0, getTotalContentHeight() - getHeight())));
        this.currentScrollOffset = targetScroll;
    }

    private void buildDefaultTree(Set<String> expandedTypeIds) {
        List<String> sortedTypes = new ArrayList<>(p2pTypeInfoMap.keySet());
        sortedTypes.sort(String::compareTo);

        for (String typeName : sortedTypes) {
            P2PTypeInfo typeInfo = p2pTypeInfoMap.get(typeName);
            if (typeInfo == null) continue;

            TreeNode typeNode = new TreeNode();
            typeNode.type = NodeType.P2P_TYPE;
            typeNode.typeName = typeName;
            typeNode.expanded = expandedTypeIds.contains("type:" + typeName)
                    || expandedStateCache.getOrDefault("type:" + typeName, false);
            typeNode.data = typeInfo;
            flatNodes.add(typeNode);

            if (!typeNode.expanded) continue;

            List<ChannelInfo> channels = typeInfo.channelInfoList();
            if (channels == null) continue;
            List<ChannelInfo> sorted = new ArrayList<>(channels);
            sorted.sort(Comparator.comparingInt(ch -> Integer.parseInt(ch.frequency(), 16)));

            for (ChannelInfo channelInfo : sorted) {
                TreeNode channelNode = new TreeNode();
                channelNode.type = NodeType.CHANNEL;
                channelNode.frequency = channelInfo.frequency();
                channelNode.expanded = expandedTypeIds.contains("channel:" + channelInfo.frequency())
                        || expandedStateCache.getOrDefault("channel:" + channelInfo.frequency(), false);
                channelNode.data = channelInfo;
                channelNode.parent = typeNode;
                flatNodes.add(channelNode);

                if (!channelNode.expanded) continue;

                List<P2PInfo> p2ps = channelInfo.p2pInfoList();
                if (p2ps == null) continue;
                List<P2PInfo> sortedP2P = new ArrayList<>(p2ps);
                sortedP2P.sort((a, b) -> Boolean.compare(a.isOutput(), b.isOutput()));
                for (P2PInfo p2pInfo : sortedP2P) {
                    TreeNode p2pNode = new TreeNode();
                    p2pNode.type = NodeType.P2P;
                    p2pNode.p2pInfo = p2pInfo;
                    p2pNode.parent = channelNode;
                    p2pNode.frequency = p2pInfo.frequency();
                    flatNodes.add(p2pNode);
                }
            }
        }
    }

    private void saveExpandedState(Set<String> expandedNodeIds) {
        for (TreeNode node : flatNodes) {
            if (node.expanded) {
                expandedNodeIds.add(node.getNodeId());
            }
            if (node.type == NodeType.P2P_TYPE || node.type == NodeType.CHANNEL) {
                expandedStateCache.put(node.getNodeId(), node.expanded);
            }
        }
    }

    public P2PInfo getSelectedP2PInfo() {
        return selectedP2PInfo;
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public int getScrollOffset() {
        return currentScrollOffset;
    }

    public void setScrollOffset(int scrollOffset) {
        this.currentScrollOffset = Math.max(0, Math.min(scrollOffset,
                Math.max(0, getTotalContentHeight() - getHeight())));
    }

    public int getTotalContentHeight() {
        return flatNodes.size() * getRowHeight();
    }

    public int getSearchResultCount() {
        return searchResultCount;
    }

    public void setSearchFilter(String query) {
        if (query == null || query.trim().isEmpty()) {
            if (!isSearchMode) return;
            this.currentSearchQuery = null;
            this.isSearchMode = false;
        } else {
            this.currentSearchQuery = query.trim().toLowerCase();
            this.isSearchMode = true;
        }
        rebuildTree();
    }

    public boolean isSearchMode() {
        return isSearchMode;
    }

    private void buildSearchResults(String lowerQuery) {
        searchResultCount = 0;
        Map<String, Map<String, List<P2PInfo>>> grouped = new LinkedHashMap<>();

        for (P2PTypeInfo typeInfo : p2pTypeInfoMap.values()) {
            List<P2PInfo> all = typeInfo.p2pInfoList();
            if (all == null) continue;
            List<P2PInfo> matched = all.stream().filter(p -> matchesSearch(p, lowerQuery)).toList();
            if (matched.isEmpty()) continue;
            Map<String, List<P2PInfo>> byFreq = new LinkedHashMap<>();
            for (P2PInfo p : matched) {
                byFreq.computeIfAbsent(p.frequency(), k -> new ArrayList<>()).add(p);
            }
            grouped.put(typeInfo.p2pType(), byFreq);
            searchResultCount += matched.size();
        }

        for (var entry : grouped.entrySet()) {
            TreeNode typeNode = new TreeNode();
            typeNode.type = NodeType.P2P_TYPE;
            typeNode.typeName = entry.getKey();
            typeNode.expanded = true;
            typeNode.data = p2pTypeInfoMap.get(entry.getKey());
            flatNodes.add(typeNode);

            List<String> freqs = new ArrayList<>(entry.getValue().keySet());
            freqs.sort(Comparator.comparingInt(f -> Integer.parseInt(f, 16)));
            for (String freq : freqs) {
                TreeNode channelNode = new TreeNode();
                channelNode.type = NodeType.CHANNEL;
                channelNode.frequency = freq;
                channelNode.expanded = true;
                channelNode.data = channelInfoMap.get(freq);
                channelNode.parent = typeNode;
                flatNodes.add(channelNode);

                List<P2PInfo> p2ps = new ArrayList<>(entry.getValue().get(freq));
                p2ps.sort((a, b) -> Boolean.compare(a.isOutput(), b.isOutput()));
                for (P2PInfo p2pInfo : p2ps) {
                    TreeNode p2pNode = new TreeNode();
                    p2pNode.type = NodeType.P2P;
                    p2pNode.p2pInfo = p2pInfo;
                    p2pNode.parent = channelNode;
                    p2pNode.frequency = p2pInfo.frequency();
                    flatNodes.add(p2pNode);
                }
            }
        }
    }

    private boolean matchesSearch(P2PInfo p2p, String lowerQuery) {
        if (p2p.p2pType() != null && p2p.p2pType().toLowerCase().contains(lowerQuery)) return true;
        if (p2p.p2pType() != null
                && P2PTypeNames.string(p2p.p2pType()).toLowerCase().contains(lowerQuery)) return true;
        if (String.valueOf(p2p.frequency()).toLowerCase().contains(lowerQuery)) return true;
        if (p2p.name() != null && p2p.name().toLowerCase().contains(lowerQuery)) return true;
        ChannelInfo channelInfo = channelInfoMap.get(p2p.frequency());
        return channelInfo != null && channelInfo.alias() != null
                && channelInfo.alias().toLowerCase().contains(lowerQuery);
    }

    private String getSelectedNodeId() {
        return selectedNode == null ? null : selectedNode.getNodeId();
    }

    private void restoreSelectedNode(String selectedNodeId) {
        if (selectedNodeId == null) {
            selectedNode = null;
            selectedP2PInfo = null;
            if (!isRebuilding && nodeSelectionListener != null) nodeSelectionListener.accept(null);
            return;
        }
        for (TreeNode node : flatNodes) {
            if (selectedNodeId.equals(node.getNodeId())) {
                selectedNode = node;
                selectedP2PInfo = node.type == NodeType.P2P ? node.p2pInfo : null;
                if (selectionListener != null && selectedP2PInfo == null) selectionListener.accept(null);
                if (!isRebuilding && nodeSelectionListener != null) nodeSelectionListener.accept(node);
                return;
            }
        }
        selectedNode = null;
        selectedP2PInfo = null;
        if (selectionListener != null) selectionListener.accept(null);
        if (!isRebuilding && nodeSelectionListener != null) nodeSelectionListener.accept(null);
    }

    private void toggleNode(TreeNode node) {
        if (node.type == NodeType.P2P) return;
        node.expanded = !node.expanded;
        rebuildTree();
    }

    // ==================== 渲染 ====================

    private int contentWidth() {
        return width - SCROLLBAR_GUTTER;
    }

    /** 频段列的右对齐基线 */
    private int freqRight() {
        return getX() + contentWidth() - COL_SUMMARY_W - RIGHT_PAD;
    }

    /** 汇总列的右对齐基线 */
    private int summaryRight() {
        return getX() + contentWidth() - RIGHT_PAD;
    }

    /** 名称列可用宽度 */
    private int nameMaxWidth(int nameX) {
        int nameEnd = getX() + contentWidth() - COL_FREQ_W - COL_SUMMARY_W;
        return Math.max(10, nameEnd - nameX - RIGHT_PAD);
    }

    /** 列头由 Screen 在 super.render() 之后调用，画在树区上方的空带里。 */
    public void renderColumnHeader(GuiGraphics g, int headerX, int headerY) {
        Font font = Minecraft.getInstance().font;
        int textColor = colorTextSecondary();
        g.drawString(font, TranslateHelper.TreeColumn.name().getString(),
                headerX + COL_STATUS_W, headerY, textColor, false);

        String freq = TranslateHelper.TreeColumn.frequency().getString();
        g.drawString(font, freq,
                headerX + contentWidth() - COL_SUMMARY_W - RIGHT_PAD - font.width(freq),
                headerY, textColor, false);

        String summary = TranslateHelper.TreeColumn.summary().getString();
        g.drawString(font, summary,
                headerX + contentWidth() - RIGHT_PAD - font.width(summary),
                headerY, textColor, false);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.isHovered = mouseX >= getX() && mouseX <= getX() + width
                && mouseY >= getY() && mouseY <= getY() + height;
        hoveredNode = getNodeAt(mouseX, mouseY);

        g.enableScissor(getX(), getY(), getX() + width, getY() + height);
        renderTreeContent(g);
        g.disableScissor();
    }

    private void renderTreeContent(GuiGraphics g) {
        Font font = Minecraft.getInstance().font;

        if (flatNodes.isEmpty()) {
            Component empty = TranslateHelper.P2PTree.empty();
            int w = font.width(empty);
            g.drawString(font, empty, getX() + (width - w) / 2, getY() + height / 2 - 4,
                    colorTextSecondary(), false);
            return;
        }

        int rowHeight = getRowHeight();
        int startIndex = currentScrollOffset / rowHeight;
        int endIndex = Math.min(flatNodes.size(), startIndex + (height + rowHeight - 1) / rowHeight + 1);
        int startY = getY() - currentScrollOffset + startIndex * rowHeight;

        for (int i = startIndex; i < endIndex; i++) {
            TreeNode node = flatNodes.get(i);
            int rowY = startY + (i - startIndex) * rowHeight;
            if (rowY + rowHeight < getY() || rowY > getY() + height) continue;

            boolean isSelected = node == selectedNode;
            boolean isMulti = node.type == NodeType.P2P && node.p2pInfo != null
                    && multiSelection.contains(positionKey(node.p2pInfo));
            boolean isHovered = node == hoveredNode && !isSelected && !isMulti;

            if (isSelected) {
                g.fill(getX(), rowY, getX() + contentWidth(), rowY + rowHeight, colorSelected());
            } else if (isMulti) {
                g.fill(getX(), rowY, getX() + contentWidth(), rowY + rowHeight, MULTI_SELECTED_BG);
            } else if (isHovered) {
                g.fill(getX(), rowY, getX() + contentWidth(), rowY + rowHeight, colorHover());
            }
            if (isMulti) {
                g.fill(getX(), rowY, getX() + 2, rowY + rowHeight, MULTI_SELECTED_BAR);
            }

            int textY = rowY + (rowHeight - 8) / 2;
            int indent = getIndent(node);
            int nameX = getX() + COL_STATUS_W + indent;

            int primary = isSelected ? colorTextOnSelected() : colorText();
            int secondary = isSelected ? colorTextOnSelectedSecondary() : colorTextSecondary();

            switch (node.type) {
                case P2P_TYPE -> {
                    drawChevron(g, nameX, rowY, node.expanded, secondary);
                    String text = fit(font, P2PTypeNames.string(node.typeName),
                            nameMaxWidth(nameX + CHEVRON_W));
                    g.drawString(font, text, nameX + CHEVRON_W, textY, primary, false);
                    if (node.data instanceof P2PTypeInfo info) {
                        String summary = TranslateHelper.translate(
                                TranslateHelper.Keys.TREE_SUMMARY_TYPE,
                                info.p2pCount(), info.channelCount()).getString();
                        drawRight(g, font, summary, summaryRight(), textY, secondary);
                    }
                }
                case CHANNEL -> {
                    drawChevron(g, nameX, rowY, node.expanded, secondary);
                    ChannelInfo info = node.data instanceof ChannelInfo c ? c : null;
                    String alias = info != null && info.alias() != null ? info.alias() : node.frequency;
                    String text = fit(font, alias, nameMaxWidth(nameX + CHEVRON_W));
                    g.drawString(font, text, nameX + CHEVRON_W, textY, primary, false);
                    if (info != null) {
                        P2PChannelUsage usage = P2PChannelUsage.ofChannel(info);
                        String summary;
                        int color = secondary;
                        if (usage.available()) {
                            summary = TranslateHelper.translate(
                                    TranslateHelper.Keys.TREE_SUMMARY_CHANNEL,
                                    info.p2pCount(), usage.remaining()).getString();
                            if (usage.remaining() < getChannelWarningThreshold()) {
                                color = isSelected ? 0xFFFFD9A0 : colorWarn();
                            }
                        } else {
                            // 非 ME 频段没有“频道”这个概念，只报台数
                            summary = TranslateHelper.translate(
                                    TranslateHelper.Keys.TREE_SUMMARY_CHANNEL_PLAIN,
                                    info.p2pCount()).getString();
                        }
                        drawRight(g, font, summary, summaryRight(), textY, color);
                    }
                    drawRight(g, font, node.frequency, freqRight(), textY, secondary);
                }
                case P2P -> {
                    P2PInfo info = node.p2pInfo;
                    if (info == null) break;
                    int dotX = getX() + 4;
                    int dotY = rowY + rowHeight / 2 - 2;
                    g.fill(dotX, dotY, dotX + 5, dotY + 5, statusColor(info, isSelected));

                    String label = info.name() == null || info.name().isEmpty()
                            ? info.toShortString()
                            : info.name();
                    if (label.length() > getP2PNameMaxLength()) {
                        label = label.substring(0, Math.max(1, getP2PNameMaxLength() - 1)) + "…";
                    }
                    String text = fit(font, label, nameMaxWidth(nameX));
                    g.drawString(font, text, nameX, textY, primary, false);

                    // ME 设备在汇总列显示频道占用，带「入 / 出」前缀区分角色：
                    // 输入端用自己的外部网络数字，输出端用频道视角（见 P2PChannelUsage.ofRow）。
                    // 非 ME 类型没有这个概念，仍然显示输入/输出角色。
                    ChannelInfo channel = channelInfoMap == null ? null : channelInfoMap.get(info.frequency());
                    P2PChannelUsage usage = P2PChannelUsage.ofRow(info, channel);
                    String summaryText;
                    int summaryColor;
                    if (usage.available()) {
                        String prefix = TranslateHelper.translate(info.isOutput()
                                ? TranslateHelper.Keys.TREE_PREFIX_OUTPUT
                                : TranslateHelper.Keys.TREE_PREFIX_INPUT).getString();
                        summaryText = prefix + " " + usage.used() + "/" + usage.max();
                        summaryColor = isSelected ? colorTextOnSelectedSecondary()
                                : (usage.remaining() < getChannelWarningThreshold()
                                ? colorWarn() : colorOk());
                    } else {
                        summaryText = TranslateHelper.translate(info.isOutput()
                                ? TranslateHelper.Keys.TREE_ROLE_OUTPUT
                                : TranslateHelper.Keys.TREE_ROLE_INPUT).getString();
                        summaryColor = isSelected ? colorTextOnSelectedSecondary()
                                : (info.isOutput() ? colorTextSecondary() : colorOk());
                    }
                    drawRight(g, font, summaryText, summaryRight(), textY, summaryColor);

                    if (info.frequency() != null && !"0000".equals(info.frequency())) {
                        drawRight(g, font, info.frequency(), freqRight(), textY, secondary);
                    }
                }
                default -> { }
            }
        }
    }

    private int statusColor(P2PInfo info, boolean isSelected) {
        if (isSelected) return 0xFFB7E3BE;
        if (!info.isActive()) return colorMuted();
        return info.isConnected() ? colorOk() : colorWarn();
    }

    private void drawRight(GuiGraphics g, Font font, String text, int rightX, int y, int color) {
        g.drawString(font, text, rightX - font.width(text), y, color, false);
    }

    private void drawChevron(GuiGraphics g, int x, int rowY, boolean expanded, int color) {
        int cy = rowY + getRowHeight() / 2;
        if (expanded) {
            g.fill(x, cy - 1, x + 5, cy, color);
            g.fill(x + 1, cy, x + 4, cy + 1, color);
            g.fill(x + 2, cy + 1, x + 3, cy + 2, color);
        } else {
            g.fill(x, cy - 2, x + 1, cy + 3, color);
            g.fill(x + 1, cy - 1, x + 2, cy + 2, color);
            g.fill(x + 2, cy, x + 3, cy + 1, color);
        }
    }

    /** 按像素宽度截断并补省略号。scissor 硬裁会切在字中间，看起来像渲染 bug。 */
    private String fit(Font font, String text, int maxWidth) {
        if (text == null) return "";
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        int budget = maxWidth - font.width(ellipsis);
        if (budget <= 0) return "";
        StringBuilder sb = new StringBuilder();
        int acc = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            int cw = font.width(ch);
            if (acc + cw > budget) break;
            sb.append(ch);
            acc += cw;
            i += Character.charCount(cp);
        }
        return sb + ellipsis;
    }

    private int getIndent(TreeNode node) {
        int depth = 0;
        TreeNode current = node.parent;
        while (current != null) {
            depth++;
            current = current.parent;
        }
        return depth * getIndentWidth();
    }

    private TreeNode getNodeAt(double mouseX, double mouseY) {
        if (mouseX < getX() || mouseX > getX() + width
                || mouseY < getY() || mouseY > getY() + height) {
            return null;
        }
        int index = rowIndexAt(mouseY);
        if (index >= 0 && index < flatNodes.size()) {
            return flatNodes.get(index);
        }
        return null;
    }

    /**
     * 屏幕 y 落在第几行。允许越界（负数或超出末行），由调用方决定是钳位还是丢弃 ——
     * 框选时指针划出控件上下边界是常态，那里应该继续往首行 / 末行延伸。
     */
    private int rowIndexAt(double mouseY) {
        int relativeY = (int) (mouseY - getY() + currentScrollOffset);
        return Math.floorDiv(relativeY, getRowHeight());
    }

    private int clampRowIndex(int index) {
        if (flatNodes.isEmpty()) return 0;
        return Math.max(0, Math.min(flatNodes.size() - 1, index));
    }

    /**
     * 把行下标限制在「当前可见的那一段」里。
     *
     * <p>框选时指针划出上下边界要能继续延伸，但不能顺手把滚出视野的行也选进来 ——
     * 那些行玩家根本没看见，而选中的设备是要拿去绑频段的（会改角色），
     * 误选一屏看不见的设备比「拖不到底」糟糕得多。想选更多就边拖边滚轮。
     */
    private int visibleRowIndex(int index) {
        if (flatNodes.isEmpty()) return 0;
        int rowHeight = getRowHeight();
        int firstVisible = currentScrollOffset / rowHeight;
        int visibleRows = Math.max(1, height / rowHeight);
        int lastVisible = Math.min(flatNodes.size() - 1, firstVisible + visibleRows - 1);
        return Math.max(firstVisible, Math.min(lastVisible, index));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        TreeNode clicked = getNodeAt(mouseX, mouseY);
        if (clicked == null || clicked.type == NodeType.SEARCH_HEADER) return;

        // Ctrl 点非设备行 = 进批量模式：这是「我知道要选一堆，别让我一直按着 Ctrl」的入口。
        // 设备行上的 Ctrl 单击仍然是「临时加选」，改的是这一次的选择，不改模式。
        if (clicked.type != NodeType.P2P && isControlDown() && !batchMode) {
            setBatchMode(true);
        }
        boolean additive = isAdditiveSelect();

        if (clicked.type == NodeType.P2P) {
            if (additive) {
                // 加选 / 再点一次取消：攒多台设备，一次绑到同一个频段
                P2PPosition key = positionKey(clicked.p2pInfo);
                String type = clicked.p2pInfo == null ? null : clicked.p2pInfo.p2pType();
                if (key != null) {
                    if (multiSelection.contains(key)) {
                        multiSelection.remove(key);
                    } else if (!multiSelection.isEmpty() && !Objects.equals(multiSelectionType, type)) {
                        // 类型不同：原来那几台全部取消，改成只选这一台。
                        // 混着不同类型去绑同一条频段没有意义 —— 频段在树里就是按类型分组的。
                        multiSelection.clear();
                        multiSelection.add(key);
                        multiSelectionType = type;
                    } else {
                        multiSelection.add(key);
                        if (multiSelectionType == null) {
                            multiSelectionType = type;
                        }
                    }
                    if (multiSelection.isEmpty()) {
                        multiSelectionType = null;
                    }
                }
            } else {
                clearMultiSelection();
            }
            selectNode(clicked);
            return;
        }

        if (additive) {
            // 批量模式下频段行是「整条频段的设备」快捷入口，不再是「选中目标频段」：
            // 点它就等于把这条频段两端全部收进多选集，不必先展开、也不必拖。
            if (clicked.type == NodeType.CHANNEL) {
                toggleChannelSelection(clicked);
            } else {
                // 类型行只负责展开折叠，不选设备 —— 点一下标题就选走一整个类型太容易误操作
                toggleNode(clicked);
            }
            return;
        }

        // 普通模式：类型 / 频段节点退出多选，并按原逻辑展开折叠、选中
        clearMultiSelection();
        toggleNode(clicked);
        selectNode(clicked);
    }

    /**
     * 批量模式下点频段行：整条频段的设备进多选集，再点一次则整条退掉。
     *
     * <p>做成「全有 / 全无」而不是无脑追加，是因为频段行本身就是这条频段的开关：
     * 追加的话第二次点击没有任何可见反馈，玩家会以为没点上。
     */
    private void toggleChannelSelection(TreeNode channelNode) {
        if (!(channelNode.data instanceof ChannelInfo info)) return;
        List<P2PInfo> members = info.p2pInfoList();
        if (members == null || members.isEmpty()) return;

        String type = anchorType(channelNode);
        boolean allSelected = true;
        for (P2PInfo member : members) {
            P2PPosition key = positionKey(member);
            if (key == null || !multiSelection.contains(key)) {
                allSelected = false;
                break;
            }
        }
        if (allSelected) {
            for (P2PInfo member : members) {
                P2PPosition key = positionKey(member);
                if (key != null) multiSelection.remove(key);
            }
            if (multiSelection.isEmpty()) multiSelectionType = null;
        } else {
            if (!multiSelection.isEmpty() && type != null
                    && !Objects.equals(multiSelectionType, type)) {
                // 与 Ctrl 加选同一条规则：类型不同就整组换掉，不做混合
                multiSelection.clear();
            }
            for (P2PInfo member : members) {
                P2PPosition key = positionKey(member);
                if (key != null) multiSelection.add(key);
            }
            if (multiSelectionType == null) multiSelectionType = type;
        }
        notifyMultiSelectionChanged();
    }

    /** 这次点击 / 拖动是否按「加选」处理：批量模式常开，或者临时按着 Ctrl。 */
    private boolean isAdditiveSelect() {
        return batchMode || isControlDown();
    }

    /** 是否处在批量操作模式（单击即选中，不需要按 Ctrl）。 */
    public boolean isBatchMode() {
        return batchMode;
    }

    /**
     * 开关批量操作模式。
     *
     * <p>退出时不清掉已选集合：批量选完正好接着选目标频段去绑定，
     * 模式只是决定「单击算不算加选」，与选择内容无关。
     */
    public void setBatchMode(boolean batchMode) {
        if (this.batchMode == batchMode) return;
        this.batchMode = batchMode;
        // 模式是在点击过程中被切换的（Ctrl 点非设备行）时，按住不放的那次点击
        // 也要按新语义处理，所以这里不打断现存的多选与拖动锚点。
        if (batchModeChangedListener != null) {
            batchModeChangedListener.accept(batchMode);
        }
    }

    public void setBatchModeChangedListener(Consumer<Boolean> listener) {
        this.batchModeChangedListener = listener;
    }

    /** 清空 Ctrl 多选（源槽上的 × 会用到）。 */
    public void clearMultiSelection() {
        multiSelection.clear();
        multiSelectionType = null;
    }

    /** 是否正在 Ctrl 拖动框选（界面层用它决定要不要抢先转发拖动事件）。 */
    public boolean isDragSelecting() {
        return dragAnchorIndex >= 0;
    }

    /** 多选集变化回调。配对条要实时显示「已选 N 台」，不能等到下一次同步包。 */
    public void setMultiSelectionChangedListener(Runnable listener) {
        this.multiSelectionChangedListener = listener;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        TreeNode hovered = getNodeAt(mouseX, mouseY);
        // 批量模式里左键被「选中 / 取消选中设备」占用了，频段行的展开折叠挪到右键。
        // 只在批量模式下改语义：普通模式左键点频段行仍然是展开/折叠。
        if (button == 1 && batchMode && hovered != null && hovered.type == NodeType.CHANNEL) {
            toggleNode(hovered);
            notifyMultiSelectionChanged();
            return true;
        }
        // 批量模式里不需要按着什么键，按下就能拖出框
        if (button == 0 && isAdditiveSelect()) {
            if (hovered != null && hovered.type != NodeType.SEARCH_HEADER) {
                beginDragSelection(hovered, mouseY);
            }
        }
        // 仍然交给原有逻辑：单击就是「加选 / 取消加选」，拖动则由 mouseDragged 接管
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void beginDragSelection(TreeNode node, double mouseY) {
        dragAnchorIndex = clampRowIndex(rowIndexAt(mouseY));
        dragAnchorNode = node;
        dragStarted = false;
        dragBaseSelection.clear();
        dragBaseSelection.addAll(multiSelection);
        dragBaseType = multiSelectionType;

        // 拖动是「刷子」：整段拖动只朝一个方向改状态。
        // 起手那一行的设备已经全在集合里时，这次拖动就是在取消；否则是在选中。
        // （往回拖不会反复横跳，因为每次都是从按下瞬间的快照重算。）
        LinkedHashSet<P2PPosition> anchorDevices = new LinkedHashSet<>();
        collectDevices(node, anchorDevices, anchorType(node));
        dragSelecting = anchorDevices.isEmpty() || !dragBaseSelection.containsAll(anchorDevices);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && dragAnchorIndex >= 0) {
            int endIndex = visibleRowIndex(rowIndexAt(mouseY));
            // 指针必须离开起手那一行才算拖动，否则 Ctrl 单击会被当成「只选了起手行」
            if (endIndex != dragAnchorIndex) {
                dragStarted = true;
            }
            if (dragStarted) {
                applyDragSelection(endIndex);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragAnchorIndex >= 0) {
            boolean wasDragging = dragStarted;
            clearDragState();
            if (wasDragging) {
                notifyMultiSelectionChanged();
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * 把 [锚点..endIndex] 之间的行按「覆盖到的设备」重新算进多选集。
     *
     * <p>每次都从按下瞬间的快照重算，而不是增量 add / remove —— 往回拖的时候要能把
     * 多出来或少掉的行还原。方向由 {@link #dragSelecting} 决定：扫过即选中，或者扫过即取消。
     */
    private void applyDragSelection(int endIndex) {
        if (flatNodes.isEmpty()) return;
        int anchorIndex = clampRowIndex(dragAnchorIndex);
        int from = Math.min(anchorIndex, endIndex);
        int to = Math.max(anchorIndex, endIndex);

        // 多选只允许同类型，和 Ctrl 加选同一条规则：起手那一行的类型优先，
        // 判定不出时才用按下前已有选中集合的类型。
        String pressedType = anchorType(dragAnchorNode);
        String allowedType = pressedType != null ? pressedType : dragBaseType;

        // 被这次拖动覆盖到的设备（已按类型过滤）
        LinkedHashSet<P2PPosition> covered = new LinkedHashSet<>();
        for (int i = from; i <= to; i++) {
            collectDevices(flatNodes.get(i), covered, allowedType);
        }
        // 从频段行起手：整条频段的两端都算覆盖到，而不只是被拖过的子行 ——
        // 频段行折叠起来时子行根本不在 flatNodes 里。
        if (dragAnchorNode != null && dragAnchorNode.type == NodeType.CHANNEL) {
            collectDevices(dragAnchorNode, covered, allowedType);
        }

        // 已有选中集合与这次拖动的类型不同时整组丢掉 —— 和 Ctrl 点到其他类型设备时
        // 「之前选中的全部取消」是同一个语义。多选集内部恒为同一类型（multiSelectionType
        // 始终记录它），所以这里只需比对一次整组，不用逐台查类型。
        LinkedHashSet<P2PPosition> picked = new LinkedHashSet<>();
        if (allowedType == null || dragBaseType == null || dragBaseType.equals(allowedType)) {
            picked.addAll(dragBaseSelection);
        }
        if (dragSelecting) {
            picked.addAll(covered);
        } else {
            picked.removeAll(covered);
        }

        multiSelection.clear();
        multiSelection.addAll(picked);
        multiSelectionType = picked.isEmpty() ? null : (allowedType != null ? allowedType : dragBaseType);
        notifyMultiSelectionChanged();
    }

    /** 起手那一行属于哪个 P2P 类型；判定不出时返回 null（不按类型过滤）。 */
    private String anchorType(TreeNode node) {
        if (node == null) return null;
        return switch (node.type) {
            case P2P -> node.p2pInfo == null ? null : node.p2pInfo.p2pType();
            case CHANNEL -> node.data instanceof ChannelInfo info ? info.p2pType()
                    : (node.parent == null ? null : node.parent.typeName);
            case P2P_TYPE -> node.typeName;
            case SEARCH_HEADER -> null;
        };
    }

    /**
     * 收集一行代表的设备：设备行是它自己，频段行是这条频段下的全部设备。
     * 类型行不展开 —— 划过类型标题不该把整个类型都选上。
     */
    private void collectDevices(TreeNode node, Set<P2PPosition> sink, String allowedType) {
        if (node == null) return;
        if (node.type == NodeType.P2P) {
            addDevice(node.p2pInfo, sink, allowedType);
        } else if (node.type == NodeType.CHANNEL && node.data instanceof ChannelInfo info) {
            List<P2PInfo> members = info.p2pInfoList();
            if (members == null) return;
            for (P2PInfo p : members) {
                addDevice(p, sink, allowedType);
            }
        }
    }

    private void addDevice(P2PInfo info, Set<P2PPosition> sink, String allowedType) {
        if (info == null) return;
        if (allowedType != null && !allowedType.equals(info.p2pType())) return;
        P2PPosition key = positionKey(info);
        if (key != null) {
            sink.add(key);
        }
    }

    private void notifyMultiSelectionChanged() {
        if (multiSelectionChangedListener != null) {
            multiSelectionChangedListener.run();
        }
    }

    private void clearDragState() {
        dragAnchorIndex = -1;
        dragAnchorNode = null;
        dragStarted = false;
        dragBaseSelection.clear();
        dragBaseType = null;
        dragSelecting = true;
    }

    /** Ctrl 是否按下。Screen 才有这个方法，widget 自己拿不到。 */
    private static boolean isControlDown() {
        var screen = Minecraft.getInstance().screen;
        return screen != null && screen.hasControlDown();
    }

    /** 设备身份键：维度 + 坐标 + 朝向，和服务端下发的 map 键一致 */
    private static P2PPosition positionKey(P2PInfo info) {
        if (info == null) return null;
        return new P2PPosition(info.position(), info.direction(),
                info.dimension().location().toString());
    }

    /**
     * 当前该作为“配对源”的设备列表。
     *
     * <p>有 Ctrl 多选时返回多选集合（按选中顺序，第一台会成为该频段的输入端），
     * 否则返回当前单选的设备。
     */
    public List<P2PInfo> getSourceCandidates() {
        if (!multiSelection.isEmpty()) {
            List<P2PInfo> picked = new ArrayList<>();
            for (P2PPosition key : multiSelection) {
                P2PInfo p = p2pInfoMap.get(key);
                if (p != null) picked.add(p);
            }
            if (!picked.isEmpty()) {
                return picked;
            }
        }
        return selectedP2PInfo == null ? List.of() : List.of(selectedP2PInfo);
    }

    private void selectNode(TreeNode node) {
        this.selectedNode = node;
        if (node.type == NodeType.P2P) {
            this.selectedP2PInfo = node.p2pInfo;
            if (selectionListener != null) selectionListener.accept(node.p2pInfo);
        } else {
            this.selectedP2PInfo = null;
            if (selectionListener != null) selectionListener.accept(null);
        }
        if (!isRebuilding && nodeSelectionListener != null) {
            nodeSelectionListener.accept(node);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationOutput) {
        this.defaultButtonNarrationText(narrationOutput);
    }
}
