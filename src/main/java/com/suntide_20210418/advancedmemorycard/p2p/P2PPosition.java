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
        // 维度必须参与比较：主世界与下界可以存在同坐标、同朝向的两台 P2P。
        // 只比坐标会把它们并成同一个键 —— 多选集合永远只选中一台，按位置反查
        // 设备也会命中错误的那一台。
        return Objects.equals(position, that.position)
                && direction == that.direction
                && Objects.equals(dimension, that.dimension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, direction, dimension);
    }
}
