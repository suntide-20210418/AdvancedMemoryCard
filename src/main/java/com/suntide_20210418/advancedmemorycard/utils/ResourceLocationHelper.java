package com.suntide_20210418.advancedmemorycard.utils;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.util.ResourceLocation;

public class ResourceLocationHelper {

    public static ResourceLocation modLoc(String path) {
        return new ResourceLocation(AdvancedMemoryCardMod.MOD_ID, path);
    }

    public static ResourceLocation blockModel(String blockName) {
        return modLoc("block/" + blockName);
    }

    public static ResourceLocation itemModel(String itemName) {
        return modLoc("item/" + itemName);
    }

    public static ResourceLocation blockState(String blockName) {
        return modLoc("blockstates/" + blockName);
    }

    public static ResourceLocation lang(String language) {
        return modLoc("lang/" + language);
    }

    public static ResourceLocation texture(String texturePath) {
        return modLoc("textures/" + texturePath);
    }

    public static ResourceLocation guiTexture(String guiTexture) {
        return texture("gui/" + guiTexture);
    }

    public static ResourceLocation blockTexture(String blockTexture) {
        return texture("block/" + blockTexture);
    }

    public static ResourceLocation itemTexture(String itemTexture) {
        return texture("item/" + itemTexture);
    }

    public static ResourceLocation sound(String soundName) {
        return modLoc("sounds/" + soundName);
    }

    public static ResourceLocation recipe(String recipeName) {
        return modLoc("recipes/" + recipeName);
    }

    public static ResourceLocation lootTable(String lootTablePath) {
        return modLoc("loot_tables/" + lootTablePath);
    }

    public static boolean isModResource(ResourceLocation resourceLocation) {
        return AdvancedMemoryCardMod.MOD_ID.equals(resourceLocation.getResourceDomain());
    }

    public static String getPath(ResourceLocation resourceLocation) {
        return resourceLocation.getResourcePath();
    }

    public static String getNamespace(ResourceLocation resourceLocation) {
        return resourceLocation.getResourceDomain();
    }
}
