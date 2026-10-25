package com.suntide_20210418.advancedmemorycard.p2p;

import net.minecraft.network.chat.Component;
import java.util.Objects;

/**
 * 树节点类 - 用于 P2P 树形结构
 * 可在服务端和客户端共用
 */
public class TreeNode {
    public NodeType type;
    public String typeName;      // P2P 类型名称
    public String frequency;      // 频道频率（十六进制字符串格式）
    public Component displayComponent;
    public boolean expanded = true;
    public Object data;          // P2PTypeInfo 或 ChannelInfo
    public P2PInfo p2pInfo;
    public TreeNode parent;

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

    /**
     * 获取节点唯一标识
     *
     * <p>P2P 节点必须用"设备身份"（维度 + 坐标 + 朝向）做标识，不能用
     * {@code P2PInfo.hashCode()}：那是 record 的全字段哈希，名字、连接状态、
     * 频道占用数任一项变化都会换 id，于是树每 tick 重建后
     * {@code restoreSelectedNode} 找不到原节点，选中项被清空 ——
     * 表现就是"改完名字详情面板自己跳回未选中"。
     */
    public String getNodeId() {
        return switch (type) {
            case P2P_TYPE -> "type:" + typeName;
            case CHANNEL -> "channel:" + frequency;
            case P2P -> p2pNodeId(p2pInfo);
            case SEARCH_HEADER -> "search_header";
        };
    }

    /**
     * 生成 P2P 节点的稳定标识：维度 + 坐标 + 朝向。与名字 / 频段 / 连接状态无关。
     */
    public static String p2pNodeId(P2PInfo info) {
        if (info == null) {
            return "p2p:null";
        }
        return "p2p:" + info.dimension().location() + "@" + info.position().asLong()
                + ":" + info.direction().ordinal();
    }
}
