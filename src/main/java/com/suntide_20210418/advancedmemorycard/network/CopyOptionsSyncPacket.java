package com.suntide_20210418.advancedmemorycard.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 服务端 -> 客户端：同步复制模式的粘贴选项与「复制后自动弹出」开关。
 *
 * <p>
 * 为什么不直接依赖物品 NBT 同步：{@code CopyModeMenu} 是空容器（没有槽位），
 * {@code Container#detectAndSendChanges} 不会为它发 {@code S2FPacketSetSlot}，
 * 所以服务端改写卡片 NBT 后客户端要等到下次重新打开界面才看得到新值 ——
 * 表现就是「点了复选框，画面纹丝不动，重开界面才变」。
 *
 * <p>
 * Handler 只把包放进队列，由客户端界面在自己线程里取用，避免在 netty 线程碰客户端类
 * （专用服务端也会加载这个 Handler，所以这里不能静态引用任何客户端类型）。
 */
public class CopyOptionsSyncPacket implements IMessage {

    private int mask;
    private boolean autoOpen;

    public CopyOptionsSyncPacket() {}

    public CopyOptionsSyncPacket(int mask, boolean autoOpen) {
        this.mask = mask;
        this.autoOpen = autoOpen;
    }

    public int getMask() {
        return mask;
    }

    public boolean isAutoOpen() {
        return autoOpen;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(mask);
        buf.writeBoolean(autoOpen);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        mask = buf.readInt();
        autoOpen = buf.readBoolean();
    }

    public static class Handler implements IMessageHandler<CopyOptionsSyncPacket, IMessage> {

        @Override
        public IMessage onMessage(CopyOptionsSyncPacket msg, MessageContext ctx) {
            NetworkHandler.copyOptionsQueue.add(msg);
            return null;
        }
    }
}
