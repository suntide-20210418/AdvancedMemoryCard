package com.suntide_20210418.advancedmemorycard.p2p;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

public record P2PInfo(
        boolean isActive,
        boolean isOutput,
        boolean isConnected,
        boolean isMEP2P,
        boolean isPendingBind,
        int channel,
        int maxChannel,
        String frequency,
        String name,
        String p2pType,
        ResourceKey<Level> dimension,
        BlockPos position,
        Direction direction
){
    public @NotNull String toString() {
        StringJoiner join = new StringJoiner(" | ");
        join.add(position.toShortString());
        join.add(direction.toString());
        join.add(dimension.location().getPath());
        return join.toString();
    }

    public String toShortString() {
        return position.toShortString() + " | " + direction.toString();
    }
}
