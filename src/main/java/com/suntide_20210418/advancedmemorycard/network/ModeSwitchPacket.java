package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import com.suntide_20210418.advancedmemorycard.utils.ResourceLocationHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ModeSwitchPacket(InteractionHand hand) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ModeSwitchPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocationHelper.modLoc("mode_switch"));

    public static final StreamCodec<FriendlyByteBuf, ModeSwitchPacket> STREAM_CODEC =
            StreamCodec.of(ModeSwitchPacket::encode, ModeSwitchPacket::decode);

    private static void encode(FriendlyByteBuf buffer, ModeSwitchPacket msg) {
        buffer.writeInt(msg.hand.ordinal());
    }

    private static ModeSwitchPacket decode(FriendlyByteBuf buffer) {
        int handOrdinal = buffer.readInt();
        InteractionHand hand = InteractionHand.values()[handOrdinal];
        return new ModeSwitchPacket(hand);
    }

    public static void handleServer(ModeSwitchPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                ItemStack itemStack = msg.hand == InteractionHand.MAIN_HAND
                        ? player.getMainHandItem()
                        : player.getOffhandItem();
                if (itemStack.getItem() instanceof AdvancedMemoryCardItem memoryCardItem) {
                    memoryCardItem.cycleMode(player, itemStack);
                }
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
