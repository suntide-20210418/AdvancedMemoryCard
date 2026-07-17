package com.suntide_20210418.advancedmemorycard.p2p;

import java.util.ArrayList;

public class P2PTypeInfo {
    public final String p2pType;
    public final int channelCount;
    public final int p2pCount;
    public final ArrayList<ChannelInfo> channelInfoList;
    public final ArrayList<P2PInfo> p2pInfoList;

    public P2PTypeInfo(String p2pType, int channelCount, int p2pCount,
                       ArrayList<ChannelInfo> channelInfoList, ArrayList<P2PInfo> p2pInfoList) {
        this.p2pType = p2pType;
        this.channelCount = channelCount;
        this.p2pCount = p2pCount;
        this.channelInfoList = channelInfoList;
        this.p2pInfoList = p2pInfoList;
    }

    public String p2pType() { return p2pType; }
    public int channelCount() { return channelCount; }
    public int p2pCount() { return p2pCount; }
    public ArrayList<ChannelInfo> channelInfoList() { return channelInfoList; }
    public ArrayList<P2PInfo> p2pInfoList() { return p2pInfoList; }
}
