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
                return "p2p:" + (p2pInfo != null ? p2pInfo.hashCode() : "null") + ":" + frequency;
            case SEARCH_HEADER:
            default:
                return "search_header";
        }
    }
}
