package com.suntide_20210418.advancedmemorycard.p2p;

import java.util.ArrayList;

public record P2PTypeInfo(
        String p2pType,
        int channelCount,
        int p2pCount,
        ArrayList<ChannelInfo> channelInfoList,
        ArrayList<P2PInfo> p2pInfoList
){}
