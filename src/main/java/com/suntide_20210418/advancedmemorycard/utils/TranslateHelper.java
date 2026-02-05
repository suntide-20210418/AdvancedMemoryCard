package com.suntide_20210418.advancedmemorycard.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

/**
 * 翻译助手工具类
 * 提供统一的翻译、格式化和本地化功能
 * 
 * @author suntide_20210418
 * @version 1.0.0
 */
public class TranslateHelper {
    
    // 基础翻译键前缀
    private static final String BASE_KEY = "advanced_memory_card";
    
    // 常用翻译键分类
    public static class Keys {
        // 物品相关
        public static final String ITEM_PREFIX = "item." + BASE_KEY + ".";
        public static final String ITEM_ADVANCED_MEMORY_CARD = ITEM_PREFIX + "advanced_memory_card";
        
        // 物品组相关
        public static final String ITEM_GROUP_PREFIX = "itemGroup." + BASE_KEY + ".";
        public static final String ITEM_GROUP_TAB = ITEM_GROUP_PREFIX + "tab";
        
        // GUI相关
        public static final String GUI_PREFIX = "gui." + BASE_KEY + ".";
        
        // 复制模式相关
        public static final String COPY_MODE_PREFIX = GUI_PREFIX + "advanced_memory_card.player.copy.";
        public static final String COPY_MODE_SHOW = COPY_MODE_PREFIX + "show";
        public static final String COPY_MODE_FIRST_POS_MARKED = COPY_MODE_PREFIX + "first_pos_marked";
        public static final String COPY_MODE_SECOND_POS_MARKED = COPY_MODE_PREFIX + "second_pos_marked";
        public static final String COPY_MODE_TOO_LARGE = COPY_MODE_PREFIX + "too_large";
        public static final String COPY_MODE_COMPLETED = COPY_MODE_PREFIX + "completed";
        public static final String COPY_MODE_ALREADY_MARKED = COPY_MODE_PREFIX + "already_marked";
        public static final String COPY_MODE_FAILED = COPY_MODE_PREFIX + "failed";
        
        // 配置模式相关
        public static final String CONFIG_MODE_PREFIX = GUI_PREFIX + "advanced_memory_card.player.config.";
        public static final String CONFIG_MODE_SHOW = CONFIG_MODE_PREFIX + "show";
        
        // 工具提示相关
        public static final String TOOLTIP_PREFIX = GUI_PREFIX + "advanced_memory_card.tooltip.";
        public static final String TOOLTIP_COPY_INFO = TOOLTIP_PREFIX + "copy.info";
        public static final String TOOLTIP_COPY_FIRST_POS = TOOLTIP_PREFIX + "copy.first_pos";
        public static final String TOOLTIP_COPY_SECOND_POS = TOOLTIP_PREFIX + "copy.second_pos";
        public static final String TOOLTIP_COPY_READY = TOOLTIP_PREFIX + "copy.ready";
        public static final String TOOLTIP_CONFIG_INFO = TOOLTIP_PREFIX + "config.info";
    }
    
    /**
     * 获取基础翻译组件
     * @param key 翻译键
     * @return 可变组件
     */
    public static MutableComponent translate(String key) {
        return Component.translatable(key);
    }
    
    /**
     * 获取带参数的翻译组件
     * @param key 翻译键
     * @param args 参数
     * @return 可变组件
     */
    public static MutableComponent translate(String key, Object... args) {
        return Component.translatable(key, args);
    }
    
    /**
     * 获取带样式的翻译组件
     * @param key 翻译键
     * @param style 样式
     * @return 可变组件
     */
    public static MutableComponent translateWithStyle(String key, Style style) {
        return Component.translatable(key).setStyle(style);
    }
    
    /**
     * 获取带颜色的翻译组件
     * @param key 翻译键
     * @param color 颜色
     * @return 可变组件
     */
    public static MutableComponent translateWithColor(String key, ChatFormatting color) {
        return Component.translatable(key).withStyle(color);
    }
    
    /**
     * 获取带多个参数和颜色的翻译组件
     * @param key 翻译键
     * @param color 颜色
     * @param args 参数
     * @return 可变组件
     */
    public static MutableComponent translateWithColor(String key, ChatFormatting color, Object... args) {
        return Component.translatable(key, args).withStyle(color);
    }
    
    /**
     * 获取成功消息样式
     * @param key 翻译键
     * @param args 参数
     * @return 可变组件
     */
    public static MutableComponent successMessage(String key, Object... args) {
        return translateWithColor(key, ChatFormatting.GREEN, args);
    }
    
    /**
     * 获取警告消息样式
     * @param key 翻译键
     * @param args 参数
     * @return 可变组件
     */
    public static MutableComponent warningMessage(String key, Object... args) {
        return translateWithColor(key, ChatFormatting.YELLOW, args);
    }
    
    /**
     * 获取错误消息样式
     * @param key 翻译键
     * @param args 参数
     * @return 可变组件
     */
    public static MutableComponent errorMessage(String key, Object... args) {
        return translateWithColor(key, ChatFormatting.RED, args);
    }
    
