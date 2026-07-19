package com.suntide_20210418.advancedmemorycard.utils;

/**
 * 1.7.10 兼容用的 BlockPos 占位实现。
 *
 * <p>
 * 原代码面向 1.8+ 编写，使用 {@code net.minecraft.util.BlockPos}；而 1.7.10 中并不存在该类，
 * 因此在此提供最小兼容实现，仅覆盖本项目实际用到的 API（坐标构造、字段访问、getter、
 * 值相等的 equals/hashCode，以及匹配解析正则的 toString）。
 * </p>
 */
public class BlockPos {

    public final int x;
    public final int y;
    public final int z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockPos blockPos = (BlockPos) o;
        return x == blockPos.x && y == blockPos.y && z == blockPos.z;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Override
    public String toString() {
        return "BlockPos{x=" + x + ", y=" + y + ", z=" + z + "}";
    }
}
