package com.suntide_20210418.advancedmemorycard.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端 -> 服务端：高级内存卡对某个方块的右键交互。
 *
 * <p>为什么不直接靠原版 C08 让服务端分派到 {@code Item#onItemUseFirst}：
 * <ul>
 * <li>客户端在 {@code PlayerControllerMP#onPlayerRightClick} 里会先跑一遍
 * {@code block.onBlockActivated}，AE2 的线缆/部件在那一步就会执行原生内存卡逻辑
 * （聊天栏的“成功载入设置”就是它打的）；</li>
 * <li>服务端是否把 C08 落到 {@code activateBlockOrUseItem} 还受 {@code hasMoved}、
 * 距离、{@code PlayerInteractEvent} 取消等前置条件影响，一旦被吞掉，物品方法根本不会被调用。</li>
 * </ul>
 *
 * <p>所以这里改由客户端在 {@code onItemUseFirst} 里直接发本包并返回 true，
 * 客户端不再跑本地方块交互、也不再发 C08；服务端收到本包后统一执行，
 * 行为完全由服务端决定，AE2 没有插手的余地。
 *
 * <p>1.7.10 的 SimpleNetworkWrapper 在 netty 线程回调，但原版的开箱/开工作台也是在同一线程
 * 直接执行的，所以这里直接操作玩家是可接受的。
 */
public class CardInteractPacket implements IMessage {

    private int x;
    private int y;
    private int z;
    private int side;
    private float hitX;
    private float hitY;
    private float hitZ;
    private boolean sneaking;

    public CardInteractPacket() {}

    public CardInteractPacket(int x, int y, int z, int side, float hitX, float hitY, float hitZ, boolean sneaking) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.side = side;
        this.hitX = hitX;
        this.hitY = hitY;
        this.hitZ = hitZ;
        this.sneaking = sneaking;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeByte(side);
        buf.writeFloat(hitX);
        buf.writeFloat(hitY);
        buf.writeFloat(hitZ);
        buf.writeBoolean(sneaking);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        side = buf.readByte();
        hitX = buf.readFloat();
        hitY = buf.readFloat();
        hitZ = buf.readFloat();
        sneaking = buf.readBoolean();
    }

    public static class Handler implements IMessageHandler<CardInteractPacket, IMessage> {

        @Override
        public IMessage onMessage(CardInteractPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            ItemStack held = player.getHeldItem();
            // 服务端复核手持物：客户端说什么不算数
            if (held == null || !(held.getItem() instanceof AdvancedMemoryCardItem)) {
                return null;
            }
            ((AdvancedMemoryCardItem) held.getItem()).handleBlockInteract(
                player,
                player.worldObj,
                msg.x,
                msg.y,
                msg.z,
                msg.side,
                msg.hitX,
                msg.hitY,
                msg.hitZ,
                msg.sneaking);
            if (AdvancedMemoryCardMod.getLogger() != null) {
                AdvancedMemoryCardMod.getLogger()
                    .info(
                        "[amc] interact packet done, openContainer={}",
                        player.openContainer == null ? "null"
                            : player.openContainer.getClass()
                                .getSimpleName());
            }
            return null;
        }
    }
}
