package com.suntide_20210418.advancedmemorycard.p2p;

import java.util.ArrayList;

public record ChannelInfo(
        short frequency,
        String alias,
        int p2pCount,
        int channelRemaining,
        String p2pType,
        ArrayList<P2PInfo> p2pInfoList
){}
