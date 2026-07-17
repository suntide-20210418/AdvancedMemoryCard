package com.suntide_20210418.advancedmemorycard.client.gui.widgets;

import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import com.suntide_20210418.advancedmemorycard.p2p.*;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.function.Consumer;

import static com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod.getLogger;

/**
 * P2P 树形控件（1.12.2 vanilla 实现）。<br/>
 * 显示 P2P类型 -> 频段 -> P2P 的树形结构。
 */
public class P2PTreeWidget {

    public P2PTreeWidget(int x, int y, int width, int height) {
        setBounds(x, y, width, height);
    }

    private static final int ROW_HEIGHT_FALLBACK = 12;

    private int x;
    private int y;
    private int width;
    private int height;

    private Map<String, P2PTypeInfo> p2pTypeInfoMap = new HashMap<>();
    private final Map<String, Boolean> expandedStateCache = new HashMap<>();
    private Map<P2PPosition, P2PInfo> p2pInfoMap = new HashMap<>();

    private List<TreeNode> flatNodes = new ArrayList<>();
    private int currentScrollOffset = 0;
    private int totalContentHeight = 0;

    private TreeNode selectedNode = null;
    private P2PInfo selectedP2PInfo = null;
    private Map<String, ChannelInfo> channelInfoMap = new HashMap<>();

    private Consumer<P2PInfo> selectionListener;
    private Consumer<TreeNode> nodeSelectionListener;

    private TreeNode hoveredNode = null;

    private String currentSearchQuery = null;
    private boolean isSearchMode = false;
    private int searchResultCount = 0;

    private P2PInfo pendingNavigateTarget = null;
    private boolean scrollToSelected = false;
    private boolean isNavigating = false;
    private boolean isRebuilding = false;

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static int getRowHeight() {
        return ModConfigs.getClientConfig().treeRowHeight > 0 ? ModConfigs.getClientConfig().treeRowHeight : ROW_HEIGHT_FALLBACK;
    }

    private int getColorBg() { return ModConfigs.getClientConfig().treeColorBg; }
    private int getColorSelected() { return ModConfigs.getClientConfig().treeColorSelected; }
    private int getColorHover() { return ModConfigs.getClientConfig().treeColorHover; }
    private int getIndentWidth() { return ModConfigs.getClientConfig().treeIndentWidth; }
    private int getIconTextSpacing() { return ModConfigs.getClientConfig().treeIconTextSpacing; }
    private int getExpandIconWidth() { return ModConfigs.getClientConfig().treeExpandIconWidth; }
    private int getChannelWarningThreshold() { return ModConfigs.getClientConfig().treeChannelWarningThreshold; }
    private int getP2PNameMaxLength() { return ModConfigs.getClientConfig().treeP2PNameMaxLength; }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

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

