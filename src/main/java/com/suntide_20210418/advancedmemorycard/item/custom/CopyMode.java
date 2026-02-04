package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.items.tools.MemoryCardItem;
import appeng.util.SettingsFrom;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import static com.suntide_20210418.advancedmemorycard.utils.AreaHelper.Area;
import static com.suntide_20210418.advancedmemorycard.utils.AreaHelper.calculateVolume;

public class CopyMode extends CardMode {

    private static final int MAX_VOLUME = 2048;
    private static final String START_POS = "start_pos";
    private static final String END_POS = "end_pos";
    private static final String IS_COPYING = "is_copying";

    private BlockPos startPos;
    private BlockPos endPos;
    private boolean isCopying;

    public CopyMode() {}

    @Override
    protected CardMode load(CompoundTag tag) {
        // 处理 startPos 为空的情况
        if (tag.contains(START_POS)) {
            startPos = BlockPos.of(tag.getLong(START_POS));
        } else {
            startPos = null;
        }
        
        // 处理 endPos 为空的情况
        if (tag.contains(END_POS)) {
            endPos = BlockPos.of(tag.getLong(END_POS));
        } else {
            endPos = null;
        }
        
        // 处理 isCopying 为空的情况，默认为 false
        isCopying = tag.getBoolean(IS_COPYING);
        
        return this;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag data = super.save(tag);
        
        // 只有当值不为 null 时才保存
        if (startPos != null) {
            data.putLong(START_POS, startPos.asLong());
        }
        
        if (endPos != null) {
            data.putLong(END_POS, endPos.asLong());
        }
        
        data.putBoolean(IS_COPYING, isCopying);
        return data;
    }

    // 获取渲染颜色（可以根据不同状态调整）
    public int getSelectionColor() {
        if (isCopying) {
            return 0x00FF00; // 绿色 - 准备粘贴状态
        } else if (endPos == null && startPos != null) {
            return 0xFFFFFF; // 白色 - 选择第二个点状态
        } else {
            return 0xFF0000; // 红色 - 选择第一个点状态
        }
    }

    public BlockPos getStartPos() {
        return startPos;
    }

    public BlockPos getEndPos() {
        return endPos;
    }

    @Override
    public InteractionResultHolder<ItemStack> onItemUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.isCopying) {
            MemoryCardItem memoryCardItem = (MemoryCardItem) stack.getItem();
            CompoundTag data = memoryCardItem.getData(stack);
            int count = 0;

            if (startPos != null && endPos != null) {
                Iterable<BlockPos> positions = BlockPos.betweenClosed(startPos, endPos);
                for (BlockPos pos : positions) {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof AEBaseBlockEntity aeBlockEntity) {
                        aeBlockEntity.importSettings(SettingsFrom.MEMORY_CARD, data, player);
                        count++;
                    }
                    // 处理AE2部件宿主（IPartHost）
                    if (blockEntity instanceof IPartHost partHost) {
                        // 获取所有方向（包括内部）
                        for (Direction direction : Direction.values()) {
                            IPart part = partHost.getPart(direction);
                            if (part != null) {
                                // 尝试导入设置到部件
                                part.onActivate(player, InteractionHand.MAIN_HAND, pos.getCenter());
                                count++;
                            }
                        }
                    }
                }
            }
            
            player.displayClientMessage(
                    Component.translatable(
                            "gui.advanced_memory_card.advanced_memory_card.player.copy.completed", count
                    ),
                    true
            );
            this.isCopying = false;
            this.startPos = null;
            this.endPos = null;
            this.save(stack.getOrCreateTag());
        } else {
            player.displayClientMessage(
                    Component.translatable(
                            "gui.advanced_memory_card.advanced_memory_card.player.copy.failed"
                    ),
                    true
            );
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ResourceLocation getType() {
        return ResourceLocation.fromNamespaceAndPath(AdvancedMemoryCardMod.MOD_ID, "copy");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();
        if (startPos == null){
            startPos = clickedPos;
            this.save(stack.getOrCreateTag());

            // 向玩家发送信息
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable(
                                "gui.advanced_memory_card.advanced_memory_card.player.copy.first_pos_marked",
                                startPos.toShortString()
                        ),
                        true
                );
            }

            return InteractionResult.SUCCESS;

        } else {
            endPos = clickedPos;
            
            long currentVolume = calculateVolume(startPos, endPos);
            if (currentVolume > MAX_VOLUME) {
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    "gui.advanced_memory_card.advanced_memory_card.player.copy.too_large",
                                    currentVolume, MAX_VOLUME
                            ),
                            true
                    );
                }
                // 重置选择
                this.startPos = null;
                this.endPos = null;
                this.save(stack.getOrCreateTag());
                return InteractionResult.FAIL;
            }

            isCopying = true;
            this.save(stack.getOrCreateTag());

            if (player != null) {
                player.displayClientMessage(
                        Component.translatable(
                                "gui.advanced_memory_card.advanced_memory_card.player.copy.second_pos_marked",
                                endPos.toShortString(),
                                Area(startPos, endPos)[0],
                                Area(startPos, endPos)[1],
                                Area(startPos, endPos)[2]
                        ),
                        true
                );
            }

            return InteractionResult.SUCCESS;
        }
    }

    public BlockPos getTargetedBlockPos(Player player) {
        if (player == null) return null;

        // 使用内置的视线追踪方法
        HitResult hitResult = player.pick(player.getBlockReach(), 1.0F, false);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) hitResult).getBlockPos();
        }

        return null;
    }

    @Override
    protected Component getName() {
        return Component.translatable(
                "gui.advanced_memory_card.advanced_memory_card.player.copy.show"
        );
    }

    @Override
    protected Component getDescription() {
        if (startPos == null) {
            // 没有选择任何位置，显示Copy Mode
            return Component.translatable("gui.advanced_memory_card.advanced_memory_card.tooltip.copy.info");
        } else if (endPos == null) {
            // 已选择第一个位置，显示第一个位置信息
            return Component.translatable("gui.advanced_memory_card.advanced_memory_card.tooltip.copy.info")
                    .append(Component.translatable("gui.advanced_memory_card.advanced_memory_card.tooltip.copy.first_pos", startPos.toShortString()));
        } else {
            // 已选择两个位置，显示完整信息并提示准备粘贴
            return Component.translatable("gui.advanced_memory_card.advanced_memory_card.tooltip.copy.info")
                    .append(Component.translatable("gui.advanced_memory_card.advanced_memory_card.tooltip.copy.first_pos", startPos.toShortString()))
                    .append(Component.translatable("gui.advanced_memory_card.advanced_memory_card.tooltip.copy.second_pos", endPos.toShortString()))
                    .append(Component.translatable("gui.advanced_memory_card.advanced_memory_card.tooltip.copy.ready"));
        }
    }

    public static int getMaxVolume() {
        return MAX_VOLUME;
    }
}
