package com.suntide_20210418.advancedmemorycard.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 按钮点击音效。
 *
 * <p>1.7.10 没有 {@code SoundEvents} 常量类，音效只能按资源名找；{@code gui.button.press}
 * 是原版自己按钮用的那条，和 1.12.2 的 {@code SoundEvents.UI_BUTTON_CLICK} 是同一个音效。
 */
@SideOnly(Side.CLIENT)
public final class ClickSound {

    private static final ResourceLocation BUTTON_PRESS = new ResourceLocation("gui.button.press");

    private ClickSound() {}

    public static void play() {
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(PositionedSoundRecord.func_147674_a(BUTTON_PRESS, 1.0F));
    }
}
