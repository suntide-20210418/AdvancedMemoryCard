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
                // 必须是稳定标识，不能用 p2pInfo.hashCode()：后者是 14 个字段的全量哈希，
                // 连接状态一变（插拔输出端、上线/下线）id 就变。界面靠这个 id 记住
                // “当前选中/正在编辑的是哪一行”，id 一抖就会表现为改名后详情面板自己
                // 跳回未选中、跟随选中反复滚动。
                return "p2p:" + (p2pInfo != null
                        ? p2pInfo.dimensionId() + "@" + p2pInfo.position().getX() + ","
                          + p2pInfo.position().getY() + "," + p2pInfo.position().getZ()
                          + ":" + p2pInfo.direction().ordinal()
                        : "null")
                        + ":" + frequency;
            case SEARCH_HEADER:
            default:
                return "search_header";
        }
    }

    /**
     * 由 P2PInfo 直接构造节点 id，必须与 {@link #getNodeId()} 的 P2P 分支完全一致。
     * 重建树后用它把“待定位的 P2P”对回 flat 列表里的那一行，从而对上选中态。
     * 绝对不能再用 dimension()/省掉 :frequency 的旧写法 —— 那样 id 拼出来和
     * getNodeId() 对不上，选中行会丢失。
     */
    public static String p2pNodeId(P2PInfo info) {
        if (info == null) return null;
        return "p2p:" + info.dimensionId() + "@" + info.position().getX() + ","
                + info.position().getY() + "," + info.position().getZ()
                + ":" + info.direction().ordinal()
                + ":" + info.frequency();
    }
}
