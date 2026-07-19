package com.suntide_20210418.advancedmemorycard.utils;

import net.minecraft.util.AxisAlignedBB;

public class AreaHelper {

    public static AxisAlignedBB createAABB(BlockPos startPos, BlockPos endPos) {
        if (startPos != null && endPos != null) {
            double minX = Math.min(startPos.getX(), endPos.getX());
            double minY = Math.min(startPos.getY(), endPos.getY());
            double minZ = Math.min(startPos.getZ(), endPos.getZ());
            double maxX = Math.max(startPos.getX(), endPos.getX()) + 1;
            double maxY = Math.max(startPos.getY(), endPos.getY()) + 1;
            double maxZ = Math.max(startPos.getZ(), endPos.getZ()) + 1;
            return AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            return null;
        }
    }

    public static long calculateVolume(AxisAlignedBB aabb) {
        if (aabb == null) {
            return 0;
        }
        double width = aabb.maxX - aabb.minX;
        double height = aabb.maxY - aabb.minY;
        double depth = aabb.maxZ - aabb.minZ;
        return (long) (width * height * depth);
    }

    public static long calculateVolume(BlockPos startPos, BlockPos endPos) {
        int width = Math.abs(startPos.getX() - endPos.getX()) + 1;
        int height = Math.abs(startPos.getY() - endPos.getY()) + 1;
        int depth = Math.abs(startPos.getZ() - endPos.getZ()) + 1;
        return (long) width * height * depth;
    }

    public static int[] Area(BlockPos startPos, BlockPos endPos) {
        int width = Math.abs(startPos.getX() - endPos.getX()) + 1;
        int height = Math.abs(startPos.getY() - endPos.getY()) + 1;
        int depth = Math.abs(startPos.getZ() - endPos.getZ()) + 1;
        return new int[] { width, height, depth };
    }

    public static float[] RGB(int color) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        return new float[] { red, green, blue };
    }
}
