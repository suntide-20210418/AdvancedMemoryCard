package com.suntide_20210418.advancedmemorycard.command;

import com.suntide_20210418.advancedmemorycard.utils.DimensionHelper;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * 服务端指令：将执行者传送至指定维度与坐标。
 * 跨维度传送通过 Forge API（EntityPlayerMP.changeDimension + setPositionAndUpdate）完成。
 * 指令由聊天栏中的可点击链接（ClickEvent.RUN_COMMAND）触发，故需要 OP/作弊权限。
 */
public class TeleportCommand extends CommandBase {

    @Override
    public String getName() {
        return "amc_tp";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return TranslateHelper.Chat.teleportUsage();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 4) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + getUsage(sender)));
            return;
        }
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + TranslateHelper.Chat.teleportOnlyPlayer()));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        int targetDim = parseInt(args[0]);
        double x = parseDouble(args[1]);
        double y = parseDouble(args[2]);
        double z = parseDouble(args[3]);

        // 跨维度传送：changeDimension 会重新创建玩家实体，必须接收返回值。
        if (player.dimension != targetDim) {
            Entity transferred = player.changeDimension(targetDim);
            if (transferred instanceof EntityPlayerMP) {
                player = (EntityPlayerMP) transferred;
            } else {
                sender.sendMessage(new TextComponentString(TextFormatting.RED + TranslateHelper.Chat.teleportDimFailed()));
                return;
            }
        }

        player.setPositionAndUpdate(x, y, z);
        String dimName = DimensionHelper.getDimensionName(targetDim);
        String coords = x + ", " + y + ", " + z;
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + TranslateHelper.Chat.teleportSuccess(dimName, coords)));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, @Nullable BlockPos targetPos) {
        return Collections.emptyList();
    }
}
