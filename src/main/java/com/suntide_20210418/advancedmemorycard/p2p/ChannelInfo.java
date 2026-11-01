package com.suntide_20210418.advancedmemorycard.p2p;

import java.util.ArrayList;
import java.util.Map;

public class ChannelInfo {

    public final String frequency;
    public final String alias;
    public final int p2pCount;
    public final int maxChannel;
    public final int channelRemaining;
    public final String p2pType;
    public final ArrayList<P2PInfo> p2pInfoList;

    public ChannelInfo(String frequency, String alias, int p2pCount, int maxChannel, int channelRemaining,
        String p2pType, ArrayList<P2PInfo> p2pInfoList) {
        this.frequency = frequency;
        this.alias = alias;
        this.p2pCount = p2pCount;
        this.maxChannel = maxChannel;
        this.channelRemaining = channelRemaining;
        this.p2pType = p2pType;
        this.p2pInfoList = p2pInfoList;
    }

    public String frequency() {
        return frequency;
    }

    public String alias() {
        return alias;
    }

    public int p2pCount() {
        return p2pCount;
    }

    public int maxChannel() {
        return maxChannel;
    }

    public int channelRemaining() {
        return channelRemaining;
    }

    public String p2pType() {
        return p2pType;
    }

    public ArrayList<P2PInfo> p2pInfoList() {
        return p2pInfoList;
    }

    /**
     * 按「频段 + P2P 类型」查频道信息。
     *
     * <p>
     * 1.7.10 的同步 map 以 {@code frequency|p2pType} 为键（同一频段可能被不同类型的 P2P 占用），
     * 所以只按频段查永远是 miss。为了兼容仍可能出现的纯频段键，这里查不到时退回纯频段。
     */
    public static ChannelInfo find(Map<String, ChannelInfo> map, String frequency, String p2pType) {
        if (map == null || frequency == null) return null;
        if (p2pType != null) {
            ChannelInfo typed = map.get(frequency + "|" + p2pType);
            if (typed != null) return typed;
        }
        return map.get(frequency);
    }
}
