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
import net.minecraft.world.phys.AABB;

public class CopyMode extends CardMode {

    private static final int MAX_VOLUME = 1024;
    private static final String START_POS = "start_pos";
    private static final String END_POS = "end_pos";
    private static final String IS_COPYING = "is_copying";

    private BlockPos startPos;
    private BlockPos endPos;
    private boolean isCopying;
    private AABB selectionBox; // 添加AABB用于区域表示

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
        
        // 重建AABB
        updateSelectionBox();
        
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

    // 更新选择框AABB
    private void updateSelectionBox() {
        if (startPos != null && endPos != null) {
            // 创建标准化的AABB（确保min <= max）
            double minX = Math.min(startPos.getX(), endPos.getX());
            double minY = Math.min(startPos.getY(), endPos.getY());
            double minZ = Math.min(startPos.getZ(), endPos.getZ());
            double maxX = Math.max(startPos.getX(), endPos.getX()) + 1;
            double maxY = Math.max(startPos.getY(), endPos.getY()) + 1;
            double maxZ = Math.max(startPos.getZ(), endPos.getZ()) + 1;
            
            selectionBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            selectionBox = null;
        }
    }

    // 获取当前选择的AABB
    public AABB getSelectionBox() {
        return selectionBox;
    }

    // 检查位置是否在选择区域内
    public boolean isInSelection(BlockPos pos) {
        if (selectionBox == null) return false;
        return selectionBox.contains(pos.getCenter());
    }

    // 获取选择区域的体积
    public long getSelectionVolume() {
        if (selectionBox == null) return 0;
        return (long) (selectionBox.getXsize() * selectionBox.getYsize() * selectionBox.getZsize());
    }

    // 检查是否有有效的选择区域
    public boolean hasValidSelection() {
        return selectionBox != null && startPos != null && endPos != null;
    }

    // 获取渲染颜色（可以根据不同状态调整）
    public int getSelectionColor() {
        if (isCopying) {
            return 0x00FF00; // 绿色 - 准备粘贴状态
        } else if (endPos == null && startPos != null) {
            return 0xFFFF00; // 黄色 - 选择第二个点状态
        } else {
            return 0xFF0000; // 红色 - 选择第一个点状态
        }
    }

    // 获取线框的渲染位置（世界坐标）
    public BlockPos getRenderMinPos() {
        if (selectionBox == null) return null;
        return BlockPos.containing(selectionBox.minX, selectionBox.minY, selectionBox.minZ);
    }

    public BlockPos getRenderMaxPos() {
        if (selectionBox == null) return null;
        return BlockPos.containing(selectionBox.maxX - 0.001, selectionBox.maxY - 0.001, selectionBox.maxZ - 0.001);
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
            
            // 使用AABB遍历区域内的所有方块
            if (selectionBox != null) {
                BlockPos minPos = BlockPos.containing(selectionBox.minX, selectionBox.minY, selectionBox.minZ);
                BlockPos maxPos = BlockPos.containing(selectionBox.maxX - 1, selectionBox.maxY - 1, selectionBox.maxZ - 1);
                
                Iterable<BlockPos> positions = BlockPos.betweenClosed(minPos, maxPos);
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
            this.selectionBox = null;
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

        } else if (endPos == null) {
            endPos = clickedPos;
            
            // 更新AABB
            updateSelectionBox();
            
            long currentVolume = getSelectionVolume();
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
                this.selectionBox = null;
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
                                selectionBox.getXsize(),
                                selectionBox.getYsize(),
                                selectionBox.getZsize(),
                                currentVolume
                        ),
                        true
                );
            }

            return InteractionResult.SUCCESS;
        } else {
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable(
                                "gui.advanced_memory_card.advanced_memory_card.player.copy.already_marked"
                        ),
                        true
                );
            }
        }

        return InteractionResult.PASS;
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
}
