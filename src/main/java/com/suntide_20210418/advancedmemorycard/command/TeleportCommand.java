package com.suntide_20210418.advancedmemorycard.command;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.suntide_20210418.advancedmemorycard.utils.DimensionHelper;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;

/**
 * 服务端指令：将执行者传送至指定维度与坐标。
 * 跨维度传送通过 Forge API（EntityPlayerMP.changeDimension + setPositionAndUpdate）完成。
 * 指令由聊天栏中的可点击链接（ClickEvent 触发）触发，故需要 OP/作弊权限。
 */
public class TeleportCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "amc_tp";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return TranslateHelper.Chat.teleportUsage();
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return sender.canCommandSenderUseCommand(2, getCommandName());
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + getCommandUsage(sender)));
            return;
        }
        if (!(sender instanceof EntityPlayerMP)) {
            sender.addChatMessage(
                new ChatComponentText(EnumChatFormatting.RED + TranslateHelper.Chat.teleportOnlyPlayer()));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        int targetDim = parseInt(sender, args[0]);
        double x = parseDouble(sender, args[1]);
        double y = parseDouble(sender, args[2]);
        double z = parseDouble(sender, args[3]);

        // 跨维度传送：1.7.10 通过 ServerConfigurationManager 移动同一玩家实体，无需替换引用。
        if (player.dimension != targetDim) {
            player.mcServer.getConfigurationManager()
                .transferPlayerToDimension(player, targetDim);
        }

        player.setPositionAndUpdate(x, y, z);
        String dimName = DimensionHelper.getDimensionName(targetDim);
        String coords = x + ", " + y + ", " + z;
        player.addChatMessage(
            new ChatComponentText(EnumChatFormatting.GREEN + TranslateHelper.Chat.teleportSuccess(dimName, coords)));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
