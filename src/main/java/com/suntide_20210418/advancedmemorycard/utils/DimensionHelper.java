package com.suntide_20210418.advancedmemorycard.utils;

/**
 * 维度相关工具：将维度 ID(int) 映射为可读的名称字符串。
 */
public class DimensionHelper {

    /**
     * 将维度 ID 转换为名称字符串（如 "Overworld" / "The Nether" / "The End"）。
     * 未知维度回退为数字字符串。
     */
    public static String getDimensionName(int dimId) {
        switch (dimId) {
            case 0:
                return "Overworld";
            case -1:
                return "The Nether";
            case 1:
                return "The End";
            default:
                return String.valueOf(dimId);
        }
    }
}
