package com.suntide_20210418.advancedmemorycard.p2p;

import appeng.api.util.AEPartLocation;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class P2PPosition {
    public final BlockPos position;
    public final AEPartLocation direction;
    public final String dimension;

    public P2PPosition(BlockPos position, AEPartLocation direction, String dimension) {
        this.position = position;
        this.direction = direction;
        this.dimension = dimension;
    }

    public BlockPos position() { return position; }
    public AEPartLocation direction() { return direction; }
    public String dimension() { return dimension; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        P2PPosition that = (P2PPosition) o;
        return Objects.equals(position, that.position) && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, direction);
    }
}
