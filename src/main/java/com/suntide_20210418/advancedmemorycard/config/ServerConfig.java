package com.suntide_20210418.advancedmemorycard.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 服务端配置
 * <ul>
 *   <li>最大复制数量（区域体积上限）</li>
 *   <li>是否发送高亮信息至聊天栏</li>
 * </ul>
 */
public class ServerConfig {
    public final ForgeConfigSpec.IntValue maxCopyVolume;
    public final ForgeConfigSpec.BooleanValue sendHighlightToChat;
    public final ForgeConfigSpec.BooleanValue allowTeleport;

    public ServerConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Advanced Memory Card - 服务端配置")
                .push("server");

        maxCopyVolume = builder
                .comment("复制模式下允许的最大方块数量（体积），默认 2048",
                        "Maximum number of blocks (volume) allowed in Copy Mode. Default: 2048.")
                .defineInRange("maxCopyVolume", 2048, 1, Integer.MAX_VALUE);

        sendHighlightToChat = builder
                .comment("高亮 P2P 设备时是否在聊天栏发送位置信息和传送链接",
                        "Set to false to disable chat messages when highlighting a P2P device.")
                .define("sendHighlightToChat", true);

        allowTeleport = builder
                .comment("是否允许玩家通过配置模式界面的「传送」按钮直接传送到 P2P 设备位置",
                        "关闭时界面不显示传送按钮，服务端也会拒绝任何传送请求",
                        "Allow players to teleport to a P2P device via the 'Teleport' button in the Config Mode GUI.",
                        "When false the button is hidden and the server rejects teleport requests. Default: true.")
                .define("allowTeleport", true);

        builder.pop();
    }
}
