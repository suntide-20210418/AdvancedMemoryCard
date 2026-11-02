package com.suntide_20210418.advancedmemorycard.config;

import net.minecraftforge.common.config.Configuration;

public class ServerConfig {

    public final int maxCopyVolume;
    public final boolean sendHighlightToChat;
    public final boolean allowTeleport;

    public ServerConfig(Configuration config) {
        config.load();
        config.addCustomCategoryComment("server", "Advanced Memory Card - 服务端配置");

        maxCopyVolume = config
            .getInt("maxCopyVolume", "server", 2048, 1, Integer.MAX_VALUE, "复制模式下允许的最大方块数量（体积），默认 2048");

        sendHighlightToChat = config.getBoolean("sendHighlightToChat", "server", true, "高亮 P2P 设备时是否在聊天栏发送位置信息和传送链接");

        allowTeleport = config
            .getBoolean("allowTeleport", "server", true, "是否允许玩家通过配置模式界面的「传送」按钮直接传送到 P2P 设备位置（关闭时界面不显示按钮，服务端也会拒绝传送请求）");
    }
}
