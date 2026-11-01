package com.suntide_20210418.advancedmemorycard.p2p;

import java.util.Objects;

public class TreeNode {

    public NodeType type;
    public String typeName;
    public String frequency;
    public Object data;
    public P2PInfo p2pInfo;
    public TreeNode parent;
    public boolean expanded = true;

    public TreeNode() {}

    public TreeNode(NodeType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TreeNode other = (TreeNode) obj;
        if (type != other.type) return false;
        if (type == NodeType.P2P_TYPE) return Objects.equals(typeName, other.typeName);
        if (type == NodeType.CHANNEL) return Objects.equals(frequency, other.frequency);
        if (type == NodeType.SEARCH_HEADER) return true;
        return Objects.equals(p2pInfo, other.p2pInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, typeName, frequency, p2pInfo);
    }

    public String getNodeId() {
        switch (type) {
            case P2P_TYPE:
                return "type:" + typeName;
            case CHANNEL:
                return "channel:" + frequency;
            case P2P:
                // 必须是稳定标识，不能用 p2pInfo.hashCode()：后者是全字段哈希，
                // 连接状态一变（插拔输出端、上线/下线）id 就变。界面靠这个 id 记住
                // “当前选中/正在编辑的是哪一行”，id 一抖就会表现为改名后详情面板自己
                // 跳回未选中、跟随选中反复滚动。
                return "p2p:" + (p2pInfo != null ? p2pInfo.dimensionId() + "@"
                    + p2pInfo.position()
                        .getX()
                    + ","
                    + p2pInfo.position()
                        .getY()
                    + ","
                    + p2pInfo.position()
                        .getZ()
                    + ":"
                    + p2pInfo.direction()
                        .ordinal()
                    : "null") + ":" + frequency;
            case SEARCH_HEADER:
            default:
                return "search_header";
        }
    }
}