    public void navigateToP2P(P2PInfo targetP2P) {
        if (targetP2P == null || isNavigating) return;
        this.pendingNavigateTarget = targetP2P;
        this.scrollToSelected = true;
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

    private void rebuildTree() {
        Set<String> expandedTypeIds = new HashSet<>();
        saveExpandedState(expandedTypeIds);

        String selectedNodeId = getSelectedNodeId();

        P2PInfo targetP2P = pendingNavigateTarget;
        if (targetP2P == null && selectedNode != null && selectedNode.type == NodeType.P2P && selectedNode.p2pInfo != null) {
            targetP2P = selectedNode.p2pInfo;
        }

        if (targetP2P != null) {
            expandedTypeIds.add("type:" + targetP2P.p2pType());
            expandedTypeIds.add("channel:" + targetP2P.frequency());
            expandedStateCache.put("type:" + targetP2P.p2pType(), true);
            expandedStateCache.put("channel:" + targetP2P.frequency(), true);
        }

        if (pendingNavigateTarget != null) {
            selectedNodeId = "p2p:" + pendingNavigateTarget.hashCode() + ":" + pendingNavigateTarget.frequency();
        }

        flatNodes.clear();

        if (isSearchMode && currentSearchQuery != null) {
            TreeNode headerNode = new TreeNode();
            headerNode.type = NodeType.SEARCH_HEADER;
            flatNodes.add(headerNode);
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
            scrollToSelectedNode();
            scrollToSelected = false;
        }

        pendingNavigateTarget = null;
    }

    private void scrollToSelectedNode() {
        if (selectedNode == null) return;
        int index = flatNodes.indexOf(selectedNode);
        if (index < 0) return;
        int rowH = getRowHeight();
        int nodeY = index * rowH;
        int visibleHeight = getHeight();
        int targetScroll = nodeY - visibleHeight / 2 + rowH / 2;
        targetScroll = Math.max(0, Math.min(targetScroll, Math.max(0, getTotalContentHeight() - visibleHeight)));
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
            typeNode.expanded = expandedTypeIds.contains(typeNode.getNodeId())
                    || expandedStateCache.getOrDefault(typeNode.getNodeId(), false);
            typeNode.data = typeInfo;
            flatNodes.add(typeNode);

            if (typeNode.expanded) {
                List<ChannelInfo> channels = typeInfo.channelInfoList();
                if (channels != null) {
                    channels.sort(Comparator.comparingInt(ch -> Integer.parseInt(ch.frequency(), 16)));
                    for (ChannelInfo channelInfo : channels) {
                        TreeNode channelNode = new TreeNode();
                        channelNode.type = NodeType.CHANNEL;
                        channelNode.frequency = channelInfo.frequency();
                        channelNode.expanded = expandedTypeIds.contains(channelNode.getNodeId())
                                || expandedStateCache.getOrDefault(channelNode.getNodeId(), false);
                        channelNode.data = channelInfo;
                        channelNode.parent = typeNode;
                        flatNodes.add(channelNode);

                        if (channelNode.expanded) {
                            List<P2PInfo> p2ps = channelInfo.p2pInfoList();
                            if (p2ps != null) {
                                p2ps.sort((a, b) -> Boolean.compare(a.isOutput(), b.isOutput()));
                                for (P2PInfo p2pInfo : p2ps) {
                                    TreeNode p2pNode = new TreeNode();
                                    p2pNode.type = NodeType.P2P;
                                    p2pNode.p2pInfo = p2pInfo;
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

    public P2PInfo getSelectedP2PInfo() { return selectedP2PInfo; }
    public TreeNode getSelectedNode() { return selectedNode; }
    public int getScrollOffset() { return currentScrollOffset; }
    public void setScrollOffset(int scrollOffset) { this.currentScrollOffset = scrollOffset; }
    public int getTotalContentHeight() { return flatNodes.size() * getRowHeight(); }

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

    private void buildSearchResults(String query) {
        String lowerQuery = query.toLowerCase();
        Map<String, Map<String, List<P2PInfo>>> groupedResults = new LinkedHashMap<>();
        searchResultCount = 0;

        for (P2PTypeInfo typeInfo : p2pTypeInfoMap.values()) {
            List<P2PInfo> allP2ps = typeInfo.p2pInfoList();
            if (allP2ps == null) continue;
            List<P2PInfo> matchedP2ps = new ArrayList<>();
            for (P2PInfo p2p : allP2ps) {
                if (matchesSearch(p2p, lowerQuery)) matchedP2ps.add(p2p);
            }
            if (matchedP2ps.isEmpty()) continue;
            Map<String, List<P2PInfo>> byFrequency = new LinkedHashMap<>();
            for (P2PInfo p2p : matchedP2ps) {
                String freq = String.valueOf(p2p.frequency());
                byFrequency.computeIfAbsent(freq, k -> new ArrayList<>()).add(p2p);
            }
            groupedResults.put(typeInfo.p2pType(), byFrequency);
            searchResultCount += matchedP2ps.size();
        }

        for (Map.Entry<String, Map<String, List<P2PInfo>>> typeEntry : groupedResults.entrySet()) {
            String typeName = typeEntry.getKey();
            Map<String, List<P2PInfo>> freqMap = typeEntry.getValue();
            P2PTypeInfo typeInfo = p2pTypeInfoMap.get(typeName);
            int typeMatchCount = freqMap.values().stream().mapToInt(List::size).sum();

            TreeNode typeNode = new TreeNode();
            typeNode.type = NodeType.P2P_TYPE;
            typeNode.typeName = typeName;
            typeNode.expanded = true;
            typeNode.data = typeInfo;
            flatNodes.add(typeNode);

            List<String> sortedFreqs = new ArrayList<>(freqMap.keySet());
            sortedFreqs.sort(Comparator.comparingInt(f -> Integer.parseInt(f, 16)));
            for (String freq : sortedFreqs) {
                List<P2PInfo> p2ps = freqMap.get(freq);
                ChannelInfo channelInfo = channelInfoMap.get(freq);
                TreeNode channelNode = new TreeNode();
                channelNode.type = NodeType.CHANNEL;
                channelNode.frequency = freq;
                channelNode.expanded = true;
                channelNode.data = channelInfo;
                channelNode.parent = typeNode;
                flatNodes.add(channelNode);

                p2ps.sort((a, b) -> Boolean.compare(a.isOutput(), b.isOutput()));
                for (P2PInfo p2pInfo : p2ps) {
                    TreeNode p2pNode = new TreeNode();
                    p2pNode.type = NodeType.P2P;
                    p2pNode.p2pInfo = p2pInfo;
                    p2pNode.parent = channelNode;
                    p2pNode.frequency = String.valueOf(p2pInfo.frequency());
                    flatNodes.add(p2pNode);
                }
            }
        }
    }

    private boolean matchesSearch(P2PInfo p2p, String lowerQuery) {
        if (p2p.p2pType() != null && p2p.p2pType().toLowerCase().contains(lowerQuery)) return true;
        if (String.valueOf(p2p.frequency()).toLowerCase().contains(lowerQuery)) return true;
        if (p2p.name() != null && p2p.name().toLowerCase().contains(lowerQuery)) return true;
        ChannelInfo channelInfo = channelInfoMap.get(String.valueOf(p2p.frequency()));
        if (channelInfo != null && channelInfo.alias() != null && channelInfo.alias().toLowerCase().contains(lowerQuery)) return true;
        return false;
    }

    private String getSelectedNodeId() {
        if (selectedNode == null) return null;
        return selectedNode.getNodeId();
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
        if (node.type == NodeType.SEARCH_HEADER) return;
        node.expanded = !node.expanded;
        rebuildTree();
    }

    private String formatTypeDisplayName(String typeName, P2PTypeInfo typeInfo) {
        return TextFormatting.GOLD + typeName + " "
                + TextFormatting.GRAY + String.format("(%d P2P, %d Freq)", typeInfo.p2pCount(), typeInfo.channelCount());
    }

    private String formatChannelDisplayName(ChannelInfo channelInfo) {
        String alias = channelInfo.alias();
        String freq = channelInfo.frequency();
        int channelRemaining = channelInfo.channelRemaining();
        int p2pCount = channelInfo.p2pCount();
        boolean isMEP2P = channelInfo.p2pType().equals("ME");

        String aliasStr;
        if (alias != null && !alias.equals(freq)) {
            aliasStr = TextFormatting.AQUA + alias + " " + TextFormatting.DARK_AQUA + "(" + freq + ")";
        } else {
            aliasStr = TextFormatting.AQUA + freq;
        }

        StringBuilder sb = new StringBuilder(aliasStr);
        if (isMEP2P) {
            TextFormatting channelColor = TextFormatting.GREEN;
            if (channelRemaining <= 0) channelColor = TextFormatting.RED;
            else if (channelRemaining < getChannelWarningThreshold()) channelColor = TextFormatting.YELLOW;
            sb.append(" ").append(channelColor).append("[").append(p2pCount).append(" P2P | ").append(channelRemaining).append("]");
        } else {
            sb.append(" ").append(TextFormatting.GREEN).append("[").append(p2pCount).append(" P2P]");
        }
        return sb.toString();
    }

    private String formatP2PDisplayName(P2PInfo p2pInfo) {
        String name = p2pInfo.name().isEmpty() ? p2pInfo.toShortString() : p2pInfo.name();
        boolean isOutput = p2pInfo.isOutput();
        boolean isActive = p2pInfo.isActive();
        boolean isConnected = p2pInfo.isConnected();
        boolean isPendingBind = p2pInfo.isPendingBind();

        String statusMarker;
        if (!isActive) statusMarker = TextFormatting.GRAY + "[ ]";
        else if (isConnected) statusMarker = TextFormatting.GREEN + "[*]";
        else statusMarker = TextFormatting.RED + "[!]";

        String dirMarker = isOutput ? TextFormatting.YELLOW + "[O]" : TextFormatting.BLUE + "[I]";

        String nameColor;
        if (isPendingBind) nameColor = TextFormatting.LIGHT_PURPLE + "" + TextFormatting.BOLD;
        else if (isActive && isConnected) nameColor = TextFormatting.WHITE + "";
        else nameColor = TextFormatting.YELLOW + "";

        return statusMarker + " " + dirMarker + " " + nameColor + truncateString(name, getP2PNameMaxLength()) + TextFormatting.RESET;
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }

    private TreeNode getNodeAt(int mouseX, int mouseY) {
        if (mouseX < getX() || mouseX > getX() + width || mouseY < getY() || mouseY > getY() + height) return null;
        int relativeY = (mouseY - getY() + currentScrollOffset);
        int index = relativeY / getRowHeight();
        if (index >= 0 && index < flatNodes.size()) return flatNodes.get(index);
        return null;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        TreeNode clicked = getNodeAt(mouseX, mouseY);
        if (clicked != null) {
            if (clicked.type == NodeType.SEARCH_HEADER) return false;
            toggleNode(clicked);
            selectNode(clicked);
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(int mouseX, int mouseY, double delta) {
        if (mouseX < getX() || mouseX > getX() + width || mouseY < getY() || mouseY > getY() + height) return false;
        int rowH = getRowHeight();
        currentScrollOffset += (int) (-delta * rowH);
        clampScroll();
        return true;
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, getTotalContentHeight() - getHeight());
        if (currentScrollOffset < 0) currentScrollOffset = 0;
        if (currentScrollOffset > maxScroll) currentScrollOffset = maxScroll;
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
        if (!isRebuilding && nodeSelectionListener != null) nodeSelectionListener.accept(node);
    }

    public void draw(FontRenderer font, int mouseX, int mouseY) {
        hoveredNode = getNodeAt(mouseX, mouseY);

        Minecraft mc = Minecraft.getMinecraft();
        int scale = new ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(getX() * scale, mc.displayHeight - (getY() + height) * scale, width * scale, height * scale);

        net.minecraft.client.gui.Gui.drawRect(getX(), getY(), getX() + width, getY() + height, getColorBg());

        if (flatNodes.isEmpty()) {
            String emptyText = TranslateHelper.P2PTree.empty();
            int textWidth = font.getStringWidth(emptyText);
            font.drawString(emptyText, getX() + (width - textWidth) / 2, getY() + height / 2 - 4, -1);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            return;
        }

        int rowH = getRowHeight();
        int startIndex = currentScrollOffset / rowH;
        int endIndex = Math.min(flatNodes.size(), startIndex + (height + rowH - 1) / rowH + 1);
        int yOffset = getY() - currentScrollOffset + startIndex * rowH;

        for (int i = startIndex; i < endIndex; i++) {
            TreeNode node = flatNodes.get(i);
            int rowY = yOffset + (i - startIndex) * rowH;
            if (rowY + rowH < getY() || rowY > getY() + height) continue;

            int indent = getIndent(node);
            int iconX = getX() + indent;
            int textX = getX() + indent + getExpandIconWidth() + getIconTextSpacing();
            int textY = rowY + (rowH - 8) / 2;

            if (node == selectedNode) {
                net.minecraft.client.gui.Gui.drawRect(getX(), rowY, getX() + width, rowY + rowH, getColorSelected());
            } else if (node == hoveredNode) {
                net.minecraft.client.gui.Gui.drawRect(getX(), rowY, getX() + width, rowY + rowH, getColorHover());
            }

            if (node.type == NodeType.P2P_TYPE || node.type == NodeType.CHANNEL) {
                String iconSymbol = node.expanded ? "-" : "+";
                font.drawString(iconSymbol, iconX, rowY + (rowH - 8) / 2, -1);
            }

            String display = getDisplayString(node);
            font.drawString(display, textX, textY, -1);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private String getDisplayString(TreeNode node) {
        switch (node.type) {
            case P2P_TYPE:
                if (node.data instanceof P2PTypeInfo) return formatTypeDisplayName(node.typeName, (P2PTypeInfo) node.data);
                return node.typeName != null ? node.typeName : "";
            case CHANNEL:
                if (node.data instanceof ChannelInfo) return formatChannelDisplayName((ChannelInfo) node.data);
                return node.frequency != null ? node.frequency : "";
            case P2P:
                if (node.p2pInfo != null) return formatP2PDisplayName(node.p2pInfo);
                return TranslateHelper.P2PTree.unknownP2P();
            case SEARCH_HEADER:
            default:
                return TranslateHelper.P2PTree.searchResultCount(searchResultCount);
        }
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
}
