package com.suntide_20210418.advancedmemorycard.util;

import net.minecraft.core.BlockPos;

public class Caculation {
    public static int calculateVolume(BlockPos pos1, BlockPos pos2) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());

        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        int width = maxX - minX + 1;  // +1 因为包括两端
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;

        return width * height * depth;  // 体积（方块数量）
    }
}
