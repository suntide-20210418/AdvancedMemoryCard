package com.suntide_20210418.advancedmemorycard.p2p;

import java.util.ArrayList;

public class ChannelInfo {
    public final String frequency;
    public final String alias;
    public final int p2pCount;
    public final int maxChannel;
    public final int channelRemaining;
    public final String p2pType;
    public final ArrayList<P2PInfo> p2pInfoList;

    public ChannelInfo(String frequency, String alias, int p2pCount, int maxChannel,
                       int channelRemaining, String p2pType, ArrayList<P2PInfo> p2pInfoList) {
        this.frequency = frequency;
        this.alias = alias;
        this.p2pCount = p2pCount;
        this.maxChannel = maxChannel;
        this.channelRemaining = channelRemaining;
        this.p2pType = p2pType;
        this.p2pInfoList = p2pInfoList;
    }

    public String frequency() { return frequency; }
    public String alias() { return alias; }
    public int p2pCount() { return p2pCount; }
    public int maxChannel() { return maxChannel; }
    public int channelRemaining() { return channelRemaining; }
    public String p2pType() { return p2pType; }
    public ArrayList<P2PInfo> p2pInfoList() { return p2pInfoList; }
}
