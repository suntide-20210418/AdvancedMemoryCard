package com.suntide_20210418.advancedmemorycard.p2p;

import java.util.ArrayList;

/**
 * 频段信息 record。
 * alias 字段现在指向该频段中第一个输入端 P2P 的名称，不再是用户自定义的独立别名。
 */
public record ChannelInfo(
        String frequency,
        String alias,
        int p2pCount,
        int channelRemaining,
        String p2pType,
        ArrayList<P2PInfo> p2pInfoList
){}
