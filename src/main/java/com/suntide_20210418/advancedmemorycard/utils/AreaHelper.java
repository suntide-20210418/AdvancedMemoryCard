package com.suntide_20210418.advancedmemorycard.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class AreaHelper {
    public static AABB createAABB(BlockPos startPos, BlockPos endPos) {
        if (startPos != null && endPos != null) {
            // 创建标准化的AABB（确保min <= max）
            double minX = Math.min(startPos.getX(), endPos.getX());
            double minY = Math.min(startPos.getY(), endPos.getY());
            double minZ = Math.min(startPos.getZ(), endPos.getZ());
            double maxX = Math.max(startPos.getX(), endPos.getX()) + 1;
            double maxY = Math.max(startPos.getY(), endPos.getY()) + 1;
            double maxZ = Math.max(startPos.getZ(), endPos.getZ()) + 1;

            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            return null;
        }
    }

    public static long calculateVolume(AABB aabb) {
        if (aabb == null) {
            return 0;
        }
        double width = aabb.getXsize();
        double height = aabb.getYsize();
        double depth = aabb.getZsize();
        return (long) (width * height * depth);
    }

    public static long calculateVolume(BlockPos startPos, BlockPos endPos) {
        int width = Math.abs(startPos.getX() - endPos.getX()) + 1;
        int height = Math.abs(startPos.getY() - endPos.getY()) + 1;
        int depth = Math.abs(startPos.getZ() - endPos.getZ()) + 1;
        return (long) width * height * depth;
    }

    public static int[] Area(BlockPos startPos, BlockPos endPos){
        int width = Math.abs(startPos.getX() - endPos.getX()) + 1;
        int height = Math.abs(startPos.getY() - endPos.getY()) + 1;
        int depth = Math.abs(startPos.getZ() - endPos.getZ()) + 1;
        return new int[]{width, height, depth};
    }

    public static float[] RGB(int color){
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        return new float[]{red, green, blue};
    }
}
