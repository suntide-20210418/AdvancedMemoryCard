package com.suntide_20210418.advancedmemorycard.utils;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.resources.ResourceLocation;

public class ResourceLocationHelper {

  /**
   * 创建带有模组ID前缀的ResourceLocation
   *
   * @param path 资源路径
   * @return ResourceLocation对象
   */
  public static ResourceLocation modLoc(String path) {
    return ResourceLocation.fromNamespaceAndPath(AdvancedMemoryCardMod.MOD_ID, path);
  }

  /**
   * 创建方块模型的ResourceLocation
   *
   * @param blockName 方块名称
   * @return 方块模型的ResourceLocation
   */
  public static ResourceLocation blockModel(String blockName) {
    return modLoc("block/" + blockName);
  }

  /**
   * 创建物品模型的ResourceLocation
   *
   * @param itemName 物品名称
   * @return 物品模型的ResourceLocation
   */
  public static ResourceLocation itemModel(String itemName) {
    return modLoc("item/" + itemName);
  }

  /**
   * 创建方块状态JSON的ResourceLocation
   *
   * @param blockName 方块名称
   * @return 方块状态的ResourceLocation
   */
  public static ResourceLocation blockState(String blockName) {
    return modLoc("blockstates/" + blockName);
  }

  /**
   * 创建语言文件的ResourceLocation
   *
   * @param language 语言代码 (如 "en_us", "zh_cn")
   * @return 语言文件的ResourceLocation
   */
  public static ResourceLocation lang(String language) {
    return modLoc("lang/" + language);
  }

  /**
   * 创建纹理的ResourceLocation
   *
   * @param texturePath 纹理路径 (相对于textures目录)
   * @return 纹理的ResourceLocation
   */
  public static ResourceLocation texture(String texturePath) {
    return modLoc("textures/" + texturePath);
  }

  /**
   * 创建GUI纹理的ResourceLocation
   *
   * @param guiTexture GUI纹理名称
   * @return GUI纹理的ResourceLocation
   */
  public static ResourceLocation guiTexture(String guiTexture) {
    return texture("gui/" + guiTexture);
  }

  /**
   * 创建方块纹理的ResourceLocation
   *
   * @param blockTexture 方块纹理名称
   * @return 方块纹理的ResourceLocation
   */
  public static ResourceLocation blockTexture(String blockTexture) {
    return texture("block/" + blockTexture);
  }

  /**
   * 创建物品纹理的ResourceLocation
   *
   * @param itemTexture 物品纹理名称
   * @return 物品纹理的ResourceLocation
   */
  public static ResourceLocation itemTexture(String itemTexture) {
    return texture("item/" + itemTexture);
  }

  /**
   * 创建声音文件的ResourceLocation
   *
   * @param soundName 声音名称
   * @return 声音文件的ResourceLocation
   */
  public static ResourceLocation sound(String soundName) {
    return modLoc("sounds/" + soundName);
  }

  /**
   * 创建配方文件的ResourceLocation
   *
   * @param recipeName 配方名称
   * @return 配方文件的ResourceLocation
   */
  public static ResourceLocation recipe(String recipeName) {
    return modLoc("recipes/" + recipeName);
  }

  /**
   * 创建战利品表的ResourceLocation
   *
   * @param lootTablePath 战利品表路径
   * @return 战利品表的ResourceLocation
   */
  public static ResourceLocation lootTable(String lootTablePath) {
    return modLoc("loot_tables/" + lootTablePath);
  }

  /**
   * 验证ResourceLocation是否属于本模组
   *
   * @param resourceLocation 要验证的ResourceLocation
   * @return 如果属于本模组返回true，否则返回false
   */
  public static boolean isModResource(ResourceLocation resourceLocation) {
    return AdvancedMemoryCardMod.MOD_ID.equals(resourceLocation.getNamespace());
  }

  /**
   * 获取ResourceLocation的路径部分（不包含命名空间）
   *
   * @param resourceLocation ResourceLocation对象
   * @return 路径字符串
   */
  public static String getPath(ResourceLocation resourceLocation) {
    return resourceLocation.getPath();
  }

  /**
   * 获取ResourceLocation的命名空间
   *
   * @param resourceLocation ResourceLocation对象
   * @return 命名空间字符串
   */
  public static String getNamespace(ResourceLocation resourceLocation) {
    return resourceLocation.getNamespace();
  }
}
