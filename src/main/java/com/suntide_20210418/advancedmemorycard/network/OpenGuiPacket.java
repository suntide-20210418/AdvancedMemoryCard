package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.client.gui.menu.AdvancedMemoryCardMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
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
            if (player != null) {
                // 打开 GUI
                player.openMenu(new SimpleMenuProvider((wnd, inv, pl) -> new AdvancedMemoryCardMenu(wnd, inv, msg.hand), Component.empty()));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}