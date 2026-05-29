package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ModSwitchPacket {
    InteractionHand hand;
    public ModSwitchPacket(InteractionHand hand) {
        this.hand = hand;
    }

    public static void encode(ModSwitchPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.hand.ordinal());
    }

    public static ModSwitchPacket decode(FriendlyByteBuf buffer) {
        int handOrdinal = buffer.readInt();
        InteractionHand hand = InteractionHand.values()[handOrdinal];
        return new ModSwitchPacket(hand);
    }

    public static void handle(ModSwitchPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack itemStack = msg.hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();
                if (itemStack.getItem() instanceof AdvancedMemoryCardItem memoryCardItem) {
                    memoryCardItem.cycleMode(player, itemStack);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
