package com.suntide_20210418.advancedmemorycard.p2p;

/**
 * 频道占用（used / max）。
 *
 * <p>来源是 AE2 的 {@code MEP2PTunnelPart#getExternalFacingNode()}：只有 ME P2P 会真正
 * 占用频道数，其它类型（物品 / 流体 / 能量 / 红石 / 光）在 {@code updateItemInfo} 里
 * 恒为 0。所以这里的每个工厂方法都先判类型，非 ME 一律返回 {@link #NONE}——
 * 以前树里给非 ME 频段显示「余 0」，那是把「没有这个概念」渲染成了「剩余为零」。
 */
public record P2PChannelUsage(int used, int max) {

    public static final P2PChannelUsage NONE = new P2PChannelUsage(0, 0);

    /** ME 隧道的类型 id（{@code IPartItem.getId(...).getPath()}） */
    public static final String ME_TYPE = "me_p2p_tunnel";

    public boolean available() {
        return max > 0;
    }

    public int remaining() {
        return Math.max(0, max - used);
    }

    /**
     * 单台 ME 设备在它外部网络上的占用。
     *
     * <p>输入端与输出端都算：{@code getExternalFacingNode()} 对输出端指向下游网络、
     * 对输入端指向上游网络，两者都是真实存在的“该侧网络用了多少频道”。
     * 注意输入端的数字反映的是上游那条网络，不是这台隧道自己消耗的部分。
     */
    public static P2PChannelUsage ofDevice(P2PInfo info) {
        if (info == null || !info.isMEP2P() || info.maxChannel() <= 0) {
            return NONE;
        }
        return new P2PChannelUsage(Math.max(0, info.channel()), info.maxChannel());
    }

    /**
     * 整条频段的占用（输入端口径：这条隧道从上游拿到了多少频道）。
     *
     * <p>用来画频段行与频段节点的“剩余”，跟 AE2 自己的语义一致；
     * 单台输出设备的占用请看 {@link #ofDevice(P2PInfo)}。
     */
    public static P2PChannelUsage ofChannel(ChannelInfo channel) {
        // 0000 是“未连接”分组，不是真实频段；给它算占用只会让人更困惑
        if (channel == null || "0000".equals(channel.frequency())
                || !isMe(channel.p2pType()) || channel.maxChannel() <= 0) {
            return NONE;
        }
        int max = channel.maxChannel();
        int used = Math.max(0, max - channel.channelRemaining());
        return new P2PChannelUsage(used, max);
    }


    /**
     * 列表 / 检查器里“这一行该显示什么数字”。
     *
     * <p>输入端就是它自己外部网络的那一对数字。
     *
     * <p>输出端分母换成“这条频段还能给它多少”：{@code 总数 = 输入剩余 + 本输出已用}，
     * 分子仍然是本输出自己的已用。等于在问“分到它头上的那份额度里，它用掉了多少”。
     *
     * @param channel 该设备所在频段（用于取输入端那一对数字），可为 null
     */
    public static P2PChannelUsage ofRow(P2PInfo device, ChannelInfo channel) {
        P2PChannelUsage own = ofDevice(device);
        if (!own.available()) {
            return NONE;
        }
        if (!device.isOutput()) {
            return own;
        }
        P2PChannelUsage input = ofChannel(channel);
        if (!input.available()) {
            return own;
        }
        int total = Math.max(0, input.remaining()) + own.used();
        return total > 0 ? new P2PChannelUsage(own.used(), total) : own;
    }

    public static boolean isMe(String p2pType) {
        return ME_TYPE.equals(p2pType);
    }

    /** 频段里只要有一台 ME 设备就算 ME 频段（同频段可能混装不同类型）。 */
    public static boolean isMeChannel(ChannelInfo channel) {
        if (channel == null || channel.p2pInfoList() == null) return false;
        for (P2PInfo p : channel.p2pInfoList()) {
            if (p != null && p.isMEP2P()) return true;
        }
        return false;
    }

    /**
     * 一个类型下「所有输出设备」的合计占用 —— 这是汇总列挂在类型行上的那一个。
     *
     * <p>频段 0000 的未绑定设备不参与：它们不属于任何通道，混进合计只会把
     * 一个和“这条通道占了多少”无关的数字加进来。
     */
    public static P2PChannelUsage ofType(P2PTypeInfo type) {
        if (type == null || !isMe(type.p2pType()) || type.p2pInfoList() == null) {
            return NONE;
        }
        int used = 0;
        int max = 0;
        for (P2PInfo p : type.p2pInfoList()) {
            if (p == null || !p.isOutput() || !p.isMEP2P() || "0000".equals(p.frequency())) continue;
            used += Math.max(0, p.channel());
            max += Math.max(0, p.maxChannel());
        }
        return max > 0 ? new P2PChannelUsage(used, max) : NONE;
    }
}
