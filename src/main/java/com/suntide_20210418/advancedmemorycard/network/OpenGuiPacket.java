package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;
import com.suntide_20210418.advancedmemorycard.menu.CopyModeMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenGuiPacket {
    InteractionHand hand;
    public OpenGuiPacket(InteractionHand hand) {
        this.hand = hand;
    }

    public static void encode(OpenGuiPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.hand.ordinal());
    }

    public static OpenGuiPacket decode(FriendlyByteBuf buffer) {
        int handOrdinal = buffer.readInt();
        InteractionHand hand = InteractionHand.values()[handOrdinal];
        return new OpenGuiPacket(hand);
    }

    public static void handle(OpenGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            // 客户端只在「手持高级内存卡且处于复制模式」时发包，但服务端不能信客户端：
            // 界面是可写的，手被换掉或模式被切走之后就没有合法写入目标了。
            ItemStack stack = player.getItemInHand(msg.hand);
            if (!(stack.getItem() instanceof AdvancedMemoryCardItem)
                    || !(CardMode.of(stack) instanceof CopyMode)) {
                return;
            }
            player.openMenu(new SimpleMenuProvider(
                    (wnd, inv, pl) -> new CopyModeMenu(wnd, inv, msg.hand), stack.getHoverName()));
        });
        ctx.get().setPacketHandled(true);
    }
}