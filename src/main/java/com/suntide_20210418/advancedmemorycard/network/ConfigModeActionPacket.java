package com.suntide_20210418.advancedmemorycard.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端 -> 服务端：配置模式 GUI 中的各种操作（绑定频段、改名、高亮、刷新、自动配置等）。
 * 通过 IActionMenu 接口回调，避免本类引用客户端类。
 */
public class ConfigModeActionPacket implements IMessage {

    // 客户端容器实现此接口以接收操作分发
    public interface IActionMenu {

        void handleAction(String action, String param);
    }

    private String action;
    private String param;

    public ConfigModeActionPacket() {}

    public ConfigModeActionPacket(String action) {
        this(action, null);
    }

    public ConfigModeActionPacket(String action, String param) {
        this.action = action;
        this.param = param;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, action);
        buf.writeBoolean(param != null);
        if (param != null) {
            ByteBufUtils.writeUTF8String(buf, param);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf);
        param = buf.readBoolean() ? ByteBufUtils.readUTF8String(buf) : null;
    }

    public String getAction() {
        return action;
    }

    public String getParam() {
        return param;
    }

    public static class Handler implements IMessageHandler<ConfigModeActionPacket, IMessage> {

        @Override
        public IMessage onMessage(final ConfigModeActionPacket msg, final MessageContext ctx) {
            // 1.7.10 的 SimpleNetworkWrapper 处理器跑在 Netty 线程上，且 MinecraftServer
            // 没有任务调度 API（已核对 srg 映射，func_152344_a 只在客户端 Minecraft 上）。
            // 批量绑定一次会改多台设备的网格状态并落盘，先入队，服务器 tick 末尾再执行。
            // 执行时重新取 openContainer 并校验类型 —— 玩家可能已经关掉界面换了容器。
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            ServerTaskQueue.enqueue(new Runnable() {

                @Override
                public void run() {
                    Container c = player.openContainer;
                    if (c instanceof IActionMenu) {
                        ((IActionMenu) c).handleAction(msg.getAction(), msg.getParam());
                    }
                }
            });
            return null;
        }
    }
}
