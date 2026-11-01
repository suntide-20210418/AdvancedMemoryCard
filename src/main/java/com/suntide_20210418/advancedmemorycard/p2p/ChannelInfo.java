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

    /**
     * 按频段数值排序。1.7.10 的频段是 64 位，服务端下发的字符串最长 16 位十六进制
     * （例如 {@code 000001A13FF44AA4}），用 {@code Integer.parseInt} 解析会溢出并抛
     * {@link NumberFormatException}，把客户端的界面 tick 整个打崩。
     *
     * <p>
     * 这里统一按无符号 64 位比较；遇到解析不了的脏数据就退化成「先比长度、再比字典序」，
     * 让排序永远返回一个确定的顺序，而不是把异常抛到渲染循环里。
     */
    public static int compareFrequency(String left, String right) {
        if (left == null && right == null) return 0;
        if (left == null) return -1;
        if (right == null) return 1;
        try {
            long leftValue = Long.parseUnsignedLong(left.trim(), 16);
            long rightValue = Long.parseUnsignedLong(right.trim(), 16);
            return Long.compareUnsigned(leftValue, rightValue);
        } catch (NumberFormatException ignored) {
            // 脏数据：退化成字符串比较，排序结果稳定即可
        }
        if (left.length() != right.length()) {
            return left.length() < right.length() ? -1 : 1;
        }
        return left.compareTo(right);
    }
}
