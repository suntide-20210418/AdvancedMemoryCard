package com.suntide_20210418.advancedmemorycard.utils;

import net.minecraft.world.DimensionType;

/**
 * 维度相关工具：将维度 ID(int) 映射为可读的名称字符串。
 */
public class DimensionHelper {

    /**
     * 将维度 ID 转换为名称字符串（如 "Overworld" / "The Nether" / "The End"）。
     * 未知维度回退为数字字符串。
     */
    public static String getDimensionName(int dimId) {
        DimensionType type = DimensionType.getById(dimId);
        if (type != null) {
            return type.getName();
        }
        return String.valueOf(dimId);
    }
}