    /**
     * 获取信息消息样式
     * @param key 翻译键
     * @param args 参数
     * @return 可变组件
     */
    public static MutableComponent infoMessage(String key, Object... args) {
        return translateWithColor(key, ChatFormatting.BLUE, args);
    }
    
    /**
     * 获取标题样式
     * @param key 翻译键
     * @param args 参数
     * @return 可变组件
     */
    public static MutableComponent titleMessage(String key, Object... args) {
        return translateWithColor(key, ChatFormatting.BOLD, args);
    }
    
    /**
     * 组合多个组件
     * @param components 组件数组
     * @return 组合后的可变组件
     */
    public static MutableComponent concat(Component... components) {
        if (components.length == 0) {
            return Component.empty();
        }
        
        MutableComponent result = components[0].copy();
        for (int i = 1; i < components.length; i++) {
            result.append(components[i]);
        }
        return result;
    }
    
    /**
     * 添加换行符
     * @param component 原始组件
     * @return 添加换行符后的组件
     */
    public static MutableComponent withNewLine(Component component) {
        return concat(component, Component.literal("\n"));
    }
    
    /**
     * 添加空格
     * @param component 原始组件
     * @return 添加空格后的组件
     */
    public static MutableComponent withSpace(Component component) {
        return concat(component, Component.literal(" "));
    }
    
    /**
     * 格式化坐标显示
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @return 格式化后的坐标字符串组件
     */
    public static MutableComponent formatCoordinates(int x, int y, int z) {
        return Component.literal(String.format("[%d, %d, %d]", x, y, z))
                .withStyle(ChatFormatting.AQUA);
    }
    
    /**
     * 格式化区域大小显示
     * @param width 宽度
     * @param height 高度
     * @param depth 深度
     * @return 格式化后的区域大小组件
     */
    public static MutableComponent formatAreaSize(int width, int height, int depth) {
        return Component.literal(String.format("[%d×%d×%d]", width, height, depth))
                .withStyle(ChatFormatting.GOLD);
    }
    
    /**
     * 格式化数字显示
     * @param number 数字
     * @param color 颜色
     * @return 格式化后的数字组件
     */
    public static MutableComponent formatNumber(long number, ChatFormatting color) {
        return Component.literal(String.valueOf(number)).withStyle(color);
    }
    
    /**
     * 格式化百分比显示
     * @param value 当前值
     * @param max 最大值
     * @return 格式化后的百分比组件
     */
    public static MutableComponent formatPercentage(long value, long max) {
        double percentage = max > 0 ? (double) value / max * 100 : 0;
        ChatFormatting color = percentage > 90 ? ChatFormatting.RED : 
                              percentage > 75 ? ChatFormatting.YELLOW : 
                              ChatFormatting.GREEN;
        return Component.literal(String.format("%.1f%%", percentage)).withStyle(color);
    }
    
    /**
     * 获取复制模式相关翻译
     */
    public static class CopyMode {
        public static MutableComponent show() {
            return translate(Keys.COPY_MODE_SHOW);
        }
        
        public static MutableComponent firstPosMarked(String pos) {
            return translate(Keys.COPY_MODE_FIRST_POS_MARKED, pos);
        }
        
        public static MutableComponent secondPosMarked(String pos, int width, int height, int depth) {
            return translate(Keys.COPY_MODE_SECOND_POS_MARKED, pos, width, height, depth);
        }
        
        public static MutableComponent tooLarge(long current, long max) {
            return errorMessage(Keys.COPY_MODE_TOO_LARGE, current, max);
        }
        
        public static MutableComponent completed(long count) {
            return successMessage(Keys.COPY_MODE_COMPLETED, count);
        }
        
        public static MutableComponent alreadyMarked() {
            return warningMessage(Keys.COPY_MODE_ALREADY_MARKED);
        }
        
        public static MutableComponent failed() {
            return errorMessage(Keys.COPY_MODE_FAILED);
        }
    }
    
    /**
     * 获取配置模式相关翻译
     */
    public static class ConfigMode {
        public static MutableComponent show() {
            return translate(Keys.CONFIG_MODE_SHOW);
        }
    }
    
    /**
     * 获取工具提示相关翻译
     */
    public static class Tooltip {
        public static MutableComponent copyInfo() {
            return translate(Keys.TOOLTIP_COPY_INFO);
        }
        
        public static MutableComponent copyFirstPos(String pos) {
            return translate(Keys.TOOLTIP_COPY_FIRST_POS, pos);
        }
        
        public static MutableComponent copySecondPos(String pos) {
            return translate(Keys.TOOLTIP_COPY_SECOND_POS, pos);
        }
        
        public static MutableComponent copyReady() {
            return translate(Keys.TOOLTIP_COPY_READY);
        }
        
        public static MutableComponent configInfo() {
            return translate(Keys.TOOLTIP_CONFIG_INFO);
        }
    }
    
    /**
     * 获取常用翻译键的便捷方法
     */
    public static class Common {
        public static MutableComponent itemName() {
            return translate(Keys.ITEM_ADVANCED_MEMORY_CARD);
        }
        
        public static MutableComponent itemGroupName() {
            return translate(Keys.ITEM_GROUP_TAB);
        }
    }
}
