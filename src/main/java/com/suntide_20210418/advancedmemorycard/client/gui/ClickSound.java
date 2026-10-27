package com.suntide_20210418.advancedmemorycard.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 界面控件的点击音效。
 *
 * <p>1.12.2 里没有统一的控件基类，复选框、列表行、自绘按钮各自处理点击，
 * 于是音效很容易漏掉某一种。统一走这里，保证"能点的东西都有声音"。
 */
@SideOnly(Side.CLIENT)
public final class ClickSound {

    private ClickSound() {
    }

    public static void play() {
        Minecraft.getMinecraft().getSoundHandler()
                .playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
