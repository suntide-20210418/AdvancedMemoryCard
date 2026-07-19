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
        public IMessage onMessage(ConfigModeActionPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container c = player.openContainer;
            if (c instanceof IActionMenu) {
                ((IActionMenu) c).handleAction(msg.getAction(), msg.getParam());
            }
            return null;
        }
    }
}
