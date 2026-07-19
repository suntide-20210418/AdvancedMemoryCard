package com.suntide_20210418.advancedmemorycard.p2p;

import java.util.Objects;

import net.minecraftforge.common.util.ForgeDirection;

import com.suntide_20210418.advancedmemorycard.utils.BlockPos;

public class P2PInfo {

    public final boolean isActive;
    public final boolean isOutput;
    public final boolean isConnected;
    public final boolean isMEP2P;
    public final boolean isPendingBind;
    public final int channel;
    public final int maxChannel;
    public final String frequency;
    public final String name;
    public final String p2pType;
    public final String dimension;
    public final int dimensionId;
    public final BlockPos position;
    public final ForgeDirection direction;

    public P2PInfo(boolean isActive, boolean isOutput, boolean isConnected, boolean isMEP2P, boolean isPendingBind,
        int channel, int maxChannel, String frequency, String name, String p2pType, String dimension, int dimensionId,
        BlockPos position, ForgeDirection direction) {
        this.isActive = isActive;
        this.isOutput = isOutput;
        this.isConnected = isConnected;
        this.isMEP2P = isMEP2P;
        this.isPendingBind = isPendingBind;
        this.channel = channel;
        this.maxChannel = maxChannel;
        this.frequency = frequency;
        this.name = name;
        this.p2pType = p2pType;
        this.dimension = dimension;
        this.dimensionId = dimensionId;
        this.position = position;
        this.direction = direction;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isOutput() {
        return isOutput;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isMEP2P() {
        return isMEP2P;
    }

    public boolean isPendingBind() {
        return isPendingBind;
    }

    public int channel() {
        return channel;
    }

    public int maxChannel() {
        return maxChannel;
    }

    public String frequency() {
        return frequency;
    }

    public String name() {
        return name;
    }

    public String p2pType() {
        return p2pType;
    }

    public String dimension() {
        return dimension;
    }

    public int dimensionId() {
        return dimensionId;
    }

    public BlockPos position() {
        return position;
    }

    public ForgeDirection direction() {
        return direction;
    }

    public String toString() {
        return position.toString() + " | " + direction + " | " + (dimension != null ? dimension : "?");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        P2PInfo p2PInfo = (P2PInfo) o;
        return isActive == p2PInfo.isActive && isOutput == p2PInfo.isOutput
            && isConnected == p2PInfo.isConnected
            && isMEP2P == p2PInfo.isMEP2P
            && isPendingBind == p2PInfo.isPendingBind
            && channel == p2PInfo.channel
            && maxChannel == p2PInfo.maxChannel
            && Objects.equals(frequency, p2PInfo.frequency)
            && Objects.equals(name, p2PInfo.name)
            && Objects.equals(p2pType, p2PInfo.p2pType)
            && Objects.equals(dimension, p2PInfo.dimension)
            && dimensionId == p2PInfo.dimensionId
            && Objects.equals(position, p2PInfo.position)
            && direction == p2PInfo.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            isActive,
            isOutput,
            isConnected,
            isMEP2P,
            isPendingBind,
            channel,
            maxChannel,
            frequency,
            name,
            p2pType,
            dimension,
            dimensionId,
            position,
            direction);
    }

    public String toShortString() {
        return position.toString() + " | " + direction;
    }
}
